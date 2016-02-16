package fr.ippon.tatami.repository.cassandra;

import com.datastax.driver.core.*;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.utils.UUIDs;
import fr.ippon.tatami.config.ColumnFamilyKeys;
import fr.ippon.tatami.domain.User;
import fr.ippon.tatami.domain.Username;
import fr.ippon.tatami.repository.UsernameRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.validation.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;

/**
 * Cassandra implementation of the user repository.
 *
 * @author Julien Dubois
 */
@Repository
public class CassandraUsernameRepository implements UsernameRepository {

    public static final String TABLE = "username";
    private final Logger log = LoggerFactory.getLogger(CassandraUsernameRepository.class);

    @Inject
    private Session session;

    @Inject
    Environment env;

    private ConsistencyLevel consistencyLevel = ConsistencyLevel.ONE;


    @PostConstruct
    public void init() {
        String stringConsistencyLevel = env.getProperty("cassandra.unique.consistency");
        if (stringConsistencyLevel != null && ConsistencyLevel.valueOf(stringConsistencyLevel) != null) {
            consistencyLevel = ConsistencyLevel.valueOf(stringConsistencyLevel);
        }
        log.debug("Unique consistency level: " + consistencyLevel);
    }

    private void updateActivated(User user, boolean activated) {
        Statement update = QueryBuilder.update("user")
                .with(QueryBuilder.set("activated", activated))
                .where((QueryBuilder.eq("login", user.getLogin())));
        session.execute(update);
    }

    @Override
    public void createUsername(Username user) throws ConstraintViolationException {
        Statement statement = QueryBuilder.insertInto(TABLE)
                .value("username", user.getUsername())
                .value("domain", user.getDomain())
                .value("login",user.getLogin())
                .value("created", UUIDs.timeBased());
        session.execute(statement);

    }


    @Override
    public void deleteUsername(Username user) {
        Statement statement = QueryBuilder.delete().from(TABLE)
                .where(eq("username", user.getUsername()))
                .and(eq("domain", user.getDomain()))
                .and(eq("created", user.getCreated()));
        session.execute(statement);
    }

    @Override
    public List<Username> findUsernamesByDomainAndUsername(String domain, String username) {
        Statement statement = QueryBuilder.select()
                .all()
                .from(TABLE)
                .where(eq("username", username))
                .and(eq("domain", domain))
                .setConsistencyLevel(consistencyLevel);
        ResultSet results = session.execute(statement);
        return results
                .all()
                .stream()
                .map(getUsernameFromRowFunction())
                .collect(Collectors.toList());
    }

    private Function<Row, Username> getUsernameFromRowFunction() {
        return row ->
                new Username(row.getString("username"),
                        row.getString("domain"),
                        row.getString("login"),
                        row.getUUID("created"));
    }
}
