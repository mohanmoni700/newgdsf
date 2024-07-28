
package com.compassites.model.travelomatrix.ResponseModels.UpdateFareQuotes;

import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;


public class JourneyList {

    @JsonProperty("Attr")
    private Attr attr;
    @JsonProperty("FlightDetails")
    private FlightDetails flightDetails;
    @JsonProperty("HoldTicket")
    private Boolean holdTicket;
    @JsonProperty("Price")
    private Price price;
    @JsonProperty("ResultToken")
    private String resultToken;

    public Attr getAttr() {
        return attr;
    }

    public void setAttr(Attr attr) {
        this.attr = attr;
    }

    public FlightDetails getFlightDetails() {
        return flightDetails;
    }

    public void setFlightDetails(FlightDetails flightDetails) {
        this.flightDetails = flightDetails;
    }

    public Boolean getHoldTicket() {
        return holdTicket;
    }

    public void setHoldTicket(Boolean holdTicket) {
        this.holdTicket = holdTicket;
    }

    public Price getPrice() {
        return price;
    }

    public void setPrice(Price price) {
        this.price = price;
    }

    public String getResultToken() {
        return resultToken;
    }

    public void setResultToken(String resultToken) {
        this.resultToken = resultToken;
    }

}
