package fr.ippon.tatami.service;

import com.datastax.driver.core.utils.UUIDs;
import fr.ippon.tatami.config.Constants;
import fr.ippon.tatami.domain.DigestType;
import fr.ippon.tatami.domain.User;
import fr.ippon.tatami.domain.Username;
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

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.*;
import java.util.function.Predicate;

/**
 * Manages the application's users.
 *
 * @author Lars Nørregaard
 */
@Service
public class UsernameService {

    private final Logger log = LoggerFactory.getLogger(UsernameService.class);

    @Inject
    private UsernameRepository usernameRepository;


    public void deleteUsernameForUser(User user) {
        List<Username> usernames = usernameRepository.findUsernamesByDomainAndUsername(user.getDomain(),user.getUsername());
        Optional<Username> username = usernames.stream()
                .filter(isUsernameEquals(user))
                .findFirst();
        if (username.isPresent()) {
            usernameRepository.deleteUsername(username.get());
        }
    }

    public void updateUsername(User user, User currentUser) throws UsernameExistException {
        if (user.getUsername().equals(currentUser.getUsername())) {
            return;
        }
        createUsername(user);
        deleteUsernameForUser(currentUser);
    }

    public void createUsername(User user) throws UsernameExistException {
        List<Username> usernames = usernameRepository.findUsernamesByDomainAndUsername(user.getDomain(),user.getUsername());
        if (!usernames.isEmpty()) {
            throw new UsernameExistException("Username already exists");
        }
        Username username = new Username();
        username.setLogin(user.getLogin());
        username.setUsername(user.getUsername());
        username.setDomain(user.getDomain());
        username.setCreated(UUIDs.timeBased());
        usernameRepository.createUsername(username);
        usernames = usernameRepository.findUsernamesByDomainAndUsername(user.getDomain(),user.getUsername());
        if (usernames.size() > 1 || !usernames.iterator().next().getLogin().equals(user.getLogin())) {
            usernameRepository.deleteUsername(username);
            throw new UsernameExistException("Username was reserved already");
        }
    }

    private Predicate<Username> isUsernameEquals(User user) {
        return e -> user.getLogin().equals(e.getLogin());
    }

    public String getLoginFromUsernameAndDomain(String username, String domain) {
        if (Constants.USER_AND_FRIENDS) {
            List<Username> usernames = usernameRepository.findUsernamesByDomainAndUsername(domain, username);
            if (usernames.size() == 1) {
                return usernames.iterator().next().getLogin();
            }
        }
        return DomainUtil.getLoginFromUsernameAndDomain(username, domain);
    }
}
