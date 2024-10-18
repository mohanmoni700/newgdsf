package dto.reissue;

import com.compassites.model.AirSolution;
import com.compassites.model.ErrorMessage;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import models.FlightSearchOffice;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ReIssueSearchResponse {

    private AirSolution airSolution;
    //private AirSolution airSolutionReturn;
    private String provider; //Amedeus or Travelport (Galileo)

    private FlightSearchOffice flightSearchOffice;

    private List<ErrorMessage> errorMessageList;

    private boolean isReIssueSearch;

//    public ReIssueSearchResponse() {
//        airSolution = new AirSolution();
//        errorMessageList = new ArrayList<>();
//    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public FlightSearchOffice getFlightSearchOffice() {
        return flightSearchOffice;
    }

    public void setFlightSearchOffice(FlightSearchOffice flightSearchOffice) {
        this.flightSearchOffice = flightSearchOffice;
    }

    public AirSolution getAirSolution() {
        return airSolution;
    }

    public void setAirSolution(AirSolution airSolution) {
        this.airSolution = airSolution;
    }

    public List<ErrorMessage> getErrorMessageList() { return errorMessageList; }

    public void setErrorMessageList(List<ErrorMessage> errorMessageList) { this.errorMessageList = errorMessageList; }

    public boolean isReIssueSearch() {
        return isReIssueSearch;
    }

    public void setReIssueSearch(boolean reIssueSearch) {
        isReIssueSearch = reIssueSearch;
    }

    @Override
    public String toString() {
        return "SearchResponse [airSolution=" + airSolution + ", provider=" + provider + ", errorMessageList="
                + errorMessageList + "]";
    }



}
