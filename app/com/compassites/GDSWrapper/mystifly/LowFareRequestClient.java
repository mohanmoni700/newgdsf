package com.compassites.GDSWrapper.mystifly;

import com.compassites.model.Passenger;
import com.compassites.model.PassengerTypeCode;
import com.compassites.model.SearchParameters;
import onepoint.mystifly.AirLowFareSearchDocument;
import onepoint.mystifly.AirLowFareSearchDocument.AirLowFareSearch;
import onepoint.mystifly.AirLowFareSearchResponseDocument;
import onepoint.mystifly.OnePointStub;
import org.datacontract.schemas._2004._07.mystifly_onepoint.*;
import org.datacontract.schemas._2004._07.mystifly_onepoint.MaxStopsQuantity.Enum;

import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.List;

/**
 * 
 * @author Santhosh
 */
public class LowFareRequestClient {

	public AirLowFareSearchRS search(SearchParameters searchParams) {
		SessionsHandler sessionsHandler = new SessionsHandler();
		SessionCreateRS sessionRS = sessionsHandler.login();
		OnePointStub onePointStub = sessionsHandler.getOnePointStub();

		AirLowFareSearchDocument airLowFareSearchDocument = AirLowFareSearchDocument.Factory
				.newInstance();
		AirLowFareSearch airLowFareSearch = airLowFareSearchDocument
				.addNewAirLowFareSearch();
		AirLowFareSearchRQ airLowFareSearchRQ = airLowFareSearch.addNewRq();

		airLowFareSearchRQ.setSessionId(sessionRS.getSessionId());
		airLowFareSearchRQ.setIsRefundable(searchParams.getRefundableFlights());

		ArrayOfOriginDestinationInformation originDestinationInformations = airLowFareSearchRQ
				.addNewOriginDestinationInformations();
		OriginDestinationInformation onwardTrip = originDestinationInformations
				.addNewOriginDestinationInformation();
		Calendar calendar = Calendar.getInstance();
		/*calendar.setTime(searchParams.getFromDate());
		onwardTrip.setDepartureDateTime(calendar);
		onwardTrip.setOriginLocationCode(searchParams.getOrigin());
		onwardTrip.setDestinationLocationCode(searchParams.getDestination());

		if (searchParams.getJourneyType().equals(JourneyType.ROUND_TRIP)) {
			OriginDestinationInformation returnTrip = originDestinationInformations
					.addNewOriginDestinationInformation();
			calendar.setTime(searchParams.getReturnDate());
			returnTrip.setDepartureDateTime(calendar);
			returnTrip.setOriginLocationCode(searchParams.getDestination());
			returnTrip.setDestinationLocationCode(searchParams.getOrigin());
		}*/

		// Set passenger info
		ArrayOfPassengerTypeQuantity passengers = airLowFareSearchRQ
				.addNewPassengerTypeQuantities();
		setPassengerTypeQuantities(passengers, searchParams.getAdultCount(),
				searchParams.getChildCount(), searchParams.getInfantCount());
		// setPassengerTypeQuantities(passengers, searchParams.getPassengers());

		// Set Travel preferences
		TravelPreferences prefs = airLowFareSearchRQ.addNewTravelPreferences();

		prefs.setCabinPreference(CabinType.Enum.forString(Constants.CABIN_TYPE
				.get(searchParams.getCabinClass())));
		prefs.setAirTripType(AirTripType.Enum.forString(Constants.JOURNEY_TYPE
				.get(searchParams.getJourneyType())));

		// TODO: Not working. fix
		// ArrayOfstring preferredAirlines = preferences
		// .addNewVendorPreferenceCodes();
		// preferredAirlines.addString(searchParams.getPreferredAirlines());
		// preferences.setVendorPreferenceCodes(preferredAirlines);

		prefs.setMaxStopsQuantity(Enum.forString(Constants.ALL));
		if (searchParams.getDirectFlights() || searchParams.getNoOfStops() == 0) {
			prefs.setMaxStopsQuantity(Enum.forString(Constants.DIRECT));
		} else if (searchParams.getNoOfStops() == 1) {
			prefs.setMaxStopsQuantity(Enum.forString(Constants.ONE_STOP));
		}

		// TODO: search params to be added
		// stopOver, currency, preferredFood

		AirLowFareSearchResponseDocument searchResDoc = null;
		try {
			searchResDoc = onePointStub
					.airLowFareSearch(airLowFareSearchDocument);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		AirLowFareSearchRS searchRS = searchResDoc
				.getAirLowFareSearchResponse().getAirLowFareSearchResult();
		return searchRS;
	}

	private void setPassengerTypeQuantities(
			ArrayOfPassengerTypeQuantity passengerTypeQuantities,
			List<Passenger> passengers) {
		for (Passenger passenger : passengers) {
			PassengerTypeQuantity passengerTypeQuantity = passengerTypeQuantities
					.addNewPassengerTypeQuantity();
			passengerTypeQuantity.setCode(PassengerType.Enum
					.forString(passenger.getPassengerType().name()));
			passengerTypeQuantity.setQuantity(1);
		}
	}

	private void setPassengerTypeQuantities(
			ArrayOfPassengerTypeQuantity passengerTypeQuantities,
			int adultCount, int childCount, int infantCount) {
		PassengerTypeQuantity adults = passengerTypeQuantities
				.addNewPassengerTypeQuantity();
		String passengerCode = PassengerTypeCode.ADT.name();
		adults.setCode(PassengerType.Enum.forString(passengerCode));
		adults.setQuantity(adultCount);

		if (childCount != 0) {
			PassengerTypeQuantity kids = passengerTypeQuantities
					.addNewPassengerTypeQuantity();
			passengerCode = PassengerTypeCode.CHD.name();
			kids.setCode(PassengerType.Enum.forString(passengerCode));
			kids.setQuantity(childCount);
		}

		if (infantCount != 0) {
			PassengerTypeQuantity infants = passengerTypeQuantities
					.addNewPassengerTypeQuantity();
			passengerCode = PassengerTypeCode.INF.name();
			infants.setCode(PassengerType.Enum.forString(passengerCode));
			infants.setQuantity(infantCount);
		}
	}

}
