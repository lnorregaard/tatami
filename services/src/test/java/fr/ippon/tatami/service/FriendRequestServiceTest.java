package fr.ippon.tatami.service;

import fr.ippon.tatami.AbstractCassandraTatamiTest;
import fr.ippon.tatami.config.Constants;
import fr.ippon.tatami.domain.User;
import fr.ippon.tatami.security.AuthenticationService;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.inject.Inject;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FriendRequestServiceTest extends AbstractCassandraTatamiTest {

    @Inject
    public UserService userService;

    @Inject
    public FriendshipService friendshipService;

    @BeforeClass
    public static void setup() {
        Constants.USER_AND_FRIENDS = true;
    }

    @AfterClass
    public static void tearDown() {
        Constants.USER_AND_FRIENDS = false;
    }

    @Test
    public void shouldGetAUserServiceInjected() {
        assertThat(userService, notNullValue());
    }

    @Test
    public void shouldGetAFollowerServiceInjected() {
        assertThat(friendshipService, notNullValue());
    }

    @Test
    public void shouldBeFriendWithUser() {
        Constants.USER_AND_FRIENDS = false;
        mockAuthentication("userWhoWillBeFollowed@ippon.fr");
        friendshipService.followUser("userWhoWantToFollow");

        mockAuthentication("userWhoWantToFollow@ippon.fr");

        User userWhoFollow = userService.getUserByUsername("userWhoWantToFollow");
        assertThat(userWhoFollow.getFriendsCount(), is(0L));

        assertTrue(friendshipService.followUser("userWhoWillBeFollowed"));

        /* verify */
        userWhoFollow = userService.getUserByUsername("userWhoWantToFollow");
        assertThat(userWhoFollow.getFriendsCount(), is(1L));
        assertThat(userWhoFollow.getFollowersCount(), is(1L));

        User userWhoIsFollowed = userService.getUserByUsername("userWhoWillBeFollowed");
        assertThat(userWhoIsFollowed.getFriendsCount(), is(1L));
        assertThat(userWhoIsFollowed.getFollowersCount(), is(1L));

        // Clean up
        friendshipService.unfollowUser("userWhoWillBeFollowed");

        /* verify clean up */
        userWhoFollow = userService.getUserByUsername("userWhoWantToFollow");
        assertThat(userWhoFollow.getFriendsCount(), is(0L));

        userWhoIsFollowed = userService.getUserByUsername("userWhoWillBeFollowed");
        assertThat(userWhoIsFollowed.getFollowersCount(), is(0L));
    }

    @Test
    public void shouldNotSendFriendRequestAgain() throws Exception {
        Constants.USER_AND_FRIENDS = false;

        mockAuthentication("userWhoWantToFollow@ippon.fr");

        User userWhoWillBeFollowed = new User();
        userWhoWillBeFollowed.setLogin("userWhoWillBeFollowed@ippon.fr");
        userService.createUser(userWhoWillBeFollowed);
        userWhoWillBeFollowed.setDailyDigestSubscription(false);
        userWhoWillBeFollowed.setWeeklyDigestSubscription(false);
        userService.updateUser(userWhoWillBeFollowed);
        Constants.USER_AND_FRIENDS = true;
        User userWhoFollow = userService.getUserByUsername("userWhoWantToFollow");
        assertThat(userWhoFollow.getFriendsCount(), is(0L));

        assertTrue(friendshipService.followUser("userWhoWillBeFollowed"));
        assertFalse(friendshipService.followUser("userWhoWillBeFollowed"));
    }

    private void mockAuthentication(String login) {
        User authenticateUser = constructAUser(login);
        AuthenticationService mockAuthenticationService = mock(AuthenticationService.class);
        when(mockAuthenticationService.getCurrentUser()).thenReturn(authenticateUser);
        ReflectionTestUtils.setField(friendshipService, "authenticationService", mockAuthenticationService);
        ReflectionTestUtils.setField(userService, "authenticationService", mockAuthenticationService);
    }
}