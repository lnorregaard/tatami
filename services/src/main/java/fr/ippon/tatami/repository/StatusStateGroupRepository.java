package fr.ippon.tatami.repository;

import javax.validation.ConstraintViolationException;
import java.util.List;
import java.util.UUID;

/**
 * The Moderated status Repository.
 *
 * @author Lars Nørregaard
 */
public interface StatusStateGroupRepository {

    void createStatusStateGroup(UUID statusId,
                                  String state,
                                  String groupId) throws ConstraintViolationException;

    void updateState(String groupId, UUID statusId, String newState);

    List<UUID> findStatuses(String state, String groupId, UUID from, UUID to, int count, String order);

    Long findStatusesCount(String types, String groupId);

    void removeState(String groupId, UUID statusId);
}
