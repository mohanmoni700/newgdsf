package services;

import models.MiniRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.compassites.GDSWrapper.mystifly.Mystifly;
import com.compassites.model.FlightItinerary;
import com.compassites.model.SearchParameters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author Santhosh
 */

@Service
public class FlightInfoServiceWrapper {

	@Autowired
	private AmadeusFlightInfoServiceImpl amadeusFlightInfoService;

	@Autowired
	private MystiflyFlightInfoServiceImpl mystiflyFlightInfoService;
	
	@Autowired
	private TravelportFlightInfoServiceImpl travelportFlightInfoServiceImpl;

	public FlightItinerary getBaggageInfo(FlightItinerary flightItinerary,
			SearchParameters searchParams, String provider, boolean seamen) {
		FlightItinerary response = null;
		if ("Travelport".equalsIgnoreCase(provider)) {
			response = flightItinerary;
			// Baggage info is available in search response
		} else if ("Amadeus".equalsIgnoreCase(provider)) {
			response = amadeusFlightInfoService.getBaggageInfo(
					flightItinerary, searchParams, seamen);
		} else if (Mystifly.PROVIDER.equalsIgnoreCase(provider)) {
			response = mystiflyFlightInfoService.getBaggageInfo(
					flightItinerary, searchParams, seamen);
		}
		return response;
	}
	
	public FlightItinerary getInFlightDetails(FlightItinerary flightItinerary, String provider, boolean seamen) {
		FlightItinerary response = flightItinerary;
		if ("Travelport".equalsIgnoreCase(provider)) {
			response = travelportFlightInfoServiceImpl.getInFlightDetails(flightItinerary, seamen);
		} else if ("Amadeus".equalsIgnoreCase(provider)) { 
			response = amadeusFlightInfoService.getInFlightDetails(flightItinerary, seamen);
		} else if (Mystifly.PROVIDER.equalsIgnoreCase(provider)) {
			// No Flight Amenities
		}
		return response;
	}
	
	public String getCancellationFee(FlightItinerary flightItinerary,
			SearchParameters searchParams, String provider, boolean seamen) {
		String fareRules = "";
		if ("Travelport".equalsIgnoreCase(provider)) {
			// Cancellation fee is available in search response
		} else if ("Amadeus".equalsIgnoreCase(provider)) {
			fareRules = amadeusFlightInfoService.getCancellationFee(
					flightItinerary, searchParams, seamen);
		} else if (Mystifly.PROVIDER.equalsIgnoreCase(provider)) {
			fareRules = mystiflyFlightInfoService.getMystiflyFareRules(flightItinerary, searchParams, seamen);
		}
		return fareRules;
	}

	public MiniRule getMiniRuleFeeFromFlightItenary(FlightItinerary flightItinerary,
                                        SearchParameters searchParams, String provider, boolean seamen) {
		MiniRule miniRule = new MiniRule();
		if ("Travelport".equalsIgnoreCase(provider)) {
			// MiniRule not avaliable
		} else if ("Amadeus".equalsIgnoreCase(provider)) {
			miniRule = amadeusFlightInfoService.getMiniRules(
					flightItinerary, searchParams, seamen,miniRule);
		} else if (Mystifly.PROVIDER.equalsIgnoreCase(provider)) {
			// MiniRule not avaliable
		}
		return miniRule;
	}
}
