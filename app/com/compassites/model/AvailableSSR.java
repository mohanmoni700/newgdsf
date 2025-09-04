package com.compassites.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Date;

public class AvailableSSR {

    private String origin;

    private String destination;

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

    public Date getDepartureDate() {
        return departureDate;
    }

    public void setDepartureDate(Date departureDate) {
        this.departureDate = departureDate;
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public void setFlightNumber(String flightNumber) {
        this.flightNumber = flightNumber;
    }

    public String getCarrierCode() {
        return carrierCode;
    }

    public void setCarrierCode(String carrierCode) {
        this.carrierCode = carrierCode;
    }

    private Date departureDate;

    private String flightNumber;

    private String carrierCode;


    private String ssrCode;
    private String ssrDescription;
    private String ssrType;
    private String actionStatusCode;
    private BigDecimal ssrPrice;
    private String ssrCurrency;
    private String ssrFeeType;
    private List<AvailableSSRServiceCharges> availableSSRServiceCharges;


    public List<AvailableSSRServiceCharges> getAvailableSSRServiceCharges() {
        return availableSSRServiceCharges;
    }

    public void setAvailableSSRServiceCharges(List<AvailableSSRServiceCharges> availableSSRServiceCharges) {
        this.availableSSRServiceCharges = availableSSRServiceCharges;
    }

    public String getSsrCode() {
        return ssrCode;
    }

    public void setSsrCode(String ssrCode) {
        this.ssrCode = ssrCode;
    }

    public String getSsrDescription() {
        return ssrDescription;
    }

    public void setSsrDescription(String ssrDescription) {
        this.ssrDescription = ssrDescription;
    }

    public String getSsrType() {
        return ssrType;
    }

    public void setSsrType(String ssrType) {
        this.ssrType = ssrType;
    }

    public String getActionStatusCode() {
        return actionStatusCode;
    }

    public void setActionStatusCode(String actionStatusCode) {
        this.actionStatusCode = actionStatusCode;
    }

    public BigDecimal getSsrPrice() {
        return ssrPrice;
    }

    public void setSsrPrice(BigDecimal ssrPrice) {
        this.ssrPrice = ssrPrice;
    }

    public String getSsrCurrency() {
        return ssrCurrency;
    }

    public void setSsrCurrency(String ssrCurrency) {
        this.ssrCurrency = ssrCurrency;
    }

    public String getSsrFeeType() {
        return ssrFeeType;
    }

    public void setSsrFeeType(String ssrFeeType) {
        this.ssrFeeType = ssrFeeType;
    }
}
