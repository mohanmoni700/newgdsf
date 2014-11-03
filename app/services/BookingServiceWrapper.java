package services;

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
        String provider = travellerMasterInfo.getItinerary().getProvider();
        PNRResponse pnrResponse = null;
        if("Travelport".equalsIgnoreCase(provider)) {
            pnrResponse = travelPortBookingService.generatePNR(travellerMasterInfo);
        } else if ("Amadeus".equalsIgnoreCase(provider)) {
            pnrResponse = amadeusBookingService.generatePNR(travellerMasterInfo);
        } else {
        	pnrResponse = mystiflyBookingService.generatePNR(travellerMasterInfo);
        }
        return pnrResponse;
    }

    public PNRResponse priceChangePNR(TravellerMasterInfo travellerMasterInfo) {
        String provider = travellerMasterInfo.getItinerary().getProvider();
        PNRResponse pnrResponse = null;
        if("Travelport".equalsIgnoreCase(provider)) {
            pnrResponse = travelPortBookingService.priceChangePNR(travellerMasterInfo);
        } else if ("Amadeus".equalsIgnoreCase(provider)) {
        	pnrResponse = amadeusBookingService.priceChangePNR(travellerMasterInfo);
		} else {
			pnrResponse = mystiflyBookingService.priceChangePNR(travellerMasterInfo);
        }
        return pnrResponse;
    }


    public PNRResponse issueTicket(String gdsPNR){
      PNRResponse pnrResponse = amadeusBookingService.issueTicket(gdsPNR);
      return pnrResponse;
    }
}
