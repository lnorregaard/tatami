package fr.ippon.tatami.domain;

import java.util.UUID;

/**
 * Created by lnorregaard on 05/01/2017.
 */
public class StatusReplyUser {
    private UUID statusId;
    private String username;

    public StatusReplyUser(UUID statusId, String username) {
        this.statusId = statusId;
        this.username = username;
    }

    public UUID getStatusId() {
        return statusId;
    }

    public void setStatusId(UUID statusId) {
        this.statusId = statusId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
