
package com.compassites.model.travelomatrix.ResponseModels.CommitBookingReply;

import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PriceBreakup {
        @JsonProperty("AgentCommission")
        private Double agentCommission;
        @JsonProperty("AgentTdsOnCommision")
        private Double agentTdsOnCommision;
        @JsonProperty("BasicFare")
        private Long basicFare;
        @JsonProperty("CommissionEarned")
        private Long commissionEarned;
        @JsonProperty("PLBEarned")
        private Double pLBEarned;
        @JsonProperty("Tax")
        private Double tax;
        @JsonProperty("TdsOnCommission")
        private Long tdsOnCommission;
        @JsonProperty("TdsOnPLB")
        private Double tdsOnPLB;

        public Double getAgentCommission() {
        return agentCommission;
    }

        public void setAgentCommission(Double agentCommission) {
        this.agentCommission = agentCommission;
    }

        public Double getAgentTdsOnCommision() {
        return agentTdsOnCommision;
    }

        public void setAgentTdsOnCommision(Double agentTdsOnCommision) {
        this.agentTdsOnCommision = agentTdsOnCommision;
    }

        public Long getBasicFare() {
        return basicFare;
    }

        public void setBasicFare(Long basicFare) {
        this.basicFare = basicFare;
    }

        public Long getCommissionEarned() {
        return commissionEarned;
    }

        public void setCommissionEarned(Long commissionEarned) {
        this.commissionEarned = commissionEarned;
    }

        public Double getPLBEarned() {
        return pLBEarned;
    }

        public void setPLBEarned(Double pLBEarned) {
        this.pLBEarned = pLBEarned;
    }

        public Double getTax() {
        return tax;
    }

        public void setTax(Double tax) {
        this.tax = tax;
    }

        public Long getTdsOnCommission() {
        return tdsOnCommission;
    }

        public void setTdsOnCommission(Long tdsOnCommission) {
        this.tdsOnCommission = tdsOnCommission;
    }

        public Double getTdsOnPLB() {
        return tdsOnPLB;
    }

        public void setTdsOnPLB(Double tdsOnPLB) {
        this.tdsOnPLB = tdsOnPLB;
    }
 }

