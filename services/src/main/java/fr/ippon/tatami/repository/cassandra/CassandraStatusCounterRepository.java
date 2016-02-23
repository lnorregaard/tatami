package fr.ippon.tatami.repository.cassandra;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import fr.ippon.tatami.repository.StatusCounterRepository;
import org.springframework.stereotype.Repository;

import javax.inject.Inject;

import java.util.UUID;

import static com.datastax.driver.core.querybuilder.QueryBuilder.*;


/**
 * Cassandra implementation of the Counter repository.
 * <p/>
 * Structure :
 * - Key = login
 * - Name = counterId
 * - Value = count
 *
 * @author Julien Dubois
 */
@Repository
public class CassandraStatusCounterRepository implements StatusCounterRepository {

    public static final String STATUS_COUNTER = "statusCounter";
    public static final String STATUS_ID = "statusId";
    @Inject
    Session session;

    private static final String LIKE_COUNTER = "LIKE_COUNTER";

    @Override
    public void incrementLikeCounter(UUID statusId) {
        incrementCounter(LIKE_COUNTER, statusId);
    }


    @Override
    public void decrementLikeCounter(UUID statusId) {
        decrementCounter(LIKE_COUNTER, statusId);
    }

    @Override
    public void createLikeCounter(UUID statusId) {
        createLikeCounter(LIKE_COUNTER, statusId);
    }

    @Override
    public long getLikeCounter(UUID statusId) {
        return getCounter(LIKE_COUNTER, statusId);
    }


    @Override
    public void deleteCounters(UUID statusId) {
        Statement statement = QueryBuilder.delete().from(STATUS_COUNTER)
                .where(eq(STATUS_ID, statusId));
        session.execute(statement);
    }

    private void createLikeCounter(String counterName, UUID statusId) {
        Statement statement = QueryBuilder.update(STATUS_COUNTER)
                .with(incr(counterName,0))
                .where(eq(STATUS_ID,statusId));
        session.execute(statement);
    }

    private void incrementCounter(String counterName, UUID statusId) {
        Statement statement = QueryBuilder.update(STATUS_COUNTER)
                .with(incr(counterName,1))
                .where(eq(STATUS_ID,statusId));
        session.execute(statement);
    }

    private void decrementCounter(String counterName, UUID statusId) {
        Statement statement = QueryBuilder.update(STATUS_COUNTER)
                .with(decr(counterName,1))
                .where(eq(STATUS_ID,statusId));
        session.execute(statement);
    }

    private long getCounter(String counterName, UUID statusId) {
        Statement statement = QueryBuilder.select()
                .column(counterName)
                .from(STATUS_COUNTER)
                .where(eq(STATUS_ID, statusId));
        ResultSet results = session.execute(statement);
        Row row = results.one();
        if (row != null) {
            return row.getLong(counterName);
        } else {
            return 0;
        }
    }
}
