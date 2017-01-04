package fr.ippon.tatami.repository;

import java.util.UUID;

/**
 * The Reply Counter Repository.
 *
 * @author Lars Nørregaard
 */
public interface StatusReplyUserRepository {
    void updateReplyUser(UUID statusId, String username);
    void deleteReplyUser(UUID statusId);
}
