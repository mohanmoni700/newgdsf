package com.compassites.GDSWrapper.travelomatrix;

import com.compassites.model.travelomatrix.FareRuleRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import configs.WsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import play.Play;
import play.libs.ws.WSRequestHolder;

public class ExtraServicesTMX {
    static Logger logger = LoggerFactory.getLogger("gds");

    static Logger travelomatrixLogger = LoggerFactory.getLogger("travelomatrix");

    public WsConfig wsconf = new WsConfig();

    FareRuleRequest fareRuleRequest = new FareRuleRequest();

    @Autowired
    public WSRequestHolder wsrholder;

    public JsonNode getExtraServices(String resultToken) {
        JsonNode jsonRequest = getJsonFromResultToken(resultToken);
        JsonNode response = null;
        try {
            int timeout = Play.application().configuration().getInt("travelomatrix.timeout");
            wsrholder= wsconf.getRequestHolder("/ExtraServices");
            travelomatrixLogger.debug("TraveloMatixExtraServices : Request to TM : "+ jsonRequest.toString());
            travelomatrixLogger.debug("TraveloMatixExtraServices : Call to Travelomatix Backend : "+ System.currentTimeMillis());
            response = wsrholder.post(jsonRequest).get(timeout).asJson();
            travelomatrixLogger.debug("TraveloMatixExtraServices : Recieved Response from Travelomatrix Backend : "+ System.currentTimeMillis());
            travelomatrixLogger.debug("TraveloMatixExtraServices Response:"+response.toString());
        }catch(Exception e){
            travelomatrixLogger.error(e.getMessage());
            e.printStackTrace();
        }
        return response;
    }

    public JsonNode getJsonFromResultToken(String resultToken){
        JsonNode node = null;
        try{
            fareRuleRequest.setResultToken(resultToken);
            ObjectMapper mapper = new ObjectMapper();
            node= mapper.valueToTree(fareRuleRequest);

        }catch(Exception e){
            travelomatrixLogger.error("Exception Occured:"+ e.getMessage());
            e.printStackTrace();
        }
        return node;
    }
}
