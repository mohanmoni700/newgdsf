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
	
	private BigDecimal adtBasePrice;

    private BigDecimal chdBasePrice;
	
    private BigDecimal infBasePrice;

	private BigDecimal adtTotalPrice;

	private BigDecimal chdTotalPrice;

	private BigDecimal infTotalPrice;

	private BigDecimal tax;

	private BigDecimal totalPrice;

	private String currency;

    private String gdsCurrency;
	
	private BigDecimal totalPriceValue;

	private List<PassengerTax> passengerTaxes;

	private List<PAXFareDetails> paxFareDetailsList;

	private Map<String, BigDecimal> taxMap;

	private BigDecimal totalBasePrice;

    private BigDecimal totalTax;

    private BigDecimal discount;

	private BigDecimal totalCalculatedValue;

    private BigDecimal cancelFee;

	private String fareRules;

	private String provider;

	private boolean segmentWisePricing;

	private List<SegmentPricing> segmentPricingList;

	public PricingInformation() {
		paxFareDetailsList = new ArrayList<>();
		segmentWisePricing = false;
		segmentPricingList = new ArrayList<>();
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

	public BigDecimal getAdtBasePrice() {
		return adtBasePrice;
	}

	public void setAdtBasePrice(BigDecimal adtBasePrice) {
		this.adtBasePrice = adtBasePrice;
	}

	public BigDecimal getChdBasePrice() {
		return chdBasePrice;
	}

	public void setChdBasePrice(BigDecimal chdBasePrice) {
		this.chdBasePrice = chdBasePrice;
	}

	public BigDecimal getInfBasePrice() {
		return infBasePrice;
	}

	public void setInfBasePrice(BigDecimal infBasePrice) {
		this.infBasePrice = infBasePrice;
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

	public String getFareRules() {
		return fareRules;
	}

	public void setFareRules(String fareRules) {
		this.fareRules = fareRules;
	}
    public String getGdsCurrency() {
        return gdsCurrency;
    }

    public void setGdsCurrency(String gdsCurrency) {
        this.gdsCurrency = gdsCurrency;
    }

    public BigDecimal getTotalTax() {
        return totalTax;
    }

    public void setTotalTax(BigDecimal totalTax) {
        this.totalTax = totalTax;
    }

    public BigDecimal getCancelFee() {
        return cancelFee;
    }

    public void setCancelFee(BigDecimal cancelFee) {
        this.cancelFee = cancelFee;
    }

	public BigDecimal getAdtTotalPrice() {
		return adtTotalPrice;
	}

	public void setAdtTotalPrice(BigDecimal adtTotalPrice) {
		this.adtTotalPrice = adtTotalPrice;
	}

	public BigDecimal getChdTotalPrice() {
		return chdTotalPrice;
	}

	public void setChdTotalPrice(BigDecimal chdTotalPrice) {
		this.chdTotalPrice = chdTotalPrice;
	}

	public BigDecimal getInfTotalPrice() {
		return infTotalPrice;
	}

	public void setInfTotalPrice(BigDecimal infTotalPrice) {
		this.infTotalPrice = infTotalPrice;
	}

	public boolean isSegmentWisePricing() {
		return segmentWisePricing;
	}

	public void setSegmentWisePricing(boolean segmentWisePricing) {
		this.segmentWisePricing = segmentWisePricing;
	}

	public List<SegmentPricing> getSegmentPricingList() {
		return segmentPricingList;
	}

	public void setSegmentPricingList(List<SegmentPricing> segmentPricingList) {
		this.segmentPricingList = segmentPricingList;
	}
}
