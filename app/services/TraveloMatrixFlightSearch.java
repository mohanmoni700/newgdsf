package services;

import com.compassites.GDSWrapper.travelomatrix.SearchFlights;
import com.compassites.constants.TraveloMatrixConstants;
import com.compassites.exceptions.RetryException;
import com.compassites.model.*;
import com.compassites.model.travelomatrix.ResponseModels.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import models.Airline;
import models.Airport;
import models.FlightSearchOffice;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import play.libs.Json;
import scala.reflect.runtime.SymbolLoaders;
import utils.DateUtility;
import utils.ErrorMessageHelper;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TraveloMatrixFlightSearch implements FlightSearch {

    static Logger logger = LoggerFactory.getLogger("gds");

    static Logger travelomatrixLogger = LoggerFactory.getLogger("travelomatrix");


    public SearchFlights searchFlights = new SearchFlights();

    @Autowired
    private RedisTemplate redisTemplate;

    public RedisTemplate getRedisTemplate() {
        return redisTemplate;
    }

    public void setRedisTemplate(RedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Boolean isRefundable;

    public Boolean nonStop;


    @RetryOnFailure(attempts = 2, delay =30000, exception = RetryException.class)
    public SearchResponse search(SearchParameters searchParameters, FlightSearchOffice office) throws Exception {
        LocalDateTime start = LocalDateTime.now();
        logger.debug("Performance logs : TravelomatrixFlightSearch: Search: search: start: Current Date and Time: " + start);

        SearchResponse sr = null;
        if(!searchParameters.getJourneyType().equals(JourneyType.ROUND_TRIP)) {
            isRefundable = searchParameters.getRefundableFlights();
            nonStop = searchParameters.getDirectFlights();
            travelomatrixLogger.debug("#####################TraveloMatrixFlightSearch started  : ");
            travelomatrixLogger.debug("#####################TraveloMatrixFlightSearch : SearchParameters: \n" + Json.toJson(searchParameters));
            JsonNode jsonResponse = searchFlights.getFlights(searchParameters);
            try {
                TravelomatrixSearchReply response = new ObjectMapper().treeToValue(jsonResponse, TravelomatrixSearchReply.class);
                if (!response.getStatus()) {
                    sr = new SearchResponse();
                    travelomatrixLogger.debug("Converted Travelomatrix Reply:" + response.getMessage().toString());
                    ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage("travelomatrix.2000", ErrorMessage.ErrorType.ERROR, "TraveloMatrix");
                    sr.getErrorMessageList().add(errorMessage);
                }
                if (response.getStatus()) {
                    travelomatrixLogger.debug("Converted Travelomatrix Reply:" + response.toString());
                    LocalDateTime getairsolution = LocalDateTime.now();
                    logger.debug("Performance logs : TravelomatrixFlightSearch: getairsolution: start: Current Date and Time: " + getairsolution);

                    AirSolution airSolution = getAirSolution(response);
                    LocalDateTime getairsolution1 = LocalDateTime.now();
                    logger.debug("Performance logs : TravelomatrixFlightSearch: getairsolution1: end: Current Date and Time: " + getairsolution1);

                    sr = new SearchResponse();
                    sr.setFlightSearchOffice(getOfficeList().get(0));
                    sr.setAirSolution(airSolution);
                    sr.setProvider(TraveloMatrixConstants.provider);
                } else {
                    ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage(response.getMessage(), ErrorMessage.ErrorType.ERROR, "TraveloMatrix");
                    sr.getErrorMessageList().add(errorMessage);
                }

            } catch (Exception e) {
                travelomatrixLogger.info("TimeOut during Travelomagrix flight search ");
                e.printStackTrace();
                ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage("Timed Out", ErrorMessage.ErrorType.ERROR, "TraveloMatrix");
                sr.getErrorMessageList().add(errorMessage);
            }

            travelomatrixLogger.debug("TraveloMatrix SearchResponse created:" + sr.toString());
            logger.debug("#####################TraveloMatrixFlightSearch Search is completed ##################" + sr.toString());
        }
        LocalDateTime end = LocalDateTime.now();
        logger.debug("Performance logs : TravelomatrixFlightSearch: Search: search: end: Current Date and Time: " + end);

        return sr;
    }

    @Override
    public String provider() {
        return TraveloMatrixConstants.provider;
    }

    @Override
    public List<FlightSearchOffice> getOfficeList() {
        FlightSearchOffice fs = new FlightSearchOffice();
        fs.setOfficeId(TraveloMatrixConstants.tmofficeId);
        List<FlightSearchOffice> lfs = new ArrayList<>();
        lfs.add(fs);
        return lfs;
    }

    public AirSolution getAirSolution(TravelomatrixSearchReply response) {
        AirSolution airSolution = new AirSolution();
        ConcurrentHashMap<Integer, FlightItinerary> nonSeamenHashMap = getFlightIternary(response.getSearch().getFlightDataList());
        airSolution.setNonSeamenHashMap(nonSeamenHashMap);
        return airSolution;
    }

    public  ConcurrentHashMap<Integer, FlightItinerary> getFlightIternary(FlightDataList flightDataList) {
        ConcurrentHashMap<Integer, FlightItinerary> flightItineraryHashMap = new ConcurrentHashMap<>();
        try {
           List<List<JourneyList>> journeyList = flightDataList.getJourneyList();
           for (List<JourneyList> journey : journeyList) {
               for (JourneyList journeyDetails : journey) {
                   FlightItinerary flightItinerary = new FlightItinerary();
                   List<Journey> consolidatedJourney = new LinkedList<>();
                   if (isRefundable && !journeyDetails.getAttr().getIsRefundable()) {
                       continue;
                   }
                   if (nonStop && journeyDetails.getFlightDetails().getDetails().get(0).size() > 1) {
                       continue;
                   }
                   consolidatedJourney = getJourneyList(journeyDetails.getFlightDetails());
                   flightItinerary.setJourneyList(consolidatedJourney);
                   flightItinerary.setNonSeamenJourneyList(consolidatedJourney);
                   flightItinerary.setPassportMandatory(Boolean.FALSE);
                   PricingInformation pricingInformation = getPricingInformation(journeyDetails);
                   flightItinerary.setPricingInformation(pricingInformation);
//                if(journeyDetails.getFlightDetails().getDetails().get(0).get(0).getFlightNumber().equalsIgnoreCase("809")){
//                    System.out.println("Flightnumber");
//                }
                   flightItinerary.setResultToken(journeyDetails.getResultToken());
                   if (journeyDetails.getAttr().getIsLCC() != null)
                       flightItinerary.setIsLCC(journeyDetails.getAttr().getIsLCC());
                   else
                       flightItinerary.setIsLCC(false);

                   flightItineraryHashMap.put(flightItinerary.hashCode(), flightItinerary);
               }
           }
       }catch(Exception e){
            logger.error("Error while creating the airsolution for travelomatrix"+ e.getMessage());
            e.printStackTrace();
        }
        return flightItineraryHashMap;
    }

    public PricingInformation getPricingInformation(JourneyList journeyDetails) {
        PricingInformation pricingInformation = new PricingInformation();
        pricingInformation.setBasePrice(new BigDecimal(journeyDetails.getPrice().getPriceBreakup().getBasicFare()));
        pricingInformation.setGdsCurrency(journeyDetails.getPrice().getCurrency());
        pricingInformation.setAdtBasePrice(new BigDecimal(journeyDetails.getPrice().getPassengerBreakup().getADT().getBasePrice()));
        if(journeyDetails.getPrice().getPassengerBreakup().getcHD() != null)
            pricingInformation.setChdBasePrice(new BigDecimal(journeyDetails.getPrice().getPassengerBreakup().getcHD().getBasePrice()));
        if(journeyDetails.getPrice().getPassengerBreakup().getiNF() != null)
            pricingInformation.setInfBasePrice(new BigDecimal(journeyDetails.getPrice().getPassengerBreakup().getiNF().getBasePrice()));
        pricingInformation.setAdtTotalPrice(new BigDecimal(journeyDetails.getPrice().getPassengerBreakup().getADT().getTotalPrice()));
        if(journeyDetails.getPrice().getPassengerBreakup().getcHD() != null)
            pricingInformation.setChdTotalPrice(new BigDecimal(journeyDetails.getPrice().getPassengerBreakup().getcHD().getTotalPrice()));
        if(journeyDetails.getPrice().getPassengerBreakup().getiNF() != null)
            pricingInformation.setInfTotalPrice(new BigDecimal(journeyDetails.getPrice().getPassengerBreakup().getiNF().getTotalPrice()));
        pricingInformation.setTotalTax(new BigDecimal(journeyDetails.getPrice().getPriceBreakup().getTax()));
        pricingInformation.setTax(new BigDecimal(journeyDetails.getPrice().getPriceBreakup().getTax()));
        pricingInformation.setTotalPrice(new BigDecimal(journeyDetails.getPrice().getTotalDisplayFare()));
        pricingInformation.setTotalPriceValue(new BigDecimal(journeyDetails.getPrice().getTotalDisplayFare()));
        pricingInformation.setTotalBasePrice(new BigDecimal(journeyDetails.getPrice().getPriceBreakup().getBasicFare()));
        pricingInformation.setTotalCalculatedValue(new BigDecimal(journeyDetails.getPrice().getTotalDisplayFare()));
        pricingInformation.setLCC(journeyDetails.getAttr().getIsLCC());
        pricingInformation.setPricingOfficeId(TraveloMatrixConstants.tmofficeId);
        List<PassengerTax> passengerTaxesList = new ArrayList<>();
        ADT adt = journeyDetails.getPrice().getPassengerBreakup().getADT();
        if(adt != null) {
            PassengerTax adutlPassengerTax = new PassengerTax();
            adutlPassengerTax.setPassengerCount(Integer.parseInt(adt.getPassengerCount()));
            adutlPassengerTax.setPassengerType("ADT");
            adutlPassengerTax.setTotalTax(new BigDecimal(adt.getTax()));
            passengerTaxesList.add(adutlPassengerTax);
        }
        CHD chd = journeyDetails.getPrice().getPassengerBreakup().getcHD();
        if(chd != null) {
            PassengerTax chdPassengerTax = new PassengerTax();
            chdPassengerTax.setPassengerCount(Integer.parseInt(chd.getPassengerCount()));
            chdPassengerTax.setPassengerType("CHD");
            chdPassengerTax.setTotalTax(new BigDecimal(chd.getTax()));
            passengerTaxesList.add(chdPassengerTax);
        }

        INF inf = journeyDetails.getPrice().getPassengerBreakup().getiNF();
        if(inf != null) {
            PassengerTax infPassengerTax = new PassengerTax();
            infPassengerTax.setPassengerCount(Integer.parseInt(inf.getPassengerCount()));
            infPassengerTax.setPassengerType("INF");
            infPassengerTax.setTotalTax(new BigDecimal(inf.getTax()));
            passengerTaxesList.add(infPassengerTax);
        }
        pricingInformation.setPassengerTaxes(passengerTaxesList);
        pricingInformation.setProvider(TraveloMatrixConstants.provider);
        return pricingInformation;
    }

    public List<Journey> getJourneyList(FlightDetails flightDetails) {
        List<AirSegmentInformation> airsegmentList = new ArrayList<>();
        //calculate duration
        Long durationTime = 0L;
        Long layOver = 0L;

        List<Journey> journeyList = new ArrayList<>();
        for(List<Detail> detailsList :flightDetails.getDetails()) {
            List<AirSegmentInformation> airSegmentInformationList = new ArrayList<>();
            for (Detail journeyData : detailsList) {
                AirSegmentInformation airSegmentInformation = new AirSegmentInformation();
                airSegmentInformation.setFromLocation(journeyData.getOrigin().getAirportCode());
                airSegmentInformation.setToLocation(journeyData.getDestination().getAirportCode());
                airSegmentInformation.setFromDate(journeyData.getOrigin().getDateTime());
                airSegmentInformation.setToDate(journeyData.getDestination().getDateTime());
                if(journeyData.getOrigin().getTerminal() != null)
                    airSegmentInformation.setFromTerminal(journeyData.getOrigin().getTerminal());
                if(journeyData.getDestination().getTerminal() != null)
                    airSegmentInformation.setToTerminal(journeyData.getOrigin().getTerminal());
                if (journeyData.getLayOverTime() != null) {
                    layOver += journeyData.getLayOverTime().intValue();
                    airSegmentInformation.setConnectionTime(journeyData.getLayOverTime().intValue());
                }
                if (journeyData.getDuration() != null) {
                    durationTime += journeyData.getDuration();
                    airSegmentInformation.setTravelTime(journeyData.getDuration().toString());
                }
                airSegmentInformation.setFlightNumber(journeyData.getFlightNumber());
                airSegmentInformation.setCarrierCode(journeyData.getOperatorCode());
                airSegmentInformation.setOperatingCarrierCode(journeyData.getOperatorCode());
                Airport fromAirport = new Airport();
                Airport toAirport = new Airport();
                fromAirport = Airport.getAirport(journeyData.getOrigin().getAirportCode(), redisTemplate);
                toAirport = Airport.getAirport(journeyData.getDestination().getAirportCode(), redisTemplate);
                airSegmentInformation.setFromAirport(fromAirport);
                airSegmentInformation.setToAirport(toAirport);
                DateTime departure = DateUtility.convertTimewithZone(fromAirport.getTime_zone(),journeyData.getOrigin().getDateTime());
                airSegmentInformation.setDepartureDate(departure.toDate());
                airSegmentInformation.setDepartureTime(departure.toString());
                DateTime arrival = DateUtility.convertTimewithZone(toAirport.getTime_zone(), journeyData.getDestination().getDateTime());
                airSegmentInformation.setArrivalTime(arrival.toString());
                airSegmentInformation.setArrivalDate(arrival.toDate());
                airSegmentInformation.setAirline(Airline.getAirlineByCode(journeyData.getOperatorCode(), redisTemplate));
                airSegmentInformation.setOperatingAirline(Airline.getAirlineByCode(journeyData.getOperatorCode(), redisTemplate));
                airSegmentInformation.setCabinClass(journeyData.getCabinClass());
                if(journeyData.getAttr() != null) {
                    airSegmentInformation.setFareBasis(journeyData.getAttr().getAirlineRemark());
                    airSegmentInformation.setCabinBaggage(journeyData.getAttr().getCabinBaggage());
                    airSegmentInformation.setBaggage(journeyData.getAttr().getBaggage());
                    airSegmentInformation.setAvailbleSeats(journeyData.getAttr().getAvailableSeats());
                }

                if(toAirport.getAirportName() != null && fromAirport.getAirportName() != null)
                 airSegmentInformationList.add(airSegmentInformation);
            }
            Journey asJourney = new Journey();
            asJourney.setProvider(TraveloMatrixConstants.provider);
            asJourney.setAirSegmentList(airSegmentInformationList);
            asJourney.setNoOfStops(airSegmentInformationList.size()-1);
            //Convert minutes to milliseconds
            Long totalTravelTime = durationTime*60000 + layOver*60000;
            asJourney.setTravelTimeMillis(totalTravelTime);
             asJourney.setTravelTime(DateUtility.convertMillistoString(totalTravelTime));
            journeyList.add(asJourney);
        }

        return journeyList;
    }

}
