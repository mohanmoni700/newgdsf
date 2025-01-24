package com.compassites.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;

public class MealDetails {
    private Long contactMasterId;
    private String segment;
    private Long ticketId;
    private String tmxTicketNumber;
    private Boolean returnDetails;
    private String segmentNumber;

    private String mealId;
    private String mealType;
    private String origin;
    private String destination;
    private String mealCode;
    private String mealDesc;
    private BigDecimal mealPrice;
    private BigInteger availability;
    private String rfic;
    private String rfisc;
    private boolean isRefundable;
    private boolean isMIF;
    private String bkm;
    private String serviceId;
    private String carrierCode;
    private boolean isFMT;
    private boolean isFTXT;

    public BigInteger getAvailability() {
        return availability;
    }

    public void setAvailability(BigInteger availability) {
        this.availability = availability;
    }

    public String getRfic() {
        return rfic;
    }

    public void setRfic(String rfic) {
        this.rfic = rfic;
    }

    public String getRfisc() {
        return rfisc;
    }

    public void setRfisc(String rfisc) {
        this.rfisc = rfisc;
    }

    public boolean isRefundable() {
        return isRefundable;
    }

    public void setRefundable(boolean refundable) {
        isRefundable = refundable;
    }

    public boolean isMIF() {
        return isMIF;
    }

    public void setMIF(boolean MIF) {
        isMIF = MIF;
    }

    public String getBkm() {
        return bkm;
    }

    public void setBkm(String bkm) {
        this.bkm = bkm;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getCarrierCode() {
        return carrierCode;
    }

    public void setCarrierCode(String carrierCode) {
        this.carrierCode = carrierCode;
    }

    public boolean isFMT() {
        return isFMT;
    }

    public void setFMT(boolean FMT) {
        isFMT = FMT;
    }

    public boolean isFTXT() {
        return isFTXT;
    }

    public void setFTXT(boolean FTXT) {
        isFTXT = FTXT;
    }

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

    public String getSegmentNumber() {
        return segmentNumber;
    }

    public void setSegmentNumber(String segmentNumber) {
        this.segmentNumber = segmentNumber;
    }
}
