package com.compassites.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * Created by yaseen on 14-10-2015.
 */

public class SegmentPricing {

    private Long id;

    private BigDecimal totalPrice;

    private BigDecimal basePrice;

    private BigDecimal tax;

    private String segmentIds;

//    private Long contactId;

    private Long passengerCount;

    private Long bookingId;

    private String passengerType;

    private List<TaxDetails> taxDetailsList;      //store just the ids instead of the object, one to many relationship with the tax

    private PriceDetails priceDetails;  // store just the ids, many to one relation ship with the priceDetails

    private List<String> segmentKeysList;

    private PassengerTax passengerTax;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    public BigDecimal getTax() {
        return tax;
    }

    public void setTax(BigDecimal tax) {
        this.tax = tax;
    }

    public String getSegmentIds() {
        return segmentIds;
    }

    public void setSegmentIds(String segmentIds) {
        this.segmentIds = segmentIds;
    }

    public Long getPassengerCount() {
        return passengerCount;
    }

    public void setPassengerCount(Long passengerCount) {
        this.passengerCount = passengerCount;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public String getPassengerType() {
        return passengerType;
    }

    public void setPassengerType(String passengerType) {
        this.passengerType = passengerType;
    }

    public List<TaxDetails> getTaxDetailsList() {
        return taxDetailsList;
    }

    public void setTaxDetailsList(List<TaxDetails> taxDetailsList) {
        this.taxDetailsList = taxDetailsList;
    }

    public PriceDetails getPriceDetails() {
        return priceDetails;
    }

    public void setPriceDetails(PriceDetails priceDetails) {
        this.priceDetails = priceDetails;
    }

    public List<String> getSegmentKeysList() {
        return segmentKeysList;
    }

    public void setSegmentKeysList(List<String> segmentKeysList) {
        this.segmentKeysList = segmentKeysList;
    }

    public PassengerTax getPassengerTax() {
        return passengerTax;
    }

    public void setPassengerTax(PassengerTax passengerTax) {
        this.passengerTax = passengerTax;
    }
}
