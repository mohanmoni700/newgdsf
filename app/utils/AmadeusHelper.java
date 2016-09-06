package utils;

import com.amadeus.xml.pnracc_11_3_1a.PNRReply;
import com.amadeus.xml.pnracc_11_3_1a.PNRReply.OriginDestinationDetails;
import com.amadeus.xml.pnracc_11_3_1a.ReservationControlInformationTypeI115879S;
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

    public static Map<String,String> readMultipleAirlinePNR(PNRReply gdsPNRReply){
        Map<String,String>  airlinePNRMap = new HashMap<>();
        int segmentSequence = 1;
        for(OriginDestinationDetails originDestinationDetails : gdsPNRReply.getOriginDestinationDetails()){

            for(OriginDestinationDetails.ItineraryInfo itineraryInfo : originDestinationDetails.getItineraryInfo() ){
                ReservationControlInformationTypeI115879S itineraryReservationInfo = itineraryInfo.getItineraryReservationInfo();
                if(itineraryReservationInfo != null && itineraryReservationInfo.getReservation() != null){
                    String airlinePNR = itineraryInfo.getItineraryReservationInfo().getReservation().getControlNumber();
                    String segments = itineraryInfo.getTravelProduct().getBoardpointDetail().getCityCode() + itineraryInfo.getTravelProduct().getOffpointDetail().getCityCode() + segmentSequence;
                    airlinePNRMap.put(segments.toLowerCase(), airlinePNR);
                    segmentSequence++;
                }

            }
        }
        return airlinePNRMap;
    }
}
