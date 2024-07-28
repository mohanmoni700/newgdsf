
package com.compassites.model.travelomatrix.ResponseModels.UpdateFareQuotes;

import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;


public class Price {

    @JsonProperty("Currency")
    private String currency;
    @JsonProperty("PassengerBreakup")
    private PassengerBreakup passengerBreakup;
    @JsonProperty("PriceBreakup")
    private PriceBreakup priceBreakup;
    @JsonProperty("TotalDisplayFare")
    private Double totalDisplayFare;

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public PassengerBreakup getPassengerBreakup() {
        return passengerBreakup;
    }

    public void setPassengerBreakup(PassengerBreakup passengerBreakup) {
        this.passengerBreakup = passengerBreakup;
    }

    public PriceBreakup getPriceBreakup() {
        return priceBreakup;
    }

    public void setPriceBreakup(PriceBreakup priceBreakup) {
        this.priceBreakup = priceBreakup;
    }

    public Double getTotalDisplayFare() {
        return totalDisplayFare;
    }

    public void setTotalDisplayFare(Double totalDisplayFare) {
        this.totalDisplayFare = totalDisplayFare;
    }

}
