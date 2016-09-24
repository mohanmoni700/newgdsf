package com.compassites.model;

import java.math.BigDecimal;

/**
 * @author Santhosh
 */
public class LowFareResponse {
	
	private String gdsPnr;
	
	private BigDecimal amount;
	
	private int maxBaggageWeight;
	
	private int baggageCount;

	private String bookingClass;

	public String getGdsPnr() {
		return gdsPnr;
	}

	public void setGdsPnr(String gdsPnr) {
		this.gdsPnr = gdsPnr;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public int getMaxBaggageWeight() {
		return maxBaggageWeight;
	}

	public void setMaxBaggageWeight(int maxBaggageWeight) {
		this.maxBaggageWeight = maxBaggageWeight;
	}

	public int getBaggageCount() {
		return baggageCount;
	}

	public void setBaggageCount(int baggageCount) {
		this.baggageCount = baggageCount;
	}

	public String getBookingClass() {
		return bookingClass;
	}

	public void setBookingClass(String bookingClass) {
		this.bookingClass = bookingClass;
	}
}
