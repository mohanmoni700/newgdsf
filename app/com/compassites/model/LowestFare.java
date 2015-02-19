package com.compassites.model;

import java.math.BigDecimal;

/**
 * @author Santhosh
 */
public class LowestFare {
	
	private String gdsPnr;
	
	private BigDecimal amount;

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

}
