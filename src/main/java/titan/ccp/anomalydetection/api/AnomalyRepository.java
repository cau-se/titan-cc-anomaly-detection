package titan.ccp.anomalydetection.api;

import java.util.List;
import titan.ccp.model.records.AnomalyPowerRecord;

/**
 * An interface for encapsulating the data storage from queries to it.
 */
public interface AnomalyRepository {


  /**
   * Get all records for the provided identifier in the provided time interval.
   */
  List<AnomalyPowerRecord> getAnomalies(String identifier, long from, long to);

}
