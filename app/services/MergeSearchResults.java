package services;

import akka.dispatch.Futures;
import com.compassites.constants.CacheConstants;
import com.compassites.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import play.libs.Akka;
import play.libs.Json;
import scala.Option;
import scala.concurrent.ExecutionContext;
import scala.util.Try;
import utils.ErrorMessageHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by yaseen on 09-02-2015.
 */
@Service
public class MergeSearchResults {

    @Autowired
    private List<FlightSearch> flightSearchList;

    @Autowired
    private RedisTemplate redisTemplate;

    static Logger logger = LoggerFactory.getLogger("gds");

    public void searchAndMerge( final SearchParameters searchParameters){

        final String redisKey = searchParameters.redisKey();
        SearchResponse searchResponseList = new SearchResponse();
        List<scala.concurrent.Future<SearchResponse>> futureSearchResponseList = new ArrayList<>();
        List<ErrorMessage> errorMessageList = new ArrayList<>();
        ConcurrentHashMap<Integer,FlightItinerary> hashMap =  new ConcurrentHashMap<>();

//        MessageDispatcher messageDispatcher = play.libs.Akka.system().dispatchers().lookup("search");
        ExecutionContext myExecutionContext = Akka.system().dispatchers().lookup("play.akka.actor.search");
//        messageDispatcher.

//        ExecutionContext myEc = HttpExecution.fromThread(messageDispatcher);


        for (final FlightSearch flightSearch: flightSearchList) {
            //if( !(searchParameters.getBookingType() == BookingType.SEAMEN && flightSearch.provider().equals("Mystifly")) ) {
            final String providerStatusCacheKey = redisKey + flightSearch.provider() + "status";

            try {
                logger.debug("Flight Search Provider["+redisKey+"] : "+flightSearch.provider());

                //Call provider if response is not already present;
                if (!checkOrSetStatus(providerStatusCacheKey)) {
                    futureSearchResponseList.add(Futures.future(new Callable<SearchResponse>() {
                        public SearchResponse call() throws Exception {
                            SearchResponse response = flightSearch.search(searchParameters);
                            logger.debug("[" + redisKey + "]Response from provider:" + flightSearch.provider());
                            checkResponseAndSetStatus(response, providerStatusCacheKey);
                            return response;
                        }
                    },myExecutionContext));
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
            ListIterator<scala.concurrent.Future<SearchResponse>> listIterator = futureSearchResponseList.listIterator();
            while (listIterator.hasNext()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                scala.concurrent.Future<SearchResponse> future = listIterator.next();
                SearchResponse searchResponse = null;

                if(future.isCompleted()){
                    try {
                        Option<Try<SearchResponse>> searchResponses = future.value();
                        searchResponse = searchResponses.get().get();
                    }catch (Exception e){

                        ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage("partialResults",ErrorMessage.ErrorType.WARNING,"Application");
                        errorMessageList.add(errorMessage);
                        e.printStackTrace();
                    }
                    listIterator.remove();
                    counter++;

                    if(searchResponse != null){
                        logger.debug("["+redisKey+"]Received Response "+ counter +" from : " + searchResponse.getProvider()+" Size: " + searchResponse.getAirSolution().getFlightItineraryList().size());
                        SearchResponse searchResponseCache=new SearchResponse();

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
                        logger.debug("["+redisKey+"]Added response to final hashmap"+ counter +" from : " + searchResponse.getProvider()+": hashmap size: "+ searchResponseCache.getAirSolution().getFlightItineraryList().size());
                    }
                    else
                    {
                        logger.debug("["+redisKey+"]Received Response "+ counter +" Null" );
                    }

                }
            }

//            newExecutor.shutdown();
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
                logger.debug("***********SEARCH END key: ["+ redisKey +"]***********");
            }
        }
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
        redisTemplate.expire( key, CacheConstants.CACHE_TIMEOUT_IN_SECS, TimeUnit.SECONDS );
    }



    public void mergeResults(ConcurrentHashMap<Integer, FlightItinerary> allFightItineraries, SearchResponse searchResponse) {
        AirSolution airSolution = searchResponse.getAirSolution();
        if(allFightItineraries.isEmpty()) {
            mergeSeamenAndNonSeamenResults(allFightItineraries, airSolution);
        } else {
            ConcurrentHashMap<Integer, FlightItinerary> seamenFareHash = airSolution.getSeamenHashMap();
            ConcurrentHashMap<Integer, FlightItinerary> nonSeamenFareHash = airSolution.getNonSeamenHashMap();

            for(Integer hashKey : allFightItineraries.keySet()){
                if(seamenFareHash == null || nonSeamenFareHash == null){
                    logger.debug("==================================NULL POINTER EXECEPTION============"+ searchResponse.getProvider()+Json.toJson(searchResponse));
                }
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
            ConcurrentHashMap<Integer, FlightItinerary> list = mergeSeamenAndNonSeamenResults(new ConcurrentHashMap<Integer, FlightItinerary>(), airSolution);
            allFightItineraries.putAll(list);
        }
    }


    public ConcurrentHashMap<Integer, FlightItinerary> mergeSeamenAndNonSeamenResults(ConcurrentHashMap<Integer, FlightItinerary> allFightItineraries, AirSolution airSolution) {
        if(airSolution.getSeamenHashMap() == null || airSolution.getSeamenHashMap().isEmpty()){
            allFightItineraries.putAll(airSolution.getNonSeamenHashMap());
            return allFightItineraries;
        } else if(airSolution.getNonSeamenHashMap() == null || airSolution.getNonSeamenHashMap().isEmpty()){
            for (Map.Entry<Integer, FlightItinerary> entry : airSolution.getSeamenHashMap().entrySet()) {
                FlightItinerary itinerary = entry.getValue();
                itinerary.setSeamanPricingInformation(itinerary.getPricingInformation());
            }
            allFightItineraries.putAll(airSolution.getSeamenHashMap());
            return allFightItineraries;
        }
        ConcurrentHashMap<Integer, FlightItinerary> seamenFareHash = airSolution.getSeamenHashMap();
        ConcurrentHashMap<Integer, FlightItinerary> nonSeamenFareHash = airSolution.getNonSeamenHashMap();
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
