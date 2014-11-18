package services;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import models.Airline;
import models.Airport;

import org.springframework.stereotype.Service;

import play.Logger;
import play.libs.Json;
import utils.ErrorMessageHelper;
import utils.StringUtility;

import com.compassites.GDSWrapper.travelport.Helper;
import com.compassites.GDSWrapper.travelport.LowFareRequestClient;
import com.compassites.exceptions.IncompleteDetailsMessage;
import com.compassites.exceptions.RetryException;
import com.compassites.model.AirSegmentInformation;
import com.compassites.model.AirSolution;
import com.compassites.model.BaggageInfo;
import com.compassites.model.BookingType;
import com.compassites.model.ErrorMessage;
import com.compassites.model.FlightItinerary;
import com.compassites.model.SearchParameters;
import com.compassites.model.SearchResponse;
import com.sun.xml.ws.client.ClientTransportException;
import com.travelport.schema.air_v26_0.AirPricingSolution;
import com.travelport.schema.air_v26_0.AirSegmentRef;
import com.travelport.schema.air_v26_0.FareInfo;
import com.travelport.schema.air_v26_0.FlightDetails;
import com.travelport.schema.air_v26_0.LowFareSearchRsp;
import com.travelport.schema.air_v26_0.TypeBaseAirSegment;
import com.travelport.schema.air_v26_0.TypeWeight;
import com.travelport.schema.common_v26_0.ResponseMessage;
import com.travelport.service.air_v26_0.AirFaultMessage;

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
    public SearchResponse search (SearchParameters searchParameters) throws IncompleteDetailsMessage, RetryException, IOException {
        Logger.info("[TravelPort] search called at " + new Date());

        SearchResponse seamanResponse = null;
        SearchResponse nonSeamanResponse = null;

        SearchResponse searchResponse = new SearchResponse();

        Logger.info("[Travelport] Starting non-seaman search");
        nonSeamanResponse = search(searchParameters, "nonseaman");
        if(nonSeamanResponse != null){
            searchResponse.getAirSolution().setNonSeamenHashMap(nonSeamanResponse.getAirSolution().getNonSeamenHashMap());
        }

        Logger.info("[Travelport] End non-seaman search. Response size: "+ nonSeamanResponse.getAirSolution().getFlightItineraryList().size() );

        if (searchParameters.getBookingType()==BookingType.SEAMEN){
            Logger.info("[Travelport] Starting seaman search.");
            seamanResponse = search(searchParameters, "seaman");
            if(seamanResponse != null){
                searchResponse.getAirSolution().setSeamenHashMap(seamanResponse.getAirSolution().getNonSeamenHashMap());
            }

            Logger.info("[Travelport] End seaman search. Response size: "+ seamanResponse.getAirSolution().getFlightItineraryList().size());
        }

        //SearchResponse finalResponse = mergeResponse(nonSeamanResponse, seamanResponse);

       /* finalResponse.setProvider("Travelport");
        File file = new File("travelport-roundtrip.json");
        FileOutputStream os = new FileOutputStream(file);
        PrintStream out = new PrintStream(os);
        out.print(Json.toJson(finalResponse));*/


        return searchResponse;

    }

    //@Override
    public String provider() {
        return "Travelport";
    }

    /*private SearchResponse mergeResponse(SearchResponse nonSeamanResponse, SearchResponse seamanResponse) {
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
*/
    private SearchResponse mergeResponse(SearchResponse nonSeamanResponse, SearchResponse seamanResponse) throws FileNotFoundException {
        if (seamanResponse == null)
            return nonSeamanResponse;
        Map<Integer,FlightItinerary> seamenFlightItineraryMap=new HashMap();
        Map<Integer,FlightItinerary> nonSeamenFlightItineraryMap=new HashMap();
        //TODO-use merge sort later for efficiency
        for(FlightItinerary seamanFlightItinerary : seamanResponse.getAirSolution().getFlightItineraryList()){
            seamenFlightItineraryMap.put(seamanFlightItinerary.hashCode(), seamanFlightItinerary);
        }
        for(FlightItinerary seamanFlightItinerary : nonSeamanResponse.getAirSolution().getFlightItineraryList()){
            nonSeamenFlightItineraryMap.put(seamanFlightItinerary.hashCode(),seamanFlightItinerary);
        }
        for(Integer hashCode : seamenFlightItineraryMap.keySet()) {
            FlightItinerary seamenItinerary = null;
            if(nonSeamenFlightItineraryMap.containsKey(hashCode)){
                seamenItinerary=nonSeamenFlightItineraryMap.get(hashCode);
                seamenItinerary.setPriceOnlyPTC(true);
                seamenItinerary.setSeamanPricingInformation(seamenFlightItineraryMap.get(hashCode).getPricingInformation());
                nonSeamenFlightItineraryMap.put(hashCode,seamenItinerary);
            }
        }
        AirSolution airSolution = new AirSolution();
        airSolution.setFlightItineraryList(new ArrayList<FlightItinerary>(nonSeamenFlightItineraryMap.values()));
        nonSeamanResponse.setAirSolution(airSolution);
        File file = new File("travelport-airsolution-roundtrip.json");
        File seamen = new File("travelport-seamen-roundtrip.json");
        File nonseamen = new File("travelport-nonseamen-roundtrip.json");

        FileOutputStream os = new FileOutputStream(file);
        FileOutputStream sos = new FileOutputStream(seamen);
        FileOutputStream nsos = new FileOutputStream(nonseamen);

        PrintStream out = new PrintStream(os);
        PrintStream sout = new PrintStream(sos);
        PrintStream nout = new PrintStream(nsos);

        out.print(Json.toJson(nonSeamenFlightItineraryMap));
        sout.print(Json.toJson(seamenFlightItineraryMap));
        nout.print(Json.toJson(nonSeamanResponse));

        return nonSeamanResponse;
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
        HashMap<Integer, FlightItinerary> flightItineraryHashMap = new HashMap<>();

        List<AirSegmentInformation> airSegmentInformationList=new ArrayList<AirSegmentInformation>();
        Helper.AirSegmentMap allSegments = Helper.createAirSegmentMap(
                travelportResponse.getAirSegmentList().getAirSegment());
        Helper.FlightDetailsMap allDetails = Helper.createFlightDetailsMap(
                travelportResponse.getFlightDetailsList().getFlightDetails());
        List<AirPricingSolution> airPricingSolutions =  travelportResponse.getAirPricingSolution();

        Map<String, BaggageInfo> baggageInfoMap = new HashMap<>();
        /*for(FareInfo fareInfo : travelportResponse.getFareInfoList().getFareInfo()) {
        	TypeWeight maxWeight = fareInfo.getBaggageAllowance().getMaxWeight();
        	BaggageInfo baggageInfo = new BaggageInfo(maxWeight.getUnit().name(), maxWeight.getValue());
        	baggageInfoMap.put(fareInfo.getOrigin(), baggageInfo);
        }*/

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
            flightItinerary.getPricingInformation().setProvider("Travelport");
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
                    String equipment = "";
                    if (airSegment != null) {
                    	equipment = airSegment.getEquipment();
                        if (airSegment.getCarrier() != null) {
                            carrier = airSegment.getCarrier();
                        }
                        if (airSegment.getFlightNumber() != null) {
                            flightNum = airSegment.getFlightNumber();
                        }
                    }

                    AirSegmentInformation airSegmentInformation = new AirSegmentInformation();
                    airSegmentInformation.setEquipment(equipment);
                    airSegmentInformation.setCarrierCode(carrier);
                    airSegmentInformation.setAirline(Airline.getAirlineByCode(carrier));
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
                    airSegmentInformation.setBaggageInfo(baggageInfoMap.get(o));
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

                    flightItinerary.getJourneyList().get(journeyList.indexOf(journey)).setProvider("Provider");
                    flightItinerary.getJourneyList().get(journeyList.indexOf(journey)).getAirSegmentList().add(airSegmentInformation);
                    flightItinerary.getJourneyList().get(journeyList.indexOf(journey)).setAirlinesStrForFilter(" "+airSegmentInformation.getCarrierCode() + " " + airSegmentInformation.getAirline().getAirlineName());

                }
                flightItinerary.getJourneyList().get(journeyList.indexOf(journey)).setTravelTime(journey.getTravelTime());
                flightItinerary.getJourneyList().get(journeyList.indexOf(journey)).setNoOfStops(journeyStopCounter);
                //System.out.println("total travel time"+ flightItinerary.getJourneyList().get(journeyList.indexOf(journey)).getTravelTime().getHours()+ flightItinerary.getJourneyList().get(journeyList.indexOf(journey)).getTravelTime().getMinutes() );
            }

            //System.out.println("-----------");
            //airSolution.getFlightItineraryList().add(flightItinerary);
            flightItineraryHashMap.put(flightItinerary.hashCode(), flightItinerary);
        }
        airSolution.setNonSeamenHashMap(flightItineraryHashMap);
        searchResponse.setAirSolution(airSolution);

        return searchResponse;
    }
}
