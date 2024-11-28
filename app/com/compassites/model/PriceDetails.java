package com.compassites.model;

import java.io.Serializable;
import java.math.BigDecimal;

public class PriceDetails implements Serializable {
	
	private Long id;
	
    private BigDecimal adtBasePrice;
	
    private BigDecimal chdBasePrice;
	
    private BigDecimal infBasePrice;
	
    private BigDecimal gdsAdtBasePrice;
	
    private BigDecimal gdsChdBasePrice;
	
    private BigDecimal gdsInfBasePrice;
    
    private PassengerTax passengerTax;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
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

	public BigDecimal getGdsAdtBasePrice() {
		return gdsAdtBasePrice;
	}

	public void setGdsAdtBasePrice(BigDecimal gdsAdtBasePrice) {
		this.gdsAdtBasePrice = gdsAdtBasePrice;
	}

	public BigDecimal getGdsChdBasePrice() {
		return gdsChdBasePrice;
	}

	public void setGdsChdBasePrice(BigDecimal gdsChdBasePrice) {
		this.gdsChdBasePrice = gdsChdBasePrice;
	}

	public BigDecimal getGdsInfBasePrice() {
		return gdsInfBasePrice;
	}

	public void setGdsInfBasePrice(BigDecimal gdsInfBasePrice) {
		this.gdsInfBasePrice = gdsInfBasePrice;
	}

	public PassengerTax getPassengerTax() {
		return passengerTax;
	}

	public void setPassengerTax(PassengerTax passengerTax) {
		this.passengerTax = passengerTax;
	}
    
}
