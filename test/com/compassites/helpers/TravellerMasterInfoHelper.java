package com.compassites.helpers;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import com.compassites.model.FlightItinerary;
import com.compassites.model.Nationality;
import com.compassites.model.traveller.AdditionalInfo;
import com.compassites.model.traveller.PassportDetails;
import com.compassites.model.traveller.PersonalDetails;
import com.compassites.model.traveller.Traveller;
import com.compassites.model.traveller.TravellerMasterInfo;

public class TravellerMasterInfoHelper {
	
	public static TravellerMasterInfo getTravellerMasterInfo() {
		TravellerMasterInfo travellerMasterInfo = new TravellerMasterInfo();
		
		travellerMasterInfo.setItinerary(new FlightItinerary());

		AdditionalInfo additionalInfo = new AdditionalInfo();
		additionalInfo.setEmail("bruce.wayne@email.com");
		additionalInfo.setPhoneNumber("8098765432");
		travellerMasterInfo.setAdditionalInfo(additionalInfo);
		
		List<Traveller> travelers = new ArrayList<>();
		Traveller traveller = new Traveller();
		PassportDetails passportDetails = new PassportDetails();
		Calendar calendar = Calendar.getInstance();
		calendar.set(2020, 2, 20);
		passportDetails.setDateOfExpiry(calendar.getTime());
		Nationality nationality = new Nationality();
		nationality.setNationality("India");
		nationality.setPhoneCode("+91");
		nationality.setTwoLetterCode("IN");
		nationality.setThreeLetterCode("IND");
		passportDetails.setNationality(nationality);
		passportDetails.setPassportNumber("G8965777");
		traveller.setPassportDetails(passportDetails);
		PersonalDetails personalDetails = new PersonalDetails();
		calendar.set(1990, 9, 19);
		personalDetails.setDateOfBirth(calendar.getTime());
		personalDetails.setFirstName("Bruce");
		personalDetails.setLastName("Wayne");
		personalDetails.setGender("Male");
		personalDetails.setSalutation("Mr");
		traveller.setPersonalDetails(personalDetails);
		travelers.add(traveller);
		travellerMasterInfo.setTravellersList(travelers);
		
		return travellerMasterInfo;
	}

}
