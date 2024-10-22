package com.compassites.model;

public class BaggageDetails{

    private Long contactMasterId;

    private String baggageId;

    private String code;

    private String destination;

    private String origin;

    private Long price;

    private String weight;

    public String getBaggageId() {
        return baggageId;
    }

    public void setBaggageId(String baggageId) {
        this.baggageId = baggageId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public Long getPrice() {
        return price;
    }

    public void setPrice(Long price) {
        this.price = price;
    }

    public String getWeight() {
        return weight;
    }

    public void setWeight(String weight) {
        this.weight = weight;
    }

    public Long getContactMasterId() {
        return contactMasterId;
    }

    public void setContactMasterId(Long contactMasterId) {
        this.contactMasterId = contactMasterId;
    }
}

