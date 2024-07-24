
package com.compassites.model.travelomatrix.ResponseModels.IssueTicket;

import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

public class IssueTicketResponse {

    @JsonProperty("Message")
    private String mMessage;
    @JsonProperty("Status")
    private Long mStatus;

    public String getMessage() {
        return mMessage;
    }

    public void setMessage(String message) {
        mMessage = message;
    }

    public Long getStatus() {
        return mStatus;
    }

    public void setStatus(Long status) {
        mStatus = status;
    }

}
