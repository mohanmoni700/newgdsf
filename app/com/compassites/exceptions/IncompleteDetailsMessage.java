package com.compassites.exceptions;


/**
 * Created with IntelliJ IDEA.
 * User: Renu
 * Date: 5/23/14
 * Time: 6:09 PM
 * To change this template use File | Settings | File Templates.
 */
public class IncompleteDetailsMessage extends BaseCompassitesException {
    /**
     * @param message
     */
    public IncompleteDetailsMessage(String message) {
        super(message);
    }

    /**
     * @param message
     * @param cause
     */
    public IncompleteDetailsMessage(String message, Throwable cause) {
        super(message, cause);
    }


}
