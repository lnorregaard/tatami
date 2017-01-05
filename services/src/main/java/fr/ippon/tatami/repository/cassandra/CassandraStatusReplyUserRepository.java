package fr.ippon.tatami.repository.cassandra;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import fr.ippon.tatami.domain.StatusReplyCount;
import fr.ippon.tatami.domain.StatusReplyUser;
import fr.ippon.tatami.repository.StatusReplyUserRepository;
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
public class CassandraStatusReplyUserRepository implements StatusReplyUserRepository {

    public static final String STATUS_USER = "statusreplyuser";
    public static final String STATUS_ID = "statusId";
    @Inject
    Session session;

    private static final String USERNAME = "username";

    @Override
    public void updateReplyUser(UUID statusId, String username) {
        Statement statement = QueryBuilder.update(STATUS_USER)
                .with(set(USERNAME,username))
                .where(eq(STATUS_ID,statusId));
        session.execute(statement);
    }

    @Override
    public void deleteReplyUser(UUID statusId) {
        Statement statement = QueryBuilder.delete()
                .from(STATUS_USER)
                .where(eq(STATUS_ID,statusId));
        session.execute(statement);
    }

    @Override
    public List<StatusReplyUser> getUsersForList(List<UUID> uuids) {
        Select select = QueryBuilder.select()
                .column(STATUS_ID)
                .column(USERNAME)
                .from(STATUS_USER);
        Select.Where where = null;
        where = select.where(in(STATUS_ID, uuids));
        Statement statement = where;
        ResultSet results = session.execute(statement);
        return results
                .all()
                .stream()
                .map(e -> new StatusReplyUser(e.getUUID(STATUS_ID),e.getString(USERNAME)))
                .collect(Collectors.toList());
    }
}
