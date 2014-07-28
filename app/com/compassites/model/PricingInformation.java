package com.compassites.model;

/**
 * Created by mahendra-singh on 23/5/14.
 */
public class PricingInformation {
    private String basePrice;
    private String tax;
    private String totalPrice;
    private String currency;
    private Long totalPriceValue;

    public PricingInformation() {
    }

    public String getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(String basePrice) {
        this.basePrice = basePrice;
    }

    public String getTax() {
        return tax;
    }

    public void setTax(String tax) {
        this.tax = tax;
    }

    public String getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(String totalPrice) {
        this.totalPrice = totalPrice;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Long getTotalPriceValue() {
        return totalPriceValue;
    }

    public void setTotalPriceValue(Long totalPriceValue) {
        this.totalPriceValue = totalPriceValue;
    }
}
