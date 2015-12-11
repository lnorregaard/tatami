package fr.ippon.tatami.repository;

import fr.ippon.tatami.domain.User;

/**
 * Created by lnorregaard on 08/12/15.
 */
public interface AuditRepository {
    void blockStatus(String moderator, String statusId, String username, String comment);

    void blockUser(String moderator, String username, String comment);
}
