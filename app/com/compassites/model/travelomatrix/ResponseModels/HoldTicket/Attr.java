
package com.compassites.model.travelomatrix.ResponseModels.HoldTicket;

import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
@JsonIgnoreProperties(ignoreUnknown = true)
public class Attr {

    @JsonProperty("AvailableSeats")
    private Object availableSeats;
    @JsonProperty("Baggage")
    private Object baggage;
    @JsonProperty("CabinBaggage")
    private Object cabinBaggage;

    public Object getAvailableSeats() {
        return availableSeats;
    }

    public void setAvailableSeats(Object availableSeats) {
        this.availableSeats = availableSeats;
    }

    public Object getBaggage() {
        return baggage;
    }

    public void setBaggage(Object baggage) {
        this.baggage = baggage;
    }

    public Object getCabinBaggage() {
        return cabinBaggage;
    }

    public void setCabinBaggage(Object cabinBaggage) {
        this.cabinBaggage = cabinBaggage;
    }

}
