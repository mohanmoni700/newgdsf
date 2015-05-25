package com.compassites.model;

import com.compassites.model.traveller.Traveller;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

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


    public boolean isCappingLimitReached() {
        return isCappingLimitReached;
    }

    public void setCappingLimitReached(boolean isCappingLimitReached) {
        this.isCappingLimitReached = isCappingLimitReached;
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
}
