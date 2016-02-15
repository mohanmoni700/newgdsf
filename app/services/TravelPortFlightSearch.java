package services;

import com.compassites.GDSWrapper.travelport.Helper;
import com.compassites.GDSWrapper.travelport.LowFareRequestClient;
import com.compassites.constants.TravelportConstants;
import com.compassites.exceptions.IncompleteDetailsMessage;
import com.compassites.exceptions.RetryException;
import com.compassites.model.*;
import com.compassites.model.AirSolution;
import com.compassites.model.FlightInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.gson.JsonObject;
import com.sun.xml.ws.client.ClientTransportException;
import com.travelport.schema.air_v26_0.*;
import com.travelport.schema.common_v26_0.ResponseMessage;
import com.travelport.service.air_v26_0.AirFaultMessage;
import models.Airline;
import models.Airport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import play.libs.Json;
import utils.ErrorMessageHelper;
import utils.StringUtility;
import utils.TravelportHelper;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created with IntelliJ IDEA.
 * User: Renu
 * Date: 5/23/14
 * Time: 3:43 PM
 * To change this template use File | Settings | File Templates.
 */
@Service
public class TravelPortFlightSearch implements FlightSearch {

    static Logger logger = LoggerFactory.getLogger("gds");

    @RetryOnFailure(attempts = 2, delay = 2000, exception = RetryException.class )
    public SearchResponse search (SearchParameters searchParameters) throws IncompleteDetailsMessage, RetryException, IOException {
        logger.debug("[TravelPort] search started at " + new Date());

        SearchResponse seamanResponse = null;
        SearchResponse nonSeamanResponse = null;

        SearchResponse searchResponse = new SearchResponse();

        /*
            Return null if search Date type is arrival
         */
        JsonNode jsonNode =  Json.toJson(searchParameters);
        if(jsonNode.findValue("dateType").asText().equals(DateType.ARRIVAL.name().toString())){
            return null;
        }

        logger.debug("[Travelport] Starting non-seaman search");
        nonSeamanResponse = search(searchParameters, "nonseaman");
        if(nonSeamanResponse != null){
            searchResponse.getAirSolution().setNonSeamenHashMap(nonSeamanResponse.getAirSolution().getNonSeamenHashMap());
        }

        logger.debug("[Travelport] End non-seaman search. Response size: " + nonSeamanResponse.getAirSolution().getSeamenHashMap().size());

        if (searchParameters.getBookingType()==BookingType.SEAMEN){
            logger.debug("[Travelport] Starting seaman search.");
            seamanResponse = search(searchParameters, "seaman");
            if(seamanResponse != null){
                searchResponse.getAirSolution().setSeamenHashMap(seamanResponse.getAirSolution().getNonSeamenHashMap());
            }

            logger.debug("[Travelport] End seaman search. Response size: " + seamanResponse.getAirSolution().getSeamenHashMap().size());
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
            //logger.debug("Seaman Key" + seamanKey);

            for(FlightItinerary nonSeamanFlightItinerary : nonSeamanResponse.getAirSolution().getFlightItineraryList()){
                Journey nonSeamanOnwardJourney = nonSeamanFlightItinerary.getJourneyList().get(0);
                String nonSeamanKey ="";
                for (AirSegmentInformation nonSeamanOnwardAirSegment : nonSeamanOnwardJourney.getAirSegmentList()){
                    nonSeamanKey =  nonSeamanKey + nonSeamanOnwardAirSegment.getFromLocation()+ nonSeamanOnwardAirSegment.getToLocation()+ nonSeamanOnwardAirSegment.getCarrierCode()+"#"+ nonSeamanOnwardAirSegment.getFlightNumber()+nonSeamanOnwardAirSegment.getArrivalTime()+nonSeamanOnwardAirSegment.getDepartureTime();
                }

                //logger.debug("Non Seaman Key" + nonSeamanKey);

                if (nonSeamanKey.equalsIgnoreCase(seamanKey)){
                    //logger.debug("Matched");
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
            logger.debug("[TravelPort] FlightSearch search response at " + new Date());
            List<ResponseMessage> responseMessageList = response.getResponseMessage();
            errorExist = ((response.getAirPricingSolution() == null) || ( response.getAirPricingSolution().size() == 0)) ;
            for (ResponseMessage responseMessage : responseMessageList){
               if("Error".equalsIgnoreCase(responseMessage.getType())){
                    logger.debug("[Travelport] Error received from Travel port : " + responseMessage.getValue());

                    errorCode = ""+responseMessage.getCode();
                    errorMessage = errorMessage + responseMessage.getValue();
                }
            }
        } catch (AirFaultMessage airFaultMessage) {
            //throw new IncompleteDetailsMessage(airFaultMessage.getMessage(), airFaultMessage.getCause());
            logger.error("Travelport search error  ", airFaultMessage);
            airFaultMessage.printStackTrace();
            String code = airFaultMessage.getFaultInfo().getCode();
            if(!TravelportConstants.NO_ITINERARY_ERROR_CODE.contains(code)){
                ErrorMessage errMessage = ErrorMessageHelper.createErrorMessage("partialResults", ErrorMessage.ErrorType.ERROR, "Travelport");
                searchResponse.getErrorMessageList().add(errMessage);
            }
            return searchResponse;
        }catch (ClientTransportException clientTransportException){
            logger.error("Travelport search error  ", clientTransportException);
            clientTransportException.printStackTrace();
            throw new RetryException(clientTransportException.getMessage());
        }catch (Exception e){
            //throw new IncompleteDetailsMessage(e.getMessage(), e.getCause());
            e.printStackTrace();
            logger.error("Travelport search error  ", e);
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
        ConcurrentHashMap<Integer, FlightItinerary> flightItineraryHashMap = new ConcurrentHashMap<>();

        List<AirSegmentInformation> airSegmentInformationList=new ArrayList<AirSegmentInformation>();
        Helper.AirSegmentMap allSegments = Helper.createAirSegmentMap(
                travelportResponse.getAirSegmentList().getAirSegment());
        Helper.FlightDetailsMap allDetails = Helper.createFlightDetailsMap(
                travelportResponse.getFlightDetailsList().getFlightDetails());
        List<AirPricingSolution> airPricingSolutions =  travelportResponse.getAirPricingSolution();

        Map<String, FlightInfo> baggageInfoMap = new HashMap<>();
        for(FareInfo fareInfo : travelportResponse.getFareInfoList().getFareInfo()) {
            if(fareInfo.getBaggageAllowance() != null){
                TypeWeight maxWeight = fareInfo.getBaggageAllowance().getMaxWeight();
                if(maxWeight != null && maxWeight.getUnit() != null) {
                    FlightInfo flightInfo = new FlightInfo();
                    flightInfo.setBaggageAllowance(maxWeight.getValue());
                    flightInfo.setBaggageUnit(maxWeight.getUnit().name());
                    baggageInfoMap.put(fareInfo.getOrigin(), flightInfo);
                }
            }


        }

        flightIteratorLoop: for (Iterator<AirPricingSolution> airPricingSolutionIterator = airPricingSolutions.iterator(); airPricingSolutionIterator.hasNext();){

            AirPricingSolution airPricingSolution = (AirPricingSolution)airPricingSolutionIterator.next();
            FlightItinerary flightItinerary=new FlightItinerary();
//            flightItinerary.setProvider("Travelport");

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

            flightItinerary.getPricingInformation().setGdsCurrency(travelportResponse.getCurrencyType());
            flightItinerary.getPricingInformation().setProvider("Travelport");
            flightItinerary.getPricingInformation().setBasePrice(StringUtility.getDecimalFromString(airPricingSolution.getApproximateBasePrice()));
            flightItinerary.getPricingInformation().setTax(StringUtility.getDecimalFromString(airPricingSolution.getTaxes()));
            flightItinerary.getPricingInformation().setTotalPrice(StringUtility.getDecimalFromString(airPricingSolution.getTotalPrice()));
            flightItinerary.getPricingInformation().setTotalPriceValue(StringUtility.getDecimalFromString(airPricingSolution.getTotalPrice()));
            if(airPricingSolution.getAirPricingInfo().get(0).getCancelPenalty() != null) {
                flightItinerary.getPricingInformation().setFareRules(airPricingSolution.getAirPricingInfo().get(0).getCancelPenalty().getAmount());
                flightItinerary.getPricingInformation().setCancelFee(StringUtility.getDecimalFromString(airPricingSolution.getAirPricingInfo().get(0).getCancelPenalty().getAmount()));
            }
            TravelportHelper.getPassengerTaxes(flightItinerary.getPricingInformation(), airPricingSolution.getAirPricingInfo());
            
            //System.out.print("Price:"+ airPricingSolution.getTotalPrice());
            //System.out.print(" Travelport BasePrice "+airPricingSolution.getBasePrice() +", ");
            //System.out.print("Taxes "+airPricingSolution.getTaxes()+"]");

            /*List<Integer> connectionIndexes = new ArrayList<>();
            for(Connection connection :airPricingSolution.getConnection()){
                connectionIndexes.add(connection.getSegmentIndex());
            }
            flightItinerary.getPricingInformation().setConnectionIndexes(connectionIndexes);*/

            List<com.travelport.schema.air_v26_0.Journey> journeyList = airPricingSolution.getJourney();
            
            for (Iterator<com.travelport.schema.air_v26_0.Journey> journeyIterator = journeyList.iterator(); journeyIterator.hasNext();) {

                com.travelport.schema.air_v26_0.Journey journey = journeyIterator.next();
                flightItinerary.AddBlankJourney();
                int journeyStopCounter = -1;
                //logger.debug("CabinClass " + airPricingSolution.getAirPricingInfo().get(0).getBookingInfo().get(journeyList.indexOf(journey)).getCabinClass());
                List<AirSegmentRef> airSegmentRefList = journey.getAirSegmentRef();
                for (Iterator<AirSegmentRef> airSegmentRefIterator = airSegmentRefList.iterator(); airSegmentRefIterator.hasNext(); ) {
                    journeyStopCounter++;
                    AirSegmentRef airSegmentRef = airSegmentRefIterator.next();
                    TypeBaseAirSegment airSegment = allSegments.getByRef(airSegmentRef);
//                    String carrier = "??";
                    String flightNum = "???";
//                    String equipment = "";
//                    String operatingCarrier = null;
                    
                    AirSegmentInformation airSegmentInformation = new AirSegmentInformation();
                    
                    if (airSegment != null) {
                    	airSegmentInformation.setEquipment(airSegment.getEquipment());
                        if (airSegment.getCarrier() != null) {
                            String carrier = airSegment.getCarrier();
                            if(carrier != null) {
                            	airSegmentInformation.setCarrierCode(carrier);
                                airSegmentInformation.setAirline(Airline.getAirlineByCode(carrier));
                            }
                        }
                        if(airSegment.getCodeshareInfo() != null) {
                        	String operatingCarrier = airSegment.getCodeshareInfo().getOperatingCarrier();
                        	if(operatingCarrier != null) {
        	                    airSegmentInformation.setOperatingCarrierCode(operatingCarrier);
        	                    airSegmentInformation.setOperatingAirline(Airline.getAirlineByCode(operatingCarrier));
                            }
                        }
                        if (airSegment.getFlightNumber() != null)
                            flightNum = airSegment.getFlightNumber();
                    }
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
                    airSegmentInformation.setFlightInfo(baggageInfoMap.get(o));
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
                        airSegmentInformation.setArrivalDate(sdf.parse(atime));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }

                    airSegmentInformation.setArrivalTime(atime);


                    if ((flightDetails != null) && (flightDetails.getFlightTime() != null)) {
                        //logger.debug(" (flight time " + flightDetails.getFlightTime() + " minutes)");
                        airSegmentInformation.setTravelTime(String.valueOf(flightDetails.getFlightTime()));
                    } else {
                        //logger.debug();
                    }
                    airSegmentInformation.setFlightDetailsKey(flightDetails.getKey());
                    airSegmentInformation.setAirSegmentKey(airSegment.getKey());

                    flightItinerary.getJourneyList().get(journeyList.indexOf(journey)).setProvider("Provider");
                    flightItinerary.getJourneyList().get(journeyList.indexOf(journey)).getAirSegmentList().add(airSegmentInformation);
                    flightItinerary.getJourneyList().get(journeyList.indexOf(journey)).setAirlinesStrForFilter(" "+airSegmentInformation.getCarrierCode() + " " + airSegmentInformation.getAirline().getAirlineName());

                }
                
                
                  getConnectionTime(flightItinerary.getJourneyList().get(journeyList.indexOf(journey)).getAirSegmentList());
                flightItinerary.getJourneyList().get(journeyList.indexOf(journey)).setTravelTime(journey.getTravelTime());
                flightItinerary.getJourneyList().get(journeyList.indexOf(journey)).setNoOfStops(journeyStopCounter);
                
                //logger.debug("total travel time"+ flightItinerary.getJourneyList().get(journeyList.indexOf(journey)).getTravelTime().getHours()+ flightItinerary.getJourneyList().get(journeyList.indexOf(journey)).getTravelTime().getMinutes() );
            }

            //logger.debug("-----------");
            //airSolution.getFlightItineraryList().add(flightItinerary);
            flightItinerary.setNonSeamenJourneyList(flightItinerary.getJourneyList());
            flightItineraryHashMap.put(flightItinerary.hashCode(), flightItinerary);
           // getConnectionTimes(flightItinerary.getJourneyList().get(journeyList.indexOf(journey)).getAirSegmentList());
        }
        airSolution.setNonSeamenHashMap(flightItineraryHashMap);
        searchResponse.setAirSolution(airSolution);
        // flightItinerary.getJourneyList().get(journeyList.indexOf(journey)).getAirSegmentList();
        
        

        return searchResponse;
    }
    

    
    private void getConnectionTime(List<AirSegmentInformation> airSegments) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS"); 
		if (airSegments.size() > 1) {
			for (int i = 1; i < airSegments.size(); i++) {
				Long arrivalTime;
				try {
					arrivalTime = dateFormat.parse(
							airSegments.get(i - 1).getArrivalTime()).getTime();
				
				Long departureTime = dateFormat.parse(
						airSegments.get(i).getDepartureTime()).getTime();
				Long transit = departureTime - arrivalTime;
				airSegments.get(i - 1).setConnectionTime(
						Integer.valueOf((int) (transit / 60000)));
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
}
