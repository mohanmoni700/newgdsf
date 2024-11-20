package com.compassites.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;

public class MealDetails {
    private Long contactMasterId;
    private String segment;
    private BigDecimal mealPrice;
    private String mealId;
    private String mealType;
    private String mealDesc;
    private Long ticketId;
    private String origin;

    private String tmxTicketNumber;

    private String mealCode;

    private Boolean returnDetails;

    public Boolean getReturnDetails() {
        return returnDetails;
    }

    public void setReturnDetails(Boolean returnDetails) {
        this.returnDetails = returnDetails;
    }



    public String getMealCode() {
        return mealCode;
    }

    public void setMealCode(String mealCode) {
        this.mealCode = mealCode;
    }

    public String getTmxTicketNumber() {
        return tmxTicketNumber;
    }

    public void setTmxTicketNumber(String tmxTicketNumber) {
        this.tmxTicketNumber = tmxTicketNumber;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    private String destination;
    public Long getTicketId() {
        return ticketId;
    }

    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
    }
    public String getMealDesc() {
        return mealDesc;
    }

    public void setMealDesc(String mealDesc) {
        this.mealDesc = mealDesc;
    }

    public Long getContactMasterId() {
        return contactMasterId;
    }

    public void setContactMasterId(Long contactMasterId) {
        this.contactMasterId = contactMasterId;
    }

    public String getSegment() {
        return segment;
    }

    public void setSegment(String segment) {
        this.segment = segment;
    }

    public BigDecimal getMealPrice() {
        return mealPrice;
    }

    public void setMealPrice(BigDecimal mealPrice) {
        this.mealPrice = mealPrice;
    }

    public String getMealId() {
        return mealId;
    }

    public void setMealId(String mealId) {
        this.mealId = mealId;
    }

    public String getMealType() {
        return mealType;
    }

    public void setMealType(String mealType) {
        this.mealType = mealType;
    }
}
