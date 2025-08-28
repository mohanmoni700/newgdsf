package services;

import com.compassites.model.FlightItinerary;
import com.compassites.model.SearchParameters;
import com.compassites.model.SearchResponse;
import models.FlightSearchOffice;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SplitIndigoSearchWrapper implements SplitAmadeusSearch {

    static org.slf4j.Logger logger = LoggerFactory.getLogger("gds");

    @Override
    public List<SearchResponse> splitSearch(List<SearchParameters> searchParameters, ConcurrentHashMap<String, List<FlightItinerary>> concurrentHashMap, boolean isDomestic) throws Exception {
        return null;
    }

    @Override
    public void splitTicketSearch(List<SearchParameters> searchParameters, SearchParameters originalSearchRequest, boolean isSourceAirportDomestic, boolean isDestinationAirportDomestic) throws Exception {

    }

    @Override
    public SearchResponse search(SearchParameters searchParameters, FlightSearchOffice office) throws Exception {
        return null;
    }

    @Override
    public String provider() {
        return null;
    }

    @Override
    public List<FlightSearchOffice> getOfficeList() {
        return null;
    }
}
