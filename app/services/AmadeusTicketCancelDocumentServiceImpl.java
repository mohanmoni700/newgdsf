package services;

import com.amadeus.xml.pnracc_11_3_1a.PNRReply;
import com.amadeus.xml.trcanr_14_1_1a.TicketCancelDocumentReply;
import com.compassites.GDSWrapper.amadeus.ServiceHandler;
import com.compassites.model.ErrorMessage;
import com.compassites.model.PROVIDERS;
import com.compassites.model.TicketCancelDocumentResponse;
import models.AmadeusSessionWrapper;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import play.libs.Json;
import utils.ErrorMessageHelper;

import java.util.List;

@Component
public class AmadeusTicketCancelDocumentServiceImpl implements TicketCancelDocumentService {


    static org.slf4j.Logger logger = LoggerFactory.getLogger("gds");

    @Autowired
    private ServiceHandler serviceHandler;

    @Override
    public TicketCancelDocumentResponse ticketCancelDocument(String pnr, List<String> ticketsList) {
        logger.debug("ticketCancelDocument called for PNR : " + pnr);
        TicketCancelDocumentResponse ticketCancelDocumentResponse = new TicketCancelDocumentResponse();
        AmadeusSessionWrapper amadeusSessionWrapper = null;
        try {
            amadeusSessionWrapper = serviceHandler.logIn();

            PNRReply pnrReply = serviceHandler.retrivePNR(pnr, amadeusSessionWrapper);
            logger.debug("retrieve PNR ===================================>>>>>>>>>>>>>>>>>>>>>>>>>"
                            + "\n" + Json.toJson(pnrReply));

            TicketCancelDocumentReply ticketCancelDocumentReply = serviceHandler.ticketCancelDocument(pnr,  ticketsList, pnrReply, amadeusSessionWrapper);
            ticketCancelDocumentResponse.setSuccess(true);
            logger.debug("Successfully Cancelled ticket document " + ticketCancelDocumentReply );
            return ticketCancelDocumentResponse;

        }catch (Exception e){
            e.printStackTrace();
            logger.error(pnr + " : Error in ticket document cancellation ", e);
            ticketCancelDocumentResponse.setSuccess(false);
            ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage("error", ErrorMessage.ErrorType.ERROR, PROVIDERS.AMADEUS.toString());
            ticketCancelDocumentResponse.setErrorMessage(errorMessage);
            return ticketCancelDocumentResponse;
        }finally {
            serviceHandler.logOut(amadeusSessionWrapper);
        }
    }
}
