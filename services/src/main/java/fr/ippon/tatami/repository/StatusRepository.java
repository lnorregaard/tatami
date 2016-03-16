package fr.ippon.tatami.repository;

import fr.ippon.tatami.domain.Group;
import fr.ippon.tatami.domain.User;
import fr.ippon.tatami.domain.status.*;

import javax.validation.ConstraintViolationException;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * The Status Repository.
 *
 * @author Julien Dubois
 */
public interface StatusRepository {

    Status createStatus(String login,
                        boolean statusPrivate,
                        Group group,
                        Collection<String> attachmentIds,
                        String content,
                        String discussionId,
                        String replyTo,
                        String replyToUsername,
                        String geoLocalization, boolean admin) throws ConstraintViolationException;

    Share createShare(String login,
                      String originalStatusId);

    Announcement createAnnouncement(String login,
                                    String originalStatusId);

    MentionFriend createMentionFriend(String login,
                                      String followerLogin);

    FavoriteShare createFavoriteShare(String login,
                                      String followerLogin,
                                      UUID originalStatusId, String username);

    FriendRequest createFriendRequest(String login,
                                      String followerLogin, String username);

    void acceptFriendRequest(String statusId);

    void rejectFriendRequest(String statusId);

    MentionShare createMentionShare(String login,
                                    String originalStatusId);

    void removeStatus(AbstractStatus status);

    /**
     * Retrieve a persisted status.
     *
     * @return null if status was removed
     */
    AbstractStatus findStatusById(String statusId);

    List<String> findStatusByStates(String types, String groupId, Integer count);

    AbstractStatus findStatusById(String statusId, boolean excludeStates);

    void updateState(String statusId, String state);

    List<String> findStatusByUser(User user);

    AbstractStatus findStatusByIdDeletedUser(String statusId);

    void removeStatus(List<String> statuses);
}
