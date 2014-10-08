package services;

import com.amadeus.xml.fmptbr_12_4_1a.*;
import com.compassites.GDSWrapper.amadeus.SearchFlights;
import com.compassites.GDSWrapper.amadeus.ServiceHandler;
import com.compassites.exceptions.IncompleteDetailsMessage;
import com.compassites.exceptions.RetryException;
import com.compassites.model.*;
import com.sun.xml.ws.client.ClientTransportException;
import com.sun.xml.ws.fault.ServerSOAPFaultException;
import models.AirlineCode;
import models.Airport;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Minutes;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.stereotype.Service;
import play.Logger;
import play.libs.Json;
import utils.ErrorMessageHelper;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;


/**
 * Created with IntelliJ IDEA.
 * User: Renu
 * Date: 5/23/14
 * Time: 3:50 PM
 * To change this template use File | Settings | File Templates.
 */
@Service
public class AmadeusFlightSearch implements FlightSearch{

    @RetryOnFailure(attempts = 2, delay = 2000, exception = RetryException.class)
    public SearchResponse search(SearchParameters searchParameters) throws Exception, IncompleteDetailsMessage {
        Logger.info("AmadeusFlightSearch called at : " + new Date());
        SearchFlights searchFlights = new SearchFlights();
        SearchResponse searchResponse = new SearchResponse();
        searchResponse.setProvider("Amadeus");
        FareMasterPricerTravelBoardSearchReply fareMasterPricerTravelBoardSearchReply = null;
        FareMasterPricerTravelBoardSearchReply seamenReply = null;
        try {
            ServiceHandler serviceHandler = new ServiceHandler();
            serviceHandler.logIn();
            if (searchParameters.getBookingType() == BookingType.SEAMEN) {
                seamenReply = serviceHandler.searchAirlines(searchParameters);
                searchParameters.setBookingType(BookingType.NON_MARINE);
                fareMasterPricerTravelBoardSearchReply = serviceHandler.searchAirlines(searchParameters);
                searchParameters.setBookingType(BookingType.SEAMEN);
                File file = new File("roundtrip.json");
                FileOutputStream os = new FileOutputStream(file);
                PrintStream out = new PrintStream(os);
                out.print(Json.toJson(seamenReply));

                file=new File("nonseamenAmadeusResponseCF.json");
                os=new FileOutputStream(file);
                out = new PrintStream(os);
                out.print(Json.toJson(fareMasterPricerTravelBoardSearchReply));

            } else {
                fareMasterPricerTravelBoardSearchReply = serviceHandler.searchAirlines(searchParameters);
            }
        } catch (ServerSOAPFaultException soapFaultException) {

            soapFaultException.printStackTrace();
            throw new IncompleteDetailsMessage(soapFaultException.getMessage(), soapFaultException.getCause());
        } catch (ClientTransportException clientTransportException) {

            //throw new IncompleteDetailsMessage(soapFaultException.getMessage(), soapFaultException.getCause());

            ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage("partialResults", ErrorMessage.ErrorType.ERROR, "Amadeus");
            searchResponse.getErrorMessageList().add(errorMessage);
            return searchResponse;
        } catch (Exception e) {
            e.printStackTrace();
            //throw new IncompleteDetailsMessage(e.getMessage(), e.getCause());

            ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage("partialResults", ErrorMessage.ErrorType.ERROR, "Amadeus");
            searchResponse.getErrorMessageList().add(errorMessage);
            return searchResponse;
        }

        Logger.info("AmadeusFlightSearch search reponse at : " + new Date());
        FareMasterPricerTravelBoardSearchReply.ErrorMessage seamenErrorMessage = null;
        FareMasterPricerTravelBoardSearchReply.ErrorMessage errorMessage = fareMasterPricerTravelBoardSearchReply.getErrorMessage();
        if (seamenReply != null) {
            seamenErrorMessage = seamenReply.getErrorMessage();
        }

        AirSolution airSolution = new AirSolution();
        if (errorMessage != null) {
            String errorCode = errorMessage.getApplicationError().getApplicationErrorDetail().getError();
            InputStream input = null;
            try {

                errorCode = "amadeus." + errorCode;
                boolean errorCodeExist = ErrorMessageHelper.checkErrorCodeExist(errorCode);
                if (errorCodeExist) {
                    ErrorMessage errMessage = ErrorMessageHelper.createErrorMessage(errorCode, ErrorMessage.ErrorType.WARNING, "Amadeus");
                    throw new RetryException(errMessage.getMessage());
                }
                ErrorMessage errMessage = ErrorMessageHelper.createErrorMessage(errorCode, ErrorMessage.ErrorType.WARNING, "Amadeus");

                searchResponse.getErrorMessageList().add(errMessage);
            } catch (Exception e) {

                e.printStackTrace();
            }
        } else {
            //return flight
            airSolution = createAirSolutionFromRecommendation(fareMasterPricerTravelBoardSearchReply);
            if (searchParameters.getBookingType() == BookingType.SEAMEN && seamenErrorMessage == null) {
                AirSolution seamenSolution = new AirSolution();
                seamenSolution = createAirSolutionFromRecommendation(seamenReply);
                addSeamenFareToSolution(airSolution, seamenSolution);
            }
        }
        searchResponse.setAirSolution(airSolution);
        return searchResponse;
    }

    //@Override
    public String provider() {
        return "Amadeus";
    }

    //earlier without refactoring
    /*private AirSolution createAirSolutionFromRecommendations(FareMasterPricerTravelBoardSearchReply fareMasterPricerTravelBoardSearchReply) {
        AirSolution airSolution = new AirSolution();
        List<FlightItinerary> flightItineraries = new ArrayList<FlightItinerary>();
        String currency = fareMasterPricerTravelBoardSearchReply.getConversionRate().getConversionRateDetail().get(0).getCurrency();

        //each flightindex has one itinerary
        //each itinerary has multiple segments each corresponding to one flight in the itinerary in the airSegmentInformation
        for (FareMasterPricerTravelBoardSearchReply.Recommendation recommendation : fareMasterPricerTravelBoardSearchReply.getRecommendation()) {

            BigDecimal totalAmount = BigDecimal.valueOf(0);
            BigDecimal taxAmount = BigDecimal.valueOf(0);
            BigDecimal baseAmount = BigDecimal.valueOf(0);
            for (FareMasterPricerTravelBoardSearchReply.Recommendation.PaxFareProduct paxFareProduct : recommendation.getPaxFareProduct()) {
                taxAmount = taxAmount.add(paxFareProduct.getPaxFareDetail().getTotalTaxAmount());
                totalAmount = totalAmount.add(paxFareProduct.getPaxFareDetail().getTotalFareAmount());
                baseAmount = baseAmount.add(totalAmount.subtract(taxAmount));

            }

            //String cabinClass = recommendation.getPaxFareProduct().get(0).getFareDetails().get(0).getMajCabin().get(0).getBookingClassDetails().get(0).getDesignator();

            for (ReferenceInfoType segmentRef : recommendation.getSegmentFlightRef()) {
                int returnCounter = 0;
                FlightItinerary flightItinerary = new FlightItinerary();
                flightItinerary.setProvider("Amadeus");
                for (ReferencingDetailsType191583C referencingDetailsType : segmentRef.getReferencingDetail()) {
                    int multiStopCounter = 0;
                    int journeyStopCounter = -1;
                    BigInteger flightDetailReference = referencingDetailsType.getRefNumber();

                    List<FareMasterPricerTravelBoardSearchReply.Recommendation.PaxFareProduct.FareDetails.GroupOfFares> groupOfFaresList = recommendation.getPaxFareProduct().get(0).getFareDetails().get(0).getGroupOfFares();
                    FareMasterPricerTravelBoardSearchReply.FlightIndex.GroupOfFlights groupOfFlights = fareMasterPricerTravelBoardSearchReply.getFlightIndex().get(multiStopCounter).getGroupOfFlights().get(flightDetailReference.intValue() - 1);
                    String prevArrivalTime = "";
                    int segmentIndex = 0;
                    List<FareJourney> fareJourneyList = new ArrayList<>();
                    List<FareSegment> fareSegmentList = new ArrayList<>();
                    for (FareMasterPricerTravelBoardSearchReply.FlightIndex.GroupOfFlights.FlightDetails flightDetails : groupOfFlights.getFlightDetails()) {
                        String rbd = groupOfFaresList.get(segmentIndex).getProductInformation().getCabinProduct().getRbd();
                        FareSegment fareSegment = new FareSegment();
                        fareSegment.setBookingClass(rbd);
                        fareSegmentList.add(fareSegment);
                        AirSegmentInformation airSegmentInformation = createSegment(flightDetails.getFlightInformation(), prevArrivalTime);
                        airSegmentInformation.setBookingClass(rbd);
                        if (journeyStopCounter > -1) {
                            flightItinerary.getJourneyList().get(multiStopCounter).getAirSegmentList().get(journeyStopCounter).setConnectionTime(airSegmentInformation.getConnectionTime());
                            airSegmentInformation.setConnectionTime(0);
                        }

                        flightItinerary.AddBlankJourney();
                        flightItinerary.getPricingInformation().addBlankFareJourney();
                        flightItinerary.getPricingInformation().getFareJourneyList().get(multiStopCounter).getFareSegmentList().add(fareSegment);
                        flightItinerary.getJourneyList().get(multiStopCounter).getAirSegmentList().add(airSegmentInformation);
                        flightItinerary.getJourneyList().get(multiStopCounter).setAirlinesStrForFilter(" " + airSegmentInformation.getCarrierCode() + " " + airSegmentInformation.getAirline().airline);
                        prevArrivalTime = airSegmentInformation.getArrivalTime();
                        journeyStopCounter++;
                        segmentIndex++;
                    }

                    flightItinerary.getJourneyList().get(multiStopCounter).setNoOfStops(journeyStopCounter);

                    for (ProposedSegmentDetailsType proposedSegmentDetailsTypes : groupOfFlights.getPropFlightGrDetail().getFlightProposal()) {
                        if ("EFT".equals(proposedSegmentDetailsTypes.getUnitQualifier())) {
                            Journey journey = flightItinerary.getJourneyList().get(0);
                            String elapsedTime = proposedSegmentDetailsTypes.getRef();
                            String strHours = elapsedTime.substring(0, 2);
                            String strMinutes = elapsedTime.substring(2);
                            Duration duration = null;
                            Integer hours = new Integer(strHours);
                            int days = hours / 24;
                            int dayHours = hours - (days * 24);
                            try {
                                duration = DatatypeFactory.newInstance().newDuration(true, 0, 0, days, dayHours, new Integer(strMinutes), 0);
                            } catch (DatatypeConfigurationException e) {
                                e.printStackTrace();
                            }
                            journey.setTravelTime(duration);
                            break;
                        }
                    }
                    multiStopCounter++;

                }
                flightItinerary.getPricingInformation().setBasePrice(baseAmount.toString());
                flightItinerary.getPricingInformation().setTax(taxAmount.toString());
                flightItinerary.getPricingInformation().setTotalPrice(totalAmount.toString());
                flightItinerary.getPricingInformation().setTotalPriceValue(totalAmount.longValue());
                flightItinerary.getPricingInformation().setCurrency(currency);
                flightItineraries.add(flightItinerary);
            }


        }

        airSolution.setFlightItineraryList(flightItineraries);
        return airSolution;
    }*/

    //for round trip and refactored
    private AirSolution createAirSolutionFromRecommendation(FareMasterPricerTravelBoardSearchReply fareMasterPricerTravelBoardSearchReply) {
        AirSolution airSolution = new AirSolution();
        airSolution.setFlightItineraryList(createFlightItineraryList(airSolution, fareMasterPricerTravelBoardSearchReply));
        return airSolution;
    }

    //list with all flight informaition
    private List<FareMasterPricerTravelBoardSearchReply.FlightIndex> flightIndexList=new ArrayList<>();

    private List<FlightItinerary> createFlightItineraryList(AirSolution airSolution, FareMasterPricerTravelBoardSearchReply fareMasterPricerTravelBoardSearchReply) {
        List<FlightItinerary> flightItineraryList = new ArrayList<>();
        String currency = fareMasterPricerTravelBoardSearchReply.getConversionRate().getConversionRateDetail().get(0).getCurrency();
        flightIndexList=fareMasterPricerTravelBoardSearchReply.getFlightIndex();
        for (FareMasterPricerTravelBoardSearchReply.Recommendation recommendation : fareMasterPricerTravelBoardSearchReply.getRecommendation()) {
            for (ReferenceInfoType segmentRef : recommendation.getSegmentFlightRef()) {
                FlightItinerary flightItinerary = new FlightItinerary();
                flightItinerary.setProvider("Amadeus");
                //pricing information
                List<FareMasterPricerTravelBoardSearchReply.Recommendation.PaxFareProduct.FareDetails.GroupOfFares> groupOfFaresList = recommendation.getPaxFareProduct().get(0).getFareDetails().get(0).getGroupOfFares();
                FareMasterPricerTravelBoardSearchReply.Recommendation.PaxFareProduct paxFareProduct = recommendation.getPaxFareProduct().get(0);
                flightItinerary.setPricingInformation(setPricingInformation(paxFareProduct.getPaxFareDetail().getTotalTaxAmount(), paxFareProduct.getPaxFareDetail().getTotalFareAmount(), currency));
                flightItinerary.getPricingInformation().setPaxFareDetailsList(createFareDetails(recommendation));
                //journey information

                flightItinerary=createJourneyInformation(segmentRef,flightItinerary);
                flightItineraryList.add(flightItinerary);
            }
        }
        return flightItineraryList;
    }

    private FlightItinerary createJourneyInformation(ReferenceInfoType segmentRef,FlightItinerary flightItinerary){
        /*Journey forwardJourney=new Journey();
        Journey returnJourney=new Journey();*/
        int flightIndexNumber=0;
        for(ReferencingDetailsType191583C referencingDetailsType : segmentRef.getReferencingDetail()) {
            //0 is for forward journey and refQualifier should be S for segment
            if (referencingDetailsType.getRefQualifier().equalsIgnoreCase("S") ) {
                Journey journey=new Journey();
                flightItinerary.getJourneyList().add(setJourney(journey,flightIndexList.get(flightIndexNumber).getGroupOfFlights().get(referencingDetailsType.getRefNumber().intValue()-1)));
                //flightItinerary.getJourneyList().add(setJourney(flightIndexNumber == 0 ? forwardJourney : returnJourney, flightIndexList.get(flightIndexNumber).getGroupOfFlights().get(referencingDetailsType.getRefNumber().intValue()-1)));
                //flightIndexNumber = ++flightIndexNumber % 2;
                ++flightIndexNumber;
            }
        }
        return flightItinerary;
    }

    private Duration setTravelDuraion(String totalElapsedTime){
        String strHours = totalElapsedTime.substring(0, 2);
        String strMinutes = totalElapsedTime.substring(2);
        Duration duration = null;
        Integer hours = new Integer(strHours);
        int days = hours / 24;
        int dayHours = hours - (days * 24);
        try {
            duration = DatatypeFactory.newInstance().newDuration(true, 0, 0, days, dayHours, new Integer(strMinutes), 0);
        } catch (DatatypeConfigurationException e) {
            e.printStackTrace();
        }
        return duration;
    }

    private Journey setJourney(Journey journey,FareMasterPricerTravelBoardSearchReply.FlightIndex.GroupOfFlights groupOfFlight){
        //no of stops
        journey.setNoOfStops(groupOfFlight.getFlightDetails().size()-1);
        //set travel time
        Iterator iterator=groupOfFlight.getPropFlightGrDetail().getFlightProposal().iterator();
        if(iterator.hasNext()){
            ProposedSegmentDetailsType proposedSegmentDetailsType=(ProposedSegmentDetailsType)iterator.next();
            if(proposedSegmentDetailsType.getUnitQualifier()=="EFT"){
                journey.setTravelTime(setTravelDuraion(proposedSegmentDetailsType.getRef()));
            }
        }
        //set segments information
        for(FareMasterPricerTravelBoardSearchReply.FlightIndex.GroupOfFlights.FlightDetails flightDetails:groupOfFlight.getFlightDetails()){
            journey.getAirSegmentList().add(setSegmentInformation(flightDetails));
        }

        return journey;
    }

    private AirSegmentInformation setSegmentInformation(FareMasterPricerTravelBoardSearchReply.FlightIndex.GroupOfFlights.FlightDetails flightDetails){
        AirSegmentInformation airSegmentInformation = new AirSegmentInformation();
        TravelProductType flightInformation=flightDetails.getFlightInformation();

        airSegmentInformation.setCarrierCode(flightInformation.getCompanyId().getMarketingCarrier());
        airSegmentInformation.setFlightNumber(flightInformation.getFlightOrtrainNumber());

        //airSegmentInformation.setArrivalTime(flightInformation.getProductDateTime().getTimeOfArrival());
        //airSegmentInformation.setDepartureTime(flightInformation.getProductDateTime().getDateOfDeparture());
        airSegmentInformation.setFromTerminal(flightInformation.getLocation().get(0).getTerminal());
        airSegmentInformation.setToTerminal(flightInformation.getLocation().get(1).getTerminal());
        airSegmentInformation.setToDate(flightInformation.getProductDateTime().getDateOfDeparture());
        airSegmentInformation.setFromDate(flightInformation.getProductDateTime().getDateOfArrival());
        airSegmentInformation.setToLocation(flightInformation.getLocation().get(1).getLocationId());
        airSegmentInformation.setFromLocation(flightInformation.getLocation().get(0).getLocationId());
        Airport fromAirport = new Airport();
        Airport toAirport = new Airport();
        fromAirport = Airport.getAiport(airSegmentInformation.getFromLocation());
        toAirport = Airport.getAiport(airSegmentInformation.getToLocation());

        SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyHHmm");
        String DATE_FORMAT = "ddMMyyHHmm";
        DateTimeFormatter DATETIME_FORMATTER = DateTimeFormat.forPattern(DATE_FORMAT);
        DateTimeZone dateTimeZone = DateTimeZone.forID(fromAirport.getTime_zone());
        DateTime departureDate = DATETIME_FORMATTER.withZone(dateTimeZone).parseDateTime(flightInformation.getProductDateTime().getDateOfDeparture() + flightInformation.getProductDateTime().getTimeOfDeparture());
        dateTimeZone = DateTimeZone.forID(toAirport.getTime_zone());
        DateTime arrivalDate = DATETIME_FORMATTER.withZone(dateTimeZone).parseDateTime(flightInformation.getProductDateTime().getDateOfArrival() + flightInformation.getProductDateTime().getTimeOfArrival());

        airSegmentInformation.setDepartureDate(departureDate.toDate());
        airSegmentInformation.setDepartureTime(departureDate.toString());
        airSegmentInformation.setArrivalTime(arrivalDate.toString());
        airSegmentInformation.setFromAirport(fromAirport);
        airSegmentInformation.setToAirport(toAirport);
        Minutes diff = Minutes.minutesBetween(departureDate, arrivalDate);

        /*if (prevArrivalTime.equalsIgnoreCase("")) {
            airSegmentInformation.setConnectionTime(0);
        } else {
            String pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
            DateTimeFormatter dtf = DateTimeFormat.forPattern(pattern);
            DateTime prevArrivalDateTime = dtf.parseDateTime(prevArrivalTime);
            Minutes diffDeparture = Minutes.minutesBetween(prevArrivalDateTime, departureDate);
            airSegmentInformation.setConnectionTime(diffDeparture.getMinutes());
        }*/

        airSegmentInformation.setTravelTime("" + diff.getMinutes());
        if (flightInformation.getCompanyId() != null && flightInformation.getCompanyId().getMarketingCarrier() != null && flightInformation.getCompanyId().getMarketingCarrier().length() >= 2) {
            airSegmentInformation.setAirline(AirlineCode.getAirlineByCode(flightInformation.getCompanyId().getMarketingCarrier()));
        }
        return airSegmentInformation;
    }

    private PricingInformation setPricingInformation(BigDecimal taxAmount, BigDecimal totalAmount, String currency) {
        PricingInformation pricingInformation = new PricingInformation();
        pricingInformation.setBasePrice(totalAmount.subtract(taxAmount).toString());
        pricingInformation.setTax(taxAmount.toString());
        pricingInformation.setTotalPrice(totalAmount.toString());
        pricingInformation.setTotalPriceValue(totalAmount.longValue());
        pricingInformation.setCurrency(currency);
        return pricingInformation;
    }

    public static SimpleDateFormat searchFormat = new SimpleDateFormat("ddMMyy-kkmm");

    private AirSegmentInformation createSegment(TravelProductType flightInformation, String prevArrivalTime) {

        AirSegmentInformation airSegmentInformation = new AirSegmentInformation();
        airSegmentInformation.setCarrierCode(flightInformation.getCompanyId().getMarketingCarrier());
        airSegmentInformation.setFlightNumber(flightInformation.getFlightOrtrainNumber());

        //airSegmentInformation.setArrivalTime(flightInformation.getProductDateTime().getTimeOfArrival());
        //airSegmentInformation.setDepartureTime(flightInformation.getProductDateTime().getDateOfDeparture());
        airSegmentInformation.setFromTerminal(flightInformation.getLocation().get(0).getTerminal());
        airSegmentInformation.setToTerminal(flightInformation.getLocation().get(1).getTerminal());
        airSegmentInformation.setToDate(flightInformation.getProductDateTime().getDateOfDeparture());
        airSegmentInformation.setFromDate(flightInformation.getProductDateTime().getDateOfArrival());
        airSegmentInformation.setToLocation(flightInformation.getLocation().get(1).getLocationId());
        airSegmentInformation.setFromLocation(flightInformation.getLocation().get(0).getLocationId());
        Airport fromAirport = new Airport();
        Airport toAirport = new Airport();
        fromAirport = Airport.getAiport(airSegmentInformation.getFromLocation());
        toAirport = Airport.getAiport(airSegmentInformation.getToLocation());

        SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyHHmm");
        String DATE_FORMAT = "ddMMyyHHmm";
        DateTimeFormatter DATETIME_FORMATTER = DateTimeFormat.forPattern(DATE_FORMAT);
        DateTimeZone dateTimeZone = DateTimeZone.forID(fromAirport.getTime_zone());
        DateTime departureDate = DATETIME_FORMATTER.withZone(dateTimeZone).parseDateTime(flightInformation.getProductDateTime().getDateOfDeparture() + flightInformation.getProductDateTime().getTimeOfDeparture());
        dateTimeZone = DateTimeZone.forID(toAirport.getTime_zone());
        DateTime arrivalDate = DATETIME_FORMATTER.withZone(dateTimeZone).parseDateTime(flightInformation.getProductDateTime().getDateOfArrival() + flightInformation.getProductDateTime().getTimeOfArrival());

        airSegmentInformation.setDepartureDate(departureDate.toDate());
        airSegmentInformation.setDepartureTime(departureDate.toString());
        airSegmentInformation.setArrivalTime(arrivalDate.toString());
        airSegmentInformation.setFromAirport(fromAirport);
        airSegmentInformation.setToAirport(toAirport);
        Minutes diff = Minutes.minutesBetween(departureDate, arrivalDate);

        if (prevArrivalTime.equalsIgnoreCase("")) {
            airSegmentInformation.setConnectionTime(0);
        } else {
            String pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
            DateTimeFormatter dtf = DateTimeFormat.forPattern(pattern);
            DateTime prevArrivalDateTime = dtf.parseDateTime(prevArrivalTime);
            Minutes diffDeparture = Minutes.minutesBetween(prevArrivalDateTime, departureDate);
            airSegmentInformation.setConnectionTime(diffDeparture.getMinutes());
        }

        airSegmentInformation.setTravelTime("" + diff.getMinutes());
        if (flightInformation.getCompanyId() != null && flightInformation.getCompanyId().getMarketingCarrier() != null && flightInformation.getCompanyId().getMarketingCarrier().length() >= 2) {
            airSegmentInformation.setAirline(AirlineCode.getAirlineByCode(flightInformation.getCompanyId().getMarketingCarrier()));
        }

        return airSegmentInformation;
    }

    private SearchResponse addSeamenFareToSolution(AirSolution allSolution, AirSolution seamenSolution) {
        System.out.println();
        System.out.println("==============================================================================");
        System.out.println("[Amadeus] All Solution Length:::::" + allSolution.getFlightItineraryList().size());
        System.out.println("[Amadeus] Seamen Solution Length:::::" + seamenSolution.getFlightItineraryList().size());
        System.out.println("==============================================================================");
        System.out.println();
        SearchResponse searchResponse = new SearchResponse();
        HashMap<Integer, FlightItinerary> allFaresHash = new HashMap<>();
        HashMap<Integer, FlightItinerary> seamenFareHash = new HashMap<>();
        for (FlightItinerary flightItinerary : seamenSolution.getFlightItineraryList()) {
            seamenFareHash.put(flightItinerary.hashCode(), flightItinerary);
        }
        for (FlightItinerary flightItinerary : allSolution.getFlightItineraryList()) {
            allFaresHash.put(flightItinerary.hashCode(), flightItinerary);
        }
        for (Integer hashKey : seamenFareHash.keySet()) {
            FlightItinerary seamenItinerary = null;
            if (allFaresHash.containsKey(hashKey)) {
                seamenItinerary = allFaresHash.get(hashKey);
                seamenItinerary.setPriceOnlyPTC(true);
                seamenItinerary.setPricingMessage(seamenFareHash.get(hashKey).getPricingMessage());
                seamenItinerary.setSeamanPricingInformation(seamenFareHash.get(hashKey).getPricingInformation());
                allFaresHash.put(hashKey, seamenItinerary);
            } else {
                seamenItinerary = seamenFareHash.get(hashKey);
                seamenItinerary.setPriceOnlyPTC(true);
                seamenItinerary.setSeamanPricingInformation(seamenItinerary.getPricingInformation());
                seamenItinerary.setPricingInformation(null);
                allFaresHash.put(hashKey, seamenItinerary);
            }
        }
        AirSolution airSolution = new AirSolution();
        airSolution.setFlightItineraryList(new ArrayList<FlightItinerary>(allFaresHash.values()));
        searchResponse.setAirSolution(airSolution);
        return searchResponse;
    }


    private List<PAXFareDetails> createFareDetails(FareMasterPricerTravelBoardSearchReply.Recommendation recommendation){
        List<PAXFareDetails> paxFareDetailsList = new ArrayList<>();
        for(FareMasterPricerTravelBoardSearchReply.Recommendation.PaxFareProduct paxFareProduct :recommendation.getPaxFareProduct()){
            PAXFareDetails paxFareDetails = new PAXFareDetails();
            for(FareMasterPricerTravelBoardSearchReply.Recommendation.PaxFareProduct.FareDetails fareDetails :paxFareProduct.getFareDetails()){
                FareJourney fareJourney = new FareJourney();
                for(FareMasterPricerTravelBoardSearchReply.Recommendation.PaxFareProduct.FareDetails.GroupOfFares groupOfFares: fareDetails.getGroupOfFares()){
                    FareSegment fareSegment = new FareSegment();
                    fareSegment.setBookingClass(groupOfFares.getProductInformation().getCabinProduct().getRbd());
                    paxFareDetails.setPassengerTypeCode(PassengerTypeCode.valueOf(groupOfFares.getProductInformation().getFareProductDetail().getPassengerType()));
                    fareJourney.getFareSegmentList().add(fareSegment);
                }
                paxFareDetails.getFareJourneyList().add(fareJourney);
            }
            paxFareDetailsList.add(paxFareDetails);
        }
        return paxFareDetailsList;
    }
}
