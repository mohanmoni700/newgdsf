package services;

import com.compassites.model.*;
import com.compassites.model.splitticket.PossibleRoutes;
import ennum.ConfigMasterConstants;
import models.Airport;
import models.FlightSearchOffice;
import models.SplitTicketTransitAirports;
import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import play.libs.Json;
import utils.SplitTicketHelper;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class SplitTicketSearchWrapper {

    @Autowired
    @Qualifier("splitAmadeusSearchWrapper")
    private SplitAmadeusSearch splitAmadeusSearch;

    @Autowired
    private PossibleRoutesService possibleRoutesService;

    @Autowired
    private RedisTemplate redisTemplate;

    static Logger logger = LoggerFactory.getLogger("splitticket");

    public boolean isSourceAirportDomestic = false;
    public boolean isDestinationAirportDomestic = false;
    @Autowired
    private ConfigurationMasterService configurationMasterService;
    @Autowired
    private SplitTicketHelper splitTicketHelper;

    /*@Autowired
    private SplitTicketHelper splitTicketHelper;*/

    //private static final boolean transitEnabled = play.Play.application().configuration().getBoolean("split.transitpoint.enabled");
    public boolean transitEnabled = false; // For testing purposes, set to true
    private Map<String, String> configMap;
    /*@PostConstruct
    public void loadSplitConfig() {
        logger.info("Loading split ticket configurations" + System.currentTimeMillis() + " - " + System.nanoTime() + " - " + System.currentTimeMillis() / 1000);
        configMap = configurationMasterService.getAllConfigurations(0, 0, "splitTicket");
        logger.info("Split ticket configurations loaded: " + configMap + " - " + System.currentTimeMillis() + " - " + System.nanoTime() + " - " + System.currentTimeMillis() / 1000);
    }*/
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

    public List<SearchParameters> createTransitPointSearch(SearchParameters searchParameters, List<SplitTicketTransitAirports> splitTicketTransitAirports) throws Exception {
        List<SearchParameters> searchParametersList = new ArrayList<>();
        List<SearchParameters> searchParameters2 = new ArrayList<>();

        for (SplitTicketTransitAirports splitTicketTransitAirports1: splitTicketTransitAirports) {
            SearchParameters searchParameters1 = SerializationUtils.clone(searchParameters);
            List<SearchJourney> journeyList = new ArrayList<>();
            for (SearchJourney searchJourneyItem: searchParameters.getJourneyList()) {
                SearchJourney searchJourney = SerializationUtils.clone(searchJourneyItem);
                searchJourney.setOrigin(searchParameters.getJourneyList().get(0).getOrigin());
                searchJourney.setDestination(splitTicketTransitAirports1.getTransitAirport());
                searchJourney.setTravelDate(searchParameters.getJourneyList().get(0).getTravelDate());
                searchJourney.setTravelDateStr(searchParameters.getJourneyList().get(0).getTravelDateStr());
                journeyList.add(searchJourney);
            }
            searchParameters1.setJourneyList(journeyList);
            searchParameters2.add(searchParameters1);
        }


        /*SearchParameters searchParameters1 = SerializationUtils.clone(searchParameters);
        List<SearchJourney> journeyList = new ArrayList<>();
        for (SearchJourney searchJourneyItem: searchParameters.getJourneyList()) {
            SearchJourney searchJourney = SerializationUtils.clone(searchJourneyItem);
            searchJourney.setOrigin(searchParameters.getJourneyList().get(0).getOrigin());
            searchJourney.setDestination(splitTicketTransitAirports.get(0).getTransitAirport());
            searchJourney.setTravelDate(searchParameters.getJou rneyList().get(0).getTravelDate());
            searchJourney.setTravelDateStr(searchParameters.getJourneyList().get(0).getTravelDateStr());
            journeyList.add(searchJourney);
        }
        searchParameters1.setJourneyList(journeyList);
        searchParameters2.add(searchParameters1);*/


        ConcurrentHashMap<String,List<FlightItinerary>> concurrentHashMap = new ConcurrentHashMap<>();
        List<SearchResponse> responses = this.splitSearch(searchParameters2,concurrentHashMap,true);
        boolean isSeamenSearch = true;
        if (responses == null || responses.get(0).getAirSolution().getFlightItineraryList().size() == 0) {
            logger.info("Split ticket search returned no results or only one result, returning original search parameters");
            SearchParameters searchParameters1 = SerializationUtils.clone(searchParameters2.get(0));
            searchParameters1.setBookingType(BookingType.NON_MARINE);
            List<SearchParameters> searchParameters3 = new ArrayList<>();
            searchParameters3.add(searchParameters1);
            responses = this.splitSearch(searchParameters3,concurrentHashMap,true);
            isSeamenSearch = false;
            searchParameters2.add(searchParameters1);
        }
        logger.debug("responses "+Json.toJson(responses));
        Map<String, PossibleRoutes> possibleRoutesMap = this.findNextSegmentDepartureDate(responses,isSeamenSearch);
        System.out.println("responses "+Json.toJson(possibleRoutesMap));
        //SplitTicketHelper splitTicketHelper = new SplitTicketHelper();
        List<SearchParameters> searchParameters3 = splitTicketHelper.createSplitSearchParameters(possibleRoutesMap,searchParameters, null);
        searchParameters2.addAll(searchParameters3);
        searchParametersList.addAll(searchParameters2);
        return searchParametersList;
        /*List<SearchParameters> finalSearchParameters = new ArrayList<>();
        for (SearchParameters sp : searchParametersList) {
            SearchParameters seamenSp = SerializationUtils.clone(sp);
            seamenSp.setBookingType(BookingType.SEAMEN);
            finalSearchParameters.add(seamenSp);

            SearchParameters nonMarineSp = SerializationUtils.clone(sp);
            nonMarineSp.setBookingType(BookingType.NON_MARINE);
            finalSearchParameters.add(nonMarineSp);
        }
        // De-duplicate using redisKey (captures journey, dates, pax, cabin, booking type, etc.)
        Map<String, SearchParameters> uniqueByKey = new LinkedHashMap<>();
        for (SearchParameters sp : finalSearchParameters) {
            uniqueByKey.put(sp.redisKey(), sp);
        }
        return new ArrayList<>(uniqueByKey.values());*/
    }

    public List<SearchParameters> createSearch(SearchParameters searchParameters) throws Exception {
        List<SearchParameters> searchParametersList = new ArrayList<>();
        boolean isSourceDomestic = false;
        Airport airport = Airport.getAirportByIataCode(searchParameters.getJourneyList().get(0).getOrigin());
        boolean isDomestic = isDomesticAirport(airport);
        isDestinationAirportDomestic = isDomesticAirport(Airport.getAirportByIataCode(searchParameters.getJourneyList().get(0).getDestination()));
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
            Map<String, PossibleRoutes> possibleRoutesMap = this.findNextSegmentDepartureDate(responses, true);
            //SplitTicketHelper splitTicketHelper = new SplitTicketHelper();
            List<SearchParameters> searchParameters1 = splitTicketHelper.createSearchParameters(possibleRoutesMap,searchParameters, null);
            searchParametersList.addAll(searchParameters1);
        }
        return searchParametersList;
    }

    private Map<String, PossibleRoutes> findNextSegmentDepartureDate(List<SearchResponse> searchResponses, boolean isSeamenSearch) {
        System.out.println("findNextSegmentDepartureDate");
        Map<String, PossibleRoutes> possibleRoutesMap = new LinkedHashMap<>();
        for (SearchResponse searchResponse:searchResponses) {
            ConcurrentHashMap<Integer, FlightItinerary> nonSeamenHashMap = null;
            if (!isSeamenSearch) {
                nonSeamenHashMap = searchResponse.getAirSolution().getNonSeamenHashMap();
            } else {
                nonSeamenHashMap = searchResponse.getAirSolution().getSeamenHashMap();
            }
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
        return addNonMarineForSeamenAndDedup(searchParametersList);
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
            System.out.println(airport.getTime_zone());
            /*ZonedDateTime zonedDateTime1 = ZonedDateTime.parse(searchParameters.getJourneyList().get(0).getTravelDateStr());
            ZonedDateTime zonedDateTime2 = searchParameters.getJourneyList()
                    .get(0)
                    .getTravelDate()
                    .toInstant()
                    .atZone(ZoneId.of(airport.getTime_zone()));*/
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime localDateTime = LocalDateTime.parse(searchParameters.getJourneyList().get(0).getTravelDateStr(), formatter);
            ZonedDateTime zonedDateTime = localDateTime.atZone(ZoneId.of(airport.getTime_zone()));
            ZonedDateTime midnightTime = zonedDateTime.withHour(0).withMinute(0).withSecond(0).withNano(0).plusHours(24);
            //DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
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
            SearchResponse searchResponse = null;
            List<SearchParameters> searchParameters1 = null;
            //configMap = configurationMasterService.getAllConfigurations(0, 0, "splitTicket");
            //transitEnabled = configMap.get(ConfigMasterConstants.SPLIT_TICKET_TRANSIT_ENABLED) != null && Boolean.parseBoolean(configMap.get(ConfigMasterConstants.SPLIT_TICKET_TRANSIT_ENABLED));
            transitEnabled = Boolean.valueOf(configurationMasterService.getConfig(ConfigMasterConstants.SPLIT_TICKET_TRANSIT_ENABLED.getKey()));
            System.out.println("transitEnabled "+transitEnabled);
            logger.debug("transitEnabled "+transitEnabled);
            if(transitEnabled) {
                List<SearchParameters> searchParametersTransit = null;
                List<SplitTicketTransitAirports> splitTicketTransitAirports = isTransitAdded(searchParameters);
                System.out.println("splitTicketTransitAirports "+splitTicketTransitAirports.size());
                logger.info("splitTicketTransitAirports "+Json.toJson(splitTicketTransitAirports));
                if (splitTicketTransitAirports.size() > 0) {
                    searchParameters1 = createTransitPointSearch(searchParameters, splitTicketTransitAirports);
                } else {
                    searchParameters1 = createSearch(searchParameters);
                }
                System.out.println("searchParameters1 before "+Json.toJson(searchParameters1));
                searchParametersTransit = createNonSeamenSearchParameters(searchParameters1, splitTicketTransitAirports);
                System.out.println("searchParametersTransit before "+Json.toJson(searchParametersTransit));
                if(splitTicketTransitAirports.size()>1) {
                    for (int i=0; i<splitTicketTransitAirports.size()-1; i++) {
                        searchParameters1.add(searchParametersTransit.get(splitTicketTransitAirports.size()+i));
                    }
                } else {
                    searchParameters1.add(searchParametersTransit.get(searchParametersTransit.size()-1));
                }

                //searchParameters1.add(searchParametersTransit.get(1));
                //System.out.println("searchParameters1 searchParameters1 after "+Json.toJson(searchParameters1));
            } else {
                searchParameters1 = createSearch(searchParameters);
            }
            System.out.println("after searchParameters1 "+Json.toJson(searchParameters1));
            logger.debug("Possible search routes "+Json.toJson(searchParameters1));
            createSplitSearch(searchParameters1, searchParameters);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<SearchParameters> createNonSeamenSearchParameters(List<SearchParameters> searchParameters,List<SplitTicketTransitAirports> splitTicketTransitAirports) {
        List<SearchParameters> searchParametersList = new ArrayList<>();
        for (SearchParameters searchParametersItem: searchParameters) {
            SearchParameters searchParameters1 = SerializationUtils.clone(searchParametersItem);
            searchParameters1.setBookingType(BookingType.NON_MARINE);
            if (splitTicketTransitAirports.size()>0) {
                for (SplitTicketTransitAirports splitTicketTransitAirport : splitTicketTransitAirports) {
                    if(splitTicketTransitAirport.getAirline()!=null) {
                        List<String> preferredAirlines = Arrays.asList(splitTicketTransitAirport.getAirline().split(","));
                        searchParameters1.setPreferredAirlinesList(preferredAirlines);
                        //searchParameters1.setPreferredAirlines(splitTicketTransitAirport.getAirline());
                    }
                }
            }
            searchParametersList.add(searchParameters1);
        }
        return searchParametersList;
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
        List<SplitTicketTransitAirports> splitTicketTransitAirports = null;
        String toLocation = searchParameters.getJourneyList().get(searchParameters.getJourneyList().size()-1).getDestination();
        String fromLocation = searchParameters.getJourneyList().get(0).getOrigin();
        splitTicketTransitAirports = SplitTicketTransitAirports.getAllTransitByIata(toLocation);
        if (splitTicketTransitAirports == null || splitTicketTransitAirports.size() == 0) {
            splitTicketTransitAirports = SplitTicketTransitAirports.getAllTransitByIata(fromLocation);
        }
        return splitTicketTransitAirports;
    }

    public void createSplitSearch(List<SearchParameters> searchParameters, SearchParameters originalSearchRequest) throws Exception {
        searchParameters = addNonMarineForSeamenAndDedup(searchParameters);
        splitAmadeusSearch.splitTicketSearch(searchParameters, originalSearchRequest, isSourceAirportDomestic,isDestinationAirportDomestic);
    }

    // Adds a NON_MARINE variant for every SEAMEN search parameter and removes duplicates (by redisKey)
    private List<SearchParameters> addNonMarineForSeamenAndDedup(List<SearchParameters> searchParameters) {
        if (searchParameters == null || searchParameters.isEmpty()) {
            return searchParameters;
        }
        List<SearchParameters> expanded = new ArrayList<>(searchParameters.size() * 2);
        for (SearchParameters sp : searchParameters) {
            expanded.add(sp);
            if (sp != null && sp.getBookingType() == BookingType.SEAMEN) {
                SearchParameters nm = SerializationUtils.clone(sp);
                nm.setBookingType(BookingType.NON_MARINE);
                expanded.add(nm);
            }
        }
        Map<String, SearchParameters> unique = new LinkedHashMap<>();
        for (SearchParameters sp : expanded) {
            if (sp != null) {
                unique.put(sp.redisKey(), sp);
            }
        }
        return new ArrayList<>(unique.values());
    }

}
