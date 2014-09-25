package com.compassites.GDSWrapper.mystifly;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.datacontract.schemas._2004._07.mystifly_onepoint.AirBookRS;
import org.junit.Test;

import com.compassites.model.FlightItinerary;
import com.compassites.model.traveller.AdditionalInfo;
import com.compassites.model.traveller.PassportDetails;
import com.compassites.model.traveller.PersonalDetails;
import com.compassites.model.traveller.Traveller;
import com.compassites.model.traveller.TravellerMasterInfo;

public class BookFlightClientTest {

	@Test
	public void test() {
		BookFlightClient bookFlightClient = new BookFlightClient();
		AirBookRS bookRS = bookFlightClient.bookFlight(getTravellerMasterInfo());
		assertTrue(bookRS.getSuccess());
	}
	
	
	public TravellerMasterInfo getTravellerMasterInfo() {
		String fareSourceCode = "MTUwMDAwMyZISCZISCY4YmFmMGU5Zi02MmQyLTRjYzgtOGE2NC1lNGExYTEzNjBmZDEmSEg=";
		TravellerMasterInfo travellerMasterInfo = new TravellerMasterInfo();
		
		FlightItinerary itinerary = new FlightItinerary();
		itinerary.setFareSourceCode(fareSourceCode);
		travellerMasterInfo.setItinerary(itinerary);
		
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
		passportDetails.setPassportCountry("IN");
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
