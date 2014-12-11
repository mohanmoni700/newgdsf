package com.compassites.model.traveller;

import com.compassites.model.PassengerTypeCode;
import com.fasterxml.jackson.annotation.JsonIgnore;
import utils.DateUtility;

import java.util.Date;

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

    private String mobileNumber;

    private String email;

    private String officeNumber;

    private String emergencyContactNumber;

    private String countryCode;

    private String officeNoCode;

    private String emergencyContactCode;
    
    private String designation;

	public String getDesignation() {
		return designation;
	}

	public void setDesignation(String designation) {
		this.designation = designation;
	}

    @JsonIgnore
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

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getOfficeNumber() {
        return officeNumber;
    }

    public void setOfficeNumber(String officeNumber) {
        this.officeNumber = officeNumber;
    }

    public String getEmergencyContactNumber() {
        return emergencyContactNumber;
    }

    public void setEmergencyContactNumber(String emergencyContactNumber) {
        this.emergencyContactNumber = emergencyContactNumber;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getOfficeNoCode() {
        return officeNoCode;
    }

    public void setOfficeNoCode(String officeNoCode) {
        this.officeNoCode = officeNoCode;
    }

    public String getEmergencyContactCode() {
        return emergencyContactCode;
    }

    public void setEmergencyContactCode(String emergencyContactCode) {
        this.emergencyContactCode = emergencyContactCode;
    }
}
