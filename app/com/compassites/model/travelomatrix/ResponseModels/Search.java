
package com.compassites.model.travelomatrix.ResponseModels;

import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;


@JsonIgnoreProperties(ignoreUnknown = true)
public class Search {

    @JsonProperty("FlightDataList")
    private FlightDataList flightDataList;

    public FlightDataList getFlightDataList() {
        return flightDataList;
    }

    public void setFlightDataList(FlightDataList flightDataList) {
        this.flightDataList = flightDataList;
    }

    @Override
    public String toString() {
        return "Search{" +
                "flightDataList=" + flightDataList +
                '}';
    }
}
