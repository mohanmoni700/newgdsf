package services;

import com.compassites.GDSWrapper.travelport.Helper;
import com.compassites.GDSWrapper.travelport.LowFareRequestClient;
import com.compassites.exceptions.IncompleteDetailsMessage;
import com.compassites.model.*;
import com.compassites.model.AirSolution;
import com.travelport.schema.air_v26_0.*;
import com.travelport.service.air_v26_0.AirFaultMessage;
import org.springframework.stereotype.Service;
import play.Logger;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Renu
 * Date: 5/23/14
 * Time: 3:43 PM
 * To change this template use File | Settings | File Templates.
 */
@Service
public class TravelPortFlightSearch implements FlightSearch {

    public SearchResponse search (SearchParameters searchParameters) throws IncompleteDetailsMessage {
        Logger.info("TravelPortFlightSearch called at " + new Date());
        LowFareRequestClient lowFareRequestClient = new LowFareRequestClient();
        LowFareSearchRsp response = null;
        try {
            response = lowFareRequestClient.search(searchParameters);
            Logger.info("TravelPortFlightSearch search response at "+ new Date());

        } catch (AirFaultMessage airFaultMessage) {
            throw new IncompleteDetailsMessage(airFaultMessage.getMessage(), airFaultMessage.getCause());
        }
        return mapTravelportToCompassites(response) ;
    }

    private SearchResponse mapTravelportToCompassites(LowFareSearchRsp travelportResponse){
        SearchResponse searchResponse= new SearchResponse();

        AirSolution airSolution=new AirSolution();
        List<FlightItinerary> flightItineraries=new ArrayList<FlightItinerary>();


        List<AirSegmentInformation> airSegmentInformationList=new ArrayList<AirSegmentInformation>();
        Helper.AirSegmentMap allSegments = Helper.createAirSegmentMap(
                travelportResponse.getAirSegmentList().getAirSegment());
        Helper.FlightDetailsMap allDetails = Helper.createFlightDetailsMap(
                travelportResponse.getFlightDetailsList().getFlightDetails());
        List<AirPricingSolution> airPricingSolutions =  travelportResponse.getAirPricingSolution();
        for (Iterator<AirPricingSolution> airPricingSolutionIterator = airPricingSolutions.iterator(); airPricingSolutionIterator.hasNext();){
            AirPricingSolution airPricingSolution = (AirPricingSolution)airPricingSolutionIterator.next();
            FlightItinerary flightItinerary=new FlightItinerary();
            flightItinerary.setProvider("Travelport");
            flightItinerary.getPricingInformation().setBasePrice(airPricingSolution.getBasePrice());
            flightItinerary.getPricingInformation().setTax(airPricingSolution.getTaxes());
            flightItinerary.getPricingInformation().setTotalPrice(airPricingSolution.getTotalPrice());

            System.out.print("Price:"+ airPricingSolution.getTotalPrice());
            System.out.print(" [BasePrice "+airPricingSolution.getBasePrice() +", ");
            System.out.print("Taxes "+airPricingSolution.getTaxes()+"]");
            List<Journey> journeyList = airPricingSolution.getJourney();
            for (Iterator<Journey> journeyIterator = journeyList.iterator(); journeyIterator.hasNext();) {

                Journey journey = journeyIterator.next();
                flightItinerary.AddBlankJourney();
                //journey.getTravelTime();
                System.out.println("CabinClass " + airPricingSolution.getAirPricingInfo().get(0).getBookingInfo().get(journeyList.indexOf(journey)).getCabinClass());
                List<AirSegmentRef> airSegmentRefList = journey.getAirSegmentRef();
                for (Iterator<AirSegmentRef> airSegmentRefIterator = airSegmentRefList.iterator(); airSegmentRefIterator.hasNext(); ) {
                    AirSegmentRef airSegmentRef = airSegmentRefIterator.next();
                    TypeBaseAirSegment airSegment = allSegments.getByRef(airSegmentRef);
                    String carrier = "??";
                    String flightNum = "???";
                    if (airSegment != null) {
                        if (airSegment.getCarrier() != null) {
                            carrier = airSegment.getCarrier();
                        }
                        if (airSegment.getFlightNumber() != null) {
                            flightNum = airSegment.getFlightNumber();
                        }
                    }

                    AirSegmentInformation airSegmentInformation = new AirSegmentInformation();
                    airSegmentInformation.setCarrierCode(carrier);
                    airSegmentInformation.setFlightNumber(flightNum);
                    System.out.print(carrier + "#" + flightNum);
                    String o = "???", d = "???";
                    if (airSegment != null) {
                        if (airSegment.getOrigin() != null) {
                            o = airSegment.getOrigin();
                        }
                        if (airSegment.getDestination() != null) {
                            d = airSegment.getDestination();
                        }
                    }
                    System.out.print(" from " + o + " to " + d);
                    airSegmentInformation.setFromLocation(o);
                    airSegmentInformation.setToLocation(d);
                    String dtime = "??:??";
                    String atime = "??:??";
                    FlightDetails flightDetails = allDetails.getByRef(airSegment.getFlightDetailsRef().get(0));
                    if (flightDetails != null) {
                        if (flightDetails.getDepartureTime() != null) {
                            dtime = flightDetails.getDepartureTime();
                        }
                        if (flightDetails.getArrivalTime() != null) {
                            atime = flightDetails.getArrivalTime();
                        }
                    }
                    System.out.print(" at " + dtime);
                    System.out.print(" arrives at " + atime);
                    airSegmentInformation.setDepartureTime(dtime);
                    airSegmentInformation.setArrivalTime(atime);

                    if ((flightDetails != null) && (flightDetails.getFlightTime() != null)) {
                        System.out.println(" (flight time " + flightDetails.getFlightTime() + " minutes)");
                        airSegmentInformation.setTravelTime(String.valueOf(flightDetails.getFlightTime()));
                    } else {
                        System.out.println();
                    }
                    flightItinerary.getJourneyList().get(journeyList.indexOf(journey)).getAirSegmentList().add(airSegmentInformation);

                }
                flightItinerary.getJourneyList().get(journeyList.indexOf(journey)).setTravelTime(journey.getTravelTime());
                System.out.println("total travel time"+ flightItinerary.getJourneyList().get(journeyList.indexOf(journey)).getTravelTime().getHours()+ flightItinerary.getJourneyList().get(journeyList.indexOf(journey)).getTravelTime().getMinutes() );
            }

            System.out.println("-----------");
            airSolution.getFlightItineraryList().add(flightItinerary);
        }

        searchResponse.setAirSolution(airSolution);

        return searchResponse;
    }
}
