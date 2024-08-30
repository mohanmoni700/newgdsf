
package com.compassites.model.travelomatrix.ResponseModels.HoldTicket;

import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;
@JsonIgnoreProperties(ignoreUnknown = true)
public class Detail {

    @JsonProperty("AirlinePNR")
    private String airlinePNR;
    @JsonProperty("Attr")
    private Attr attr;
    @JsonProperty("CabinClass")
    private String cabinClass;
    @JsonProperty("Destination")
    private Destination destination;
    @JsonProperty("DisplayOperatorCode")
    private String displayOperatorCode;
    @JsonProperty("FlightNumber")
    private String flightNumber;
    @JsonProperty("OperatorCode")
    private String operatorCode;
    @JsonProperty("OperatorName")
    private String operatorName;
    @JsonProperty("Origin")
    private Origin origin;

    public String getAirlinePNR() {
        return airlinePNR;
    }

    public void setAirlinePNR(String airlinePNR) {
        this.airlinePNR = airlinePNR;
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

    public String getDisplayOperatorCode() {
        return displayOperatorCode;
    }

    public void setDisplayOperatorCode(String displayOperatorCode) {
        this.displayOperatorCode = displayOperatorCode;
    }

    public String getFlightNumber() {
        return flightNumber;
    }

    public void setFlightNumber(String flightNumber) {
        this.flightNumber = flightNumber;
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
