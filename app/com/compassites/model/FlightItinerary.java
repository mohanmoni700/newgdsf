package com.compassites.model;


import org.pojomatic.Pojomatic;
import org.pojomatic.annotations.Property;

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

    public FlightItinerary() {
        journeyList = new ArrayList<Journey>();
        pricingInformation = new PricingInformation();
        seamanPricingInformation = new PricingInformation();
    }
    
    private long id;
    
    private boolean priceOnlyPTC;
    
    private String provider; //travelport or amadeus
    
    private String fareSourceCode; // for Mystifly
    
    private PricingMessage pricingMessage;
    
    private PricingInformation pricingInformation;
    
    private PricingInformation seamanPricingInformation;
    
    @Property
    private List<Journey> journeyList;

    public PricingMessage getPricingMessage() {
        return pricingMessage;
    }

    public void setPricingMessage(PricingMessage pricingMessage) {
        this.pricingMessage = pricingMessage;
    }

    public boolean isPriceOnlyPTC() {
        return priceOnlyPTC;
    }

    public void setPriceOnlyPTC(boolean priceOnlyPTC) {
        this.priceOnlyPTC = priceOnlyPTC;
    }

    public List<Journey> getJourneyList() {
        return journeyList;
    }

    public void setJourneyList(List<Journey> journeyList) {
        this.journeyList = journeyList;
    }

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

    public PricingInformation getSeamanPricingInformation() {
        return seamanPricingInformation;
    }

    public void setSeamanPricingInformation(PricingInformation seamanPricingInformation) {
        this.seamanPricingInformation = seamanPricingInformation;
    }

    public void AddBlankJourney(){
        Journey journey = new Journey();
        journeyList.add(journey);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getFareSourceCode() {
		return fareSourceCode;
	}

	public void setFareSourceCode(String fareSourceCode) {
		this.fareSourceCode = fareSourceCode;
	}
    
    /* @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof FlightItinerary)){
             return  false;
        }
        FlightItinerary object = (FlightItinerary) obj;

        if(this.journeyList.get(0).airSegmentList.size() != ((FlightItinerary) obj).getJourneyList().get(0).getAirSegmentList().size()){
            return false;
        }else {
            for (int i = 0; i <this.journeyList.get(0).airSegmentList.size() ; i++) {
                AirSegmentInformation segmentInformation = this.journeyList.get(0).getAirSegmentList().get(i);
                AirSegmentInformation segmentInformation1 = object.getJourneyList().get(0).getAirSegmentList().get(i);
                if(!segmentInformation.equals(segmentInformation1)){
                    return  false;
                }
            }
        }
        *//*if(!this.pricingInformation.getTotalPrice().equals(object.getPricingInformation().getTotalPrice())) {
            return false;
        }*//*
        return true;

    }*/

	@Override
    public boolean equals(Object obj) {
        return Pojomatic.equals(this,obj);
    }

    @Override
    public int hashCode() {
        return Pojomatic.hashCode(this);
    }

    @Override
    public String toString() {
        return Pojomatic.toString(this);
    }

}
