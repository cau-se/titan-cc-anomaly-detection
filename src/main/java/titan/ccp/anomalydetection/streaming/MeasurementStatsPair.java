package titan.ccp.anomalydetection.streaming;

import java.util.Objects;
import titan.ccp.model.records.ActivePowerRecord;
import titan.ccp.model.records.HourOfWeekActivePowerRecord;

/**
 * Pair of a {@link ActivePowerRecord} measurement and {@link HourOfWeekActivePowerRecord}
 * statistics.
 */
public class MeasurementStatsPair {

  private final ActivePowerRecord measurement;
  private final HourOfWeekActivePowerRecord stats;

  public MeasurementStatsPair(final ActivePowerRecord measurement,
      final HourOfWeekActivePowerRecord stats) {
    this.measurement = measurement;
    this.stats = stats;
  }

  public ActivePowerRecord getMeasurement() {
    return this.measurement;
  }

  public HourOfWeekActivePowerRecord getStats() {
    return this.stats;
  }

  @Override
  public String toString() {
    return '{' + this.measurement.toString() + ',' + this.stats.toString() + '}';
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.measurement, this.stats);
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj instanceof MeasurementStatsPair) {
      final MeasurementStatsPair other = (MeasurementStatsPair) obj;
      return Objects.equals(this.measurement, other.measurement)
          && Objects.equals(this.stats, other.stats);
    }
    return false;
  }

}
