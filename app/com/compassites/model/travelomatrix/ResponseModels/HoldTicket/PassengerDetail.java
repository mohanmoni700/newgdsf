
package com.compassites.model.travelomatrix.ResponseModels.HoldTicket;

import javax.annotation.Generated;

import com.compassites.model.travelomatrix.ResponseModels.Baggage;
import com.compassites.model.travelomatrix.ResponseModels.Meal;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PassengerDetail {

    @JsonProperty("FirstName")
    private String firstName;
    @JsonProperty("LastName")
    private String lastName;
    @JsonProperty("PassengerId")
    private String passengerId;
    @JsonProperty("PassengerType")
    private String passengerType;
    @JsonProperty("TicketId")
    private String ticketId;
    @JsonProperty("TicketNumber")
    private Object ticketNumber;
    @JsonProperty("Title")
    private String title;
    @JsonProperty("Baggage")
    private List<Baggage> baggageList;
    @JsonProperty("Meal")
    private List<Meal> mealList;

    public List<Baggage> getBaggageList() {
        return baggageList;
    }

    public void setBaggageList(List<Baggage> baggageList) {
        this.baggageList = baggageList;
    }

    public List<Meal> getMealList() {
        return mealList;
    }

    public void setMealList(List<Meal> mealList) {
        this.mealList = mealList;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPassengerId() {
        return passengerId;
    }

    public void setPassengerId(String passengerId) {
        this.passengerId = passengerId;
    }

    public String getPassengerType() {
        return passengerType;
    }

    public void setPassengerType(String passengerType) {
        this.passengerType = passengerType;
    }

    public String getTicketId() {
        return ticketId;
    }

    public void setTicketId(String ticketId) {
        this.ticketId = ticketId;
    }

    public Object getTicketNumber() {
        return ticketNumber;
    }

    public void setTicketNumber(Object ticketNumber) {
        this.ticketNumber = ticketNumber;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

}
