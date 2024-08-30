
package com.compassites.model.travelomatrix.ResponseModels;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DateChange {

    @JsonProperty("amount")
    private Long amount;
    @JsonProperty("end_time")
    private String endTime;
    @JsonProperty("policyInfo")
    private String policyInfo;
    @JsonProperty("start_time")
    private String startTime;
    @JsonProperty("tax")
    private Long tax;



    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getPolicyInfo() {
        return policyInfo;
    }

    public void setPolicyInfo(String policyInfo) {
        this.policyInfo = policyInfo;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public Long getTax() {
        return tax;
    }

    public void setTax(Long tax) {
        this.tax = tax;
    }

}
