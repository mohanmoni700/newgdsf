package com.compassites.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AncillaryServicesResponse {

    private boolean success;
    private ErrorMessage errorMessage;
    private String provider;
    private List<BaggageDetails> baggageList;
    private Map<String,List<BaggageDetails>> baggageMap;
    private HashMap<String,List<MealDetails>> mealDetailsMap;

    public Map<String, List<BaggageDetails>> getBaggageMap() {
        return baggageMap;
    }

    public void setBaggageMap(Map<String, List<BaggageDetails>> baggageMap) {
        this.baggageMap = baggageMap;
    }


    public HashMap<String, List<MealDetails>> getMealDetailsMap() {
        return mealDetailsMap;
    }

    public void setMealDetailsMap(HashMap<String, List<MealDetails>> mealDetailsMap) {
        this.mealDetailsMap = mealDetailsMap;
    }

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
