package com.compassites.model;

import java.math.BigInteger;
import java.util.List;

/**
 * @author Santhosh
 */
public class FlightInfo {
	
	private BigInteger baggageAllowance;
	
	private String baggageUnit;
	
	private List<String> amneties;

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

	public List<String> getAmneties() {
		return amneties;
	}

	public void setAmneties(List<String> amneties) {
		this.amneties = amneties;
	}

}
