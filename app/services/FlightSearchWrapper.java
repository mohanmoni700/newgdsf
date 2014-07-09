package services;

import com.compassites.exceptions.RetryException;
import com.compassites.model.FlightItinerary;
import com.compassites.model.SearchParameters;
import com.compassites.model.SearchResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import play.Logger;
import play.libs.Json;
import redis.clients.jedis.Jedis;

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
        Logger.info("***********SEARCH STARTED***********");
        List<SearchResponse> searchResponseList = new ArrayList<SearchResponse>();
        int maxThreads = 5;
        int queueSize = 100;
        ExecutorService newExecutor = new ThreadPoolExecutor(maxThreads, maxThreads, Long.MAX_VALUE, TimeUnit.NANOSECONDS, new ArrayBlockingQueue<Runnable>(queueSize, true));
        List<Future<SearchResponse>> futureSearchResponseList = new ArrayList<>();

        for (final FlightSearch flightSearch: flightSearchList){
            try {
               System.out.println("Flight Search : "+flightSearch.getClass());

                futureSearchResponseList.add(newExecutor.submit(new Callable<SearchResponse>() {
                    public SearchResponse call() throws Exception {
                        SearchResponse response = flightSearch.search(searchParameters);
                        return response;
                    }
                }));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Logger.info("Threads initiated");
        int counter = 0;
        boolean loop = true;
        int searchResponseListSize = futureSearchResponseList.size();
        int exceptionCounter = 0;
        Jedis j = new Jedis("localhost", 6379);
        j.connect();
        HashMap<Integer,FlightItinerary> hashMap =  new HashMap<>();
        while(loop){
            ListIterator<Future<SearchResponse>> listIterator = futureSearchResponseList.listIterator();
            while(listIterator.hasNext()) {
                Future<SearchResponse>  future = listIterator.next();
                SearchResponse searchResponse = null;
                if(future.isDone()){
                    try {
                        searchResponse = future.get();
                    }catch (RetryException retryOnFailure){
                        exceptionCounter++;
                        if(exceptionCounter == flightSearchList.size()){
                            //send email to IT admin
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }

                    if (searchResponseList.size() > 0){
                        searchResponseList.get(0).getAirSolution().getFlightItineraryList().addAll(searchResponse.getAirSolution().getFlightItineraryList()) ;
                    }
                    else {
                        searchResponseList.add(searchResponse);
                    }
                    listIterator.remove();
                    Logger.info("Redis Key :" + searchParameters.redisKey());
                    String res = j.set(searchParameters.redisKey(), Json.stringify(Json.toJson(searchResponseList)));

                    res = j.set(searchParameters.redisKey()+":status","partial");

                    counter ++;
                    if(counter == 1){
                        //hashSet.addAll(searchResponseList.get(0).getAirSolution().getFlightItineraryList());
                        Logger.info("counter :"+counter+"Search Response FligthItinary Size: "+searchResponse.getAirSolution().getFlightItineraryList().size());
                        for (FlightItinerary flightItinerary : searchResponse.getAirSolution().getFlightItineraryList()){

                            hashMap.put(flightItinerary.hashCode(),flightItinerary);
                        }
                    }
                    if(counter > 1){
                        Logger.info("counter :"+counter+"Search Response FligthItinary Size: "+searchResponse.getAirSolution().getFlightItineraryList().size());
                        for (FlightItinerary flightItinerary : searchResponse.getAirSolution().getFlightItineraryList()){
                            //Logger.info("FlightItinary string :"+flightItinerary.toString());
                            if(hashMap.containsKey(flightItinerary.hashCode())){
                                Logger.info("Common Flights"+Json.toJson(flightItinerary));
                                FlightItinerary hashFlightItinerary = hashMap.get(flightItinerary.hashCode());
                                if(hashFlightItinerary.getPricingInformation() != null && hashFlightItinerary.getPricingInformation().getTotalPrice() != null
                                        && flightItinerary.getPricingInformation() != null && flightItinerary.getPricingInformation().getTotalPrice() != null){
                                    Integer hashItinaryPrice = new Integer(hashFlightItinerary.getPricingInformation().getTotalPrice().substring(3));
                                    Integer iteratorItinaryPrice = new Integer( flightItinerary.getPricingInformation().getTotalPrice().substring(3));
                                    if(iteratorItinaryPrice < hashItinaryPrice){
                                        hashMap.remove(hashFlightItinerary.hashCode());
                                        hashMap.put(flightItinerary.hashCode(),flightItinerary);
                                     }
                                }
                            }else{
                                hashMap.put(flightItinerary.hashCode(),flightItinerary);
                            }
                        }
                    }
                }
            }
            if(counter == searchResponseListSize){
                loop = false;
                j.set(searchParameters.redisKey()+":status","complete");
            }
        }
        Logger.info("HashMap Size: "+hashMap.size());



        Logger.info("***********SEARCH END***********");
        return searchResponseList;
    }
}
