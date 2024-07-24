
package com.compassites.model.travelomatrix.ResponseModels.CommitBookingReply;

import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;


public class CommitBookingReply {

    @JsonProperty("CommitBooking")
    private CommitBooking commitBooking;
    @JsonProperty("Message")
    private String message;
    @JsonProperty("Status")
    private String status;

    public CommitBooking getCommitBooking() {
        return commitBooking;
    }

    public void setCommitBooking(CommitBooking commitBooking) {
        this.commitBooking = commitBooking;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

}
