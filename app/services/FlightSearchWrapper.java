package services;

import com.compassites.constants.CacheConstants;
import com.compassites.exceptions.RetryException;
import com.compassites.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import play.Logger;
import play.libs.Json;
import redis.clients.jedis.Jedis;
import utils.ErrorMessageHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.*;

/**
 * Created by user on 17-06-2014.
 */
@Service
public class FlightSearchWrapper {

    @Autowired
    private List<FlightSearch> flightSearchList;


    public List<SearchResponse> search(final SearchParameters searchParameters) {
        final String redisKey = searchParameters.redisKey();
        Logger.info("***********SEARCH STARTED key: ["+ redisKey +"]***********");
        SearchResponse searchResponseList = new SearchResponse();
        int maxThreads = 5;
        int queueSize = 100;
        final Jedis j = new Jedis("localhost", 6379);
        j.connect();

        j.setex(redisKey + ":status", CacheConstants.CACHE_TIMEOUT_IN_SECS, "started");

        ExecutorService newExecutor = new ThreadPoolExecutor(maxThreads, maxThreads, Long.MAX_VALUE, TimeUnit.NANOSECONDS, new ArrayBlockingQueue<Runnable>(queueSize, true));
        List<Future<SearchResponse>> futureSearchResponseList = new ArrayList<>();
        List<ErrorMessage> errorMessageList = new ArrayList<>();
        for (final FlightSearch flightSearch: flightSearchList){
            final String providerStatusCacheKey = redisKey + flightSearch.provider() + "status";

            try {
                System.out.println("Flight Search Provider["+redisKey+"] : "+flightSearch.provider());
                //Call provider if response is not already present;

                if (!checkOrSetStatus(j, providerStatusCacheKey)) {
                    futureSearchResponseList.add(newExecutor.submit(new Callable<SearchResponse>() {
                        public SearchResponse call() throws Exception {
                            SearchResponse response = flightSearch.search(searchParameters);
                            Logger.info("[" + redisKey + "]Response from provider:" + flightSearch.provider());
                            checkResponseAndSetStatus(response, providerStatusCacheKey, j);
                            return response;
                        }
                    }));
                }

            } catch (Exception e) {
                checkResponseAndSetStatus(null, providerStatusCacheKey, j);
                Logger.info("["+redisKey+"]Response from provider:" +flightSearch.provider());
                e.printStackTrace();
            }
        }

        Logger.info("["+redisKey+"] : " + futureSearchResponseList.size()+ "Threads initiated");
        int counter = 0;
        boolean loop = true;
        int searchResponseListSize = futureSearchResponseList.size();
        int exceptionCounter = 0;
        HashMap<Integer,FlightItinerary> hashMap =  new HashMap<>();
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
                    String res = j.set(searchParameters.redisKey(), Json.stringify(Json.toJson(searchResponseList)));

                    res = j.set(searchParameters.redisKey()+":status","partial");*/
                    listIterator.remove();
                    counter++;

                    if(searchResponse != null){
                        Logger.info("["+redisKey+"]Received Response "+ counter +" from : " + searchResponse.getProvider()+" Size: " + searchResponse.getAirSolution().getFlightItineraryList().size());
                        SearchResponse searchResponseCache=new SearchResponse();
                        if(counter == 1){
                            searchResponseCache =  searchResponse;
                            searchResponseList  = searchResponseCache;
                            //hashSet.addAll(searchResponseList.get(0).getAirSolution().getFlightItineraryList());
                            for (FlightItinerary flightItinerary : searchResponse.getAirSolution().getFlightItineraryList()){

                                hashMap.put(flightItinerary.hashCode(),flightItinerary);
                            }
                            errorMessageList.addAll(searchResponse.getErrorMessageList());
                        }
                        if (counter > 1) {
                            //Logger.info("counter :"+counter+"Search Response FligthItinary Size: "+searchResponse.getAirSolution().getFlightItineraryList().size());
                            for (FlightItinerary flightItinerary : searchResponse.getAirSolution().getFlightItineraryList()) {
                                //Logger.info("FlightItinary string :"+flightItinerary.toString());
                                if(hashMap.containsKey(flightItinerary.hashCode())){
                                    //Logger.info("Common Flights"+Json.toJson(flightItinerary));
                                    FlightItinerary hashFlightItinerary = hashMap.get(flightItinerary.hashCode());
                                    if (hashFlightItinerary.getPricingInformation() != null && hashFlightItinerary.getPricingInformation().getTotalPrice() != null
                                            && flightItinerary.getPricingInformation() != null && flightItinerary.getPricingInformation().getTotalPrice() != null) {
                                        Integer hashItinaryPrice = new Integer(hashFlightItinerary.getPricingInformation().getTotalPrice().substring(3));
                                        Integer iteratorItinaryPrice = new Integer(flightItinerary.getPricingInformation().getTotalPrice().substring(3));
                                        if (iteratorItinaryPrice < hashItinaryPrice) {
                                            hashMap.remove(hashFlightItinerary.hashCode());
                                            hashMap.put(flightItinerary.hashCode(), flightItinerary);
                                        }
                                    }
                                } else {
                                    hashMap.put(flightItinerary.hashCode(), flightItinerary);
                                }
                            }
                            errorMessageList.addAll(searchResponse.getErrorMessageList());
                            AirSolution airSolution = new AirSolution();
                            airSolution.setFlightItineraryList(new ArrayList<FlightItinerary>(hashMap.values()));
                            searchResponseCache.setAirSolution(airSolution);
                            searchResponseCache.getErrorMessageList().addAll(searchResponse.getErrorMessageList());

                        }

                        //searchResponseList.add(searchResponseCache);
                        searchResponseCache.setErrorMessageList(errorMessageList);
                        String res = j.setex(searchParameters.redisKey(), CacheConstants.CACHE_TIMEOUT_IN_SECS, Json.stringify(Json.toJson(searchResponseCache)));
                        res = j.setex(searchParameters.redisKey()+":status", CacheConstants.CACHE_TIMEOUT_IN_SECS, "partial");

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
                    String res = j.setex(searchParameters.redisKey(), CacheConstants.CACHE_TIMEOUT_IN_SECS, Json.stringify(Json.toJson(searchResponse)));
                }
                j.setex(searchParameters.redisKey()+":status",CacheConstants.CACHE_TIMEOUT_IN_SECS, "complete");

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

    private void checkResponseAndSetStatus(SearchResponse response, String providerStatusCacheKey, Jedis j) {
        String status = "invalid";
        if (validResponse(response))
            status = "success";
        setCacheValue(j, providerStatusCacheKey, status);
    }

    private Boolean checkOrSetStatus(Jedis j, String key){
        String status = j.get(key);
        if (status != null){
            return status.equalsIgnoreCase("success");
        }

        setCacheValue(j, key, "in-progress");
        return false;
    }

    private void setCacheValue(Jedis j, String key, String value){
        j.setex(key, CacheConstants.CACHE_TIMEOUT_IN_SECS, value);
    }

}
