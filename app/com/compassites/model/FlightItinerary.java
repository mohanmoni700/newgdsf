package com.compassites.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Renu
 * Date: 5/23/14
 * Time: 4:30 PM
 * To change this template use File | Settings | File Templates.
 */
public class FlightItinerary implements Serializable{

    public FlightItinerary(){
        journeyList = new ArrayList<Journey>();
        pricingInformation = new PricingInformation();
    }

    private String provider; //travelport or amadeus
    private PricingInformation pricingInformation;

    public List<Journey> getJourneyList() {
        return journeyList;
    }

    public void setJourneyList(List<Journey> journeyList) {
        this.journeyList = journeyList;
    }

    private List<Journey> journeyList;

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }


    public PricingInformation getPricingInformation() {
        return pricingInformation;
    }

    public void setPricingInformation(PricingInformation pricingInformation) {
        this.pricingInformation = pricingInformation;
    }

    public void AddBlankJourney(){
        Journey journey = new Journey();
        journeyList.add(journey);
    }

    public class Journey{
        private List <AirSegmentInformation> airSegmentList;
        public Journey(){
            airSegmentList=new ArrayList<AirSegmentInformation>();
        }

        public List<AirSegmentInformation> getAirSegmentList() {
            return airSegmentList;
        }

        public void setAirSegmentList(List<AirSegmentInformation> airSegmentList) {
            this.airSegmentList = airSegmentList;
        }
    }
}
