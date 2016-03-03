package fr.ippon.tatami.domain.status;

import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;

import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.UUID;

/**
 * Created by lnorregaard on 03/03/16.
 */
public class FavoriteShare implements AbstractStatus{
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

    private String originalStatusId;

    private String followerLogin;

    public String getFollowerLogin() {
        return followerLogin;
    }

    public void setFollowerLogin(String followerLogin) {
        this.followerLogin = followerLogin;
    }

    public String getOriginalStatusId() {
        return originalStatusId;
    }

    public void setOriginalStatusId(String originalStatusId) {
        this.originalStatusId = originalStatusId;
    }


    @Override
    public UUID getStatusId() {
        return null;
    }

    @Override
    public StatusType getType() {
        return null;
    }

    @Override
    public String getLogin() {
        return null;
    }

    @Override
    public Date getStatusDate() {
        return null;
    }

    public String getGeoLocalization() {
        return geoLocalization;
    }

    @Override
    public void setStatusId(UUID uuid) {

    }

    @Override
    public void setStatusDate(Date date) {

    }

    @Override
    public String getUsername() {
        return null;
    }

    @Override
    public String getDomain() {
        return null;
    }

    @Override
    public void setLogin(String string) {

    }

    @Override
    public void setUsername(String string) {

    }

    @Override
    public void setDomain(String domain) {

    }

    @Override
    public void setRemoved(boolean removed) {

    }

    @Override
    public void setState(String state) {

    }

    @Override
    public String getState() {
        return null;
    }

    public void setType(StatusType type) {
        this.type = type;
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
