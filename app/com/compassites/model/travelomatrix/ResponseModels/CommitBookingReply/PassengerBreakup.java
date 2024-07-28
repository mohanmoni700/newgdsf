
package com.compassites.model.travelomatrix.ResponseModels.CommitBookingReply;

import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

public class PassengerBreakup {

    @JsonProperty("ADT")
    private ADT aDT;
    @JsonProperty("CHD")
    private CHD cHD;
    @JsonProperty("INF")
    private INF iNF;

    public ADT getADT() {
        return aDT;
    }

    public void setADT(ADT aDT) {
        this.aDT = aDT;
    }

    public CHD getCHD() {
        return cHD;
    }

    public void setCHD(CHD cHD) {
        this.cHD = cHD;
    }

    public INF getINF() {
        return iNF;
    }

    public void setINF(INF iNF) {
        this.iNF = iNF;
    }

}
