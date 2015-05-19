package services;

import com.compassites.GDSWrapper.travelport.AirCancelClient;
import com.compassites.GDSWrapper.travelport.UniversalRecordClient;
import com.compassites.exceptions.BaseCompassitesException;
import com.compassites.model.CancelPNRResponse;
import com.compassites.model.ErrorMessage;
import com.compassites.model.PROVIDERS;
import com.travelport.schema.universal_v26_0.AirCancelRsp;
import com.travelport.schema.universal_v26_0.UniversalRecordRetrieveRsp;
import org.springframework.stereotype.Service;
import utils.ErrorMessageHelper;

/**
 * Created by Yaseen on 08-05-2015.
 */
@Service
public class TravelportCancelServiceImpl implements CancelService {

    @Override
    public CancelPNRResponse cancelPNR(String pnr) {
        CancelPNRResponse cancelPNRResponse = new CancelPNRResponse();
        UniversalRecordRetrieveRsp universalRecordRetrieveRsp = UniversalRecordClient.retrievePNR(pnr);
        String airReservationLocatorCode = universalRecordRetrieveRsp.getUniversalRecord().getAirReservation().get(0).getLocatorCode();
        AirCancelClient airCancelClient = new AirCancelClient();

        AirCancelRsp airCancelRsp = null;
        try {
            airCancelRsp = airCancelClient.cancelPNR(airReservationLocatorCode);

            cancelPNRResponse.setSuccess(true);
        } catch (BaseCompassitesException e) {
            e.printStackTrace();
            ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage(e.getErrorCode(), ErrorMessage.ErrorType.ERROR, PROVIDERS.TRAVELPORT.toString());
            cancelPNRResponse.setSuccess(false);
            cancelPNRResponse.setErrorMessage(errorMessage);
        }
        /*if(airCancelRsp == null){
            cancelPNRResponse.setSuccess(false);
            ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage("error", ErrorMessage.ErrorType.ERROR, "Travelport");
            cancelPNRResponse.setErrorMessage(errorMessage);
            return cancelPNRResponse;
        }*/

        return cancelPNRResponse;

    }
}
