package services;

import com.compassites.GDSWrapper.mystifly.Mystifly;
import com.compassites.GDSWrapper.mystifly.SessionsHandler;
import com.compassites.model.CancelPNRResponse;
import com.compassites.model.ErrorMessage;
import com.compassites.model.PROVIDERS;
import onepoint.mystifly.CancelBookingDocument;
import onepoint.mystifly.CancelBookingResponseDocument;
import onepoint.mystifly.OnePointStub;
import org.datacontract.schemas._2004._07.mystifly_onepoint.AirCancelRQ;
import org.datacontract.schemas._2004._07.mystifly_onepoint.AirCancelRQDocument;
import org.datacontract.schemas._2004._07.mystifly_onepoint.SessionCreateRS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import utils.ErrorMessageHelper;

import java.rmi.RemoteException;

/**
 * Created by yaseen on 30-03-2016.
 */
@Service
public class MystiflyCancelServiceImpl implements CancelService {

    static org.slf4j.Logger logger = LoggerFactory.getLogger("gds");

    static Logger mystiflyLogger = LoggerFactory.getLogger("mystifly");

    @Override
    public CancelPNRResponse cancelPNR(String pnr) {
        logger.info("mystifly cancelPNR called .....");
        CancelPNRResponse cancelPNRResponse = new CancelPNRResponse();

        SessionsHandler sessionsHandler = new SessionsHandler();
        SessionCreateRS sessionRS = sessionsHandler.login();
        OnePointStub onePointStub = sessionsHandler.getOnePointStub();

        //todo -- check ticket is issued then only cancel the pnr
        CancelBookingDocument cancelBookingDocument = CancelBookingDocument.Factory.newInstance();

        AirCancelRQ airCancelRQ = cancelBookingDocument.addNewCancelBooking().addNewRq();
        airCancelRQ.setSessionId(sessionRS.getSessionId());
        airCancelRQ.setUniqueID(pnr);
        airCancelRQ.setTarget(Mystifly.TARGET);

        try {
            mystiflyLogger.debug("AirCancelRQ : " + cancelBookingDocument.xmlText());
            CancelBookingResponseDocument cancelBookingResponseDocument = onePointStub.cancelBooking(cancelBookingDocument);
            mystiflyLogger.debug("AirCancelRes : " + cancelBookingResponseDocument.xmlText());
            boolean isSuccess = cancelBookingResponseDocument.getCancelBookingResponse().getCancelBookingResult().getSuccess();
            cancelPNRResponse.setSuccess(isSuccess);
        } catch (RemoteException e) {
            logger.error("Error in Mystifly cancelPNR", e);
            ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage("error", ErrorMessage.ErrorType.ERROR, PROVIDERS.MYSTIFLY.toString());
            cancelPNRResponse.setSuccess(false);
            cancelPNRResponse.setErrorMessage(errorMessage);
            e.printStackTrace();
        }
        return cancelPNRResponse;
    }
}
