package com.compassites.model.traveller;

/**
 * Created by mahendra-singh on 25/7/14.
 */
public class VisaDetails {

	public Traveller traveller;

	private Long id;

	private String destinationVisaNumber;

	private String usVisaNumber;

	private String schengenVisaNumber;

	public Long getId() {
		return id;
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
