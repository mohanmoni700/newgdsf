package models;

import com.compassites.model.FlightItinerary;
import com.compassites.model.Passenger;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AncillaryServiceRequest implements Serializable {


    private String provider;
    private String gdsPNR;
    private FlightItinerary flightItinerary;
    private boolean isSeamen;
    private int adultCount;
    private int childCount;
    private int infantCount;
    private List<Passenger> passengers;


    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public  String getGdsPNR() {
        return  gdsPNR;
    }

    public void setGdsPNR(String gdsPNR) {
        this.gdsPNR = gdsPNR;
    }

    public FlightItinerary getFlightItinerary() {
        return flightItinerary;
    }

    public void setFlightItinerary(FlightItinerary flightItinerary) {
        this.flightItinerary = flightItinerary;
    }

    public boolean isSeamen() {
        return isSeamen;
    }

    public void setSeamen(boolean seamen) {
        isSeamen = seamen;
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

    public List<Passenger> getPassengers() {
        return passengers;
    }

    public void setPassengers(List<Passenger> passengers) {
        this.passengers = passengers;
    }
}

