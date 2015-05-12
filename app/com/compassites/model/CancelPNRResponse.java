package com.compassites.model;


import java.io.Serializable;

/**
 * Created by Yaseen on 11-05-2015.
 */
public class CancelPNRResponse implements Serializable {

    private boolean success;

    private ErrorMessage errorMessage;


    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public ErrorMessage getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(ErrorMessage errorMessage) {
        this.errorMessage = errorMessage;
    }
}
