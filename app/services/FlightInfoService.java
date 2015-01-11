package services;

import com.compassites.model.FlightItinerary;
import com.compassites.model.SearchParameters;

/**
 * @author Santhosh
 */
public interface FlightInfoService {
	
	public FlightItinerary getBaggageInfo(FlightItinerary flightItinerary, SearchParameters searchParams, boolean seamen);
	
	public FlightItinerary getInFlightDetails(FlightItinerary flightItinerary, boolean seamen);

}
