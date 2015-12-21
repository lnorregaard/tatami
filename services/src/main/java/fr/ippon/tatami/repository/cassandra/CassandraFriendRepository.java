package fr.ippon.tatami.repository.cassandra;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import fr.ippon.tatami.repository.FriendRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static fr.ippon.tatami.config.ColumnFamilyKeys.FRIENDS;

/**
 * Cassandra implementation of the Friend repository.
 * <p/>
 * Structure :
 * - Key = login
 * - Name = friend login
 * - Value = time
 *
 * @author Julien Dubois
 */
@Repository
public class CassandraFriendRepository extends AbstractCassandraFriendRepository implements FriendRepository {

    @Override
    @CacheEvict(value = "friends-cache", key = "#login")
    public void addFriend(String login, String friendLogin) {
        super.addFriend(login, friendLogin);
    }

    @Override
    @CacheEvict(value = "friends-cache", key = "#login")
    public void removeFriend(String login, String friendLogin) {
        super.removeFriend(login, friendLogin);
    }

    @Override
    @Cacheable("friends-cache")
    public List<String> findFriendsForUser(String login) {
        return super.findFriends(login);
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
    public boolean addFriendRequest(String currentUserLogin, String followedUserLogin) {
        Statement statement = QueryBuilder.insertInto("friendRequests")
                .value("login", currentUserLogin)
                .value("friendLogin", followedUserLogin);
        session.execute(statement);
        return true;
    }

    @Override
    public String getFriendsTable() {
        return FRIENDS;
    }
}
