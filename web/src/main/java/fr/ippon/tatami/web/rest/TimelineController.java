package fr.ippon.tatami.web.rest;

import com.yammer.metrics.annotation.Timed;
import fr.ippon.tatami.config.Constants;
import fr.ippon.tatami.domain.Group;
import fr.ippon.tatami.domain.User;
import fr.ippon.tatami.domain.status.Status;
import fr.ippon.tatami.domain.status.StatusDetails;
import fr.ippon.tatami.domain.status.StatusType;
import fr.ippon.tatami.security.AuthenticationService;
import fr.ippon.tatami.service.GroupService;
import fr.ippon.tatami.service.StatusUpdateService;
import fr.ippon.tatami.service.TimelineService;
import fr.ippon.tatami.service.dto.StatusDTO;
import fr.ippon.tatami.service.dto.StatusReplyInfo;
import fr.ippon.tatami.service.exception.ArchivedGroupException;
import fr.ippon.tatami.service.exception.ReplyStatusException;
import fr.ippon.tatami.web.rest.dto.ActionModerator;
import fr.ippon.tatami.web.rest.dto.ActionStatus;
import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * REST controller for managing status.
 *
 * @author Julien Dubois
 */
@Controller
public class TimelineController {

    public static final String APPROVED = "APPROVED";
    public static final String BLOCKED = "BLOCKED";
    public static final String END_ESI = "\"/>";
    public static final String TTL_ESI = "20m";
    public static final String ESI_ENABLE = "ESI-enable";
    private final Logger log = LoggerFactory.getLogger(TimelineController.class);

    @Inject
    private TimelineService timelineService;

    @Inject
    private StatusUpdateService statusUpdateService;

    @Inject
    private GroupService groupService;

    @Inject
    private AuthenticationService authenticationService;

    @Inject
    private Environment env;

    /**
     * GET  /statuses/details/:id -> returns the details for a status, specified by the id parameter
     */
    @RequestMapping(value = "/rest/statuses/details/{statusId}",
            method = RequestMethod.GET,
            produces = "application/json")
    @ResponseBody
    public StatusDetails getStatusDetails(@PathVariable("statusId") String statusId) {
        log.debug("REST request to get status details Id : {}", statusId);
        return timelineService.getStatusDetails(statusId);
    }

    /**
     * GET  /statuses/:id/audit -> returns the details for a status, specified by the id parameter
     */
    @RequestMapping(value = "/rest/statuses/{statusId}/audit",
            method = RequestMethod.GET,
            produces = "application/json")
    @ResponseBody
    public Audit getAuditMessage(@PathVariable("statusId") String statusId) {
        log.debug("REST request to get audit message for Id : {}", statusId);
        Audit audit = new Audit(timelineService.getAuditMessage(statusId));
        return audit;
    }


    /**
     * GET  /statuses/home_timeline -> get the latest statuses from the current user
     */
    @RequestMapping(value = "/rest/statuses/home_timeline",
            method = RequestMethod.GET,
            produces = "application/json")
    @ResponseBody
    @Timed
    public Collection<StatusDTO> listStatus(@RequestParam(required = false) Integer count,
                                            @RequestParam(required = false) String start,
                                            @RequestParam(required = false) String finish) {
        if (count == null || count == 0) {
            count = 20; //Default value
        }
        try {
            return timelineService.getTimeline(count, start, finish, null);
        } catch (Exception e) {
            StringWriter stack = new StringWriter();
            PrintWriter pw = new PrintWriter(stack);
            e.printStackTrace(pw);
            log.debug("{}", stack.toString());
            return null;
        }
    }

    /**
     * GET  /statuses/home_timeline -> get the latest messages from the current user
     */
    @RequestMapping(value = "/rest/statuses/home_messages",
            method = RequestMethod.GET,
            produces = "application/json")
    @ResponseBody
    @Timed
    public Collection<StatusDTO> listStatusMessages(@RequestParam(required = false) Integer count,
                                            @RequestParam(required = false) String start,
                                            @RequestParam(required = false) String finish) {
        if (count == null || count == 0) {
            count = 20; //Default value
        }
        try {
            return timelineService.getTimeline(count, start, finish, StatusType.STATUS.toString()).stream()
                    .filter(status -> !status.isStatusPrivate())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            StringWriter stack = new StringWriter();
            PrintWriter pw = new PrintWriter(stack);
            e.printStackTrace(pw);
            log.debug("{}", stack.toString());
            return null;
        }
    }

    /**
     * GET  /statuses/home_messages/count -> get the message count from the current user
     */
    @RequestMapping(value = "/rest/statuses/home_messages/count",
            method = RequestMethod.GET,
            produces = "application/json")
    @ResponseBody
    @Timed
    public Long listStatusMessagesCount(@RequestParam(required = false) Integer count,
                                                    @RequestParam(required = false) String start,
                                                    @RequestParam(required = false) String finish) {
        try {
            return timelineService.getTimelineCount();
        } catch (Exception e) {
            StringWriter stack = new StringWriter();
            PrintWriter pw = new PrintWriter(stack);
            e.printStackTrace(pw);
            log.debug("{}", stack.toString());
            return null;
        }
    }


    /**
     * GET  /statuses/user_timeline?screen_name=jdubois -> get the latest statuses from user "jdubois"
     */
    @RequestMapping(value = "/rest/statuses/{username}/timeline",
            method = RequestMethod.GET,
            produces = "application/json")
    @ResponseBody
    public Collection<StatusDTO> listStatusForUser(@PathVariable("username") String username,
                                                   @RequestParam(required = false) Integer count,
                                                   @RequestParam(required = false) String start,
                                                   @RequestParam(required = false) String finish) {

        if (count == null || count == 0) {
            count = 20; //Default value
        }
        log.debug("REST request to get someone's status (username={}).", username);
        if (username == null || username.length() == 0) {
            return new ArrayList<StatusDTO>();
        }
        try {
            return timelineService.getUserline(username, count, start, finish);
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                e.printStackTrace();
            }
            return new ArrayList<StatusDTO>();
        }
    }

    /**
     * GET  /statuses/user_timeline?screen_name=jdubois -> get the latest statuses from user "jdubois"
     */
    @RequestMapping(value = "/rest/statuses/moderator/count",
            method = RequestMethod.GET,
            produces = "application/json")
    @ResponseBody
    public Long listStatusForModeratorCount(@RequestParam(required = false) String states,
                                                        @RequestParam(required = false) String groupId) {

        try {
            return timelineService.getStatusForStatesCount(states, groupId);
        } catch (Exception e) {
            log.warn("No status found: ",e);
            return -2L;
        }
    }

    /**
     * GET  /statuses/user_timeline?screen_name=jdubois -> get the latest statuses from user "jdubois"
     */
    @RequestMapping(value = "/rest/statuses/moderator",
            method = RequestMethod.GET,
            produces = "application/json")
    @ResponseBody
    public Collection<StatusDTO> listStatusForModerator(@RequestParam(required = false) String states,
                                                        @RequestParam(required = false) String groupId,
                                                        @RequestParam(required = false) String start,
                                                        @RequestParam(required = false) String finish,
                                                        @RequestParam(required = false) Integer count,
                                                        @RequestParam(required = false) String order) {

        if (count == null || count == 0) {
            count = 20; //Default value
        }
        try {
            return timelineService.getStatusForStates(states, groupId, start,finish, count,order);
        } catch (Exception e) {
            log.warn("No status found: ",e);
            return new ArrayList<>();
        }
    }


    /**
     * POST /statuses/ -> create a new Status
     */
    @RequestMapping(value = "/rest/statuses/moderator",
            method = RequestMethod.POST,
            produces = "application/json")
    @Timed
    public Status postModeratorStatus(@RequestBody StatusDTO status, HttpServletResponse response) throws ArchivedGroupException, ReplyStatusException {
        try {
            authenticationService.validateStatus();
        } catch (UsernameNotFoundException e) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return null;
        }
        log.debug("REST request to add status : {}", status.getContent());
        String escapedContent = StringEscapeUtils.escapeHtml(status.getContent());
        Collection<String> attachmentIds = status.getAttachmentIds();

        Status createdStatus = statusUpdateService.postStatusAndSendToUser(escapedContent, false, attachmentIds, status.getGeoLocalization(),status.getReplyToUsername());
        if (createdStatus == null) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        }
        return createdStatus;
    }


    @RequestMapping(value = "/rest/statuses/moderator/{statusId}",
            method = RequestMethod.PATCH)
    @ResponseBody
    public StatusDTO updateStatus(@RequestBody ActionModerator action, @PathVariable("statusId") String statusId) {
        try {
            authenticationService.validateStatus();
        } catch (UsernameNotFoundException e) {
            return null;
        }
        try {
            StatusDTO status = timelineService.getPendingStatus(statusId);
            if (action != null) {
                if (action.getState() != null && action.getState().equals(APPROVED)) {
                    log.info("Approve status: {} with message: {} for username : {} by moderator: {}",
                            statusId,
                            status.getContent(),
                            status.getUsername(),
                            authenticationService.getCurrentUser().getLogin());
                    timelineService.approveStatus(statusId);
                    status.setState(action.getState());
                } else if (action.getState() != null && action.getState().equals(BLOCKED) && action.getComment() != null && !action.getComment().isEmpty()) {
                    log.info("Block status: {} with message: {} for username : {} by moderator: {}",
                            statusId,
                            status.getContent(),
                            status.getUsername(),
                            authenticationService.getCurrentUser().getLogin());
                    timelineService.blockStatus(authenticationService.getCurrentUser().getLogin(),statusId,action.getComment(),status.getUsername());
                    status.setState(action.getState());
                } else {
                    log.debug("state or comment is null statusId: {} State: {} Comment: {}",statusId,action.getState(),action.getComment());
                    return null;
                }
            }
            return status;
        } catch (Exception e) {
            StringWriter stack = new StringWriter();
            PrintWriter pw = new PrintWriter(stack);
            e.printStackTrace(pw);
            log.debug("{}", stack.toString());
            return null;
        }
    }

    /**
     * GET  /statuses/{statusId}/replies?start=id&finish=id&count=10&desc=true -> get the replies for status"
     */
    @RequestMapping(value = {"/rest/statuses/{statusId}/replies","/statuses/{statusId}/replies"},
            method = RequestMethod.GET,
            produces = "application/json")
    @ResponseBody
    public Collection<StatusDTO> listReplyStatuses(@PathVariable("statusId") String statusId,
                                                        @RequestParam(required = false) String start,
                                                        @RequestParam(required = false) String finish,
                                                        @RequestParam(required = false) Integer count,
                                                        @RequestParam(required = false) boolean desc) {

        if (count == null || count == 0) {
            count = 20; //Default value
        }
        try {
            return timelineService.getRepliesForStatus(statusId,start,finish, count,desc);
        } catch (Exception e) {
            log.warn("No status found: ",e);
            return new ArrayList<>();
        }
    }

    /**
     * GET  /statuses/replies?id=id1&id=id2 -> get the reply info for statuses"
     */
    @RequestMapping(value = {"/rest/statuses/replies"},
            method = RequestMethod.GET,
            produces = "application/json")
    @ResponseBody
    public ResponseEntity<Object> listStatusReplies(
            @RequestHeader(required = false, name = ESI_ENABLE) boolean esi,
            @RequestParam(name = "id", required = false) List<String> statusIds) {
        if (esi && statusIds != null && statusIds.stream().distinct().count() <= 10) {
            String startEsi = new StringBuilder().append("<esi:include src=\"")
                    .append(env.getProperty("tatami.url"))
                    .append(env.getProperty("tatami.path",""))
                    .append("/rest/statuses/replies/").toString();
            StringBuilder builder = new StringBuilder("[");
            builder.append(statusIds.stream()
                    .distinct()
                    .map(id -> new StringBuilder().append(startEsi).append(id).append(END_ESI).toString())
                    .collect(Collectors.joining(",")));
            builder.append("]");
            return ResponseEntity.status(HttpStatus.OK)
                    .header("ESI-ttl", TTL_ESI)
                    .header(ESI_ENABLE, "true")
                    .body(builder.toString());
        } else {
            try {
                return ResponseEntity.status(HttpStatus.OK)
                        .body(timelineService.getReplyInfos(statusIds));
            } catch (Exception e) {
                log.warn("No status found: ", e);
                return ResponseEntity.status(HttpStatus.OK).body(new ArrayList<StatusReplyInfo>());
            }
        }
    }

    @RequestMapping(value = {"/rest/statuses/replies/{statusId}"},
            method = RequestMethod.GET,
            produces = "application/json")
    @ResponseBody
    public ResponseEntity<Object> listStatusReply(@PathVariable String statusId) {
        try {
            StatusReplyInfo replyInfo = timelineService.getReplyInfos(Arrays.asList(statusId)).stream()
                    .findFirst().orElse(new StatusReplyInfo("",0,""));
            return ResponseEntity.ok()
                    .header(ESI_ENABLE,null)
                    .body(replyInfo);
        } catch (Exception e) {
            log.warn("No status found: ", e);
            return ResponseEntity.status(HttpStatus.OK).body(new StatusReplyInfo("",0,""));
        }
    }


    @RequestMapping(value = "/rest/statuses/{statusId}",
            method = RequestMethod.GET,
            produces = "application/json")
    @ResponseBody
    public StatusDTO getStatus(@PathVariable("statusId") String statusId) {
        log.debug("REST request to get status Id : {}", statusId);
        return timelineService.getStatus(statusId);
    }

    @RequestMapping(value = "/rest/statuses/{statusId}",
            method = RequestMethod.DELETE)
    public void deleteStatus(@PathVariable("statusId") String statusId) {
        try {
            authenticationService.validateStatus();
        } catch (UsernameNotFoundException e) {
            log.warn("Can not delete status as the user is not active");
            return;
        }
        log.debug("REST request to get status Id : {}", statusId);
        timelineService.removeStatus(statusId);
    }

    @RequestMapping(value = "/rest/statuses/{statusId}",
            method = RequestMethod.PATCH)
    @ResponseBody
    public StatusDTO updateStatus(@RequestBody ActionStatus action, @PathVariable("statusId") String statusId) {
        try {
            authenticationService.validateStatus();
        } catch (UsernameNotFoundException e) {
            return null;
        }
        try {
            StatusDTO status = timelineService.getStatus(statusId);
            if (action.isFavorite() != null && status.isFavorite() != action.isFavorite()) {
                if (action.isFavorite()) {
                    timelineService.addFavoriteStatus(statusId);
                } else {
                    timelineService.removeFavoriteStatus(statusId);
                }
                status.setFavorite(action.isFavorite());
            }
            if (action.isShared() != null && action.isShared()) {
                timelineService.shareStatus(statusId);
                status.setShareByMe(action.isShared());
            }
            if (action.isAnnounced() != null && action.isAnnounced()) {
                timelineService.announceStatus(statusId);
            }
            return status;
        } catch (Exception e) {
            StringWriter stack = new StringWriter();
            PrintWriter pw = new PrintWriter(stack);
            e.printStackTrace(pw);
            log.debug("{}", stack.toString());
            return null;
        }
    }

    /**
     * POST /statuses/ -> create a new Status
     */
    @RequestMapping(value = "/rest/statuses/",
            method = RequestMethod.POST,
            produces = "application/json")
    @Timed
    public Status postStatus(@RequestBody StatusDTO status, HttpServletResponse response) throws ArchivedGroupException, ReplyStatusException {
        try {
            authenticationService.validateStatus();
        } catch (UsernameNotFoundException e) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return null;
        }
        log.debug("REST request to add status : {}", status.getContent());
        String escapedContent = StringEscapeUtils.escapeHtml(status.getContent());
        Collection<String> attachmentIds = status.getAttachmentIds();

        if (status.getReplyTo() != null && !status.getReplyTo().isEmpty()) {
            log.debug("Creating a reply to : {}", status.getReplyTo());
            statusUpdateService.replyToStatus(escapedContent, status.getReplyTo(), attachmentIds);
        } else if (status.isStatusPrivate() || status.getGroupId() == null || status.getGroupId().equals("")) {
            log.debug("Private status");
            return statusUpdateService.postStatus(escapedContent, status.isStatusPrivate(), attachmentIds, status.getGeoLocalization());
        } else {
            User currentUser = authenticationService.getCurrentUser();
            Collection<Group> groups = groupService.getGroupsForUser(currentUser);
            Group group = null;
            UUID statusGroupId = UUID.fromString(status.getGroupId());
            for (Group testGroup : groups) {
                if (testGroup.getGroupId().equals(statusGroupId)) {
                    group = testGroup;
                    break;
                }
            }
            if (Constants.NON_GROUP_MEMBER_POST_TIMELINE) {
                group = groupService.getGroupById(currentUser.getDomain(), statusGroupId);
            }
            if (group == null) {
                log.info("Permission denied! User {} tried to access " +
                        "group ID = {}", currentUser.getLogin(), status.getGroupId());
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            } else if (group.isArchivedGroup()) {
                log.info("Archived group! User {} tried to post a message to archived " +
                        "group ID = {}", currentUser.getLogin(), status.getGroupId());
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            } else {
                return statusUpdateService.postStatusToGroup(escapedContent, group, attachmentIds, status.getUsername(),status.getGeoLocalization());
            }
        }
        return null;
    }
}
