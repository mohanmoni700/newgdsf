package com.compassites.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Renu
 * Date: 5/20/14
 * Time: 4:35 PM
 * To change this template use File | Settings | File Templates.
 */
public class SearchParameters {
    private String origin;
    private String destination;
    private String stopOver;
    private String currency;
    private List <Passenger> passengers;
    private Boolean withReturnJourney;
    private Integer noOfStops;
    private JourneySpecificParameters onwardJourney;
    private JourneySpecificParameters returnJourney;
    private Integer adultCount;
    private Integer childCount;
    private Integer infantCount;

    public JourneySpecificParameters getOnwardJourney() {
        return onwardJourney;
    }

    public void setOnwardJourney(JourneySpecificParameters onwardJourney) {
        this.onwardJourney = onwardJourney;
    }

    public JourneySpecificParameters getReturnJourney() {
        return returnJourney;
    }

    public void setReturnJourney(JourneySpecificParameters returnJourney) {
        this.returnJourney = returnJourney;
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

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Boolean getWithReturnJourney() {
        return withReturnJourney;
    }

    public void setWithReturnJourney(Boolean withReturnJourney) {
        this.withReturnJourney = withReturnJourney;
    }

    public Integer getNoOfStops() {
        return noOfStops;
    }

    public void setNoOfStops(Integer noOfStops) {
        this.noOfStops = noOfStops;
    }

    public SearchParameters(){

        withReturnJourney = false;
        currency = "INR";
        onwardJourney = new JourneySpecificParameters();
        returnJourney = new JourneySpecificParameters();
        passengers = new ArrayList<Passenger>();

    }

    public List<Passenger> getPassengers() {
        return passengers;
    }

    public void setPassengers(List<Passenger> passengers) {
        this.passengers = passengers;
    }

    public String getStopOver() {
        return stopOver;
    }

    public void setStopOver(String stopOver) {
        this.stopOver = stopOver;
    }

    public Integer getAdultCount() {
        return adultCount;
    }

    public void setAdultCount(Integer adultCount) {
        this.adultCount = adultCount;
    }

    public Integer getChildCount() {
        return childCount;
    }

    public void setChildCount(Integer childCount) {
        this.childCount = childCount;
    }

    public Integer getInfantCount() {
        return infantCount;
    }

    public void setInfantCount(Integer infantCount) {
        this.infantCount = infantCount;
    }

    public class JourneySpecificParameters{

        public CabinClass getCabinClass() {
            return cabinClass;
        }

        public void setCabinClass(CabinClass cabinClass) {
            this.cabinClass = cabinClass;
        }

        public String getPreferredAirlineCode() {
            return preferredAirlineCode;
        }

        public void setPreferredAirlineCode(String preferredAirlineCode) {
            this.preferredAirlineCode = preferredAirlineCode;
        }

        public String getPreferredFood() {
            return preferredFood;
        }

        public void setPreferredFood(String preferredFood) {
            this.preferredFood = preferredFood;
        }

        private CabinClass cabinClass;
        private String preferredAirlineCode;
        private String preferredFood;
        private Date journeyDate;

        public JourneySpecificParameters(){
            cabinClass = CabinClass.ECONOMY;
        }

        public Date getJourneyDate() {
            return journeyDate;
        }

        public void setJourneyDate(Date journeyDate) {
            this.journeyDate = journeyDate;
        }
    }

    public String redisKey(){
        Date journeyDate = onwardJourney.getJourneyDate();
        return origin+destination+"ADT"+adultCount+"CHD"+childCount+"INF"+infantCount+journeyDate.getDate()+journeyDate.getMonth()+journeyDate.getYear()+onwardJourney.getCabinClass().upperValue();
    }
}