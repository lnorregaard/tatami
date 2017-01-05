package fr.ippon.tatami.domain;

import java.util.UUID;

/**
 * Created by lnorregaard on 05/01/2017.
 */
public class StatusReplyCount {
    private UUID statusId;
    private int count;

    public StatusReplyCount(UUID statusId, long count) {
        this.statusId = statusId;
        this.count = (int) count;
    }

    public UUID getStatusId() {
        return statusId;
    }

    public void setStatusId(UUID statusId) {
        this.statusId = statusId;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
