package services;

import com.compassites.model.FlightItinerary;
import com.compassites.model.travelomatrix.ResponseModels.TraveloMatrixFaruleReply;

import java.util.List;

public interface TraveloMatrixFlightInfoService {
    List<TraveloMatrixFaruleReply> flightFareRules(String resultToken , String returnResultToken);
//    public List<HashMap> flightFareRules(String resultToken,String returnResultToken);
    public FlightItinerary getFlightInfo(FlightItinerary flightItinerary);

}
