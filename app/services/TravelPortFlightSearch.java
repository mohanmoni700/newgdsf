package services;

import com.compassites.GDSWrapper.travelport.Helper;
import com.compassites.GDSWrapper.travelport.LowFareRequestClient;
import com.compassites.exceptions.IncompleteDetailsMessage;
import com.compassites.exceptions.RetryException;
import com.compassites.model.*;
import com.compassites.model.AirSolution;
import com.compassites.model.Journey;
import com.sun.xml.ws.client.ClientTransportException;
import com.travelport.schema.air_v26_0.*;
import com.travelport.schema.common_v26_0.ResponseMessage;
import com.travelport.service.air_v26_0.AirFaultMessage;
import models.AirlineCode;
import models.Airport;
import org.springframework.stereotype.Service;
import play.Logger;
import utils.ErrorMessageHelper;
import utils.StringUtility;

import java.text.ParseException;
import java.text.SimpleDateFormat;
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
public class TravelPortFlightSearch implements  FlightSearch{

    @RetryOnFailure(attempts = 2, delay = 2000, exception = RetryException.class )
    public SearchResponse search (SearchParameters searchParameters) throws IncompleteDetailsMessage, RetryException {
        Logger.info("[TravelPort] search called at " + new Date());

        SearchResponse seamanResponse = null;
        SearchResponse nonSeamanResponse = null;

        Logger.info("[Travelport] Starting non-seaman search");
        nonSeamanResponse = search(searchParameters, "nonseaman");
        Logger.info("[Travelport] End non-seaman search. Response size: "+ nonSeamanResponse.getAirSolution().getFlightItineraryList().size() );
        /*if(seamanResponse.getAirSolution().getFlightItineraryList().size() > 0){
            System.out.print("bs");
        }*/
        if (searchParameters.getBookingType()==BookingType.SEAMEN){
            Logger.info("[Travelport] Starting seaman search.");

            seamanResponse = search(searchParameters, "seaman");
            Logger.info("[Travelport] End seaman search. Response size: "+ seamanResponse.getAirSolution().getFlightItineraryList().size());
        }

        SearchResponse finalResponse = mergeResponse(nonSeamanResponse, seamanResponse);

        finalResponse.setProvider("Travelport");
        return finalResponse;

    }

    //@Override
    public String provider() {
        return "Travelport";
    }

    private SearchResponse mergeResponse(SearchResponse nonSeamanResponse, SearchResponse seamanResponse) {
        if (seamanResponse == null)
            return nonSeamanResponse;
        for(FlightItinerary seamanFlightItinerary : seamanResponse.getAirSolution().getFlightItineraryList()) {
            Journey seamanOnwardJourney = seamanFlightItinerary.getJourneyList().get(0);
            String seamanKey ="";
            seamanFlightItinerary.setSeamanPricingInformation(seamanFlightItinerary.getPricingInformation());
            seamanFlightItinerary.setPricingInformation(new PricingInformation());

            for (AirSegmentInformation seamanOnwardAirSegment : seamanOnwardJourney.getAirSegmentList()){
                seamanKey =  seamanKey + seamanOnwardAirSegment.getFromLocation()+ seamanOnwardAirSegment.getToLocation()+ seamanOnwardAirSegment.getCarrierCode()+"#"+ seamanOnwardAirSegment.getFlightNumber()+seamanOnwardAirSegment.getArrivalTime()+seamanOnwardAirSegment.getDepartureTime();

            }
            //System.out.println("Seaman Key" + seamanKey);

            for(FlightItinerary nonSeamanFlightItinerary : nonSeamanResponse.getAirSolution().getFlightItineraryList()){
                Journey nonSeamanOnwardJourney = nonSeamanFlightItinerary.getJourneyList().get(0);
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
        SearchResponse searchResponse=new SearchResponse();
        try {
            response = lowFareRequestClient.search(searchParameters);
            Logger.info("[TravelPort] FlightSearch search response at "+ new Date());
            List<ResponseMessage> responseMessageList = response.getResponseMessage();
            errorExist = ((response.getAirPricingSolution() ==null) || ( response.getAirPricingSolution().size() == 0)) ;
            for (ResponseMessage responseMessage : responseMessageList){
               if("Error".equalsIgnoreCase(responseMessage.getType())){
                    Logger.info("[Travelport] Error received from Travel port : "+ responseMessage.getValue());

                    errorCode = ""+responseMessage.getCode();
                    errorMessage = errorMessage + responseMessage.getValue();
                }
            }
        } catch (AirFaultMessage airFaultMessage) {
            //throw new IncompleteDetailsMessage(airFaultMessage.getMessage(), airFaultMessage.getCause());


            ErrorMessage errMessage = ErrorMessageHelper.createErrorMessage("partialResults", ErrorMessage.ErrorType.ERROR, "Travelport");
            searchResponse.getErrorMessageList().add(errMessage);
            return searchResponse;
        }catch (ClientTransportException clientTransportException){

            clientTransportException.printStackTrace();
            throw new RetryException(clientTransportException.getMessage());
        }catch (Exception e){
            //throw new IncompleteDetailsMessage(e.getMessage(), e.getCause());

            ErrorMessage errMessage = ErrorMessageHelper.createErrorMessage("partialResults", ErrorMessage.ErrorType.ERROR, "Travelport");
            searchResponse.getErrorMessageList().add(errMessage);
            return searchResponse;
        }
        if(errorExist){

            errorCode = "Travelport."+errorCode;
            boolean errorCodeExist = ErrorMessageHelper.checkErrorCodeExist(errorCode);
            if(errorCodeExist){
                ErrorMessage errMessage = ErrorMessageHelper.createErrorMessage(errorCode, ErrorMessage.ErrorType.WARNING, "Travelport");
                throw new RetryException(errMessage.getMessage());
            }
            ErrorMessage errMessage = ErrorMessageHelper.createErrorMessage(errorCode, ErrorMessage.ErrorType.WARNING, "Travelport");
            searchResponse.getErrorMessageList().add(errMessage);

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

        flightIteratorLoop: for (Iterator<AirPricingSolution> airPricingSolutionIterator = airPricingSolutions.iterator(); airPricingSolutionIterator.hasNext();){


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
            flightItinerary.getPricingInformation().setBasePrice(StringUtility.getPriceFromString(airPricingSolution.getBasePrice()));
            flightItinerary.getPricingInformation().setTax(StringUtility.getPriceFromString(airPricingSolution.getTaxes()));
            flightItinerary.getPricingInformation().setTotalPrice(StringUtility.getPriceFromString(airPricingSolution.getTotalPrice()));
            flightItinerary.getPricingInformation().setTotalPriceValue(Long.parseLong(airPricingSolution.getTotalPrice().substring(3)));

            //System.out.print("Price:"+ airPricingSolution.getTotalPrice());
            //System.out.print(" Travelport BasePrice "+airPricingSolution.getBasePrice() +", ");
            //System.out.print("Taxes "+airPricingSolution.getTaxes()+"]");
            List<com.travelport.schema.air_v26_0.Journey> journeyList = airPricingSolution.getJourney();
            for (Iterator<com.travelport.schema.air_v26_0.Journey> journeyIterator = journeyList.iterator(); journeyIterator.hasNext();) {

                com.travelport.schema.air_v26_0.Journey journey = journeyIterator.next();
                flightItinerary.AddBlankJourney();
                int journeyStopCounter = -1;
                //System.out.println("CabinClass " + airPricingSolution.getAirPricingInfo().get(0).getBookingInfo().get(journeyList.indexOf(journey)).getCabinClass());
                List<AirSegmentRef> airSegmentRefList = journey.getAirSegmentRef();
                for (Iterator<AirSegmentRef> airSegmentRefIterator = airSegmentRefList.iterator(); airSegmentRefIterator.hasNext(); ) {
                    journeyStopCounter++;
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
                    //System.out.print(carrier + "#" + flightNum);
                    String o = "???", d = "???";
                    if (airSegment != null) {
                        if (airSegment.getOrigin() != null) {
                            o = airSegment.getOrigin();
                        }
                        if (airSegment.getDestination() != null) {
                            d = airSegment.getDestination();
                        }
                    }
                    //System.out.print(" from " + o + " to " + d);
                    airSegmentInformation.setFromLocation(o);
                    airSegmentInformation.setToLocation(d);
                    airSegmentInformation.setFromAirport(Airport.getAiport(o));
                    airSegmentInformation.setToAirport(Airport.getAiport(d));
                    if (airSegment.getConnection() != null)
                        airSegmentInformation.setConnectionTime(airSegment.getConnection().getDuration());

                    String dtime = "??:??";
                    String atime = "??:??";
                    FlightDetails flightDetails = allDetails.getByRef(airSegment.getFlightDetailsRef().get(0));
                    if (flightDetails != null) {
                        if (flightDetails.getDepartureTime() == null) {
                            continue  flightIteratorLoop;
                        }else{
                            dtime = flightDetails.getDepartureTime();
                        }
                        if (flightDetails.getArrivalTime() == null) {
                            continue flightIteratorLoop;
                        }else {
                            atime = flightDetails.getArrivalTime();
                        }
                    }
                    //System.out.print(" at " + dtime);
                    //System.out.print(" arrives at " + atime);
                    airSegmentInformation.setDepartureTime(dtime);

                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
                    try {
                        airSegmentInformation.setDepartureDate(sdf.parse(dtime));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }

                    airSegmentInformation.setArrivalTime(atime);

                    if ((flightDetails != null) && (flightDetails.getFlightTime() != null)) {
                        //System.out.println(" (flight time " + flightDetails.getFlightTime() + " minutes)");
                        airSegmentInformation.setTravelTime(String.valueOf(flightDetails.getFlightTime()));
                    } else {
                        //System.out.println();
                    }
                    airSegmentInformation.setFlightDetailsKey(flightDetails.getKey());
                    airSegmentInformation.setAirSegmentKey(airSegment.getKey());

                    flightItinerary.getJourneyList().get(journeyList.indexOf(journey)).getAirSegmentList().add(airSegmentInformation);
                    flightItinerary.getJourneyList().get(journeyList.indexOf(journey)).setAirlinesStrForFilter(" "+airSegmentInformation.getCarrierCode() + " " + airSegmentInformation.getAirline().airline );

                }
                flightItinerary.getJourneyList().get(journeyList.indexOf(journey)).setTravelTime(journey.getTravelTime());
                flightItinerary.getJourneyList().get(journeyList.indexOf(journey)).setNoOfStops(journeyStopCounter);
                //System.out.println("total travel time"+ flightItinerary.getJourneyList().get(journeyList.indexOf(journey)).getTravelTime().getHours()+ flightItinerary.getJourneyList().get(journeyList.indexOf(journey)).getTravelTime().getMinutes() );
            }

            //System.out.println("-----------");
            airSolution.getFlightItineraryList().add(flightItinerary);
        }

        searchResponse.setAirSolution(airSolution);

        return searchResponse;
    }
}
