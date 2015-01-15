package services;

import com.compassites.GDSWrapper.mystifly.Mystifly;
import com.compassites.model.FlightItinerary;
import com.compassites.model.IssuanceRequest;
import com.compassites.model.IssuanceResponse;
import com.compassites.model.PNRResponse;
import com.compassites.model.traveller.TravellerMasterInfo;
import com.travelport.schema.universal_v26_0.UniversalRecordRetrieveRsp;

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

	public IssuanceResponse getPnrDetails(IssuanceRequest issuanceRequest, String gdsPNR, String provider){
    	IssuanceResponse issuanceResponse = null;
    	if(provider.equalsIgnoreCase("Travelport")){
    		issuanceResponse = travelPortBookingService.allPNRDetails(issuanceRequest, gdsPNR);
    	}else if(provider.equalsIgnoreCase("Amadeus")){
    		issuanceResponse = amadeusBookingService.allPNRDetails(issuanceRequest,gdsPNR);
    	}else if(provider.equalsIgnoreCase(Mystifly.PROVIDER)){
    		issuanceResponse = mystiflyBookingService.allPNRDetails(gdsPNR);
    	}
    	return issuanceResponse;
    }
}
