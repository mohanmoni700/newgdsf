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
import org.datacontract.schemas._2004._07.mystifly_onepoint.PassengerType;
import org.datacontract.schemas._2004._07.mystifly_onepoint.Passport;
import org.datacontract.schemas._2004._07.mystifly_onepoint.SessionCreateRS;
import org.datacontract.schemas._2004._07.mystifly_onepoint.Target;
import org.datacontract.schemas._2004._07.mystifly_onepoint.TravelerInfo;
import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.joda.time.PeriodType;

import com.compassites.model.FlightItinerary;
import com.compassites.model.traveller.AdditionalInfo;
import com.compassites.model.traveller.PassportDetails;
import com.compassites.model.traveller.PersonalDetails;
import com.compassites.model.traveller.Traveller;
import com.compassites.model.traveller.TravellerMasterInfo;

/**
 * @author Santhosh
 */
public class BookFlightClient {

	public AirBookRS bookFlight(TravellerMasterInfo travellerMasterInfo) {
		SessionsHandler sessionsHandler = new SessionsHandler();
		SessionCreateRS sessionRS = sessionsHandler.login();
		OnePointStub onePointStub = sessionsHandler.getOnePointStub();

		FlightItinerary itinerary = travellerMasterInfo.getItinerary();
		String fareSourceCode = itinerary.getFareSourceCode();
		AirBookRS airBookRS = null;
		try {
			BookFlightDocument bookFlightDocument = BookFlightDocument.Factory
					.newInstance();
			AirBookRQ airBookRQ = bookFlightDocument.addNewBookFlight()
					.addNewRq();

			airBookRQ.setSessionId(sessionRS.getSessionId());
			airBookRQ.setFareSourceCode(fareSourceCode);
			TravelerInfo travelerInfo = airBookRQ.addNewTravelerInfo();

			ArrayOfAirTraveler arrayOfTravelers = travelerInfo
					.addNewAirTravelers();
			setTravelers(arrayOfTravelers,
					travellerMasterInfo.getTravellersList());

			AdditionalInfo addInfo = travellerMasterInfo.getAdditionalInfo();
			travelerInfo.setPhoneNumber(addInfo.getPhoneNumber());
			travelerInfo.setEmail(addInfo.getEmail());

			// TODO: Set dynamic values
			travelerInfo.setAreaCode("809");
			travelerInfo.setCountryCode("91");
			airBookRQ.setTarget(Target.TEST);

			BookFlightResponseDocument rsDoc = onePointStub
					.bookFlight(bookFlightDocument);
			airBookRS = rsDoc.getBookFlightResponse().getBookFlightResult();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return airBookRS;
	}

	private void setTravelers(ArrayOfAirTraveler arrayOfTravelers,
			List<Traveller> travellers) {
		AirTraveler airTraveler = arrayOfTravelers.addNewAirTraveler();
		for (Traveller traveler : travellers) {
			setPersonalDetails(airTraveler, traveler.getPersonalDetails());
			// setPassportDetails(airTraveler, traveler.getPassportDetails());
		}
	}

	private void setPersonalDetails(AirTraveler airTraveler,
			PersonalDetails personalDetails) {
		Calendar calendar = Calendar.getInstance();
		if (personalDetails.getDateOfBirth() != null) {
			calendar.setTime(personalDetails.getDateOfBirth());
		} else {
			calendar.set(1990, 1, 1);
			calendar.setTime(calendar.getTime());
		}

		String passengerType = getPassengerType(calendar.getTime());
		airTraveler.setPassengerType(PassengerType.Enum
				.forString(passengerType));
		airTraveler.setGender(Constants.GENDER.get(personalDetails.getGender()
				.toLowerCase()));
		PassengerName passengerName = airTraveler.addNewPassengerName();
		passengerName.setPassengerFirstName(personalDetails.getFirstName());
		passengerName.setPassengerLastName(personalDetails.getLastName());
		passengerName.setPassengerTitle(Constants.PASSENGER_TITLE
				.get(personalDetails.getSalutation().toLowerCase()));
		airTraveler.setPassengerName(passengerName);
		airTraveler.setDateOfBirth(calendar);
	}

	private void setPassportDetails(AirTraveler airTraveler,
			PassportDetails passportDetails) {
		Passport passport = airTraveler.addNewPassport();

		// TODO: Get Country code
		// passport.setCountry(passportDetails.getPassportCountry());
		passport.setCountry("IN");
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(passportDetails.getDateOfExpiry());
		passport.setExpiryDate(calendar);
		passport.setPassportNumber(passportDetails.getPassportNumber());
	}

	// TODO: Move to a Util class
	private String getPassengerType(Date dob) {
		LocalDate birthdate = new LocalDate(dob);
		LocalDate now = new LocalDate();
		Period period = new Period(birthdate, now, PeriodType.yearMonthDay());
		int age = period.getYears();
		String passengerType;
		if (age <= 2) {
			passengerType = "INF";
		} else if (age <= 12) {
			passengerType = "CH";
		} else {
			passengerType = "ADT";
		}
		return passengerType;
	}

}
