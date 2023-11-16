package com.compassites.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import models.FlightSearchOffice;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Renu
 * Date: 5/22/14
 * Time: 2:14 PM
 * To change this template use File | Settings | File Templates.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchResponse {
    private AirSolution airSolution;
    //private AirSolution airSolutionReturn;
    private String provider; //Amedeus or Travelport (Galileo)

    private FlightSearchOffice flightSearchOffice;

    private List<ErrorMessage> errorMessageList;

    public SearchResponse() {
        airSolution = new AirSolution();
        errorMessageList = new ArrayList<>();
    }

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

	@Override
	public String toString() {
		return "SearchResponse [airSolution=" + airSolution + ", provider=" + provider + ", errorMessageList="
				+ errorMessageList + "]";
	}
    
    
}
