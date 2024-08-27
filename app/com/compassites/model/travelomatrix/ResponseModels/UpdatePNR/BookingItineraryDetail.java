
package com.compassites.model.travelomatrix.ResponseModels.UpdatePNR;

import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
@JsonIgnoreProperties(ignoreUnknown = true)
public class BookingItineraryDetail {

    @JsonProperty("AirlinePNR")
    private String airlinePNR;
    @JsonProperty("DepartureDatetime")
    private String departureDatetime;
    @JsonProperty("FromAirlineCode")
    private String fromAirlineCode;
    @JsonProperty("ToAirlineCode")
    private String toAirlineCode;

    public String getAirlinePNR() {
        return airlinePNR;
    }

    public void setAirlinePNR(String airlinePNR) {
        this.airlinePNR = airlinePNR;
    }

    public String getDepartureDatetime() {
        return departureDatetime;
    }

    public void setDepartureDatetime(String departureDatetime) {
        this.departureDatetime = departureDatetime;
    }

    public String getFromAirlineCode() {
        return fromAirlineCode;
    }

    public void setFromAirlineCode(String fromAirlineCode) {
        this.fromAirlineCode = fromAirlineCode;
    }

    public String getToAirlineCode() {
        return toAirlineCode;
    }

    public void setToAirlineCode(String toAirlineCode) {
        this.toAirlineCode = toAirlineCode;
    }

}
