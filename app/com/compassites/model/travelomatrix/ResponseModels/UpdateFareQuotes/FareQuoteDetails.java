
package com.compassites.model.travelomatrix.ResponseModels.UpdateFareQuotes;

import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FareQuoteDetails {

    @JsonProperty("JourneyList")
    private JourneyList journeyList;

    public JourneyList getJourneyList() {
        return journeyList;
    }

    public void setJourneyList(JourneyList journeyList) {
        this.journeyList = journeyList;
    }

}
