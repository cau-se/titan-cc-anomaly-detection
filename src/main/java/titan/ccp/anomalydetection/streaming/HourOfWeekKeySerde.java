package titan.ccp.anomalydetection.streaming;

import java.time.DayOfWeek;
import titan.ccp.common.kafka.simpleserdes.BufferSerde;
import titan.ccp.common.kafka.simpleserdes.ReadBuffer;
import titan.ccp.common.kafka.simpleserdes.WriteBuffer;

/**
 * {@link BufferSerde} for a {@link HourOfWeekKey}.
 */
public class HourOfWeekKeySerde implements BufferSerde<HourOfWeekKey> {

  @Override
  public void serialize(final WriteBuffer buffer, final HourOfWeekKey data) {
    buffer.putInt(data.getDayOfWeek().getValue());
    buffer.putInt(data.getHourOfDay());
    buffer.putString(data.getSensorId());
  }

  @Override
  public HourOfWeekKey deserialize(final ReadBuffer buffer) {
    final DayOfWeek dayOfWeek = DayOfWeek.of(buffer.getInt());
    final int hourOfDay = buffer.getInt();
    final String sensorId = buffer.getString();
    return new HourOfWeekKey(dayOfWeek, hourOfDay, sensorId);
  }

}
