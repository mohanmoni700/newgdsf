package com.compassites.model;


import com.compassites.model.amadeus.reissue.ReIssuePricingInformation;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.pojomatic.Pojomatic;
import org.pojomatic.annotations.Property;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * User: Renu
 * Date: 5/23/14
 * Time: 4:30 PM
 * To change this template use File | Settings | File Templates.
 */
@JsonIgnoreProperties(ignoreUnknown = true)

public class FlightItinerary implements Serializable{

    public FlightItinerary() {
        journeyList = new ArrayList<Journey>();
        nonSeamenJourneyList = new ArrayList<Journey>();
        pricingInformation = new PricingInformation();
        seamanPricingInformation = new PricingInformation();
    }
    
    private long id;
    
    private boolean priceOnlyPTC;

    private String fromLocation;

    private String toLocation;

    private Map<String,Double> carbonDioxide;

    public Map<String, Double> getCarbonDioxide() {
        return carbonDioxide;
    }

    public void setCarbonDioxide(Map<String, Double> carbonDioxide) {
        this.carbonDioxide = carbonDioxide;
    }

    public String getFromLocation() {
        return fromLocation;
    }

    public void setFromLocation(String fromLocation) {
        this.fromLocation = fromLocation;
    }

    public String getToLocation() {
        return toLocation;
    }

    public void setToLocation(String toLocation) {
        this.toLocation = toLocation;
    }

    public List<PricingInformation> splitPricingInformationList;

    public List<PricingInformation> getSplitPricingInformationList() {
        return splitPricingInformationList;
    }

    public void setSplitPricingInformationList(List<PricingInformation> splitPricingInformationList) {
        this.splitPricingInformationList = splitPricingInformationList;
    }

    private boolean isSplitTicket;

    public boolean isSplitTicket() {
        return isSplitTicket;
    }

    public void setSplitTicket(boolean splitTicket) {
        isSplitTicket = splitTicket;
    }

    private ConcurrentHashMap<String, List<FlightItinerary>> groupingMap;

    public ConcurrentHashMap<String, List<FlightItinerary>> getGroupingMap() {
        return groupingMap;
    }

    public void setGroupingMap(ConcurrentHashMap<String, List<FlightItinerary>> groupingMap) {
        this.groupingMap = groupingMap;
    }

    //    private String provider; //travelport or amadeus

    //private String amadeusOfficeId;

    //private String seamenAmadeusOfficeId;
    
    private String fareSourceCode; // for Mystifly
    
    private PricingMessage pricingMessage;
    
    private PricingInformation pricingInformation;
    
    private PricingInformation seamanPricingInformation;

    private ReIssuePricingInformation reIssuePricingInformation;

    @Property
    private List<Journey> journeyList;

    @Property
    private List<Journey> nonSeamenJourneyList;

    private Long totalTravelTime;

    private String totalTravelTimeStr;

    private boolean isPassportMandatory;

    // added for Travelomatrix
    private String resultToken;

    // added for Travelomatrix roundtrip
    private String returnResultToken;

    private Boolean isLCC;

    private Boolean isRefundable;

    private String fareType;

    public Boolean getRefundable() {
        return isRefundable;
    }

    public void setRefundable(Boolean refundable) {
        isRefundable = refundable;
    }

    public String getFareType() {
        return fareType;
    }

    public void setFareType(String fareType) {
        this.fareType = fareType;
    }

    public Boolean getLCC() {
        return isLCC;
    }

    public void setLCC(Boolean LCC) {
        isLCC = LCC;
    }

    public String getResultToken() {
        return resultToken;
    }

    public void setResultToken(String resultToken) {
        this.resultToken = resultToken;
    }

    public Boolean IsLCC() {
        return isLCC;
    }

    public void setIsLCC(Boolean isLCC) {
        this.isLCC = isLCC;
    }

    public PricingMessage getPricingMessage() {
        return pricingMessage;
    }

    public void setPricingMessage(PricingMessage pricingMessage) {
        this.pricingMessage = pricingMessage;
    }

    public boolean isPriceOnlyPTC() {
        return priceOnlyPTC;
    }

    public void setPriceOnlyPTC(boolean priceOnlyPTC) {
        this.priceOnlyPTC = priceOnlyPTC;
    }

    public List<Journey> getJourneyList() {
        return journeyList;
    }

    public void setJourneyList(List<Journey> journeyList) {
        this.journeyList = journeyList;
    }

//    public String getAmadeusOfficeId() {
//        return amadeusOfficeId;
//    }
//
//    public void setAmadeusOfficeId(String amadeusOfficeId) {
//        this.amadeusOfficeId = amadeusOfficeId;
//    }
//    public String getProvider() {
//        return provider;
//    }
//
//    public void setProvider(String provider) {
//        this.provider = provider;
//    }


//    public String getSeamenAmadeusOfficeId() {
//        return seamenAmadeusOfficeId;
//    }
//
//    public void setSeamenAmadeusOfficeId(String seamenAmadeusOfficeId) {
//        this.seamenAmadeusOfficeId = seamenAmadeusOfficeId;
//    }

    public PricingInformation getPricingInformation() {
        return pricingInformation;
    }

    public void setPricingInformation(PricingInformation pricingInformation) {
        this.pricingInformation = pricingInformation;
    }

    public PricingInformation getSeamanPricingInformation() {
        return seamanPricingInformation;
    }

    public void setSeamanPricingInformation(PricingInformation seamanPricingInformation) {
        this.seamanPricingInformation = seamanPricingInformation;
    }

    public void AddBlankJourney(){
        Journey journey = new Journey();
        journeyList.add(journey);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getFareSourceCode() {
		return fareSourceCode;
	}

	public void setFareSourceCode(String fareSourceCode) {
		this.fareSourceCode = fareSourceCode;
	}

    public ReIssuePricingInformation getReIssuePricingInformation() {
        return reIssuePricingInformation;
    }

    public void setReIssuePricingInformation(ReIssuePricingInformation reIssuePricingInformation) {
        this.reIssuePricingInformation = reIssuePricingInformation;
    }

    /* @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof FlightItinerary)){
             return  false;
        }
        FlightItinerary object = (FlightItinerary) obj;

        if(this.journeyList.get(0).airSegmentList.size() != ((FlightItinerary) obj).getJourneyList().get(0).getAirSegmentList().size()){
            return false;
        }else {
            for (int i = 0; i <this.journeyList.get(0).airSegmentList.size() ; i++) {
                AirSegmentInformation segmentInformation = this.journeyList.get(0).getAirSegmentList().get(i);
                AirSegmentInformation segmentInformation1 = object.getJourneyList().get(0).getAirSegmentList().get(i);
                if(!segmentInformation.equals(segmentInformation1)){
                    return  false;
                }
            }
        }
        *//*if(!this.pricingInformation.getTotalPrice().equals(object.getPricingInformation().getTotalPrice())) {
            return false;
        }*//*
        return true;

    }*/

	@Override
    public boolean equals(Object obj) {
        return Pojomatic.equals(this, obj);
    }

    @Override
    public int hashCode() {
        return Pojomatic.hashCode(this);
    }

    @Override
    public String toString() {
        return Pojomatic.toString(this);
    }

    public List<Journey> getNonSeamenJourneyList() {
        return nonSeamenJourneyList;
    }

    public void setNonSeamenJourneyList(List<Journey> nonSeamenJourneyList) {
        this.nonSeamenJourneyList = nonSeamenJourneyList;
    }

    public PricingInformation getPricingInformation(boolean isSeamen){
        if(isSeamen){
            return seamanPricingInformation;
        }
        return pricingInformation;
    }

    public void setPricingInformation(boolean isSeamen, PricingInformation pricingInformation){
        if(isSeamen){
            this.seamanPricingInformation = pricingInformation;
        }
        this.pricingInformation = pricingInformation;
    }

    public List<Journey> getJourneys(boolean isSeamen){
        if(isSeamen){
           return journeyList;
        }
        return nonSeamenJourneyList;
    }

    public Long getTotalTravelTime() {
        Long totalTravelTime = new Long(0);

        for(Journey journey : journeyList){
            if(journey != null && journey.getTravelTimeMillis() != null){
                totalTravelTime = totalTravelTime + journey.getTravelTimeMillis();
            }
        }
        return totalTravelTime;
    }

    public void setTotalTravelTime(Long totalTravelTime) {
        this.totalTravelTime = totalTravelTime;
    }


    public String getTotalTravelTimeStr() {
        Long totalTravelTime = getTotalTravelTime();
        if(totalTravelTime != null) {
            String hms = String.format("%02d Hour(s)%02d Minutes", TimeUnit.MILLISECONDS.toHours(totalTravelTime),
                    TimeUnit.MILLISECONDS.toMinutes(totalTravelTime) % TimeUnit.HOURS.toMinutes(1));
            return  hms;
        }
        return "";
    }

    public void setTotalTravelTimeStr(String totalTravelTimeStr) {
        this.totalTravelTimeStr = totalTravelTimeStr;
    }

    public boolean isPassportMandatory() {
        return isPassportMandatory;
    }

    public void setPassportMandatory(boolean passportMandatory) {
        isPassportMandatory = passportMandatory;
    }

    public String getReturnResultToken() {
        return returnResultToken;
    }

    public void setReturnResultToken(String returnResultToken) {
        this.returnResultToken = returnResultToken;
    }

}
