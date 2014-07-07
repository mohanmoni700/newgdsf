package controllers;


import com.compassites.model.SearchParameters;
import com.compassites.model.SearchResponse;
import com.fasterxml.jackson.databind.JsonNode;

import models.Bar;
import org.springframework.beans.factory.annotation.Autowired;
import play.data.Form;
import play.libs.Json;
import play.mvc.Result;
import play.mvc.Controller;
import services.FlightSearchWrapper;

import java.util.List;

import static play.mvc.Controller.request;

@org.springframework.stereotype.Controller
public class Application {

    @Autowired
    private FlightSearchWrapper flightSearchWrapper;


    public Result flightSearch(){
        //SearchParameters searchParameters = new Gson().fromJson(request().body().asText(), SearchParameters.class);
        System.out.println("Request recieved");
        JsonNode json = request().body().asJson();

        SearchParameters  searchParameters = Json.fromJson(json, SearchParameters.class);
        System.out.println("SearchParamerters: " + json.toString());
        List<SearchResponse> responseList =  flightSearchWrapper.search(searchParameters);
//        Controller.response().setHeader("Access-Control-Allow-Origin", "*");
//        Controller.response().setHeader("Access-Control-Allow-Methods", "GET, POST, PUT");
//        Controller.response().setHeader("Access-Control-Allow-Headers", "accept, content-type");
        return Controller.ok(Json.toJson(responseList));
    }

}