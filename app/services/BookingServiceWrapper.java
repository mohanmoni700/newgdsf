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
    private AmadeusBookinServiceImpl amadeusBookinService;

    public AmadeusBookinServiceImpl getAmadeusBookinService() {
        return amadeusBookinService;
    }

    public void setAmadeusBookinService(AmadeusBookinServiceImpl amadeusBookinService) {
        this.amadeusBookinService = amadeusBookinService;
    }

    public PNRResponse generatePNR(TravellerMasterInfo travellerMasterInfo) {

        PNRResponse pnrResponse = amadeusBookinService.generatePNR(travellerMasterInfo);

        return pnrResponse;
    }



}
