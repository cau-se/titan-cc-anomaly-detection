package titan.ccp.anomalydetection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A microservice for detecting anomalies.
 *
 */
public class AnomalyDetectionService {

  private static final Logger LOGGER = LoggerFactory.getLogger(AnomalyDetectionService.class);

  // private final Configuration config = ServiceConfigurations.createWithDefaults();

  /**
   * Start the service.
   */
  public void run() {
    LOGGER.info("Starting Titan Control Center Anomaly Detection microservice.");
    // Do nothing so far
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
