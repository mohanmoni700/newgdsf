
package com.compassites.model.travelomatrix.ResponseModels;

import java.util.List;
import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.gson.annotations.SerializedName;
import utils.TravelomatrixFareRuleDeserializer;
@JsonIgnoreProperties(ignoreUnknown = true)
public class FareRuleDetail {

    @JsonProperty("Airline")
    private String airline;
    @JsonProperty("CancellationCharge")
    @JsonDeserialize(using = TravelomatrixFareRuleDeserializer.class)
    private List<Rule> cancellationCharge;
    @JsonProperty("DateChange")
   @JsonDeserialize(using = TravelomatrixFareRuleDeserializer.class)
    private List<Rule> dateChange;
    @JsonProperty("Destination")
    private String destination;
    @JsonProperty("FareRules")
    private String fareRules;
    @JsonProperty("NoShowCharge")
    @JsonDeserialize(using = TravelomatrixFareRuleDeserializer.class)
    private List<Rule> noShowCharge;
    @JsonProperty("Origin")
    private String origin;
    @JsonProperty("SeatCharge")
    private List<Object> seatCharge;

    public String getAirline() {
        return airline;
    }

    public void setAirline(String airline) {
        this.airline = airline;
    }

    public List<Rule> getCancellationCharge() {
        return cancellationCharge;
    }

    public void setCancellationCharge(List<Rule> cancellationCharge) {
        this.cancellationCharge = cancellationCharge;
    }

    public List<Rule> getDateChange() {
        return dateChange;
    }

    public void setDateChange(List<Rule> dateChange) {
        this.dateChange = dateChange;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getFareRules() {
        return fareRules;
    }

    public void setFareRules(String fareRules) {
        this.fareRules = fareRules;
    }

    public List<Rule> getNoShowCharge() {
        return noShowCharge;
    }

    public void setNoShowCharge(List<Rule> noShowCharge) {
        this.noShowCharge = noShowCharge;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public List<Object> getSeatCharge() {
        return seatCharge;
    }

    public void setSeatCharge(List<Object> seatCharge) {
        this.seatCharge = seatCharge;
    }

}
