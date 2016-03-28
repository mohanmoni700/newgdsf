package services;

import com.amadeus.xml.pnracc_11_3_1a.PNRReply;
import com.compassites.GDSWrapper.amadeus.ServiceHandler;
import com.compassites.model.CancelPNRResponse;
import com.compassites.model.ErrorMessage;
import com.compassites.model.PROVIDERS;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import utils.ErrorMessageHelper;

/**
 * Created by Yaseen on 08-05-2015.
 */
@Service
public class AmadeusCancelServiceImpl implements CancelService {

    static org.slf4j.Logger logger = LoggerFactory.getLogger("gds");

    @Override
    public CancelPNRResponse cancelPNR(String pnr) {
        logger.debug("cancelPNR called for PNR : " + pnr);
        CancelPNRResponse cancelPNRResponse = new CancelPNRResponse();
        ServiceHandler serviceHandler = null;
        try {
            serviceHandler = new ServiceHandler();
            serviceHandler.logIn();

            PNRReply pnrReply = serviceHandler.retrivePNR(pnr);
            for(PNRReply.DataElementsMaster.DataElementsIndiv dataElementsDiv : pnrReply.getDataElementsMaster().getDataElementsIndiv()) {
                if ("FA".equals(dataElementsDiv.getElementManagementData().getSegmentName())) {
                    logger.debug("Tickets are already issued cannot cancel the pnr: " + pnr);
                    cancelPNRResponse.setSuccess(false);
                    ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage("ticketIssuedError", ErrorMessage.ErrorType.ERROR, PROVIDERS.AMADEUS.toString());
                    cancelPNRResponse.setErrorMessage(errorMessage);
                    return cancelPNRResponse;
                }
            }
            pnrReply = serviceHandler.cancelPNR(pnr);
            com.amadeus.xml.pnracc_11_3_1a.PNRReply savePNRReply = serviceHandler.savePNR();
            PNRReply retrievePNRReply = serviceHandler.retrivePNR(pnr);

            //todo check for origindestinationDetails in retrievePNRReply to confirm cancellation
            cancelPNRResponse.setSuccess(true);
            logger.debug("Succesfully Cancelled PNR " + pnr );
            return cancelPNRResponse;

        }catch (Exception e){
            e.printStackTrace();
            logger.error(pnr + " : Error in PNR cancellation ", e);
            cancelPNRResponse.setSuccess(false);
            ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage("error", ErrorMessage.ErrorType.ERROR, PROVIDERS.AMADEUS.toString());
            cancelPNRResponse.setErrorMessage(errorMessage);
            return cancelPNRResponse;
        }finally {
            serviceHandler.logOut();
        }
    }
}
