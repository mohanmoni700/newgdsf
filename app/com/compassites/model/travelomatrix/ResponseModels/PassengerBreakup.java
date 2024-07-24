
package com.compassites.model.travelomatrix.ResponseModels;

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

    public CHD getcHD() {
        return cHD;
    }

    public void setcHD(CHD cHD) {
        this.cHD = cHD;
    }

    public INF getiNF() {
        return iNF;
    }

    public void setiNF(INF iNF) {
        this.iNF = iNF;
    }
}
