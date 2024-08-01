package com.compassites.GDSWrapper.travelomatrix;

import com.compassites.model.*;
import com.compassites.model.travelomatrix.FlightSearchRequest;
import com.compassites.model.travelomatrix.Segment;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.gson.Gson;
import configs.WsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import play.Configuration;
import play.Play;
import play.libs.F;
import play.libs.ws.WS;
import play.libs.ws.WSRequestHolder;
import play.libs.ws.WSResponse;
import utils.ErrorMessageHelper;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Function;


public class SearchFlights {
    static Logger logger = LoggerFactory.getLogger("gds");

    static Logger travelomatrixLogger = LoggerFactory.getLogger("travelomatrix");

   public WsConfig wsconf = new WsConfig();
   public FlightSearchRequest flightSearchRq = new FlightSearchRequest();
    @Autowired
    public WSRequestHolder wsrholder;

    public JsonNode getFlights(SearchParameters searchParameters) {
        JsonNode jsonRequest = getJsonFromSearchParams(searchParameters);
        JsonNode response = null;
        try {
            wsrholder= wsconf.getRequestHolder("/Search");
            travelomatrixLogger.debug("TraveloMatrixFlightSearch : Request to TM : "+ jsonRequest.toString());
            travelomatrixLogger.debug("TraveloMatrixFlightSearch : Call to Travelomatrix Backend : "+ System.currentTimeMillis());
            response = wsrholder.post(jsonRequest).get(30000).asJson();
            travelomatrixLogger.debug("TraveloMatrixFlightSearch : Recieved Response from Travelomatrix Backend : "+ System.currentTimeMillis());
            travelomatrixLogger.debug("TraveloMatrix Response:"+response.toString());
        }catch(Exception e){
            travelomatrixLogger.error(e.getMessage());
            e.printStackTrace();
        }
        return response;
   }


   public String getJourneyType(String value){
        String type = null;
        switch (value) {
            case "ONE_WAY":
                type= "OneWay";
            break;
            case "ROUND_TRIP":
                type= "Return";
            break;
            case "MULTI_CITY":
                type=  "Multicity";
            break;
            default:break;
        }
        return type;
   }
   public JsonNode getJsonFromSearchParams(SearchParameters searchParameters){
       JsonNode node = null;
       try {
           flightSearchRq.setAdultCount(searchParameters.getAdultCount().toString());
           flightSearchRq.setChildCount(searchParameters.getChildCount().toString());
           flightSearchRq.setInfantCount(searchParameters.getInfantCount().toString());
           flightSearchRq.setJourneyType(getJourneyType(searchParameters.getJourneyType().name()));
           flightSearchRq.setPreferredAirlines(searchParameters.getPreferredAirlinesList());
           flightSearchRq.setCabinClass(searchParameters.getCabinClass());
           List<SearchJourney> journeyList1 = searchParameters.getJourneyList();
           List<Segment> segments  = getSegments(journeyList1,flightSearchRq.getJourneyType());
           flightSearchRq.setSegmentsdata(segments);
           ObjectMapper mapper = new ObjectMapper();
           node= mapper.valueToTree(flightSearchRq);
       }catch(Exception e){
           travelomatrixLogger.error("Exception Occured:"+ e.getMessage());
           e.printStackTrace();
       }
       return node;
   }

    public List<Segment> getSegments(List<SearchJourney> journeyList , String journeyType){
        List<Segment> segments = new ArrayList<>();
        if(journeyType.equals("OneWay") || journeyType.equals("Multicity")){
            for(SearchJourney journey : journeyList) {
                Segment segment = new Segment();
                segment.setOrigin(journey.getOrigin());
                segment.setDestination(journey.getDestination());
                segment.setDepartureDate(journey.getTravelDateStr());
                segments.add(segment);
            }
        }else if(journeyType.equals("Return")){
            Segment segment = new Segment();
            segment.setOrigin(journeyList.get(0).getOrigin());
            segment.setDestination(journeyList.get(0).getDestination());
            segment.setDepartureDate(journeyList.get(0).getTravelDateStr());
            segment.setReturnDate(journeyList.get(1).getTravelDateStr());
            segments.add(segment);
        }
     return segments;
    }
}

