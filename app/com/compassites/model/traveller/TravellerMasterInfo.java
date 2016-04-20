package com.compassites.model.traveller;

import com.compassites.model.CabinClass;
import com.compassites.model.FlightItinerary;
import com.compassites.model.PassengerTypeCode;
import com.fasterxml.jackson.annotation.JsonIgnore;
import utils.DateUtility;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by user on 06-08-2014.
 */
public class TravellerMasterInfo {

    private List<Traveller> travellersList;

    private AdditionalInfo additionalInfo;

    private FlightItinerary itinerary;

    private String accountId;

    private boolean seamen;

    private CabinClass cabinClass;

    private String approvers;

    private String cancellationFeeText;

    private String reasonForApproval;

    private String sessionIdRef;

    private Date validTillDate;
    
    private String journeyType;

    private Map<String, String> segmentBaggageMap;

    private String userTimezone;

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

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getApprovers() {
        return approvers;
    }

    public void setApprovers(String approvers) {
        this.approvers = approvers;
    }

    public String getCancellationFeeText() {
        return cancellationFeeText;
    }

    public void setCancellationFeeText(String cancellationFeeText) {
        this.cancellationFeeText = cancellationFeeText;
    }

    public String getReasonForApproval() {
        return reasonForApproval;
    }

    public void setReasonForApproval(String reasonForApproval) {
        this.reasonForApproval = reasonForApproval;
    }

    public String getSessionIdRef() {
        return sessionIdRef;
    }

    public void setSessionIdRef(String sessionIdRef) {
        this.sessionIdRef = sessionIdRef;
    }

    public Date getValidTillDate() {
        return validTillDate;
    }

    public void setValidTillDate(Date validTillDate) {
        this.validTillDate = validTillDate;
    }

    
    public String getJourneyType() {
		return journeyType;
	}

	public void setJourneyType(String journeyType) {
		this.journeyType = journeyType;
	}

    public Map<String, String> getSegmentBaggageMap() {
        return segmentBaggageMap;
    }

    public void setSegmentBaggageMap(Map<String, String> segmentBaggageMap) {
        this.segmentBaggageMap = segmentBaggageMap;
    }

    public String getUserTimezone() {
        return userTimezone;
    }

    public void setUserTimezone(String userTimezone) {
        this.userTimezone = userTimezone;
    }

    @JsonIgnore
    public int getAdultChildPaxCount(){
        int count = 0;

        for(Traveller traveller : this.travellersList){
            if(traveller.getPassportDetails() != null){
                PassengerTypeCode passengerTypeCode = DateUtility.getPassengerTypeFromDOB(traveller.getPassportDetails().getDateOfBirth());
                if(!"INF".equalsIgnoreCase(passengerTypeCode.toString())){
                    count = count + 1;
                }
            }
        }
        return count;
    }
}
