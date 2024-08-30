
package com.compassites.model.travelomatrix.ResponseModels;

import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Detail {

     @JsonProperty("Attr")
    private Attr attr;
     @JsonProperty("CabinClass")
    private String cabinClass;
     @JsonProperty("Destination")
    private Destination destination;
     @JsonProperty("DisplayOperatorCode")
    private String displayOperatorCode;
     @JsonProperty("Duration")
    private Long duration;
    @JsonProperty("AccumulatedDuration")
     private Long accumulatedDuration;
     @JsonProperty("FlightNumber")
    private String flightNumber;
     @JsonProperty("Operatedbyairline")
    private String operatedbyairline;
     @JsonProperty("Operatedbyairlinename")
    private Object operatedbyairlinename;
     @JsonProperty("OperatorCode")
    private String operatorCode;
     @JsonProperty("OperatorName")
    private String operatorName;
     @JsonProperty("Origin")
    private Origin origin;
     @JsonProperty("stop_over")
    private String stopOver;
    @JsonProperty("LayOverTime")
    private Long layOverTime;
     @JsonProperty("ValidatingAirline")
    private String validatingAirline;

    public Attr getAttr() {
        return attr;
    }

    public void setAttr(Attr attr) {
        this.attr = attr;
    }


    public Long getAccumulatedDuration() {
        return accumulatedDuration;
    }

    public void setAccumulatedDuration(Long accumulatedDuration) {
        this.accumulatedDuration = accumulatedDuration;
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

    public String getDisplayOperatorCode() {
        return displayOperatorCode;
    }

    public void setDisplayOperatorCode(String displayOperatorCode) {
        this.displayOperatorCode = displayOperatorCode;
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

    public Object getOperatedbyairlinename() {
        return operatedbyairlinename;
    }

    public void setOperatedbyairlinename(Object operatedbyairlinename) {
        this.operatedbyairlinename = operatedbyairlinename;
    }

    public Long getLayOverTime() {
        return layOverTime;
    }

    public void setLayOverTime(Long layOverTime) {
        this.layOverTime = layOverTime;
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

    public String getStopOver() {
        return stopOver;
    }

    public void setStopOver(String stopOver) {
        this.stopOver = stopOver;
    }

    public String getValidatingAirline() {
        return validatingAirline;
    }

    public void setValidatingAirline(String validatingAirline) {
        this.validatingAirline = validatingAirline;
    }

    @Override
    public String toString() {
        return "Detail{" +
                "attr=" + attr +
                ", cabinClass='" + cabinClass + '\'' +
                ", destination=" + destination.toString() +
                ", displayOperatorCode='" + displayOperatorCode + '\'' +
                ", duration=" + duration +
                ", flightNumber='" + flightNumber + '\'' +
                ", operatedbyairline='" + operatedbyairline + '\'' +
                ", operatedbyairlinename=" + operatedbyairlinename +
                ", operatorCode='" + operatorCode + '\'' +
                ", operatorName='" + operatorName + '\'' +
                ", origin=" + origin.toString() +
                ", stopOver='" + stopOver + '\'' +
                ", layOverTime=" + layOverTime +
                ", validatingAirline='" + validatingAirline + '\'' +
                '}';
    }
}
