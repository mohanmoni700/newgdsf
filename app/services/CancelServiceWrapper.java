package services;

import com.compassites.model.CancelPNRResponse;
import com.compassites.model.PROVIDERS;
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

    @Autowired
    private MystiflyCancelServiceImpl mystiflyCancelService;

    @Autowired
    private TraveloMatrixCancelService traveloMatrixCancelService;

    public CancelPNRResponse cancelPNR(String pnr, String provider,String appRef,String bookingId) {
        CancelPNRResponse result = null;
        if ("Amadeus".equalsIgnoreCase(provider)) {
           result =  amadeusCancelService.cancelPNR(pnr);
        }else if("Travelport".equalsIgnoreCase(provider)){
            result =  travelportCancelService.cancelPNR(pnr);
        }else if(PROVIDERS.MYSTIFLY.toString().equalsIgnoreCase(provider)){
            result = mystiflyCancelService.cancelPNR(pnr);
        }else if(PROVIDERS.TRAVELOMATRIX.toString().equalsIgnoreCase(provider)){
            result = traveloMatrixCancelService.cancelPNR(pnr,appRef,bookingId);
        }

        return result;
    }
}
