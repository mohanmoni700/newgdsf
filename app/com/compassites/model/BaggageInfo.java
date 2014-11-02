package com.compassites.model;

import java.math.BigInteger;

/**
 * @author Santhosh
 */
public class BaggageInfo {
	
	private BigInteger value;
	
	private String unit;
	
	public BaggageInfo() {}
	
	public BaggageInfo(String unit, BigInteger value) {
		this.unit = unit;
		this.value = value;
	}

	public BigInteger getValue() {
		return value;
	}

	public void setValue(BigInteger value) {
		this.value = value;
	}

	public String getUnit() {
		return unit;
	}

	public void setUnit(String unit) {
		this.unit = unit;
	}

}
