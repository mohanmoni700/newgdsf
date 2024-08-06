
package com.compassites.model.travelomatrix.ResponseModels;

import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

@JsonIgnoreProperties(ignoreUnknown = true)
public class JourneyList {

    @JsonProperty("Attr")
    private Attr attr;
    @JsonProperty("FlightDetails")
    private FlightDetails flightDetails;
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

    @Override
    public String toString() {
        return "JourneyList{" +
                "attr=" + attr +
                ", flightDetails=" + flightDetails.toString() +
                ", price=" + price +
                ", resultToken='" + resultToken + '\'' +
                '}';
    }
}
