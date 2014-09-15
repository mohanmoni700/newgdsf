package com.compassites.model;

/**
 * Created by user on 18-08-2014.
 */
public class PNRResponse {

    private  String pnrNumber;

    private ErrorMessage errorMessage;

    private String validTillDate;

    private boolean isPriceChanged;

    private Long originalPrice;

    private Long changedPrice;

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
}
