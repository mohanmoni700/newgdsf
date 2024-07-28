package com.compassites.model.travelomatrix;

import com.compassites.model.CabinClass;
import com.compassites.model.JourneyType;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FlightSearchRequest implements Serializable {

    @JsonProperty("AdultCount")
    public String adultCount;

    @JsonProperty("ChildCount")
    public String childCount;

    @JsonProperty("InfantCount")
    public String infantCount;

    @JsonProperty("JourneyType")
    public String journeyType;
    @JsonProperty("PreferredAirlines")
    public List<String> preferredAirlines;
    @JsonProperty("CabinClass")
    public CabinClass cabinClass;
    @JsonProperty("Segments")
    public List<Segment> segmentsdata;

    public String getAdultCount() {
        return adultCount;
    }

    public void setAdultCount(String adultCount) {
        this.adultCount = adultCount;
    }

    public String getChildCount() {
        return childCount;
    }

    public void setChildCount(String childCount) {
        this.childCount = childCount;
    }

    public String getInfantCount() {
        return infantCount;
    }

    public void setInfantCount(String infantCount) {
        this.infantCount = infantCount;
    }

    public String getJourneyType() {
        return journeyType;
    }

    public void setJourneyType(String journeyType) {
        this.journeyType = journeyType;
    }

    public List<String> getPreferredAirlines() {
        return preferredAirlines;
    }

    public void setPreferredAirlines(List<String> preferredAirlines) {
        this.preferredAirlines = preferredAirlines;
    }

    public CabinClass getCabinClass() {
        return cabinClass;
    }

    public void setCabinClass(CabinClass cabinClass) {
        this.cabinClass = cabinClass;
    }

    public List<Segment> getSegmentsdata() {
        return segmentsdata;
    }

    public void setSegmentsdata(List<Segment> segmentsdata) {
        this.segmentsdata = segmentsdata;
    }
}
