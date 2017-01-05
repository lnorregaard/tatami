package fr.ippon.tatami.repository.cassandra;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import fr.ippon.tatami.repository.StatusRepliesRepository;
import org.springframework.stereotype.Repository;

import javax.inject.Inject;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.datastax.driver.core.querybuilder.QueryBuilder.*;
import static com.datastax.driver.core.querybuilder.QueryBuilder.desc;


/**
 *
 * @author Lars Nørregaard
 */
@Repository
public class CassandraStatusRepliesRepository implements StatusRepliesRepository {

    public static final String STATUS_REPLIES = "statusreplies";
    public static final String ORIGINAL_STATUS_ID = "statusId";
    @Inject
    Session session;

    private static final String REPLY_ID = "replyid";


    @Override
    public void insertReply(UUID originalStatusId, UUID replyStatusId) {
        Statement statement = QueryBuilder.insertInto(STATUS_REPLIES)
                .value(ORIGINAL_STATUS_ID,originalStatusId)
                .value(REPLY_ID,replyStatusId);
        session.execute(statement);

    }

    @Override
    public void deleteReply(UUID originalStatusId, UUID replyStatusId) {
        Statement statement = QueryBuilder.delete()
                .from(STATUS_REPLIES)
                .where(eq(ORIGINAL_STATUS_ID,originalStatusId))
                .and(eq(REPLY_ID,replyStatusId));
        session.execute(statement);
    }

    @Override
    public List<String> getReplies(UUID originalStatusId, int size, String start, String finish, boolean desc) {
        Select.Where where = QueryBuilder.select()
                .column(REPLY_ID)
                .from(STATUS_REPLIES)
                .where(eq(ORIGINAL_STATUS_ID, originalStatusId));
        if(finish != null) {
            where.and(lt(REPLY_ID, UUID.fromString(finish)));
        } else if(start != null) {
            where.and(gt(REPLY_ID,UUID.fromString(start)));
        }
        if (size > 0) {
            where.limit(size);
        }
        if (desc) {
            where.orderBy(desc(REPLY_ID));
        }
        Statement statement = where;
        ResultSet results = session.execute(statement);
        return results
                .all()
                .stream()
                .map(e -> e.getUUID(REPLY_ID).toString())
                .collect(Collectors.toList());
    }
}
