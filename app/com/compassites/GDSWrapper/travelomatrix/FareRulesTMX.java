package com.compassites.GDSWrapper.travelomatrix;

import com.compassites.model.SearchParameters;
import com.compassites.model.travelomatrix.FareRuleRequest;
import com.compassites.model.travelomatrix.FlightSearchRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import configs.WsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import play.Play;
import play.libs.ws.WSRequestHolder;

import java.util.TimeZone;

public class FareRulesTMX {
    static Logger logger = LoggerFactory.getLogger("gds");

    static Logger travelomatrixLogger = LoggerFactory.getLogger("travelomatrix");

    public WsConfig wsconf = new WsConfig();

    FareRuleRequest fareRuleRequest = new FareRuleRequest();

    @Autowired
    public WSRequestHolder wsrholder;

    public JsonNode getFareRules(String resultToken) {
        JsonNode jsonRequest = getJsonFromResultToken(resultToken);
        JsonNode response = null;
        try {
            int timeout = Play.application().configuration().getInt("travelomatrix.timeout");
            wsrholder= wsconf.getRequestHolder("/FareRule");
            travelomatrixLogger.debug("TraveloMatrix getFareRules : Request to TM : "+ jsonRequest.toString());
            travelomatrixLogger.debug("TraveloMatrix getFareRules : Call to Travelomatrix Backend : "+ System.currentTimeMillis());
            response = wsrholder.post(jsonRequest).get(timeout).asJson();
            travelomatrixLogger.debug("TraveloMatrix getFareRules : Recieved Response from Travelomatrix Backend : "+ System.currentTimeMillis());
            travelomatrixLogger.debug("TraveloMatrix Response:"+response.toString());
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
