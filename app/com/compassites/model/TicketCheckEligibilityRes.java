package com.compassites.model;

import java.math.BigDecimal;

public class TicketCheckEligibilityRes {
    private Boolean status;
    private BigDecimal refundableAmount;
    private String currency;
    private String formOfPayment;
    private ErrorMessage message;

    public ErrorMessage getMessage() {
        return message;
    }

    public void setMessage(ErrorMessage message) {
        this.message = message;
    }

    public String getFormOfPayment() {
        return formOfPayment;
    }

    public void setFormOfPayment(String formOfPayment) {
        this.formOfPayment = formOfPayment;
    }
    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }

    public BigDecimal getRefundableAmount() {
        return refundableAmount;
    }

    public void setRefundableAmount(BigDecimal refundableAmount) {
        this.refundableAmount = refundableAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

}
