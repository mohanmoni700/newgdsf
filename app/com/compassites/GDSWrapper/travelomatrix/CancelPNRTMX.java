package com.compassites.GDSWrapper.travelomatrix;

import com.compassites.model.traveller.TravellerMasterInfo;
import com.compassites.model.travelomatrix.CancellationRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import configs.WsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import play.Play;
import play.libs.ws.WSRequestHolder;

import java.util.ArrayList;
import java.util.List;

public class CancelPNRTMX {
    static Logger logger = LoggerFactory.getLogger("gds");

    static Logger travelomatrixLogger = LoggerFactory.getLogger("travelomatrix");

    public WsConfig wsconf = new WsConfig();

    @Autowired
    public WSRequestHolder wsrholder;

    public JsonNode CancelPNR(String pnr,String appRef,String bookingId,List<String> ticketList,Boolean isFullPNR){
        JsonNode jsonRequest = getJsonforCancelPNRRequest(pnr,appRef,bookingId,ticketList,isFullPNR);
        JsonNode response = null;
        try {
            int timeout = Play.application().configuration().getInt("travelomatrix.timeout");
            wsrholder= wsconf.getRequestHolder("/CancelBooking");
            travelomatrixLogger.debug("TraveloMatrixFlightSearch : Request to TM : "+ jsonRequest.toString());
            travelomatrixLogger.debug("TraveloMatrixFlightSearch : Call to Travelomatrix Backend : "+ System.currentTimeMillis());
            response = wsrholder.post(jsonRequest).get(timeout).asJson();
            travelomatrixLogger.debug("TraveloMatrixFlightSearch : Recieved Response from Travelomatrix Backend : "+ System.currentTimeMillis());
            travelomatrixLogger.debug("TraveloMatrix Response:"+response.toString());
        }catch(Exception e){
            travelomatrixLogger.error(e.getMessage());
            e.printStackTrace();
        }
        return response;

    }

    public JsonNode getJsonforCancelPNRRequest(String pnr,String appRef,String bookingId,List<String> ticketList,Boolean isFullPNR){
        JsonNode jsonNode = null;
        CancellationRequest cancellationRequest = new CancellationRequest();
        cancellationRequest.setSequenceNumber(0);
        cancellationRequest.setpNR(pnr);
        cancellationRequest.setAppReference(appRef);
        cancellationRequest.setBookingId(bookingId);
        ArrayList<String> ticketIdList = new ArrayList<>();
        if(ticketList != null && ticketList.size() >0 && !isFullPNR){
            cancellationRequest.setisFullBookingCancel(Boolean.FALSE);
            ticketIdList.addAll(ticketList);
        }else {
            cancellationRequest.setisFullBookingCancel(Boolean.TRUE);
            ticketIdList.addAll(ticketList);
        }
        cancellationRequest.setTicketId(ticketIdList);
        try{
            ObjectMapper mapper = new ObjectMapper();
            jsonNode= mapper.valueToTree(cancellationRequest);

        }catch(Exception e){
            travelomatrixLogger.error("Exception Occured while creating Cancellation Request:"+ e.getMessage());
            e.printStackTrace();
        }
        return jsonNode;
    }
}
