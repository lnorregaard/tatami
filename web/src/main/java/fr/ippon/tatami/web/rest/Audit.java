package fr.ippon.tatami.web.rest;

import java.io.Serializable;

/**
 * Created by lnorregaard on 17/05/16.
 */
public class Audit implements Serializable{

    private String response;

    public Audit(String auditMessage) {
        response = auditMessage;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }
}
