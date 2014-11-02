package controllers;


import com.compassites.model.FlightItinerary;
import com.compassites.model.PNRResponse;
import com.compassites.model.SearchParameters;
import com.compassites.model.SearchResponse;
import com.compassites.model.traveller.TravellerMasterInfo;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import services.BookingServiceWrapper;
import services.FlightInfoServiceWrapper;
import services.FlightSearchWrapper;

import java.util.List;

import static play.mvc.Controller.request;
import static play.mvc.Results.ok;

@org.springframework.stereotype.Controller
public class Application {

    @Autowired
    private FlightSearchWrapper flightSearchWrapper;

    @Autowired
    private BookingServiceWrapper bookingService;
    
    @Autowired
    private FlightInfoServiceWrapper flightInfoService;

    public Result flightSearch(){
        System.out.println("Request recieved");
        JsonNode json = request().body().asJson();

        SearchParameters  searchParameters = Json.fromJson(json, SearchParameters.class);
        System.out.println("SearchParamerters: " + json.toString());
        List<SearchResponse> responseList =  flightSearchWrapper.search(searchParameters);
        return Controller.ok(Json.toJson(searchParameters.redisKey()));
    }

    public Result generatePNR(){
        JsonNode json = request().body().asJson();

        TravellerMasterInfo travellerMasterInfo = Json.fromJson(json, TravellerMasterInfo.class);
        PNRResponse pnrResponse = bookingService.generatePNR(travellerMasterInfo);
        System.out.println("-----------------PNR Response: " + Json.toJson(pnrResponse));
        return Controller.ok(Json.toJson(pnrResponse));
    }

    @BodyParser.Of(BodyParser.Json.class)
    public Result priceChangePNR(){
        JsonNode json = request().body().asJson();

        TravellerMasterInfo travellerMasterInfo = Json.fromJson(json, TravellerMasterInfo.class);
        PNRResponse pnrResponse = bookingService.priceChangePNR(travellerMasterInfo);
        System.out.println("-----------------PNR Response: " + Json.toJson(pnrResponse));
        return Controller.ok(Json.toJson(pnrResponse));
    }
    
    @BodyParser.Of(BodyParser.Json.class)
    public Result getFlightInfo() {
    	JsonNode json = request().body().asJson();
    	SearchParameters searchParams = Json.fromJson(json.findPath("searchParams"), SearchParameters.class);
    	FlightItinerary flightItinerary = Json.fromJson(json.findPath("flightItinerary"), FlightItinerary.class);
    	FlightItinerary response = flightInfoService.getFlightInfo(flightItinerary, searchParams);
    	return Controller.ok(Json.toJson(response));
    }

    public Result issueTicket(String gdsPNR){
        PNRResponse pnrResponse = bookingService.issueTicket(gdsPNR);
        return ok();
    }
}