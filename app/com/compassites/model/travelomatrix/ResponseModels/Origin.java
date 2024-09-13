
package com.compassites.model.travelomatrix.ResponseModels;

import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

@JsonIgnoreProperties(ignoreUnknown = true)
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
    private String terminal;

    public String getTerminal() {
        return terminal;
    }

    public void setTerminal(String terminal) {
        this.terminal = terminal;
    }

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

    @Override
    public String toString() {
        return "Origin{" +
                "airportCode='" + airportCode + '\'' +
                ", airportName='" + airportName + '\'' +
                ", cityName='" + cityName + '\'' +
                ", dateTime='" + dateTime + '\'' +
                ", fDTV=" + fDTV +
                '}';
    }
}
