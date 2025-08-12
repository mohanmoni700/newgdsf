package services;

import com.compassites.model.FlightItinerary;
import com.compassites.model.traveller.TravellerMasterInfo;

public interface IndigoFlightInfoService {
    public FlightItinerary getFlightInfo(FlightItinerary flightItinerary, TravellerMasterInfo travellerMasterInfo);
    public String getCancellationFee(FlightItinerary flightItinerary);
}
