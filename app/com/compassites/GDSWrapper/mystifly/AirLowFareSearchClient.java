package com.compassites.GDSWrapper.mystifly;

import java.rmi.RemoteException;
import java.util.Calendar;

import onepoint.mystifly.AirLowFareSearchDocument;
import onepoint.mystifly.AirLowFareSearchDocument.AirLowFareSearch;
import onepoint.mystifly.AirLowFareSearchResponseDocument;
import onepoint.mystifly.OnePointStub;

import org.datacontract.schemas._2004._07.mystifly_onepoint.AirLowFareSearchRQ;
import org.datacontract.schemas._2004._07.mystifly_onepoint.AirLowFareSearchRS;
import org.datacontract.schemas._2004._07.mystifly_onepoint.AirTripType.Enum;
import org.datacontract.schemas._2004._07.mystifly_onepoint.AirTripType;
import org.datacontract.schemas._2004._07.mystifly_onepoint.ArrayOfOriginDestinationInformation;
import org.datacontract.schemas._2004._07.mystifly_onepoint.ArrayOfPassengerTypeQuantity;
import org.datacontract.schemas._2004._07.mystifly_onepoint.MaxStopsQuantity;
import org.datacontract.schemas._2004._07.mystifly_onepoint.OriginDestinationInformation;
import org.datacontract.schemas._2004._07.mystifly_onepoint.PassengerType;
import org.datacontract.schemas._2004._07.mystifly_onepoint.PassengerTypeQuantity;
import org.datacontract.schemas._2004._07.mystifly_onepoint.RequestOptions;
import org.datacontract.schemas._2004._07.mystifly_onepoint.SessionCreateRS;
import org.datacontract.schemas._2004._07.mystifly_onepoint.TravelPreferences;

import com.compassites.model.SearchJourney;
import com.compassites.model.SearchParameters;

/**
 * @author Santhosh
 */
public class AirLowFareSearchClient {

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
		searchRQ.setRequestOptions(RequestOptions.FIFTY);
		searchRQ.setIsRefundable(searchParams.getRefundableFlights());

		setJourneys(searchRQ, searchParams);
		ArrayOfPassengerTypeQuantity passengers = searchRQ
				.addNewPassengerTypeQuantities();
		setPassengerTypeQuantities(passengers, searchParams.getAdultCount(),
				searchParams.getChildCount(), searchParams.getInfantCount());
		setPreferences(searchRQ, searchParams);

		// TODO: search params to be added
		// stopOver, currency, preferredFood

		AirLowFareSearchResponseDocument searchResDoc = onePointStub
				.airLowFareSearch(searchRQDoc);
		AirLowFareSearchRS searchRS = searchResDoc
				.getAirLowFareSearchResponse().getAirLowFareSearchResult();
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
				journey.setDestinationLocationCode(searchJourney
						.getDestination());
				calendar.setTime(searchJourney.getTravelDate());
				journey.setDepartureDateTime(calendar);
			}
		} else {
			for (SearchJourney searchJourney : searchParams.getJourneyList()) {
				OriginDestinationInformation firstLeg = originDestinationInformations
						.addNewOriginDestinationInformation();
				calendar.setTime(searchJourney.getTravelDate());
				firstLeg.setDepartureDateTime(calendar);
				firstLeg.setOriginLocationCode(searchJourney.getOrigin());
				firstLeg.setDestinationLocationCode(searchParams.getTransit());

				OriginDestinationInformation secondLeg = originDestinationInformations
						.addNewOriginDestinationInformation();
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
		if (searchParams.getPreferredAirlines() != null)
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
