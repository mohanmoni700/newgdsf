package com.compassites.model;

import java.util.List;

/**
 * Created by user on 18-08-2014.
 */
public class PNRResponse {

    private  String pnrNumber;

    private ErrorMessage errorMessage;

    private String validTillDate;

    private boolean isPriceChanged;

    private boolean isFlightAvailable;

    private Long originalPrice;

    private Long changedPrice;

    private String priceChangeKey;

    private boolean isCappingLimitReached;

    private List<TaxDetails> taxDetailsList;

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

    public String getValidTillDate() {
        return validTillDate;
    }

    public void setValidTillDate(String validTillDate) {
        this.validTillDate = validTillDate;
    }

    public boolean isPriceChanged() {
        return isPriceChanged;
    }

    public void setPriceChanged(boolean isPriceChanged) {
        this.isPriceChanged = isPriceChanged;
    }

    public Long getOriginalPrice() {
        return originalPrice;
    }

    public void setOriginalPrice(Long originalPrice) {
        this.originalPrice = originalPrice;
    }

    public Long getChangedPrice() {
        return changedPrice;
    }

    public void setChangedPrice(Long changedPrice) {
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

    public List<TaxDetails> getTaxDetailsList() {
        return taxDetailsList;
    }

    public void setTaxDetailsList(List<TaxDetails> taxDetailsList) {
        this.taxDetailsList = taxDetailsList;
    }
}

