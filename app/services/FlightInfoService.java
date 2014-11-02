package services;

import com.compassites.model.FlightItinerary;
import com.compassites.model.SearchParameters;

/**
 * @author Santhosh
 */
public interface FlightInfoService {
	
	public FlightItinerary getFlightnfo(FlightItinerary fligtItinerary, SearchParameters searchParams);

}
