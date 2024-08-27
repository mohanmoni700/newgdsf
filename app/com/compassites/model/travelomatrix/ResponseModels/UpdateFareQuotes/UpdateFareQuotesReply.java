
package com.compassites.model.travelomatrix.ResponseModels.UpdateFareQuotes;

import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateFareQuotesReply {

    @JsonProperty("Message")
    private String message;
    @JsonProperty("Status")
    private Long status;
    @JsonProperty("UpdateFareQuote")
    private UpdateFareQuote updateFareQuote;

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

    public UpdateFareQuote getUpdateFareQuote() {
        return updateFareQuote;
    }

    public void setUpdateFareQuote(UpdateFareQuote updateFareQuote) {
        this.updateFareQuote = updateFareQuote;
    }

}
