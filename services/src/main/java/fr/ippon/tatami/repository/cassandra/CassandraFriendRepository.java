package fr.ippon.tatami.repository.cassandra;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import fr.ippon.tatami.config.Constants;
import fr.ippon.tatami.repository.FriendRepository;
import fr.ippon.tatami.service.UserService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

import javax.inject.Inject;
import java.util.ArrayList;
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

    @Inject
    private UserService userService;

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
        if (Constants.USER_AND_FRIENDS && userService.isAdmin(login)) {
            return new ArrayList<>();
        } else {
            return super.findFriends(login);
        }
    }

    @Override
    public String getFriendsTable() {
        return FRIENDS;
    }
}
