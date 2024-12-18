package com.compassites.model.traveller;

import com.compassites.model.BaggageDetails;
import com.compassites.model.MealDetails;
import com.compassites.model.MealDetailsMap;
import com.fasterxml.jackson.annotation.JsonIgnore;


import java.util.List;

import java.math.BigInteger;

import java.util.Map;

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

	private List<BaggageDetails> baggageDetails;

	private List<MealDetails> mealDetails;

  private String amadeusPaxRefQualifier;

	private BigInteger amadeusPaxRefNumber;

	private String amadeusPaxSegLineRef;
  
	public List<MealDetails> getMealDetails() {
		return mealDetails;
	}

	public void setMealDetails(List<MealDetails> mealDetails) {
		this.mealDetails = mealDetails;
	}

	private TravellerMasterInfo travellerMasterInfo;

    private Long contactId;

    private Map<String,String> ticketNumberMap;

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

	public List<BaggageDetails> getBaggageDetails() {
		return baggageDetails;
	}

	public void setBaggageDetails(List<BaggageDetails> baggageDetails) {
		this.baggageDetails = baggageDetails;
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

    public Long getContactId() {
        return contactId;
    }

    public void setContactId(Long contactId) {
        this.contactId = contactId;
    }

    public Map<String,String> getTicketNumberMap() {
        return ticketNumberMap;
    }

    public void setTicketNumberMap(Map<String, String> ticketNumberMap) {
        this.ticketNumberMap = ticketNumberMap;
    }

	public String getAmadeusPaxRefQualifier() {
		return amadeusPaxRefQualifier;
	}

	public void setAmadeusPaxRefQualifier(String amadeusPaxRefQualifier) {
		this.amadeusPaxRefQualifier = amadeusPaxRefQualifier;
	}

	public BigInteger getAmadeusPaxRefNumber() {
		return amadeusPaxRefNumber;
	}

	public void setAmadeusPaxRefNumber(BigInteger amadeusPaxRefNumber) {
		this.amadeusPaxRefNumber = amadeusPaxRefNumber;
	}

	public String getAmadeusPaxSegLineRef() {
		return amadeusPaxSegLineRef;
	}

	public void setAmadeusPaxSegLineRef(String amadeusPaxSegLineRef) {
		this.amadeusPaxSegLineRef = amadeusPaxSegLineRef;
	}

}
