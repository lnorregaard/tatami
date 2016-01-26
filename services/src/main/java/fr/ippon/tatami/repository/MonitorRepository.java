package fr.ippon.tatami.repository;

import fr.ippon.tatami.domain.Ping;
import fr.ippon.tatami.domain.User;

import javax.validation.ConstraintViolationException;

/**
 * The User Repository.
 *
 * @author Julien Dubois
 */
public interface MonitorRepository {

    Ping createCassandraPing(Ping ping);

}
