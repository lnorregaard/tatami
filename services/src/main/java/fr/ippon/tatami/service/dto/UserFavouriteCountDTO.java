package fr.ippon.tatami.service.dto;

/**
 * Created by lnorregaard on 25/01/16.
 */
public class UserFavouriteCountDTO {
    private String id;
    private long total;
    private long friends = 0;

    public UserFavouriteCountDTO(String id, Long total, Long friends) {
        this.id = id;
        this.total = total;
        if (friends == null) {
            this.friends = 0;
        } else {
            this.friends = friends;
        }
    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getFriends() {
        return friends;
    }

    public void setFriends(long friends) {
        this.friends = friends;
    }
}
