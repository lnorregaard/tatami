package fr.ippon.tatami.domain;

import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

import java.io.Serializable;
import java.util.UUID;

/**
 * A user.
 *
 * @author Julien Dubois
 */
@Table(name = "username")
public class Username implements Serializable {


    @PartitionKey
    private String username;

    private String domain;

    private String login;

    private UUID created;

    public Username() {
    }

    public Username(String username, String domain, String login, UUID created) {
        this.username = username;
        this.domain = domain;
        this.login = login;
        this.created = created;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public UUID getCreated() {
        return created;
    }

    public void setCreated(UUID created) {
        this.created = created;
    }

    @Override
    public String toString() {
        return "Username{" +
                "username='" + username + '\'' +
                ", domain='" + domain + '\'' +
                ", login='" + login + '\'' +
                ", created=" + created +
                '}';
    }
}
