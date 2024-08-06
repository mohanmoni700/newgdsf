
package com.compassites.model.travelomatrix.ResponseModels;

import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

public class Attr {

    @JsonProperty("AirlineRemark")
    private String airlineRemark;
    @JsonProperty("AvailableSeats")
    private Long availableSeats;
    @JsonProperty("Baggage")
    private String baggage;
    @JsonProperty("CabinBaggage")
    private String cabinBaggage;
    @JsonProperty("IsLCC")
    private Boolean isLCC;
    @JsonProperty("IsRefundable")
    private Boolean isRefundable;
    @JsonProperty("FareType")
    private String fareType;
    @JsonProperty("LastTicketDate")
    private String lastTicketDate;
    @JsonProperty("ExtraBaggage")
    private boolean extraBaggage;

    public boolean isExtraBaggage() {
        return extraBaggage;
    }

    public void setExtraBaggage(boolean extraBaggage) {
        this.extraBaggage = extraBaggage;
    }

    public String getLastTicketDate() {
        return lastTicketDate;
    }

    public void setLastTicketDate(String lastTicketDate) {
        this.lastTicketDate = lastTicketDate;
    }

    public String getFareType() {
        return fareType;
    }

    public void setFareType(String fareType) {
        this.fareType = fareType;
    }

    public String getAirlineRemark() {
        return airlineRemark;
    }

    public void setAirlineRemark(String airlineRemark) {
        this.airlineRemark = airlineRemark;
    }

    public Long getAvailableSeats() {
        return availableSeats;
    }

    public void setAvailableSeats(Long availableSeats) {
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
