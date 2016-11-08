package com.compassites.model;

import java.math.BigDecimal;

/**
 * Created by yaseen on 03-11-2016.
 */
public class TSTPrice {

    private BigDecimal basePrice;

    private BigDecimal TotalPrice;

    private int maxBaggageWeight;

    private int baggageCount;

    private String bookingClass;

    private String passengerType;

    PassengerTax passengerTax;

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    public BigDecimal getTotalPrice() {
        return TotalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        TotalPrice = totalPrice;
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
        this.passengerType = passengerType;
    }

    public PassengerTax getPassengerTax() {
        return passengerTax;
    }

    public void setPassengerTax(PassengerTax passengerTax) {
        this.passengerTax = passengerTax;
    }
}
