package titan.ccp.anomalydetection.streaming;

import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.common.serialization.Serde;
import titan.ccp.common.kafka.avro.SchemaRegistryAvroSerdeFactory;

final class Serdes {

  private final SchemaRegistryAvroSerdeFactory avroSerdeFactory;

  public Serdes(final String schemaRegistryUrl) {
    this.avroSerdeFactory = new SchemaRegistryAvroSerdeFactory(schemaRegistryUrl);
  }

  public Serde<String> string() {
    return org.apache.kafka.common.serialization.Serdes.String();
  }

  public <T extends SpecificRecord> Serde<T> avroValues() {
    return this.avroSerdeFactory.forValues();
  }

}
