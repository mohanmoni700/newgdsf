package com.compassites.model;

/**
 * Created with IntelliJ IDEA.
 * User: Renu
 * Date: 5/22/14
 * Time: 2:14 PM
 * To change this template use File | Settings | File Templates.
 */
public class SearchResponse {
    private AirSolution airSolution;
    //private AirSolution airSolutionReturn;
    private String provider; //Amedeus or Travelport (Galileo)


    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public AirSolution getAirSolution() {
        return airSolution;
    }

    public void setAirSolution(AirSolution airSolution) {
        this.airSolution = airSolution;
    }
}
