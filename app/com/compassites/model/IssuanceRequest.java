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

    private CabinClass cabinClass;

    private FlightItinerary flightItinerary;

    private List<Traveller> travellerList;

    private String sessionIdRef;

    private String jocPNR;

    private String bookingId;

    private String appRef;

    private String resultToken;

    private String reBookingId;

    private String reAppRef;

    private String reResultToken;

    private String reGdsPNR;

    public String getReGdsPNR() {
        return reGdsPNR;
    }

    public void setReGdsPNR(String reGdsPNR) {
        this.reGdsPNR = reGdsPNR;
    }

    public String getReBookingId() {
        return reBookingId;
    }

    public void setReBookingId(String reBookingId) {
        this.reBookingId = reBookingId;
    }

    public String getReAppRef() {
        return reAppRef;
    }

    public void setReAppRef(String reAppRef) {
        this.reAppRef = reAppRef;
    }

    public String getReResultToken() {
        return reResultToken;
    }

    public void setReResultToken(String reResultToken) {
        this.reResultToken = reResultToken;
    }

    public String getResultToken() {
        return resultToken;
    }

    public void setResultToken(String resultToken) {
        this.resultToken = resultToken;
    }

    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public String getAppRef() {
        return appRef;
    }

    public void setAppRef(String appRef) {
        this.appRef = appRef;
    }

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

    public CabinClass getCabinClass() {
        return cabinClass;
    }

    public void setCabinClass(CabinClass cabinClass) {
        this.cabinClass = cabinClass;
    }

    public String getSessionIdRef() {
        return sessionIdRef;
    }

    public void setSessionIdRef(String sessionIdRef) {
        this.sessionIdRef = sessionIdRef;
    }

    public String getJocPNR() {
        return jocPNR;
   }

    public void setJocPNR(String jocPNR) {
      this.jocPNR = jocPNR;
    }
}
