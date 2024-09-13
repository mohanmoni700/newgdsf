package com.compassites.GDSWrapper.travelomatrix;

import com.compassites.model.IssuanceRequest;
import com.compassites.model.traveller.TravellerMasterInfo;
import com.compassites.model.travelomatrix.IssueHoldTicket;
import com.compassites.model.travelomatrix.UpdatePNRRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import configs.WsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import play.libs.ws.WSRequestHolder;

public class IssueHoldTicketTMX {
    static Logger logger = LoggerFactory.getLogger("gds");

    static Logger travelomatrixLogger = LoggerFactory.getLogger("travelomatrix");

    public WsConfig wsconf = new WsConfig();

    @Autowired
    public WSRequestHolder wsrholder;

    public JsonNode issueHoldTicket(IssuanceRequest issuanceRequest){
        JsonNode jsonRequest = getJsonforIssueHoldTicketRequest(issuanceRequest);
        JsonNode response = null;
        try {
            wsrholder= wsconf.getRequestHolder("/IssueHoldTicket");
            travelomatrixLogger.debug("TraveloMatrixissueHoldTicket : Request to TM : "+ jsonRequest.toString());
            travelomatrixLogger.debug("TraveloMatrixissueHoldTicket : Call to Travelomatrix Backend : "+ System.currentTimeMillis());
            response = wsrholder.post(jsonRequest).get(30000).asJson();
            travelomatrixLogger.debug("TraveloMatrixissueHoldTicket : Recieved Response from Travelomatrix Backend : "+ System.currentTimeMillis());
            travelomatrixLogger.debug("TraveloMatrixissueHoldTicket Response:"+response.toString());
        }catch(Exception e){
            travelomatrixLogger.error(e.getMessage());
            e.printStackTrace();
        }
        return response;
    }

    public JsonNode getJsonforIssueHoldTicketRequest(IssuanceRequest issuanceRequest){
        JsonNode jsonNode = null;
        IssueHoldTicket issueHoldTicket = new IssueHoldTicket();
        issueHoldTicket.setAppReference(issuanceRequest.getAppRef());
        issueHoldTicket.setBookingId(issuanceRequest.getBookingId());
        issueHoldTicket.setPnr(issuanceRequest.getGdsPNR());
        issueHoldTicket.setSequenceNumber("0");
        try{
            ObjectMapper mapper = new ObjectMapper();
            jsonNode= mapper.valueToTree(issueHoldTicket);
        }catch(Exception e){
            travelomatrixLogger.error("Exception Occured:"+ e.getMessage());
            e.printStackTrace();
        }

        return jsonNode;
    }


    public JsonNode getUpdatePNRResponse(String appRef){
        JsonNode jsonRequest = getJsonforUpdatePNRRequest(appRef);
        JsonNode response = null;
        try {
            wsrholder= wsconf.getRequestHolder("/BookingDetails");
            travelomatrixLogger.debug("TraveloMatrixBookingDetails to TM : "+ jsonRequest.toString());
            travelomatrixLogger.debug("TraveloMatrixBookingDetails : Call to Travelomatrix Backend : "+ System.currentTimeMillis());
            response = wsrholder.post(jsonRequest).get(30000).asJson();
            travelomatrixLogger.debug("TraveloMatrixBookingDetails : Recieved Response from Travelomatrix Backend : "+ System.currentTimeMillis());
            travelomatrixLogger.debug("TraveloMatrixBookingDetails Response:"+response.toString());
        }catch(Exception e){
            travelomatrixLogger.error(e.getMessage());
            e.printStackTrace();
        }
        return response;
    }

    public JsonNode getJsonforUpdatePNRRequest(String appRef){
        JsonNode jsonNode = null;
        UpdatePNRRequest updatePNRRequest = new UpdatePNRRequest();
        updatePNRRequest.setAppReference(appRef);
        try{
            ObjectMapper mapper = new ObjectMapper();
            jsonNode= mapper.valueToTree(updatePNRRequest);
        }catch(Exception e){
            travelomatrixLogger.error("Exception Occured:"+ e.getMessage());
            e.printStackTrace();
        }

        return jsonNode;

    }
}
