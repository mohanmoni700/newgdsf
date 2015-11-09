package utils;

import com.compassites.model.AirSegmentInformation;
import com.compassites.model.Journey;
import models.Airport;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by yaseen on 19-10-2015.
 */
public class AmadeusHelper {

    public static boolean checkAirportCountry(String country , List<Journey> journeys){
        List<String> airportCodes = new ArrayList<>();
        for(Journey journey : journeys){
            for(AirSegmentInformation airSegmentInformation : journey.getAirSegmentList()){
                airportCodes.add(airSegmentInformation.getFromLocation());
                airportCodes.add(airSegmentInformation.getToLocation());
            }
        }
        boolean result = Airport.checkCountry(country, airportCodes);
        return result;
    }
}
