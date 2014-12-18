package com.compassites.model;

import com.compassites.model.traveller.Traveller;

import java.io.Serializable;
import java.util.List;

/**
 * Created by Yaseen on 04-12-2014.
 */
public class IssuanceRequest implements Serializable{

    private int adultCount;

    private int childCount;

    private int infantCount;
    
    private String gdsPNR;
    
    private String provider;

    private boolean isSeamen;

    private FlightItinerary flightItinerary;

    private List<Traveller> travellerList;

    public int getAdultCount() {
        return adultCount;
    }

    public void setAdultCount(int adultCount) {
        this.adultCount = adultCount;
    }

    public int getChildCount() {
        return childCount;
    }

    public void setChildCount(int childCount) {
        this.childCount = childCount;
    }

    public int getInfantCount() {
        return infantCount;
    }

    public void setInfantCount(int infantCount) {
        this.infantCount = infantCount;
    }

    public FlightItinerary getFlightItinerary() {
        return flightItinerary;
    }

    public void setFlightItinerary(FlightItinerary flightItinerary) {
        this.flightItinerary = flightItinerary;
    }

    public String getGdsPNR() {
        return gdsPNR;
    }

    public void setGdsPNR(String gdsPNR) {
        this.gdsPNR = gdsPNR;
    }

    public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	public List<Traveller> getTravellerList() {
        return travellerList;
    }

    public void setTravellerList(List<Traveller> travellerList) {
        this.travellerList = travellerList;
    }

    public boolean isSeamen() {
        return isSeamen;
    }

    public void setSeamen(boolean isSeamen) {
        this.isSeamen = isSeamen;
    }
}
