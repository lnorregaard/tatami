package fr.ippon.tatami.repository.cassandra;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import fr.ippon.tatami.repository.FriendRequestRepository;
import org.springframework.stereotype.Repository;

import javax.inject.Inject;
import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;

/**
 * Cassandra implementation of the Friend Request repository.
 * <p/>
 * Structure :
 * - Key = login
 * - Name = friend login
 *
 * @author Lars Nørregaard
 */
@Repository
public class CassandraFriendRequestRepository implements FriendRequestRepository {

    @Inject
    private Session session;

    @Override
    public UUID getStatusIdForFriendRequest(String login, String friendLogin) {
        Statement statement = QueryBuilder.select()
                .column("statusId")
                .from("friendRequests")
                .where(eq("login", login))
                .and(eq("friendLogin",friendLogin));
        ResultSet results = session.execute(statement);
        if (!results.isExhausted()) {
            return results.one().getUUID("statusId");
        } else {
            return null;
        }
    }
    @Override
    public boolean getFriendRequest(String login, String friendLogin) {
        Statement statement = QueryBuilder.select()
                .column("friendLogin")
                .from("friendRequests")
                .where(eq("login", login))
                .and(eq("friendLogin",friendLogin));
        ResultSet results = session.execute(statement);
        return !results
                .isExhausted();
    }

    @Override
    public void removeFriendRequest(String login, String friendLogin) {
        Statement statement = QueryBuilder.delete().from("friendRequests")
                .where(eq("login", login))
                .and(eq("friendLogin", friendLogin));
        session.execute(statement);
    }

    @Override
    public boolean addFriendRequest(String currentUserLogin, String followedUserLogin, UUID statusId) {
        Statement statement = QueryBuilder.insertInto("friendRequests")
                .value("login", currentUserLogin)
                .value("friendLogin", followedUserLogin)
                .value("statusId",statusId);
        session.execute(statement);
        return true;
    }

    @Override
    public Collection<String> findFriendRequests(String username) {
        Statement statement = QueryBuilder.select()
                .column("login")
                .from("friendRequests")
                .where(eq("friendLogin", username));
        ResultSet results = session.execute(statement);
        return results
                .all()
                .stream()
                .map(e -> e.getString("login"))
                .collect(Collectors.toList());
    }

    @Override
    public Collection<String> findLoginsFriendRequests(String login) {
        Statement statement = QueryBuilder.select()
                .column("friendLogin")
                .from("friendRequests")
                .where(eq("login", login));
        ResultSet results = session.execute(statement);
        return results
                .all()
                .stream()
                .map(e -> e.getString("friendLogin"))
                .collect(Collectors.toList());
    }

}
