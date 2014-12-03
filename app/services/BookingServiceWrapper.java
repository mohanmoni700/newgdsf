package services;

import com.compassites.GDSWrapper.mystifly.Mystifly;
import com.compassites.model.FlightItinerary;
import com.compassites.model.PNRResponse;
import com.compassites.model.traveller.TravellerMasterInfo;
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

    public void setAmadeusBookingService(AmadeusBookingServiceImpl amadeusBookingService) {
        this.amadeusBookingService = amadeusBookingService;
    }

    public PNRResponse generatePNR(TravellerMasterInfo travellerMasterInfo) {
    	String provider = getProvider(travellerMasterInfo);
        PNRResponse pnrResponse = null;
        if("Travelport".equalsIgnoreCase(provider)) {
            pnrResponse = travelPortBookingService.generatePNR(travellerMasterInfo);
        } else if ("Amadeus".equalsIgnoreCase(provider)) {
            pnrResponse = amadeusBookingService.generatePNR(travellerMasterInfo);
        } else if (Mystifly.PROVIDER.equalsIgnoreCase(provider)) {
        	pnrResponse = mystiflyBookingService.generatePNR(travellerMasterInfo);
        }
        return pnrResponse;
    }

    public PNRResponse priceChangePNR(TravellerMasterInfo travellerMasterInfo) {
    	String provider = getProvider(travellerMasterInfo);
        PNRResponse pnrResponse = null;
        if("Travelport".equalsIgnoreCase(provider)) {
            pnrResponse = travelPortBookingService.priceChangePNR(travellerMasterInfo);
        } else if ("Amadeus".equalsIgnoreCase(provider)) {
        	pnrResponse = amadeusBookingService.priceChangePNR(travellerMasterInfo);
		} else if (Mystifly.PROVIDER.equalsIgnoreCase(provider)) {
			pnrResponse = mystiflyBookingService.priceChangePNR(travellerMasterInfo);
        }
        return pnrResponse;
    }

    public PNRResponse issueTicket(String gdsPNR){
      PNRResponse pnrResponse = amadeusBookingService.issueTicket(gdsPNR);
      return pnrResponse;
    }
    
	private String getProvider(TravellerMasterInfo travellerMasterInfo) {
		FlightItinerary itinerary = travellerMasterInfo.getItinerary();
		return travellerMasterInfo.isSeamen() ? itinerary
				.getSeamanPricingInformation().getProvider() : itinerary
				.getPricingInformation().getProvider();
	}

}
