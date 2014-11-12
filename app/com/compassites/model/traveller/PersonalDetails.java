package com.compassites.model.traveller;

import java.util.Date;

import utils.DateUtility;

import com.compassites.model.PassengerTypeCode;

/**
 * Created by mahendra-singh on 25/7/14.
 */
public class PersonalDetails {

	private Long id;

	private String salutation;

	private String firstName;

	private String middleName;

	private String lastName;

	private Date dateOfBirth;

	private String rank;

	private String gender;

	public PassengerTypeCode getPassengerType() {
		return DateUtility.getPassengerTypeFromDOB(dateOfBirth);
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getSalutation() {
		return salutation;
	}

	public void setSalutation(String salutation) {
		this.salutation = salutation;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getMiddleName() {
		return middleName;
	}

	public void setMiddleName(String middleName) {
		this.middleName = middleName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public Date getDateOfBirth() {
		return dateOfBirth;
	}

	public void setDateOfBirth(Date dateOfBirth) {
		this.dateOfBirth = dateOfBirth;
	}

	public String getRank() {
		return rank;
	}

	public void setRank(String rank) {
		this.rank = rank;
	}

	public String getGender() {
		return gender;
	}

	public void setGender(String gender) {
		this.gender = gender;
	}
}
