package titan.ccp.anomalydetection.api;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import titan.ccp.common.cassandra.AvroMapper;
import titan.ccp.common.cassandra.DecodeException;
import titan.ccp.model.records.AnomalyPowerRecord;

/**
 * An {@link AnomalyRepository} for the Cassandra data storage.
 */
public class CassandraRepository implements AnomalyRepository {

  private static final Logger LOGGER = LoggerFactory.getLogger(CassandraRepository.class);

  private static final String TABLE_NAME = "anomalies";
  private static final String TIMESTAMP_KEY = "timestamp";
  private static final String IDENTIFIER_KEY = "identifier";

  private final Function<Row, AnomalyPowerRecord> recordFactory =
      new AvroMapper<>(AnomalyPowerRecord::new);
  private final Session cassandraSession;

  /**
   * Create a new {@link CassandraRepository}.
   */
  public CassandraRepository(final Session cassandraSession) {
    this.cassandraSession = cassandraSession;
  }

  @Override
  public List<AnomalyPowerRecord> getAnomalies(final String identifier, final long from,
      final long to) {
    final Statement statement = QueryBuilder.select().all() // NOPMD
        .from(TABLE_NAME)
        .where(QueryBuilder.eq(IDENTIFIER_KEY, identifier))
        .and(QueryBuilder.gte(TIMESTAMP_KEY, from))
        .and(QueryBuilder.lte(TIMESTAMP_KEY, to));

    final ResultSet resultSet = this.cassandraSession.execute(statement); // NOPMD

    final List<AnomalyPowerRecord> records = new ArrayList<>();
    for (final Row row : resultSet) {
      try {
        final AnomalyPowerRecord record = this.recordFactory.apply(row);
        records.add(record);
      } catch (final DecodeException e) {
        LOGGER.error("Cannot create object from cassandra row.", e);
      }
    }
    return records;
  }

}
