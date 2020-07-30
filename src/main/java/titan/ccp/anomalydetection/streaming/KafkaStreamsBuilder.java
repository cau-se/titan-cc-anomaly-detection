package titan.ccp.anomalydetection.streaming;

import com.datastax.driver.core.Session;
import java.util.Objects;
import java.util.Properties;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import titan.ccp.common.kafka.streams.PropertiesBuilder;

/**
 * Builder for the statistics {@link KafkaStreams} configuration.
 */
public class KafkaStreamsBuilder { // NOPMD Builder class

  private static final String APPLICATION_NAME = "titan-ccp-anomaly-detection";
  private static final String APPLICATION_VERSION = "0.0.1";

  private String bootstrapServers; // NOPMD
  private String activePowerTopic; // NOPMD
  private String aggrActivePowerTopic; // NOPMD
  private String anomaliesTopic; // NOPMD
  private String schemaRegistryUrl; // NOPMD
  private Session cassandraSession; // NOPMD
  private int numThreads = -1; // NOPMD
  private int commitIntervalMs = -1; // NOPMD
  private int cacheMaxBytesBuff = -1; // NOPMD

  public KafkaStreamsBuilder bootstrapServers(final String bootstrapServers) {
    this.bootstrapServers = bootstrapServers;
    return this;
  }

  public KafkaStreamsBuilder cassandraSession(final Session cassandraSession) {
    this.cassandraSession = cassandraSession;
    return this;
  }

  public KafkaStreamsBuilder activePowerTopic(final String activePowerTopic) {
    this.activePowerTopic = activePowerTopic;
    return this;
  }

  public KafkaStreamsBuilder aggrActivePowerTopic(final String aggrActivePowerTopic) {
    this.aggrActivePowerTopic = aggrActivePowerTopic;
    return this;
  }

  public KafkaStreamsBuilder anomaliesTopic(final String anomaliesTopic) {
    this.anomaliesTopic = anomaliesTopic;
    return this;
  }

  public KafkaStreamsBuilder schemaRegistry(final String url) {
    this.schemaRegistryUrl = url;
    return this;
  }

  /**
   * Sets the Kafka Streams property for the number of threads (num.stream.threads). Can be minus
   * one for using the default.
   */
  public KafkaStreamsBuilder numThreads(final int numThreads) {
    if (numThreads < -1 || numThreads == 0) {
      throw new IllegalArgumentException("Number of threads must be greater 0 or -1.");
    }
    this.numThreads = numThreads;
    return this;
  }

  /**
   * Sets the Kafka Streams property for the frequency with which to save the position (offsets in
   * source topics) of tasks (commit.interval.ms). Must be zero for processing all record, for
   * example, when processing bulks of records. Can be minus one for using the default.
   */
  public KafkaStreamsBuilder commitIntervalMs(final int commitIntervalMs) {
    if (commitIntervalMs < -1) {
      throw new IllegalArgumentException("Commit interval must be greater or equal -1.");
    }
    this.commitIntervalMs = commitIntervalMs;
    return this;
  }

  /**
   * Sets the Kafka Streams property for maximum number of memory bytes to be used for record caches
   * across all threads (cache.max.bytes.buffering). Must be zero for processing all record, for
   * example, when processing bulks of records. Can be minus one for using the default.
   */
  public KafkaStreamsBuilder cacheMaxBytesBuffering(final int cacheMaxBytesBuffering) {
    if (cacheMaxBytesBuffering < -1) {
      throw new IllegalArgumentException("Cache max bytes buffering must be greater or equal -1.");
    }
    this.cacheMaxBytesBuff = cacheMaxBytesBuffering;
    return this;
  }

  /**
   * Builds the {@link KafkaStreams} instance.
   */
  public KafkaStreams build() {
    return new KafkaStreams(this.buildTopology(), this.buildProperties());
  }

  private Topology buildTopology() {
    Objects.requireNonNull(this.activePowerTopic,
        "Kafka topic for active power records has not been set.");
    Objects.requireNonNull(this.aggrActivePowerTopic,
        "Kafka topic for aggregated active power records has not been set.");
    Objects.requireNonNull(this.anomaliesTopic,
        "Kafka topic detected anomalies has not been set.");
    Objects.requireNonNull(this.cassandraSession, "Cassandra session has not been set.");
    // TODO log parameters
    final TopologyBuilder topologyBuilder = new TopologyBuilder(
        new Serdes(this.schemaRegistryUrl),
        this.cassandraSession,
        this.activePowerTopic,
        this.aggrActivePowerTopic);
    return topologyBuilder.build();
  }

  private Properties buildProperties() {
    return PropertiesBuilder
        .bootstrapServers(this.bootstrapServers)
        .applicationId(APPLICATION_NAME + '-' + APPLICATION_VERSION) // TODO as parameter
        .set(StreamsConfig.NUM_STREAM_THREADS_CONFIG, this.numThreads, p -> p > 0)
        .set(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, this.commitIntervalMs, p -> p >= 0)
        .set(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, this.cacheMaxBytesBuff, p -> p >= 0)
        .build();
  }

}
