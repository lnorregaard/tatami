package fr.ippon.tatami.repository.cassandra;

import com.datastax.driver.core.*;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.core.querybuilder.Update;
import com.datastax.driver.core.utils.UUIDs;
import com.datastax.driver.mapping.Mapper;
import com.datastax.driver.mapping.MappingManager;
import fr.ippon.tatami.config.Constants;
import fr.ippon.tatami.domain.Attachment;
import fr.ippon.tatami.domain.Group;
import fr.ippon.tatami.domain.User;
import fr.ippon.tatami.repository.*;
import fr.ippon.tatami.service.UserService;
import fr.ippon.tatami.service.util.DomainUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;
import fr.ippon.tatami.domain.status.*;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.validation.*;
import java.util.*;
import java.util.stream.Collectors;

import static com.datastax.driver.core.querybuilder.QueryBuilder.*;

/**
 * Cassandra implementation of the status repository.
 * <p/>
 * Timeline and Userline have the same structure :
 * - Key : login
 * - Name : status Id
 * - Value : ""
 *
 * @author Julien Dubois
 */
@Repository
public class CassandraStatusRepository implements StatusRepository {

    private final Logger log = LoggerFactory.getLogger(CassandraStatusRepository.class);

    private static final String LOGIN = "login";
    private static final String TYPE = "type";
    private static final String USERNAME = "username";
    private static final String DOMAIN = "domain";
    private static final String STATUS_DATE = "statusDate";

    //Normal status
    private static final String STATUS_PRIVATE = "statusPrivate";
    private static final String GROUP_ID = "groupId";
    private static final String HAS_ATTACHMENTS = "hasAttachments";
    private static final String CONTENT = "content";
    private static final String DISCUSSION_ID = "discussionId";
    private static final String REPLY_TO = "replyTo";
    private static final String REPLY_TO_USERNAME = "replyToUsername";
    private static final String REMOVED = "removed";
    private static final String GEO_LOCALIZATION = "geoLocalization";

    //Share, Mention Share & Announcement
    private static final String ORIGINAL_STATUS_ID = "originalStatusId";

    //Mention Friend
    private static final String FOLLOWER_LOGIN = "followerLogin";


    //Bean validation
    private static final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    private static final Validator validator = factory.getValidator();

    private final static int COLUMN_TTL = 60 * 60 * 24 * 90; // The column is stored for 90 days.
    private final static int MAXTTL = 630720000;
    //Cassandra Template

    @Inject
    private UserService userService;

    @Inject
    private DiscussionRepository discussionRepository;

    @Inject
    private SharesRepository sharesRepository;

    @Inject
    private StatusAttachmentRepository statusAttachmentRepository;

    @Inject
    private AttachmentRepository attachmentRepository;

    @Inject
    private StatusStateGroupRepository statusStateGroupRepository;

    private PreparedStatement findOneByIdStmt;


    private PreparedStatement deleteByIdStmt;


    @Inject
    Session session;

    private Mapper<Status> mapper;

    @PostConstruct
    public void init() {
        mapper = new MappingManager(session).mapper(Status.class);
        findOneByIdStmt = session.prepare(
                "SELECT * " +
                        "FROM status " +
                        "WHERE statusId = :statusId");
        deleteByIdStmt = session.prepare("DELETE FROM status " +
                "WHERE statusId = :statusId");
    }


    @Override
    public Status createStatus(String login,
                               boolean statusPrivate,
                               Group group,
                               Collection<String> attachmentIds,
                               String content,
                               String discussionId,
                               String replyTo,
                               String replyToUsername,
                               String geoLocalization,
                               boolean admin)
            throws ConstraintViolationException {

        Status status = new Status();
        status.setStatusId(UUIDs.timeBased());
        status.setLogin(login);
        status.setType(StatusType.STATUS);
        String username = DomainUtil.getUsernameFromLogin(login);
        status.setUsername(username);
        String domain = DomainUtil.getDomainFromLogin(login);
        status.setDomain(domain);
        status.setStatusPrivate(statusPrivate);
        if (group != null) {
            UUID groupId = group.getGroupId();
            status.setGroupId(groupId.toString());
        }

        status.setContent(content);
        if (!admin && Constants.MODERATOR_STATUS && (status.getStatusPrivate() == null || !status.getStatusPrivate()) ) {
            status.setState("PENDING");
            statusStateGroupRepository.createStatusStateGroup(status.getStatusId(),"PENDING",status.getGroupId());
        } else if (admin && Constants.MODERATOR_STATUS && (status.getStatusPrivate() == null || !status.getStatusPrivate())) {
            statusStateGroupRepository.createStatusStateGroup(status.getStatusId(),"APPROVED",status.getGroupId());
        }

        Set<ConstraintViolation<Status>> constraintViolations = validator.validate(status);
        if (!constraintViolations.isEmpty()) {
            if (log.isDebugEnabled()) {
                constraintViolations.forEach(e -> log.debug("Constraint violation: {}", e.getMessage()));
            }
            throw new ConstraintViolationException(new HashSet<>(constraintViolations));
        }

        if (attachmentIds != null && attachmentIds.size() > 0) {
            status.setHasAttachments(true);
        }

        if (discussionId != null) {
            status.setDiscussionId(discussionId);
        }

        if (replyTo != null) {
            status.setReplyTo(replyTo);
        }

        if (replyToUsername != null) {
            status.setReplyToUsername(replyToUsername);
        }
        if(geoLocalization!=null) {
            status.setGeoLocalization(geoLocalization);
        }
        status.setStatusDate(new Date());
        BatchStatement batch = new BatchStatement();
        batch.add(mapper.saveQuery(status));
        session.execute(batch);

        return status;
    }

    @Override
    public Share createShare(String login, String originalStatusId) {
        Share share = new Share();
        share.setLogin(login);
        share.setType(StatusType.SHARE);
        String username = DomainUtil.getUsernameFromLogin(login);
        share.setUsername(username);
        String domain = DomainUtil.getDomainFromLogin(login);
        share.setDomain(domain);

        Insert inserter = this.createBaseStatus(share);
        share.setOriginalStatusId(originalStatusId);
        inserter = inserter.value("originalStatusId",UUID.fromString(originalStatusId));
        log.debug("Persisting Share : {}", share);
        session.execute(inserter);
        return share;
    }

    private Insert createBaseStatus(AbstractStatus abstractStatus) {

        abstractStatus.setStatusId(UUIDs.timeBased());
        abstractStatus.setStatusDate(Calendar.getInstance().getTime());
        if (abstractStatus.getLogin() == null) {
            throw new IllegalStateException("Login cannot be null for status: " + abstractStatus);
        }
        if (abstractStatus.getUsername() == null) {
            throw new IllegalStateException("Username cannot be null for status: " + abstractStatus);
        }
        if (abstractStatus.getDomain() == null) {
            throw new IllegalStateException("Domain cannot be null for status: " + abstractStatus);
        }

        return QueryBuilder.insertInto("status")
                .value("statusId",abstractStatus.getStatusId())
                .value("statusDate",abstractStatus.getStatusDate())
                .value("login", abstractStatus.getLogin())
                .value("username",abstractStatus.getUsername())
                .value("domain",abstractStatus.getDomain())
                .value("type",abstractStatus.getType().name());
    }

    @Override
    public Announcement createAnnouncement(String login, String originalStatusId) {
        Announcement announcement = new Announcement();
        announcement.setLogin(login);
        announcement.setType(StatusType.ANNOUNCEMENT);
        String username = DomainUtil.getUsernameFromLogin(login);
        announcement.setUsername(username);
        String domain = DomainUtil.getDomainFromLogin(login);
        announcement.setDomain(domain);

        Insert inserter = this.createBaseStatus(announcement);
        announcement.setOriginalStatusId(originalStatusId);
        inserter = inserter.value("originalStatusId",UUID.fromString(originalStatusId));
        log.debug("Persisting Announcement : {}", announcement);
        session.execute(inserter);
        return announcement;
    }

    @Override
    public MentionFriend createMentionFriend(String login, String followerLogin) {
        MentionFriend mentionFriend = new MentionFriend();
        mentionFriend.setLogin(login);
        mentionFriend.setType(StatusType.MENTION_FRIEND);
        String username = DomainUtil.getUsernameFromLogin(login);
        mentionFriend.setUsername(username);
        String domain = DomainUtil.getDomainFromLogin(login);
        mentionFriend.setDomain(domain);

        Insert inserter = this.createBaseStatus(mentionFriend);
        mentionFriend.setFollowerLogin(followerLogin);
        inserter = inserter.value("followerLogin",followerLogin);
        log.debug("Persisting Announcement : {}", mentionFriend);
        session.execute(inserter);
        return mentionFriend;
    }

    @Override
    public FavoriteShare createFavoriteShare(String login, String followerLogin, UUID originalStatusId) {
        FavoriteShare favoriteShare = new FavoriteShare();
        favoriteShare.setLogin(login);
        favoriteShare.setType(StatusType.FAVORITE_SHARE);

        User user = userService.getUserByLogin(login);
        favoriteShare.setUsername(user.getUsername());
        String domain = DomainUtil.getDomainFromLogin(login);
        favoriteShare.setDomain(domain);
        favoriteShare.setLogin(login);

        Insert inserter = this.createBaseStatus(favoriteShare);
        favoriteShare.setFollowerLogin(followerLogin);
        favoriteShare.setOriginalStatusId(originalStatusId.toString());
        inserter = inserter.value("originalStatusId",originalStatusId);
        inserter = inserter.value("followerLogin",followerLogin);
        log.debug("Persisting Announcement : {}", favoriteShare);
        session.execute(inserter);
        return favoriteShare;
    }

    @Override
    public FriendRequest createFriendRequest(String login, String followerLogin) {
        FriendRequest friendRequest = new FriendRequest();
        friendRequest.setLogin(login);
        friendRequest.setType(StatusType.FRIEND_REQUEST);
        User user = userService.getUserByLogin(login);
        friendRequest.setUsername(user.getUsername());
        String domain = DomainUtil.getDomainFromLogin(login);
        friendRequest.setDomain(domain);

        Insert inserter = this.createBaseStatus(friendRequest);
        friendRequest.setFollowerLogin(followerLogin);
        inserter = inserter.value("followerLogin",followerLogin);
        friendRequest.setContent("PENDING");
        inserter = inserter.value("content",friendRequest.getContent());
        log.debug("Persisting Announcement : {}", friendRequest);
        session.execute(inserter);
        return friendRequest;
    }

    @Override
    public void acceptFriendRequest(String statusId) {
        Update.Where where = QueryBuilder.update("status")
                .with(set("content","ACCEPTED"))
                .where(eq("statusId",UUID.fromString(statusId)));
        Statement statement = where;
        session.execute(statement);

    }

    @Override
    public void rejectFriendRequest(String statusId) {
        Update.Where where = QueryBuilder.update("status")
                .with(set("content","REJECTED"))
                .where(eq("statusId",UUID.fromString(statusId)));
        Statement statement = where;
        session.execute(statement);

    }


    @Override
    public MentionShare createMentionShare(String login, String originalStatusId) {
        MentionShare mentionShare = new MentionShare();
        mentionShare.setLogin(login);
        mentionShare.setType(StatusType.MENTION_SHARE);
        String username = DomainUtil.getUsernameFromLogin(login);
        mentionShare.setUsername(username);
        String domain = DomainUtil.getDomainFromLogin(login);
        mentionShare.setDomain(domain);

        Insert inserter = this.createBaseStatus(mentionShare);
        mentionShare.setOriginalStatusId(originalStatusId);
        inserter = inserter.value("originalStatusId",UUID.fromString(originalStatusId));
        log.debug("Persisting Announcement : {}", mentionShare);
        session.execute(inserter);

        return mentionShare;
    }

    @Override
    @Cacheable("status-cache")
    public AbstractStatus findStatusById(String statusId) {
        return findStatusById(statusId,true);
    }

    @Override
    public List<String> findStatusByStates(String types, String groupId,Integer count) {
        List<String> states = new ArrayList<>();
        if (types != null && types.contains(",")) {
            states = Arrays.asList(types.split(","));
        } else if (types != null) {
            states.add(types);
        }
        Select select = QueryBuilder.select()
                .column("statusId")
                .from("status");
        select = select.allowFiltering();
        Select.Where where = null;
        if (states.isEmpty()) {
            where = select.where(eq("type", "STATUS"));
        } else {
            where = select.where(in("state",states));
        }
        if (groupId != null) {
            where.and(eq("groupId",groupId));
        }
        if (count > 0) {
            where.limit(count);
        }

        where.orderBy(asc("statusId"));
        Statement statement = where;
        ResultSet results = session.execute(statement);
        return results
                .all()
                .stream()
                .map(e -> e.getUUID("statusId").toString())
                .collect(Collectors.toList());

    }

    public AbstractStatus findStatusById(String statusId, boolean excludeStates) {
        if (statusId == null || statusId.equals("")) {
            return null;
        }
        if (log.isTraceEnabled()) {
            log.trace("Finding status : " + statusId);
        }
        BoundStatement stmt = findOneByIdStmt.bind();
        stmt.setUUID("statusId", UUID.fromString(statusId));
        ResultSet rs = session.execute(stmt);
        if (rs.isExhausted()) {
            return null;
        }
        Row row = rs.one();
        AbstractStatus status = null;
//        if (excludeStates && row.getString("state") != null) {
//            return status;
//        }
        String type = row.getString(TYPE);
        if (type == null || type.equals(StatusType.STATUS.name())) {
            status = findStatus(row, statusId);
        } else if (type.equals(StatusType.SHARE.name())) {
            status = findShare(row);
        } else if (type.equals(StatusType.ANNOUNCEMENT.name())) {
            status = findAnnouncement(row);
        } else if (type.equals(StatusType.MENTION_FRIEND.name())) {
            status = findMentionFriend(row);
        } else if (type.equals(StatusType.MENTION_SHARE.name())) {
            status = findMentionShare(row);
        } else if (type.equals(StatusType.FAVORITE_SHARE.name())) {
            status = findFavoriteShare(row);
        } else if (type.equals(StatusType.FRIEND_REQUEST.name())) {
            status = findFriendRequest(row);

        } else {
            throw new IllegalStateException("Status has an unknown type: " + type);
        }
        if (status == null) { // Status was not found, or was removed
            return null;
        }
        status.setStatusId(UUID.fromString(statusId));
        User user = userService.getUserByLogin(row.getString(LOGIN));
        if (user != null) {
            status.setLogin(user.getLogin());
            status.setUsername(user.getUsername());
        } else {
            status.setLogin(row.getString(LOGIN));
            status.setUsername(row.getString(USERNAME));
        }
        status.setState(row.getString("state"));

        String domain = row.getString(DOMAIN);
        if (domain != null) {
            status.setDomain(domain);
        } else {
            throw new IllegalStateException("Status cannot have a null domain: " + status);
        }

        status.setStatusDate(row.getDate(STATUS_DATE));
        Boolean removed = row.getBool(REMOVED);
        if (removed != null) {
            status.setRemoved(removed);
        }
        return status;

    }

    @Override
    @CacheEvict(value = "status-cache", key = "#statusId")
    public void updateState(String statusId, String state) {
        Update.Where where = QueryBuilder.update("status")
                .with(set("state",state))
                .where(eq("statusId",UUID.fromString(statusId)));
        if (state != null && state.equals("BLOCKED")) {
            where.using(ttl(COLUMN_TTL));
        } else {
            where.using(ttl(MAXTTL));
        }
        Statement statement = where;
        session.execute(statement);
    }

    private AbstractStatus findMentionShare(Row result) {
        MentionShare mentionShare = new MentionShare();
        mentionShare.setType(StatusType.MENTION_SHARE);
        mentionShare.setOriginalStatusId(result.getUUID(ORIGINAL_STATUS_ID).toString());
        return mentionShare;
    }

    private AbstractStatus findFavoriteShare(Row result) {
        FavoriteShare favoriteShare = new FavoriteShare();
        favoriteShare.setType(StatusType.FAVORITE_SHARE);
        favoriteShare.setOriginalStatusId(result.getUUID(ORIGINAL_STATUS_ID).toString());
        favoriteShare.setLogin(result.getString(LOGIN));
        favoriteShare.setFollowerLogin(result.getString(FOLLOWER_LOGIN));
        return favoriteShare;
    }

    private AbstractStatus findFriendRequest(Row result) {
        FriendRequest friendRequest = new FriendRequest();
        friendRequest.setType(StatusType.FRIEND_REQUEST);
        friendRequest.setLogin(result.getString(LOGIN));
        friendRequest.setFollowerLogin(result.getString(FOLLOWER_LOGIN));
        friendRequest.setContent(result.getString(CONTENT));
        return friendRequest;
    }

    private AbstractStatus findMentionFriend(Row result) {
        MentionFriend mentionFriend = new MentionFriend();
        mentionFriend.setType(StatusType.MENTION_FRIEND);
        mentionFriend.setFollowerLogin(result.getString(FOLLOWER_LOGIN));
        return mentionFriend;
    }

    private AbstractStatus findAnnouncement(Row result) {
        Announcement announcement = new Announcement();
        announcement.setType(StatusType.ANNOUNCEMENT);
        announcement.setOriginalStatusId(result.getUUID(ORIGINAL_STATUS_ID).toString());
        return announcement;
    }

    private AbstractStatus findShare(Row result) {
        Share share = new Share();
        share.setType(StatusType.SHARE);
        share.setOriginalStatusId(result.getUUID(ORIGINAL_STATUS_ID).toString());
        return share;
    }

    private AbstractStatus findStatus(Row result, String statusId) {
        Status status = new Status();
        status.setStatusId(UUID.fromString(statusId));
        status.setType(StatusType.STATUS);
        status.setContent(result.getString(CONTENT));
        status.setStatusPrivate(result.getBool(STATUS_PRIVATE));
        status.setGroupId(result.getString(GROUP_ID));
        status.setHasAttachments(result.getBool(HAS_ATTACHMENTS));
        status.setDiscussionId(result.getString(DISCUSSION_ID));
        status.setReplyTo(result.getString(REPLY_TO));
        status.setReplyToUsername(result.getString(REPLY_TO_USERNAME));
        status.setGeoLocalization(result.getString(GEO_LOCALIZATION));
        status.setRemoved(result.getBool(REMOVED));
        if (status.isRemoved()) {
            return null;
        }
        status.setDetailsAvailable(computeDetailsAvailable(status));
        if (status.getHasAttachments() != null && status.getHasAttachments()) {
            Collection<String> attachmentIds = statusAttachmentRepository.findAttachmentIds(statusId);
            Collection<Attachment> attachments = new ArrayList<>();
            for (String attachmentId : attachmentIds) {
                Attachment attachment = attachmentRepository.findAttachmentMetadataById(attachmentId);
                if (attachment != null) {
                    // We copy everything excepted the attachment content, as we do not want it in the status cache
                    Attachment attachmentCopy = new Attachment();
                    attachmentCopy.setAttachmentId(attachmentId);
                    attachmentCopy.setSize(attachment.getSize());
                    attachmentCopy.setFilename(attachment.getFilename());
                    attachments.add(attachment);
                }
            }
            status.setAttachments(attachments);
        }
        return status;
    }

    @Override
    @CacheEvict(value = "status-cache", key = "#status.statusId")
    public void removeStatus(AbstractStatus status) {
        log.debug("Removing Status : {}", status);
        BatchStatement batch = new BatchStatement();
        batch.add(deleteByIdStmt.bind().setUUID("statusId", status.getStatusId()));
        session.execute(batch);
    }

    private boolean computeDetailsAvailable(Status status) {
        boolean detailsAvailable = false;
        if (status.getType().equals(StatusType.STATUS)) {
            if (StringUtils.isNotBlank(status.getReplyTo())) {
                detailsAvailable = true;
//            } else if (discussionRepository.hasReply(status.getStatusId())) {
//                detailsAvailable = true;
//            } else if (sharesRepository.hasBeenShared(status.getStatusId())) {
//                detailsAvailable = true;
            }
        }
        return detailsAvailable;
    }
}
