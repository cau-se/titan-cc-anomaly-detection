package titan.ccp.anomalydetection.streaming;

import java.time.DayOfWeek;
import java.util.Objects;

/**
 * Composed key of a {@link DayOfWeek}, an hour of the day and a sensor id.
 */
public class HourOfWeekKey {

  private final DayOfWeek dayOfWeek;
  private final int hourOfDay;
  private final String sensorId;

  /**
   * Create a new {@link HourOfDayKey} using its components.
   */
  public HourOfWeekKey(final DayOfWeek dayOfWeek, final int hourOfDay, final String sensorId) {
    this.dayOfWeek = dayOfWeek;
    this.hourOfDay = hourOfDay;
    this.sensorId = sensorId;
  }

  public DayOfWeek getDayOfWeek() {
    return this.dayOfWeek;
  }

  public int getHourOfDay() {
    return this.hourOfDay;
  }

  public String getSensorId() {
    return this.sensorId;
  }

  @Override
  public String toString() {
    return this.sensorId + ";" + this.dayOfWeek.toString() + ";" + this.hourOfDay;
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.dayOfWeek, this.hourOfDay, this.sensorId);
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj instanceof HourOfWeekKey) {
      final HourOfWeekKey other = (HourOfWeekKey) obj;
      return Objects.equals(this.dayOfWeek, other.dayOfWeek)
          && Objects.equals(this.hourOfDay, other.hourOfDay)
          && Objects.equals(this.sensorId, other.sensorId);
    }
    return false;
  }

}
