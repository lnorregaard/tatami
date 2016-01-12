package fr.ippon.tatami.service;

import javax.validation.ValidationException;

/**
 * Created by lnorregaard on 12/01/16.
 */
public class UsernameExistException extends ValidationException {
    public UsernameExistException(String message) {
        super(message);
    }
}
