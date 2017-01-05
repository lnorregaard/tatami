package fr.ippon.tatami.repository;

import java.util.List;
import java.util.UUID;

/**
 * The Reply Counter Repository.
 *
 * @author Lars Nørregaard
 */
public interface StatusRepliesRepository {
    void insertReply(UUID originalStatusId, UUID replyStatusId);
    void deleteReply(UUID originalStatusId, UUID replyStatusId);
    List<String> getReplies(UUID originalStatusId, int size, String start, String finish, boolean desc);
}
