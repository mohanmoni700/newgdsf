
package com.compassites.model.travelomatrix.ResponseModels;

import java.util.List;
import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FlightDataList {

    @JsonProperty("JourneyList")
    private List<List<JourneyList>> journeyList;

    public List<List<JourneyList>> getJourneyList() {
        return journeyList;
    }

    public void setJourneyList(List<List<JourneyList>> journeyList) {
        this.journeyList = journeyList;
    }

    @Override
    public String toString() {
        return "FlightDataList{" +
                "journeyList=" + journeyList.toString() +
                '}';
    }
}
