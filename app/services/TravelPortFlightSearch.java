package services;

import com.compassites.GDSWrapper.travelport.Helper;
import com.compassites.GDSWrapper.travelport.LowFareRequestClient;
import com.compassites.exceptions.IncompleteDetailsMessage;
import com.compassites.exceptions.RetryException;
import com.compassites.model.*;
import com.compassites.model.AirSolution;
import com.travelport.schema.air_v26_0.*;
import com.travelport.schema.common_v26_0.ResponseMessage;
import com.travelport.service.air_v26_0.AirFaultMessage;
import models.AirlineCode;
import org.springframework.stereotype.Service;
import play.Logger;

import java.io.FileInputStream;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: Renu
 * Date: 5/23/14
 * Time: 3:43 PM
 * To change this template use File | Settings | File Templates.
 */
@Service
public class TravelPortFlightSearch implements FlightSearch {

    @RetryOnFailure(attempts = 2, delay = 2000, exception = RetryException.class )
    public SearchResponse search (SearchParameters searchParameters) throws IncompleteDetailsMessage, RetryException {
        Logger.info("TravelPortFlightSearch called at " + new Date());

        SearchResponse seamanResponse = null;
        SearchResponse nonSeamanResponse = null;

        nonSeamanResponse = search(searchParameters, "nonseaman");

        if (searchParameters.getBookingType()==BookingType.SEAMEN){
            seamanResponse = search(searchParameters, "seaman");
        }

        SearchResponse finalResponse = mergeResponse(nonSeamanResponse, seamanResponse);

        return finalResponse;

    }

    private SearchResponse mergeResponse(SearchResponse nonSeamanResponse, SearchResponse seamanResponse) {
        if (seamanResponse == null)
            return nonSeamanResponse;
        for(FlightItinerary seamanFlightItinerary : seamanResponse.getAirSolution().getFlightItineraryList()) {
            FlightItinerary.Journey seamanOnwardJourney = seamanFlightItinerary.getJourneyList().get(0);
            String seamanKey ="";
            seamanFlightItinerary.setSeamanPricingInformation(seamanFlightItinerary.getPricingInformation());
            seamanFlightItinerary.setPricingInformation(new PricingInformation());

            for (AirSegmentInformation seamanOnwardAirSegment : seamanOnwardJourney.getAirSegmentList()){
                seamanKey =  seamanKey + seamanOnwardAirSegment.getFromLocation()+ seamanOnwardAirSegment.getToLocation()+ seamanOnwardAirSegment.getCarrierCode()+"#"+ seamanOnwardAirSegment.getFlightNumber()+seamanOnwardAirSegment.getArrivalTime()+seamanOnwardAirSegment.getDepartureTime();

            }
            //System.out.println("Seaman Key" + seamanKey);

            for(FlightItinerary nonSeamanFlightItinerary : nonSeamanResponse.getAirSolution().getFlightItineraryList()){
                FlightItinerary.Journey nonSeamanOnwardJourney = nonSeamanFlightItinerary.getJourneyList().get(0);
                String nonSeamanKey ="";
                for (AirSegmentInformation nonSeamanOnwardAirSegment : nonSeamanOnwardJourney.getAirSegmentList()){
                    nonSeamanKey =  nonSeamanKey + nonSeamanOnwardAirSegment.getFromLocation()+ nonSeamanOnwardAirSegment.getToLocation()+ nonSeamanOnwardAirSegment.getCarrierCode()+"#"+ nonSeamanOnwardAirSegment.getFlightNumber()+nonSeamanOnwardAirSegment.getArrivalTime()+nonSeamanOnwardAirSegment.getDepartureTime();
                }

                //System.out.println("Non Seaman Key" + nonSeamanKey);

                if (nonSeamanKey.equalsIgnoreCase(seamanKey)){
                    //System.out.println("Matched");
                    seamanFlightItinerary.setPricingInformation(nonSeamanFlightItinerary.getPricingInformation());
                    break;
                }
            }

        }
        return seamanResponse;
    }

    private SearchResponse search (SearchParameters searchParameters, String bookingtype) throws IncompleteDetailsMessage, RetryException {
        searchParameters.setSearchBookingType(bookingtype);
        LowFareRequestClient lowFareRequestClient = new LowFareRequestClient();
        LowFareSearchRsp response = null;
        boolean errorExist = false;
        String errorMessage = null;
        String errorCode = null;
        try {
            response = lowFareRequestClient.search(searchParameters);
            Logger.info("TravelPortFlightSearch search response at "+ new Date());
            List<ResponseMessage> responseMessageList = response.getResponseMessage();
            errorExist = ((response.getAirPricingSolution() ==null) || ( response.getAirPricingSolution().size() == 0)) ;
            for (ResponseMessage responseMessage : responseMessageList){
               if("Error".equalsIgnoreCase(responseMessage.getType())){
                    System.out.println("Error received from Travel port : "+ responseMessage.getValue());

                    errorCode = ""+responseMessage.getCode();
                    errorMessage = errorMessage + responseMessage.getValue();
                }
            }
        } catch (AirFaultMessage airFaultMessage) {
            throw new IncompleteDetailsMessage(airFaultMessage.getMessage(), airFaultMessage.getCause());
        }catch (Exception e){
            throw new IncompleteDetailsMessage(e.getMessage(), e.getCause());
        }
        if(errorExist){
            Properties prop = new Properties();
            InputStream input = null;
            try {
                input = new FileInputStream("conf/galileoErrorCodes.properties");
                prop.load(input);
                if(!prop.containsKey(errorCode)){
                    throw new RetryException(prop.getProperty(errorCode));
                }
            }catch (Exception e){
                e.printStackTrace();
            }

        }else {
            return createAirSolutionFromRecommendations(response, searchParameters) ;
        }
         return null;
    }

    private SearchResponse createAirSolutionFromRecommendations(LowFareSearchRsp travelportResponse, SearchParameters searchParameters){
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

            //Seaman Fares
            /*
            {
                AirItinerary airItinerary = AirRequestClient.getItinerary(travelportResponse, airPricingSolution);
                AirPriceRsp priceRsp = null;
                try {
                    priceRsp = AirRequestClient.priceItinerary(airItinerary, "SEA", searchParameters.getCurrency(), TypeCabinClass.ECONOMY, searchParameters.getPassengers() );
                    AirPricingSolution airPriceSolution = AirReservationClient.stripNonXmitSections(AirRequestClient.getPriceSolution(priceRsp));

                    flightItinerary.getSeamanPricingInformation().setBasePrice(airPriceSolution.getBasePrice());
                    flightItinerary.getSeamanPricingInformation().setTax(airPriceSolution.getTaxes());
                    flightItinerary.getSeamanPricingInformation().setTotalPrice(airPriceSolution.getTotalPrice());

                } catch (AirFaultMessage airFaultMessage) {
                    airFaultMessage.printStackTrace();
                }
            }
            */
            flightItinerary.getPricingInformation().setBasePrice(airPricingSolution.getBasePrice());
            flightItinerary.getPricingInformation().setTax(airPricingSolution.getTaxes());
            flightItinerary.getPricingInformation().setTotalPrice(airPricingSolution.getTotalPrice());

            System.out.print("Price:"+ airPricingSolution.getTotalPrice());
            System.out.print(" Travelport BasePrice "+airPricingSolution.getBasePrice() +", ");
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
                    airSegmentInformation.setAirline(AirlineCode.getAirlineByCode(carrier));
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

                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
                    try {
                        airSegmentInformation.setDepartureDate(sdf.parse(dtime));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }

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
