package controllers;

import com.compassites.GDSWrapper.travelomatrix.SearchFlights;
import com.compassites.model.*;
import com.compassites.model.splitticket.PossibleRoutes;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import models.FlightSearchOffice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import services.PossibleRoutesService;
import services.SplitAmadeusSearch;
import services.SplitTicketSearchWrapper;
import services.TraveloMatrixFlightSearch;
import utils.SplitTicketHelper;
import utils.SplitTicketMerger;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static play.mvc.Controller.request;

@org.springframework.stereotype.Controller
public class SplitTicketController {

    static Logger logger = LoggerFactory.getLogger("gds");

    @Autowired
    private SplitTicketSearchWrapper splitTicketSearchWrapper;

    public Result findPossibleRoutes() throws Exception, InterruptedException, ExecutionException {
        logger.debug("Request recieved");
        JsonNode json = request().body().asJson();
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JodaModule());
        SearchParameters searchParameters = null;
        searchParameters = Json.fromJson(json, SearchParameters.class);
        String fromLocation = searchParameters.getJourneyList().get(0).getOrigin();
        String toLocation = searchParameters.getJourneyList().get(searchParameters.getJourneyList().size()-1).getDestination();
        SearchResponse searchResponse = splitTicketSearchWrapper.createRoutes(searchParameters);
        System.out.println("Routes "+Json.toJson(searchResponse));
        SplitTicketHelper splitTicketHelper = new SplitTicketHelper();
        Map<String, PossibleRoutes> possibleRoutesMap = splitTicketHelper.createPossibleRoutes(searchResponse);
        List<SearchParameters> searchParameters1 = splitTicketHelper.createSearchParameters(possibleRoutesMap,searchParameters, null);
        //ExecutorService executorService = Executors.newFixedThreadPool(searchParameters1.size()-1);
        //List<CompletableFuture<SearchResponse>> futures = new ArrayList<>();
        ConcurrentHashMap<String,List<FlightItinerary>> concurrentHashMap = new ConcurrentHashMap<>();
        List<SearchResponse> responses = splitTicketSearchWrapper.splitSearch(searchParameters1,concurrentHashMap,false);
        SplitTicketMerger splitTicketMerger = new SplitTicketMerger();
        List<FlightItinerary> flightItineraries = splitTicketMerger.mergingSplitTicket(fromLocation,toLocation,concurrentHashMap, false);
        ConcurrentHashMap<Integer,FlightItinerary> hashMap =  new ConcurrentHashMap<>();
        for (SearchResponse searchResponse2: responses) {
            splitTicketMerger.splitMergeResults(hashMap, searchResponse2);
        }
        /*ExecutorService executorService = Executors.newFixedThreadPool(searchParameters1.size()-1);
        List<CompletableFuture<SearchResponse>> futures = new ArrayList<>();
        for (SearchParameters searchParameters2: searchParameters1) {
            FlightSearchOffice searchOffice = new FlightSearchOffice();
            searchOffice.setOfficeId("BOMVS34C3");
            searchOffice.setName("");
            CompletableFuture<SearchResponse> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return splitTicketSearchWrapper.search(searchParameters2,searchOffice);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            },executorService);
            futures.add(future);
        }

        List<SearchResponse> mergedResults = futures.stream()
                .map(CompletableFuture::join)
                //.flatMap(List::stream)
                .distinct()
                .collect(Collectors.toList());

        executorService.shutdown();*/


        //logger.debug("responses "+Json.toJson(responses));
        ObjectNode nodes = Json.newObject();
        nodes.put("route", Json.toJson(possibleRoutesMap));
        nodes.put("searchparam", Json.toJson(searchParameters1));
        nodes.put("search", Json.toJson(responses));
        nodes.put("resultAfterMerge",Json.toJson(hashMap));
        nodes.put("result",Json.toJson(flightItineraries));
        return Controller.ok(nodes);
    }
}
