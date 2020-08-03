package titan.ccp.anomalydetection.streaming;

import com.datastax.driver.core.Session;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.Joined;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Produced;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import titan.ccp.common.avro.cassandra.AvroDataAdapter;
import titan.ccp.common.cassandra.CassandraWriter;
import titan.ccp.common.cassandra.ExplicitPrimaryKeySelectionStrategy;
import titan.ccp.model.records.ActivePowerRecord;
import titan.ccp.model.records.AggregatedActivePowerRecord;
import titan.ccp.model.records.AnomalyPowerRecord;
import titan.ccp.model.records.HourOfWeekActivePowerRecord;

/**
 * Builds Kafka Stream Topology for the Stats microservice.
 */
public class TopologyBuilder {

  private static final Logger LOGGER = LoggerFactory.getLogger(TopologyBuilder.class);

  private static final String ANOMALIES_TABLE = "anomalies";
  private static final String IDENTIFIER_COLUMN = "identifier";
  private static final String TIMESTAMP_COLUMN = "timestamp";

  private final ZoneId zone = ZoneId.of("Europe/Paris"); // TODO as parameter

  private final Serdes serdes;
  private final Session cassandraSession;
  private final StreamsBuilder builder = new StreamsBuilder();

  /**
   * Create a new {@link TopologyBuilder}.
   */
  public TopologyBuilder(
      final Serdes serdes,
      final Session cassandraSession,
      final String activePowerTopic,
      final String aggregatedActivePowerTopic,
      final String hourOfWeekStatsTopic,
      final String anomaliesTopic) {

    this.serdes = serdes;
    this.cassandraSession = cassandraSession;

    final KStream<String, ActivePowerRecord> recordStream =
        this.buildInputStream(activePowerTopic, aggregatedActivePowerTopic);

    final KStream<String, AnomalyPowerRecord> anomalyStream =
        this.buildAnomalyStream(recordStream, hourOfWeekStatsTopic);
    this.storeAnomalyStream(anomalyStream);
    this.publishAnomalyStream(anomalyStream, anomaliesTopic);
  }

  public Topology build() {
    return this.builder.build();
  }

  private KStream<String, ActivePowerRecord> buildInputStream(final String activePowerTopic,
      final String aggrActivePowerTopic) {
    final KStream<String, ActivePowerRecord> activePowerStream = this.builder
        .stream(
            activePowerTopic,
            Consumed.with(
                this.serdes.string(),
                this.serdes.avroValues()));

    final KStream<String, ActivePowerRecord> aggrActivePowerStream =
        this.builder
            .stream(
                aggrActivePowerTopic,
                Consumed.with(
                    this.serdes.string(),
                    this.serdes.<AggregatedActivePowerRecord>avroValues()))
            .mapValues(
                aggr -> new ActivePowerRecord(
                    aggr.getIdentifier(),
                    aggr.getTimestamp(),
                    aggr.getSumInW()));

    return activePowerStream.merge(aggrActivePowerStream);
  }

  private KStream<String, AnomalyPowerRecord> buildAnomalyStream(
      final KStream<String, ActivePowerRecord> recordStream, final String hourOfWeekStatsTopic) {

    final KTable<HourOfWeekKey, HourOfWeekActivePowerRecord> statsTable = this.builder
        .stream(
            hourOfWeekStatsTopic,
            Consumed.with(
                this.serdes.string(),
                this.serdes.<HourOfWeekActivePowerRecord>avroValues()))
        .groupBy(
            (k, record) -> new HourOfWeekKey(
                DayOfWeek.of(record.getDayOfWeek()),
                record.getHourOfDay(),
                record.getIdentifier()),
            Grouped.with(
                this.serdes.hourOfWeekKey(),
                this.serdes.<HourOfWeekActivePowerRecord>avroValues()))
        .reduce((a, b) -> {
          if (a.getPeriodEnd() < b.getPeriodEnd()) {
            return b;
          } else if (a.getPeriodEnd() > b.getPeriodEnd()) {
            return a;
          } else if (a.getCount() <= b.getCount()) {
            return b;
          } else {
            return a;
          }
        });

    return recordStream
        .selectKey((key, value) -> {
          final Instant instant = Instant.ofEpochMilli(value.getTimestamp());
          final LocalDateTime dateTime = LocalDateTime.ofInstant(instant, this.zone);
          final DayOfWeek dayOfWeek = dateTime.getDayOfWeek();
          final int hourOfDay = dateTime.getHour();
          return new HourOfWeekKey(dayOfWeek, hourOfDay, value.getIdentifier());
        })
        .join(statsTable,
            (measurement, stats) -> new MeasurementStatsPair(measurement, stats),
            Joined.with(
                this.serdes.hourOfWeekKey(),
                this.serdes.avroValues(),
                this.serdes.avroValues()))
        .mapValues(j -> new AnomalyPowerRecord(
            j.getMeasurement().getIdentifier(),
            j.getMeasurement().getTimestamp(),
            j.getMeasurement().getValueInW(),
            this.calculateAnomalyScore(j)))
        .selectKey((key, v) -> key.getSensorId());
  }

  private void storeAnomalyStream(final KStream<String, AnomalyPowerRecord> recordStream) {
    final ExplicitPrimaryKeySelectionStrategy keySelector =
        new ExplicitPrimaryKeySelectionStrategy();
    keySelector.registerPartitionKeys(ANOMALIES_TABLE, IDENTIFIER_COLUMN);
    keySelector.registerClusteringColumns(ANOMALIES_TABLE, TIMESTAMP_COLUMN);
    final CassandraWriter<SpecificRecord> cassandraWriter = CassandraWriter
        .builder(this.cassandraSession, new AvroDataAdapter())
        .tableNameMapper(r -> ANOMALIES_TABLE)
        .primaryKeySelectionStrategy(keySelector)
        .build();

    recordStream
        .peek((k, v) -> LOGGER.info("Write anomaly record to database: {}.", v))
        .foreach((k, v) -> cassandraWriter.write(v));
  }

  private void publishAnomalyStream(final KStream<String, AnomalyPowerRecord> recordStream,
      final String anomaliesTopic) {
    recordStream.to(anomaliesTopic, Produced.with(
        this.serdes.string(),
        this.serdes.avroValues()));
  }

  private double calculateAnomalyScore(final MeasurementStatsPair measurementStatsPair) {
    final double value = measurementStatsPair.getMeasurement().getValueInW();
    final double mean = measurementStatsPair.getStats().getMean();
    final double standardDeviation =
        Math.sqrt(measurementStatsPair.getStats().getPopulationVariance());

    return (value - mean) / standardDeviation; // zScore
  }


}
