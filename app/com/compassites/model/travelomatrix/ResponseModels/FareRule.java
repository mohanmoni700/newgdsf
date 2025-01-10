
package com.compassites.model.travelomatrix.ResponseModels;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.*;
@JsonIgnoreProperties(ignoreUnknown = true)
public class FareRule {

    @JsonProperty("FareRuleDetail")
    private List<FareRuleDetail> fareRuleDetail;

    public List<FareRuleDetail> getFareRuleDetail() {
        return fareRuleDetail;
    }

    public void setFareRuleDetail(List<FareRuleDetail> fareRuleDetail) {
        this.fareRuleDetail = fareRuleDetail;
    }
}
