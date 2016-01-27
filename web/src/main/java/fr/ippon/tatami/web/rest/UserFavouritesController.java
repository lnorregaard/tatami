package fr.ippon.tatami.web.rest;

import fr.ippon.tatami.domain.User;
import fr.ippon.tatami.security.AuthenticationService;
import fr.ippon.tatami.service.SearchService;
import fr.ippon.tatami.service.UserService;
import fr.ippon.tatami.service.dto.UserDTO;
import fr.ippon.tatami.service.dto.UserFavouriteCountDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

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

    @Inject
    private UserService userService;

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
        favourite = favourite.replace(":","_");
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
        favourite = favourite.replace(":","_");
        searchService.removeUserFavourite(favourite,user.getLogin());
    }

    /**
     * GET /user/favourites/count -> count user for favourites
     */
    @RequestMapping(value = {"/rest/user/favourites/count","/user/favourites/count"},
            method = RequestMethod.GET)
    @ResponseBody
    public List<UserFavouriteCountDTO> countUserFavourites(@RequestParam("id") List<String> ids) {
        User user = null;
        if (authenticationService.hasAuthenticatedUser()) {
            user = authenticationService.getCurrentUser();
        }
        log.debug("REST request to get favourites : {}", ids);
        List<String> idsReplaced = ids.stream()
                .map(id -> id.replace(":", "_"))
                .collect(Collectors.toList());
        return searchService.countUsersForUserFavourites(idsReplaced,user);
    }

    /**
     * GET /user/favourites/:id/friends -> Return friends for a user favourite
     */
    @RequestMapping(value = "/rest/user/favourites/{favourite}/friends",
            method = RequestMethod.GET)
    @ResponseBody
    public Collection<User> getFriendsForUserFavourite(@PathVariable("favourite") String id,
                                                       @RequestParam(value = "from",required = false) Integer from,
                                                       @RequestParam(value = "size",required = false) Integer size) {
        User user = authenticationService.getCurrentUser();
        id = id.replace(":","_");
        log.debug("REST request to get firends for user: {} and favourite : {}", user,id);
        int simpleFrom= 0;
        if (from != null) {
            simpleFrom = from;
        }
        int simpleSize = 0;
        if (size != null) {
            simpleSize = size;
        }
        List<String> logins = searchService.getFriendsForUserFavourite(id,user,simpleFrom,simpleSize);
        return userService.getUsersByLogin(logins);
    }

    /**
     * GET /user/favourites/friends -> create a user favourite
     */
    @RequestMapping(value = "/rest/user/{username}/favourites",
            method = RequestMethod.GET)
    @ResponseBody
    public Collection<String> getFavouritesForUser(@PathVariable("username") String username) {
        log.debug("REST request to get firends for user: {} ", username);
        if (authenticationService.hasAuthenticatedUser()) {
            User user = authenticationService.getCurrentUser();
            return searchService.getUserFavouritesForUser(username,user.getDomain()).stream()
                    .map(id -> id.replace("_",":"))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }


}
