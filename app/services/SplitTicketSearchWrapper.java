package services;

import com.compassites.constants.CacheConstants;
import com.compassites.model.*;
import com.compassites.model.splitticket.PossibleRoutes;
import models.Airport;
import models.FlightSearchOffice;
import models.SplitTicketTransitAirports;
import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import play.libs.Json;
import utils.SplitTicketHelper;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class SplitTicketSearchWrapper {

    @Autowired
    private SplitAmadeusSearch splitAmadeusSearch;

    @Autowired
    private PossibleRoutesService possibleRoutesService;

    @Autowired
    private RedisTemplate redisTemplate;

    static Logger logger = LoggerFactory.getLogger("splitticket");

    public boolean isSourceAirportDomestic = false;

    public SearchResponse createRoutes(SearchParameters searchParameters) {
        SearchResponse searchResponse = possibleRoutesService.createRoutes(searchParameters);
        return searchResponse;
    }

    public List<SearchResponse> splitSearch(List<SearchParameters> searchParametersList, ConcurrentHashMap<String,List<FlightItinerary>> concurrentHashMap, boolean isDomestic) throws Exception {
        List<SearchResponse> searchResponses = splitAmadeusSearch.splitSearch(searchParametersList, concurrentHashMap,isDomestic);
        return searchResponses;
    }

    public SearchResponse search(SearchParameters searchParameters, FlightSearchOffice office) throws Exception {
        SearchResponse searchResponse = splitAmadeusSearch.search(searchParameters, office);
        return searchResponse;
    }

    public List<SearchParameters> createSearch(SearchParameters searchParameters) throws Exception {
        List<SearchParameters> searchParametersList = new ArrayList<>();
        boolean isSourceDomestic = false;
        Airport airport = Airport.getAirportByIataCode(searchParameters.getJourneyList().get(0).getOrigin());
        boolean isDomestic = isDomesticAirport(airport);
        if(!isDomestic) {
            System.out.println("Domestic false");
            isSourceDomestic = true;
            searchParametersList = findNearestAirport(searchParameters,isDomestic, airport,isSourceDomestic);
            isSourceAirportDomestic = true;
            /*isSourceDomestic = false;
            Airport destinationAirport = Airport.getAirportByIataCode(searchParameters.getJourneyList().get(0).getOrigin());
            searchParametersList = findNearestAirport(searchParameters,false, destinationAirport,isSourceDomestic);
            ConcurrentHashMap<String,List<FlightItinerary>> concurrentHashMap = new ConcurrentHashMap<>();
            List<SearchResponse> responses = this.splitSearch(searchParametersList,concurrentHashMap,true);
            logger.debug("responses "+Json.toJson(responses));
            Map<String, PossibleRoutes> possibleRoutesMap = this.findNextSegmentDepartureDate(responses);
            SplitTicketHelper splitTicketHelper = new SplitTicketHelper();
            List<SearchParameters> searchParameters1 = splitTicketHelper.createSearchParameters(possibleRoutesMap,searchParameters, null);
            searchParametersList.addAll(searchParameters1);*/
        } else {
            System.out.println("Domestic true");
            isSourceAirportDomestic = false;
            isSourceDomestic = false;
            Airport destinationAirport = Airport.getAirportByIataCode(searchParameters.getJourneyList().get(0).getDestination());
            searchParametersList = findNearestAirport(searchParameters,isDomestic, destinationAirport,isSourceDomestic);
            ConcurrentHashMap<String,List<FlightItinerary>> concurrentHashMap = new ConcurrentHashMap<>();
            List<SearchResponse> responses = this.splitSearch(searchParametersList,concurrentHashMap,true);
            logger.debug("responses "+Json.toJson(responses));
            Map<String, PossibleRoutes> possibleRoutesMap = this.findNextSegmentDepartureDate(responses);
            SplitTicketHelper splitTicketHelper = new SplitTicketHelper();
            List<SearchParameters> searchParameters1 = splitTicketHelper.createSearchParameters(possibleRoutesMap,searchParameters, null);
            searchParametersList.addAll(searchParameters1);
        }
        return searchParametersList;
    }

    private Map<String, PossibleRoutes> findNextSegmentDepartureDate(List<SearchResponse> searchResponses) {
        System.out.println("findNextSegmentDepartureDate");
        Map<String, PossibleRoutes> possibleRoutesMap = new LinkedHashMap<>();
        for (SearchResponse searchResponse:searchResponses) {
            ConcurrentHashMap<Integer, FlightItinerary> nonSeamenHashMap = searchResponse.getAirSolution().getSeamenHashMap();
            for (Map.Entry<Integer, FlightItinerary> flightItineraryEntry: nonSeamenHashMap.entrySet()) {
                FlightItinerary flightItinerary = flightItineraryEntry.getValue();
                if(flightItinerary != null) {
                    List<Journey> journeyList = flightItinerary.getJourneyList();
                    for (Journey journey: journeyList) {
                        AirSegmentInformation airSegmentInformation = journey.getAirSegmentList().get(journey.getAirSegmentList().size()-1);
                        StringBuilder mapKey = new StringBuilder();
                        mapKey.append(journey.getAirSegmentList().get(0).getFromLocation());
                        mapKey.append(airSegmentInformation.getToLocation());
                        logger.debug(mapKey.toString()+"  Date -  "+airSegmentInformation.getArrivalDate()+"  -  "+airSegmentInformation.getArrivalTime());
                        //System.out.println(mapKey.toString()+"  Time -  "+airSegmentInformation.getArrivalTime());
                        if(!nonSeamenHashMap.containsKey(mapKey.toString())) {
                            PossibleRoutes possibleRoutes = new PossibleRoutes();
                            possibleRoutes.setFromLocation(journey.getAirSegmentList().get(0).getFromLocation());
                            possibleRoutes.setToLocation(airSegmentInformation.getToLocation());
                            possibleRoutes.setDepartureDate(journey.getAirSegmentList().get(0).getDepartureDate());
                            possibleRoutes.setDepartureTime(journey.getAirSegmentList().get(0).getDepartureTime());
                            possibleRoutes.setArrivalDate(airSegmentInformation.getArrivalDate());
                            possibleRoutes.setArrivalTime(airSegmentInformation.getArrivalTime());
                            possibleRoutes.setKey(flightItineraryEntry.getKey());
                            possibleRoutesMap.put(mapKey.toString(), possibleRoutes);
                        } else {
                            PossibleRoutes possibleRoutes = possibleRoutesMap.get(mapKey.toString());
                            if(possibleRoutes.getArrivalDate().after(airSegmentInformation.getArrivalDate())) {
                                possibleRoutes.setFromLocation(journey.getAirSegmentList().get(0).getFromLocation());
                                possibleRoutes.setToLocation(airSegmentInformation.getToLocation());
                                possibleRoutes.setDepartureDate(journey.getAirSegmentList().get(0).getDepartureDate());
                                possibleRoutes.setDepartureTime(journey.getAirSegmentList().get(0).getDepartureTime());
                                possibleRoutes.setArrivalDate(airSegmentInformation.getArrivalDate());
                                possibleRoutes.setArrivalTime(airSegmentInformation.getArrivalTime());
                                possibleRoutes.setKey(flightItineraryEntry.getKey());
                                possibleRoutesMap.put(mapKey.toString(), possibleRoutes);
                            }
                        }
                    }
                }
            }
        }
        System.out.println(Json.toJson(possibleRoutesMap));
        return possibleRoutesMap;
    }
    private List<SearchParameters> findNearestAirport(SearchParameters searchParameters, boolean isDomestic, Airport airport, boolean isSourceDomestic) {
        List<SearchParameters> searchParametersList = new ArrayList<>();
        if (!isDomestic) {
            List<Airport> airportList = Airport.findNearestAirport(airport.getLatitude(), airport.getLongitude());
            for (Airport airport1: airportList) {
                SearchParameters searchParameters1 = SerializationUtils.clone(searchParameters);
                searchParameters1.setJourneyList(searchJourneys(airport1, searchParameters, isSourceDomestic));
                searchParametersList.add(searchParameters1);
                if (isSourceDomestic) {
                    createDestinationJourney(searchParameters,searchParametersList,airport1);
                }
            }
        } else {
            List<Airport> airportList = Airport.findNearestAirport(airport.getLatitude(), airport.getLongitude());
            for (Airport airport1: airportList) {
                SearchParameters searchParameters1 = SerializationUtils.clone(searchParameters);
                searchParameters1.setJourneyList(searchJourneys(airport1, searchParameters, isSourceDomestic));
                searchParametersList.add(searchParameters1);
            }
        }
        return searchParametersList;
    }

    private List<SearchParameters> findNearestDestinationAirport(SearchParameters searchParameters, boolean isDomestic, Airport airport, boolean isSourceDomestic) {
        List<SearchParameters> searchParametersList = new ArrayList<>();
        if (!isDomestic) {
            List<Airport> airportList = Airport.findNearestAirport(airport.getLatitude(), airport.getLongitude());
            for (Airport airport1: airportList) {
                SearchParameters searchParameters1 = SerializationUtils.clone(searchParameters);
                searchParameters1.setJourneyList(searchJourneys(airport1, searchParameters, isSourceDomestic));
                searchParametersList.add(searchParameters1);
                if (!isSourceDomestic) {
                    createDestinationJourney(searchParameters,searchParametersList,airport1);
                }
            }
        }
        return searchParametersList;
    }

    private void createDestinationJourney(SearchParameters searchParameters, List<SearchParameters> searchParametersList, Airport airport) {
        SearchParameters searchParameters1 = SerializationUtils.clone(searchParameters);
        List<SearchJourney> journeyList = new ArrayList<>();
        for (SearchJourney searchJourneyItem: searchParameters.getJourneyList()) {
            SearchJourney searchJourney = SerializationUtils.clone(searchJourneyItem);
            searchJourney.setOrigin(airport.getIata_code());
            searchJourney.setDestination(searchParameters.getJourneyList().get(0).getDestination());
            searchJourney.setTravelDate(searchParameters.getJourneyList().get(0).getTravelDate());

            //ZonedDateTime zonedDateTime1 = ZonedDateTime.parse(searchParameters.getJourneyList().get(0).getTravelDateStr());
            ZonedDateTime zonedDateTime = searchParameters.getJourneyList().get(0).getTravelDate().toInstant().atZone(ZoneId.systemDefault());
            ZonedDateTime midnightTime = zonedDateTime.withHour(0).withMinute(0).withSecond(0).withNano(0).plusHours(12);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String formattedTime = midnightTime.format(formatter);

            searchJourney.setTravelDateStr(formattedTime);
            journeyList.add(searchJourney);
        }
        searchParameters1.setJourneyList(journeyList);
        searchParametersList.add(searchParameters1);
    }

    private boolean isDomesticAirport(Airport airport) {
        boolean isDomestic = false;
        if(airport.getClassification().equalsIgnoreCase("1") || airport.getClassification().equalsIgnoreCase("2")) {
            isDomestic = true;
        }
        return isDomestic;
    }

    private List<SearchJourney> searchJourneys(Airport airport, SearchParameters searchParameters, boolean isSourceDomestic) {
        List<SearchJourney> journeyList = new ArrayList<>();
        for (SearchJourney searchJourneyItem: searchParameters.getJourneyList()) {
            SearchJourney searchJourney = SerializationUtils.clone(searchJourneyItem);
            searchJourney.setOrigin(searchParameters.getJourneyList().get(0).getOrigin());
            searchJourney.setDestination(airport.getIata_code());
            searchJourney.setTravelDate(searchParameters.getJourneyList().get(0).getTravelDate());
            searchJourney.setTravelDateStr(searchParameters.getJourneyList().get(0).getTravelDateStr());
            journeyList.add(searchJourney);
        }
        return journeyList;
    }

    public void searchSplitTicket(SearchParameters searchParameters) throws Exception {
        logger.info("original searchParameters "+ Json.toJson(searchParameters));
        try {
            /*redisTemplate.opsForValue().set(searchParameters.redisKey() + ":status", "started");
            redisTemplate.expire(searchParameters.redisKey(), CacheConstants.CACHE_TIMEOUT_IN_SECS, TimeUnit.SECONDS);*/
            List<SplitTicketTransitAirports> splitTicketTransitAirports = isTransitAdded(searchParameters);
            SearchResponse searchResponse = null;
            List<SearchParameters> searchParameters1 = null;
            SplitTicketHelper splitTicketHelper = new SplitTicketHelper();
            /*if(splitTicketTransitAirports.size()>0) {
                String destination = searchParameters.getJourneyList().get(0).getDestination();
                SearchParameters searchParameters2 = SerializationUtils.clone(searchParameters);
                searchParameters2.getJourneyList().get(0).setDestination(splitTicketTransitAirports.get(0).getTransitAirport());
                searchResponse = createRoutes(searchParameters2);
                searchParameters1 = createSearchParameters(searchResponse, splitTicketTransitAirports,searchParameters2,destination);
            } else {*/
                //searchResponse = createRoutes(searchParameters);
                //Map<String, PossibleRoutes> possibleRoutesMap = splitTicketHelper.createPossibleRoutes(searchResponse);
                searchParameters1 = createSearch(searchParameters);//splitTicketHelper.createSearchParameters(possibleRoutesMap, searchParameters, null);
            //}
            logger.debug("Possible search routes "+Json.toJson(searchParameters1));
            createSplitSearch(searchParameters1, searchParameters);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<SearchParameters> createSearchParameters(SearchResponse searchResponse, List<SplitTicketTransitAirports> splitTicketTransitAirports, SearchParameters searchParameters,String toAirport ) {
        List<SearchParameters> searchParametersList = new ArrayList<>();
        searchParameters.setSequence(1);
        searchParametersList.add(searchParameters);
        ConcurrentHashMap<Integer, FlightItinerary> nonSeamenHashMap = searchResponse.getAirSolution().getNonSeamenHashMap();
        Map<String, FlightItinerary> toLocationMap = new HashMap<>();
        for (Map.Entry<Integer, FlightItinerary> flightItineraryEntry: nonSeamenHashMap.entrySet()) {
            FlightItinerary flightItinerary = flightItineraryEntry.getValue();
            if(flightItinerary != null) {
                List<Journey> journeyList = flightItinerary.getJourneyList();
                for (Journey journey : journeyList) {
                    String destination = journey.getAirSegmentList().get(journey.getAirSegmentList().size()-1).getToLocation();
                    boolean isAvailable = splitTicketTransitAirports.stream().allMatch(splitTicketTransitAirports1 -> {
                        return destination.equalsIgnoreCase(splitTicketTransitAirports1.getTransitAirport());
                    });
                    if (isAvailable) {
                        if(!toLocationMap.containsKey(destination)) {
                            toLocationMap.put(destination, flightItinerary);
                            break;
                        }
                    }
                }
            }
        }
        createSearchParam(toLocationMap,searchParametersList,searchParameters,toAirport);
        return searchParametersList;
    }

    private void createSearchParam(Map<String, FlightItinerary> toLocationMap,List<SearchParameters> searchParametersList, SearchParameters searchParameters, String toAirport) {
        List<SearchJourney> journeyList = new ArrayList<>();
        for (Map.Entry<String, FlightItinerary> flightItineraryEntry: toLocationMap.entrySet()) {
            SearchParameters searchParameters1 = SerializationUtils.clone(searchParameters);
            searchParameters1.setSequence(2);
            FlightItinerary flightItinerary = flightItineraryEntry.getValue();
            for (SearchJourney searchJourneyItem : searchParameters.getJourneyList()) {
                SearchJourney searchJourney = SerializationUtils.clone(searchJourneyItem);
                searchJourney.setOrigin(flightItinerary.getJourneyList().get(0).getAirSegmentList().get(flightItinerary.getJourneyList().get(0).getAirSegmentList().size()-1).getToLocation());
                searchJourney.setDestination(toAirport);
                searchJourney.setTravelDate(flightItinerary.getJourneyList().get(0).getAirSegmentList().get(flightItinerary.getJourneyList().get(0).getAirSegmentList().size()-1).getArrivalDate());

                ZonedDateTime zonedDateTime = ZonedDateTime.parse(flightItinerary.getJourneyList().get(0).getAirSegmentList().get(flightItinerary.getJourneyList().get(0).getAirSegmentList().size()-1).getArrivalTime());
                ZonedDateTime midnightTime = zonedDateTime.withHour(0).withMinute(0).withSecond(0).withNano(0);
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                String formattedTime = midnightTime.format(formatter);

                searchJourney.setTravelDateStr(formattedTime);
                journeyList.add(searchJourney);
            }
            searchParameters1.setJourneyList(journeyList);
            searchParametersList.add(searchParameters1);
        }
    }

    private List<SplitTicketTransitAirports> isTransitAdded(SearchParameters searchParameters) {
        String toLocation = searchParameters.getJourneyList().get(searchParameters.getJourneyList().size()-1).getDestination();
        List<SplitTicketTransitAirports> splitTicketTransitAirports = SplitTicketTransitAirports.getAllTransitByIata(toLocation);
        return splitTicketTransitAirports;
    }

    public void createSplitSearch(List<SearchParameters> searchParameters, SearchParameters originalSearchRequest) throws Exception {
        splitAmadeusSearch.splitTicketSearch(searchParameters, originalSearchRequest, isSourceAirportDomestic);
    }

}
