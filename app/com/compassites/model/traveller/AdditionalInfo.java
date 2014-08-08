package com.compassites.model.traveller;

/**
 * Created by user on 07-08-2014.
 */
public class AdditionalInfo {

    private String phoneNumber;

    private String email;

    private String emergencyContact;

    private String vesselName;

    private String vesselRegistrationCountry;

    private String purposeOfTravel;



    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEmergencyContact() {
        return emergencyContact;
    }

    public void setEmergencyContact(String emergencyContact) {
        this.emergencyContact = emergencyContact;
    }

    public String getVesselName() {
        return vesselName;
    }

    public void setVesselName(String vesselName) {
        this.vesselName = vesselName;
    }

    public String getVesselRegistrationCountry() {
        return vesselRegistrationCountry;
    }

    public void setVesselRegistrationCountry(String vesselRegistrationCountry) {
        this.vesselRegistrationCountry = vesselRegistrationCountry;
    }

    public String getPurposeOfTravel() {
        return purposeOfTravel;
    }

    public void setPurposeOfTravel(String purposeOfTravel) {
        this.purposeOfTravel = purposeOfTravel;
    }
}
