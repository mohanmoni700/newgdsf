
package com.compassites.model.travelomatrix.ResponseModels.UpdateFareQuotes;

import java.util.List;
import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;


public class FlightDetails {

    @JsonProperty("Details")
    private List<List<Detail>> details;

    public List<List<Detail>> getDetails() {
        return details;
    }

    public void setDetails(List<List<Detail>> details) {
        this.details = details;
    }

}
