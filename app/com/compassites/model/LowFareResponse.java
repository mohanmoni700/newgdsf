package com.compassites.model;

import java.math.BigDecimal;
import java.util.Map;

/**
 * @author Santhosh
 */
public class LowFareResponse {
	
	private String gdsPnr;
	
	private BigDecimal amount;


	private Map<String, TSTLowestFare> tstLowestFareMap;

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


	public Map<String, TSTLowestFare> getTstLowestFareMap() {
		return tstLowestFareMap;
	}

	public void setTstLowestFareMap(Map<String, TSTLowestFare> tstLowestFareMap) {
		this.tstLowestFareMap = tstLowestFareMap;
	}
}
