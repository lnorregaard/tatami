package fr.ippon.tatami.repository.cassandra;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import fr.ippon.tatami.domain.StatusReplyCount;
import fr.ippon.tatami.repository.StatusCounterRepository;
import fr.ippon.tatami.repository.StatusReplyCounterRepository;
import org.springframework.stereotype.Repository;

import javax.inject.Inject;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
public class CassandraStatusReplyCounterRepository implements StatusReplyCounterRepository {

    public static final String STATUS_COUNTER = "statusreplycounter";
    public static final String STATUS_ID = "statusId";
    @Inject
    Session session;

    private static final String TOTAL = "total";

    @Override
    public void incrementReplyCounter(UUID statusId) {
        Statement statement = QueryBuilder.update(STATUS_COUNTER)
                .with(incr(TOTAL,1))
                .where(eq(STATUS_ID,statusId));
        session.execute(statement);
    }

    @Override
    public void decrementReplyCounter(UUID statusId) {
        Statement statement = QueryBuilder.update(STATUS_COUNTER)
                .with(decr(TOTAL,1))
                .where(eq(STATUS_ID,statusId));
        session.execute(statement);
    }

    @Override
    public List<StatusReplyCount> getCountForList(List<UUID> uuids) {
        Select select = QueryBuilder.select()
                .column(STATUS_ID)
                .column(TOTAL)
                .from(STATUS_COUNTER);
        Select.Where where = null;
        where = select.where(in(STATUS_ID, uuids));
        Statement statement = where;
        ResultSet results = session.execute(statement);
        return results
                .all()
                .stream()
                .map(e -> new StatusReplyCount(e.getUUID(STATUS_ID),e.getLong(TOTAL)))
                .collect(Collectors.toList());
    }


}
