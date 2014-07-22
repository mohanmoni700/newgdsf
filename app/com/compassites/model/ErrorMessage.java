package com.compassites.model;

/**
 * Created by user on 17-07-2014.
 */
public class ErrorMessage {

    private String errorCode;

    private String message;

    private ErrorType type;

    private String provider;


    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public ErrorType getType() {
        return type;
    }

    public void setType(ErrorType type) {
        this.type = type;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public enum ErrorType{
        ERROR,WARNING;
    }
}

