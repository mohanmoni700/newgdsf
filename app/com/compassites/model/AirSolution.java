package com.compassites.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
        seamenHashMap = new HashMap<>();
        nonSeamenHashMap = new HashMap<>();
    }

    private List<FlightItinerary> flightItineraryList;

    @JsonIgnore
    private HashMap<Integer, FlightItinerary> seamenHashMap;

    @JsonIgnore
    private HashMap<Integer, FlightItinerary> nonSeamenHashMap;

    public List<FlightItinerary> getFlightItineraryList() {
        return flightItineraryList;
    }

    public void setFlightItineraryList(List<FlightItinerary> flightItineraryList) {
        this.flightItineraryList = flightItineraryList;
    }

    public HashMap<Integer, FlightItinerary> getSeamenHashMap() {
        return seamenHashMap;
    }

    public void setSeamenHashMap(HashMap<Integer, FlightItinerary> seamenHashMap) {
        this.seamenHashMap = seamenHashMap;
    }

    public HashMap<Integer, FlightItinerary> getNonSeamenHashMap() {
        return nonSeamenHashMap;
    }

    public void setNonSeamenHashMap(HashMap<Integer, FlightItinerary> nonSeamenHashMap) {
        this.nonSeamenHashMap = nonSeamenHashMap;
    }
}
