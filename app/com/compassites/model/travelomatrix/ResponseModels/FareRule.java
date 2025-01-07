
package com.compassites.model.travelomatrix.ResponseModels;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.*;
@JsonIgnoreProperties(ignoreUnknown = true)
public class FareRule {

    @JsonProperty("FareRuleDetail")
    private Map<String, FareRuleDetail> fareRuleDetail;

    public Map<String, FareRuleDetail> getFareRuleDetail() {
        return fareRuleDetail;
    }

    public void setFareRuleDetail(Map<String, FareRuleDetail> fareRuleDetail) {
        this.fareRuleDetail = fareRuleDetail;
    }
}
