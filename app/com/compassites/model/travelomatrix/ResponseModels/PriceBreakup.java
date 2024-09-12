
package com.compassites.model.travelomatrix.ResponseModels;

import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PriceBreakup {

     @JsonProperty("AgentCommission")
    private Long agentCommission;
     @JsonProperty("AgentTdsOnCommision")
    private Long agentTdsOnCommision;
     @JsonProperty("BasicFare")
    private Long basicFare;
     @JsonProperty("Tax")
    private Long tax;
    @JsonProperty("YQValue")
    private String yQValue;
    @JsonProperty("CommissionEarned")
    private Long commissionEarned;
    @JsonProperty("PLBEarned")
    private Long plbEarned;
    @JsonProperty("TdsOnCommission")
    private Long tdsOnCommission;
    @JsonProperty("TdsOnPLB")
    private Long tdsOnPLB;

    public Long getAgentCommission() {
        return agentCommission;
    }

    public void setAgentCommission(Long agentCommission) {
        this.agentCommission = agentCommission;
    }

    public Long getAgentTdsOnCommision() {
        return agentTdsOnCommision;
    }

    public void setAgentTdsOnCommision(Long agentTdsOnCommision) {
        this.agentTdsOnCommision = agentTdsOnCommision;
    }

    public Long getBasicFare() {
        return basicFare;
    }

    public void setBasicFare(Long basicFare) {
        this.basicFare = basicFare;
    }

    public String getyQValue() {
        return yQValue;
    }

    public void setyQValue(String yQValue) {
        this.yQValue = yQValue;
    }

    public Long getCommissionEarned() {
        return commissionEarned;
    }

    public void setCommissionEarned(Long commissionEarned) {
        this.commissionEarned = commissionEarned;
    }

    public Long getPlbEarned() {
        return plbEarned;
    }

    public void setPlbEarned(Long plbEarned) {
        this.plbEarned = plbEarned;
    }

    public Long getTdsOnCommission() {
        return tdsOnCommission;
    }

    public void setTdsOnCommission(Long tdsOnCommission) {
        this.tdsOnCommission = tdsOnCommission;
    }

    public Long getTdsOnPLB() {
        return tdsOnPLB;
    }

    public void setTdsOnPLB(Long tdsOnPLB) {
        this.tdsOnPLB = tdsOnPLB;
    }

    public Long getTax() {
        return tax;
    }

    public void setTax(Long tax) {
        this.tax = tax;
    }

}
