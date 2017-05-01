package com.compassites.GDSWrapper.mystifly;

import com.compassites.constants.AmadeusConstants;
import com.compassites.model.FlightItinerary;
import com.compassites.model.traveller.*;
import com.travelport.schema.common_v26_0.LoyaltyCard;
import onepoint.mystifly.BookFlightDocument;
import onepoint.mystifly.BookFlightResponseDocument;
import onepoint.mystifly.OnePointStub;
import org.apache.xmlbeans.XmlDateTime;
import org.datacontract.schemas._2004._07.mystifly_onepoint.*;
import org.datacontract.schemas._2004._07.mystifly_onepoint.Preferences;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import utils.DateUtility;
import utils.XMLFileUtility;

import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * @author Santhosh
 */
public class BookFlightClient {

    static Logger mystiflyLogger = LoggerFactory.getLogger("mystifly");

	public AirBookRS bookFlight(FlightItinerary itinerary, List<Traveller> travellerList, String userTimezone)
			throws RemoteException {
		SessionsHandler sessionsHandler = new SessionsHandler();
		//SessionCreateRS sessionRS = sessionsHandler.login();
		String sessoinId = sessionsHandler.mystiflySessionHandler();
		OnePointStub onePointStub = sessionsHandler.getOnePointStub();

//		FlightItinerary itinerary = travellerMasterInfo.getItinerary();
		String fareSourceCode = itinerary.getFareSourceCode();
		BookFlightDocument bookFlightDocument = BookFlightDocument.Factory
				.newInstance();
		AirBookRQ airBookRQ = bookFlightDocument.addNewBookFlight().addNewRq();
		airBookRQ.setSessionId(sessoinId);
		airBookRQ.setTarget(Mystifly.TARGET);
		airBookRQ.setFareSourceCode(fareSourceCode);
		TravelerInfo travelerInfo = airBookRQ.addNewTravelerInfo();
		ArrayOfAirTraveler arrayOfTravelers = travelerInfo.addNewAirTravelers();
		setTravelers(arrayOfTravelers, travellerList, userTimezone);
		PersonalDetails personalDetails = travellerList.get(0).getPersonalDetails();
		travelerInfo.setPhoneNumber(personalDetails.getMobileNumber());
		travelerInfo.setCountryCode(personalDetails.getCountryCode());

		travelerInfo.setEmail(personalDetails.getEmail());

		// TODO: Set dynamic values
//		 travelerInfo.setAreaCode("809");
		//XMLFileUtility.createFile(airBookRQ.xmlText(), "AirBookRQ.xml");
        mystiflyLogger.debug("AirBookRQ "+ new Date() +" ----->>" + airBookRQ.xmlText());
		BookFlightResponseDocument rsDoc = onePointStub.bookFlight(bookFlightDocument);
		//XMLFileUtility.createFile(rsDoc.getBookFlightResponse().getBookFlightResult().xmlText(), "AirBookRS.xml");
        mystiflyLogger.debug("AirBookRS "+ new Date() +" ----->>" + rsDoc.getBookFlightResponse().getBookFlightResult().xmlText());
		return rsDoc.getBookFlightResponse().getBookFlightResult();
	}

	private void setTravelers(ArrayOfAirTraveler arrayOfTravelers,
			List<Traveller> travellers, String userTimezone) {
		for (Traveller traveler : travellers) {
			AirTraveler airTraveler = arrayOfTravelers.addNewAirTraveler();
			setPersonalDetails(airTraveler, traveler.getPersonalDetails());
			setPassportDetails(airTraveler, traveler.getPassportDetails(), userTimezone);
			setSeatAndFrequentFlyerNumber(airTraveler, traveler.getPreferences());
		}
	}

	private void setPersonalDetails(AirTraveler airTraveler,
			PersonalDetails personalDetails) {
		airTraveler.setGender(Mystifly.GENDER.get(personalDetails.getGender()
				.toLowerCase()));
		PassengerName passengerName = airTraveler.addNewPassengerName();
		passengerName.setPassengerFirstName(personalDetails.getFirstName());
		passengerName.setPassengerLastName(personalDetails.getLastName());
		String title = personalDetails.getSalutation().toLowerCase();
		if(!title.contains(".")){
			title = title + ".";
		}
		passengerName.setPassengerTitle(Mystifly.PASSENGER_TITLE
				.get(title));
		airTraveler.setPassengerName(passengerName);
	}

	private void setPassportDetails(AirTraveler airTraveler,
			PassportDetails passportDetails, String userTimezone) {
		Passport passport = airTraveler.addNewPassport();

		passport.setCountry(passportDetails.getNationality().getTwoLetterCode());
		passport.setPassportNumber(passportDetails.getPassportNumber());
		DateTimeZone dateTimeZone  = DateTimeZone.forID(userTimezone);

		DateTime dob = new DateTime(passportDetails.getDateOfBirth()).withZone(dateTimeZone);
		dob = dob.withHourOfDay(0);
		dob = dob.withMinuteOfHour(0);
		dob = dob.withSecondOfMinute(0);
		Calendar calendar1 = dob.toGregorianCalendar();
		calendar1.clear(Calendar.ZONE_OFFSET);
		airTraveler.setDateOfBirth(calendar1);

		DateTime dateOfExpiry = new DateTime(passportDetails.getDateOfExpiry()).withZone(dateTimeZone);
		dateOfExpiry = dateOfExpiry.withHourOfDay(0);
		dateOfExpiry = dateOfExpiry.withMinuteOfHour(0);
		dateOfExpiry = dateOfExpiry.withSecondOfMinute(0);
		Calendar calendar = dateOfExpiry.toGregorianCalendar();
		calendar.clear(Calendar.ZONE_OFFSET);
		passport.setExpiryDate(calendar);


		airTraveler.setPassengerType(Mystifly.PASSENGER_TYPE.get(DateUtility
				.getPassengerTypeFromDOB(dob.toDate())));
		airTraveler.setPassengerNationality(passportDetails.getNationality().getTwoLetterCode());
	}


	private void setSeatAndFrequentFlyerNumber(AirTraveler airTraveler, com.compassites.model.traveller.Preferences preferences){
		SpecialServiceRequest specialServiceRequest = SpecialServiceRequest.Factory.newInstance();

		if(StringUtils.hasText(preferences.getSeatPreference()) && !"any".equalsIgnoreCase(preferences.getSeatPreference())){
			SeatPreference seatPreference = SeatPreference.Factory.newInstance();
			if("aisle".equalsIgnoreCase(preferences.getSeatPreference())){
				specialServiceRequest.setSeatPreference(SeatPreference.A);
			}else if("window".equalsIgnoreCase(preferences.getSeatPreference())){
				specialServiceRequest.setSeatPreference(SeatPreference.W);
			}
			airTraveler.setSpecialServiceRequest(specialServiceRequest);
		}

		if(preferences != null && StringUtils.hasText(preferences.getFrequentFlyerNumber())){
			airTraveler.setFrequentFlyerNumber(preferences.getFrequentFlyerNumber());
		}
	}



}
