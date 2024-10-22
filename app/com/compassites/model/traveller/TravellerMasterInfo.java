package com.compassites.model.traveller;

import com.compassites.model.BaggageDetails;
import com.compassites.model.CabinClass;
import com.compassites.model.FlightItinerary;
import com.compassites.model.PassengerTypeCode;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import utils.DateUtility;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by user on 06-08-2014.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
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

    private boolean isCreateTmpPNR;

    private String searchSelectOfficeId;

    private boolean isOfficeIdPricingError = false;

    private String gdsPNR;

    private String appReference;

    private String returnAppRef;

    private String searchResultToken;

    public String getSearchResultToken() {
        return searchResultToken;
    }

    public void setSearchResultToken(String searchResultToken) {
        this.searchResultToken = searchResultToken;
    }

    public String getReturnSearchResultToken() {
        return returnSearchResultToken;
    }

    public void setReturnSearchResultToken(String returnSearchResultToken) {
        this.returnSearchResultToken = returnSearchResultToken;
    }

    private String returnSearchResultToken;


    private List<BaggageDetails> baggageDetails;

    public List<BaggageDetails> getBaggageDetails() {
        return baggageDetails;
    }

    public void setBaggageDetails(List<BaggageDetails> baggageDetails) {
        this.baggageDetails = baggageDetails;
    }

    public String getReturnAppRef() {
        return returnAppRef;
    }

    public void setReturnAppRef(String returnAppRef) {
        this.returnAppRef = returnAppRef;
    }

    private Map<String,Map> benzyFareRuleMap;

    private boolean bookAndHold;

    private String vesselName;

    public boolean isBookAndHold() {
        return bookAndHold;
    }

    public void setBookAndHold(boolean bookAndHold) {
        this.bookAndHold = bookAndHold;
    }


    public Map<String, Map> getBenzyFareRuleMap() {
        return benzyFareRuleMap;
    }

    public void setBenzyFareRuleMap(Map<String, Map> benzyFareRuleMap) {
        this.benzyFareRuleMap = benzyFareRuleMap;
    }

    public String getAppReference() {
        return appReference;
    }

    public void setAppReference(String appReference) {
        this.appReference = appReference;
    }

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

    public boolean isOfficeIdPricingError() { return isOfficeIdPricingError; }

    public void setOfficeIdPricingError(boolean pricingError) { isOfficeIdPricingError = pricingError; }

    public String getSearchSelectOfficeId() { return searchSelectOfficeId; }

    public void setSearchSelectOfficeId(String searchSelectOfficeId) { this.searchSelectOfficeId = searchSelectOfficeId; }


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

    public boolean isCreateTmpPNR() {
        return isCreateTmpPNR;
    }

    public void setCreateTmpPNR(boolean createTmpPNR) {
        isCreateTmpPNR = createTmpPNR;
    }

    public String getGdsPNR() {
        return gdsPNR;
    }

    public void setGdsPNR(String gdsPNR) {
        this.gdsPNR = gdsPNR;
    }

    public String getVesselName() {
        return vesselName;
    }

}
