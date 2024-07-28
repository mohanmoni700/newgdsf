
package com.compassites.model.travelomatrix.ResponseModels.HoldTicket;

import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;


public class PassengerBreakup {

    @JsonProperty("ADT")
    private ADT aDT;

    public ADT getADT() {
        return aDT;
    }

    public void setADT(ADT aDT) {
        this.aDT = aDT;
    }

}
