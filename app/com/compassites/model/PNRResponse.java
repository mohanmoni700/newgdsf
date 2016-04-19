package com.compassites.model;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

/**
 * Created by user on 18-08-2014.
 */
public class PNRResponse {

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

    private Map<String, String> airlinePNRMap;

    private Map<String, FlightInfo> flightInfoMap;

    private Map<String, String> segmentBaggageMap;

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
}

