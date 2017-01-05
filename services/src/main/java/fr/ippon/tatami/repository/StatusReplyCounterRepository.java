package fr.ippon.tatami.repository;

import fr.ippon.tatami.domain.StatusReplyCount;

import java.util.List;
import java.util.UUID;

/**
 * The Reply Counter Repository.
 *
 * @author Lars Nørregaard
 */
public interface StatusReplyCounterRepository {
    void incrementReplyCounter(UUID statusId);
    void decrementReplyCounter(UUID statusId);

    List<StatusReplyCount> getCountForList(List<UUID> uuids);
}
