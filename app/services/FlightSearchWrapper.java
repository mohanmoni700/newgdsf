package services;

import com.compassites.constants.CacheConstants;
import com.compassites.exceptions.RetryException;
import com.compassites.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import play.Logger;
import play.libs.Json;
import utils.ErrorMessageHelper;

import java.util.ArrayList;
import java.util.HashMap;
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

    public List<SearchResponse> search(final SearchParameters searchParameters) {
        final String redisKey = searchParameters.redisKey();
        Logger.info("***********SEARCH STARTED key: ["+ redisKey +"]***********");
        SearchResponse searchResponseList = new SearchResponse();
        int maxThreads = 5;
        int queueSize = 100;

        redisTemplate.opsForValue().set(redisKey + ":status", "started");
        redisTemplate.expire(redisKey,CacheConstants.CACHE_TIMEOUT_IN_SECS,TimeUnit.SECONDS);
        ExecutorService newExecutor = new ThreadPoolExecutor(maxThreads, maxThreads, Long.MAX_VALUE, TimeUnit.NANOSECONDS, new ArrayBlockingQueue<Runnable>(queueSize, true));
        List<Future<SearchResponse>> futureSearchResponseList = new ArrayList<>();
        List<ErrorMessage> errorMessageList = new ArrayList<>();
        HashMap<Integer,FlightItinerary> hashMap =  new HashMap<>();
        for (final FlightSearch flightSearch: flightSearchList) {
        	//if( !(searchParameters.getBookingType() == BookingType.SEAMEN && flightSearch.provider().equals("Mystifly")) ) {
	            final String providerStatusCacheKey = redisKey + flightSearch.provider() + "status";
	
	            try {
	                System.out.println("Flight Search Provider["+redisKey+"] : "+flightSearch.provider());
	                
	                //Call provider if response is not already present;
	                if (!checkOrSetStatus(providerStatusCacheKey)) {
	                    futureSearchResponseList.add(newExecutor.submit(new Callable<SearchResponse>() {
	                        public SearchResponse call() throws Exception {
	                            SearchResponse response = flightSearch.search(searchParameters);
	                            Logger.info("[" + redisKey + "]Response from provider:" + flightSearch.provider());
	                            checkResponseAndSetStatus(response, providerStatusCacheKey);
	                            return response;
	                        }
	                    }));
	                } else {
	                    String cachedResponse = (String) redisTemplate.opsForValue().get(redisKey);
	                    JsonNode rs = null;
	                    if (cachedResponse != null)
	                        rs = Json.parse(cachedResponse);
	                    SearchResponse chachedRespons = null;
	                    if (rs != null){
	                        SearchResponse chachedResponse = Json.fromJson(rs, SearchResponse.class);
	                        for(FlightItinerary flightItinerary : chachedResponse.getAirSolution().getFlightItineraryList()){
	                            hashMap.put(flightItinerary.hashCode(),flightItinerary);
	                        }
	                    }
	                }
	            } catch (Exception e) {
	                checkResponseAndSetStatus(null, providerStatusCacheKey);
	                Logger.info("["+redisKey+"]Response from provider:" +flightSearch.provider());
	                e.printStackTrace();
	            }
        	//}
        }

        Logger.info("["+redisKey+"] : " + futureSearchResponseList.size()+ "Threads initiated");
        int counter = 0;
        boolean loop = true;
        int searchResponseListSize = futureSearchResponseList.size();
        int exceptionCounter = 0;

        while(loop){
            ListIterator<Future<SearchResponse>> listIterator = futureSearchResponseList.listIterator();
            while (listIterator.hasNext()) {
                Future<SearchResponse> future = listIterator.next();
                SearchResponse searchResponse = null;

                if(future.isDone()){
                    try {
                        searchResponse = future.get();
                    } catch (RetryException retryOnFailure) {
                        exceptionCounter++;

                        if(exceptionCounter == flightSearchList.size()){
                            Logger.error("["+redisKey+"]All providers gave error");
                            //send email to IT admin
                        }
                        ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage("retrialError",ErrorMessage.ErrorType.ERROR,"Application");
                        errorMessageList.add(errorMessage);
                    }catch (Exception e){

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

                    Logger.info("Redis Key :" + searchParameters.redisKey());
                    String res = je(searchParameters.redisKey(), Json.stringify(Json.toJson(searchResponseList)));

                    res = j.set(searchParameters.redisKey()+":status","partial");*/
                    listIterator.remove();
                    counter++;

                    if(searchResponse != null){
                        Logger.info("["+redisKey+"]Received Response "+ counter +" from : " + searchResponse.getProvider()+" Size: " + searchResponse.getAirSolution().getFlightItineraryList().size());
                        SearchResponse searchResponseCache=new SearchResponse();

                        //Logger.info("counter :"+counter+"Search Response FligthItinary Size: "+searchResponse.getAirSolution().getFlightItineraryList().size());
                       /* for (FlightItinerary flightItinerary : searchResponse.getAirSolution().getFlightItineraryList()) {
                            //Logger.info("FlightItinary string :"+flightItinerary.toString());
                            if(hashMap.containsKey(flightItinerary.hashCode())){
                                //Logger.info("Common Flights"+Json.toJson(flightItinerary));
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
                        mergeResults(hashMap, searchResponse);

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
                        searchResponseList = searchResponseCache;
                        Logger.info("["+redisKey+"]Added response to final hashmap"+ counter +" from : " + searchResponse.getProvider()+": hashmap size: "+ searchResponseCache.getAirSolution().getFlightItineraryList().size());
                    }
                    else
                    {
                        Logger.info("["+redisKey+"]Received Response "+ counter +" Null" );
                    }

                }
            }
            if(counter == searchResponseListSize){

                loop = false;
                if(hashMap.size() == 0 && errorMessageList.size() > 0)  {
                    SearchResponse searchResponse = new SearchResponse();
                    searchResponse.setErrorMessageList(errorMessageList);
                    redisTemplate.opsForValue().set(searchParameters.redisKey(), Json.stringify(Json.toJson(searchResponse)));
                    redisTemplate.expire(searchParameters.redisKey(),CacheConstants.CACHE_TIMEOUT_IN_SECS,TimeUnit.SECONDS);
                }
                redisTemplate.opsForValue().set(searchParameters.redisKey()+":status", "complete");
                redisTemplate.expire(searchParameters.redisKey()+":status",CacheConstants.CACHE_TIMEOUT_IN_SECS,TimeUnit.SECONDS);
                Logger.info("***********SEARCH END key: ["+ redisKey +"]***********");
            }
        }
        /*
        Logger.info("HashMap Size: "+hashMap.size());
        SearchResponse searchResponse=new SearchResponse();
        searchResponse.setErrorMessageList(errorMessageList);
        AirSolution airSolution = new AirSolution();
        airSolution.setFlightItineraryList(new ArrayList<FlightItinerary>(hashMap.values()));
        searchResponse.setAirSolution(airSolution);
        searchResponseList.add(searchResponse);
        Logger.info("***********SEARCH END***********");
        */
        return null;
    }

    private boolean validResponse(SearchResponse response){
        if ((response != null) && (response.getAirSolution() != null)){
            if (response.getAirSolution().getFlightItineraryList() != null){
                return (response.getAirSolution().getFlightItineraryList().size() > 0);
            }
        }
        return false;
    }

    private void checkResponseAndSetStatus(SearchResponse response, String providerStatusCacheKey) {
        String status = "invalid";
        if (validResponse(response))
            status = "success";
        setCacheValue(providerStatusCacheKey, status);
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

    public void mergeResults(HashMap<Integer, FlightItinerary> allFightItineraries, SearchResponse searchResponse) {
    	AirSolution airSolution = searchResponse.getAirSolution();
        if(allFightItineraries.isEmpty()) {
            mergeSeamenAndNonSeamenResults(allFightItineraries, airSolution);
        } else {
            HashMap<Integer, FlightItinerary> seamenFareHash = airSolution.getSeamenHashMap();
            HashMap<Integer, FlightItinerary> nonSeamenFareHash = airSolution.getNonSeamenHashMap();

            for(Integer hashKey : allFightItineraries.keySet()){
                if(seamenFareHash.containsKey(hashKey) && nonSeamenFareHash.containsKey(hashKey)){
                    FlightItinerary mainFlightItinerary = allFightItineraries.get(hashKey);
                    if(mainFlightItinerary.getSeamanPricingInformation() == null ||
                            mainFlightItinerary.getSeamanPricingInformation().getTotalPriceValue() == null || seamenFareHash.get(hashKey).getPricingInformation().getTotalPriceValue().longValue() < mainFlightItinerary.getSeamanPricingInformation().getTotalPriceValue().longValue()){
                        mainFlightItinerary.setSeamanPricingInformation(seamenFareHash.get(hashKey).getPricingInformation());
                        mainFlightItinerary.setJourneyList(seamenFareHash.get(hashKey).getJourneyList());
                    }
                    if(mainFlightItinerary.getPricingInformation() == null || nonSeamenFareHash.get(hashKey).getPricingInformation().getTotalPriceValue().longValue() < mainFlightItinerary.getPricingInformation().getTotalPriceValue().longValue()){
                        mainFlightItinerary.setPricingInformation(nonSeamenFareHash.get(hashKey).getPricingInformation());
                        mainFlightItinerary.setNonSeamenJourneyList(nonSeamenFareHash.get(hashKey).getJourneyList());
                    }
                    seamenFareHash.remove(hashKey);
                    nonSeamenFareHash.remove(hashKey);
                    allFightItineraries.put(hashKey, mainFlightItinerary);
                } else if(seamenFareHash.containsKey(hashKey)){
                    FlightItinerary mainFlightItinerary = allFightItineraries.get(hashKey);
                    if(mainFlightItinerary.getSeamanPricingInformation() == null ||
                    		mainFlightItinerary.getSeamanPricingInformation().getTotalPriceValue() == null ||
                    		seamenFareHash.get(hashKey).getPricingInformation().getTotalPriceValue().longValue() < mainFlightItinerary.getSeamanPricingInformation().getTotalPriceValue().longValue()){
                        mainFlightItinerary.setSeamanPricingInformation(seamenFareHash.get(hashKey).getPricingInformation());
                        mainFlightItinerary.setJourneyList(seamenFareHash.get(hashKey).getJourneyList());
                    }
                    allFightItineraries.put(hashKey, mainFlightItinerary);
                    seamenFareHash.remove(hashKey);
                } else if(nonSeamenFareHash.containsKey(hashKey)){
                    FlightItinerary mainFlightItinerary = allFightItineraries.get(hashKey);
                    if(mainFlightItinerary.getPricingInformation() == null ||
                    		mainFlightItinerary.getPricingInformation().getTotalPriceValue() == null ||
                    		nonSeamenFareHash.get(hashKey).getPricingInformation().getTotalPriceValue().longValue() < mainFlightItinerary.getPricingInformation().getTotalPriceValue().longValue()){
                        mainFlightItinerary.setPricingInformation(nonSeamenFareHash.get(hashKey).getPricingInformation());
                        mainFlightItinerary.setNonSeamenJourneyList(nonSeamenFareHash.get(hashKey).getJourneyList());
                    }
                    allFightItineraries.put(hashKey,mainFlightItinerary);
                    nonSeamenFareHash.remove(hashKey);
                }
            }
            HashMap<Integer, FlightItinerary> list = mergeSeamenAndNonSeamenResults(new HashMap<Integer, FlightItinerary>(), airSolution);
            allFightItineraries.putAll(list);
        }
    }


    public HashMap<Integer, FlightItinerary> mergeSeamenAndNonSeamenResults(HashMap<Integer, FlightItinerary> allFightItineraries, AirSolution airSolution) {
        if(airSolution.getSeamenHashMap() == null || airSolution.getSeamenHashMap().isEmpty()){
            allFightItineraries.putAll(airSolution.getNonSeamenHashMap());
            return allFightItineraries;
        } else if(airSolution.getNonSeamenHashMap() == null || airSolution.getNonSeamenHashMap().isEmpty()){
        	for (Entry<Integer, FlightItinerary> entry : airSolution.getSeamenHashMap().entrySet()) { 
        		FlightItinerary itinerary = entry.getValue();
        		itinerary.setSeamanPricingInformation(itinerary.getPricingInformation());
        	}
            allFightItineraries.putAll(airSolution.getSeamenHashMap());
            return allFightItineraries;
        }
        HashMap<Integer, FlightItinerary> seamenFareHash = airSolution.getSeamenHashMap();
        HashMap<Integer, FlightItinerary> nonSeamenFareHash = airSolution.getNonSeamenHashMap();
        allFightItineraries.putAll(nonSeamenFareHash);
        
        for (Integer hashKey : seamenFareHash.keySet()) {
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
        return allFightItineraries;
    }
    
}
