package com.compassites.model.traveller;


import com.compassites.model.CabinClass;
import com.compassites.model.FlightItinerary;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by user on 06-08-2014.
 */
public class TravellerMasterInfo {

    private List<Traveller> travellersList;

    private AdditionalInfo additionalInfo;

    private FlightItinerary itinerary;

    private boolean seamen;

    private CabinClass cabinClass;

    public TravellerMasterInfo() {
        this.travellersList = new ArrayList<>();
    }

    public AdditionalInfo getAdditionalInfo() {
        return additionalInfo;
    }

    public void setAdditionalInfo(AdditionalInfo additionalInfo) {
        this.additionalInfo = additionalInfo;
    }

    public List<Traveller> getTravellersList() {
        return travellersList;
    }

    public void setTravellersList(List<Traveller> travellersList) {
        this.travellersList = travellersList;
    }

    public FlightItinerary getItinerary() {
        return itinerary;
    }

    public void setItinerary(FlightItinerary itinerary) {
        this.itinerary = itinerary;
    }

    public boolean isSeamen() {
        return seamen;
    }

    public void setSeamen(boolean seamen) {
        this.seamen = seamen;
    }

    public CabinClass getCabinClass() {
        return cabinClass;
    }

    public void setCabinClass(CabinClass cabinClass) {
        this.cabinClass = cabinClass;
    }
}
