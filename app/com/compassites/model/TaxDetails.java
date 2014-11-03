package com.compassites.model;

import java.math.BigDecimal;

/**
 * Created by user on 31-10-2014.
 */

public class TaxDetails {

    private String taxCode;

    private BigDecimal taxAmount;


    public String getTaxCode() {
        return taxCode;
    }

    public void setTaxCode(String taxCode) {
        this.taxCode = taxCode;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }
}
