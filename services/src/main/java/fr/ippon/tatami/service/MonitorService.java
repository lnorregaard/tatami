package fr.ippon.tatami.service;

import fr.ippon.tatami.config.Constants;
import fr.ippon.tatami.domain.DigestType;
import fr.ippon.tatami.domain.Ping;
import fr.ippon.tatami.domain.User;
import fr.ippon.tatami.repository.*;
import fr.ippon.tatami.security.AuthenticationService;
import fr.ippon.tatami.service.dto.UserDTO;
import fr.ippon.tatami.service.util.DomainUtil;
import fr.ippon.tatami.service.util.RandomUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.core.env.Environment;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.crypto.password.StandardPasswordEncoder;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.validation.ConstraintViolationException;
import java.util.*;

/**
 * Manages the application's users.
 *
 * @author Julien Dubois
 */
@Service
public class MonitorService {

    private final Logger log = LoggerFactory.getLogger(MonitorService.class);

    @Inject
    private MonitorRepository monitorRepository;

    @Inject
    private SearchService searchService;


    @Inject
    Environment env;


    public Ping ping() {
        Ping ping = new Ping();
        ping = monitorRepository.createCassandraPing(ping);
        return searchService.createElasticSearchPing(ping);


    }
}
