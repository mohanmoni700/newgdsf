package com.compassites.model;

import java.util.ArrayList;
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
    }

    private List<FlightItinerary> flightItineraryList;

    public List<FlightItinerary> getFlightItineraryList() {
        return flightItineraryList;
    }

    public void setFlightItineraryList(List<FlightItinerary> flightItineraryList) {
        this.flightItineraryList = flightItineraryList;
    }
}
