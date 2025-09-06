package services;

import com.compassites.GDSWrapper.amadeus.ServiceHandler;
import com.compassites.model.FlightItinerary;
import com.compassites.model.SearchParameters;
import com.compassites.model.SearchResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import ennum.ConfigMasterConstants;
import models.FlightSearchOffice;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import play.Play;
import play.libs.Json;
import utils.AmadeusSessionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SplitTicketIndigoSearch implements SplitTicketSearch{

    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private ConfigurationMasterService configurationMasterService;
    static org.slf4j.Logger amadeusLogger = LoggerFactory.getLogger("amadeus");

    @Autowired
    private ServiceHandler serviceHandler;

    @Autowired
    private AmadeusSessionManager amadeusSessionManager;

    @Autowired
    private AmadeusSourceOfficeService sourceOfficeService;

    public RedisTemplate getRedisTemplate() {
        return redisTemplate;
    }

    public void setRedisTemplate(RedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    //private String searchOfficeID = play.Play.application().configuration().getString("split.ticket.officeId");
    private static String searchOfficeID = "";
    static org.slf4j.Logger logger = LoggerFactory.getLogger("gds");
    private static final OkHttpClient client = new OkHttpClient();
    private static final String endPoint = Play.application().configuration().getString("indigo.service.endPoint");
    static Logger indigoLogger = LoggerFactory.getLogger("indigo");
    @Override
    public List<SearchResponse> splitSearch(List<SearchParameters> searchParameters, ConcurrentHashMap<String, List<FlightItinerary>> concurrentHashMap, boolean isDomestic) throws Exception {
        List<SearchResponse> responses = new ArrayList<>();
        searchOfficeID = configurationMasterService.getConfig(ConfigMasterConstants.SPLIT_TICKET_AMADEUS_OFFICE_ID_GLOBAL.getKey());
        for (SearchParameters searchParameters1: searchParameters)  {
            FlightSearchOffice searchOffice = new FlightSearchOffice();
            searchOffice.setOfficeId("Indigo");
            searchOffice.setName("");
            if(searchOffice==null){
                logger.error("Invalid Indigo Office Id " + searchOfficeID + " provided for Split Ticketing");
                SearchResponse searchResponse = new SearchResponse();
                searchResponse.setFlightSearchOffice(searchOffice);
                searchResponse.setProvider("Indigo");
                responses.add(searchResponse);
            } else {
                SearchResponse searchResponse = search(searchParameters1, searchOffice);
                if (searchResponse.getAirSolution().getFlightItineraryList().size()>0) {
                    if (concurrentHashMap.containsKey(searchParameters1.getJourneyList().get(0).getOrigin())) {
                        concurrentHashMap.get(searchParameters1.getJourneyList().get(0).getOrigin()).addAll(new ArrayList<FlightItinerary>(searchResponse.getAirSolution().getSeamenHashMap().values()));
                        concurrentHashMap.get(searchParameters1.getJourneyList().get(0).getOrigin()).addAll(new ArrayList<FlightItinerary>(searchResponse.getAirSolution().getNonSeamenHashMap().values()));
                        System.out.println("Size of non indigo seamen if "+searchResponse.getAirSolution().getNonSeamenHashMap().values().size());
                    } else {
                        concurrentHashMap.put(searchParameters1.getJourneyList().get(0).getOrigin(), new ArrayList<FlightItinerary>(searchResponse.getAirSolution().getSeamenHashMap().values()));
                        System.out.println("Size of indigo non seamen else "+searchResponse.getAirSolution().getNonSeamenHashMap().values().size());
                        //concurrentHashMap.put(searchParameters1.getJourneyList().get(0).getOrigin(), new ArrayList<FlightItinerary>(searchResponse.getAirSolution().getNonSeamenHashMap().values()));
                    }
                    responses.add(searchResponse);
                }
            }
        }
        System.out.println("indigo split search response size: "+responses.size());
        System.out.println("indigo split search concurrentHashMap size: "+concurrentHashMap.size());
        for (Map.Entry<String, List<FlightItinerary>> flightItineraryEntry : concurrentHashMap.entrySet()) {
            logger.debug("flightItineraryEntry size: "+flightItineraryEntry.getKey()+"  -  "+flightItineraryEntry.getValue().size());
            System.out.println("indigo size: "+flightItineraryEntry.getKey()+"  -  "+flightItineraryEntry.getValue().size());
            if(flightItineraryEntry.getValue().size() == 0) {
                concurrentHashMap.remove(flightItineraryEntry.getKey());
            }
        }
        logger.info("indigo split search response size: "+responses.size());
        logger.info("indigo split search concurrentHashMap size: "+concurrentHashMap.size());

        System.out.println("indigo split search response size: "+responses.size());
        System.out.println("indigo split search concurrentHashMap size: "+concurrentHashMap.size());
        //logger.info("indigo split search concurrentHashMap details response : "+ Json.toJson(responses));
        //logger.info("indigo split search concurrentHashMap details: "+ Json.toJson(concurrentHashMap));
        return responses;
    }

    public SearchResponse search(SearchParameters searchParameters, FlightSearchOffice office) throws Exception {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonString = objectMapper.writeValueAsString(searchParameters);
            RequestBody requestBody = RequestBody.create(jsonString, MediaType.get("application/json; charset=utf-8"));
            Request request = new Request.Builder().url(endPoint+"flightSearch").post(requestBody).build();
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    SearchResponse searchResponse = objectMapper.readValue(responseBody, SearchResponse.class);
                    indigoLogger.debug("Indigo Flight Search Response: " + responseBody);
                    searchResponse.setFlightSearchOffice(office);
                    searchResponse.setProvider("Indigo");
                    return searchResponse;
                } else {
                    logger.error("Failed to fetch data from Indigo API: " + response.message() +
                            " for search parameters: " + Json.toJson(searchParameters));
                    SearchResponse searchResponse = new SearchResponse();
                    searchResponse.setFlightSearchOffice(office);
                    searchResponse.setProvider("Indigo");
                    return searchResponse;
                    //throw new Exception("Failed to fetch data from Indigo API: " + response.message());
                }
            }
        } catch (Exception e) {
            logger.error("Error during Indigo flight search: " + e.getMessage() +
                    " for search parameters: " + Json.toJson(searchParameters), e);
            //e.printStackTrace();
            SearchResponse searchResponse = new SearchResponse();
            searchResponse.setFlightSearchOffice(office);
            searchResponse.setProvider("Indigo");
            return searchResponse;
        }
    }
}
