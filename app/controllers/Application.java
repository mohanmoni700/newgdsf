package controllers;


import com.compassites.model.*;
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
import services.MergeSearchResults;

import static play.mvc.Controller.request;
import static play.mvc.Results.ok;

@org.springframework.stereotype.Controller
public class Application {

    @Autowired
    private FlightSearchWrapper flightSearchWrapper;

    @Autowired
    private MergeSearchResults mergeSearchResults;

    @Autowired
    private BookingServiceWrapper bookingService;
    
    @Autowired
    private FlightInfoServiceWrapper flightInfoService;

    public Result flightSearch(){
        System.out.println("Request recieved");
        JsonNode json = request().body().asJson();

        SearchParameters  searchParameters = Json.fromJson(json, SearchParameters.class);
        System.out.println("SearchParamerters: " + json.toString());
//        flightSearchWrapper.search(searchParameters);
        mergeSearchResults.searchAndMerge(searchParameters);
        return Controller.ok(Json.toJson(searchParameters.redisKey()));
    }

    public Result generatePNR(){
        JsonNode json = request().body().asJson();

        TravellerMasterInfo travellerMasterInfo = Json.fromJson(json, TravellerMasterInfo.class);
        PNRResponse pnrResponse = bookingService.generatePNR(travellerMasterInfo);
        System.out.println("-----------------PNR Response: " + Json.toJson(pnrResponse));
        return Controller.ok(Json.toJson(pnrResponse));
    }

    public Result checkFareChangeAndAvailability(){
        JsonNode json = request().body().asJson();

        TravellerMasterInfo travellerMasterInfo = Json.fromJson(json, TravellerMasterInfo.class);
        PNRResponse pnrResponse = bookingService.checkFareChangeAndAvailability(travellerMasterInfo);

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
    public Result getBaggageInfo() {
    	JsonNode json = request().body().asJson();
    	SearchParameters searchParams = Json.fromJson(json.findPath("searchParams"), SearchParameters.class);
    	FlightItinerary flightItinerary = Json.fromJson(json.findPath("flightItinerary"), FlightItinerary.class);
    	String provider = Json.fromJson(json.findPath("provider"), String.class);
    	Boolean seamen = Json.fromJson(json.findPath("travellerInfo").findPath("seamen"), Boolean.class);
    	
    	FlightItinerary response = flightInfoService.getBaggageInfo(flightItinerary, searchParams, provider, seamen);
    	return Controller.ok(Json.toJson(response));
    }
    
    @BodyParser.Of(BodyParser.Json.class)
    public Result getCancellationFee() {
    	JsonNode json = request().body().asJson();
    	SearchParameters searchParams = Json.fromJson(json.findPath("searchParams"), SearchParameters.class);
    	FlightItinerary flightItinerary = Json.fromJson(json.findPath("flightItinerary"), FlightItinerary.class);
    	String provider = Json.fromJson(json.findPath("provider"), String.class);
    	Boolean seamen = Json.fromJson(json.findPath("travellerInfo").findPath("seamen"), Boolean.class);
    	
    	String fareRules = flightInfoService.getCancellationFee(flightItinerary, searchParams, provider, seamen);
    	return Controller.ok(Json.toJson(fareRules));
    }
    
    @BodyParser.Of(BodyParser.Json.class)
    public Result getFlightDetails() {
    	JsonNode json = request().body().asJson();
    	FlightItinerary flightItinerary = Json.fromJson(json.findPath("flightItinerary"), FlightItinerary.class);
    	String provider = Json.fromJson(json.findPath("provider"), String.class);
    	Boolean seamen = Json.fromJson(json.findPath("travellerInfo").findPath("seamen"), Boolean.class);
    	FlightItinerary response = flightInfoService.getInFlightDetails(flightItinerary, provider, seamen);
    	return Controller.ok(Json.toJson(response));
    }
    
    @BodyParser.Of(BodyParser.Json.class)
    public Result issueTicket(){
        JsonNode json = request().body().asJson();
        IssuanceRequest issuanceRequest = Json.fromJson(json, IssuanceRequest.class);
        IssuanceResponse issuanceResponse = bookingService.issueTicket(issuanceRequest);
        return ok(Json.toJson(issuanceResponse));
    }
    
    @BodyParser.Of(BodyParser.Json.class)
    public Result getPnrDetails(){
    	JsonNode json = request().body().asJson();
    	IssuanceRequest issuanceRequest = Json.fromJson(json, IssuanceRequest.class);
    	String gdsPNR = issuanceRequest.getGdsPNR();
    	String provider = issuanceRequest.getProvider();
    	TravellerMasterInfo masterInfo=bookingService.getPnrDetails(issuanceRequest,gdsPNR, provider);
    	System.out.println("==== in Application INFO ==== >>>>>>"+Json.toJson(masterInfo));
		return ok(Json.toJson(masterInfo));
    	
    }
    
}