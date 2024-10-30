package com.compassites.model;

import java.math.BigDecimal;
import java.util.List;

public class TicketProcessRefundRes {
    private Boolean status;
    private List<String> refTicketsList;
    private String refundableAmount;
    private String currency;

    private ErrorMessage message;

    public ErrorMessage getMessage() {
        return message;
    }

    public void setMessage(ErrorMessage message) {
        this.message = message;
    }

    public String getRefundableAmount() {
        return refundableAmount;
    }

    public void setRefundableAmount(String refundableAmount) {
        this.refundableAmount = refundableAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }

    public List<String> getRefTicketsList() {
        return refTicketsList;
    }

    public void setRefTicketsList(List<String> refTicketsList) {
        this.refTicketsList = refTicketsList;
    }
}
