package services;

import com.compassites.GDSWrapper.travelport.AirCancelClient;
import com.compassites.GDSWrapper.travelport.UniversalRecordClient;
import com.compassites.exceptions.BaseCompassitesException;
import com.compassites.model.CancelPNRResponse;
import com.compassites.model.ErrorMessage;
import com.compassites.model.PROVIDERS;
import com.travelport.schema.air_v26_0.AirReservation;
import com.travelport.schema.air_v26_0.TicketInfo;
import com.travelport.schema.universal_v26_0.AirCancelRsp;
import com.travelport.schema.universal_v26_0.UniversalRecordRetrieveRsp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import utils.ErrorMessageHelper;

/**
 * Created by Yaseen on 08-05-2015.
 */
@Service
public class TravelportCancelServiceImpl implements CancelService {

    static Logger logger = LoggerFactory.getLogger("gds");

    @Override
    public CancelPNRResponse cancelPNR(String pnr,Boolean isFullPNR) {
        logger.debug("Travelport cancelPNR called ..... ");
        CancelPNRResponse cancelPNRResponse = new CancelPNRResponse();
        UniversalRecordRetrieveRsp universalRecordRetrieveRsp = UniversalRecordClient.retrievePNR(pnr);
        //check ticket is issued or not
        for(AirReservation airReservation : universalRecordRetrieveRsp.getUniversalRecord().getAirReservation()){
            for(TicketInfo ticketInfo : airReservation.getDocumentInfo().getTicketInfo()){
                if(StringUtils.hasText(ticketInfo.getNumber())){
                    logger.debug("Tickets are already issued cannot cancel the pnr: " + pnr);
                    cancelPNRResponse.setSuccess(false);
                    ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage("ticketIssuedError", ErrorMessage.ErrorType.ERROR, PROVIDERS.AMADEUS.toString());
                    cancelPNRResponse.setErrorMessage(errorMessage);
                    return cancelPNRResponse;
                }
            }
        }
        String airReservationLocatorCode = universalRecordRetrieveRsp.getUniversalRecord().getAirReservation().get(0).getLocatorCode();
        AirCancelClient airCancelClient = new AirCancelClient();

        AirCancelRsp airCancelRsp = null;
        try {
            airCancelRsp = airCancelClient.cancelPNR(airReservationLocatorCode);
            logger.debug("Succesfully Cancelled PNR " + pnr );
            cancelPNRResponse.setSuccess(true);
        } catch (BaseCompassitesException e) {
            e.printStackTrace();
            logger.error("Travelport cancelPNR ", e);
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
