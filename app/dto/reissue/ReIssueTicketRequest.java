package dto.reissue;

import com.compassites.model.CabinClass;
import com.compassites.model.FlightItinerary;
import com.compassites.model.Passenger;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ReIssueTicketRequest implements Serializable {

    private boolean isSeaman;

    private int adultCount;

    private int childCount;

    private int infantCount;

    private String gdsPNR;

    private String provider;

    private FlightItinerary flightItinerary;

    private List<Passenger> passengers;

    private List<ReIssueSearchParameters> requestedChange;

    private CabinClass cabinClass;

    public boolean isSeaman() {
        return isSeaman;
    }

    public void setSeaman(boolean seaman) {
        isSeaman = seaman;
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

    public FlightItinerary getFlightItinerary() {
        return flightItinerary;
    }

    public void setFlightItinerary(FlightItinerary flightItinerary) {
        this.flightItinerary = flightItinerary;
    }

    public List<Passenger> getPassengers() {
        return passengers;
    }

    public void setPassengers(List<Passenger> passengers) {
        this.passengers = passengers;
    }

    public List<ReIssueSearchParameters> getRequestedChange() {
        return requestedChange;
    }

    public void setRequestedChange(List<ReIssueSearchParameters> requestedChange) {
        this.requestedChange = requestedChange;
    }

    public CabinClass getCabinClass() {
        return cabinClass;
    }

    public void setCabinClass(CabinClass cabinClass) {
        this.cabinClass = cabinClass;
    }

}