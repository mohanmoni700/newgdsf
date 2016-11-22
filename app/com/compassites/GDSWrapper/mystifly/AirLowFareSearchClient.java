package com.compassites.GDSWrapper.mystifly;

import com.compassites.model.SearchJourney;
import com.compassites.model.SearchParameters;
import onepoint.mystifly.AirLowFareSearchDocument;
import onepoint.mystifly.AirLowFareSearchDocument.AirLowFareSearch;
import onepoint.mystifly.AirLowFareSearchResponseDocument;
import onepoint.mystifly.OnePointStub;
import org.datacontract.schemas._2004._07.mystifly_onepoint.*;
import org.datacontract.schemas._2004._07.mystifly_onepoint.AirTripType.Enum;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import utils.XMLFileUtility;

import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.Date;

/**
 * @author Santhosh
 */
public class AirLowFareSearchClient {

    static Logger mystiflyLogger = LoggerFactory.getLogger("mystifly");

	public AirLowFareSearchRS search(SearchParameters searchParams)
			throws RemoteException {
		SessionsHandler sessionsHandler = new SessionsHandler();
		SessionCreateRS sessionRS = sessionsHandler.login();
		OnePointStub onePointStub = sessionsHandler.getOnePointStub();

		AirLowFareSearchDocument searchRQDoc = AirLowFareSearchDocument.Factory
				.newInstance();
		AirLowFareSearch lowFareSearch = searchRQDoc.addNewAirLowFareSearch();
		AirLowFareSearchRQ searchRQ = lowFareSearch.addNewRq();

		searchRQ.setSessionId(sessionRS.getSessionId());
		searchRQ.setTarget(Mystifly.TARGET);
		searchRQ.setRequestOptions(RequestOptions.HUNDRED);
		searchRQ.setIsRefundable(searchParams.getRefundableFlights());
		searchRQ.setPricingSourceType(PricingSourceType.ALL);
		//searchRQ.setRequestOptions();

		setJourneys(searchRQ, searchParams);
		ArrayOfPassengerTypeQuantity passengers = searchRQ
				.addNewPassengerTypeQuantities();
		setPassengerTypeQuantities(passengers, searchParams.getAdultCount(),
				searchParams.getChildCount(), searchParams.getInfantCount());
		setPreferences(searchRQ, searchParams);

		// TODO: search params to be added
		// stopOver, currency, preferredFood

		XMLFileUtility.createFile(searchRQ.xmlText(), "MystiflySearchRQ.xml");
        mystiflyLogger.debug("MystiflySearchRQ "+ new Date() +" ----->>" + searchRQ.xmlText());
		AirLowFareSearchResponseDocument searchResDoc = onePointStub
				.airLowFareSearch(searchRQDoc);
		AirLowFareSearchRS searchRS = searchResDoc
				.getAirLowFareSearchResponse().getAirLowFareSearchResult();
//		XMLFileUtility.createFile(searchRS.xmlText(), "MystiflySearchRS.xml");
        mystiflyLogger.debug("MystiflySearchRS "+ new Date() +" ----->>" + searchRS.xmlText());
		return searchRS;
	}

	private void setJourneys(AirLowFareSearchRQ searchRQ,
			SearchParameters searchParams) {
		ArrayOfOriginDestinationInformation originDestinationInformations = searchRQ
				.addNewOriginDestinationInformations();
		Calendar calendar = Calendar.getInstance();
		if (searchParams.getTransit() == null) {
			for (SearchJourney searchJourney : searchParams.getJourneyList()) {
				OriginDestinationInformation journey = originDestinationInformations
						.addNewOriginDestinationInformation();
				journey.setOriginLocationCode(searchJourney.getOrigin());

//                journey.setOriginLocationCode("NYC");
//                journey.setDestinationLocationCode("BLR");

				journey.setDestinationLocationCode(searchJourney
						.getDestination());
				DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
				DateTime dateTime = fmt.parseDateTime(searchJourney.getTravelDateStr());
				calendar.setTime(searchJourney.getTravelDate());
				calendar.setTime(dateTime.toDate());
				calendar.clear(Calendar.ZONE_OFFSET);
				calendar.set(Calendar.HOUR_OF_DAY, 0);
				calendar.set(Calendar.MINUTE, 0);
				calendar.set(Calendar.SECOND, 0);
				journey.setDepartureDateTime(calendar);
			}
		} else {
			for (SearchJourney searchJourney : searchParams.getJourneyList()) {
				OriginDestinationInformation firstLeg = originDestinationInformations
						.addNewOriginDestinationInformation();
				DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
				DateTime dateTime = fmt.parseDateTime(searchJourney.getTravelDateStr());
				calendar.setTime(dateTime.toDate());
				calendar.clear(Calendar.ZONE_OFFSET);
				calendar.set(Calendar.HOUR_OF_DAY, 0);
				calendar.set(Calendar.MINUTE, 0);
				calendar.set(Calendar.SECOND, 0);
				firstLeg.setDepartureDateTime(calendar);
				firstLeg.setOriginLocationCode(searchJourney.getOrigin());
				firstLeg.setDestinationLocationCode(searchParams.getTransit());

				OriginDestinationInformation secondLeg = originDestinationInformations
						.addNewOriginDestinationInformation();
				calendar.clear(Calendar.ZONE_OFFSET);
				calendar.set(Calendar.HOUR_OF_DAY, 0);
				calendar.set(Calendar.MINUTE, 0);
				calendar.set(Calendar.SECOND, 0);
				secondLeg.setDepartureDateTime(calendar);
				secondLeg.setOriginLocationCode(searchParams.getTransit());
				secondLeg.setDestinationLocationCode(searchJourney
						.getDestination());
			}
		}
	}

	private void setPreferences(AirLowFareSearchRQ searchRQ,
			SearchParameters searchParams) {
		TravelPreferences prefs = searchRQ.addNewTravelPreferences();
		prefs.setMaxStopsQuantity(MaxStopsQuantity.ALL);
		if (searchParams.getDirectFlights())
			prefs.setMaxStopsQuantity(MaxStopsQuantity.DIRECT);
		if (StringUtils.hasText(searchParams.getPreferredAirlines()))
			prefs.addNewVendorPreferenceCodes().addString(
					searchParams.getPreferredAirlines());
		prefs.setCabinPreference(Mystifly.CABIN_TYPE.get(searchParams
				.getCabinClass()));

		Enum journeyType = searchParams.getTransit() == null ? Mystifly.JOURNEY_TYPE
				.get(searchParams.getJourneyType()) : AirTripType.OPEN_JAW;
		prefs.setAirTripType(journeyType);
	}

	private void setPassengerTypeQuantities(
			ArrayOfPassengerTypeQuantity passengerTypeQuantities,
			int adultCount, int childCount, int infantCount) {
		PassengerTypeQuantity adults = passengerTypeQuantities
				.addNewPassengerTypeQuantity();
		adults.setCode(PassengerType.ADT);
		adults.setQuantity(adultCount);
		if (childCount != 0) {
			PassengerTypeQuantity kids = passengerTypeQuantities
					.addNewPassengerTypeQuantity();
			kids.setCode(PassengerType.CHD);
			kids.setQuantity(childCount);
		}
		if (infantCount != 0) {
			PassengerTypeQuantity infants = passengerTypeQuantities
					.addNewPassengerTypeQuantity();
			infants.setCode(PassengerType.INF);
			infants.setQuantity(infantCount);
		}
	}

}
