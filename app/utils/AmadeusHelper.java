package utils;

import com.compassites.model.AirSegmentInformation;
import com.compassites.model.Journey;
import models.Airport;

import java.util.*;

/**
 * Created by yaseen on 19-10-2015.
 */
public class AmadeusHelper {

    public static boolean checkAirportCountry(String country , List<Journey> journeys){
        Set<String> airportCodes = new HashSet<>();
        for(Journey journey : journeys){
            for(AirSegmentInformation airSegmentInformation : journey.getAirSegmentList()){
                airportCodes.add(airSegmentInformation.getFromLocation());
                airportCodes.add(airSegmentInformation.getToLocation());
            }
        }
        List<String> list = new ArrayList<>();
        list.addAll(airportCodes);
        boolean result = Airport.checkCountry(country, list);
        return result;
    }
}
