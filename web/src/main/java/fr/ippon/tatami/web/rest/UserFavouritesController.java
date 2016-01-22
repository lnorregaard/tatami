package fr.ippon.tatami.web.rest;

import com.yammer.metrics.annotation.Timed;
import fr.ippon.tatami.domain.User;
import fr.ippon.tatami.security.AuthenticationService;
import fr.ippon.tatami.service.SearchService;
import fr.ippon.tatami.service.TimelineService;
import fr.ippon.tatami.service.dto.StatusDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.inject.Inject;
import java.util.Collection;

/**
 * REST controller for managing user favourites.
 *
 * @author Julien Dubois
 */
@Controller
public class UserFavouritesController {

    private final Logger log = LoggerFactory.getLogger(UserFavouritesController.class);

    @Inject
    private SearchService searchService;

    @Inject
    private AuthenticationService authenticationService;

    /**
     * POST /user/favourites/:id -> create a user favourite
     */
    @RequestMapping(value = "/rest/user/favourites/{favourite}",
            method = RequestMethod.POST)
    @ResponseBody
    public void addUserFavourite(@PathVariable("favourite") String favourite) {
        User user = null;
        try {
            authenticationService.validateStatus();
            user = authenticationService.getCurrentUser();
        } catch (UsernameNotFoundException e) {
            log.info("The user is not active and can not make a user favourite");
            return;
        }
        log.debug("REST request to like favourite : {}", favourite);
        searchService.indexUserFavourite(favourite,user.getLogin());
    }

//    /**
//     * DELETE /user/favorites/:id -> Unfavorites the status
//     */
//    @RequestMapping(value = "/rest/user/favourites/{statusId}",
//            method = RequestMethod.POST)
//    @ResponseBody
//    public void unfavoriteStatus(@PathVariable("statusId") String statusId) {
//        try {
//            authenticationService.validateStatus();
//        } catch (UsernameNotFoundException e) {
//            log.info("The user is not active and can not remove a favorite");
//            return;
//        }
//        log.debug("REST request to unlike status : {}", statusId);
//        timelineService.removeFavoriteStatus(statusId);
//    }

}
