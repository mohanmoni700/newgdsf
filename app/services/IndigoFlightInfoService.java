package services;

import com.compassites.model.FlightItinerary;

public interface IndigoFlightInfoService {
    public FlightItinerary getFlightInfo(FlightItinerary flightItinerary);
    public String getCancellationFee(FlightItinerary flightItinerary);
}
