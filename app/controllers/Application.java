package controllers;


import com.compassites.model.CabinClass;
import com.compassites.model.Passenger;
import com.compassites.model.SearchParameters;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import models.Bar;
import org.springframework.beans.factory.annotation.Autowired;
import play.data.Form;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Result;
import services.BarService;
import services.FlightSearchWrapper;
import views.html.index;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static play.mvc.Controller.request;

@org.springframework.stereotype.Controller
public class Application {

    @Autowired
    private BarService barService;

    @Autowired
    private FlightSearchWrapper flightSearchWrapper;


    public Result index() {
        return play.mvc.Controller.ok(index.render(Form.form(Bar.class)));
    }

    public Result addBar() {
        Form<Bar> form = Form.form(Bar.class).bindFromRequest();
        Bar bar = form.get();
        barService.addBar(bar);
        return play.mvc.Controller.redirect(controllers.routes.Application.index());
    }

    public Result listBars() {
        return play.mvc.Controller.ok(Json.toJson(barService.getAllBars()));
    }


    public Result flightSearch(){
        //SearchParameters searchParameters = new Gson().fromJson(request().body().asText(), SearchParameters.class);
        System.out.println("Request recieved");
        JsonNode json = request().body().asJson();

        SearchParameters  searchParameters = Json.fromJson(json, SearchParameters.class);
        System.out.println("SearchParamerters: " + json.toString());
        /*searchParameters.getOnwardJourney().setCabinClass(CabinClass.ECONOMY);
        searchParameters.setCurrency(json.findPath("currency").textValue());
        searchParameters.setDestination(json.findPath("destination").textValue());
        searchParameters.setOrigin(json.findPath("origin").textValue());
        String dateText = json.findPath("onwardJourney").findPath("journeyDate").textValue();
        System.out.println("Date :"+dateText);
        Date onwardDate = null;
        try {
            onwardDate = new SimpleDateFormat("MM/dd/yyyy").parse(dateText);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        searchParameters.getOnwardJourney().setJourneyDate(onwardDate );
        searchParameters.setNoOfStops(json.findPath("noOfStops").asInt());
        Passenger passenger = new Passenger();
        passenger.setAge(30);
        passenger.setPassengerType(json.findPath("passengerType").textValue());
        searchParameters.getPassengers().add(passenger);*/
        System.out.println("Details: "+searchParameters.getPassengers().get(0).getPassengerType()+  searchParameters.getOrigin()+
                searchParameters.getDestination()+searchParameters.getOnwardJourney().getJourneyDate()+ searchParameters.getPassengers().get(0).getPassengerType());

        return play.mvc.Controller.ok(Json.toJson(flightSearchWrapper.search(searchParameters)));
    }

}