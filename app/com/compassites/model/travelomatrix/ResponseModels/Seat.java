
package com.compassites.model.travelomatrix.ResponseModels;

import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Seat {

    @JsonProperty("AirlineCode")
    private String airlineCode;
    @JsonProperty("AvailablityType")
    private Long availablityType;
    @JsonProperty("Destination")
    private String destination;
    @JsonProperty("FlightNumber")
    private String flightNumber;
    @JsonProperty("Origin")
    private String origin;
    @JsonProperty("Price")
    private Long price;
    @JsonProperty("RowNumber")
    private String rowNumber;
    @JsonProperty("SeatId")
    private String seatId;
    @JsonProperty("SeatNumber")
    private String seatNumber;
    @JsonProperty("SeatTypes")
    private String seatTypes;

    public String getAirlineCode() {
        return airlineCode;
    }

    public void setAirlineCode(String airlineCode) {
        this.airlineCode = airlineCode;
    }

    public Long getAvailablityType() {
        return availablityType;
    }

    public void setAvailablityType(Long availablityType) {
        this.availablityType = availablityType;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
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

    public Long getPrice() {
        return price;
    }

    public void setPrice(Long price) {
        this.price = price;
    }

    public String getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(String rowNumber) {
        this.rowNumber = rowNumber;
    }

    public String getSeatId() {
        return seatId;
    }

    public void setSeatId(String seatId) {
        this.seatId = seatId;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }

    public String getSeatTypes() {
        return seatTypes;
    }

    public void setSeatTypes(String seatTypes) {
        this.seatTypes = seatTypes;
    }

}
