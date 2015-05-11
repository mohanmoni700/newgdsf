package services;

import com.amadeus.xml.pnracc_10_1_1a.PNRReply;
import com.compassites.GDSWrapper.mystifly.Mystifly;
import com.compassites.model.*;
import com.compassites.model.traveller.TravellerMasterInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by user on 07-08-2014.
 */
@Service
public class BookingServiceWrapper {

	@Autowired
	private AmadeusBookingServiceImpl amadeusBookingService;

	@Autowired
	private TravelportBookingServiceImpl travelPortBookingService;

	@Autowired
	private MystiflyBookingServiceImpl mystiflyBookingService;

	public AmadeusBookingServiceImpl getAmadeusBookingService() {
		return amadeusBookingService;
	}

	public void setAmadeusBookingService(
			AmadeusBookingServiceImpl amadeusBookingService) {
		this.amadeusBookingService = amadeusBookingService;
	}

	public PNRResponse generatePNR(TravellerMasterInfo travellerMasterInfo) {
		String provider = getProvider(travellerMasterInfo);
		PNRResponse pnrResponse = null;
		if ("Travelport".equalsIgnoreCase(provider)) {
			pnrResponse = travelPortBookingService
					.generatePNR(travellerMasterInfo);
		} else if ("Amadeus".equalsIgnoreCase(provider)) {
			pnrResponse = amadeusBookingService
					.generatePNR(travellerMasterInfo);
		} else if (Mystifly.PROVIDER.equalsIgnoreCase(provider)) {
			pnrResponse = mystiflyBookingService
					.generatePNR(travellerMasterInfo);
		}
		return pnrResponse;
	}

	public PNRResponse priceChangePNR(TravellerMasterInfo travellerMasterInfo) {
		String provider = getProvider(travellerMasterInfo);
		PNRResponse pnrResponse = null;
		if ("Travelport".equalsIgnoreCase(provider)) {
			pnrResponse = travelPortBookingService
					.priceChangePNR(travellerMasterInfo);
		} else if ("Amadeus".equalsIgnoreCase(provider)) {
			pnrResponse = amadeusBookingService
					.priceChangePNR(travellerMasterInfo);
		} else if (Mystifly.PROVIDER.equalsIgnoreCase(provider)) {
			pnrResponse = mystiflyBookingService
					.priceChangePNR(travellerMasterInfo);
		}
		return pnrResponse;
	}

	public IssuanceResponse issueTicket(IssuanceRequest issuanceRequest) {
		IssuanceResponse issuanceResponse = null;
		if ("Travelport".equalsIgnoreCase(issuanceRequest.getProvider())) {
			issuanceResponse = travelPortBookingService
					.issueTicket(issuanceRequest);
		} else if ("Amadeus".equalsIgnoreCase(issuanceRequest.getProvider())) {
			issuanceResponse = amadeusBookingService
					.issueTicket(issuanceRequest);
		} else if (Mystifly.PROVIDER.equalsIgnoreCase(issuanceRequest
				.getProvider())) {
			issuanceResponse = mystiflyBookingService
					.issueTicket(issuanceRequest);
		}
		return issuanceResponse;
	}

	private String getProvider(TravellerMasterInfo travellerMasterInfo) {
		FlightItinerary itinerary = travellerMasterInfo.getItinerary();
		return travellerMasterInfo.isSeamen() ? itinerary
				.getSeamanPricingInformation().getProvider() : itinerary
				.getPricingInformation().getProvider();
	}

	public PNRResponse checkFareChangeAndAvailability(
			TravellerMasterInfo travellerMasterInfo) {
		String provider = getProvider(travellerMasterInfo);
		PNRResponse pnrResponse = null;
		if ("Travelport".equalsIgnoreCase(provider)) {
			pnrResponse = travelPortBookingService
					.checkFareChangeAndAvailability(travellerMasterInfo);
		} else if ("Amadeus".equalsIgnoreCase(provider)) {
			pnrResponse = amadeusBookingService
					.checkFareChangeAndAvailability(travellerMasterInfo);
		} else if (Mystifly.PROVIDER.equalsIgnoreCase(provider)) {
			pnrResponse = mystiflyBookingService
					.checkFareChangeAndAvailability(travellerMasterInfo);
		}
		return pnrResponse;
	}

	public TravellerMasterInfo getPnrDetails(IssuanceRequest issuanceRequest, String gdsPNR, String provider){
    	TravellerMasterInfo masterInfo = null;
    	if("Travelport".equalsIgnoreCase(provider)){
    		masterInfo = travelPortBookingService.allPNRDetails(issuanceRequest, gdsPNR);
    	} else if("Amadeus".equalsIgnoreCase(provider)){
    		masterInfo = amadeusBookingService.allPNRDetails(issuanceRequest,gdsPNR);
    	} else if(Mystifly.PROVIDER.equalsIgnoreCase(provider)){
    		masterInfo = mystiflyBookingService.allPNRDetails(gdsPNR);
    	}
    	return masterInfo;
    }
	
	public ObjectNode getBookingDetails(String provider, String gdsPNR) {
		ObjectNode json = null;
		if("Travelport".equalsIgnoreCase(provider)){
			json = travelPortBookingService.getBookingDetails(gdsPNR);
    	} else if("Amadeus".equalsIgnoreCase(provider)){
    		json = amadeusBookingService.getBookingDetails(gdsPNR);
    	}
		return json;
	}
	
	public LowFareResponse getLowestFare(String pnr, String provider, boolean isSeamen) {
		LowFareResponse lowFareRS = null;
		if("Amadeus".equalsIgnoreCase(provider)) {
			lowFareRS = amadeusBookingService.getLowestFare(pnr, isSeamen);
    	} else if("Travelport".equalsIgnoreCase(provider)) {
            lowFareRS = travelPortBookingService.getLowestFare(pnr, provider, isSeamen);
    	} else if(Mystifly.PROVIDER.equalsIgnoreCase(provider)) {
    		// Not implemented.
    	}
		return lowFareRS;
	}
	
}
