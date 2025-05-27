package dto;

import java.math.BigInteger;

public class FreeMealsDetails {

    private String mealName;
    private String mealCode;
    private String mealStatus;
    private String mealStatusDescription;
    private BigInteger mealQuantity;
    private String comments;
    private String amadeusPaxReference;
    private String paxName;
    private String airlineName;
    private String airlineCode;
    private String flightNumber;
    private String amadeusSegmentRef;
    private String origin;
    private String destination;

    public String getAmadeusSegmentRef() {
        return amadeusSegmentRef;
    }

    public void setAmadeusSegmentRef(String amadeusSegmentRef) {
        this.amadeusSegmentRef = amadeusSegmentRef;
    }

    public String getAirlineName() {
        return airlineName;
    }

    public void setAirlineName(String airlineName) {
        this.airlineName = airlineName;
    }

    public String getAirlineCode() {
        return airlineCode;
    }

    public void setAirlineCode(String airlineCode) {
        this.airlineCode = airlineCode;
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public void setFlightNumber(String flightNumber) {
        this.flightNumber = flightNumber;
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


    public String getMealName() {
        return mealName;
    }

    public void setMealName(String mealName) {
        this.mealName = mealName;
    }

    public String getMealCode() {
        return mealCode;
    }

    public void setMealCode(String mealCode) {
        this.mealCode = mealCode;
    }

    public String getMealStatus() {
        return mealStatus;
    }

    public void setMealStatus(String mealStatus) {
        this.mealStatus = mealStatus;
    }

    public BigInteger getMealQuantity() {
        return mealQuantity;
    }

    public void setMealQuantity(BigInteger mealQuantity) {
        this.mealQuantity = mealQuantity;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getAmadeusPaxReference() {
        return amadeusPaxReference;
    }

    public void setAmadeusPaxReference(String amadeusPaxReference) {
        this.amadeusPaxReference = amadeusPaxReference;
    }

    public String getPaxName() {
        return paxName;
    }

    public void setPaxName(String paxName) {
        this.paxName = paxName;
    }


    public String getMealStatusDescription() {
        return mealStatusDescription;
    }

    public void setMealStatusDescription(String mealStatusDescription) {
        this.mealStatusDescription = mealStatusDescription;
    }


}
