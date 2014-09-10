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
    private TravelPortBookingServiceImpl travelPortBookingService;


    public AmadeusBookingServiceImpl getAmadeusBookingService() {
        return amadeusBookingService;
    }

    public void setAmadeusBookingService(AmadeusBookingServiceImpl amadeusBookingService) {
        this.amadeusBookingService = amadeusBookingService;
    }

    public PNRResponse generatePNR(TravellerMasterInfo travellerMasterInfo) {
        String provider = travellerMasterInfo.getItinerary().getProvider();
        PNRResponse pnrResponse = null;
        if("Travelport".equalsIgnoreCase(provider)){
            pnrResponse  = travelPortBookingService.generatePNR(travellerMasterInfo);
        }else{
            pnrResponse  = amadeusBookingService.generatePNR(travellerMasterInfo);
        }

        return pnrResponse;
    }



}
