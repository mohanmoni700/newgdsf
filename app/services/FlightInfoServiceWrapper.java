package services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.compassites.model.FlightItinerary;
import com.compassites.model.SearchParameters;

/**
 * @author Santhosh
 */

@Service
public class FlightInfoServiceWrapper {
	
	@Autowired
    private AmadeusFlightInfoServiceImpl amadeusFlightInfoService;
	
	public FlightItinerary getFlightInfo(FlightItinerary fligtItinerary, SearchParameters searchParams) {
		String provider = fligtItinerary.getProvider();
		FlightItinerary flightItinerary = null;
		if("Travelport".equalsIgnoreCase(provider)) {
			// TODO:
        } else if ("Amadeus".equalsIgnoreCase(provider)) {
        	flightItinerary = amadeusFlightInfoService.getFlightnfo(fligtItinerary, searchParams);
		} else {
			// TODO:
        }
		
		return flightItinerary;
	}

}
