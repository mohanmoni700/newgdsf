package services;

import com.compassites.constants.CacheConstants;
import com.compassites.exceptions.RetryException;
import com.compassites.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import models.FlightSearchOffice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import play.libs.Json;
import utils.AmadeusSessionManager;
import utils.ErrorMessageHelper;

import java.security.Provider;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map.Entry;
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

        long startTime = System.currentTimeMillis();
        SearchResponse searchResponseList = new SearchResponse();
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
                                        if(checkResponseAndSetStatus(response, finalProviderStatusCacheKey)){
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

                    if(searchResponse != null){
                        logger.debug("3-["+redisKey+"]Received Response "+ counter +"  | from : " + searchResponse.getProvider()+  "   | office:"+ searchResponse.getFlightSearchOffice().getOfficeId()  +"  | Seaman size: " + searchResponse.getAirSolution().getSeamenHashMap().size() + " | normal size:"+searchResponse.getAirSolution().getNonSeamenHashMap().size() );
                        SearchResponse searchResponseCache=new SearchResponse();
                        searchResponseCache.setFlightSearchOffice(searchResponse.getFlightSearchOffice());
                        searchResponseCache.setProvider(searchResponse.getProvider());
                        //logger.debug("counter :"+counter+"Search Response FligthItinary Size: "+searchResponse.getAirSolution().getFlightItineraryList().size());
                       /* for (FlightItinerary flightItinerary : searchResponse.getAirSolution().getFlightItineraryList()) {
                            //logger.debug("FlightItinary string :"+flightItinerary.toString());
                            if(hashMap.containsKey(flightItinerary.hashCode())){
                                //logger.debug("Common Flights"+Json.toJson(flightItinerary));
                                FlightItinerary hashFlightItinerary = hashMap.get(flightItinerary.hashCode());
                                //if(searchParameters.getBookingType().equals(BookingType.NON_MARINE)){
                                    if (hashFlightItinerary.getPricingInformation() != null && hashFlightItinerary.getPricingInformation().getTotalPrice() != null
                                            && flightItinerary.getPricingInformation() != null && flightItinerary.getPricingInformation().getTotalPrice() != null) {
                                        Integer hashItineraryPrice = new Integer(hashFlightItinerary.getPricingInformation().getTotalPrice());
                                        Integer iteratorItineraryPrice = new Integer(flightItinerary.getPricingInformation().getTotalPrice());
                                        if (iteratorItineraryPrice < hashItineraryPrice) {
                                            hashMap.remove(hashFlightItinerary.hashCode());
                                            hashMap.put(flightItinerary.hashCode(), flightItinerary);
                                        }
                                    } *//*else {
                                        if (hashFlightItinerary.getSeamanPricingInformation() != null && hashFlightItinerary.getSeamanPricingInformation().getTotalPrice() != null
                                                && flightItinerary.getSeamanPricingInformation() != null && flightItinerary.getSeamanPricingInformation().getTotalPrice() != null) {
                                            Integer hashItineraryPrice = new Integer(hashFlightItinerary.getSeamanPricingInformation().getTotalPrice());
                                            Integer iteratorItineraryPrice = new Integer(flightItinerary.getSeamanPricingInformation().getTotalPrice());
                                            if (iteratorItineraryPrice < hashItineraryPrice) {
                                                hashMap.remove(hashFlightItinerary.hashCode());
                                                hashMap.put(flightItinerary.hashCode(), flightItinerary);
                                            }
                                        }
                                    }*//*
                                //}

                            } else {
                                hashMap.put(flightItinerary.hashCode(), flightItinerary);
                            }
                        }*/
                        System.out.println("\n\n----------- before MergeResults "+ counter +"--------"+ searchResponse.getFlightSearchOffice().getOfficeId());
                        //AmadeusFlightSearch.printHashmap(hashMap,false);
                        mergeResults(hashMap, searchResponse);
                        System.out.println("----------- After MergeResults "+ counter +"--------" +searchResponse.getFlightSearchOffice().getOfficeId());
                        //AmadeusFlightSearch.printHashmap(hashMap,false);
                        errorMessageList.addAll(searchResponse.getErrorMessageList());
                        AirSolution airSolution = new AirSolution();
                        airSolution.setFlightItineraryList(new ArrayList<FlightItinerary>(hashMap.values()));
                        searchResponseCache.setAirSolution(airSolution);

                        searchResponseCache.getErrorMessageList().addAll(searchResponse.getErrorMessageList());
                        //searchResponseList.add(searchResponseCache);
                        searchResponseCache.setErrorMessageList(errorMessageList);
                        redisTemplate.opsForValue().set(searchParameters.redisKey(), Json.stringify(Json.toJson(searchResponseCache)));
                        redisTemplate.expire(searchParameters.redisKey(),CacheConstants.CACHE_TIMEOUT_IN_SECS,TimeUnit.SECONDS);
                        redisTemplate.opsForValue().set(searchParameters.redisKey()+":status", "partial" + counter);
                        redisTemplate.expire(searchParameters.redisKey()+":status",CacheConstants.CACHE_TIMEOUT_IN_SECS,TimeUnit.SECONDS);
                        //searchResponseList.remove(0);
                        //searchResponseList = searchResponseCache;
                        long endTime = System.currentTimeMillis();
                        long duration = endTime - startTime;
                        //System.out.println("Execution time final gds: " + duration/1000 + " seconds");
                        logger.debug("Execution time final gds: " + duration/1000 + " seconds");
                        ////logger.debug("4-["+redisKey+"]Added response to final hashmap"+ counter +"  | from:" + searchResponseCache.getProvider()+ "  | office:"+ searchResponseCache.getFlightSearchOffice().getOfficeId()+"  | hashmap size: "+ searchResponseCache.getAirSolution().getFlightItineraryList().size() +" | search:"+ searchResponse.getAirSolution().getNonSeamenHashMap().size() + " + "+ searchResponse.getAirSolution().getNonSeamenHashMap().size());
                    }
                    else
                    {
                        logger.debug("["+redisKey+"]Received Response "+ counter +" Null" );
                    }

                }
            }

            //newExecutor.shutdown();
            if(counter == searchResponseListSize){
                newExecutor.shutdown();
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
                logger.debug("***********SEARCH END key: ["+ redisKey +"]***********\n\n");
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
        return ;
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
        try {
            AirSolution airSolution = searchResponse.getAirSolution();
            if (allFightItineraries.isEmpty()) {
                mergeSeamenAndNonSeamenResults(allFightItineraries, airSolution);

            } else {
                ConcurrentHashMap<Integer, FlightItinerary> seamenFareHash = airSolution.getSeamenHashMap();
                ConcurrentHashMap<Integer, FlightItinerary> nonSeamenFareHash = airSolution.getNonSeamenHashMap();

                for (Integer hashKey : allFightItineraries.keySet()) {
                    if(hashKey == 1521758370 || hashKey == 1521756448){
                        FlightItinerary mainFlightItinerary = allFightItineraries.get(hashKey);
                        FlightItinerary seamenItinerary = seamenFareHash.get(hashKey);
                        if(seamenItinerary == null )
                            System.out.println("***** 3: hashkey:"+ hashKey + "   mainItinerary: "+ mainFlightItinerary.getAmadeusOfficeId() +"  main_price:"+mainFlightItinerary.getSeamanPricingInformation().getTotalPriceValue() );
                        else{
                            System.out.println("***** 31: hashkey:"+ hashKey + "   mainItinerary: "+ mainFlightItinerary.getAmadeusOfficeId() +"  main_price:"+mainFlightItinerary.getSeamanPricingInformation().getTotalPriceValue() + "   tempItinerary:"+ seamenItinerary.getAmadeusOfficeId()+"  temp_price:"+seamenItinerary.getPricingInformation().getTotalPriceValue());
                        }
                        int t =0;
                    }
//                if(seamenFareHash == null && nonSeamenFareHash == null){
//                    logger.debug("==================================NULL POINTER EXECEPTION============"+ searchResponse.getProvider()+Json.toJson(searchResponse));
//                    break;
//                }
//                if(seamenFareHash.containsKey(hashKey) && nonSeamenFareHash.containsKey(hashKey)){
//                    FlightItinerary mainFlightItinerary = allFightItineraries.get(hashKey);
//                    FlightItinerary seamenItinerary = seamenFareHash.get(hashKey);
//                    FlightItinerary nonSeamenItinerary = nonSeamenFareHash.get(hashKey);
//                    if(mainFlightItinerary.getSeamanPricingInformation() == null ||
//                            mainFlightItinerary.getSeamanPricingInformation().getTotalPriceValue() == null
//                            || (seamenItinerary.getPricingInformation() != null && seamenItinerary.getPricingInformation().getTotalPriceValue().longValue() < mainFlightItinerary.getSeamanPricingInformation().getTotalPriceValue().longValue())
//                            || ( seamenItinerary.getAmadeusOfficeId().equalsIgnoreCase(amadeusSourceOfficeService.getPrioritySourceOffice().getOfficeId()) && (seamenItinerary.getPricingInformation() != null && seamenItinerary.getPricingInformation().getTotalPriceValue().longValue() < mainFlightItinerary.getSeamanPricingInformation().getTotalPriceValue().longValue()))
//
//                    ){
//                        mainFlightItinerary.setSeamanPricingInformation(seamenItinerary.getPricingInformation());
//                        mainFlightItinerary.setJourneyList(seamenItinerary.getJourneyList());
//
//                        //compareItinerary(mainFlightItinerary,seamenItinerary,true, provider);
//                    }
//                    if(mainFlightItinerary.getPricingInformation() == null
//                            || mainFlightItinerary.getPricingInformation().getTotalPriceValue() == null
//                            || nonSeamenItinerary.getPricingInformation().getTotalPriceValue().longValue() < mainFlightItinerary.getPricingInformation().getTotalPriceValue().longValue()){
//                        mainFlightItinerary.setPricingInformation(nonSeamenItinerary.getPricingInformation());
//                        mainFlightItinerary.setNonSeamenJourneyList(nonSeamenItinerary.getJourneyList());
//
//                        //compareItinerary(mainFlightItinerary,nonSeamenItinerary,false, provider);
//                    }
//                    seamenFareHash.remove(hashKey);
//                    nonSeamenFareHash.remove(hashKey);
//                    allFightItineraries.put(hashKey, mainFlightItinerary);
//                }
                    if (seamenFareHash != null && seamenFareHash.containsKey(hashKey)) {
                        FlightItinerary mainFlightItinerary = allFightItineraries.get(hashKey);
                        FlightItinerary seamenItinerary = seamenFareHash.get(hashKey);
                        if (mainFlightItinerary.getSeamanPricingInformation() == null
                                || mainFlightItinerary.getSeamanPricingInformation().getTotalPriceValue() == null
                                || (seamenItinerary.getPricingInformation() != null && seamenItinerary.getPricingInformation().getTotalPriceValue() != null
                                && seamenItinerary.getPricingInformation().getTotalPriceValue().longValue() < mainFlightItinerary.getSeamanPricingInformation().getTotalPriceValue().longValue())
                                || (seamenItinerary.getAmadeusOfficeId().equalsIgnoreCase(amadeusSourceOfficeService.getPrioritySourceOffice().getOfficeId())
                                && seamenItinerary.getPricingInformation() != null && seamenItinerary.getPricingInformation().getTotalPriceValue() != null
                                && seamenItinerary.getPricingInformation().getTotalPriceValue().longValue() <= mainFlightItinerary.getSeamanPricingInformation().getTotalPriceValue().longValue())) {
                            mainFlightItinerary.setSeamanPricingInformation(seamenItinerary.getPricingInformation());
                            mainFlightItinerary.setJourneyList(seamenItinerary.getJourneyList());
                            mainFlightItinerary.setAmadeusOfficeId(seamenItinerary.getAmadeusOfficeId());
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
                                || (nonSeamenItinerary.getAmadeusOfficeId().equalsIgnoreCase(amadeusSourceOfficeService.getPrioritySourceOffice().getOfficeId())
                                && nonSeamenItinerary.getPricingInformation() != null && nonSeamenItinerary.getPricingInformation().getTotalPriceValue() != null
                                && nonSeamenItinerary.getPricingInformation().getTotalPriceValue().longValue() <= mainFlightItinerary.getPricingInformation().getTotalPriceValue().longValue())) {
                            mainFlightItinerary.setPricingInformation(nonSeamenItinerary.getPricingInformation());
                            mainFlightItinerary.setNonSeamenJourneyList(nonSeamenItinerary.getJourneyList());
                            mainFlightItinerary.setAmadeusOfficeId(nonSeamenItinerary.getAmadeusOfficeId());
                            //compareItinerary(mainFlightItinerary,nonSeamenItinerary,false, provider);
                        }
                        allFightItineraries.put(hashKey, mainFlightItinerary);
                        nonSeamenFareHash.remove(hashKey);
                    }
                }
                if(allFightItineraries.containsKey(1521756448) || allFightItineraries.containsKey(1521758370) ){
                    FlightItinerary mainFlightItinerary = allFightItineraries.get(1521756448);
                    FlightItinerary mainFlightItinerary1 = allFightItineraries.get(1521758370);
                    if(mainFlightItinerary != null){
                        System.out.println("***** 4-: hashkey:"+ 1521756448 + "   mainItinerary: "+ mainFlightItinerary.getAmadeusOfficeId() +"  main_price:"+mainFlightItinerary.getSeamanPricingInformation().getTotalPriceValue() );
                    }
                    if(mainFlightItinerary1 != null){
                        System.out.println("***** 4-: hashkey:"+ 1521758370 + "   mainItinerary: "+ mainFlightItinerary1.getAmadeusOfficeId() +"  main_price:"+mainFlightItinerary1.getSeamanPricingInformation().getTotalPriceValue() );
                    }
                    int t = 0;
                }
                ConcurrentHashMap<Integer, FlightItinerary> list = mergeSeamenAndNonSeamenResults(new ConcurrentHashMap<Integer, FlightItinerary>(), airSolution);
                allFightItineraries.putAll(list);
                if(allFightItineraries.containsKey(1521756448) || allFightItineraries.containsKey(1521758370) ){
                    FlightItinerary mainFlightItinerary = allFightItineraries.get(1521756448);
                    FlightItinerary mainFlightItinerary1 = allFightItineraries.get(1521758370);
                    if(mainFlightItinerary != null){
                        System.out.println("***** 5-: hashkey:"+ 1521756448 + "   mainItinerary: "+ mainFlightItinerary.getAmadeusOfficeId() +"  main_price:"+mainFlightItinerary.getSeamanPricingInformation().getTotalPriceValue() );
                    }
                    if(mainFlightItinerary1 != null){
                        System.out.println("***** 5-: hashkey:"+ 1521758370 + "   mainItinerary: "+ mainFlightItinerary1.getAmadeusOfficeId() +"  main_price:"+mainFlightItinerary1.getSeamanPricingInformation().getTotalPriceValue() );
                    }
                    int t = 0;
                }
            }
        }catch (Exception e){
            logger.error("MergeResults:: ex:"+ e.getMessage());
        }
    }

    public ConcurrentHashMap<Integer, FlightItinerary> mergeSeamenAndNonSeamenResults(ConcurrentHashMap<Integer, FlightItinerary> allFightItineraries, AirSolution airSolution) {
//        if (airSolution.getSeamenHashMap() == null || airSolution.getSeamenHashMap().isEmpty()) {
//            allFightItineraries.putAll(airSolution.getNonSeamenHashMap());
//            return allFightItineraries;
//        } else if (airSolution.getNonSeamenHashMap() == null || airSolution.getNonSeamenHashMap().isEmpty()) {
//            for (Entry<Integer, FlightItinerary> entry : airSolution.getSeamenHashMap().entrySet()) {
//                FlightItinerary itinerary = entry.getValue();
//                itinerary.setSeamanPricingInformation(itinerary.getPricingInformation());
//            }
//            allFightItineraries.putAll(airSolution.getSeamenHashMap());
//            return allFightItineraries;
//        }
//
//        ConcurrentHashMap<Integer, FlightItinerary> nonSeamenFareHash = airSolution.getNonSeamenHashMap();
//        allFightItineraries.putAll(nonSeamenFareHash);
        //System.out.println("\n\n before mergeSeamenAndNonSeamenResults");
        //AmadeusFlightSearch.printHashmap(allFightItineraries,false);

        if (airSolution.getNonSeamenHashMap() != null && !airSolution.getNonSeamenHashMap().isEmpty()) {
            ConcurrentHashMap<Integer, FlightItinerary> nonSeamenFareHash = airSolution.getNonSeamenHashMap();
            allFightItineraries.putAll(nonSeamenFareHash);
        }
        if (airSolution.getSeamenHashMap() != null && !airSolution.getSeamenHashMap().isEmpty()) {
            ConcurrentHashMap<Integer, FlightItinerary> seamenFareHash = airSolution.getSeamenHashMap();
            for (Integer hashKey : seamenFareHash.keySet()) {
                if(hashKey == 1521758370 || hashKey == 1521756448){
                    FlightItinerary mainFlightItinerary = seamenFareHash.get(hashKey);
                    if(mainFlightItinerary != null){
                        System.out.println("***** 1: hashkey:"+ hashKey + "   mainItinerary: "+ mainFlightItinerary.getAmadeusOfficeId() +"  main_price:"+mainFlightItinerary.getPricingInformation().getTotalPriceValue() );
                    }
                    int t = 0;
                }
                FlightItinerary seamenItinerary = null;
                if (allFightItineraries.containsKey(hashKey)) {
                    seamenItinerary = seamenFareHash.get(hashKey);
                    seamenItinerary.setPriceOnlyPTC(true);
                    seamenItinerary.setPricingMessage(seamenFareHash.get(hashKey).getPricingMessage());
                    seamenItinerary.setSeamanPricingInformation(seamenFareHash.get(hashKey).getPricingInformation());
                    seamenItinerary.setPricingInformation(allFightItineraries.get(hashKey).getPricingInformation());
                    seamenItinerary.setNonSeamenJourneyList(allFightItineraries.get(hashKey).getJourneyList());
                    allFightItineraries.put(hashKey, seamenItinerary);
                } else {
                    seamenItinerary = seamenFareHash.get(hashKey);
                    seamenItinerary.setPriceOnlyPTC(true);
                    seamenItinerary.setSeamanPricingInformation(seamenItinerary.getPricingInformation());
                    seamenItinerary.setPricingInformation(null);
                    allFightItineraries.put(hashKey, seamenItinerary);
                }
            }
        }
        //System.out.println("\n\n after mergeSeamenAndNonSeamenResults");
        //AmadeusFlightSearch.printHashmap(allFightItineraries,false);
        if(allFightItineraries.containsKey(1521756448) || allFightItineraries.containsKey(1521758370) ){
            FlightItinerary mainFlightItinerary = allFightItineraries.get(1521756448);
            FlightItinerary mainFlightItinerary1 = allFightItineraries.get(1521758370);
            if(mainFlightItinerary != null){
                System.out.println("***** 2-: hashkey:"+ 1521756448 + "   mainItinerary: "+ mainFlightItinerary.getAmadeusOfficeId() +"  main_price:"+mainFlightItinerary.getSeamanPricingInformation().getTotalPriceValue() );
            }
            if(mainFlightItinerary1 != null){
                System.out.println("***** 2-: hashkey:"+ 1521758370 + "   mainItinerary: "+ mainFlightItinerary1.getAmadeusOfficeId() +"  main_price:"+mainFlightItinerary1.getSeamanPricingInformation().getTotalPriceValue() );
            }
            int t = 0;
        }
        return allFightItineraries;
    }

//    private void compareItinerary(FlightItinerary mainFlightItinerary, FlightItinerary itinerary, boolean isSeaman, String provider){
//        if(provider.equalsIgnoreCase("Amadeus")){
//            AmadeusSourceOfficeService.EOffice_source office_type = AmadeusSourceOfficeService.EOffice_source.fromString(itinerary.getAmadeusOfficeId());
//            //logger.debug("office Id:"+ itinerary.getAmadeusOfficeId()+ "  ::itinerary-TotalPriceValue():"+ itinerary.getPricingInformation().getTotalPriceValue().longValue()+ "   ::mainItinerary:price:"+ itinerary.getPricingInformation().getTotalPriceValue().longValue());
//            switch (office_type){
//                case eMumbai_id:
//                    mainFlightItinerary.setAmadeusOfficeId(itinerary.getAmadeusOfficeId());
//                    break;
//                case eDelhi_id:
//                    if(isSeaman) {
//                        if (itinerary.getPricingInformation().getTotalPriceValue().longValue() < mainFlightItinerary.getSeamanPricingInformation().getTotalPriceValue().longValue()) {
//                            mainFlightItinerary.setAmadeusOfficeId(itinerary.getAmadeusOfficeId());
//                        }
//                    }else{
//                        if (itinerary.getPricingInformation().getTotalPriceValue().longValue() < mainFlightItinerary.getPricingInformation().getTotalPriceValue().longValue()) {
//                            mainFlightItinerary.setAmadeusOfficeId(itinerary.getAmadeusOfficeId());
//                        }
//                    }
//                    break;
//                case eBenzy_id:
//                    if(isSeaman)
//                        if (itinerary.getPricingInformation().getTotalPriceValue().longValue() < mainFlightItinerary.getSeamanPricingInformation().getTotalPriceValue().longValue()) {
//                            mainFlightItinerary.setAmadeusOfficeId(itinerary.getAmadeusOfficeId());
//                        }
//
//                        else{
//                            //todo travelmatrix
//                            //if(itinerary.getJourneyList().get(0).getAirSegmentList().get(0).getCarrierCode().equalsIgnoreCase())//airlinesStrForFilter
//                            if (itinerary.getPricingInformation().getTotalPriceValue().longValue() < mainFlightItinerary.getPricingInformation().getTotalPriceValue().longValue()) {
//                                mainFlightItinerary.setAmadeusOfficeId(itinerary.getAmadeusOfficeId());
//                            }
//                        }
//                    break;
//                default:
//                    break;
//            }
//        }
//        else if(provider.equalsIgnoreCase("Travelport")){ }
//    }

//    public void mergeResults_(ConcurrentHashMap<Integer, FlightItinerary> allFightItineraries, SearchResponse searchResponse) {
//        AirSolution airSolution = searchResponse.getAirSolution();
//        String provider = searchResponse.getProvider();
//        FlightSearchOffice office = searchResponse.getFlightSearchOffice();
//        if(allFightItineraries.isEmpty()) {
//            mergeSeamenAndNonSeamenResults(allFightItineraries, airSolution);
//        } else {
//            ConcurrentHashMap<Integer, FlightItinerary> seamenFareHash = airSolution.getSeamenHashMap();
//            ConcurrentHashMap<Integer, FlightItinerary> nonSeamenFareHash = airSolution.getNonSeamenHashMap();
//
//            for(Integer hashKey : allFightItineraries.keySet()){
//                if(seamenFareHash == null || nonSeamenFareHash == null){
//                    logger.debug("==================================NULL POINTER EXECEPTION============"+ searchResponse.getProvider()+Json.toJson(searchResponse));
//                    break;
//                }
//                if(seamenFareHash.containsKey(hashKey) && nonSeamenFareHash.containsKey(hashKey)){
//                    FlightItinerary mainFlightItinerary = allFightItineraries.get(hashKey);
//                    if(mainFlightItinerary.getSeamanPricingInformation() == null ||
//                            mainFlightItinerary.getSeamanPricingInformation().getTotalPriceValue() == null
//                            || seamenFareHash.get(hashKey).getPricingInformation().getTotalPriceValue().longValue() < mainFlightItinerary.getSeamanPricingInformation().getTotalPriceValue().longValue()){
//                        mainFlightItinerary.setSeamanPricingInformation(seamenFareHash.get(hashKey).getPricingInformation());
//                        mainFlightItinerary.setJourneyList(seamenFareHash.get(hashKey).getJourneyList());
//                    }
//                    if(mainFlightItinerary.getPricingInformation() == null || nonSeamenFareHash.get(hashKey).getPricingInformation().getTotalPriceValue().longValue() < mainFlightItinerary.getPricingInformation().getTotalPriceValue().longValue()){
//                        mainFlightItinerary.setPricingInformation(nonSeamenFareHash.get(hashKey).getPricingInformation());
//                        mainFlightItinerary.setNonSeamenJourneyList(nonSeamenFareHash.get(hashKey).getJourneyList());
//                    }
//                    seamenFareHash.remove(hashKey);
//                    nonSeamenFareHash.remove(hashKey);
//                    allFightItineraries.put(hashKey, mainFlightItinerary);
//                } else if(seamenFareHash.containsKey(hashKey)){
//                    FlightItinerary mainFlightItinerary = allFightItineraries.get(hashKey);
//                    if(mainFlightItinerary.getSeamanPricingInformation() == null ||
//                            mainFlightItinerary.getSeamanPricingInformation().getTotalPriceValue() == null ||
//                            seamenFareHash.get(hashKey).getPricingInformation().getTotalPriceValue().longValue() < mainFlightItinerary.getSeamanPricingInformation().getTotalPriceValue().longValue()){
//                        mainFlightItinerary.setSeamanPricingInformation(seamenFareHash.get(hashKey).getPricingInformation());
//                        mainFlightItinerary.setJourneyList(seamenFareHash.get(hashKey).getJourneyList());
//                    }
//                    allFightItineraries.put(hashKey, mainFlightItinerary);
//                    seamenFareHash.remove(hashKey);
//                } else if(nonSeamenFareHash.containsKey(hashKey)){
//                    FlightItinerary mainFlightItinerary = allFightItineraries.get(hashKey);
//                    if(mainFlightItinerary.getPricingInformation() == null ||
//                            mainFlightItinerary.getPricingInformation().getTotalPriceValue() == null ||
//                            nonSeamenFareHash.get(hashKey).getPricingInformation().getTotalPriceValue().longValue() < mainFlightItinerary.getPricingInformation().getTotalPriceValue().longValue()){
//                        mainFlightItinerary.setPricingInformation(nonSeamenFareHash.get(hashKey).getPricingInformation());
//                        mainFlightItinerary.setNonSeamenJourneyList(nonSeamenFareHash.get(hashKey).getJourneyList());
//                    }
//                    allFightItineraries.put(hashKey,mainFlightItinerary);
//                    nonSeamenFareHash.remove(hashKey);
//                }
//            }
//            ConcurrentHashMap<Integer, FlightItinerary> list = mergeSeamenAndNonSeamenResults(new ConcurrentHashMap<Integer, FlightItinerary>(), airSolution);
//            allFightItineraries.putAll(list);
//        }
//    }

//    public ConcurrentHashMap<Integer, FlightItinerary> mergeSeamenAndNonSeamenResults_(ConcurrentHashMap<Integer, FlightItinerary> allFightItineraries, AirSolution airSolution) {
//        if(airSolution.getSeamenHashMap() == null || airSolution.getSeamenHashMap().isEmpty()){
//            allFightItineraries.putAll(airSolution.getNonSeamenHashMap());
//            return allFightItineraries;
//        } else if(airSolution.getNonSeamenHashMap() == null || airSolution.getNonSeamenHashMap().isEmpty()){
//        	for (Entry<Integer, FlightItinerary> entry : airSolution.getSeamenHashMap().entrySet()) {
//        		FlightItinerary itinerary = entry.getValue();
//        		itinerary.setSeamanPricingInformation(itinerary.getPricingInformation());
//        	}
//            allFightItineraries.putAll(airSolution.getSeamenHashMap());
//            return allFightItineraries;
//        }
//        ConcurrentHashMap<Integer, FlightItinerary> seamenFareHash = airSolution.getSeamenHashMap();
//        ConcurrentHashMap<Integer, FlightItinerary> nonSeamenFareHash = airSolution.getNonSeamenHashMap();
//        allFightItineraries.putAll(nonSeamenFareHash);
//
//        for (Integer hashKey : seamenFareHash.keySet()) {
//            FlightItinerary seamenItinerary = null;
//            if (allFightItineraries.containsKey(hashKey)) {
//                seamenItinerary = seamenFareHash.get(hashKey);
//                seamenItinerary.setPriceOnlyPTC(true);
//                seamenItinerary.setPricingMessage(seamenFareHash.get(hashKey).getPricingMessage());
//                seamenItinerary.setSeamanPricingInformation(seamenFareHash.get(hashKey).getPricingInformation());
//                seamenItinerary.setPricingInformation(allFightItineraries.get(hashKey).getPricingInformation());
//                seamenItinerary.setNonSeamenJourneyList(allFightItineraries.get(hashKey).getJourneyList());
//                allFightItineraries.put(hashKey, seamenItinerary);
//            } else {
//                seamenItinerary = seamenFareHash.get(hashKey);
//                seamenItinerary.setPriceOnlyPTC(true);
//                seamenItinerary.setSeamanPricingInformation(seamenItinerary.getPricingInformation());
//                seamenItinerary.setPricingInformation(null);
//                allFightItineraries.put(hashKey, seamenItinerary);
//            }
//        }
//        return allFightItineraries;
//    }

}
