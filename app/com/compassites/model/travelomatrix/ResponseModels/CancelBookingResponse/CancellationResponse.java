
package com.compassites.model.travelomatrix.ResponseModels.CancelBookingResponse;

import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;


public class CancellationResponse {

    @JsonProperty("Message")
    private String message;
    @JsonProperty("Status")
    private Long status;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getStatus() {
        return status;
    }

    public void setStatus(Long status) {
        this.status = status;
    }

}
