package com.compassites.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AncillaryServicesResponse {

    private boolean success;
    private ErrorMessage errorMessage;
    private String provider;
    private List<BaggageDetails> baggageList;
    private List<MealDetails> mealDetailsList;
    private Map<String,List<BaggageDetails>> baggageMap;
    private Map<String,List<MealDetails>> mealDetailsMap;
    private Map<String, List<AvailableSSR>> availableSSRMap;
    private List<AvailableSSR> availableSSRList;

    public Map<String, List<AvailableSSR>> getAvailableSSRMap() {
        return availableSSRMap;
    }

    public void setAvailableSSRMap(Map<String, List<AvailableSSR>> availableSSRMap) {
        this.availableSSRMap = availableSSRMap;
    }
    public List<AvailableSSR> getAvailableSSRList() {
        return availableSSRList;
    }

    public void setAvailableSSRList(List<AvailableSSR> availableSSRList) {
        this.availableSSRList = availableSSRList;
    }

    public Map<String, List<BaggageDetails>> getBaggageMap() {
        return baggageMap;
    }
    public void setBaggageMap(Map<String, List<BaggageDetails>> baggageMap) {
        this.baggageMap = baggageMap;
    }

    public Map<String, List<MealDetails>> getMealDetailsMap() {
        return mealDetailsMap;
    }

    public void setMealDetailsMap(Map<String, List<MealDetails>> mealDetailsMap) {
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

    public List<MealDetails> getMealDetailsList() {
        return mealDetailsList;
    }

    public void setMealDetailsList(List<MealDetails> mealDetailsList) {
        this.mealDetailsList = mealDetailsList;
    }

}
