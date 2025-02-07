package services;

import com.compassites.GDSWrapper.travelomatrix.CancelPNRTMX;
import com.compassites.GDSWrapper.travelomatrix.HoldTicketTMX;
import com.compassites.model.CancelPNRResponse;
import com.compassites.model.ErrorMessage;
import com.compassites.model.travelomatrix.ResponseModels.CancelBookingResponse.CancellationResponse;
import com.compassites.model.travelomatrix.ResponseModels.HoldTicket.HoldTicketResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TraveloMatrixCancelService {

    static org.slf4j.Logger travelomatrixLogger = LoggerFactory.getLogger("travelomatrix");

    static org.slf4j.Logger logger = LoggerFactory.getLogger("gds");

    public CancelPNRTMX cancelPNRTMX = new CancelPNRTMX();
    public CancelPNRResponse cancelPNR(String pnr, String appRef, String bookingId, Boolean isFullPNR, List<String> ticketList){
       CancelPNRResponse cancelPNRResponse = null;
       travelomatrixLogger.debug("TMX cancel PNR called...........");
        JsonNode jsonResponse = cancelPNRTMX.CancelPNR(pnr, appRef, bookingId,ticketList,isFullPNR);
        try {
            travelomatrixLogger.debug("Response for generatePNR: " + jsonResponse);
            CancellationResponse response = new ObjectMapper().treeToValue(jsonResponse, CancellationResponse.class);
            if (response.getStatus() == 0) {
                travelomatrixLogger.debug("Response is not valid for CancelPNR: " + response.getMessage());
                cancelPNRResponse = getCancelPnrResponse(response);
                ErrorMessage em = new ErrorMessage();
                em.setMessage(response.getMessage());
                cancelPNRResponse.setErrorMessage(em);
            } else {
                cancelPNRResponse = getCancelPnrResponse(response);
            }
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        return cancelPNRResponse;
    }
    public CancelPNRResponse getCancelPnrResponse(CancellationResponse cancellationResponse){
        CancelPNRResponse cancelPNRResponse = new CancelPNRResponse();
        if(cancellationResponse.getStatus() != 0)
         cancelPNRResponse.setSuccess(true);
        else {
            cancelPNRResponse.setSuccess(false);
        }

        return cancelPNRResponse;
    }

}
