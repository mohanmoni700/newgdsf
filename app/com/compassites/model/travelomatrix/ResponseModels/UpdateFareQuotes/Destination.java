
package com.compassites.model.travelomatrix.ResponseModels.UpdateFareQuotes;

import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;


public class Destination {

    @JsonProperty("AirportCode")
    private String airportCode;
    @JsonProperty("AirportName")
    private String airportName;
    @JsonProperty("CityName")
    private String cityName;
    @JsonProperty("DateTime")
    private String dateTime;
    @JsonProperty("FATV")
    private Long fATV;
    @JsonProperty("Terminal")
    private Object terminal;

    public String getAirportCode() {
        return airportCode;
    }

    public void setAirportCode(String airportCode) {
        this.airportCode = airportCode;
    }

    public String getAirportName() {
        return airportName;
    }

    public void setAirportName(String airportName) {
        this.airportName = airportName;
    }

    public String getCityName() {
        return cityName;
    }

    public void setCityName(String cityName) {
        this.cityName = cityName;
    }

    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    public Long getFATV() {
        return fATV;
    }

    public void setFATV(Long fATV) {
        this.fATV = fATV;
    }

    public Object getTerminal() {
        return terminal;
    }

    public void setTerminal(Object terminal) {
        this.terminal = terminal;
    }

}
