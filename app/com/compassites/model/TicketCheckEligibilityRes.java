package com.compassites.model;

import dto.refund.PerPaxRefundPricingInformation;

import java.math.BigDecimal;
import java.util.List;

public class TicketCheckEligibilityRes {
    private Boolean status;
    private BigDecimal refundableAmount;
    private String currency;
    private String formOfPayment;
    private ErrorMessage message;

    List<PerPaxRefundPricingInformation> perPaxRefundPricingInformationList;

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

    public List<PerPaxRefundPricingInformation> getPerPaxRefundPricingInformationList() {
        return perPaxRefundPricingInformationList;
    }

    public void setPerPaxRefundPricingInformationList(List<PerPaxRefundPricingInformation> perPaxRefundPricingInformationList) {
        this.perPaxRefundPricingInformationList = perPaxRefundPricingInformationList;
    }

}
