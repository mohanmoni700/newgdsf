package com.compassites.model.amadeus;

import com.amadeus.xml.fmptbr_12_4_1a.FareMasterPricerTravelBoardSearchReply;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by mahendra-singh on 26/5/14.
 */
public class AmadeusRecommendation implements Serializable {

    public AmadeusRecommendation(){
        groupOfFlights=new ArrayList<FareMasterPricerTravelBoardSearchReply.FlightIndex.GroupOfFlights>();
    }
    private FareMasterPricerTravelBoardSearchReply.Recommendation recommendation;
    private List<FareMasterPricerTravelBoardSearchReply.FlightIndex.GroupOfFlights> groupOfFlights;

    public FareMasterPricerTravelBoardSearchReply.Recommendation getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(FareMasterPricerTravelBoardSearchReply.Recommendation recommendation) {
        this.recommendation = recommendation;
    }


    public List<FareMasterPricerTravelBoardSearchReply.FlightIndex.GroupOfFlights> getGroupOfFlights() {
        return groupOfFlights;
    }

    public void setGroupOfFlights(List<FareMasterPricerTravelBoardSearchReply.FlightIndex.GroupOfFlights> groupOfFlights) {
        this.groupOfFlights = groupOfFlights;
    }
}
