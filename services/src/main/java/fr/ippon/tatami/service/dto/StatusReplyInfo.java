package fr.ippon.tatami.service.dto;

import fr.ippon.tatami.domain.Attachment;
import fr.ippon.tatami.domain.status.StatusType;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.joda.time.format.ISODateTimeFormat;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;

/**
 * DTO to present a "complete" status to the presentation layer.
 */
public class StatusReplyInfo implements Serializable {

    private String statusId;
    private long replyCount;
    private String replyUsername;

    public StatusReplyInfo() {
    }

    public StatusReplyInfo(String statusId, long replyCount, String replyUsername) {
        this.statusId = statusId;
        this.replyCount = replyCount;
        this.replyUsername = replyUsername;
    }

    public String getStatusId() {
        return statusId;
    }

    public void setStatusId(String statusId) {
        this.statusId = statusId;
    }

    public long getReplyCount() {
        return replyCount;
    }

    public void setReplyCount(long replyCount) {
        this.replyCount = replyCount;
    }

    public String getReplyUsername() {
        return replyUsername;
    }

    public void setReplyUsername(String replyUsername) {
        this.replyUsername = replyUsername;
    }
}
