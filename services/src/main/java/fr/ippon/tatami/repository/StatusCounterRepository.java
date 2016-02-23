package fr.ippon.tatami.repository;

import java.util.UUID;

/**
 * The Counter Repository.
 *
 * @author Julien Dubois
 */
public interface StatusCounterRepository {

    void incrementLikeCounter(UUID statusId);

    void decrementLikeCounter(UUID statusId);

    long getLikeCounter(UUID statusId);

    void createLikeCounter(UUID statusId);

    void deleteCounters(UUID statusId);
}
