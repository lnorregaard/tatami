package fr.ippon.tatami.repository;

import java.util.UUID;

/**
 * The Reply Counter Repository.
 *
 * @author Lars Nørregaard
 */
public interface StatusReplyCounterRepository {
    void incrementReplyCounter(UUID statusId);
    void decrementReplyCounter(UUID statusId);
}
