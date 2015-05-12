package com.compassites.exceptions;

/**
 * Created with IntelliJ IDEA.
 * User: Renu
 * Date: 5/23/14
 * Time: 6:08 PM
 * To change this template use File | Settings | File Templates.
 */
public class BaseCompassitesException extends Exception {

    private String errorCode;

    public BaseCompassitesException(String message) {
        super(message);
    }

    public BaseCompassitesException(String message, Throwable cause) {
        super(message, cause);
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
}
