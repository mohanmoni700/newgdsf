package com.compassites.model.traveller;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by mahendra-singh on 25/7/14.
 */
public class CdcDetails {

	private Long id;

	private String cdcNumber;

	private String placeOfIssue;

	@JsonProperty("cdcDateOfIssue")
	private Date dateOfIssue;

	@JsonProperty("cdcDateOfExpiry")
	private Date dateOfExpiry;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getCdcNumber() {
		return cdcNumber;
	}

	public void setCdcNumber(String cdcNumber) {
		this.cdcNumber = cdcNumber;
	}

	public String getPlaceOfIssue() {
		return placeOfIssue;
	}

	public void setPlaceOfIssue(String placeOfIssue) {
		this.placeOfIssue = placeOfIssue;
	}

	public Date getDateOfIssue() {
		return dateOfIssue;
	}

	public void setDateOfIssue(Date dateOfIssue) {
		this.dateOfIssue = dateOfIssue;
	}

	public Date getDateOfExpiry() {
		return dateOfExpiry;
	}

	public void setDateOfExpiry(Date dateOfExpiry) {
		this.dateOfExpiry = dateOfExpiry;
	}
}
