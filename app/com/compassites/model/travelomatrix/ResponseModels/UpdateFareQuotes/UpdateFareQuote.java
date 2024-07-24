
package com.compassites.model.travelomatrix.ResponseModels.UpdateFareQuotes;

import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;


public class UpdateFareQuote {

    @JsonProperty("FareQuoteDetails")
    private FareQuoteDetails fareQuoteDetails;

    public FareQuoteDetails getFareQuoteDetails() {
        return fareQuoteDetails;
    }

    public void setFareQuoteDetails(FareQuoteDetails fareQuoteDetails) {
        this.fareQuoteDetails = fareQuoteDetails;
    }

}
