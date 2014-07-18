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

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
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
public class AmadeusFlightSearch implements FlightSearch {


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
            } else {
                fareMasterPricerTravelBoardSearchReply = serviceHandler.searchAirlines(searchParameters);
            }
        } catch (ServerSOAPFaultException soapFaultException) {

            soapFaultException.printStackTrace();
            throw new IncompleteDetailsMessage(soapFaultException.getMessage(), soapFaultException.getCause());
        } catch (ClientTransportException clientTransportException) {

            clientTransportException.printStackTrace();
            throw new RetryException(clientTransportException.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            throw new IncompleteDetailsMessage(e.getMessage(), e.getCause());
        }

        Logger.info("AmadeusFlightSearch search reponse at : " + new Date());
        FareMasterPricerTravelBoardSearchReply.ErrorMessage errorMessage = fareMasterPricerTravelBoardSearchReply.getErrorMessage();
        AirSolution airSolution = new AirSolution();
        if (errorMessage != null) {
            String errorCode = errorMessage.getApplicationError().getApplicationErrorDetail().getError();
            Properties prop = new Properties();
            InputStream input = null;
            try {
                input = new FileInputStream("conf/amadeusErrorCodes.properties");
                prop.load(input);
                if (!prop.containsKey(errorCode)) {
                    throw new RetryException(prop.getProperty(errorCode));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            airSolution = createAirSolutionFromRecommendations(fareMasterPricerTravelBoardSearchReply);
            if (searchParameters.getBookingType() == BookingType.SEAMEN) {
                AirSolution seamenSolution = new AirSolution();
                seamenSolution = createAirSolutionFromRecommendations(seamenReply);
                addSeamenFareToSolution(airSolution, seamenSolution);
            }
        }
        searchResponse.setAirSolution(airSolution);
        return searchResponse;
    }

    private AirSolution createAirSolutionFromRecommendations(FareMasterPricerTravelBoardSearchReply fareMasterPricerTravelBoardSearchReply) {
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
                baseAmount = baseAmount.add(paxFareProduct.getPaxFareDetail().getTotalFareAmount());
                totalAmount = totalAmount.add(taxAmount).add(baseAmount);
            }


            for (ReferenceInfoType segmentRef : recommendation.getSegmentFlightRef()) {
                int i = 0;
                FlightItinerary flightItinerary = new FlightItinerary();
                flightItinerary.setProvider("Amadeus");
                for (ReferencingDetailsType191583C referencingDetailsType : segmentRef.getReferencingDetail()) {
                    BigInteger flightDetailReference = referencingDetailsType.getRefNumber();
                    FareMasterPricerTravelBoardSearchReply.FlightIndex.GroupOfFlights groupOfFlights = fareMasterPricerTravelBoardSearchReply.getFlightIndex().get(i).getGroupOfFlights().get(flightDetailReference.intValue() - 1);
                    for (FareMasterPricerTravelBoardSearchReply.FlightIndex.GroupOfFlights.FlightDetails flightDetails : groupOfFlights.getFlightDetails()) {
                        AirSegmentInformation airSegmentInformation = createSegment(flightDetails.getFlightInformation());
                        flightItinerary.AddBlankJourney();
                        flightItinerary.getJourneyList().get(i).getAirSegmentList().add(airSegmentInformation);

                    }
                    for (ProposedSegmentDetailsType proposedSegmentDetailsTypes : groupOfFlights.getPropFlightGrDetail().getFlightProposal()) {
                        if ("EFT".equals(proposedSegmentDetailsTypes.getUnitQualifier())) {
                            FlightItinerary.Journey journey = flightItinerary.getJourneyList().get(0);
                            String elapsedTime = proposedSegmentDetailsTypes.getRef();
                            String hours = elapsedTime.substring(0, 2);
                            String minutes = elapsedTime.substring(2);
                            Duration duration = null;
                            try {
                                duration = DatatypeFactory.newInstance().newDuration(true, 0, 0, 0, new Integer(hours), new Integer(minutes), 0);
                            } catch (DatatypeConfigurationException e) {
                                e.printStackTrace();
                            }
                            journey.setTravelTime(duration);
                            break;
                        }
                    }
                    i++;

                }
                flightItinerary.getPricingInformation().setBasePrice(currency + baseAmount.toString());
                flightItinerary.getPricingInformation().setTax(currency + taxAmount.toString());
                flightItinerary.getPricingInformation().setTotalPrice(currency + totalAmount.toString());
                flightItinerary.getPricingInformation().setCurrency(currency);
                flightItineraries.add(flightItinerary);
            }
        }

        airSolution.setFlightItineraryList(flightItineraries);
        return airSolution;
    }

    public static SimpleDateFormat searchFormat = new SimpleDateFormat("ddMMyy-kkmm");

    private AirSegmentInformation createSegment(TravelProductType flightInformation) {

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
        Minutes diff = Minutes.minutesBetween(departureDate, arrivalDate);

        airSegmentInformation.setTravelTime(""+diff.getMinutes());
        airSegmentInformation.setAirline(AirlineCode.getAirlineByCode(flightInformation.getCompanyId().getMarketingCarrier()));


        return airSegmentInformation;
    }

    private SearchResponse addSeamenFareToSolution(AirSolution allSolution, AirSolution seamenSolution) {
        System.out.println();
        System.out.println("==============================================================================");
        System.out.println("All Solution Length:::::" + allSolution.getFlightItineraryList().size());
        System.out.println("Seamen Solution Length:::::" + seamenSolution.getFlightItineraryList().size());
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
}
