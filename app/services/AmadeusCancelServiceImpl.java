package services;

import com.amadeus.xml.pnracc_11_3_1a.PNRReply;
import com.compassites.GDSWrapper.amadeus.ServiceHandler;
import com.compassites.model.CancelPNRResponse;
import com.compassites.model.ErrorMessage;
import com.compassites.model.PROVIDERS;
import org.springframework.stereotype.Service;
import utils.ErrorMessageHelper;

/**
 * Created by Yaseen on 08-05-2015.
 */
@Service
public class AmadeusCancelServiceImpl implements CancelService {


    @Override
    public CancelPNRResponse cancelPNR(String pnr) {
        CancelPNRResponse cancelPNRResponse = new CancelPNRResponse();
        ServiceHandler serviceHandler = null;
        try {
            serviceHandler = new ServiceHandler();
            serviceHandler.logIn();

            serviceHandler.retrivePNR(pnr);
            PNRReply pnrReply = serviceHandler.cancelPNR(pnr);
            com.amadeus.xml.pnracc_11_3_1a.PNRReply savePNRReply = serviceHandler.savePNR();
            PNRReply retrievePNRReply = serviceHandler.retrivePNR(pnr);

            //todo check for origindestinationDetails in retrievePNRReply to confirm cancellation
            serviceHandler.logOut();
            cancelPNRResponse.setSuccess(true);
            return cancelPNRResponse;

        }catch (Exception e){
            e.printStackTrace();
            cancelPNRResponse.setSuccess(false);
            ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage("error", ErrorMessage.ErrorType.ERROR, PROVIDERS.AMADEUS.toString());
            cancelPNRResponse.setErrorMessage(errorMessage);
            return cancelPNRResponse;
        }
    }
}
