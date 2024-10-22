
package com.compassites.model.travelomatrix;

import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

import java.util.List;


public class Passenger {

    @JsonProperty("AddressLine1")
    private String addressLine1;
    @JsonProperty("City")
    private String city;
    @JsonProperty("ContactNo")
    private String contactNo;
    @JsonProperty("CountryCode")
    private String countryCode;
    @JsonProperty("CountryName")
    private String countryName;
    @JsonProperty("DateOfBirth")
    private String dateOfBirth;
    @JsonProperty("Email")
    private String email;
    @JsonProperty("FirstName")
    private String firstName;
    @JsonProperty("Gender")
    private Long gender;
    @JsonProperty("IsLeadPax")
    private String isLeadPax;
    @JsonProperty("LastName")
    private String lastName;
    @JsonProperty("PassportNumber")
    private String passportNumber;
    @JsonProperty("PassportExpiry")
    private String passportExpiry;
    @JsonProperty("PaxType")
    private Long paxType;
    @JsonProperty("PinCode")
    private String pinCode;
    @JsonProperty("Title")
    private String title;
    @JsonProperty("BaggageId")
    public List<String> baggageId;
    @JsonProperty("MealId")
    public List<String> mealId;
    @JsonProperty("SeatId")
    public List<String> seatId;

    public List<String> getBaggageId() {
        return baggageId;
    }

    public void setBaggageId(List<String> baggageId) {
        this.baggageId = baggageId;
    }

    public List<String> getMealId() {
        return mealId;
    }

    public void setMealId(List<String> mealId) {
        this.mealId = mealId;
    }

    public List<String> getSeatId() {
        return seatId;
    }

    public void setSeatId(List<String> seatId) {
        this.seatId = seatId;
    }

    public String getPassportExpiry() {
        return passportExpiry;
    }

    public void setPassportExpiry(String passportExpiry) {
        this.passportExpiry = passportExpiry;
    }


    public String getAddressLine1() {
        return addressLine1;
    }

    public void setAddressLine1(String addressLine1) {
        this.addressLine1 = addressLine1;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getContactNo() {
        return contactNo;
    }

    public void setContactNo(String contactNo) {
        this.contactNo = contactNo;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getCountryName() {
        return countryName;
    }

    public void setCountryName(String countryName) {
        this.countryName = countryName;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public Long getGender() {
        return gender;
    }

    public void setGender(Long gender) {
        this.gender = gender;
    }

    public String getIsLeadPax() {
        return isLeadPax;
    }

    public void setIsLeadPax(String isLeadPax) {
        this.isLeadPax = isLeadPax;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPassportNumber() {
        return passportNumber;
    }

    public void setPassportNumber(String passportNumber) {
        this.passportNumber = passportNumber;
    }

    public Long getPaxType() {
        return paxType;
    }

    public void setPaxType(Long paxType) {
        this.paxType = paxType;
    }

    public String getPinCode() {
        return pinCode;
    }

    public void setPinCode(String pinCode) {
        this.pinCode = pinCode;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

}
