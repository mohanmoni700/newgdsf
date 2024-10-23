package services;

import com.compassites.constants.CacheConstants;
import com.compassites.constants.TraveloMatrixConstants;
import com.compassites.exceptions.RetryException;
import com.compassites.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import models.FlightSearchOffice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import play.libs.Json;
import utils.ErrorMessageHelper;


import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;

/**
 * Created by user on 17-06-2014.
 */
@Service
public class FlightSearchWrapper {

    @Autowired
    private List<FlightSearch> flightSearchList;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private AmadeusSourceOfficeService amadeusSourceOfficeService;

    static Logger logger = LoggerFactory.getLogger("gds");

    public void search(final SearchParameters searchParameters) {
        final String redisKey = searchParameters.redisKey();

        logger.debug("\n\n***********SEARCH STARTED key: [" + redisKey + "]***********");

        //long startTime = System.currentTimeMillis();
        //SearchResponse searchResponseList = new SearchResponse();
        int maxThreads = 0;
        for(FlightSearch flightSearch : flightSearchList){
            if(flightSearch.getOfficeList() == null || flightSearch.getOfficeList().size() == 0)
                  maxThreads += 1;
            else
                maxThreads += flightSearch.getOfficeList().size();
        }
        int queueSize = 10;

        redisTemplate.opsForValue().set(redisKey + ":status", "started");
        redisTemplate.expire(redisKey,CacheConstants.CACHE_TIMEOUT_IN_SECS,TimeUnit.SECONDS);
        ThreadPoolExecutor newExecutor = new ThreadPoolExecutor(maxThreads, maxThreads, Long.MAX_VALUE, TimeUnit.NANOSECONDS, new ArrayBlockingQueue<Runnable>(queueSize, true));
//        ExecutorService newExecutor = Executors.newCachedThreadPool();
        List<Future<SearchResponse>> futureSearchResponseList = new ArrayList<>();
        List<ErrorMessage> errorMessageList = new ArrayList<>();
        ConcurrentHashMap<Integer,FlightItinerary> hashMap =  new ConcurrentHashMap<>();
        for (final FlightSearch flightSearch: flightSearchList) {
        	//if( !(searchParameters.getBookingType() == BookingType.SEAMEN && flightSearch.provider().equals("Mystifly")) ) {
	            //final String providerStatusCacheKey = redisKey + flightSearch.provider() + "status";
                 String providerStatusCacheKey = "";
                    try {
                        logger.debug("Flight Search Provider["+redisKey+"] : "+flightSearch.provider());
                        for(FlightSearchOffice office: flightSearch.getOfficeList()) {
                        //Call provider if response is not already present;
                           logger.debug("**** Office: " + Json.stringify(Json.toJson(office)));
                            providerStatusCacheKey = redisKey + flightSearch.provider() +":"+ office.getOfficeId()+ "status";
                            if (!checkOrSetStatus(providerStatusCacheKey)) {
                                String finalProviderStatusCacheKey = providerStatusCacheKey;
                                futureSearchResponseList.add(newExecutor.submit(new Callable<SearchResponse>() {
                                    public SearchResponse call() throws Exception {
                                        SearchResponse response = flightSearch.search(searchParameters, office);
                                            logger.debug("1-[" + redisKey + "]Response from provider:" + flightSearch.provider() + "  officeId:" + office.getOfficeId());
                                            logger.debug("response " + Json.toJson(response));
                                            if (checkResponseAndSetStatus(response, finalProviderStatusCacheKey)) {
                                                logger.debug("2-[" + redisKey + "]Response from provider:" + flightSearch.provider() + "  officeId:" + office.getOfficeId() + "  size: " + response.getAirSolution().getFlightItineraryList().size());
                                            }

                                            return response;

                                    }
                            }));
                            } else {
                                String cachedResponse = (String) redisTemplate.opsForValue().get(redisKey);
                                JsonNode rs = null;
                                if (cachedResponse != null)
                                    rs = Json.parse(cachedResponse);
                                //SearchResponse chachedRespons = null;
                                if (rs != null){
                                    SearchResponse chachedResponse = Json.fromJson(rs, SearchResponse.class);
                                    for(FlightItinerary flightItinerary : chachedResponse.getAirSolution().getFlightItineraryList()){
                                        hashMap.put(flightItinerary.hashCode(),flightItinerary);
                                    }
                                }
                            }
                       }
                    } catch (Exception e) {
                        checkResponseAndSetStatus(null, providerStatusCacheKey);
                        logger.debug("["+redisKey+"]Response from provider:" +flightSearch.provider());
                        e.printStackTrace();
                    }
                //}
            }

        logger.debug("["+redisKey+"] : " + futureSearchResponseList.size()+ "Threads initiated");
        int counter = 0;
        boolean loop = true;
        int searchResponseListSize = futureSearchResponseList.size();
        int exceptionCounter = 0;

        while(loop){
            ListIterator<Future<SearchResponse>> listIterator = futureSearchResponseList.listIterator();
            while (listIterator.hasNext()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Future<SearchResponse> future = listIterator.next();
                SearchResponse searchResponse = null;
                /*logger.debug("=================================>" +
                        String.format("[monitor] [%d/%d] Active: %d, Completed: %d, Task: %d, isShutdown: %s, isTerminated: %s",
                                newExecutor.getPoolSize(),
                                newExecutor.getCorePoolSize(),
                                newExecutor.getActiveCount(),
                                newExecutor.getCompletedTaskCount(),
                                newExecutor.getTaskCount(),
                                newExecutor.isShutdown(),
                                newExecutor.isTerminated()));*/
                if(future.isDone()){
                    listIterator.remove();
                    counter++;
                    try {
                        searchResponse = future.get();
                    } catch (RetryException retryOnFailure) {
                        exceptionCounter++;

                        if(exceptionCounter == flightSearchList.size()){
                            logger.debug("["+redisKey+"]All providers gave error");
                            //send email to IT admin
                        }
                        logger.error("retrialError in FlightSearchWrapper : ", retryOnFailure);
                        ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage("retrialError",ErrorMessage.ErrorType.ERROR,"Application");
                        errorMessageList.add(errorMessage);
                    }catch (Exception e){
                        logger.error("Exception in FlightSearchWrapper : ", e);
                        ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage("partialResults",ErrorMessage.ErrorType.WARNING,"Application");
                        errorMessageList.add(errorMessage);
                        e.printStackTrace();
                    }

                   /* if (searchResponseList.size() > 0){
                        searchResponseList.get(0).getAirSolution().getFlightItineraryList().addAll(searchResponse.getAirSolution().getFlightItineraryList()) ;
                    }
                    else {
                        searchResponseList.add(searchResponse);
                    }

                    logger.debug("Redis Key :" + searchParameters.redisKey());
                    String res = je(searchParameters.redisKey(), Json.stringify(Json.toJson(searchResponseList)));

                    res = j.set(searchParameters.redisKey()+":status","partial");*/
                    //listIterator.remove();
                    //counter++;

                    if(searchResponse != null && searchResponse.getErrorMessageList().size() == 0){
                        logger.debug("3-["+redisKey+"]Received Response "+ counter +"  | from : " + searchResponse.getProvider()+  "   | office:"+ searchResponse.getFlightSearchOffice().getOfficeId()  +"  | Seaman size: " + searchResponse.getAirSolution().getSeamenHashMap().size() + " | normal size:"+searchResponse.getAirSolution().getNonSeamenHashMap().size() );
                        SearchResponse searchResponseCache=new SearchResponse();
                        searchResponseCache.setFlightSearchOffice(searchResponse.getFlightSearchOffice());
                        searchResponseCache.setProvider(searchResponse.getProvider());
                        //logger.debug("counter :"+counter+"Search Response FligthItinary Size: "+searchResponse.getAirSolution().getFlightItineraryList().size());
                        logger.debug("\n\n----------- before MergeResults "+ counter +"--------"+ searchResponse.getFlightSearchOffice().getOfficeId());
                        //AmadeusFlightSearch.printHashmap(hashMap,false);
                        mergeResults(hashMap, searchResponse);
                        logger.debug("----------- After MergeResults "+ counter +"--------" +searchResponse.getFlightSearchOffice().getOfficeId());
                        //AmadeusFlightSearch.printHashmap(hashMap,false);
                        errorMessageList.addAll(searchResponse.getErrorMessageList());
                        AirSolution airSolution = new AirSolution();
                        airSolution.setReIssueSearch(false);
                        airSolution.setFlightItineraryList(new ArrayList<FlightItinerary>(hashMap.values()));
                        airSolution.setGroupingKeyMap(searchResponse.getAirSolution().getGroupingKeyMap());
                        searchResponseCache.setAirSolution(airSolution);
                        searchResponseCache.setReIssueSearch(false);
                        searchResponseCache.getErrorMessageList().addAll(searchResponse.getErrorMessageList());
                        //searchResponseList.add(searchResponseCache);
                        searchResponseCache.setErrorMessageList(errorMessageList);
                        redisTemplate.opsForValue().set(searchParameters.redisKey(), Json.stringify(Json.toJson(searchResponseCache)));
                        redisTemplate.expire(searchParameters.redisKey(),CacheConstants.CACHE_TIMEOUT_IN_SECS,TimeUnit.SECONDS);
                        redisTemplate.opsForValue().set(searchParameters.redisKey()+":status", "partial" + counter);
                        redisTemplate.expire(searchParameters.redisKey()+":status",CacheConstants.CACHE_TIMEOUT_IN_SECS,TimeUnit.SECONDS);
                        //searchResponseList.remove(0);
                        //searchResponseList = searchResponseCache;
                        ////logger.debug("4-["+redisKey+"]Added response to final hashmap"+ counter +"  | from:" + searchResponseCache.getProvider()+ "  | office:"+ searchResponseCache.getFlightSearchOffice().getOfficeId()+"  | hashmap size: "+ searchResponseCache.getAirSolution().getFlightItineraryList().size() +" | search:"+ searchResponse.getAirSolution().getNonSeamenHashMap().size() + " + "+ searchResponse.getAirSolution().getNonSeamenHashMap().size());
                    }
                    else
                    {
                        logger.debug("["+redisKey+"]Received Response "+ counter +" Null" );
                    }

                }
            }

            newExecutor.shutdown();
            if(counter == searchResponseListSize){

                /*** cache for very short time(only for the purpose for JustOneClick to receive the response )if there is error. ***/
                Integer timeout = CacheConstants.CACHE_TIMEOUT_IN_SECS;
                if(errorMessageList.size() > 0){
                    timeout = CacheConstants.CACHE_TIMEOUT_FOR_ERROR_IN_SECS;
                }

                loop = false;
                if(hashMap.size() == 0 && errorMessageList.size() > 0)  {
                    SearchResponse searchResponse = new SearchResponse();
                    searchResponse.setErrorMessageList(errorMessageList);
                    redisTemplate.opsForValue().set(searchParameters.redisKey(), Json.stringify(Json.toJson(searchResponse)));
                    redisTemplate.expire(searchParameters.redisKey(),timeout,TimeUnit.SECONDS);
                }
                redisTemplate.opsForValue().set(searchParameters.redisKey()+":status", "complete");
                redisTemplate.expire(searchParameters.redisKey()+":status",timeout,TimeUnit.SECONDS);
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
        /*
        logger.debug("HashMap Size: "+hashMap.size());
        SearchResponse searchResponse=new SearchResponse();
        searchResponse.setErrorMessageList(errorMessageList);
        AirSolution airSolution = new AirSolution();
        airSolution.setFlightItineraryList(new ArrayList<FlightItinerary>(hashMap.values()));
        searchResponse.setAirSolution(airSolution);
        searchResponseList.add(searchResponse);
        logger.debug("***********SEARCH END***********");
        */
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

    public void mergeResults(ConcurrentHashMap<Integer, FlightItinerary> allFightItineraries, SearchResponse searchResponse) {
    	try{
        AirSolution airSolution = searchResponse.getAirSolution();
        String provider = searchResponse.getProvider();
        ConcurrentHashMap<String, List<Integer>> concurrentHashMap = null;
        if(provider.equals(TraveloMatrixConstants.tmofficeId)){
            System.out.println("travelomatrix merge");
            //concurrentHashMap = airSolution.getGroupingKeyMap();
        } else if (provider.equalsIgnoreCase("Amadeus")) {
            System.out.println("Amadeus merge");
        }
        concurrentHashMap = airSolution.getGroupingKeyMap();
        FlightSearchOffice office = searchResponse.getFlightSearchOffice();
        if(allFightItineraries.isEmpty()) {
            mergeSeamenAndNonSeamenResults(allFightItineraries, airSolution,provider,searchResponse);
        } else {
            ConcurrentHashMap<Integer, FlightItinerary> seamenFareHash = airSolution.getSeamenHashMap();
            ConcurrentHashMap<Integer, FlightItinerary> nonSeamenFareHash = airSolution.getNonSeamenHashMap();
                for (Integer hashKey : allFightItineraries.keySet()) {
                    if (seamenFareHash != null && seamenFareHash.containsKey(hashKey)) {
                        FlightItinerary mainFlightItinerary = allFightItineraries.get(hashKey);
                        FlightItinerary seamenItinerary = seamenFareHash.get(hashKey);
                        if (mainFlightItinerary.getSeamanPricingInformation() == null
                                || mainFlightItinerary.getSeamanPricingInformation().getTotalPriceValue() == null
                                || (seamenItinerary.getPricingInformation() != null && seamenItinerary.getPricingInformation().getTotalPriceValue() != null
                                && seamenItinerary.getPricingInformation().getTotalPriceValue().longValue() < mainFlightItinerary.getSeamanPricingInformation().getTotalPriceValue().longValue())
                                || (seamenItinerary.getPricingInformation().getPricingOfficeId().equalsIgnoreCase(amadeusSourceOfficeService.getPrioritySourceOffice().getOfficeId())
                                && seamenItinerary.getPricingInformation() != null && seamenItinerary.getPricingInformation().getTotalPriceValue() != null
                                && seamenItinerary.getPricingInformation().getTotalPriceValue().longValue() <= mainFlightItinerary.getSeamanPricingInformation().getTotalPriceValue().longValue())) {
                            mainFlightItinerary.setSeamanPricingInformation(seamenItinerary.getPricingInformation());
                            mainFlightItinerary.setJourneyList(seamenItinerary.getJourneyList());
                        }
                        allFightItineraries.put(hashKey, mainFlightItinerary);
                        seamenFareHash.remove(hashKey);
                    }
                    if (nonSeamenFareHash != null && nonSeamenFareHash.containsKey(hashKey)) {
                        FlightItinerary mainFlightItinerary = allFightItineraries.get(hashKey);
                        FlightItinerary nonSeamenItinerary = nonSeamenFareHash.get(hashKey);
                        if (mainFlightItinerary.getPricingInformation() == null
                                || mainFlightItinerary.getPricingInformation().getTotalPriceValue() == null
                                || (nonSeamenItinerary.getPricingInformation() != null && nonSeamenItinerary.getPricingInformation().getTotalPriceValue() != null
                                && nonSeamenItinerary.getPricingInformation().getTotalPriceValue().longValue() < mainFlightItinerary.getPricingInformation().getTotalPriceValue().longValue())
                                || (nonSeamenItinerary.getPricingInformation().getPricingOfficeId().equalsIgnoreCase(amadeusSourceOfficeService.getPrioritySourceOffice().getOfficeId())
                                && nonSeamenItinerary.getPricingInformation() != null && nonSeamenItinerary.getPricingInformation().getTotalPriceValue() != null
                                && nonSeamenItinerary.getPricingInformation().getTotalPriceValue().longValue() <= mainFlightItinerary.getPricingInformation().getTotalPriceValue().longValue())) {
                            mainFlightItinerary.setPricingInformation(nonSeamenItinerary.getPricingInformation());
                            mainFlightItinerary.setNonSeamenJourneyList(nonSeamenItinerary.getJourneyList());
                            //for Travelomatrix
                            if(nonSeamenItinerary.getResultToken() != null){
                                mainFlightItinerary.setResultToken(nonSeamenItinerary.getResultToken());
                                mainFlightItinerary.setIsLCC(nonSeamenItinerary.getLCC());
                            }
                            if(provider.equalsIgnoreCase(TraveloMatrixConstants.provider)) {
                                List<Journey> journeyList = new ArrayList<>();
                                boolean isMerged = false;
                                System.out.println("Not empty merge ");
                                for (Journey journey : nonSeamenItinerary.getJourneyList()) {
                                    Journey journey1 = new Journey();
                                    String groupKey = journey.getGroupingKey();
                                    if (concurrentHashMap != null && concurrentHashMap.containsKey(groupKey) && concurrentHashMap.size() > 1) {
                                        List<Integer> hashCodes = concurrentHashMap.get(groupKey);
                                        List<FlightItinerary> flightItineraries = new ArrayList<>();
                                        boolean isSkip = true;
                                        for (Integer integer : hashCodes) {
                                            FlightItinerary itinerary = nonSeamenFareHash.get(integer);
                                            if (itinerary != null) {
                                                if (!isSkip) {
                                                    FlightItinerary flightItinerary = new FlightItinerary();
                                                    BeanUtils.copyProperties(itinerary, flightItinerary);
                                                    flightItinerary.setGroupingMap(null);
                                                    PricingInformation pricingInformation = new PricingInformation();
                                                    BeanUtils.copyProperties(itinerary.getPricingInformation(), pricingInformation);
                                                    pricingInformation.setTotalCalculatedValue(pricingInformation.getTotalPriceValue());
                                                    pricingInformation.setTotalPrice(pricingInformation.getTotalPriceValue());
                                                    flightItinerary.setPricingInformation(pricingInformation);
                                                    flightItineraries.add(flightItinerary);
                                                } else {
                                                    isSkip = false;
                                                }
                                            } else {
                                                isMerged = true;
                                            }
                                        }
                                        if (flightItineraries.size() >= 1) {
                                            ConcurrentHashMap<String, List<FlightItinerary>> stringListConcurrentHashMap = new ConcurrentHashMap<>();
                                            stringListConcurrentHashMap.put(groupKey, flightItineraries);
                                            mainFlightItinerary.setGroupingMap(stringListConcurrentHashMap);
                                        }
                                        concurrentHashMap.remove(groupKey);
                                    }
                                    BeanUtils.copyProperties(journey, journey1);
                                    journey1.setAirSegmentList(createAirsegment(journey));
                                    journeyList.add(journey);
                                }
                                mainFlightItinerary.setJourneyList(journeyList);
                            } else {
                                ConcurrentHashMap<Integer, List<FlightItinerary>> groupingItinerary = searchResponse.getGroupingItinerary();
                                if(groupingItinerary.size() > 0) {
                                    if(groupingItinerary.containsKey(hashKey)) {
                                        if(groupingItinerary.get(hashKey).size()>1) {
                                            ConcurrentHashMap<String, List<FlightItinerary>> stringListConcurrentHashMap = new ConcurrentHashMap<>();
                                            List<FlightItinerary> flightItineraries = new ArrayList<>();
                                            groupingItinerary.get(hashKey).sort((p1, p2)
                                                    -> p1.getPricingInformation().getTotalPriceValue().compareTo(p2.getPricingInformation().getTotalPriceValue())
                                            );
                                            mainFlightItinerary = groupingItinerary.get(hashKey).get(0);
                                            int k =0;
                                            for (FlightItinerary flightItinerary: groupingItinerary.get(hashKey)) {
                                                if(k>=1) {
                                                    FlightItinerary flightItinerary1 = new FlightItinerary();
                                                    BeanUtils.copyProperties(flightItinerary, flightItinerary1);
                                                    flightItinerary1.setGroupingMap(null);
                                                    flightItinerary1.setPricingInformation(flightItinerary.getPricingInformation());
                                                    flightItinerary1.getPricingInformation().setTotalCalculatedValue(flightItinerary.getPricingInformation().getTotalPriceValue());
                                                    flightItinerary1.getPricingInformation().setTotalPrice(flightItinerary.getPricingInformation().getTotalPriceValue());
                                                    flightItinerary1.getPricingInformation().setTaxMap(flightItinerary.getPricingInformation().getTaxMap());
                                                    flightItinerary1.getPricingInformation().setTax(flightItinerary.getPricingInformation().getTax());
                                                    flightItinerary1.getPricingInformation().setTotalTax(flightItinerary.getPricingInformation().getTotalTax());
                                                    flightItineraries.add(flightItinerary1);
                                                }
                                                k++;
                                            }
                                            stringListConcurrentHashMap.put(String.valueOf(hashKey),flightItineraries);
                                            mainFlightItinerary.setGroupingMap(stringListConcurrentHashMap);
                                        }
                                        //groupingItinerary.remove(hashKey);
                                    }
                                }
                            }
                            //}
                            //mainFlightItinerary.getPricingInformation().setPricingOfficeId(nonSeamenItinerary.getPricingInformation().getPricingOfficeId());
                            //mainFlightItinerary.setAmadeusId(nonSeamenItinerary.getAmadeusOfficeId());
                            //compareItinerary(mainFlightItinerary,nonSeamenItinerary,false, provider);
                        }

                        allFightItineraries.put(hashKey, mainFlightItinerary);
                        nonSeamenFareHash.remove(hashKey);
                    }
                }
                System.out.println("Not empty map merge ");
                ConcurrentHashMap<Integer, FlightItinerary> list = mergeSeamenAndNonSeamenResults(new ConcurrentHashMap<Integer, FlightItinerary>(), airSolution,provider,searchResponse);
                allFightItineraries.putAll(list);
            }
        }catch (Exception e){
            e.printStackTrace();
            logger.error("MergeResults:: ex:"+ e.getMessage());
        }
    }

    private List<AirSegmentInformation> createAirsegment(Journey journey) {
        List<AirSegmentInformation> airSegmentInformations = new ArrayList<>();
        for (AirSegmentInformation airSegmentInformation: journey.getAirSegmentList()) {
            AirSegmentInformation airSegmentInformation1 = new AirSegmentInformation();
            BeanUtils.copyProperties(airSegmentInformation,airSegmentInformation1);
            airSegmentInformations.add(airSegmentInformation1);
        }
        return airSegmentInformations;
    }


    public ConcurrentHashMap<Integer, FlightItinerary> mergeSeamenAndNonSeamenResults(ConcurrentHashMap<Integer, FlightItinerary> allFightItineraries, AirSolution airSolution, String provider, SearchResponse searchResponse) {
        //System.out.println("Non seamen "+Json.toJson(airSolution.getNonSeamenHashMap().keySet()));
        //System.out.println("seamen "+Json.toJson(airSolution.getSeamenHashMap().keySet()));
        if (airSolution.getNonSeamenHashMap() != null && !airSolution.getNonSeamenHashMap().isEmpty()) {
            ConcurrentHashMap<String, List<Integer>> concurrentHashMap = airSolution.getGroupingKeyMap();
            ConcurrentHashMap<Integer, FlightItinerary> nonSeamenFareHash = airSolution.getNonSeamenHashMap();
            //|| provider.equalsIgnoreCase("Amadeus")
            if(provider.equalsIgnoreCase(TraveloMatrixConstants.provider)) {
                ConcurrentHashMap<Integer, FlightItinerary> integerFlightItineraryConcurrentHashMap = new ConcurrentHashMap<>();
                for (Integer hashKey : nonSeamenFareHash.keySet()) {
                    FlightItinerary nonSeamenItinerary = nonSeamenFareHash.get(hashKey);
                    boolean isMerged = false;
                    List<Journey> journeyList  = new ArrayList<>();
                    for (Journey journey : nonSeamenItinerary.getJourneyList()) {
                        Journey journey1 = new Journey();
                        String groupKey = journey.getGroupingKey();
                        if (concurrentHashMap != null && concurrentHashMap.containsKey(groupKey) && concurrentHashMap.get(groupKey).size() > 1) {
                            List<Integer> hashCodes = concurrentHashMap.get(groupKey);
                            List<FlightItinerary> flightItineraries = new ArrayList<>();
                            boolean isSkip = true;
                            for (Integer integer : hashCodes) {
                                FlightItinerary itinerary = nonSeamenFareHash.get(integer);
                                if(itinerary!=null) {
                                    if(!isSkip) {
                                        FlightItinerary flightItinerary = new FlightItinerary();
                                        BeanUtils.copyProperties(itinerary, flightItinerary);
                                        flightItinerary.setGroupingMap(null);
                                        PricingInformation pricingInformation = new PricingInformation();
                                        BeanUtils.copyProperties(itinerary.getPricingInformation(), pricingInformation);
                                        pricingInformation.setTotalCalculatedValue(pricingInformation.getTotalPriceValue());
                                        pricingInformation.setTotalPrice(pricingInformation.getTotalPriceValue());
                                        flightItinerary.setPricingInformation(pricingInformation);
                                        flightItineraries.add(flightItinerary);
                                    } else {
                                        isSkip = false;
                                    }
                                } else {
                                    isMerged = true;
                                }
                            }
                            if(flightItineraries.size() >=1) {
                                ConcurrentHashMap<String, List<FlightItinerary>> stringListConcurrentHashMap = new ConcurrentHashMap<>();
                                stringListConcurrentHashMap.put(groupKey, flightItineraries);
                                nonSeamenItinerary.setGroupingMap(stringListConcurrentHashMap);
                            }
                        }
                        BeanUtils.copyProperties(journey,journey1);
                        journey1.setAirSegmentList(createAirsegment(journey));
                        journeyList.add(journey);
                    }
                    if(!isMerged) {
                        nonSeamenItinerary.setJourneyList(journeyList);
                        integerFlightItineraryConcurrentHashMap.put(hashKey, nonSeamenItinerary);
                    }
                    nonSeamenFareHash.remove(hashKey);
                }
                allFightItineraries.putAll(integerFlightItineraryConcurrentHashMap);
                //return allFightItineraries;
            } else {
                ConcurrentHashMap<Integer, List<FlightItinerary>> groupingItinerary = searchResponse.getGroupingItinerary();
                System.out.println("Non has "+Json.toJson(nonSeamenFareHash.keySet()));
                ConcurrentHashMap<Integer, FlightItinerary> integerFlightItineraryConcurrentHashMap = new ConcurrentHashMap<>();
                for (Integer hashKey : nonSeamenFareHash.keySet()) {
                    FlightItinerary nonSeamenItinerary = nonSeamenFareHash.get(hashKey);
                    if(groupingItinerary.size() > 0) {
                        if(groupingItinerary.containsKey(hashKey)) {
                            if(groupingItinerary.get(hashKey).size()>1) {

                                ConcurrentHashMap<String, List<FlightItinerary>> stringListConcurrentHashMap = new ConcurrentHashMap<>();
                                List<FlightItinerary> flightItineraries = new ArrayList<>();
                                groupingItinerary.get(hashKey).sort((p1, p2)
                                        -> p1.getPricingInformation().getTotalPriceValue().compareTo(p2.getPricingInformation().getTotalPriceValue())
                                );
                                nonSeamenItinerary = groupingItinerary.get(hashKey).get(0);
                                int k =0;
                                for (FlightItinerary flightItinerary: groupingItinerary.get(hashKey)) {
                                        if(k>=1) {
                                            FlightItinerary flightItinerary1 = new FlightItinerary();
                                            BeanUtils.copyProperties(flightItinerary, flightItinerary1);
                                            flightItinerary1.setGroupingMap(null);
                                            flightItinerary1.setPricingInformation(flightItinerary.getPricingInformation());
                                            flightItinerary1.getPricingInformation().setTotalCalculatedValue(flightItinerary.getPricingInformation().getTotalPriceValue());
                                            flightItinerary1.getPricingInformation().setTotalPrice(flightItinerary.getPricingInformation().getTotalPriceValue());
                                            flightItinerary1.getPricingInformation().setTaxMap(flightItinerary.getPricingInformation().getTaxMap());
                                            flightItinerary1.getPricingInformation().setTax(flightItinerary.getPricingInformation().getTax());
                                            flightItinerary1.getPricingInformation().setTotalTax(flightItinerary.getPricingInformation().getTotalTax());
                                            flightItineraries.add(flightItinerary1);
                                        }
                                        k++;
                                }
                                stringListConcurrentHashMap.put(String.valueOf(hashKey),flightItineraries);
                                nonSeamenItinerary.setGroupingMap(stringListConcurrentHashMap);
                            }
                            //groupingItinerary.remove(hashKey);
                        }
                    }
                    integerFlightItineraryConcurrentHashMap.put(hashKey,nonSeamenItinerary);
                }

                //Commented while writing grouping flight logic
                //allFightItineraries.putAll(nonSeamenFareHash);
                allFightItineraries.putAll(integerFlightItineraryConcurrentHashMap);
            }
        }
        if(airSolution.getSeamenHashMap().isEmpty()) {
            System.out.println("Seaman map is empty");
        }
        if (airSolution.getSeamenHashMap() != null && !airSolution.getSeamenHashMap().isEmpty()) {
            ConcurrentHashMap<Integer, FlightItinerary> seamenFareHash = airSolution.getSeamenHashMap();
            System.out.println("Sea has "+Json.toJson(seamenFareHash.keySet()));
            for (Integer hashKey : seamenFareHash.keySet()) {
                FlightItinerary seamenItinerary = seamenFareHash.get(hashKey);
                if (allFightItineraries.containsKey(hashKey)) {
                    FlightItinerary itinerary = allFightItineraries.get(hashKey);
                    itinerary.setPriceOnlyPTC(true);
                    //System.out.println("Price "+Json.toJson(seamenFareHash.get(hashKey).getPricingInformation()));
                    itinerary.setSeamanPricingInformation(seamenFareHash.get(hashKey).getPricingInformation());
                    itinerary.setJourneyList(seamenFareHash.get(hashKey).getJourneyList());
                    itinerary.setNonSeamenJourneyList(allFightItineraries.get(hashKey).getJourneyList());

                    allFightItineraries.put(hashKey, itinerary);
                } else {
                    seamenItinerary.setPriceOnlyPTC(true);
                    seamenItinerary.setSeamanPricingInformation(seamenItinerary.getPricingInformation());
                    allFightItineraries.put(hashKey, seamenItinerary);
                }
            }
        }
        return allFightItineraries;

        
    }
    
}
