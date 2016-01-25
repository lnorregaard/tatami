package fr.ippon.tatami.web.rest;

import fr.ippon.tatami.domain.User;
import fr.ippon.tatami.security.AuthenticationService;
import fr.ippon.tatami.service.SearchService;
import fr.ippon.tatami.service.dto.UserFavouriteCountDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import java.util.List;

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
        user = authenticationService.getCurrentUser();
        log.debug("REST request to like favourite : {}", favourite);
        searchService.indexUserFavourite(favourite,user.getLogin());
    }

    /**
     * DELETE /user/favourites/:id -> Unfavourites the status
     */
    @RequestMapping(value = "/rest/user/favourites/{favourite}",
            method = RequestMethod.DELETE)
    @ResponseBody
    public void unfavoriteStatus(@PathVariable("favourite") String favourite) {
        User user = null;
        user = authenticationService.getCurrentUser();
        log.debug("REST request to unlike favourite : {}", favourite);
        searchService.removeUserFavourite(favourite,user.getLogin());
    }

    /**
     * GET /user/favourites/count -> create a user favourite
     */
    @RequestMapping(value = "/rest/user/favourites/count",
            method = RequestMethod.GET)
    @ResponseBody
    public List<UserFavouriteCountDTO> countUserFavourites(@RequestParam("id") List<String> ids) {
//        User user = null;
//        try {
//            authenticationService.validateStatus();
//            user = authenticationService.getCurrentUser();
//        } catch (UsernameNotFoundException e) {
//            log.info("The user is not active and can not make a user favourite");
//            return;
//        }
        User user = authenticationService.getCurrentUser();
        log.debug("REST request to get favourites : {}", ids);
        return searchService.countUsersForUserFavourites(ids,user);
    }


}
