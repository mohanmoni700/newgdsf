package com.compassites.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by mahendra-singh on 23/5/14.
 */
public class PricingInformation {

	private boolean isLCC;
	
	private BigDecimal basePrice;
	
	private BigDecimal tax;
	
	private BigDecimal totalPrice;
	
	private String currency;
	
	private BigDecimal totalPriceValue;
	
	private List<PassengerTax> passengerTaxes;

	private List<PAXFareDetails> paxFareDetailsList;
	
	private Map<String, BigDecimal> taxMap;

    private BigDecimal totalBasePrice;

    private BigDecimal discount;

    private BigDecimal totalCalculatedValue;

	private String provider;

	public PricingInformation() {
		paxFareDetailsList = new ArrayList<>();
	}

	public boolean isLCC() {
		return isLCC;
	}

	public void setLCC(boolean isLCC) {
		this.isLCC = isLCC;
	}

	public BigDecimal getBasePrice() {
		return basePrice;
	}

	public void setBasePrice(BigDecimal basePrice) {
		this.basePrice = basePrice;
	}

	public BigDecimal getTax() {
		return tax;
	}

	public void setTax(BigDecimal tax) {
		this.tax = tax;
	}

	public BigDecimal getTotalPrice() {
		return totalPrice;
	}

	public void setTotalPrice(BigDecimal totalPrice) {
		this.totalPrice = totalPrice;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public BigDecimal getTotalPriceValue() {
		return totalPriceValue;
	}

	public void setTotalPriceValue(BigDecimal totalPriceValue) {
		this.totalPriceValue = totalPriceValue;
	}

	public List<PassengerTax> getPassengerTaxes() {
		return passengerTaxes;
	}

	public void setPassengerTaxes(List<PassengerTax> passengerTaxes) {
		this.passengerTaxes = passengerTaxes;
	}

	public List<PAXFareDetails> getPaxFareDetailsList() {
		return paxFareDetailsList;
	}

	public void setPaxFareDetailsList(List<PAXFareDetails> paxFareDetailsList) {
		this.paxFareDetailsList = paxFareDetailsList;
	}

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	public Map<String, BigDecimal> getTaxMap() {
		return taxMap;
	}

	public void setTaxMap(Map<String, BigDecimal> taxMap) {
		this.taxMap = taxMap;
	}

    public BigDecimal getTotalBasePrice() {
        return totalBasePrice;
    }

    public void setTotalBasePrice(BigDecimal totalBasePrice) {
        this.totalBasePrice = totalBasePrice;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public void setDiscount(BigDecimal discount) {
        this.discount = discount;
    }

    public BigDecimal getTotalCalculatedValue() {
        return totalCalculatedValue;
    }

    public void setTotalCalculatedValue(BigDecimal totalCalculatedValue) {
        this.totalCalculatedValue = totalCalculatedValue;
    }


}
