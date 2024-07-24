
package com.compassites.model.travelomatrix.ResponseModels.HoldTicket;

import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;


public class HoldTicketResponse {

    @JsonProperty("HoldTicket")
    private HoldTicket holdTicket;
    @JsonProperty("Message")
    private String message;
    @JsonProperty("Status")
    private String status;

    public HoldTicket getHoldTicket() {
        return holdTicket;
    }

    public void setHoldTicket(HoldTicket holdTicket) {
        this.holdTicket = holdTicket;
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
