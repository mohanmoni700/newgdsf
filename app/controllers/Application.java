package controllers;


import com.compassites.model.SearchParameters;
import com.fasterxml.jackson.databind.JsonNode;

import models.Bar;
import org.springframework.beans.factory.annotation.Autowired;
import play.data.Form;
import play.libs.Json;
import play.mvc.Result;
import services.FlightSearchWrapper;
import views.html.index;

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

        return play.mvc.Controller.ok(Json.toJson(flightSearchWrapper.search(searchParameters)));
    }

}