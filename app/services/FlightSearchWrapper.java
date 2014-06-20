package services;

import com.compassites.model.SearchParameters;
import com.compassites.model.SearchResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by user on 17-06-2014.
 */
@Service
public class FlightSearchWrapper {

    @Autowired
    private List<FlightSearch> flightSearchList;

    @Autowired
    private TravelPortFlightSearch travelPortFlightSearch;

    public List<SearchResponse> search(SearchParameters searchParameters) {

        List<SearchResponse> searchResponseList = new ArrayList<SearchResponse>();

        //for (FlightSearch flightSearch: flightSearchList){
            try {
               // System.out.println("Flight Search : "+flightSearch.getClass());
                SearchResponse response = travelPortFlightSearch.search(searchParameters);
                searchResponseList.add(response);
            } catch (Exception e) {
                e.printStackTrace();
            }
       // }
        return searchResponseList;
    }
}
