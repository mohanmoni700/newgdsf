package services;

import com.compassites.GDSWrapper.mystifly.Mystifly;
import com.compassites.constants.TraveloMatrixConstants;
import com.compassites.model.*;
import com.compassites.model.traveller.TravellerMasterInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import utils.PNRRequest;

import java.io.IOException;
import java.util.HashMap;

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

	@Autowired
	private AmadeusIssuanceServiceImpl amadeusIssuanceService;

	@Autowired
	private TravelportIssuanceServiceImpl travelportIssuanceService;

	@Autowired
	private TraveloMatrixBookingServiceImpl traveloMatrixBookingService;


	private LowestFareService amadeusLowestFareService;

	public AmadeusBookingServiceImpl getAmadeusBookingService() {
		return amadeusBookingService;
	}


	public void setAmadeusBookingService(
			AmadeusBookingServiceImpl amadeusBookingService) {
		this.amadeusBookingService = amadeusBookingService;
	}

	public LowestFareService getAmadeusLowestFareService() {
		return amadeusLowestFareService;
	}

    @Autowired
	public void setAmadeusLowestFareService(LowestFareService amadeusLowestFareService) {
		this.amadeusLowestFareService = amadeusLowestFareService;
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
			pnrResponse = mystiflyBookingService.generatePNR(travellerMasterInfo);
			if(pnrResponse.getBookedStatus() != null && pnrResponse.getBookedStatus().equals("PENDING")){
				pnrResponse.setFlightAvailable(false);
				CancelServiceWrapper cancelServiceWrapper = null;
				cancelServiceWrapper.cancelPNR(pnrResponse.getPnrNumber(),"Mystifly",null,null, false);
			}
		} else if (TraveloMatrixConstants.provider.equalsIgnoreCase(provider)) {
			pnrResponse =	traveloMatrixBookingService.generatePNR(travellerMasterInfo);
		}
		return pnrResponse;
	}

	public PNRResponse createTempPNR(TravellerMasterInfo travellerMasterInfo) {
		String provider = getProvider(travellerMasterInfo);
		PNRResponse pnrResponse = null;
		if ("Amadeus".equalsIgnoreCase(provider)) {
			pnrResponse = amadeusBookingService.createTempPNR(travellerMasterInfo);
		}
		return pnrResponse;
	}

	public SplitPNRResponse splitPNR( IssuanceRequest issuanceRequest){
		SplitPNRResponse splitPNRResponse = null;
		if("Amadeus".equalsIgnoreCase(issuanceRequest.getProvider())) {
			splitPNRResponse = amadeusBookingService.splitPNR(issuanceRequest);
		}
		return splitPNRResponse;
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
		}else if (TraveloMatrixConstants.provider.equalsIgnoreCase(provider)) {
			pnrResponse = traveloMatrixBookingService.priceChangePNR(travellerMasterInfo);
		}
		return pnrResponse;
	}

	public IssuanceResponse issueTicket(IssuanceRequest issuanceRequest) {
		IssuanceResponse issuanceResponse = null;
		if ("Travelport".equalsIgnoreCase(issuanceRequest.getProvider())) {
			issuanceResponse = travelportIssuanceService.issueTicket(issuanceRequest);
		} else if ("Amadeus".equalsIgnoreCase(issuanceRequest.getProvider())) {
			issuanceResponse = amadeusIssuanceService
					.issueTicket(issuanceRequest);
		} else if (Mystifly.PROVIDER.equalsIgnoreCase(issuanceRequest
				.getProvider())) {
			issuanceResponse = mystiflyBookingService
					.issueTicket(issuanceRequest);
		}else if (TraveloMatrixConstants.provider.equalsIgnoreCase(issuanceRequest
				.getProvider())) {
			issuanceResponse = traveloMatrixBookingService.issueTicket(issuanceRequest);
		}
		return issuanceResponse;
	}

	public IssuanceResponse readTripDetails(IssuanceRequest issuanceRequest) {
		IssuanceResponse issuanceResponse = null;
		issuanceResponse = mystiflyBookingService.readTripDetails(issuanceRequest);
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
		}else if (TraveloMatrixConstants.provider.equalsIgnoreCase(provider)){
			pnrResponse =traveloMatrixBookingService.checkFareChangeAndAvailability(travellerMasterInfo);
		}

		return pnrResponse;
	}

	public TravellerMasterInfo getPnrDetails(IssuanceRequest issuanceRequest, String gdsPNR, String provider){
    	TravellerMasterInfo masterInfo = null;
    	if("Travelport".equalsIgnoreCase(provider)){
    		masterInfo = travelPortBookingService.allPNRDetails(issuanceRequest, gdsPNR);
    	} else if("Amadeus".equalsIgnoreCase(provider)){
    		masterInfo = amadeusBookingService.allPNRDetails(issuanceRequest, gdsPNR);
    	} else if(Mystifly.PROVIDER.equalsIgnoreCase(provider)){
    		masterInfo = mystiflyBookingService.allPNRDetails(gdsPNR);
    	}
    	return masterInfo;
    }
	
	public JsonNode getBookingDetails(String provider, String gdsPNR) {
		JsonNode json = null;
		if("Travelport".equalsIgnoreCase(provider) || "Galileo".equalsIgnoreCase(provider)){
			json = travelPortBookingService.getBookingDetails(gdsPNR);
    	} else if("Amadeus".equalsIgnoreCase(provider)){
    		json = amadeusBookingService.getBookingDetails(gdsPNR);
    	}else if ("Mystifly".equalsIgnoreCase(provider)){
			json =  mystiflyBookingService.getBookingDetails(gdsPNR);
		}
		return json;
	}
	public JsonNode getBookingDetailsByOfficeId(String provider, String gdsPNR, String officeId) {
		JsonNode json = null;
		if("Travelport".equalsIgnoreCase(provider) || "Galileo".equalsIgnoreCase(provider)){
			json = travelPortBookingService.getBookingDetails(gdsPNR);
    	} else if("Amadeus".equalsIgnoreCase(provider)){
    		json = amadeusBookingService.getBookingDetailsByOfficeId(gdsPNR, officeId);
    	}else if ("Mystifly".equalsIgnoreCase(provider)){
			json =  mystiflyBookingService.getBookingDetails(gdsPNR);
		}
		return json;
	}
	
	public LowFareResponse getLowestFare(IssuanceRequest issuanceRequest) {
		LowFareResponse lowFareRS = null;
		if("Amadeus".equalsIgnoreCase(issuanceRequest.getProvider())) {
			lowFareRS = amadeusLowestFareService.getLowestFare(issuanceRequest);
    	} else if("Travelport".equalsIgnoreCase(issuanceRequest.getProvider())) {
            lowFareRS = travelPortBookingService.getLowestFare(issuanceRequest.getGdsPNR(), issuanceRequest.getProvider(), issuanceRequest.isSeamen());
    	} else if(Mystifly.PROVIDER.equalsIgnoreCase(issuanceRequest.getProvider())) {
    		// Not implemented.
    	}
		return lowFareRS;
	}

    public HashMap getBookingDetailsForPNR(JsonNode json) {
        PNRRequest[] PNRRequestList = null;
        HashMap<String, Object> jsonMap = new HashMap<>();
        ObjectMapper objectMapper = new ObjectMapper();
        TypeFactory typeFactory = objectMapper.getTypeFactory();
        try {
           PNRRequestList =
                    objectMapper.readValue(json.toString(), PNRRequest[].class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        for(PNRRequest pnrRequest: PNRRequestList){
            if("Travelport".equalsIgnoreCase(pnrRequest.getProvider()) || "Galileo".equalsIgnoreCase(pnrRequest.getProvider())){
                jsonMap.put(pnrRequest.getGdsPnr(), travelPortBookingService.getBookingDetails(pnrRequest.getGdsPnr()));
            } else if("Amadeus".equalsIgnoreCase(pnrRequest.getProvider())){
                jsonMap.put(pnrRequest.getGdsPnr(), amadeusBookingService.getBookingDetails(pnrRequest.getGdsPnr()));
            } else if("Mystifly".equalsIgnoreCase(pnrRequest.getProvider())){
                jsonMap.put(pnrRequest.getGdsPnr(), mystiflyBookingService.getBookingDetails(pnrRequest.getGdsPnr()));
            }
        }
//        System.out.println(" HashMap ==============>>>>>>\n"+Json.toJson(jsonMap));
        return jsonMap;
    }

	public IssuanceResponse priceBookedPNR(IssuanceRequest issuanceRequest){

		IssuanceResponse issuanceResponse = null;
		if(PROVIDERS.TRAVELPORT.toString().equalsIgnoreCase(issuanceRequest.getProvider()) || "Galileo".equalsIgnoreCase(issuanceRequest.getProvider())){
			issuanceResponse = travelportIssuanceService.priceBookedPNR(issuanceRequest);
		}else if(PROVIDERS.AMADEUS.toString().equalsIgnoreCase(issuanceRequest.getProvider())){
			issuanceResponse = amadeusIssuanceService.priceBookedPNR(issuanceRequest);
		}

		return issuanceResponse;
	}

	/*public IssuanceResponse readTripDetails(IssuanceRequest issuanceRequest) {
		IssuanceResponse issuanceResponse = null;
		issuanceResponse = mystiflyBookingService.readTripDetails(issuanceRequest);
		return issuanceResponse;
	}*/
}
