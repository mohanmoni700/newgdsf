
package com.compassites.model.travelomatrix.ResponseModels.CommitBookingReply;

import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;


public class JourneyList {

    @JsonProperty("FlightDetails")
    private FlightDetails flightDetails;

    public FlightDetails getFlightDetails() {
        return flightDetails;
    }

    public void setFlightDetails(FlightDetails flightDetails) {
        this.flightDetails = flightDetails;
    }

}
