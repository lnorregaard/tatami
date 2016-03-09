package fr.ippon.tatami.repository.cassandra;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.core.utils.UUIDs;
import fr.ippon.tatami.domain.User;
import fr.ippon.tatami.repository.AuditRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import javax.inject.Inject;

import java.util.UUID;
import java.util.stream.Collectors;

import static com.datastax.driver.core.querybuilder.QueryBuilder.*;
import static com.datastax.driver.core.querybuilder.QueryBuilder.desc;

/**
 * Created by lnorregaard on 08/12/15.
 */
@Repository
public class CassandraAuditRepository implements AuditRepository {
    private final Logger log = LoggerFactory.getLogger(CassandraAuditRepository.class);

    private static final String ID = "id";
    private static final String MODERATOR = "moderator";
    private static final String TYPE = "type";
    private static final String BLOCKED_ID = "blockedId";
    private static final String USERNAME = "username";
    private static final String COMMENT = "comment";

    private static final String TABLE = "audit";

    private static final String STATUS = "STATUS";
    private static final String USER = "USER";

    private final static int COLUMN_TTL = 60 * 60 * 24 * 90; // The column is stored for 90 days.

    @Inject
    private Session session;

    @Override
    public void blockStatus(String moderator, String statusId, String username, String comment) {
        log.info("Creating audit for status : {} username {} moderator {} comment: {}", statusId,username,moderator,comment);
        Statement statement = QueryBuilder.insertInto(TABLE)
                .value(USERNAME,username)
                .value(ID, UUIDs.timeBased())
                .value(TYPE,STATUS)
                .value(BLOCKED_ID,statusId)
                .value(MODERATOR,moderator)
                .value(COMMENT, comment)
                .using(ttl(COLUMN_TTL));
        session.execute(statement);
    }



    @Override
    public void blockUser(String moderator, String username, String comment) {
        log.info("Creating audit for user : {} moderator {} comment: {}", username,moderator,comment);
        Statement statement = QueryBuilder.insertInto(TABLE)
                .value(USERNAME,username)
                .value(ID, UUIDs.timeBased())
                .value(TYPE,USER)
                .value(BLOCKED_ID,username)
                .value(MODERATOR,moderator)
                .value(COMMENT,comment)
                .using(ttl(COLUMN_TTL));
        session.execute(statement);
    }

    @Override
    public String getCommentForStatus(String statusId) {
        Select.Where where = QueryBuilder.select()
                .column("comment")
                .from(TABLE)
                .where(eq(BLOCKED_ID, statusId));
        Statement statement = where;
        ResultSet results = session.execute(statement);
        if (results.isExhausted()) {
            return "";
        }
        return results
                .one()
                .getString(COMMENT);

    }

}
