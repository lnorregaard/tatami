package fr.ippon.tatami.repository;

import fr.ippon.tatami.domain.User;
import fr.ippon.tatami.domain.Username;

import javax.validation.ConstraintViolationException;
import java.util.List;

/**
 * The Username Repository.
 *
 * @author Lars Nørregaard
 */
public interface UsernameRepository {

    void createUsername(Username user) throws ConstraintViolationException;

    void deleteUsername(Username user);

    List<Username> findUsernamesByDomainAndUsername(String domain, String username);

}
