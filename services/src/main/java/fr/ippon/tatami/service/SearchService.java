package fr.ippon.tatami.service;

import fr.ippon.tatami.domain.Group;
import fr.ippon.tatami.domain.User;
import fr.ippon.tatami.domain.status.Status;
import fr.ippon.tatami.service.dto.UserFavouriteCountDTO;
import org.springframework.scheduling.annotation.Async;

import java.util.Collection;
import java.util.List;

/**
 * Service used to search statuses and users.
 */
public interface SearchService {

    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int DEFAULT_TOP_N_SEARCH_USER = 8;

    /**
     * Reset the search engine.
     * <p/>
     * This is used to do a full reindexation of all the data.
     *
     * @return if the reset was completed OK
     */
    boolean reset();

    /**
     * Add a status to the index.
     *
     * @param status the status to add : can't be null
     */
    void addStatus(Status status);

    void addStatuses(Collection<Status> statuses);

    /**
     * Delete a status from the index.
     *
     * @param status the status to delete
     */
    void removeStatus(Status status);

    /**
     * Search an item in the index.
     *
     * @param query the query : mandatory
     * @param page  the page to return
     * @param size  the size of a page
     */
    List<String> searchStatus(String domain,
                              String query,
                              int page,
                              int size);


    /**
     * Add a user to the index.
     *
     * @param user the user to add : can't be null
     */
    void addUser(User user);

    void addUsers(Collection<User> users);

    void removeUser(User user);

    Collection<String> searchUserByPrefix(String domain,
                                          String prefix);
    Collection<String> searchByUsername(String domain,
                                          String prefix, int size);

    void addGroup(Group group);

    void removeGroup(Group group);

    Collection<Group> searchGroupByPrefix(String domain, String prefix, int size);


    Collection<String> searchUserByUsernameAndFirstnameAndLastname(String domain, String username, String firstname, String lastname, boolean exact, boolean all);

    @Async
    void addFirstName(String firstname);

    void addFirstnames(Collection<String> firstnames);

    void removeFirstname(String firstname);

    Collection<String> searchFirstName(String firstname, int limit);

    List<UserFavouriteCountDTO> countUsersForUserFavourites(List<String> favourites, User user);

    @Async
    void indexUserFavourite(String favourite, String login);

    void removeUserFavourite(String favourite, String login);

    List<String> getFriendsForUserFavourite(String id, User user, int from, int size);

    Collection<String> getUserFavouritesForUser(String username, String domain);
}
