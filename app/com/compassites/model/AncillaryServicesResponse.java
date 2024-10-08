package com.compassites.model;

import java.util.List;

public class AncillaryServicesResponse {

    private boolean success;
    private ErrorMessage errorMessage;
    private String provider;
    private List<BaggageDetails> baggageList;


    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public ErrorMessage getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(ErrorMessage errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public List<BaggageDetails> getBaggageList() {
        return baggageList;
    }

    public void setBaggageList(List<BaggageDetails> baggageList) {
        this.baggageList = baggageList;
    }
}
