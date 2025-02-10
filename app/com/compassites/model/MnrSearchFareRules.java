package com.compassites.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MnrSearchFareRules implements Serializable {

    private String provider;
    private BigDecimal changeFee;
    private BigDecimal cancellationFee;
    private Boolean isChangeBeforeDepartureAllowed;
    private Boolean isCancellationBeforeDepartureAllowed;

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public BigDecimal getChangeFee() {
        return changeFee;
    }

    public void setChangeFee(BigDecimal changeFee) {
        this.changeFee = changeFee;
    }

    public BigDecimal getCancellationFee() {
        return cancellationFee;
    }

    public void setCancellationFee(BigDecimal cancellationFee) {
        this.cancellationFee = cancellationFee;
    }

    public boolean getIsChangeBeforeDepartureAllowed() {
        return isChangeBeforeDepartureAllowed;
    }

    public void setChangeBeforeDepartureAllowed(boolean changeBeforeDepartureAllowed) {
        isChangeBeforeDepartureAllowed = changeBeforeDepartureAllowed;
    }

    public boolean getIsCancellationBeforeDepartureAllowed() {
        return isCancellationBeforeDepartureAllowed;
    }

    public void setCancellationBeforeDepartureAllowed(boolean cancellationBeforeDepartureAllowed) {
        isCancellationBeforeDepartureAllowed = cancellationBeforeDepartureAllowed;
    }


}
