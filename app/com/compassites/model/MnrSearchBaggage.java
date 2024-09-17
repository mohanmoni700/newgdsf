package com.compassites.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

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


    public static Map<String, String> baggageCodes = new HashMap<>();

    static {
        baggageCodes.put("700", "KG");
        baggageCodes.put("K", "KG");
        baggageCodes.put("701", "Lb");
        baggageCodes.put("L", "Lb");
        baggageCodes.put("C", "Special Charge");
        baggageCodes.put("N", "PC");
        baggageCodes.put("S", "Size");
        baggageCodes.put("V", "Value");
        baggageCodes.put("W", "Weight");
    }

}
