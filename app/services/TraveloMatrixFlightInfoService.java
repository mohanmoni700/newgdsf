package services;

import com.compassites.model.AncillaryServicesResponse;
import com.compassites.model.FlightItinerary;

import java.util.HashMap;
import java.util.List;

public interface TraveloMatrixFlightInfoService {
    public List<HashMap> flightFareRules(String resultToken,String returnResultToken);
    public FlightItinerary getFlightInfo(FlightItinerary flightItinerary);

}
