package com.compassites.model;

/**
 * Created by Satish Kumar on 03-03-2017.
 */
public class MessageQueue {

    private String bookingMode;

    private String message;

    private Integer rph;

    private String tkeTimeLimit;

    private String uniqueId;

    public String getBookingMode() {
        return bookingMode;
    }

    public void setBookingMode(String bookingMode) {
        this.bookingMode = bookingMode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getRph() {
        return rph;
    }

    public void setRph(Integer rph) {
        this.rph = rph;
    }

    public String getTkeTimeLimit() {
        return tkeTimeLimit;
    }

    public void setTkeTimeLimit(String tkeTimeLimit) {
        this.tkeTimeLimit = tkeTimeLimit;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }
}
