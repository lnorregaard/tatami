package fr.ippon.tatami.repository;

import java.util.Collection;
import java.util.UUID;

/**
 * The Friend Repository.
 *
 * @author Julien Dubois
 */
public interface FriendRequestRepository {

    UUID getStatusIdForFriendRequest(String login, String friendLogin);
    boolean getFriendRequest(String login, String friendLogin);

    void removeFriendRequest(String login, String friendLogin);

    boolean addFriendRequest(String currentUserLogin, String followedUserLogin, UUID statusId);

    Collection<String> findFriendRequests(String username);

    Collection<String> findLoginsFriendRequests(String login);
}
