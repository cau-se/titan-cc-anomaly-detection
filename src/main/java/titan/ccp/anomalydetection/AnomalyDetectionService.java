package titan.ccp.anomalydetection;

import org.apache.commons.configuration2.Configuration;
import org.apache.kafka.streams.KafkaStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import titan.ccp.anomalydetection.api.CassandraRepository;
import titan.ccp.anomalydetection.api.RestApiServer;
import titan.ccp.anomalydetection.streaming.KafkaStreamsBuilder;
import titan.ccp.common.cassandra.SessionBuilder;
import titan.ccp.common.cassandra.SessionBuilder.ClusterSession;
import titan.ccp.common.configuration.ServiceConfigurations;

/**
 * A microservice for detecting anomalies.
 *
 */
public class AnomalyDetectionService {

  private static final Logger LOGGER = LoggerFactory.getLogger(AnomalyDetectionService.class);

  private final Configuration config = ServiceConfigurations.createWithDefaults();

  /**
   * Start the service.
   */
  public void run() {
    LOGGER.info("Starting Titan Control Center Anomaly Detection microservice.");

    final ClusterSession clusterSession = new SessionBuilder()
        .contactPoint(this.config.getString(ConfigurationKeys.CASSANDRA_HOST))
        .port(this.config.getInt(ConfigurationKeys.CASSANDRA_PORT))
        .keyspace(this.config.getString(ConfigurationKeys.CASSANDRA_KEYSPACE))
        .timeoutInMillis(this.config.getInt(ConfigurationKeys.CASSANDRA_INIT_TIMEOUT_MS))
        .build();

    final KafkaStreams kafkaStreams = new KafkaStreamsBuilder()
        .cassandraSession(clusterSession.getSession())
        .bootstrapServers(this.config.getString(ConfigurationKeys.KAFKA_BOOTSTRAP_SERVERS))
        .activePowerTopic(this.config.getString(ConfigurationKeys.KAFKA_TOPIC_ACTIVE_POWER))
        .aggrActivePowerTopic(
            this.config.getString(ConfigurationKeys.KAFKA_TOPIC_AGGR_ACTIVE_POWER))
        .anomaliesTopic(this.config.getString(ConfigurationKeys.KAFKA_TOPIC_ANOMALIES))
        .schemaRegistry(this.config.getString(ConfigurationKeys.SCHEMA_REGISTRY_URL))
        .build();
    kafkaStreams.start();

    if (this.config.getBoolean(ConfigurationKeys.WEBSERVER_ENABLE)) {
      final RestApiServer restApiServer = new RestApiServer(
          new CassandraRepository(clusterSession.getSession()),
          this.config.getInt(ConfigurationKeys.WEBSERVER_PORT),
          this.config.getBoolean(ConfigurationKeys.WEBSERVER_CORS),
          this.config.getBoolean(ConfigurationKeys.WEBSERVER_GZIP));
      restApiServer.start();
    }

  }

  /**
   * Stop the service.
   */
  public void stop() {
    // Do nothing so far
    LOGGER.info("Stopping Titan Control Center Anomaly Detection microservice.");
  }

  public static void main(final String[] args) {
    new AnomalyDetectionService().run();
  }

}
