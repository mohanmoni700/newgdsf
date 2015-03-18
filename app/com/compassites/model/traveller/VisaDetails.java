package com.compassites.model.traveller;

import java.util.Date;

/**
 * Created by mahendra-singh on 25/7/14.
 */
public class VisaDetails {

	public Traveller traveller;

	private Long id;

	private String destinationVisaNumber;

	private String usVisaNumber;

	private String schengenVisaNumber;

	private Date schengenVisaDate;

	private Date destinationVisaDate;

	private Date usVisaDate;

	public Long getId() {
		return id;
	}

	public Date getSchengenVisaDate() {
		return schengenVisaDate;
	}

	public void setSchengenVisaDate(Date schengenVisaDate) {
		this.schengenVisaDate = schengenVisaDate;
	}

	public Date getDestinationVisaDate() {
		return destinationVisaDate;
	}

	public void setDestinationVisaDate(Date destinationVisaDate) {
		this.destinationVisaDate = destinationVisaDate;
	}

	public Date getUsVisaDate() {
		return usVisaDate;
	}

	public void setUsVisaDate(Date usVisaDate) {
		this.usVisaDate = usVisaDate;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getDestinationVisaNumber() {
		return destinationVisaNumber;
	}

	public void setDestinationVisaNumber(String destinationVisaNumber) {
		this.destinationVisaNumber = destinationVisaNumber;
	}

	public String getUsVisaNumber() {
		return usVisaNumber;
	}

	public void setUsVisaNumber(String usVisaNumber) {
		this.usVisaNumber = usVisaNumber;
	}

	public String getSchengenVisaNumber() {
		return schengenVisaNumber;
	}

	public void setSchengenVisaNumber(String schengenVisaNumber) {
		this.schengenVisaNumber = schengenVisaNumber;
	}
}
