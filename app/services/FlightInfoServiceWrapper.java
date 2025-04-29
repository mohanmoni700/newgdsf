package services;

import com.compassites.model.*;
import com.compassites.model.travelomatrix.ResponseModels.TraveloMatrixFaruleReply;
import com.fasterxml.jackson.databind.JsonNode;
import dto.FareCheckRulesResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.compassites.GDSWrapper.mystifly.Mystifly;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

	@Autowired
	private TraveloMatrixFlightInfoService traveloMatrixFlightInfoServiceImpl;


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
		}else if ("TraveloMatrix".equalsIgnoreCase(provider)) {
			response = traveloMatrixFlightInfoServiceImpl.getFlightInfo(flightItinerary);
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

	public List<HashMap> getMiniRuleFeeFromFlightItenary(FlightItinerary flightItinerary,
                                        SearchParameters searchParams, String provider, boolean seamen) {
		List<HashMap> miniRule = new ArrayList<>();
		if ("Travelport".equalsIgnoreCase(provider)) {
			// MiniRule not avaliable
		} else if ("Amadeus".equalsIgnoreCase(provider)) {
			miniRule = amadeusFlightInfoService.getMiniRulesFromFlightItenary(
					flightItinerary, searchParams, seamen);
		} else if (Mystifly.PROVIDER.equalsIgnoreCase(provider)) {
			// MiniRule not avaliable
		}
		return miniRule;
	}


	public Map<String, FareCheckRulesResponse> getGenericFareRuleFlightItinerary(FlightItinerary flightItinerary, SearchParameters searchParameters, boolean seamen, String provider) {

		Map<String, FareCheckRulesResponse> fareCheckRulesResponseMap = null;

		if ("Amadeus".equalsIgnoreCase(provider)) {
			fareCheckRulesResponseMap = amadeusFlightInfoService.getFareCheckRules(flightItinerary, searchParameters, seamen);
		}

		return fareCheckRulesResponseMap;
	}


///  Commenting due to new requirement of having fare check response as complete json
//	public JsonNode getGenericFareRuleFlightItenary(FlightItinerary flightItinerary, SearchParameters searchParameters,
//													boolean seamen, String provider){
//		List<HashMap> miniRule = new ArrayList<>();
//		if ("Amadeus".equalsIgnoreCase(provider)) {
//			miniRule = amadeusFlightInfoService.getGenericFareRuleFlightItenary(flightItinerary,searchParameters,seamen);
//		}
//		return miniRule;
//	}


   /*
      This function Fetches Fare rules based from TraveloMatrix API
    */
	public List<TraveloMatrixFaruleReply> getFareRuleFromTmx(String resultToken, String returnResultToken){
		List<TraveloMatrixFaruleReply> traveloMatrixFaruleReplyList = null;
		 traveloMatrixFaruleReplyList = traveloMatrixFlightInfoServiceImpl.flightFareRules(resultToken,returnResultToken);
		return traveloMatrixFaruleReplyList;
	}

}
