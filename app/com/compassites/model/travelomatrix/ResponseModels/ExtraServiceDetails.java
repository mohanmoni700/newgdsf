
package com.compassites.model.travelomatrix.ResponseModels;

import java.util.List;
import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ExtraServiceDetails {

    @JsonProperty("Baggage")
    private List<List<Baggage>> baggage;
    @JsonProperty("Meals")
    private List<List<Meal>> meals;
    @JsonProperty("MealPreference")
    private List<List<Meal>> mealPreference;

    @JsonProperty("Seat")
    private List<List<List<Seat>>> seat;

    public List<List<Baggage>> getBaggage() {
        return baggage;
    }

    public void setBaggage(List<List<Baggage>> baggage) {
        this.baggage = baggage;
    }

    public List<List<Meal>> getMeals() {
        return meals;
    }

    public void setMeals(List<List<Meal>> meals) {
        this.meals = meals;
    }

    public List<List<Meal>> getMealPreference() {
        return mealPreference;
    }

    public void setMealPreference(List<List<Meal>> mealPreference) {
        this.mealPreference = mealPreference;
    }

    public List<List<List<Seat>>> getSeat() {
        return seat;
    }

    public void setSeat(List<List<List<Seat>>> seat) {
        this.seat = seat;
    }

}
