package titan.ccp.anomalydetection.streaming;

import com.datastax.driver.core.Session;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import titan.ccp.common.avro.cassandra.AvroDataAdapter;
import titan.ccp.common.cassandra.CassandraWriter;
import titan.ccp.common.cassandra.ExplicitPrimaryKeySelectionStrategy;
import titan.ccp.model.records.ActivePowerRecord;
import titan.ccp.model.records.AggregatedActivePowerRecord;
import titan.ccp.model.records.AnomalyPowerRecord;

/**
 * Builds Kafka Stream Topology for the Stats microservice.
 */
public class TopologyBuilder {

  private static final Logger LOGGER = LoggerFactory.getLogger(TopologyBuilder.class);

  private static final String ANOMALIES_TABLE = "anomalies";
  private static final String IDENTIFIER_COLUMN = "identifier";
  private static final String TIMESTAMP_COLUMN = "timestamp";

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

    LOGGER.info("hourOfWeekStatsTopic: {}", hourOfWeekStatsTopic); // TODO remove

    return recordStream
        .mapValues(record -> new AnomalyPowerRecord(
            record.getIdentifier(),
            record.getTimestamp(),
            record.getValueInW(), 1.0));
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
        .peek((k, v) -> LOGGER.info("Write anaomly record to database: {}.", v))
        .foreach((k, v) -> cassandraWriter.write(v));
  }

  private void publishAnomalyStream(final KStream<String, AnomalyPowerRecord> recordStream,
      final String anomaliesTopic) {
    recordStream.to(anomaliesTopic, Produced.with(
        this.serdes.string(),
        this.serdes.avroValues()));
  }


}
