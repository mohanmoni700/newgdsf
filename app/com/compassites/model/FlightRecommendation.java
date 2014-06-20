package com.compassites.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mahendra-singh on 3/6/14.
 */
public class FlightRecommendation {

    public FlightRecommendation(){
        airSegmentInformationOnward=new ArrayList<AirSegmentInformation>();
        airSegmentInformationReturn=new ArrayList<AirSegmentInformation>();
    }
    private PricingInformation pricingInformation;
    private List<AirSegmentInformation> airSegmentInformationOnward;
    private List<AirSegmentInformation> airSegmentInformationReturn;

    public PricingInformation getPricingInformation() {
        return pricingInformation;
    }

    public void setPricingInformation(PricingInformation pricingInformation) {
        this.pricingInformation = pricingInformation;
    }

    public List<AirSegmentInformation> getAirSegmentInformationOnward() {
        return airSegmentInformationOnward;
    }

    public void setAirSegmentInformationOnward(List<AirSegmentInformation> airSegmentInformationOnward) {
        this.airSegmentInformationOnward = airSegmentInformationOnward;
    }

    public List<AirSegmentInformation> getAirSegmentInformationReturn() {
        return airSegmentInformationReturn;
    }

    public void setAirSegmentInformationReturn(List<AirSegmentInformation> airSegmentInformationReturn) {
        this.airSegmentInformationReturn = airSegmentInformationReturn;
    }
}
