package services;

import com.compassites.exceptions.IncompleteDetailsMessage;
import com.compassites.model.SearchParameters;
import com.compassites.model.SearchResponse;

/**
 * Created with IntelliJ IDEA.
 * User: Renu
 * Date: 5/23/14
 * Time: 3:44 PM
 * To change this template use File | Settings | File Templates.
 */
public interface FlightSearch {
    public SearchResponse search(SearchParameters searchParameters) throws IncompleteDetailsMessage,Exception;
}
