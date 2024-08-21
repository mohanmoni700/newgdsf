package com.compassites.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MnrSearchBaggage implements Serializable {

    private String provider;
    private String allowedBaggage;

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getAllowedBaggage() {
        return allowedBaggage;
    }

    public void setAllowedBaggage(String allowedBaggage) {
        this.allowedBaggage = allowedBaggage;
    }

}
