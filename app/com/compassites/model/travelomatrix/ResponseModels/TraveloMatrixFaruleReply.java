
package com.compassites.model.travelomatrix.ResponseModels;


import com.fasterxml.jackson.annotation.JsonProperty;



public class TraveloMatrixFaruleReply {

    @JsonProperty("FareRule")
    private FareRule fareRule;
    @JsonProperty("Message")
    private String message;
    @JsonProperty("Status")
    private Long status;

    public FareRule getFareRule() {
        return fareRule;
    }

    public void setFareRule(FareRule fareRule) {
        this.fareRule = fareRule;
    }

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
