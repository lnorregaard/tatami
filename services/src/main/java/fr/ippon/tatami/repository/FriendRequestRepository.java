package fr.ippon.tatami.repository;

import java.util.List;

/**
 * The Friend Repository.
 *
 * @author Julien Dubois
 */
public interface FriendRequestRepository {

    boolean getFriendRequest(String login, String friendLogin);

    void removeFriendRequest(String login, String friendLogin);

    boolean addFriendRequest(String currentUserLogin, String followedUserLogin);
}
