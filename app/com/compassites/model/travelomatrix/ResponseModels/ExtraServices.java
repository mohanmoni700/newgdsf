
package com.compassites.model.travelomatrix.ResponseModels;

import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ExtraServices {

    @JsonProperty("ExtraServiceDetails")
    private ExtraServiceDetails extraServiceDetails;

    public ExtraServiceDetails getExtraServiceDetails() {
        return extraServiceDetails;
    }

    public void setExtraServiceDetails(ExtraServiceDetails extraServiceDetails) {
        this.extraServiceDetails = extraServiceDetails;
    }

}
