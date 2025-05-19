package com.compassites.model;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

/**
 * @author Santhosh
 */
public class FlightInfo {
	
	private BigInteger baggageAllowance;
	
	private String baggageUnit;
	
	private List<String> amenities;

	private Map<String,Double> carbonDioxide;

	public Map<String, Double> getCarbonDioxide() {
		return carbonDioxide;
	}

	public void setCarbonDioxide(Map<String, Double> carbonDioxide) {
		this.carbonDioxide = carbonDioxide;
	}

	public BigInteger getBaggageAllowance() {
		return baggageAllowance;
	}

	public void setBaggageAllowance(BigInteger value) {
		this.baggageAllowance = value;
	}

	public String getBaggageUnit() {
		return baggageUnit;
	}

	public void setBaggageUnit(String unit) {
		this.baggageUnit = unit;
	}

	public List<String> getAmenities() {
		return amenities;
	}

	public void setAmenities(List<String> amenities) {
		this.amenities = amenities;
	}

}
