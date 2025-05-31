package com.compassites.model;

import com.compassites.model.traveller.Traveller;
import dto.FareCheckRulesResponse;
import org.hibernate.mapping.Bag;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by user on 05-12-2014.
 */
public class IssuanceResponse implements Serializable{

    private boolean isCappingLimitReached;

    private boolean success;

    private BigDecimal cancellationFee;

    private BigDecimal lowestPossibleFare;

    private FlightItinerary flightItinerary;

    private String pnrNumber;

    private ErrorMessage errorMessage;

    private List<Traveller> travellerList;

    private String cancellationFeeText;

    private String bookingStatus;

    private String sessionIdRef;

    private String airlinePnr;

    private Date validTillDate;

    private boolean priceChanged;

    private String errorCode;

    private boolean isChangedPriceLow;

    private BigDecimal newLowerPrice;

    private boolean isIssued;

    private Map<String,FareCheckRulesResponse> fareCheckRulesResponseMap;

    private Map<String, String> airlinePNRMap;

    public void setErrorCode(String errorCode){
        this.errorCode = errorCode;
    }

    public String getErrorCode(){
        return errorCode;
    }
    public boolean isCappingLimitReached() {
        return isCappingLimitReached;
    }

    public void setCappingLimitReached(boolean isCappingLimitReached) {
        this.isCappingLimitReached = isCappingLimitReached;
    }
    private String baggage ;
    private String bookingId;

    private List<BaggageDetails> tmxBaggageDetails;

    public List<MealDetails> getTmxMealDetails() {
        return tmxMealDetails;
    }

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

    private String returnAirlinePnr;

    public String getReturnAirlinePnr() {
        return returnAirlinePnr;
    }

    public void setReturnAirlinePnr(String returnAirlinePnr) {
        this.returnAirlinePnr = returnAirlinePnr;
    }

    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public String getBaggage() {
        return baggage;
    }

    public void setBaggage(String baggage) {
        this.baggage = baggage;
    }

    private Map<String, String> ticketNumberMap;
    private Map<String, String> reticketNumberMap;

    public Map<String, String> getReticketNumberMap() {
        return reticketNumberMap;
    }

    public void setReticketNumberMap(Map<String, String> reticketNumberMap) {
        this.reticketNumberMap = reticketNumberMap;
    }

    public Map<String, String> getTicketNumberMap() {
        return ticketNumberMap;
    }

    public void setTicketNumberMap(Map<String, String> ticketNumberMap) {
        this.ticketNumberMap = ticketNumberMap;
    }

    public Date getValidTillDate() {
        return validTillDate;
    }

    public void setValidTillDate(Date validTillDate) {
        this.validTillDate = validTillDate;
    }
    public BigDecimal getCancellationFee() {
        return cancellationFee;
    }

    public void setCancellationFee(BigDecimal cancellationFee) {
        this.cancellationFee = cancellationFee;
    }

    public BigDecimal getLowestPossibleFare() {
        return lowestPossibleFare;
    }

    public void setLowestPossibleFare(BigDecimal lowestPossibleFare) {
        this.lowestPossibleFare = lowestPossibleFare;
    }

    public FlightItinerary getFlightItinerary() {
        return flightItinerary;
    }

    public void setFlightItinerary(FlightItinerary flightItinerary) {
        this.flightItinerary = flightItinerary;
    }

    public ErrorMessage getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(ErrorMessage errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getPnrNumber() {
        return pnrNumber;
    }

    public void setPnrNumber(String pnrNumber) {
        this.pnrNumber = pnrNumber;
    }

    public String getAirlinePnr() {
        return airlinePnr;
    }

    public void setAirlinePnr(String airlinePnr) {
        this.airlinePnr = airlinePnr;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public List<Traveller> getTravellerList() {
        return travellerList;
    }

    public void setTravellerList(List<Traveller> travellerList) {
        this.travellerList = travellerList;
    }

    public String getCancellationFeeText() {
        return cancellationFeeText;
    }

    public void setCancellationFeeText(String cancellationFeeText) {
        this.cancellationFeeText = cancellationFeeText;
    }

    public String getBookingStatus() {
        return bookingStatus;
    }

    public void setBookingStatus(String bookingStatus) {
        this.bookingStatus = bookingStatus;
    }

    public String getSessionIdRef() {
        return sessionIdRef;
    }

    public void setSessionIdRef(String sessionIdRef) {
        this.sessionIdRef = sessionIdRef;
    }

    public boolean isPriceChanged() {
        return priceChanged;
    }

    public void setIsPriceChanged(boolean isPriceChanged) {
        this.priceChanged = isPriceChanged;
    }

    public boolean getChangedPriceLow() {
        return isChangedPriceLow;
    }

    public void setChangedPriceLow(boolean isChangedPriceLow) {
        this.isChangedPriceLow = isChangedPriceLow;
    }

    public BigDecimal getNewLowerPrice() {return newLowerPrice;}

    public void setNewLowerPrice(BigDecimal newLowerPrice) {this.newLowerPrice = newLowerPrice;}

    public boolean getIssued() {
        return isIssued;
    }

    public void setIssued(boolean issued) {
        isIssued = issued;
    }

    public Map<String, String> getAirlinePNRMap() {
        return airlinePNRMap;
    }

    public void setAirlinePNRMap(Map<String, String> airlinePNRMap) {
        this.airlinePNRMap = airlinePNRMap;
    }

    public Map<String, FareCheckRulesResponse> getFareCheckRulesResponseMap() {
        return fareCheckRulesResponseMap;
    }

    public void setFareCheckRulesResponseMap(Map<String, FareCheckRulesResponse> fareCheckRulesResponseMap) {
        this.fareCheckRulesResponseMap = fareCheckRulesResponseMap;
    }
}
