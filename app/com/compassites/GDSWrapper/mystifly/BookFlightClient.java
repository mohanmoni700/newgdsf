package com.compassites.GDSWrapper.mystifly;

import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import onepoint.mystifly.BookFlightDocument;
import onepoint.mystifly.BookFlightResponseDocument;
import onepoint.mystifly.OnePointStub;

import org.datacontract.schemas._2004._07.mystifly_onepoint.AirBookRQ;
import org.datacontract.schemas._2004._07.mystifly_onepoint.AirBookRS;
import org.datacontract.schemas._2004._07.mystifly_onepoint.AirTraveler;
import org.datacontract.schemas._2004._07.mystifly_onepoint.ArrayOfAirTraveler;
import org.datacontract.schemas._2004._07.mystifly_onepoint.PassengerName;
import org.datacontract.schemas._2004._07.mystifly_onepoint.Passport;
import org.datacontract.schemas._2004._07.mystifly_onepoint.SessionCreateRS;
import org.datacontract.schemas._2004._07.mystifly_onepoint.TravelerInfo;

import utils.DateUtility;
import utils.XMLFileUtility;

import com.compassites.model.FlightItinerary;
import com.compassites.model.traveller.PassportDetails;
import com.compassites.model.traveller.PersonalDetails;
import com.compassites.model.traveller.Traveller;
import com.compassites.model.traveller.TravellerMasterInfo;

/**
 * @author Santhosh
 */
public class BookFlightClient {

	public AirBookRS bookFlight(TravellerMasterInfo travellerMasterInfo)
			throws RemoteException {
		SessionsHandler sessionsHandler = new SessionsHandler();
		SessionCreateRS sessionRS = sessionsHandler.login();
		OnePointStub onePointStub = sessionsHandler.getOnePointStub();

		FlightItinerary itinerary = travellerMasterInfo.getItinerary();
		String fareSourceCode = itinerary.getFareSourceCode();
		BookFlightDocument bookFlightDocument = BookFlightDocument.Factory
				.newInstance();
		AirBookRQ airBookRQ = bookFlightDocument.addNewBookFlight().addNewRq();
		airBookRQ.setSessionId(sessionRS.getSessionId());
		airBookRQ.setTarget(Mystifly.TARGET);
		airBookRQ.setFareSourceCode(fareSourceCode);
		TravelerInfo travelerInfo = airBookRQ.addNewTravelerInfo();
		ArrayOfAirTraveler arrayOfTravelers = travelerInfo.addNewAirTravelers();
		setTravelers(arrayOfTravelers, travellerMasterInfo.getTravellersList());
		PersonalDetails personalDetails = travellerMasterInfo
				.getTravellersList().get(0).getPersonalDetails();
		travelerInfo.setPhoneNumber(personalDetails.getCountryCode()
				+ personalDetails.getMobileNumber());
		travelerInfo.setEmail(personalDetails.getEmail());

		// TODO: Set dynamic values
		// travelerInfo.setAreaCode("809");
		// travelerInfo.setCountryCode("91");
		BookFlightResponseDocument rsDoc = onePointStub
				.bookFlight(bookFlightDocument);
		XMLFileUtility.createFile(rsDoc.getBookFlightResponse().getBookFlightResult().xmlText(), "AirBookRS.xml");
		return rsDoc.getBookFlightResponse().getBookFlightResult();
	}

	private void setTravelers(ArrayOfAirTraveler arrayOfTravelers,
			List<Traveller> travellers) {
		AirTraveler airTraveler = arrayOfTravelers.addNewAirTraveler();
		for (Traveller traveler : travellers) {
			setPersonalDetails(airTraveler, traveler.getPersonalDetails());
			setPassportDetails(airTraveler, traveler.getPassportDetails());
		}
	}

	private void setPersonalDetails(AirTraveler airTraveler,
			PersonalDetails personalDetails) {
		airTraveler.setGender(Mystifly.GENDER.get(personalDetails.getGender()
				.toLowerCase()));
		PassengerName passengerName = airTraveler.addNewPassengerName();
		passengerName.setPassengerFirstName(personalDetails.getFirstName());
		passengerName.setPassengerLastName(personalDetails.getLastName());
		passengerName.setPassengerTitle(Mystifly.PASSENGER_TITLE
				.get(personalDetails.getSalutation().toLowerCase()));
		airTraveler.setPassengerName(passengerName);
	}

	private void setPassportDetails(AirTraveler airTraveler,
			PassportDetails passportDetails) {
		Passport passport = airTraveler.addNewPassport();

		passport.setCountry(passportDetails.getNationality().getTwoLetterCode());
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(passportDetails.getDateOfExpiry());
		passport.setExpiryDate(calendar);
		passport.setPassportNumber(passportDetails.getPassportNumber());

		Date dob = passportDetails.getDateOfBirth();
		calendar.setTime(dob);
		airTraveler.setDateOfBirth(calendar);
		airTraveler.setPassengerType(Mystifly.PASSENGER_TYPE.get(DateUtility
				.getPassengerTypeFromDOB(dob)));
	}

}
