package services;

import com.compassites.model.CancelPNRResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by Yaseen on 08-05-2015.
 */
@Service
public class CancelServiceWrapper {

    /*@Autowired
    private CancelService cancelService;*/

    @Autowired
    private TravelportCancelServiceImpl travelportCancelService;

    @Autowired
    private AmadeusCancelServiceImpl amadeusCancelService;

    public CancelPNRResponse cancelPNR(String pnr, String provider) {
        CancelPNRResponse result = null;
        if ("Amadeus".equalsIgnoreCase(provider)) {
           result =  amadeusCancelService.cancelPNR(pnr);
        }else if("Travelport".equalsIgnoreCase(provider)){
            result =  travelportCancelService.cancelPNR(pnr);
        }

        return result;
    }
}
