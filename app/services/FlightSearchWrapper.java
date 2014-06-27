package services;

import com.compassites.model.SearchParameters;
import com.compassites.model.SearchResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import play.Logger;
import play.libs.Json;
import redis.clients.jedis.Jedis;

import java.util.ArrayList;
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

    @Autowired
    private TravelPortFlightSearch travelPortFlightSearch;

    public List<SearchResponse> search(final SearchParameters searchParameters) {
        Logger.info("***********SEARCH STARTED***********");
        List<SearchResponse> searchResponseList = new ArrayList<SearchResponse>();
        int maxThreads = 5;
        int queueSize = 100; // should be big enough!
        ExecutorService newExecutor = new ThreadPoolExecutor(maxThreads, maxThreads, Long.MAX_VALUE, TimeUnit.NANOSECONDS, new ArrayBlockingQueue<Runnable>(queueSize, true));
        //ExecutorService newExecutor = Executors.newFixedThreadPool(1);
        List<Callable<SearchResponse>> tasks = new ArrayList<>();
        List<Future<SearchResponse>> results = new ArrayList<>();

        for (final FlightSearch flightSearch: flightSearchList){
            try {
               System.out.println("Flight Search : "+flightSearch.getClass());
                /*tasks.add(new Callable<SearchResponse>() {
                    public SearchResponse call() throws Exception {
                        SearchResponse response = flightSearch.search(searchParameters);
                        return  response;
                    }
                });*/
                //searchResponseList.add(response);
                results.add(newExecutor.submit(new Callable<SearchResponse>() {
                    public SearchResponse call() throws Exception {
                        SearchResponse response = flightSearch.search(searchParameters);
                        return response;
                    }
                }));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            //results = newExecutor.invokeAll(tasks);
            Logger.info("Threads initiated");
            int counter = 0;
            boolean loop = true;
            int size = results.size();
            Jedis j = new Jedis("localhost", 6379);
            j.connect();

            while(loop){
                ListIterator<Future<SearchResponse>> listIterator = results.listIterator();
                while(listIterator.hasNext()) {
                    Future<SearchResponse>  future = listIterator.next();
                    if(future.isDone()){
                        SearchResponse searchResponse = future.get();
                        if (searchResponseList.size() > 0){
                            searchResponseList.get(0).getAirSolution().getFlightItineraryList().addAll(searchResponse.getAirSolution().getFlightItineraryList()) ;
                        }
                        else {
                            searchResponseList.add(searchResponse);
                        }
                        listIterator.remove();
                        Logger.info("Redis Key :" + searchParameters.redisKey());
                        String res = j.set(searchParameters.redisKey(), Json.stringify(Json.toJson(searchResponseList)));
                        Logger.info("Redis set values Response: "+res);
                        res = j.set(searchParameters.redisKey()+":status","partial");
                        Logger.info("Redis set status Response: "+res);
                        counter ++;

                    }
                }
                if(counter == size){
                    loop = false;
                    j.set(searchParameters.redisKey()+":status","complete");
                }
            }


        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }


        /*Jedis j = new Jedis("localhost", 6379);
        j.connect();
        System.out.println("Redis ************************");

        j.set("1ads123","asdljasd");
        j.get("1ads123");
        System.out.println("Redis ************************");*/
        Logger.info("***********SEARCH END***********");
        return searchResponseList;
    }
}
