package services;

import com.amadeus.xml.fmptbr_12_4_1a.FareMasterPricerTravelBoardSearchReply;
import com.amadeus.xml.fmptbr_12_4_1a.ReferencingDetailsType191583C;
import com.amadeus.xml.fmptbr_12_4_1a.TravelProductType;
import com.compassites.GDSWrapper.amadeus.SearchFlights;
import com.compassites.GDSWrapper.amadeus.ServiceHandler;
import com.compassites.model.*;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Renu
 * Date: 5/23/14
 * Time: 3:50 PM
 * To change this template use File | Settings | File Templates.
 */
@Service
public class AmadeusFlightSearch implements FlightSearch {
    public SearchResponse search (SearchParameters searchParameters) throws Exception{
        SearchFlights searchFlights = new SearchFlights();
        SearchResponse searchResponse=new SearchResponse();
        searchResponse.setProvider("Amadeus");
        ServiceHandler serviceHandler=new ServiceHandler();
        serviceHandler.logIn();

        FareMasterPricerTravelBoardSearchReply fareMasterPricerTravelBoardSearchReply=
        serviceHandler.searchAirlines(searchParameters);

        AirSolution airSolution=new AirSolution();
        airSolution=createAirSolutionFromRecommendations(fareMasterPricerTravelBoardSearchReply);


        searchResponse.setAirSolution(airSolution);
        return searchResponse;
    }

    private AirSolution createAirSolutionFromRecommendations(FareMasterPricerTravelBoardSearchReply fareMasterPricerTravelBoardSearchReply){
        AirSolution airSolution=new AirSolution();
        List<FlightItinerary> flightItineraries=new ArrayList<FlightItinerary>();

        //each flightindex has one itinerary
        //each itinerary has multiple segments each corresponding to one flight in the itinerary in the airSegmentInformation
        for(FareMasterPricerTravelBoardSearchReply.Recommendation recommendation:fareMasterPricerTravelBoardSearchReply.getRecommendation()){
            FlightItinerary flightItinerary=new FlightItinerary();
            int i=0;
            for(ReferencingDetailsType191583C referencingDetailsType:recommendation.getSegmentFlightRef().get(0).getReferencingDetail()){
                BigInteger flightDetailReference=referencingDetailsType.getRefNumber();
                FareMasterPricerTravelBoardSearchReply.FlightIndex.GroupOfFlights groupOfFlights=fareMasterPricerTravelBoardSearchReply.getFlightIndex().get(i++).getGroupOfFlights().get(flightDetailReference.intValue()-1);
                for(FareMasterPricerTravelBoardSearchReply.FlightIndex.GroupOfFlights.FlightDetails flightDetails:groupOfFlights.getFlightDetails()){
                    AirSegmentInformation airSegmentInformation=createSegment(flightDetails.getFlightInformation());
                    flightItinerary.AddBlankJourney();
                    flightItinerary.getJourneyList().get(0).getAirSegmentList().add(airSegmentInformation);
                }
            }
            flightItineraries.add(flightItinerary);
        }
        airSolution.setFlightItineraryList(flightItineraries);
        return airSolution;
    }

    private AirSegmentInformation createSegment(TravelProductType flightInformation){
        AirSegmentInformation airSegmentInformation=new AirSegmentInformation();
        airSegmentInformation.setCarrierCode(flightInformation.getCompanyId().getMarketingCarrier());
        airSegmentInformation.setFlightNumber(flightInformation.getFlightOrtrainNumber());
        airSegmentInformation.setArrivalTime(flightInformation.getProductDateTime().getTimeOfArrival());
        airSegmentInformation.setDepartureTime(flightInformation.getProductDateTime().getDateOfDeparture());
        airSegmentInformation.setFromTerminal(flightInformation.getLocation().get(0).getTerminal());
        airSegmentInformation.setToTerminal(flightInformation.getLocation().get(1).getTerminal());
        airSegmentInformation.setToDate(flightInformation.getProductDateTime().getDateOfDeparture());
        airSegmentInformation.setFromDate(flightInformation.getProductDateTime().getDateOfArrival());
        airSegmentInformation.setToLocation(flightInformation.getLocation().get(1).getLocationId());
        airSegmentInformation.setFromLocation(flightInformation.getLocation().get(0).getLocationId());
        return airSegmentInformation;
    }
}
