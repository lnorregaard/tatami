package fr.ippon.tatami.repository;

import fr.ippon.tatami.domain.StatusReplyUser;

import java.util.List;
import java.util.UUID;

/**
 * The Reply Counter Repository.
 *
 * @author Lars Nørregaard
 */
public interface StatusReplyUserRepository {
    void updateReplyUser(UUID statusId, String username);
    void deleteReplyUser(UUID statusId);

    List<StatusReplyUser> getUsersForList(List<UUID> uuids);
}
