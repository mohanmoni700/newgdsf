package com.compassites.model;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Created by yaseen on 29-10-2016.
 * TST- Transitional Stored Ticket
 */
public class TSTLowestFare {

    private BigDecimal amount;

    private int maxBaggageWeight;

    private int baggageCount;

    private String bookingClass;

    private String passengerType;


    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public int getMaxBaggageWeight() {
        return maxBaggageWeight;
    }

    public void setMaxBaggageWeight(int maxBaggageWeight) {
        this.maxBaggageWeight = maxBaggageWeight;
    }

    public int getBaggageCount() {
        return baggageCount;
    }

    public void setBaggageCount(int baggageCount) {
        this.baggageCount = baggageCount;
    }

    public String getBookingClass() {
        return bookingClass;
    }

    public void setBookingClass(String bookingClass) {
        this.bookingClass = bookingClass;
    }

    public String getPassengerType() {
        return passengerType;
    }

    public void setPassengerType(String passengerType) {
        if("ch".equalsIgnoreCase(passengerType)){
            passengerType = "CHD";
        }
        if("in".equalsIgnoreCase(passengerType)){
            passengerType = "INF";
        }
        this.passengerType = passengerType;
    }
}
