package fr.ippon.tatami.domain.status;

import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Date;
import java.util.UUID;

/**
 * Created by lnorregaard on 03/03/16.
 */
public class FriendRequest implements AbstractStatus{
    @PartitionKey
    private UUID statusId;

    @NotNull
    @Column
    private StatusType type;

    @NotNull
    @Column
    private String login;

    @NotNull
    @Column
    private String username;

    @NotNull
    @Column
    private String domain;

    @Column
    private Date statusDate;


    @Column
    private String followerLogin;

    @NotNull
    @NotEmpty(message = "Content field is mandatory.")
    @Size(min = 1, max = 10)
    private String content;

    public void setType(StatusType type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getFollowerLogin() {
        return followerLogin;
    }

    public void setFollowerLogin(String followerLogin) {
        this.followerLogin = followerLogin;
    }

    @Override
    public UUID getStatusId() {
        return statusId;
    }

    @Override
    public void setStatusId(UUID statusId) {
        this.statusId = statusId;
    }

    @Override
    public StatusType getType() {
        return type;
    }

    @Override
    public String getLogin() {
        return login;
    }

    @Override
    public void setLogin(String login) {
        this.login = login;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String getDomain() {
        return domain;
    }

    @Override
    public void setDomain(String domain) {
        this.domain = domain;
    }

    @Override
    public Date getStatusDate() {
        return statusDate;
    }

    @Override
    public void setStatusDate(Date statusDate) {
        this.statusDate = statusDate;
    }

    @Override
    public String getGeoLocalization() {
        return geoLocalization;
    }

    public boolean isRemoved() {
        return removed;
    }

    @Override
    public void setRemoved(boolean removed) {
        this.removed = removed;
    }

    @Override
    public String getState() {
        return state;
    }

    @Override
    public void setState(String state) {
        this.state = state;
    }

    public void setGeoLocalization(String geoLocalization) {
        this.geoLocalization = geoLocalization;
    }

    @Column
    private String geoLocalization;

    @Column
    private boolean removed;

    @Column
    private String state;

}
