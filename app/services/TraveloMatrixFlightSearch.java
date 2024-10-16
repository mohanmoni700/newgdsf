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
import play.Play;
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
        SearchResponse sr = null;
//        if(!searchParameters.getJourneyType().equals(JourneyType.ROUND_TRIP)) {
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
                    AirSolution airSolution = getAirSolution(response,searchParameters.getJourneyType().toString());
                    sr = new SearchResponse();
                    sr.setFlightSearchOffice(getOfficeList().get(0));
                    sr.setAirSolution(airSolution);
                    sr.setProvider(TraveloMatrixConstants.provider);
                } else {
                    ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage(response.getMessage(), ErrorMessage.ErrorType.ERROR, "TraveloMatrix");
                    sr.getErrorMessageList().add(errorMessage);
                }

            } catch (Exception e) {
                sr = new SearchResponse();
                travelomatrixLogger.info("TimeOut during Travelomagrix flight search ");
                e.printStackTrace();
                ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage("Timed Out", ErrorMessage.ErrorType.ERROR, "TraveloMatrix");
                sr.getErrorMessageList().add(errorMessage);
            }

            travelomatrixLogger.debug("TraveloMatrix SearchResponse created:" + sr.toString());
            logger.debug("#####################TraveloMatrixFlightSearch Search is completed ##################" + sr.toString());
  //      }
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

    public AirSolution getAirSolution(TravelomatrixSearchReply response,String journeyType) {
        AirSolution airSolution = new AirSolution();
        ConcurrentHashMap<Integer, FlightItinerary> nonSeamenHashMap = getFlightIternary(response.getSearch().getFlightDataList(),journeyType);
        airSolution.setNonSeamenHashMap(nonSeamenHashMap);
        return airSolution;
    }

    public  ConcurrentHashMap<Integer, FlightItinerary> getFlightIternary(FlightDataList flightDataList,String journyeType) {
        ConcurrentHashMap<Integer, FlightItinerary> flightItineraryHashMap = new ConcurrentHashMap<>();
        try {
            int maxResults = Play.application().configuration().getInt("travelomatrix.noOfSearchResults");
            List<List<JourneyList>> journeyList = flightDataList.getJourneyList();
            if (journeyList.size() > 1 && journyeType.equalsIgnoreCase("ROUND_TRIP")) {
                //indian Domestic Round trip
                List<JourneyList> onWardjourneyList = journeyList.get(0);
                List<JourneyList> returnJourneyList = journeyList.get(1);
                List<JourneyList> lcconWardjourneyList =new ArrayList<>();
                List<JourneyList> lccreturnJourneyList = new ArrayList<>();
                for(int index= 0; index < maxResults ; index++){
                    FlightItinerary flightItinerary = new FlightItinerary();
                    List<Journey> consolidatedJourney = new LinkedList<>();
                    Journey onWardJourney = new Journey();
                    Journey returnJourney = new Journey();
                    if(index < onWardjourneyList.size() && index < returnJourneyList.size() && !onWardjourneyList.get(index).getAttr().getIsLCC() && !returnJourneyList.get(index).getAttr().getIsLCC()) {
                        onWardJourney = getOnwardJounery(onWardjourneyList.get(index).getFlightDetails());
                        returnJourney = getReturnJounery(returnJourneyList.get(index).getFlightDetails());
                        consolidatedJourney.add(onWardJourney);
                        consolidatedJourney.add(returnJourney);
                        flightItinerary.setJourneyList(consolidatedJourney);
                        flightItinerary.setNonSeamenJourneyList(consolidatedJourney);
                        flightItinerary.setPassportMandatory(Boolean.FALSE);
                        flightItinerary.setLCC(false);
                        PricingInformation pricingInformation = getRoundtripPricingInformation(onWardjourneyList.get(index).getPrice(),
                                returnJourneyList.get(index).getPrice(), flightItinerary.getLCC());
                        flightItinerary.setPricingInformation(pricingInformation);
                        flightItinerary.setResultToken(onWardjourneyList.get(index).getResultToken());
                        flightItinerary.setReturnResultToken(returnJourneyList.get(index).getResultToken());
                        flightItineraryHashMap.put(flightItinerary.hashCode(), flightItinerary);
                    }
                    else{
                        //lcc carriers
                        if(index < onWardjourneyList.size() &&  onWardjourneyList.get(index).getAttr().getIsLCC()){
                            lcconWardjourneyList.add(onWardjourneyList.get(index));
                        }
                        if(index < returnJourneyList.size() && returnJourneyList.get(index).getAttr().getIsLCC()){
                             lccreturnJourneyList.add(returnJourneyList.get(index)) ;
                        }
                        if (lcconWardjourneyList.size() > 0 && lccreturnJourneyList.size() > 0) {
                        for(int lccindex= 0 ; lccindex < lcconWardjourneyList.size()  && lccindex < lccreturnJourneyList.size(); lccindex++) {
                                onWardJourney = getOnwardJounery(lcconWardjourneyList.get(lccindex).getFlightDetails());
                                returnJourney = getReturnJounery(lccreturnJourneyList.get(lccindex).getFlightDetails());
                                consolidatedJourney.add(onWardJourney);
                                consolidatedJourney.add(returnJourney);
                                flightItinerary.setJourneyList(consolidatedJourney);
                                flightItinerary.setNonSeamenJourneyList(consolidatedJourney);
                                flightItinerary.setPassportMandatory(Boolean.FALSE);
                                flightItinerary.setLCC(true);
                                PricingInformation pricingInformation = getRoundtripPricingInformation(lcconWardjourneyList.get(lccindex).getPrice(),
                                        lccreturnJourneyList.get(lccindex).getPrice(), flightItinerary.getLCC());
                                flightItinerary.setPricingInformation(pricingInformation);
                                flightItinerary.setResultToken(lcconWardjourneyList.get(lccindex).getResultToken());
                                flightItinerary.setReturnResultToken(lccreturnJourneyList.get(lccindex).getResultToken());
                                flightItineraryHashMap.put(flightItinerary.hashCode(), flightItinerary);
                            }
                            lcconWardjourneyList.clear();
                            lccreturnJourneyList.clear();
                        }
                    }
                }
            }else if(journyeType.equalsIgnoreCase("ONE_WAY") || journyeType.equalsIgnoreCase("MULTI_CITY")){
                int index = 0;
                for (List<JourneyList> journey : journeyList) {
                    for (JourneyList journeyDetails : journey) {
                        if((journeyDetails.getFlightDetails().getDetails().size() >1 &&
                                journyeType.equalsIgnoreCase("MULTI_CITY") )||
                                journyeType.equalsIgnoreCase("ONE_WAY")   )
                        {
                            if (index == maxResults) {
                                break;
                            } else {
                                index++;
                            }
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
                            if(journeyDetails.getAttr() != null) {
                                flightItinerary.setFareType(journeyDetails.getAttr().getFareType());
                                flightItinerary.setRefundable(journeyDetails.getAttr().getIsRefundable());
                            }
                            flightItinerary.setResultToken(journeyDetails.getResultToken());
                            if (journeyDetails.getAttr().getIsLCC() != null)
                                flightItinerary.setIsLCC(journeyDetails.getAttr().getIsLCC());
                            else
                                flightItinerary.setIsLCC(false);

                            flightItineraryHashMap.put(flightItinerary.hashCode()+index, flightItinerary);
                        }

                    }
                }
            }
        }catch(Exception e){
            logger.error("Error while creating the airsolution for travelomatrix" + e.getMessage());
            e.printStackTrace();
        }

        return flightItineraryHashMap;
    }

    public PricingInformation getPricingInformation(JourneyList journeyDetails) {
        PricingInformation pricingInformation = new PricingInformation();
        Long agentCommission = journeyDetails.getPrice().getPriceBreakup().getAgentCommission();
        Long agentTds = journeyDetails.getPrice().getPriceBreakup().getAgentTdsOnCommision();
        pricingInformation.setBasePrice(new BigDecimal(journeyDetails.getPrice().getPriceBreakup().getBasicFare() - agentCommission +agentTds));
        pricingInformation.setGdsCurrency(journeyDetails.getPrice().getCurrency());
        pricingInformation.setAdtBasePrice(new BigDecimal(journeyDetails.getPrice().getPassengerBreakup().getADT().getBasePrice()-agentCommission+agentTds));
        if(journeyDetails.getPrice().getPassengerBreakup().getcHD() != null)
            pricingInformation.setChdBasePrice(new BigDecimal(journeyDetails.getPrice().getPassengerBreakup().getcHD().getBasePrice()-agentCommission+agentTds));
        if(journeyDetails.getPrice().getPassengerBreakup().getiNF() != null)
            pricingInformation.setInfBasePrice(new BigDecimal(journeyDetails.getPrice().getPassengerBreakup().getiNF().getBasePrice()-agentCommission+agentTds));
        //pricingInformation.setAdtTotalPrice(new BigDecimal(journeyDetails.getPrice().getPassengerBreakup().getADT().getTotalPrice()));
        if(journeyDetails.getPrice().getPassengerBreakup().getcHD() != null)
            pricingInformation.setChdTotalPrice(new BigDecimal(journeyDetails.getPrice().getPassengerBreakup().getcHD().getTotalPrice()-agentCommission+agentTds));
        if(journeyDetails.getPrice().getPassengerBreakup().getiNF() != null)
            pricingInformation.setInfTotalPrice(new BigDecimal(journeyDetails.getPrice().getPassengerBreakup().getiNF().getTotalPrice()-agentCommission+agentTds));
        pricingInformation.setTotalTax(new BigDecimal(journeyDetails.getPrice().getPriceBreakup().getTax()));
        pricingInformation.setTax(new BigDecimal(journeyDetails.getPrice().getPriceBreakup().getTax()));
        pricingInformation.setTotalBasePrice(new BigDecimal(journeyDetails.getPrice().getPriceBreakup().getBasicFare()-agentCommission+agentTds));

        BigDecimal totalFare = getTotalFare(journeyDetails.getPrice());
//        pricingInformation.setTotalPrice(new BigDecimal(journeyDetails.getPrice().getTotalDisplayFare()));
//        pricingInformation.setTotalPriceValue(new BigDecimal(journeyDetails.getPrice().getTotalDisplayFare()));
//        pricingInformation.setTotalCalculatedValue(new BigDecimal(journeyDetails.getPrice().getTotalDisplayFare()));

        pricingInformation.setTotalPrice(totalFare);
        pricingInformation.setTotalPriceValue(totalFare);
        pricingInformation.setTotalCalculatedValue(totalFare);
        pricingInformation.setPriceChanged(Boolean.FALSE);
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

    public PricingInformation getRoundtripPricingInformation(Price onwardPrice,Price returnPrice,boolean islcc){
        PricingInformation pricingInformation = new PricingInformation();
        Long basicFare = onwardPrice.getPriceBreakup().getBasicFare() + returnPrice.getPriceBreakup().getBasicFare();
        pricingInformation.setBasePrice(new BigDecimal(basicFare));
        pricingInformation.setGdsCurrency(onwardPrice.getCurrency());
        pricingInformation.setOnwardTotalBasePrice(new BigDecimal(onwardPrice.getTotalDisplayFare()));
        pricingInformation.setReturnTotalBasePrice(new BigDecimal(returnPrice.getTotalDisplayFare()));
        Long adtBasicFare = onwardPrice.getPassengerBreakup().getADT().getBasePrice() + returnPrice.getPassengerBreakup().getADT().getBasePrice();
        Long adtTotalPrice = onwardPrice.getPassengerBreakup().getADT().getTotalPrice() + returnPrice.getPassengerBreakup().getADT().getTotalPrice();
        pricingInformation.setAdtBasePrice(new BigDecimal(adtBasicFare));
        pricingInformation.setAdtTotalPrice(new BigDecimal(adtTotalPrice));
        if(onwardPrice.getPassengerBreakup().getcHD() != null && returnPrice.getPassengerBreakup().getcHD() != null) {
            Long chdBasePrice = onwardPrice.getPassengerBreakup().getcHD().getBasePrice() + returnPrice.getPassengerBreakup().getcHD().getBasePrice();
            Long chdTotalPrice = onwardPrice.getPassengerBreakup().getcHD().getTotalPrice() + returnPrice.getPassengerBreakup().getcHD().getTotalPrice();
            pricingInformation.setChdBasePrice(new BigDecimal(chdBasePrice));
            pricingInformation.setChdTotalPrice(new BigDecimal(chdTotalPrice));
        }
        if(onwardPrice.getPassengerBreakup().getiNF() != null && returnPrice.getPassengerBreakup().getiNF() != null) {
            Long infBasePrice = onwardPrice.getPassengerBreakup().getiNF().getBasePrice() + returnPrice.getPassengerBreakup().getiNF().getBasePrice();
            Long infoTotalPrice = onwardPrice.getPassengerBreakup().getiNF().getTotalPrice() + returnPrice.getPassengerBreakup().getiNF().getTotalPrice();
            pricingInformation.setInfBasePrice(new BigDecimal(infBasePrice));
            pricingInformation.setInfTotalPrice(new BigDecimal(infoTotalPrice));
        }

        pricingInformation.setTotalTax(new BigDecimal(onwardPrice.getPriceBreakup().getTax() + returnPrice.getPriceBreakup().getTax()));
        pricingInformation.setTax(new BigDecimal(onwardPrice.getPriceBreakup().getTax() + returnPrice.getPriceBreakup().getTax()));
        pricingInformation.setTotalPrice(getTotalFare(onwardPrice).add(getTotalFare(returnPrice)).add(new BigDecimal(onwardPrice.getPriceBreakup().getTax())));
        pricingInformation.setTotalPriceValue(getTotalFare(onwardPrice).add(getTotalFare(returnPrice)).add(new BigDecimal(onwardPrice.getPriceBreakup().getTax())));
        pricingInformation.setTotalCalculatedValue(getTotalFare(onwardPrice).add(getTotalFare(returnPrice)).add(new BigDecimal(onwardPrice.getPriceBreakup().getTax())));
        //pricingInformation.setTotalPrice(new BigDecimal(onwardPrice.getTotalDisplayFare()+returnPrice.getTotalDisplayFare()+onwardPrice.getPriceBreakup().getTax()));
        //pricingInformation.setTotalPriceValue(new BigDecimal(onwardPrice.getTotalDisplayFare()+returnPrice.getTotalDisplayFare()+onwardPrice.getPriceBreakup().getTax()));
        //pricingInformation.setTotalCalculatedValue(new BigDecimal(onwardPrice.getTotalDisplayFare()+returnPrice.getTotalDisplayFare()+onwardPrice.getPriceBreakup().getTax() ));
        pricingInformation.setTotalBasePrice(new BigDecimal(onwardPrice.getPriceBreakup().getBasicFare()+returnPrice.getPriceBreakup().getBasicFare()));
        pricingInformation.setLCC(islcc);
        pricingInformation.setPriceChanged(Boolean.FALSE);
        pricingInformation.setPricingOfficeId(TraveloMatrixConstants.tmofficeId);
        List<PassengerTax> passengerTaxesList = new ArrayList<>();
        ADT onwardAdt = onwardPrice.getPassengerBreakup().getADT();
        ADT returnAdt = onwardPrice.getPassengerBreakup().getADT();
        if(onwardAdt != null && returnAdt != null) {
            PassengerTax adutlPassengerTax = new PassengerTax();
            adutlPassengerTax.setPassengerCount(Integer.parseInt(onwardAdt.getPassengerCount()));
            adutlPassengerTax.setPassengerType("ADT");
            adutlPassengerTax.setTotalTax(new BigDecimal(onwardAdt.getTax()+returnAdt.getTax()));
            passengerTaxesList.add(adutlPassengerTax);
        }
        CHD chd = onwardPrice.getPassengerBreakup().getcHD();
        if(chd != null) {
            PassengerTax chdPassengerTax = new PassengerTax();
            chdPassengerTax.setPassengerCount(Integer.parseInt(chd.getPassengerCount()));
            chdPassengerTax.setPassengerType("CHD");
            chdPassengerTax.setTotalTax(new BigDecimal(chd.getTax()+returnPrice.getPassengerBreakup().getcHD().getTax()));
            passengerTaxesList.add(chdPassengerTax);
        }

        INF inf = onwardPrice.getPassengerBreakup().getiNF();
        if(inf != null) {
            PassengerTax infPassengerTax = new PassengerTax();
            infPassengerTax.setPassengerCount(Integer.parseInt(inf.getPassengerCount()));
            infPassengerTax.setPassengerType("INF");
            infPassengerTax.setTotalTax(new BigDecimal(inf.getTax()+returnPrice.getPassengerBreakup().getiNF().getTax()));
            passengerTaxesList.add(infPassengerTax);
        }
        pricingInformation.setPassengerTaxes(passengerTaxesList);
        pricingInformation.setProvider(TraveloMatrixConstants.provider);
        return pricingInformation;
    }

    public List<Journey> getJourneyList(FlightDetails flightDetails) {
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
                    String baggage = journeyData.getAttr().getBaggage();
                    String output = updateBaggeUnits(baggage);
                    airSegmentInformation.setBaggage(output);
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

    public Journey getOnwardJounery(FlightDetails flightDetails) {
        List<AirSegmentInformation> airsegmentList = new ArrayList<>();
        //calculate duration
        Long durationTime = 0L;
        Long layOver = 0L;

        Journey onwardJourney = new Journey();
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
                    String baggage = journeyData.getAttr().getBaggage();
                    String output = updateBaggeUnits(baggage);
                    airSegmentInformation.setBaggage(output);
                    airSegmentInformation.setAvailbleSeats(journeyData.getAttr().getAvailableSeats());
                }

                if(toAirport.getAirportName() != null && fromAirport.getAirportName() != null)
                    airSegmentInformationList.add(airSegmentInformation);
            }

            onwardJourney.setProvider(TraveloMatrixConstants.provider);
            onwardJourney.setAirSegmentList(airSegmentInformationList);
            onwardJourney.setNoOfStops(airSegmentInformationList.size()-1);
            //Convert minutes to milliseconds
            Long totalTravelTime = durationTime*60000 + layOver*60000;
            onwardJourney.setTravelTimeMillis(totalTravelTime);
            onwardJourney.setTravelTime(DateUtility.convertMillistoString(totalTravelTime));

        }

        return onwardJourney;
    }

    public Journey getReturnJounery(FlightDetails flightDetails) {
        List<AirSegmentInformation> airsegmentList = new ArrayList<>();
        //calculate duration
        Long durationTime = 0L;
        Long layOver = 0L;

        Journey returnJourney = new Journey();
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
                    String baggage = journeyData.getAttr().getBaggage();
                    String output = updateBaggeUnits(baggage);
                    airSegmentInformation.setBaggage(output);

                    airSegmentInformation.setAvailbleSeats(journeyData.getAttr().getAvailableSeats());
                }

                if(toAirport.getAirportName() != null && fromAirport.getAirportName() != null)
                    airSegmentInformationList.add(airSegmentInformation);
            }

            returnJourney.setProvider(TraveloMatrixConstants.provider);
            returnJourney.setAirSegmentList(airSegmentInformationList);
            returnJourney.setNoOfStops(airSegmentInformationList.size()-1);
            //Convert minutes to milliseconds
            Long totalTravelTime = durationTime*60000 + layOver*60000;
            returnJourney.setTravelTimeMillis(totalTravelTime);
            returnJourney.setTravelTime(DateUtility.convertMillistoString(totalTravelTime));

        }
        return returnJourney;
    }
    public String updateBaggeUnits(String baggage){

        String updatedBagunits = null;
        String pattern = "^KG\\d{3}$";
        if(baggage !=null && (baggage.contains("Kg") || baggage.contains("Kilograms") || baggage.contains("kg"))){
            updatedBagunits = baggage.replaceAll("(?i)\\b(kilograms|kg)\\b", "KG");
            if(updatedBagunits.contains("(")){
                int index =   updatedBagunits.indexOf('(');
                if(index != -1){
                    updatedBagunits =   updatedBagunits.substring(0,index).trim();
                }
            }
        }else if(baggage != null && baggage.contains("Piece")){
            updatedBagunits = baggage.replaceAll("^0+", "").replaceAll("\\s*Piece\\s*", " PC");
        }else if (baggage != null && baggage.matches(pattern)) {
            String number = baggage.replaceAll("[^0-9]", "");  // Extract numeric part
            updatedBagunits = Integer.parseInt(number) + " KG";  // Combine with "KG"
        }else{
            updatedBagunits = baggage;
        }
        return updatedBagunits;

    }

    public BigDecimal getTotalFare(Price price){
        Double totalprice = price.getTotalDisplayFare();
        Long agentCommission = price.getPriceBreakup().getAgentCommission();
        Long agentTdsonCommision = price.getPriceBreakup().getAgentTdsOnCommision();
        Double finalFare  = totalprice -agentCommission+agentTdsonCommision;
        BigDecimal totalValue = new BigDecimal(finalFare);
        return totalValue;
    }

}
