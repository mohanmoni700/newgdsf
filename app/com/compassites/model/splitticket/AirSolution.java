package com.compassites.model.splitticket;

import com.compassites.model.FlightItinerary;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class AirSolution {
    public AirSolution(){

        flightItineraryList=new ArrayList<FlightItinerary>();
        seamenHashMap = new ConcurrentHashMap<>();
        nonSeamenHashMap = new ConcurrentHashMap<>();
    }

    private List<FlightItinerary> flightItineraryList;

    private ConcurrentHashMap<Integer, FlightItinerary> seamenHashMap;

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
