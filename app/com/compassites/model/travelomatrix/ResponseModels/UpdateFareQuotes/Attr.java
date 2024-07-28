
package com.compassites.model.travelomatrix.ResponseModels.UpdateFareQuotes;

import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

public class Attr {

    @JsonProperty("AirlineRemark")
    private String airlineRemark;
    @JsonProperty("AvailableSeats")
    private String availableSeats;
    @JsonProperty("Baggage")
    private String baggage;
    @JsonProperty("CabinBaggage")
    private String cabinBaggage;
    @JsonProperty("FareType")
    private String fareType;
    @JsonProperty("IsLCC")
    private Boolean isLCC;
    @JsonProperty("IsRefundable")
    private Boolean isRefundable;

    public String getAirlineRemark() {
        return airlineRemark;
    }

    public void setAirlineRemark(String airlineRemark) {
        this.airlineRemark = airlineRemark;
    }

    public String getAvailableSeats() {
        return availableSeats;
    }

    public void setAvailableSeats(String availableSeats) {
        this.availableSeats = availableSeats;
    }

    public String getBaggage() {
        return baggage;
    }

    public void setBaggage(String baggage) {
        this.baggage = baggage;
    }

    public String getCabinBaggage() {
        return cabinBaggage;
    }

    public void setCabinBaggage(String cabinBaggage) {
        this.cabinBaggage = cabinBaggage;
    }

    public String getFareType() {
        return fareType;
    }

    public void setFareType(String fareType) {
        this.fareType = fareType;
    }

    public Boolean getIsLCC() {
        return isLCC;
    }

    public void setIsLCC(Boolean isLCC) {
        this.isLCC = isLCC;
    }

    public Boolean getIsRefundable() {
        return isRefundable;
    }

    public void setIsRefundable(Boolean isRefundable) {
        this.isRefundable = isRefundable;
    }

}
