
package com.compassites.model.travelomatrix.ResponseModels.UpdateFareQuotes;

import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;


public class Origin {

    @JsonProperty("AirportCode")
    private String airportCode;
    @JsonProperty("AirportName")
    private String airportName;
    @JsonProperty("CityName")
    private String cityName;
    @JsonProperty("DateTime")
    private String dateTime;
    @JsonProperty("FDTV")
    private Long fDTV;
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

    public Long getFDTV() {
        return fDTV;
    }

    public void setFDTV(Long fDTV) {
        this.fDTV = fDTV;
    }

    public Object getTerminal() {
        return terminal;
    }

    public void setTerminal(Object terminal) {
        this.terminal = terminal;
    }

}
