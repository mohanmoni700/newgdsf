
package com.compassites.model.travelomatrix.ResponseModels.HoldTicket;

import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;


public class PriceBreakup {

    @JsonProperty("AgentCommission")
    private Long agentCommission;
    @JsonProperty("AgentTdsOnCommision")
    private Long agentTdsOnCommision;
    @JsonProperty("BasicFare")
    private Long basicFare;
    @JsonProperty("Tax")
    private Long tax;

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

    public Long getTax() {
        return tax;
    }

    public void setTax(Long tax) {
        this.tax = tax;
    }

}
