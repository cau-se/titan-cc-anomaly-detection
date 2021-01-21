package titan.ccp.anomalydetection.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Service;

/**
 * Contains a web server for accessing stored anomalies via a REST interface.
 */
public class RestApiServer {

  private static final String FROM_QUERY_PARAM = "from";
  private static final String TO_QUERY_PARAM = "to";

  private static final Logger LOGGER = LoggerFactory.getLogger(RestApiServer.class);

  private final Gson gson = new GsonBuilder().create();

  private final AnomalyRepository repository;

  private final Service webService;

  private final boolean enableCors;
  private final boolean enableGzip;

  /**
   * Creates a new API server using the passed parameters.
   */
  public RestApiServer(final AnomalyRepository repository, final int port, final boolean enableCors,
      final boolean enableGzip) {
    this.repository = repository;
    LOGGER.info("Instantiate API server.");
    this.webService = Service.ignite().port(port);
    this.enableCors = enableCors;
    this.enableGzip = enableGzip;
  }

  /**
   * Start the web server by setting up the API routes.
   */
  public void start() { // NOPMD NOCS declaration of routes
    LOGGER.info("Instantiate API routes.");

    this.webService.get("/anomalies/:identifier", (request, response) -> {
      final String identifier = request.params("identifier");
      final long from =
          this.parseLongOrDefault(request.queryParams(FROM_QUERY_PARAM), Long.MIN_VALUE);
      final long to = this.parseLongOrDefault(request.queryParams(TO_QUERY_PARAM), Long.MAX_VALUE);

      return this.repository.getAnomalies(identifier, from, to)
          .stream()
          .filter(a -> Double.isFinite(a.getAnomalyScore()))
          .collect(Collectors.toList());
    }, this.gson::toJson);

    if (this.enableCors) {
      this.webService.options("/*", (request, response) -> {

        final String accessControlRequestHeaders =
            request.headers("Access-Control-Request-Headers");
        if (accessControlRequestHeaders != null) {
          response.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
        }

        final String accessControlRequestMethod = request.headers("Access-Control-Request-Method");
        if (accessControlRequestMethod != null) {
          response.header("Access-Control-Allow-Methods", accessControlRequestMethod);
        }

        return "OK";
      });

      this.webService.before((request, response) -> {
        response.header("Access-Control-Allow-Origin", "*");
      });
    }

    this.webService.after((request, response) -> {
      response.type("application/json");
      if (this.enableGzip) {
        response.header("Content-Encoding", "gzip");
      }
    });
  }

  /**
   * Stop the webserver.
   */
  public void stop() {
    this.webService.stop();
  }

  private long parseLongOrDefault(final String param, final long defaultValue) {
    if (param == null) {
      return defaultValue;
    }
    return Long.parseLong(param);
  }

}
