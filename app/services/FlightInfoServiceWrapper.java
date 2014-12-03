package services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.compassites.GDSWrapper.mystifly.Mystifly;
import com.compassites.model.FlightItinerary;
import com.compassites.model.SearchParameters;

/**
 * @author Santhosh
 */

@Service
public class FlightInfoServiceWrapper {

	@Autowired
	private AmadeusFlightInfoServiceImpl amadeusFlightInfoService;

	@Autowired
	private MystiflyFlightInfoServiceImpl mystiflyFlightInfoService;

	public FlightItinerary getFlightInfo(FlightItinerary flightItinerary,
			SearchParameters searchParams) {
		String provider = "";
//		String provider = flightItinerary.getProvider();
		FlightItinerary response = null;
		if ("Travelport".equalsIgnoreCase(provider)) {
			response = flightItinerary;
			// Travelport baggage info is available in search response
		} else if ("Amadeus".equalsIgnoreCase(provider)) {
			response = amadeusFlightInfoService.getFlightnfo(
					flightItinerary, searchParams);
		} else if (Mystifly.PROVIDER.equalsIgnoreCase(provider)) {
			response = mystiflyFlightInfoService.getFlightnfo(
					flightItinerary, searchParams);
		}
		return response;
	}

}
