package com.compassites.exceptions;

/**
 * Created by user on 09-07-2014.
 */
public class RetryException extends InterruptedException {

    public RetryException(String message) {
        super(message);
    }


}
