package com.compassites.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created with IntelliJ IDEA.
 * User: Renu
 * Date: 5/22/14
 * Time: 2:20 PM
 * To change this template use File | Settings | File Templates.
 */
public class AirSolution {
    public AirSolution(){

        flightItineraryList=new ArrayList<FlightItinerary>();
        seamenHashMap = new ConcurrentHashMap<>();
        nonSeamenHashMap = new ConcurrentHashMap<>();
    }

    private List<FlightItinerary> flightItineraryList;

    @JsonIgnore
    private ConcurrentHashMap<Integer, FlightItinerary> seamenHashMap;

    @JsonIgnore
    private ConcurrentHashMap<Integer, FlightItinerary> nonSeamenHashMap;

    public List<FlightItinerary> getFlightItineraryList() {
        return flightItineraryList;
    }

    public void setFlightItineraryList(List<FlightItinerary> flightItineraryList) {
        this.flightItineraryList = flightItineraryList;
    }

    public ConcurrentHashMap<Integer, FlightItinerary> getSeamenHashMap() {
        return seamenHashMap;
    }

    public void setSeamenHashMap(ConcurrentHashMap<Integer, FlightItinerary> seamenHashMap) {
        this.seamenHashMap = seamenHashMap;
    }

    public ConcurrentHashMap<Integer, FlightItinerary> getNonSeamenHashMap() {
        return nonSeamenHashMap;
    }

    public void setNonSeamenHashMap(ConcurrentHashMap<Integer, FlightItinerary> nonSeamenHashMap) {
        this.nonSeamenHashMap = nonSeamenHashMap;
    }

	@Override
	public String toString() {
		return "AirSolution [flightItineraryList=" + flightItineraryList + ", seamenHashMap=" + seamenHashMap
				+ ", nonSeamenHashMap=" + nonSeamenHashMap + "]";
	}
    
    
}
