
package com.compassites.model.travelomatrix.ResponseModels.UpdateFareQuotes;

import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

public class Detail {

    @JsonProperty("Attr")
    private Attr attr;
    @JsonProperty("CabinClass")
    private String cabinClass;
    @JsonProperty("Destination")
    private Destination destination;
    @JsonProperty("Duration")
    private Long duration;
    @JsonProperty("FlightNumber")
    private String flightNumber;
    @JsonProperty("Operatedbyairline")
    private String operatedbyairline;
    @JsonProperty("Operatedbyairlinename")
    private String operatedbyairlinename;
    @JsonProperty("OperatorCode")
    private String operatorCode;
    @JsonProperty("OperatorName")
    private String operatorName;
    @JsonProperty("DisplayOperatorCode")
    private String displayOperatorCode;
    @JsonProperty("Origin")
    private Origin origin;
    @JsonProperty("ValidatingAirline")
    private String validatingAirline;
    @JsonProperty("stop_over")
    private String stopOver;

    @JsonProperty("LayOverTime")
    private Long layOverTime;

    public String getStopOver() {
        return stopOver;
    }

    public void setStopOver(String stopOver) {
        this.stopOver = stopOver;
    }

    public Long getLayOverTime() {
        return layOverTime;
    }

    public void setLayOverTime(Long layOverTime) {
        this.layOverTime = layOverTime;
    }





    public String getValidatingAirline() {
        return validatingAirline;
    }
    public void setValidatingAirline(String validatingAirline) {
        this.validatingAirline = validatingAirline;
    }

    public String getDisplayOperatorCode() {
        return displayOperatorCode;
    }

    public void setDisplayOperatorCode(String displayOperatorCode) {
        this.displayOperatorCode = displayOperatorCode;
    }

    public Attr getAttr() {
        return attr;
    }

    public void setAttr(Attr attr) {
        this.attr = attr;
    }

    public String getCabinClass() {
        return cabinClass;
    }

    public void setCabinClass(String cabinClass) {
        this.cabinClass = cabinClass;
    }

    public Destination getDestination() {
        return destination;
    }

    public void setDestination(Destination destination) {
        this.destination = destination;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public void setFlightNumber(String flightNumber) {
        this.flightNumber = flightNumber;
    }

    public String getOperatedbyairline() {
        return operatedbyairline;
    }

    public void setOperatedbyairline(String operatedbyairline) {
        this.operatedbyairline = operatedbyairline;
    }

    public String getOperatedbyairlinename() {
        return operatedbyairlinename;
    }

    public void setOperatedbyairlinename(String operatedbyairlinename) {
        this.operatedbyairlinename = operatedbyairlinename;
    }

    public String getOperatorCode() {
        return operatorCode;
    }

    public void setOperatorCode(String operatorCode) {
        this.operatorCode = operatorCode;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    public Origin getOrigin() {
        return origin;
    }

    public void setOrigin(Origin origin) {
        this.origin = origin;
    }

}
