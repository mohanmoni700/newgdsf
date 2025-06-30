package com.compassites.model;

import com.compassites.model.amadeus.AmadeusPaxInformation;
import dto.AmadeusSegmentRefDTO;
import dto.FareCheckRulesResponse;
import dto.FreeMealsDetails;
import dto.FreeSeatDetails;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by user on 18-08-2014.
 */
public class PNRResponse implements Serializable  {

    private  String pnrNumber;

    private ErrorMessage errorMessage;

    private Date validTillDate;

    private boolean isPriceChanged;

    private boolean isFlightAvailable;

    private BigDecimal originalPrice;

    private BigDecimal changedPrice;

    private String priceChangeKey;

    private boolean isCappingLimitReached;

    private BigDecimal changedBasePrice;

    private String airlinePNR;
    
    private PricingInformation pricingInfo;

    private boolean holdTime;

    private String sessionIdRef;

    private boolean airlinePNRError;

    private Map<String, String> airlinePNRMap;

    private Map<String, String> ticketNumberMap;

    private Map<String, FlightInfo> flightInfoMap;

    private Map<String, String> segmentBaggageMap;

    private Map<String, AmadeusSegmentRefDTO> segmentRefMap;

    private Map<String,Double> carbonDioxide;

    public Map<String, Double> getCarbonDioxide() {
        return carbonDioxide;
    }

    public void setCarbonDioxide(Map<String, Double> carbonDioxide) {
        this.carbonDioxide = carbonDioxide;
    }

    private String bookedStatus;

    private boolean isChangedPriceLow;

    private boolean isChangedPriceHigh;

    public boolean isChangedPriceHigh() {
        return isChangedPriceHigh;
    }

    public void setChangedPriceHigh(boolean changedPriceHigh) {
        isChangedPriceHigh = changedPriceHigh;
    }

    private String creationOfficeId;

    private boolean isOfficeIdPricingError = false;

    private String resultToken;

    private String returnResultToken;

    private String returnAppReference;

    private String bookingId;

    private String returnBookingId;

    private String appReference;

    private Map<String,Map> benzyFareRuleMap;

    private String returnGdsPNR;

    private String searchResultToken;

    private String returnSearchResultToken;

    private List<BaggageDetails> tmxBaggageDetails;

    private Boolean addBooking;

    private String originalPNR;

    private boolean isPnrSplit;

    private Map<String, String> pnrMap;

    private List<FreeMealsDetails> freeMealsDetailsList;

    private List<FreeSeatDetails> freeSeatList;

    private Map<String,FareCheckRulesResponse> fareCheckRulesResponseMap;

    private String status;

    public Map<String, String> getPnrMap() {
        return pnrMap;
    }

    public void setPnrMap(Map<String, String> pnrMap) {
        this.pnrMap = pnrMap;
    }

    public Boolean getAddBooking() {
        return addBooking;
    }

    public void setAddBooking(Boolean addBooking) {
        this.addBooking = addBooking;
    }

    public String getOriginalPNR() {
        return originalPNR;
    }

    public void setOriginalPNR(String originalPNR) {
        this.originalPNR = originalPNR;
    }

    private List<AmadeusPaxInformation> amadeusPaxReference;


    public List<MealDetails> getTmxMealDetails() {
        return tmxMealDetails;
    }

    private boolean isReIssueSuccess;

    public void setTmxMealDetails(List<MealDetails> tmxMealDetails) {
        this.tmxMealDetails = tmxMealDetails;
    }

    private List<MealDetails> tmxMealDetails;

    public List<BaggageDetails> getTmxBaggageDetails() {
        return tmxBaggageDetails;
    }

    public void setTmxBaggageDetails(List<BaggageDetails> tmxBaggageDetails) {
        this.tmxBaggageDetails = tmxBaggageDetails;
    }


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

    public String getReturnGdsPNR() {
        return returnGdsPNR;
    }

    public void setReturnGdsPNR(String returnGdsPNR) {
        this.returnGdsPNR = returnGdsPNR;
    }

    public Map<String, Map> getBenzyFareRuleMap() {
        return benzyFareRuleMap;
    }

    public void setBenzyFareRuleMap(Map<String, Map> benzyFareRuleMap) {
        this.benzyFareRuleMap = benzyFareRuleMap;
    }

    public String getReturnResultToken() {
        return returnResultToken;
    }

    public void setReturnResultToken(String returnResultToken) {
        this.returnResultToken = returnResultToken;
    }

    public String getReturnAppReference() {
        return returnAppReference;
    }

    public String getReturnBookingId() {
        return returnBookingId;
    }

    public void setReturnBookingId(String returnBookingId) {
        this.returnBookingId = returnBookingId;
    }

    public void setReturnAppReference(String returnAppReference) {
        this.returnAppReference = returnAppReference;
    }

    public Map<String, String> getTicketNumberMap() {
        return ticketNumberMap;
    }

    public void setTicketNumberMap(Map<String, String> ticketNumberMap) {
        this.ticketNumberMap = ticketNumberMap;
    }

    public String getAppReference() {
        return appReference;
    }

    public void setAppReference(String appReference) {
        this.appReference = appReference;
    }

    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public String getResultToken() {
        return resultToken;
    }

    public void setResultToken(String resultToken) {
        this.resultToken = resultToken;
    }

    public boolean isOfficeIdPricingError() { return isOfficeIdPricingError; }

    public void setOfficeIdPricingError(boolean pricingError) { isOfficeIdPricingError = pricingError; }

    public String getCreationOfficeId() { return creationOfficeId; }

    public void setCreationOfficeId(String creationOfficeId) { this.creationOfficeId = creationOfficeId; }

    public String getPnrNumber() {
        return pnrNumber;
    }

    public void setPnrNumber(String pnrNumber) {
        this.pnrNumber = pnrNumber;
    }

    public ErrorMessage getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(ErrorMessage errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Date getValidTillDate() {
        return validTillDate;
    }

    public List<AmadeusPaxInformation> getAmadeusPaxReference() {
        return amadeusPaxReference;
    }

    public void setAmadeusPaxReference(List<AmadeusPaxInformation> amadeusPaxReference) {
        this.amadeusPaxReference = amadeusPaxReference;
    }
    public void setValidTillDate(Date validTillDate) {
        this.validTillDate = validTillDate;
    }

    public boolean isPriceChanged() {
        return isPriceChanged;
    }

    public void setPriceChanged(boolean isPriceChanged) {
        this.isPriceChanged = isPriceChanged;
    }

    public BigDecimal getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(BigDecimal originalPrice) {
        this.originalPrice = originalPrice;
    }

    public BigDecimal getChangedPrice() {
        return changedPrice;
    }

    public void setChangedPrice(BigDecimal changedPrice) {
        this.changedPrice = changedPrice;
    }

    public boolean isFlightAvailable() {
        return isFlightAvailable;
    }

    public void setFlightAvailable(boolean isFlightAvailable) {
        this.isFlightAvailable = isFlightAvailable;
    }

    public String getPriceChangeKey() {
        return priceChangeKey;
    }

    public void setPriceChangeKey(String priceChangeKey) {
        this.priceChangeKey = priceChangeKey;
    }

    public boolean isCappingLimitReached() {
        return isCappingLimitReached;
    }

    public void setCappingLimitReached(boolean isCappingLimitReached) {
        this.isCappingLimitReached = isCappingLimitReached;
    }
	
	public BigDecimal getChangedBasePrice() {
        return changedBasePrice;
    }

	public void setChangedBasePrice(BigDecimal changedBasePrice) {
        this.changedBasePrice = changedBasePrice;
    }

	public String getAirlinePNR() {
		return airlinePNR;
	}

	public void setAirlinePNR(String airlinePNR) {
		this.airlinePNR = airlinePNR;
	}

	public PricingInformation getPricingInfo() {
		return pricingInfo;
	}

	public void setPricingInfo(PricingInformation pricingInfo) {
		this.pricingInfo = pricingInfo;
	}

    public boolean isHoldTime() {
        return holdTime;
    }

    public void setHoldTime(boolean holdTime) {
        this.holdTime = holdTime;
    }

    public String getSessionIdRef() {
        return sessionIdRef;
    }

    public void setSessionIdRef(String sessionIdRef) {
        this.sessionIdRef = sessionIdRef;
    }

    public Map<String, String> getAirlinePNRMap() {
        return airlinePNRMap;
    }

    public void setAirlinePNRMap(Map<String, String> airlinePNRMap) {
        this.airlinePNRMap = airlinePNRMap;
    }

    public Map<String, FlightInfo> getFlightInfoMap() {
        return flightInfoMap;
    }

    public void setFlightInfoMap(Map<String, FlightInfo> flightInfoMap) {
        this.flightInfoMap = flightInfoMap;
    }

    public Map<String, String> getSegmentBaggageMap() {
        return segmentBaggageMap;
    }

    public void setSegmentBaggageMap(Map<String, String> segmentBaggageMap) {
        this.segmentBaggageMap = segmentBaggageMap;
    }

    public boolean isAirlinePNRError() {
        return airlinePNRError;
    }

    public void setAirlinePNRError(boolean airlinePNRError) {
        this.airlinePNRError = airlinePNRError;
	}
	
    public void setBookedStatus(String bookedStatus){
        this.bookedStatus = bookedStatus;
    }

    public String getBookedStatus() {
        return bookedStatus;
    }

    public boolean isChangedPriceLow() {
        return isChangedPriceLow;
    }

    public void setChangedPriceLow(boolean changedPriceLow) {
        isChangedPriceLow = changedPriceLow;
    }

    public boolean isReIssueSuccess() {
        return isReIssueSuccess;
    }

    public void setReIssueSuccess(boolean reIssueSuccess) {
        isReIssueSuccess = reIssueSuccess;
    }

    public boolean isPnrSplit() {
        return isPnrSplit;
    }

    public void setPnrSplit(boolean pnrSplit) {
        isPnrSplit = pnrSplit;
    }

    public Map<String, FareCheckRulesResponse> getFareCheckRulesResponseMap() {
        return fareCheckRulesResponseMap;
    }

    public void setFareCheckRulesResponseMap(Map<String, FareCheckRulesResponse> fareCheckRulesResponseMap) {
        this.fareCheckRulesResponseMap = fareCheckRulesResponseMap;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<FreeMealsDetails> getFreeMealsList() {
        return freeMealsDetailsList;
    }

    public void setFreeMealsList(List<FreeMealsDetails> freeMealsDetailsList) {
        this.freeMealsDetailsList = freeMealsDetailsList;
    }

    public List<FreeSeatDetails> getFreeSeatList() {
        return freeSeatList;
    }

    public void setFreeSeatList(List<FreeSeatDetails> freeSeatList) {
        this.freeSeatList = freeSeatList;
    }


    public Map<String, AmadeusSegmentRefDTO> getSegmentRefMap() {
        return segmentRefMap;
    }

    public void setSegmentRefMap(Map<String, AmadeusSegmentRefDTO> segmentRefMap) {
        this.segmentRefMap = segmentRefMap;
    }

}

