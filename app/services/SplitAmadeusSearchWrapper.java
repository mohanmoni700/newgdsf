package services;

import com.amadeus.xml.fmptbr_14_2_1a.*;
import com.compassites.GDSWrapper.amadeus.ServiceHandler;
import com.compassites.constants.CacheConstants;
import com.compassites.exceptions.IncompleteDetailsMessage;
import com.compassites.exceptions.RetryException;
import com.compassites.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.xml.ws.client.ClientTransportException;
import com.sun.xml.ws.fault.ServerSOAPFaultException;
import com.thoughtworks.xstream.XStream;
import ennum.ConfigMasterConstants;
import models.Airline;
import models.Airport;
import models.AmadeusSessionWrapper;
import models.FlightSearchOffice;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Minutes;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import play.libs.Json;
import utils.AdvancedSplitTicketMerger;
import utils.AmadeusSessionManager;
import utils.ErrorMessageHelper;
import utils.SplitTicketMerger;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
public class SplitAmadeusSearchWrapper implements SplitAmadeusSearch {

    static org.slf4j.Logger logger = LoggerFactory.getLogger("gds");

    static org.slf4j.Logger amadeusLogger = LoggerFactory.getLogger("amadeus");

    @Autowired
    private ServiceHandler serviceHandler;

    @Autowired
    private AmadeusSessionManager amadeusSessionManager;

    @Autowired
    private AmadeusSourceOfficeService sourceOfficeService;

    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private ConfigurationMasterService configurationMasterService;
    @Autowired
    private SplitTicketMerger splitTicketMerger;
    @Autowired
    private AdvancedSplitTicketMerger advancedSplitTicketMerger;

    public RedisTemplate getRedisTemplate() {
        return redisTemplate;
    }

    public void setRedisTemplate(RedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Autowired
    @Qualifier("splitTicketAmadeusSearch")
    private SplitTicketSearch splitTicketSearch;

    @Autowired
    private SplitTicketIndigoSearch ticketIndigoSearch;
  
    //private String searchOfficeID = play.Play.application().configuration().getString("split.ticket.officeId");
    private static String searchOfficeID = "";
    /*SplitAmadeusSearchWrapper() {
        searchOfficeID = configurationMasterService.getConfig(ConfigMasterConstants.SPLIT_TICKET_AMADEUS_OFFICE_ID_GLOBAL.getKey());
    }*/
    public List<SearchResponse> splitSearch(List<SearchParameters> searchParameters, ConcurrentHashMap<String,List<FlightItinerary>> concurrentHashMap, boolean isDomestic) throws Exception {
        List<SearchResponse> responses = new ArrayList<>();
        searchOfficeID = configurationMasterService.getConfig(ConfigMasterConstants.SPLIT_TICKET_AMADEUS_OFFICE_ID_GLOBAL.getKey());
        logger.info("Starting splitSearch with " + searchParameters.size() + " search parameters, isDomestic: " + isDomestic);
        for (SearchParameters searchParameters1: searchParameters)  {
            FlightSearchOffice searchOffice = new FlightSearchOffice();
            searchOffice.setOfficeId(searchOfficeID);
            searchOffice.setName("");
            SearchResponse searchResponse = null;
            if (isDomestic) {
                searchResponse = this.findNextSegmentDeparture(searchParameters1, searchOffice);
            } else {
                searchResponse = this.search(searchParameters1, searchOffice);
            }
            String origin = searchParameters1.getJourneyList().get(0).getOrigin();
            String provider = searchResponse.getProvider();
            int seamenCount = searchResponse.getAirSolution().getSeamenHashMap().values().size();
            int nonSeamenCount = searchResponse.getAirSolution().getNonSeamenHashMap().values().size();
            
            logger.info("Processing " + provider + " search for origin: " + origin + " - Seamen: " + seamenCount + ", Non-Seamen: " + nonSeamenCount);
            
            if (concurrentHashMap.containsKey(origin)) {
                concurrentHashMap.get(origin).addAll(new ArrayList<FlightItinerary>(searchResponse.getAirSolution().getSeamenHashMap().values()));
                concurrentHashMap.get(origin).addAll(new ArrayList<FlightItinerary>(searchResponse.getAirSolution().getNonSeamenHashMap().values()));
                System.out.println("Size of non seamen if "+nonSeamenCount);
                logger.debug("Size of non seamen if "+nonSeamenCount);
            } else {
                List<FlightItinerary> newFlightList = new ArrayList<>();
                newFlightList.addAll(new ArrayList<FlightItinerary>(searchResponse.getAirSolution().getSeamenHashMap().values()));
                newFlightList.addAll(new ArrayList<FlightItinerary>(searchResponse.getAirSolution().getNonSeamenHashMap().values()));
                concurrentHashMap.put(origin, newFlightList);
                System.out.println("Size of non seamen else "+nonSeamenCount);
                logger.debug("Size of non seamen else "+nonSeamenCount);
            }
            responses.add(searchResponse);
        }
        for (Map.Entry<String, List<FlightItinerary>> flightItineraryEntry : concurrentHashMap.entrySet()) {
            logger.debug("flightItineraryEntry size: "+flightItineraryEntry.getKey()+"  -  "+flightItineraryEntry.getValue().size());
            System.out.println("flightItineraryEntry size: "+flightItineraryEntry.getKey()+"  -  "+flightItineraryEntry.getValue().size());
            if(flightItineraryEntry.getValue().size() == 0) {
                concurrentHashMap.remove(flightItineraryEntry.getKey());
            }
        }
        System.out.println("responses "+responses.size());
        logger.debug("responses "+responses.size());
        logger.info("Completed splitSearch - Total origins in concurrentHashMap: " + concurrentHashMap.size());
        return responses;
    }


    public void splitTicketSearch(List<SearchParameters> searchParameters, SearchParameters originalSearchRequest, boolean isSourceAirportDomestic, boolean isDestinationAirportDomestic) throws Exception {
        final String redisKey = originalSearchRequest.redisKey();
        try {
            System.out.println("searchParameters "+Json.toJson(searchParameters));
            ConcurrentHashMap<String, List<FlightItinerary>> concurrentHashMap = new ConcurrentHashMap<>();
            ConcurrentHashMap<String, List<FlightItinerary>> indigoConcurrentHashMap = new ConcurrentHashMap<>();
            String fromLocation = originalSearchRequest.getJourneyList().get(0).getOrigin();
            String toLocation = searchParameters.get(searchParameters.size()-1).getJourneyList().get(0).getDestination();
            List<SearchResponse> searchResponses = null;
            List<Future<List<SearchResponse>>> futureSearchResponseList = new ArrayList<>();
            List<ErrorMessage> errorMessageList = new ArrayList<>();
            List<FlightItinerary> flightItineraries = null;
            List<SearchResponse> allSearchResponses = new ArrayList<>(); // To collect all results
            logger.debug("\n\n***********SEARCH STARTED key: [" + redisKey + "]***********");
            int queueSize = 10;
            redisTemplate.opsForValue().set(redisKey + ":status", "started");
            redisTemplate.expire(redisKey, CacheConstants.CACHE_TIMEOUT_IN_SECS, TimeUnit.SECONDS);
            ThreadPoolExecutor newExecutor = new ThreadPoolExecutor(4, 10, Long.MAX_VALUE, TimeUnit.NANOSECONDS, new ArrayBlockingQueue<Runnable>(queueSize, true));

                futureSearchResponseList.add(newExecutor.submit(new Callable<List<SearchResponse>>() {
                    @Override
                    public List<SearchResponse> call() throws Exception {
                        return splitTicketSearch.splitSearch(searchParameters,concurrentHashMap,false);
                    }
                }));
            futureSearchResponseList.add(newExecutor.submit(new Callable<List<SearchResponse>>() {
                @Override
                public List<SearchResponse> call() throws Exception {
                    return ticketIndigoSearch.splitSearch(searchParameters,concurrentHashMap,false);
                }
            }));

            /********** Multi threaded search for single office id **********/
            logger.debug("["+redisKey+"] : " + futureSearchResponseList.size()+ "Threads initiated");
            int counter = 0;
            boolean loop = true;
            int searchResponseListSize = futureSearchResponseList.size();
            int exceptionCounter = 0;

            while (loop) {
                ListIterator<Future<List<SearchResponse>>> listIterator = futureSearchResponseList.listIterator();
                while (listIterator.hasNext()) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Future<List<SearchResponse>> future = listIterator.next();
                    List<SearchResponse> searchResponses1 = null;
                    if(future.isDone()) {
                        listIterator.remove();
                        counter++;
                        System.out.println("counter "+counter);
                        try {
                            searchResponses1 = future.get();
                            logger.debug("result size "+searchResponses1.size());
                        } catch (RetryException retryOnFailure) {
                            exceptionCounter++;
                            logger.error("retrialError in FlightSearchWrapper : ", retryOnFailure);
                            ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage("retrialError",ErrorMessage.ErrorType.ERROR,"Application");
                            errorMessageList.add(errorMessage);
                        } catch (Exception e){
                            logger.error("Exception in FlightSearchWrapper : ", e);
                            ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage("partialResults",ErrorMessage.ErrorType.WARNING,"Application");
                            errorMessageList.add(errorMessage);
                            e.printStackTrace();
                        }

                        if(searchResponses1 != null && searchResponses1.size() !=0) {
                            // Collect all search responses for later processing
                            allSearchResponses.addAll(searchResponses1);

                        } else {
                            logger.debug("["+redisKey+"]Received Response "+ counter +" Null" );
                        }
                    }
                }
                newExecutor.shutdown();
                if(counter == searchResponseListSize){
                    Integer timeout = CacheConstants.CACHE_TIMEOUT_IN_SECS;
                    if(errorMessageList.size() > 0){
                        timeout = CacheConstants.CACHE_TIMEOUT_FOR_ERROR_IN_SECS;
                    }
                    // Process all collected results now that all futures are complete
                    if(!allSearchResponses.isEmpty()) {
                        /******* new merge logic for split ticket *******/
                        SplitTicketMerger splitTicketMerger = new SplitTicketMerger();
                        SearchResponse searchResponseCache = new SearchResponse();
                        FlightSearchOffice searchOffice = new FlightSearchOffice();
                        searchOffice.setOfficeId(searchOfficeID);
                        searchOffice.setName("");
                        searchResponseCache.setFlightSearchOffice(searchOffice);
                        searchResponseCache.setProvider("Combined"); // Indicate this is a combined result
                        
                        for (Map.Entry<String, List<FlightItinerary>> flightItineraryEntry : concurrentHashMap.entrySet()) {
                            logger.debug("flightItineraryEntry size: "+flightItineraryEntry.getKey()+"  -  "+flightItineraryEntry.getValue().size());
                            System.out.println("flightItineraryEntry size: "+flightItineraryEntry.getKey()+"  -  "+flightItineraryEntry.getValue().size());
                            /*for (FlightItinerary flightItinerary : flightItineraryEntry.getValue()) {
                                for (Journey journey: flightItinerary.getJourneyList()) {
                                   // logger.debug(journey.getAirSegmentList().get(0).getFromLocation()+"  - "+ journey.getAirSegmentList().get(0).getAirline().getIataCode()+" - "+journey.getAirSegmentList().get(journey.getAirSegmentList().size()-1).getToLocation()+" - "+journey.getAirSegmentList().get(journey.getAirSegmentList().size()-1).getAirline().getIataCode());
                                    //System.out.print(journey.getAirSegmentList().get(0).getFromLocation()+"  - "+ journey.getAirSegmentList().get(0).getAirline().getIataCode());
                                    System.out.print(" - ");
                                    //System.out.println(journey.getAirSegmentList().get(journey.getAirSegmentList().size()-1).getToLocation()+" - "+journey.getAirSegmentList().get(journey.getAirSegmentList().size()-1).getAirline().getIataCode());
                                }
                            }*/
                            if(flightItineraryEntry.getValue().size() == 0) {
                                concurrentHashMap.remove(flightItineraryEntry.getKey());
                            }
                        }
                        
                        System.out.println(fromLocation+"  -  "+toLocation);
                        logger.info("Final concurrentHashMap state - Total origins: " + concurrentHashMap.size());
                        for (Map.Entry<String, List<FlightItinerary>> entry : concurrentHashMap.entrySet()) {
                            logger.info("Origin: " + entry.getKey() + " - Flight count: " + entry.getValue().size());
                        }
                        ObjectMapper mapper = new ObjectMapper();
                        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(concurrentHashMap);
                        
                        // Log the map before sorting
                        //sorting the flight itineraries by first journey stops
                        //flightItineraries = splitTicketMerger.connectingSegments(fromLocation, toLocation, sortMapByFirstJourneyStops(concurrentHashMap), isSourceAirportDomestic);
                        //flightItineraries = splitTicketMerger.mergingSplitTicket(fromLocation, toLocation, sortMapByFirstJourneyStops(concurrentHashMap), isSourceAirportDomestic, isDestinationAirportDomestic);
                        flightItineraries = advancedSplitTicketMerger.mergeAllSplitTicketCombinations(fromLocation, toLocation, sortMapByFirstJourneyStops(concurrentHashMap), isSourceAirportDomestic, isDestinationAirportDomestic);
                        //flightItineraries = splitTicketMerger.mergingSplitTicket(fromLocation, toLocation, concurrentHashMap, isSourceAirportDomestic);
                        logger.info("Combined Split Search Result " + Json.toJson(flightItineraries));
                        if (flightItineraries == null || flightItineraries.isEmpty()) {
                            int firstLegCount = concurrentHashMap.get(fromLocation) != null ? concurrentHashMap.get(fromLocation).size() : 0;
                            int secondLegCount = concurrentHashMap.get(toLocation) != null ? concurrentHashMap.get(toLocation).size() : 0;
                            logger.warn("[split] No split results for route: " + fromLocation + " -> " + toLocation);
                            logger.warn("[split] First-leg options from " + fromLocation + ": " + firstLegCount + ", second-leg options from " + toLocation + ": " + secondLegCount);
                            logger.warn("[split] Intermediates considered: " + (Math.max(concurrentHashMap.keySet().size() - 2, 0)) + " -> " + concurrentHashMap.keySet());
                        }
                        AirSolution airSolution = new AirSolution();
                        airSolution.setReIssueSearch(false);
                        // Deduplicate itineraries by route+time signature, keep the cheaper option when duplicates found
                        if (flightItineraries != null && !flightItineraries.isEmpty()) {
                            Map<String, FlightItinerary> unique = new LinkedHashMap<>();
                            for (FlightItinerary fi : flightItineraries) {
                                if (fi == null) continue;
                                String key = buildItinerarySignature(fi);
                                if (!unique.containsKey(key)) {
                                    unique.put(key, fi);
                                } else {
                                    FlightItinerary existing = unique.get(key);
                                    if (isCheaper(fi, existing)) {
                                        unique.put(key, fi);
                                    }
                                }
                            }
                            flightItineraries = new ArrayList<>(unique.values());
                        }
                        airSolution.setFlightItineraryList(flightItineraries);
                        searchResponseCache.setAirSolution(airSolution);
                        searchResponseCache.setReIssueSearch(false);
                        
                        // Combine error messages from all search responses
                        for (SearchResponse searchResponse : allSearchResponses) {
                            searchResponseCache.getErrorMessageList().addAll(searchResponse.getErrorMessageList());
                        }
                        
                        redisTemplate.opsForValue().set(redisKey, Json.stringify(Json.toJson(searchResponseCache)));
                        redisTemplate.expire(redisKey, CacheConstants.CACHE_TIMEOUT_IN_SECS, TimeUnit.SECONDS);
                        redisTemplate.opsForValue().set(redisKey + ":status", "complete");
                        redisTemplate.expire(redisKey + ":status", CacheConstants.CACHE_TIMEOUT_IN_SECS, TimeUnit.SECONDS);
                    }

                    loop = false;
                    if((flightItineraries == null || flightItineraries.size() == 0) && errorMessageList.size() > 0)  {
                        SearchResponse searchResponse = new SearchResponse();
                        searchResponse.setErrorMessageList(errorMessageList);
                        redisTemplate.opsForValue().set(originalSearchRequest.redisKey(), Json.stringify(Json.toJson(searchResponse)));
                        redisTemplate.expire(originalSearchRequest.redisKey(),timeout,TimeUnit.SECONDS);
                    }
                    redisTemplate.opsForValue().set(originalSearchRequest.redisKey()+":status", "complete");
                    redisTemplate.expire(originalSearchRequest.redisKey()+":status",timeout,TimeUnit.SECONDS);
                    logger.debug("***********SEARCH END key: ["+ redisKey +"]***********");
                }
            }
            logger.debug("=================================>\n"+
                    "=================================>\n"+
                    String.format("[monitor] [%d/%d] Active: %d, Completed: %d, Task: %d, isShutdown: %s, isTerminated: %s",
                            newExecutor.getPoolSize(),
                            newExecutor.getCorePoolSize(),
                            newExecutor.getActiveCount(),
                            newExecutor.getCompletedTaskCount(),
                            newExecutor.getTaskCount(),
                            newExecutor.isShutdown(),
                            newExecutor.isTerminated()));

        }  catch (Exception e) {
            checkResponseAndSetStatus(null, redisKey);
            e.printStackTrace();
        }
    }

    public void splitTicketSearchNew(List<SearchParameters> searchParameters, SearchParameters originalSearchRequest, boolean isSourceAirportDomestic, boolean isDestinationAirportDomestic) throws Exception {
        final String redisKey = originalSearchRequest.redisKey();
        try {
            System.out.println("searchParameters " + Json.toJson(searchParameters));
            ConcurrentHashMap<String, List<FlightItinerary>> concurrentHashMap = new ConcurrentHashMap<>();
            String fromLocation = originalSearchRequest.getJourneyList().get(0).getOrigin();
            String toLocation = searchParameters.get(searchParameters.size() - 1).getJourneyList().get(0).getDestination();
            List<ErrorMessage> errorMessageList = new ArrayList<>();
            List<FlightItinerary> flightItineraries = null;

            logger.debug("\n\n***********SEARCH STARTED key: [" + redisKey + "]***********");

            // Use a fixed thread pool for better scalability
            int threadPoolSize = Math.min(10, searchParameters.size());
            ExecutorService executorService = Executors.newFixedThreadPool(threadPoolSize);

            List<Future<List<SearchResponse>>> futureSearchResponseList = new ArrayList<>();
            for (SearchParameters param : searchParameters) {
                futureSearchResponseList.add(executorService.submit(() -> splitSearch(Collections.singletonList(param), concurrentHashMap, false)));
            }

            // Process futures
            List<SearchResponse> searchResponses = new ArrayList<>();
            for (Future<List<SearchResponse>> future : futureSearchResponseList) {
                try {
                    searchResponses.addAll(future.get(60, TimeUnit.SECONDS)); // Timeout to prevent indefinite blocking
                } catch (TimeoutException e) {
                    logger.error("Task timed out", e);
                    errorMessageList.add(ErrorMessageHelper.createErrorMessage("timeout", ErrorMessage.ErrorType.ERROR, "Application"));
                } catch (Exception e) {
                    logger.error("Exception in task execution", e);
                    errorMessageList.add(ErrorMessageHelper.createErrorMessage("partialResults", ErrorMessage.ErrorType.WARNING, "Application"));
                }
            }

            executorService.shutdown();
            executorService.awaitTermination(1, TimeUnit.MINUTES);

            // Merge results
            if (!searchResponses.isEmpty()) {
                flightItineraries = splitTicketMerger.mergingSplitTicket(fromLocation, toLocation, sortMapByFirstJourneyStops(concurrentHashMap), isSourceAirportDomestic, isDestinationAirportDomestic);

                AirSolution airSolution = new AirSolution();
                airSolution.setReIssueSearch(false);
                airSolution.setFlightItineraryList(flightItineraries);

                SearchResponse searchResponseCache = new SearchResponse();
                searchResponseCache.setAirSolution(airSolution);
                searchResponseCache.setReIssueSearch(false);
                redisTemplate.opsForValue().set(redisKey, Json.stringify(Json.toJson(searchResponseCache)));
                redisTemplate.expire(redisKey, CacheConstants.CACHE_TIMEOUT_IN_SECS, TimeUnit.SECONDS);
            }

            // Handle errors
            if (flightItineraries == null || flightItineraries.isEmpty()) {
                SearchResponse errorResponse = new SearchResponse();
                errorResponse.setErrorMessageList(errorMessageList);
                redisTemplate.opsForValue().set(redisKey, Json.stringify(Json.toJson(errorResponse)));
                redisTemplate.expire(redisKey, CacheConstants.CACHE_TIMEOUT_FOR_ERROR_IN_SECS, TimeUnit.SECONDS);
            }

            redisTemplate.opsForValue().set(redisKey + ":status", "complete");
            redisTemplate.expire(redisKey + ":status", CacheConstants.CACHE_TIMEOUT_IN_SECS, TimeUnit.SECONDS);
            logger.debug("***********SEARCH END key: [" + redisKey + "]***********");
        } catch (Exception e) {
            logger.error("Exception in splitTicketSearch", e);
            throw e;
        }
    }

    public static ConcurrentHashMap<String, List<FlightItinerary>> sortMapByFirstJourneyStops(
            ConcurrentHashMap<String, List<FlightItinerary>> flightMap) {
        logger.debug("Sorting flightMap by first journey stops...");
        ConcurrentHashMap<String, List<FlightItinerary>> sortedMap = new ConcurrentHashMap<>();

        for (Map.Entry<String, List<FlightItinerary>> entry : flightMap.entrySet()) {
            String key = entry.getKey();
            List<FlightItinerary> itineraries = entry.getValue();

            List<FlightItinerary> sortedList = itineraries.stream()
                    .sorted(Comparator.comparingInt(itinerary -> {
                        List<Journey> journeys = itinerary.getJourneyList();
                        return (journeys != null && !journeys.isEmpty()) ? journeys.get(0).getNoOfStops() : Integer.MAX_VALUE;
                    }))
                    .collect(Collectors.toList());

            sortedMap.put(key, sortedList);
        }

        return sortedMap;
    }


    private boolean validResponse(SearchResponse response){
        if ((response != null) && (response.getAirSolution() != null)){
            if (response.getAirSolution().getFlightItineraryList() != null){
                return (response.getAirSolution().getFlightItineraryList().size() > 0);
            }
        }
        return false;
    }

    private boolean checkResponseAndSetStatus(SearchResponse response, String providerStatusCacheKey) {
        String status = "invalid";
        if (validResponse(response))
            status = "success";
        setCacheValue(providerStatusCacheKey, status);
        return status=="success";
    }

    private Boolean checkOrSetStatus(String key){
        String status = (String) redisTemplate.opsForValue().get(key);
        if (status != null){
            return status.equalsIgnoreCase("success");
        }

        setCacheValue(key, "in-progress");
        return false;
    }

    private void setCacheValue(String key, String value){
        redisTemplate.opsForValue().set( key, value );
        redisTemplate.expire( key,CacheConstants.CACHE_TIMEOUT_IN_SECS,TimeUnit.SECONDS );
    }


    public SearchResponse findNextSegmentDeparture(SearchParameters searchParameters, FlightSearchOffice office) throws Exception {
        logger.debug("#####################AmadeusFlightSearch started  : ");
        logger.debug("#####################SearchParameters: \n"+ Json.toJson(searchParameters));
        SearchResponse searchResponse = new SearchResponse();
        AmadeusSessionWrapper amadeusSessionWrapper = null;
        searchResponse.setProvider("Amadeus");
        searchResponse.setFlightSearchOffice(office);
        String from = searchParameters.getJourneyList().get(0).getOrigin();
        String  to = searchParameters.getJourneyList().get(searchParameters.getJourneyList().size()-1).getDestination();
        searchResponse.setAirSegmentKey(from+to);
        FareMasterPricerTravelBoardSearchReply fareMasterPricerTravelBoardSearchReply = null;
        FareMasterPricerTravelBoardSearchReply seamenReply = null;

        try {
            long startTime = System.currentTimeMillis();
            amadeusSessionWrapper = amadeusSessionManager.getSession(office);
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            logger.debug("...................................Amadeus Search Session used: " + Json.toJson(amadeusSessionWrapper.getmSession().value));
            logger.debug("Execution time in getting session:: " + duration/1000 + " seconds");//to be removed
            if (searchParameters.getBookingType() == BookingType.SEAMEN) {
                seamenReply = serviceHandler.searchSplitAirlines(searchParameters, amadeusSessionWrapper,true);
                amadeusLogger.debug("AmadeusSearchRes "+ new Date()+" ------->>"+ new XStream().toXML(seamenReply));
            } else {
                fareMasterPricerTravelBoardSearchReply = serviceHandler.searchSplitAirlines(searchParameters, amadeusSessionWrapper,true);
                amadeusLogger.debug("AmadeusSearchRes "+ new Date()+" ------->>"+ new XStream().toXML(fareMasterPricerTravelBoardSearchReply));
            }
        } catch (ServerSOAPFaultException soapFaultException) {

            soapFaultException.printStackTrace();
            throw new IncompleteDetailsMessage(soapFaultException.getMessage(), soapFaultException.getCause());
        } catch (ClientTransportException clientTransportException) {
            //throw new IncompleteDetailsMessage(soapFaultException.getMessage(), soapFaultException.getCause());
            ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage("partialResults", ErrorMessage.ErrorType.ERROR, "Amadeus");
            searchResponse.getErrorMessageList().add(errorMessage);
            return searchResponse;
        } catch (Exception e) {
            e.printStackTrace();
            ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage("partialResults", ErrorMessage.ErrorType.ERROR, "Amadeus");
            searchResponse.getErrorMessageList().add(errorMessage);
            return searchResponse;
        }finally {
            amadeusSessionManager.updateAmadeusSession(amadeusSessionWrapper);
        }

        FareMasterPricerTravelBoardSearchReply.ErrorMessage seamenErrorMessage = null;
         FareMasterPricerTravelBoardSearchReply.ErrorMessage errorMessage = null;
        if (seamenReply != null) {
            seamenErrorMessage = seamenReply.getErrorMessage();
            if(seamenErrorMessage != null)
                logger.debug("seamenErrorMessage :" + seamenErrorMessage.getErrorMessageText().getDescription() + "  officeId:"+ office.getOfficeId());
        }

        if(fareMasterPricerTravelBoardSearchReply !=null) {
            errorMessage = fareMasterPricerTravelBoardSearchReply.getErrorMessage();
        }


        AirSolution airSolution = new AirSolution();
        logger.debug("#####################errorMessage is null");
        if (searchParameters.getBookingType() == BookingType.SEAMEN && seamenErrorMessage == null) {
            airSolution.setSeamenHashMap(getFlightItineraryHashmap(seamenReply,office, true));
        }

        if (searchParameters.getBookingType() == BookingType.NON_MARINE && errorMessage == null) {
            airSolution.setNonSeamenHashMap(getFlightItineraryHashmap(fareMasterPricerTravelBoardSearchReply, office, false));
        }
        searchResponse.setAirSolution(airSolution);
        searchResponse.setProvider(provider());
        searchResponse.setFlightSearchOffice(office);
        return searchResponse;
    }

    @RetryOnFailure(attempts = 2, delay = 2000, exception = RetryException.class)
    public SearchResponse search(SearchParameters searchParameters, FlightSearchOffice office) throws Exception {
        logger.debug("#####################AmadeusFlightSearch started  : ");
        logger.debug("#####################SearchParameters: \n"+ Json.toJson(searchParameters));
        SearchResponse searchResponse = new SearchResponse();
        AmadeusSessionWrapper amadeusSessionWrapper = null;
        searchResponse.setProvider("Amadeus");
        searchResponse.setFlightSearchOffice(office);
        String from = searchParameters.getJourneyList().get(0).getOrigin();
        String  to = searchParameters.getJourneyList().get(searchParameters.getJourneyList().size()-1).getDestination();
        searchResponse.setAirSegmentKey(from+to);
        FareMasterPricerTravelBoardSearchReply fareMasterPricerTravelBoardSearchReply = null;
        FareMasterPricerTravelBoardSearchReply seamenReply = null;

        try {
            long startTime = System.currentTimeMillis();
            amadeusSessionWrapper = amadeusSessionManager.getSession(office);
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            logger.debug("...................................Amadeus Search Session used: " + Json.toJson(amadeusSessionWrapper.getmSession().value));
            logger.debug("Execution time in getting session:: " + duration/1000 + " seconds");//to be removed
            if (searchParameters.getBookingType() == BookingType.SEAMEN) {
                seamenReply = serviceHandler.searchSplitAirlines(searchParameters, amadeusSessionWrapper,false);
                amadeusLogger.debug("AmadeusSearchRes "+ new Date()+" ------->>"+ new XStream().toXML(seamenReply));
            } else {
                fareMasterPricerTravelBoardSearchReply = serviceHandler.searchSplitAirlines(searchParameters, amadeusSessionWrapper,false);
                amadeusLogger.debug("AmadeusSearchRes "+ new Date()+" ------->>"+ new XStream().toXML(fareMasterPricerTravelBoardSearchReply));
            }
        } catch (ServerSOAPFaultException soapFaultException) {

            soapFaultException.printStackTrace();
            throw new IncompleteDetailsMessage(soapFaultException.getMessage(), soapFaultException.getCause());
        } catch (ClientTransportException clientTransportException) {
            //throw new IncompleteDetailsMessage(soapFaultException.getMessage(), soapFaultException.getCause());
            ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage("partialResults", ErrorMessage.ErrorType.ERROR, "Amadeus");
            searchResponse.getErrorMessageList().add(errorMessage);
            return searchResponse;
        } catch (Exception e) {
            e.printStackTrace();
            ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage("partialResults", ErrorMessage.ErrorType.ERROR, "Amadeus");
            searchResponse.getErrorMessageList().add(errorMessage);
            return searchResponse;
        }finally {
            amadeusSessionManager.updateAmadeusSession(amadeusSessionWrapper);
        }

        FareMasterPricerTravelBoardSearchReply.ErrorMessage seamenErrorMessage = null;
        FareMasterPricerTravelBoardSearchReply.ErrorMessage errorMessage = null;
        if (seamenReply != null) {
            seamenErrorMessage = seamenReply.getErrorMessage();
            if(seamenErrorMessage != null)
                logger.debug("seamenErrorMessage :" + seamenErrorMessage.getErrorMessageText().getDescription() + "  officeId:"+ office.getOfficeId());
        }

        if (fareMasterPricerTravelBoardSearchReply != null) {
            errorMessage = fareMasterPricerTravelBoardSearchReply.getErrorMessage();
        }

        AirSolution airSolution = new AirSolution();
        logger.debug("#####################errorMessage is null");
        if (searchParameters.getBookingType() == BookingType.SEAMEN && seamenErrorMessage == null) {
            airSolution.setSeamenHashMap(getFlightItineraryHashmap(seamenReply,office, true));
        }

        if(searchParameters.getBookingType() == BookingType.NON_MARINE && errorMessage == null) {
            airSolution.setNonSeamenHashMap(getFlightItineraryHashmap(fareMasterPricerTravelBoardSearchReply, office,false));
        }
        searchResponse.setAirSolution(airSolution);
        searchResponse.setProvider(provider());
        searchResponse.setFlightSearchOffice(office);
        return searchResponse;
    }

    public String provider() {
        return "Amadeus";
    }

    private String buildItinerarySignature(FlightItinerary itinerary) {
        if (itinerary == null || itinerary.getJourneyList() == null || itinerary.getJourneyList().isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Journey j : itinerary.getJourneyList()) {
            if (j == null || j.getAirSegmentList() == null || j.getAirSegmentList().isEmpty()) continue;
            AirSegmentInformation first = j.getAirSegmentList().get(0);
            AirSegmentInformation last = j.getAirSegmentList().get(j.getAirSegmentList().size()-1);
            sb.append(first.getFromLocation()).append("-")
              .append(last.getToLocation()).append("|")
              .append(first.getDepartureTime()).append("->")
              .append(last.getArrivalTime()).append("|");
        }
        return sb.toString();
    }

    private boolean isCheaper(FlightItinerary a, FlightItinerary b) {
        try {
            if (a == null) return false;
            if (b == null) return true;
            PricingInformation pa = a.getPricingInformation();
            PricingInformation pb = b.getPricingInformation();
            if (pa == null || pb == null) return false;
            if (pa.getTotalPriceValue() != null && pb.getTotalPriceValue() != null) {
                return pa.getTotalPriceValue().compareTo(pb.getTotalPriceValue()) < 0;
            }
            if (pa.getTotalPrice() != null && pb.getTotalPrice() != null) {
                return pa.getTotalPrice().compareTo(pb.getTotalPrice()) < 0;
            }
        } catch (Exception ignored) {}
        return false;
    }

    public List<FlightSearchOffice> getOfficeList() {
        return sourceOfficeService.getAllOffices();
    }

    private ConcurrentHashMap<Integer, FlightItinerary> getFlightItineraryHashmap(FareMasterPricerTravelBoardSearchReply fareMasterPricerTravelBoardSearchReply, FlightSearchOffice office, boolean isSeamen) {
        ConcurrentHashMap<Integer, FlightItinerary> flightItineraryHashMap = new ConcurrentHashMap<>();
        try{
            String currency = fareMasterPricerTravelBoardSearchReply.getConversionRate().getConversionRateDetail().get(0).getCurrency();
            List<FareMasterPricerTravelBoardSearchReply.FlightIndex> flightIndexList = fareMasterPricerTravelBoardSearchReply.getFlightIndex();
            for (FareMasterPricerTravelBoardSearchReply.Recommendation recommendation : fareMasterPricerTravelBoardSearchReply.getRecommendation()) {
                for (ReferenceInfoType segmentRef : recommendation.getSegmentFlightRef()) {
                    FlightItinerary flightItinerary = new FlightItinerary();
                    flightItinerary.setPassportMandatory(false);
                    flightItinerary.setPricingInformation(getPricingInformation(recommendation));
                    flightItinerary.getPricingInformation().setGdsCurrency(currency);
                    flightItinerary.getPricingInformation().setPricingOfficeId(office.getOfficeId());
                    List<String> contextList = getAvailabilityCtx(segmentRef, recommendation.getSpecificRecDetails());
                    flightItinerary = createJourneyInformation(segmentRef, flightItinerary, flightIndexList, recommendation, contextList,isSeamen);
                    flightItinerary.getPricingInformation().setPaxFareDetailsList(createFareDetails(recommendation, flightItinerary.getJourneyList()));
                    flightItineraryHashMap.put(flightItinerary.hashCode(), flightItinerary);
                }
            }
            return flightItineraryHashMap;
        }catch (Exception e){
            logger.debug("error in getFlightItineraryHashmap :"+ e.getMessage());
        }
        return flightItineraryHashMap;
    }

    private FlightItinerary createJourneyInformation(ReferenceInfoType segmentRef, FlightItinerary flightItinerary, List<FareMasterPricerTravelBoardSearchReply.FlightIndex> flightIndexList, FareMasterPricerTravelBoardSearchReply.Recommendation recommendation, List<String> contextList,boolean isSeamen){
        int flightIndexNumber = 0;
        int segmentIndex = 0;
        for(ReferencingDetailsType191583C referencingDetailsType : segmentRef.getReferencingDetail()) {
            //0 is for forward journey and refQualifier should be S for segment
            if (referencingDetailsType.getRefQualifier().equalsIgnoreCase("S") ) {
                Journey journey = new Journey();
                journey = setJourney(journey, flightIndexList.get(flightIndexNumber).getGroupOfFlights().get(referencingDetailsType.getRefNumber().intValue()-1),recommendation);
                journey.setSeamen(isSeamen);
                if(contextList.size() > 0 ){
                    setContextInformation(contextList, journey, segmentIndex);
                }
                flightItinerary.setFromLocation(journey.getFromLocation());
                flightItinerary.setToLocation(journey.getToLocation());
                flightItinerary.getJourneyList().add(journey);
                flightItinerary.getNonSeamenJourneyList().add(journey);
                ++flightIndexNumber;
            }


        }
        return flightItinerary;
    }


    private  void setContextInformation(List<String> contextList, Journey journey, int segmentIndex){
        for (AirSegmentInformation airSegmentInformation : journey.getAirSegmentList()){
            airSegmentInformation.setContextType(contextList.get(segmentIndex));
            segmentIndex = segmentIndex + 1;
        }
    }
    private Duration setTravelDuraion(String totalElapsedTime){
        String strHours = totalElapsedTime.substring(0, 2);
        String strMinutes = totalElapsedTime.substring(2);
        Duration duration = null;
        Integer hours = new Integer(strHours);
        int days = hours / 24;
        int dayHours = hours - (days * 24);
        try {
            duration = DatatypeFactory.newInstance().newDuration(true, 0, 0, days, dayHours, new Integer(strMinutes), 0);
        } catch (DatatypeConfigurationException e) {
            e.printStackTrace();
        }
        return duration;
    }

    private Journey setJourney(Journey journey, FareMasterPricerTravelBoardSearchReply.FlightIndex.GroupOfFlights groupOfFlight, FareMasterPricerTravelBoardSearchReply.Recommendation recommendation){
        //no of stops
        journey.setNoOfStops(groupOfFlight.getFlightDetails().size()-1);

        //set travel time
        for(ProposedSegmentDetailsType proposedSegmentDetailsType : groupOfFlight.getPropFlightGrDetail().getFlightProposal()){
            if(proposedSegmentDetailsType.getUnitQualifier() != null && proposedSegmentDetailsType.getUnitQualifier().equals("EFT")){
                journey.setTravelTime(setTravelDuraion(proposedSegmentDetailsType.getRef()));
            }
        }
        //get farebases
        String fareBasis = getFareBasis(recommendation.getPaxFareProduct().get(0).getFareDetails().get(0));
        //set segments information

        String validatingCarrierCode = null;
        if(recommendation.getPaxFareProduct().get(0).getPaxFareDetail().getCodeShareDetails().get(0).getTransportStageQualifier().equals("V")) {
            validatingCarrierCode = recommendation.getPaxFareProduct().get(0).getPaxFareDetail().getCodeShareDetails().get(0).getCompany();
        }

        StringBuilder fullSegmentBuilder = new StringBuilder();
        for(FareMasterPricerTravelBoardSearchReply.FlightIndex.GroupOfFlights.FlightDetails flightDetails : groupOfFlight.getFlightDetails()){
            AirSegmentInformation airSegmentInformation = setSegmentInformation(flightDetails, fareBasis, validatingCarrierCode);
            if(airSegmentInformation.getToAirport().getAirportName() != null && airSegmentInformation.getFromAirport().getAirportName() != null) {
                journey.getAirSegmentList().add(airSegmentInformation);
                fullSegmentBuilder.append(airSegmentInformation.getFromLocation());
                fullSegmentBuilder.append(airSegmentInformation.getToLocation());
                journey.setProvider("Amadeus");
            }
        }
        List<AirSegmentInformation> airSegmentInformations = journey.getAirSegmentList();
        StringBuilder segmentBuilder = new StringBuilder();
        if(airSegmentInformations.size() > 0) {
            segmentBuilder.append(airSegmentInformations.get(0).getFromLocation());
            segmentBuilder.append(airSegmentInformations.get(airSegmentInformations.size()-1).getToLocation());
            journey.setFromLocation(airSegmentInformations.get(0).getFromLocation());
            journey.setToLocation(airSegmentInformations.get(airSegmentInformations.size()-1).getToLocation());
        }
        journey.setSegmentKey(segmentBuilder.toString());
        journey.setFullSegmentKey(fullSegmentBuilder.toString());
        getConnectionTime(journey.getAirSegmentList());
        return journey;
    }

    private void getConnectionTime(List<AirSegmentInformation> airSegments) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
        if (airSegments.size() > 1) {
            for (int i = 1; i < airSegments.size(); i++) {
                Long arrivalTime;
                try {
                    arrivalTime = dateFormat.parse(
                            airSegments.get(i - 1).getArrivalTime()).getTime();

                    Long departureTime = dateFormat.parse(
                            airSegments.get(i).getDepartureTime()).getTime();
                    Long transit = departureTime - arrivalTime;
                    airSegments.get(i - 1).setConnectionTime(
                            Integer.valueOf((int) (transit / 60000)));
                } catch (ParseException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

    private AirSegmentInformation setSegmentInformation(FareMasterPricerTravelBoardSearchReply.FlightIndex.GroupOfFlights.FlightDetails flightDetails, String fareBasis, String validatingCarrierCode){
        AirSegmentInformation airSegmentInformation = new AirSegmentInformation();
        TravelProductType flightInformation=flightDetails.getFlightInformation();
        airSegmentInformation.setCarrierCode(flightInformation.getCompanyId().getMarketingCarrier());
        if(flightInformation.getCompanyId().getOperatingCarrier() != null)
            airSegmentInformation.setOperatingCarrierCode(flightInformation.getCompanyId().getOperatingCarrier());
        airSegmentInformation.setFlightNumber(flightInformation.getFlightOrtrainNumber());
        airSegmentInformation.setEquipment(flightInformation.getProductDetail().getEquipmentType());
        airSegmentInformation.setValidatingCarrierCode(validatingCarrierCode);
        //airSegmentInformation.setArrivalTime(flightInformation.getProductDateTime().getTimeOfArrival());
        //airSegmentInformation.setDepartureTime(flightInformation.getProductDateTime().getDateOfDeparture());
        airSegmentInformation.setFromTerminal(flightInformation.getLocation().get(0).getTerminal());
        airSegmentInformation.setToTerminal(flightInformation.getLocation().get(1).getTerminal());
        airSegmentInformation.setToDate(flightInformation.getProductDateTime().getDateOfDeparture());
        airSegmentInformation.setFromDate(flightInformation.getProductDateTime().getDateOfArrival());
        airSegmentInformation.setToLocation(flightInformation.getLocation().get(1).getLocationId());
        airSegmentInformation.setFromLocation(flightInformation.getLocation().get(0).getLocationId());
        Airport fromAirport = new Airport();
        Airport toAirport = new Airport();
        fromAirport = Airport.getAirport(airSegmentInformation.getFromLocation(), redisTemplate);
        toAirport = Airport.getAirport(airSegmentInformation.getToLocation(), redisTemplate);

        SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyHHmm");
        String DATE_FORMAT = "ddMMyyHHmm";
        DateTimeFormatter DATETIME_FORMATTER = DateTimeFormat.forPattern(DATE_FORMAT);
        DateTimeZone dateTimeZone = DateTimeZone.forID(fromAirport.getTime_zone());
        DateTime departureDate = DATETIME_FORMATTER.withZone(dateTimeZone).parseDateTime(flightInformation.getProductDateTime().getDateOfDeparture() + flightInformation.getProductDateTime().getTimeOfDeparture());
        dateTimeZone = DateTimeZone.forID(toAirport.getTime_zone());
        DateTime arrivalDate = DATETIME_FORMATTER.withZone(dateTimeZone).parseDateTime(flightInformation.getProductDateTime().getDateOfArrival() + flightInformation.getProductDateTime().getTimeOfArrival());

        airSegmentInformation.setDepartureDate(departureDate.toDate());
        airSegmentInformation.setDepartureTime(departureDate.toString());
        airSegmentInformation.setArrivalTime(arrivalDate.toString());
        airSegmentInformation.setArrivalDate(arrivalDate.toDate());
        airSegmentInformation.setFromAirport(fromAirport);
        airSegmentInformation.setToAirport(toAirport);
        Minutes diff = Minutes.minutesBetween(departureDate, arrivalDate);

        airSegmentInformation.setFareBasis(fareBasis);

        airSegmentInformation.setTravelTime("" + diff.getMinutes());
        if (flightInformation.getCompanyId() != null && flightInformation.getCompanyId().getMarketingCarrier() != null && flightInformation.getCompanyId().getMarketingCarrier().length() >= 2) {
            airSegmentInformation.setAirline(Airline.getAirlineByCode(flightInformation.getCompanyId().getMarketingCarrier(), redisTemplate));
            airSegmentInformation.setOperatingAirline(Airline.getAirlineByCode(flightInformation.getCompanyId().getOperatingCarrier(), redisTemplate));
        }

        //hopping
        if(flightDetails.getTechnicalStop()!=null){
            List<HoppingFlightInformation> hoppingFlightInformations = null;
            for (DateAndTimeInformationType dateAndTimeInformationType :flightDetails.getTechnicalStop()){
                //Arrival
                HoppingFlightInformation hop =new HoppingFlightInformation();
                hop.setLocation(dateAndTimeInformationType.getStopDetails().get(0).getLocationId());
                hop.setStartTime(new StringBuilder(dateAndTimeInformationType.getStopDetails().get(0).getFirstTime()).insert(2, ":").toString());
                SimpleDateFormat dateParser = new SimpleDateFormat("ddMMyy");
                Date startDate = null;
                Date endDate = null;
                try {
                    startDate = dateParser.parse(dateAndTimeInformationType.getStopDetails().get(0).getDate());
                    endDate = dateParser.parse(dateAndTimeInformationType.getStopDetails().get(1).getDate());
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy");
                hop.setStartDate(dateFormat.format(startDate));
                //Departure
                hop.setEndTime(new StringBuilder(dateAndTimeInformationType.getStopDetails().get(1).getFirstTime()).insert(2, ":").toString());
                hop.setEndDate(dateFormat.format(endDate));
                if(hoppingFlightInformations==null){
                    hoppingFlightInformations = new ArrayList<HoppingFlightInformation>();
                }
                hoppingFlightInformations.add(hop);
            }
            airSegmentInformation.setHoppingFlightInformations(hoppingFlightInformations);
        }
        return airSegmentInformation;
    }

    private PricingInformation getPricingInformation(FareMasterPricerTravelBoardSearchReply.Recommendation recommendation) {
        PricingInformation pricingInformation = new PricingInformation();
        pricingInformation.setProvider("Amadeus");
        List<MonetaryInformationDetailsType> monetaryDetails = recommendation.getRecPriceInfo().getMonetaryDetail();
        BigDecimal totalFare = monetaryDetails.get(0).getAmount();
        BigDecimal totalTax = monetaryDetails.get(1).getAmount();
        pricingInformation.setBasePrice(totalFare.subtract(totalTax));
        pricingInformation.setTax(totalTax);
        pricingInformation.setTotalPrice(totalFare);
        pricingInformation.setTotalPriceValue(totalFare);
        List<PassengerTax> passengerTaxes= new ArrayList<>();
        for(FareMasterPricerTravelBoardSearchReply.Recommendation.PaxFareProduct paxFareProduct : recommendation.getPaxFareProduct()) {
            PassengerTax passengerTax = new PassengerTax();
            int paxCount = paxFareProduct.getPaxReference().get(0).getTraveller().size();
            String paxType = paxFareProduct.getPaxReference().get(0).getPtc().get(0);
            PricingTicketingSubsequentType144401S fareDetails = paxFareProduct.getPaxFareDetail();
            BigDecimal amount = fareDetails.getTotalFareAmount();
            BigDecimal tax = fareDetails.getTotalTaxAmount();
            BigDecimal baseFare = amount.subtract(tax);
            if(paxType.equalsIgnoreCase("ADT") || paxType.equalsIgnoreCase("SEA")) {
//        		pricingInformation.setAdtBasePrice(baseFare.multiply(new BigDecimal(paxCount)));
                pricingInformation.setAdtBasePrice(baseFare);
                pricingInformation.setAdtTotalPrice(amount);
                passengerTax.setPassengerType("ADT");
                passengerTax.setTotalTax(tax);
                passengerTax.setPassengerCount(paxCount);
            } else if(paxType.equalsIgnoreCase("CHD")) {
//				pricingInformation.setChdBasePrice(baseFare.multiply(new BigDecimal(paxCount)));
                pricingInformation.setChdBasePrice(baseFare);
                pricingInformation.setChdTotalPrice(amount);
                passengerTax.setPassengerType("CHD");
                passengerTax.setTotalTax(tax);
                passengerTax.setPassengerCount(paxCount);
            } else if(paxType.equalsIgnoreCase("INF")) {
//				pricingInformation.setInfBasePrice(baseFare.multiply(new BigDecimal(paxCount)));
                pricingInformation.setInfBasePrice(baseFare);
                pricingInformation.setInfTotalPrice(amount);
                passengerTax.setPassengerType("INF");
                passengerTax.setTotalTax(tax);
                passengerTax.setPassengerCount(paxCount);
            }
            passengerTaxes.add(passengerTax);
        }
        pricingInformation.setPassengerTaxes(passengerTaxes);
        return pricingInformation;
    }

    private AirSegmentInformation createSegment(TravelProductType flightInformation, String prevArrivalTime) {

        AirSegmentInformation airSegmentInformation = new AirSegmentInformation();
        airSegmentInformation.setCarrierCode(flightInformation.getCompanyId().getMarketingCarrier());
        if(flightInformation.getCompanyId().getOperatingCarrier() != null)
            airSegmentInformation.setOperatingCarrierCode(flightInformation.getCompanyId().getOperatingCarrier());
        airSegmentInformation.setFlightNumber(flightInformation.getFlightOrtrainNumber());

        //airSegmentInformation.setArrivalTime(flightInformation.getProductDateTime().getTimeOfArrival());
        //airSegmentInformation.setDepartureTime(flightInformation.getProductDateTime().getDateOfDeparture());
        airSegmentInformation.setFromTerminal(flightInformation.getLocation().get(0).getTerminal());
        airSegmentInformation.setToTerminal(flightInformation.getLocation().get(1).getTerminal());
        airSegmentInformation.setToDate(flightInformation.getProductDateTime().getDateOfDeparture());
        airSegmentInformation.setFromDate(flightInformation.getProductDateTime().getDateOfArrival());
        airSegmentInformation.setToLocation(flightInformation.getLocation().get(1).getLocationId());
        airSegmentInformation.setFromLocation(flightInformation.getLocation().get(0).getLocationId());
        Airport fromAirport = new Airport();
        Airport toAirport = new Airport();
        fromAirport = Airport.getAirport(airSegmentInformation.getFromLocation(), redisTemplate);
        toAirport = Airport.getAirport(airSegmentInformation.getToLocation(), redisTemplate);

        SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyHHmm");
        String DATE_FORMAT = "ddMMyyHHmm";
        DateTimeFormatter DATETIME_FORMATTER = DateTimeFormat.forPattern(DATE_FORMAT);
        DateTimeZone dateTimeZone = DateTimeZone.forID(fromAirport.getTime_zone());
        DateTime departureDate = DATETIME_FORMATTER.withZone(dateTimeZone).parseDateTime(flightInformation.getProductDateTime().getDateOfDeparture() + flightInformation.getProductDateTime().getTimeOfDeparture());
        dateTimeZone = DateTimeZone.forID(toAirport.getTime_zone());
        DateTime arrivalDate = DATETIME_FORMATTER.withZone(dateTimeZone).parseDateTime(flightInformation.getProductDateTime().getDateOfArrival() + flightInformation.getProductDateTime().getTimeOfArrival());

        airSegmentInformation.setDepartureDate(departureDate.toDate());
        airSegmentInformation.setDepartureTime(departureDate.toString());
        airSegmentInformation.setArrivalTime(arrivalDate.toString());
        airSegmentInformation.setArrivalDate(arrivalDate.toDate());
        airSegmentInformation.setFromAirport(fromAirport);
        airSegmentInformation.setToAirport(toAirport);
        Minutes diff = Minutes.minutesBetween(departureDate, arrivalDate);

        if (prevArrivalTime.equalsIgnoreCase("")) {
            airSegmentInformation.setConnectionTime(0);
        } else {
            String pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
            DateTimeFormatter dtf = DateTimeFormat.forPattern(pattern);
            DateTime prevArrivalDateTime = dtf.parseDateTime(prevArrivalTime);
            Minutes diffDeparture = Minutes.minutesBetween(prevArrivalDateTime, departureDate);
            airSegmentInformation.setConnectionTime(diffDeparture.getMinutes());
        }

        airSegmentInformation.setTravelTime("" + diff.getMinutes());
        if (flightInformation.getCompanyId() != null && flightInformation.getCompanyId().getMarketingCarrier() != null && flightInformation.getCompanyId().getMarketingCarrier().length() >= 2) {
            airSegmentInformation.setAirline(Airline.getAirlineByCode(flightInformation.getCompanyId().getMarketingCarrier(), redisTemplate));
            airSegmentInformation.setOperatingAirline(Airline.getAirlineByCode(flightInformation.getCompanyId().getOperatingCarrier(), redisTemplate));
        }

        return airSegmentInformation;
    }

    private SearchResponse addSeamenFareToSolution(AirSolution allSolution, AirSolution seamenSolution) {
        logger.debug("==============================================================================");
        logger.debug("[Amadeus] All Solution Length:::::" + allSolution.getFlightItineraryList().size());
        logger.debug("[Amadeus] Seamen Solution Length:::::" + seamenSolution.getFlightItineraryList().size());
        logger.debug("==============================================================================");
        SearchResponse searchResponse = new SearchResponse();
        ConcurrentHashMap<Integer, FlightItinerary> allFaresHash = new ConcurrentHashMap<>();
        ConcurrentHashMap<Integer, FlightItinerary> seamenFareHash = new ConcurrentHashMap<>();
        for (FlightItinerary flightItinerary : seamenSolution.getFlightItineraryList()) {
            seamenFareHash.put(flightItinerary.hashCode(), flightItinerary);
        }
        for (FlightItinerary flightItinerary : allSolution.getFlightItineraryList()) {
            allFaresHash.put(flightItinerary.hashCode(), flightItinerary);
        }
        for (Integer hashKey : seamenFareHash.keySet()) {
            FlightItinerary seamenItinerary = null;
            if (allFaresHash.containsKey(hashKey)) {
                seamenItinerary = allFaresHash.get(hashKey);
                seamenItinerary.setPriceOnlyPTC(true);
                seamenItinerary.setPricingMessage(seamenFareHash.get(hashKey).getPricingMessage());
                seamenItinerary.setSeamanPricingInformation(seamenFareHash.get(hashKey).getPricingInformation());
                allFaresHash.put(hashKey, seamenItinerary);
            } else {
                seamenItinerary = seamenFareHash.get(hashKey);
                seamenItinerary.setPriceOnlyPTC(true);
                seamenItinerary.setSeamanPricingInformation(seamenItinerary.getPricingInformation());
                seamenItinerary.setPricingInformation(null);
                allFaresHash.put(hashKey, seamenItinerary);
            }
        }
        AirSolution airSolution = new AirSolution();
        airSolution.setFlightItineraryList(new ArrayList<FlightItinerary>(allFaresHash.values()));
        searchResponse.setAirSolution(airSolution);
        return searchResponse;
    }


    private List<PAXFareDetails> createFareDetails(FareMasterPricerTravelBoardSearchReply.Recommendation recommendation, List<Journey> journeys){
        List<PAXFareDetails> paxFareDetailsList = new ArrayList<>();
        for(FareMasterPricerTravelBoardSearchReply.Recommendation.PaxFareProduct paxFareProduct :recommendation.getPaxFareProduct()){
            PAXFareDetails paxFareDetails = new PAXFareDetails();
            for(FareMasterPricerTravelBoardSearchReply.Recommendation.PaxFareProduct.FareDetails fareDetails :paxFareProduct.getFareDetails()){
                FareJourney fareJourney = new FareJourney();
                for(FareMasterPricerTravelBoardSearchReply.Recommendation.PaxFareProduct.FareDetails.GroupOfFares groupOfFares: fareDetails.getGroupOfFares()){
                    FareSegment fareSegment = new FareSegment();
                    fareSegment.setBookingClass(groupOfFares.getProductInformation().getCabinProduct().getRbd());
                    fareSegment.setCabinClass(groupOfFares.getProductInformation().getCabinProduct().getCabin());
                    paxFareDetails.setPassengerTypeCode(PassengerTypeCode.valueOf(groupOfFares.getProductInformation().getFareProductDetail().getPassengerType()));

                    fareSegment.setFareBasis(groupOfFares.getProductInformation().getFareProductDetail().getFareBasis());
                    fareJourney.getFareSegmentList().add(fareSegment);
                }
                paxFareDetails.getFareJourneyList().add(fareJourney);
            }
            paxFareDetailsList.add(paxFareDetails);
        }
        return paxFareDetailsList;
    }

    public String getFareBasis(FareMasterPricerTravelBoardSearchReply.Recommendation.PaxFareProduct.FareDetails fareDetails){
        for(FareMasterPricerTravelBoardSearchReply.Recommendation.PaxFareProduct.FareDetails.GroupOfFares groupOfFares : fareDetails.getGroupOfFares()){
            return groupOfFares.getProductInformation().getFareProductDetail().getFareBasis();

        }
        return  null;
    }

    private List getAvailabilityCtx(ReferenceInfoType segmentRef, List<FareMasterPricerTravelBoardSearchReply.Recommendation.SpecificRecDetails> specificRecDetails){
        List<String> contextList = new ArrayList<>();
        for(ReferencingDetailsType191583C referencingDetailsType : segmentRef.getReferencingDetail()) {


            if(referencingDetailsType.getRefQualifier().equalsIgnoreCase("A")){
                BigInteger refNumber = referencingDetailsType.getRefNumber();
                for(FareMasterPricerTravelBoardSearchReply.Recommendation.SpecificRecDetails specificRecDetail : specificRecDetails){
                    if(refNumber.equals(specificRecDetail.getSpecificRecItem().getRefNumber())){
                        for(FareMasterPricerTravelBoardSearchReply.Recommendation.SpecificRecDetails.SpecificProductDetails specificProductDetails : specificRecDetail.getSpecificProductDetails()){
                            for(FareMasterPricerTravelBoardSearchReply.Recommendation.SpecificRecDetails.SpecificProductDetails.FareContextDetails fareContextDetails : specificProductDetails.getFareContextDetails()){
                                for(FareMasterPricerTravelBoardSearchReply.Recommendation.SpecificRecDetails.SpecificProductDetails.FareContextDetails.CnxContextDetails cnxContextDetails : fareContextDetails.getCnxContextDetails()){
                                    contextList.addAll(cnxContextDetails.getFareCnxInfo().getContextDetails().getAvailabilityCnxType());
                                }

                            }
                        }
                    }
                }
            }

        }

        return contextList;
    }

}
