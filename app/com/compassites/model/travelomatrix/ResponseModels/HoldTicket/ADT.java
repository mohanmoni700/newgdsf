
package com.compassites.model.travelomatrix.ResponseModels.HoldTicket;

import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ADT {

    @JsonProperty("BasePrice")
    private Long basePrice;
    @JsonProperty("PassengerCount")
    private String passengerCount;
    @JsonProperty("Tax")
    private Long tax;
    @JsonProperty("TotalPrice")
    private Long totalPrice;

    public Long getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(Long basePrice) {
        this.basePrice = basePrice;
    }

    public String getPassengerCount() {
        return passengerCount;
    }

    public void setPassengerCount(String passengerCount) {
        this.passengerCount = passengerCount;
    }

    public Long getTax() {
        return tax;
    }

    public void setTax(Long tax) {
        this.tax = tax;
    }

    public Long getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(Long totalPrice) {
        this.totalPrice = totalPrice;
    }

}
