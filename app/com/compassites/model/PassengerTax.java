package com.compassites.model;

import java.math.BigDecimal;
import java.util.Map;

public class PassengerTax {

	private int passengerCount;

	private String passengerType;
	
	private BigDecimal totalTax;

//	private BigDecimal baseFare;

	private Map<String, BigDecimal> taxes;

	public int getPassengerCount() {
		return passengerCount;
	}

	public void setPassengerCount(int passengerCount) {
		this.passengerCount = passengerCount;
	}

	public String getPassengerType() {
		return passengerType;
	}

	public void setPassengerType(String passengerType) {
		this.passengerType = passengerType;
	}

//	public BigDecimal getBaseFare() {
//		return baseFare;
//	}
//
//	public void setBaseFare(BigDecimal baseFare) {
//		this.baseFare = baseFare;
//	}

	public Map<String, BigDecimal> getTaxes() {
		return taxes;
	}

	public BigDecimal getTotalTax() {
		if (totalTax == null) {
			totalTax = new BigDecimal(0);
			for (BigDecimal tax : taxes.values())
				totalTax = totalTax.add(tax);
		}
		return totalTax;
	}

	public void setTotalTax(BigDecimal totalTax) {
		this.totalTax = totalTax;
	}

	public void setTaxes(Map<String, BigDecimal> taxes) {
		this.taxes = taxes;
	}

}
