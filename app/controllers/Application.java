package controllers;


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
import services.FlightSearchWrapper;

import java.util.List;

import static play.mvc.Controller.request;

@org.springframework.stereotype.Controller
public class Application {

    @Autowired
    private FlightSearchWrapper flightSearchWrapper;

    @Autowired
    private BookingServiceWrapper bookingService;

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

}