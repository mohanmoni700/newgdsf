package com.compassites.model.travelomatrix;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class FareRuleRequest implements Serializable {
    @JsonProperty("ResultToken")
    public String resultToken;

    public String getResultToken() {
        return resultToken;
    }

    public void setResultToken(String resultToken) {
        this.resultToken = resultToken;
    }
}
