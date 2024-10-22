package com.compassites.model;

import java.util.List;

public class MealDetails {
    private Long contactMasterId;
    private List<String> mealId;

    public Long getContactMasterId() {
        return contactMasterId;
    }

    public void setContactMasterId(Long contactMasterId) {
        this.contactMasterId = contactMasterId;
    }

    public List<String> getMealId() {
        return mealId;
    }

    public void setMealId(List<String> mealId) {
        this.mealId = mealId;
    }
}
