package services;

import com.compassites.model.SearchParameters;
import com.compassites.model.SearchResponse;
import models.FlightSearchOffice;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Renu
 * Date: 5/23/14
 * Time: 3:44 PM
 * To change this template use File | Settings | File Templates.
 */
public interface FlightSearch {
    SearchResponse search(SearchParameters searchParameters, FlightSearchOffice office) throws Exception;
    String provider();
    List<FlightSearchOffice> getOfficeList();
}

