package services;

import com.compassites.model.FlightItinerary;
import com.compassites.model.SearchParameters;
import com.compassites.model.SearchResponse;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public interface SplitTicketSearch {
    public List<SearchResponse> splitSearch(List<SearchParameters> searchParameters, ConcurrentHashMap<String,List<FlightItinerary>> concurrentHashMap, boolean isDomestic) throws Exception;
}
