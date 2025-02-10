package com.compassites.model;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.util.HashMap;
import java.util.Map;

public class MealDetailsMap {
    private Long ticketId;
    private Map<String, MealDetails> mealMap = new HashMap<>();

    // Getters and Setters
    public Long getTicketId() {
        return ticketId;
    }

    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
    }

    public Map<String, MealDetails> getMealMap() {
        return mealMap;
    }
    @JsonAnySetter
    public void handleDynamicProperty(String key, MealDetails value) {
        // Store dynamic properties in mealMap or elsewhere
        if (mealMap == null) {
            mealMap = new HashMap<>();
        }
        mealMap.put(key, value);
    }
}
