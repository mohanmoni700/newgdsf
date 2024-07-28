package com.compassites.model.travelomatrix;

import java.io.Serializable;
import java.util.ArrayList;

public class TMPassenger implements Serializable {

    public String isLeadPax;

    public String title;

    public String firstName;

    public String lastName;

    public String paxType;

    public String gender;

    public String dateOfBirth;

    public String passportNumber;

    public String passportExpiry;

    public String countryCode;

    public String countryName;

    public String contactNo;

    public String city;

    public String pinCode;

    public String addressLine1;

    public String addressLine2;

    public String email;

    public ArrayList<String> baggageId;

    public ArrayList<String> mealId;

    public ArrayList<String> seatId;
}
