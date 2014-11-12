package com.compassites.model.traveller;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Created by mahendra-singh on 25/7/14.
 */
public class Traveller {

	@JsonIgnore
	private Long id;

	private PersonalDetails personalDetails;

	private Preferences preferences;

	private PassportDetails passportDetails;

	private CdcDetails cdcDetails;

	private VisaDetails visaDetails;

	private TravellerMasterInfo travellerMasterInfo;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public PersonalDetails getPersonalDetails() {
		return personalDetails;
	}

	public void setPersonalDetails(PersonalDetails personalDetails) {
		this.personalDetails = personalDetails;
	}

	public Preferences getPreferences() {
		return preferences;
	}

	public void setPreferences(Preferences preferences) {
		this.preferences = preferences;
	}

	public PassportDetails getPassportDetails() {
		return passportDetails;
	}

	public void setPassportDetails(PassportDetails passportDetails) {
		this.passportDetails = passportDetails;
	}

	public CdcDetails getCdcDetails() {
		return cdcDetails;
	}

	public void setCdcDetails(CdcDetails cdcDetails) {
		this.cdcDetails = cdcDetails;
	}

	public VisaDetails getVisaDetails() {
		return visaDetails;
	}

	public void setVisaDetails(VisaDetails visaDetails) {
		this.visaDetails = visaDetails;
	}

}
