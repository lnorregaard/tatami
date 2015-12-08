package fr.ippon.tatami.web.rest.dto;

/**
 * Created by lnorregaard on 08/12/15.
 */
public class ActionModerator {
    private String state;
    private String comment;

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
