package services;

import com.compassites.model.FlightItinerary;
import com.compassites.model.SearchParameters;
import com.compassites.model.SearchResponse;
import models.FlightSearchOffice;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public interface SplitAmadeusSearch {
    public List<SearchResponse> splitSearch(List<SearchParameters> searchParameters, ConcurrentHashMap<String,List<FlightItinerary>> concurrentHashMap) throws Exception;
    public void splitTicketSearch(List<SearchParameters> searchParameters, SearchParameters originalSearchRequest) throws Exception;
    public SearchResponse search(SearchParameters searchParameters, FlightSearchOffice office) throws Exception;
    public String provider();
    public List<FlightSearchOffice> getOfficeList();
}
