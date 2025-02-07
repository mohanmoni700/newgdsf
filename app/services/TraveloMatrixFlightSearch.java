package services;

import com.compassites.GDSWrapper.travelomatrix.SearchFlights;
import com.compassites.constants.TraveloMatrixConstants;
import com.compassites.exceptions.RetryException;
import com.compassites.model.*;
import com.compassites.model.travelomatrix.ResponseModels.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import models.Airline;
import models.Airport;
import models.FlightSearchOffice;
import org.joda.time.DateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import play.Play;
import play.libs.Json;

import utils.DateUtility;
import utils.ErrorMessageHelper;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
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

        //TODO: Remove this when trying to handle TMX MultiCity response for domestic routes
        if (searchParameters.isDomestic() && searchParameters.getJourneyType().equals(JourneyType.MULTI_CITY)) {
            sr = new SearchResponse();
            ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage("Disabled Multi City", ErrorMessage.ErrorType.WARNING, "TraveloMatrix");
            sr.getErrorMessageList().add(errorMessage);
            return sr;
        }


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
                AirSolution airSolution = getAirSolution(response,searchParameters.getJourneyType().toString(),searchParameters.getJourneyList(),searchParameters.getDateType(),searchParameters.getTransit());
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

    public AirSolution getAirSolution(TravelomatrixSearchReply response,String journeyType,List<SearchJourney> reqJourneyList, DateType dateType,String connectingAirport) {
        AirSolution airSolution = new AirSolution();
        ConcurrentHashMap<String, List<Integer>> groupingKeyMap = new ConcurrentHashMap<>();
        ConcurrentHashMap<Integer, FlightItinerary> nonSeamenHashMap = getFlightIternary(response.getSearch().getFlightDataList(),journeyType, reqJourneyList, dateType, groupingKeyMap,connectingAirport);
        airSolution.setNonSeamenHashMap(nonSeamenHashMap);
        airSolution.setGroupingKeyMap(groupingKeyMap);
        return airSolution;
    }

    public  ConcurrentHashMap<Integer, FlightItinerary> getFlightIternary(FlightDataList flightDataList,String journyeType,List<SearchJourney> reqJourneyList,DateType dateType, ConcurrentHashMap<String, List<Integer>> groupingKeyMap, String connectingAirport) {
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
                    if (isRefundable && !onWardjourneyList.get(index).getAttr().getIsRefundable()) {
                        continue;
                    }
                    if (isRefundable && !returnJourneyList.get(index).getAttr().getIsRefundable()) {
                        continue;
                    }

                    if(connectingAirport != null && connectingAirport !=""){
                        List<Detail> detailsList = onWardjourneyList.get(index).getFlightDetails().getDetails().get(0);
                        List<Detail> redetailsList = returnJourneyList.get(index).getFlightDetails().getDetails().get(0);
                        if(detailsList.size() >= 1 ){
                            boolean flag = detailsList.stream().anyMatch(data -> data.getDestination().getAirportCode().equalsIgnoreCase(connectingAirport));
                            if(!flag){
                                continue;
                            }
                        }
                        if(redetailsList.size() >= 1 ){
                            boolean flag = redetailsList.stream().anyMatch(data -> data.getDestination().getAirportCode().equalsIgnoreCase(connectingAirport));
                            if(!flag){
                                continue;
                            }
                        }
                    }
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
            }else {
                int index = 0;
                for (List<JourneyList> journey : journeyList) {
                    for (JourneyList journeyDetails : journey) {
                        if((!journeyDetails.getFlightDetails().getDetails().isEmpty() &&
                                ( journyeType.equalsIgnoreCase("MULTI_CITY")  ||
                                        journyeType.equalsIgnoreCase("ROUND_TRIP") ) ) ||
                                journyeType.equalsIgnoreCase("ONE_WAY")   )
                        {
                            if (index == maxResults) {
                                break;
                            } else {
                                index++;
                            }
                            FlightItinerary flightItinerary = new FlightItinerary();
                            int flightHash = flightItinerary.hashCode()+index;
                            List<Journey> consolidatedJourney = new LinkedList<>();
                            if (isRefundable && !journeyDetails.getAttr().getIsRefundable()) {
                                continue;
                            }
                            if (nonStop && journeyDetails.getFlightDetails().getDetails().get(0).size() > 1) {
                                continue;
                            }

                            if(connectingAirport != null && connectingAirport !=""){
                                List<Detail> detailsList = journeyDetails.getFlightDetails().getDetails().get(0);
                                if(detailsList.size() >= 1){
                                    boolean flag = detailsList.stream().anyMatch(data -> data.getDestination().getAirportCode().equalsIgnoreCase(connectingAirport));
                                    if(!flag){
                                        continue;
                                    }
                                }
                            }
                            consolidatedJourney = getJourneyList(journeyDetails.getFlightDetails(),flightHash, groupingKeyMap,journyeType,reqJourneyList,dateType);
                             logger.info("consolidatedJourney.."+consolidatedJourney.size());
                           if(!consolidatedJourney.isEmpty()) {
                               flightItinerary.setJourneyList(consolidatedJourney);
                               flightItinerary.setNonSeamenJourneyList(consolidatedJourney);
                               flightItinerary.setPassportMandatory(Boolean.FALSE);
                               PricingInformation pricingInformation = getPricingInformation(journeyDetails);
                               flightItinerary.setPricingInformation(pricingInformation);
                               if (journeyDetails.getAttr() != null) {
                                   flightItinerary.setFareType(journeyDetails.getAttr().getFareType());
                                   flightItinerary.setRefundable(journeyDetails.getAttr().getIsRefundable());
                               }
                               flightItinerary.setResultToken(journeyDetails.getResultToken());
                               if (journeyDetails.getAttr().getIsLCC() != null)
                                   flightItinerary.setIsLCC(journeyDetails.getAttr().getIsLCC());
                               else
                                   flightItinerary.setIsLCC(false);
                               flightItineraryHashMap.put(flightHash, flightItinerary);
                           }
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
        int adultCount = Integer.parseInt(journeyDetails.getPrice().getPassengerBreakup().getADT().getPassengerCount());
        int chdCount = (journeyDetails.getPrice().getPassengerBreakup().getcHD() != null)  ?
                Integer.parseInt(journeyDetails.getPrice().getPassengerBreakup().getcHD().getPassengerCount()) : 0;
        int infCount = (journeyDetails.getPrice().getPassengerBreakup().getiNF() != null ) ?
                Integer.parseInt(journeyDetails.getPrice().getPassengerBreakup().getiNF().getPassengerCount()) : 0;
        pricingInformation.setBasePrice(new BigDecimal(journeyDetails.getPrice().getPriceBreakup().getBasicFare() - agentCommission +agentTds));
        pricingInformation.setGdsCurrency(journeyDetails.getPrice().getCurrency());
        pricingInformation.setAdtBasePrice(new BigDecimal((journeyDetails.getPrice().getPassengerBreakup().getADT().getBasePrice()-agentCommission+agentTds)/adultCount));
        if(journeyDetails.getPrice().getPassengerBreakup().getcHD() != null)
            pricingInformation.setChdBasePrice(new BigDecimal((journeyDetails.getPrice().getPassengerBreakup().getcHD().getBasePrice()-agentCommission+agentTds)/chdCount));
        if(journeyDetails.getPrice().getPassengerBreakup().getiNF() != null)
            pricingInformation.setInfBasePrice(new BigDecimal((journeyDetails.getPrice().getPassengerBreakup().getiNF().getBasePrice()-agentCommission+agentTds)/infCount));
        //pricingInformation.setAdtTotalPrice(new BigDecimal(journeyDetails.getPrice().getPassengerBreakup().getADT().getTotalPrice()));
        if(journeyDetails.getPrice().getPassengerBreakup().getcHD() != null)
            pricingInformation.setChdTotalPrice(new BigDecimal(journeyDetails.getPrice().getPassengerBreakup().getcHD().getTotalPrice()-agentCommission+agentTds));
        if(journeyDetails.getPrice().getPassengerBreakup().getiNF() != null)
            pricingInformation.setInfTotalPrice(new BigDecimal(journeyDetails.getPrice().getPassengerBreakup().getiNF().getTotalPrice()-agentCommission+agentTds));
        pricingInformation.setTotalTax(new BigDecimal(journeyDetails.getPrice().getPriceBreakup().getTax()));
        pricingInformation.setTax(new BigDecimal(journeyDetails.getPrice().getPriceBreakup().getTax()));
        pricingInformation.setTotalBasePrice(new BigDecimal(journeyDetails.getPrice().getPriceBreakup().getBasicFare()-agentCommission+agentTds));

        BigDecimal totalFare = getTotalFare(journeyDetails.getPrice());
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
            adutlPassengerTax.setTotalTax(new BigDecimal(adt.getTax()/adultCount));
            passengerTaxesList.add(adutlPassengerTax);
        }
        CHD chd = journeyDetails.getPrice().getPassengerBreakup().getcHD();
        if(chd != null) {
            PassengerTax chdPassengerTax = new PassengerTax();
            chdPassengerTax.setPassengerCount(Integer.parseInt(chd.getPassengerCount()));
            chdPassengerTax.setPassengerType("CHD");
            chdPassengerTax.setTotalTax(new BigDecimal(chd.getTax()/chdCount));
            passengerTaxesList.add(chdPassengerTax);
        }

        INF inf = journeyDetails.getPrice().getPassengerBreakup().getiNF();
        if(inf != null) {
            PassengerTax infPassengerTax = new PassengerTax();
            infPassengerTax.setPassengerCount(Integer.parseInt(inf.getPassengerCount()));
            infPassengerTax.setPassengerType("INF");
            infPassengerTax.setTotalTax(new BigDecimal(inf.getTax()/infCount));
            passengerTaxesList.add(infPassengerTax);
        }
        pricingInformation.setPassengerTaxes(passengerTaxesList);
        pricingInformation.setProvider(TraveloMatrixConstants.provider);
        return pricingInformation;
    }

    public PricingInformation getRoundtripPricingInformation(Price onwardPrice,Price returnPrice,boolean islcc){
        PricingInformation pricingInformation = new PricingInformation();
        Long onwardAgentCommission = onwardPrice.getPriceBreakup().getAgentCommission();
        Long onwardAgentTds = onwardPrice.getPriceBreakup().getAgentTdsOnCommision();
        Long returnAgentCommission = returnPrice.getPriceBreakup().getAgentCommission();
        Long returnAgentTds = returnPrice.getPriceBreakup().getAgentTdsOnCommision();
        int adultCount = Integer.parseInt(onwardPrice.getPassengerBreakup().getADT().getPassengerCount());
        int chdCount = (onwardPrice.getPassengerBreakup().getcHD() != null) ? Integer.parseInt(onwardPrice.getPassengerBreakup().getcHD().getPassengerCount()) : 0;
        int infCount = (onwardPrice.getPassengerBreakup().getiNF() != null) ? Integer.parseInt(onwardPrice.getPassengerBreakup().getiNF().getPassengerCount()) : 0;
        Long basicFare = (onwardPrice.getPriceBreakup().getBasicFare()-onwardAgentCommission+onwardAgentTds) +
                (returnPrice.getPriceBreakup().getBasicFare() -returnAgentCommission+returnAgentTds);
        pricingInformation.setBasePrice(new BigDecimal(basicFare));
        pricingInformation.setGdsCurrency(onwardPrice.getCurrency());
        pricingInformation.setOnwardTotalBasePrice(new BigDecimal(onwardPrice.getPriceBreakup().getBasicFare()-onwardAgentCommission+onwardAgentTds));
        pricingInformation.setReturnTotalBasePrice(new BigDecimal(returnPrice.getPriceBreakup().getBasicFare() -returnAgentCommission+returnAgentTds));
        Long adtBasicFare = ((onwardPrice.getPassengerBreakup().getADT().getBasePrice()  -onwardAgentCommission+onwardAgentTds)+
                (returnPrice.getPassengerBreakup().getADT().getBasePrice()- returnAgentCommission+returnAgentTds))/adultCount;
        Long adtTotalPrice = (onwardPrice.getPassengerBreakup().getADT().getTotalPrice()-onwardAgentCommission+onwardAgentTds) +
                (returnPrice.getPassengerBreakup().getADT().getTotalPrice() -returnAgentCommission+returnAgentTds);

        pricingInformation.setAdtOnwardBasePrice(new BigDecimal(onwardPrice.getPassengerBreakup().getADT().getBasePrice()  -onwardAgentCommission+onwardAgentTds).divide(new BigDecimal(adultCount)));
        pricingInformation.setAdtReturnBasePrice(new BigDecimal(returnPrice.getPassengerBreakup().getADT().getBasePrice() -returnAgentCommission+returnAgentTds).divide(new BigDecimal(adultCount)));

        pricingInformation.setAdtBasePrice(new BigDecimal(adtBasicFare));
        pricingInformation.setAdtTotalPrice(new BigDecimal(adtTotalPrice));
        if(onwardPrice.getPassengerBreakup().getcHD() != null && returnPrice.getPassengerBreakup().getcHD() != null) {
            Long chdBasePrice = ((onwardPrice.getPassengerBreakup().getcHD().getBasePrice() -onwardAgentCommission+onwardAgentTds) +
                    (returnPrice.getPassengerBreakup().getcHD().getBasePrice() - returnAgentCommission+returnAgentTds))/chdCount;

            pricingInformation.setChdOnwardBasePrice(new BigDecimal(onwardPrice.getPassengerBreakup().getcHD().getBasePrice()  -onwardAgentCommission+onwardAgentTds).divide(new BigDecimal(chdCount)));
            pricingInformation.setChdReturnBasePrice(new BigDecimal(returnPrice.getPassengerBreakup().getcHD().getBasePrice() -returnAgentCommission+returnAgentTds).divide(new BigDecimal(chdCount)));

            Long chdTotalPrice = (onwardPrice.getPassengerBreakup().getcHD().getTotalPrice() -onwardAgentCommission+onwardAgentTds) +
                    (returnPrice.getPassengerBreakup().getcHD().getTotalPrice()- returnAgentCommission+returnAgentTds);
            pricingInformation.setChdBasePrice(new BigDecimal(chdBasePrice));
            pricingInformation.setChdTotalPrice(new BigDecimal(chdTotalPrice));
        }
        if(onwardPrice.getPassengerBreakup().getiNF() != null && returnPrice.getPassengerBreakup().getiNF() != null) {
            Long infBasePrice = ((onwardPrice.getPassengerBreakup().getiNF().getBasePrice() -onwardAgentCommission+onwardAgentTds) +
                    (returnPrice.getPassengerBreakup().getiNF().getBasePrice() - returnAgentCommission+returnAgentTds))/infCount;

            pricingInformation.setInfOnwardBasePrice(new BigDecimal(onwardPrice.getPassengerBreakup().getiNF().getBasePrice()  -onwardAgentCommission+onwardAgentTds).divide(new BigDecimal(infCount)));
            pricingInformation.setInfReturnBasePrice(new BigDecimal(returnPrice.getPassengerBreakup().getiNF().getBasePrice() -returnAgentCommission+returnAgentTds).divide(new BigDecimal(infCount)));

            Long infoTotalPrice = (onwardPrice.getPassengerBreakup().getiNF().getTotalPrice() -onwardAgentCommission+onwardAgentTds)
                    + (returnPrice.getPassengerBreakup().getiNF().getTotalPrice() - returnAgentCommission+returnAgentTds);
            pricingInformation.setInfBasePrice(new BigDecimal(infBasePrice));
            pricingInformation.setInfTotalPrice(new BigDecimal(infoTotalPrice));
        }

        pricingInformation.setTotalTax(new BigDecimal(onwardPrice.getPriceBreakup().getTax() + returnPrice.getPriceBreakup().getTax()));
        pricingInformation.setTax(new BigDecimal(onwardPrice.getPriceBreakup().getTax() + returnPrice.getPriceBreakup().getTax()));
        BigDecimal totalFare = getTotalFare(onwardPrice).add(getTotalFare(returnPrice));
        pricingInformation.setTotalPrice(totalFare);
        pricingInformation.setTotalPriceValue(totalFare);
        pricingInformation.setTotalCalculatedValue(totalFare);
        pricingInformation.setTotalBasePrice(new BigDecimal(
                (onwardPrice.getPriceBreakup().getBasicFare() -onwardAgentCommission+onwardAgentTds )+
                        (returnPrice.getPriceBreakup().getBasicFare()- returnAgentCommission+returnAgentTds)));
        pricingInformation.setLCC(islcc);
        pricingInformation.setPriceChanged(Boolean.FALSE);
        pricingInformation.setPricingOfficeId(TraveloMatrixConstants.tmofficeId);
        List<PassengerTax> passengerTaxesList = new ArrayList<>();
        ADT onwardAdt = onwardPrice.getPassengerBreakup().getADT();
        ADT returnAdt = returnPrice.getPassengerBreakup().getADT();
        if(onwardAdt != null && returnAdt != null) {
            PassengerTax adutlPassengerTax = new PassengerTax();
            adutlPassengerTax.setPassengerCount(Integer.parseInt(onwardAdt.getPassengerCount()));
            adutlPassengerTax.setPassengerType("ADT");
            adutlPassengerTax.setTotalTax(new BigDecimal((onwardAdt.getTax()+returnAdt.getTax())/adultCount));
            adutlPassengerTax.setOnwardTax(new BigDecimal(onwardAdt.getTax()/adultCount));
            adutlPassengerTax.setReturnTax(new BigDecimal(returnAdt.getTax()/adultCount));
            passengerTaxesList.add(adutlPassengerTax);
        }
        CHD chd = onwardPrice.getPassengerBreakup().getcHD();
        if(chd != null) {
            PassengerTax chdPassengerTax = new PassengerTax();
            chdPassengerTax.setPassengerCount(Integer.parseInt(chd.getPassengerCount()));
            chdPassengerTax.setPassengerType("CHD");
            chdPassengerTax.setTotalTax(new BigDecimal((chd.getTax()+returnPrice.getPassengerBreakup().getcHD().getTax())/chdCount));
            chdPassengerTax.setOnwardTax(new BigDecimal(chd.getTax()/chdCount));
            chdPassengerTax.setReturnTax(new BigDecimal(returnPrice.getPassengerBreakup().getcHD().getTax()/chdCount));
            passengerTaxesList.add(chdPassengerTax);
        }

        INF inf = onwardPrice.getPassengerBreakup().getiNF();
        if(inf != null) {
            PassengerTax infPassengerTax = new PassengerTax();
            infPassengerTax.setPassengerCount(Integer.parseInt(inf.getPassengerCount()));
            infPassengerTax.setPassengerType("INF");
            infPassengerTax.setTotalTax(new BigDecimal((inf.getTax()+returnPrice.getPassengerBreakup().getiNF().getTax())/infCount));
            infPassengerTax.setOnwardTax(new BigDecimal(inf.getTax()/infCount));
            infPassengerTax.setReturnTax(new BigDecimal(returnPrice.getPassengerBreakup().getiNF().getTax()/infCount));
            passengerTaxesList.add(infPassengerTax);
        }
        pricingInformation.setPassengerTaxes(passengerTaxesList);
        pricingInformation.setProvider(TraveloMatrixConstants.provider);
        return pricingInformation;
    }


    public List<Journey> getJourneyList(FlightDetails flightDetails, int flightHash, ConcurrentHashMap<String, List<Integer>> concurrentHashMap, String journyeType,List<SearchJourney> reqJourneyList,DateType dateType) {
        List<Journey> journeyList = new ArrayList<>();
        Boolean arrivalFlag = false;
        for(List<Detail> detailsList :flightDetails.getDetails()) {
            Long durationTime = 0L;
            Long layOver = 0L;
            List<AirSegmentInformation> airSegmentInformationList = new ArrayList<>();
            StringBuilder groupingKey = new StringBuilder();

            for (Detail journeyData : detailsList) {
                // Extract requested arrival date
                String dateString = reqJourneyList.get(0).getTravelDateStr();
                // Define the format that matches the input string
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                // Parse the string to LocalDateTime
                LocalDateTime dateTime = LocalDateTime.parse(dateString, formatter);
                // Extract the date part
                String reqDate = dateTime.toLocalDate().toString();

                // Extract search response arrival date
                Airport toAirport = Airport.getAirport(journeyData.getDestination().getAirportCode(), redisTemplate);
                DateTime arrival = DateUtility.convertTimewithZone(toAirport.getTime_zone(), journeyData.getDestination().getDateTime());
                String arrivalDate = arrival.toDate().toString();
                // Get the date part only (no time)
                String date = arrival.toLocalDate().toString();  // Convert to LocalDate and then to String

                int result = reqDate.compareTo(date);
               if (result != 0 && dateType == DateType.ARRIVAL) {
                    arrivalFlag = true;
                }
                if ((result == 0 && dateType == DateType.ARRIVAL) || (dateType == DateType.DEPARTURE)) {
                   AirSegmentInformation airSegmentInformation = new AirSegmentInformation();
                    airSegmentInformation.setFromLocation(journeyData.getOrigin().getAirportCode());
                    airSegmentInformation.setToLocation(journeyData.getDestination().getAirportCode());
                    airSegmentInformation.setFromDate(journeyData.getOrigin().getDateTime());
                    airSegmentInformation.setToDate(journeyData.getDestination().getDateTime());
                    airSegmentInformation.setFdtv(journeyData.getOrigin().getFDTV());
                    airSegmentInformation.setFatv(journeyData.getDestination().getfATV());
                    if (journeyData.getOrigin().getTerminal() != null)
                        airSegmentInformation.setFromTerminal(journeyData.getOrigin().getTerminal());
                    if (journeyData.getDestination().getTerminal() != null)
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
                    Airport fromAirport = Airport.getAirport(journeyData.getOrigin().getAirportCode(), redisTemplate);
                    airSegmentInformation.setFromAirport(fromAirport);
                    airSegmentInformation.setToAirport(toAirport);
                    DateTime departure = DateUtility.convertTimewithZone(fromAirport.getTime_zone(), journeyData.getOrigin().getDateTime());
                    airSegmentInformation.setDepartureDate(departure.toDate());
                    airSegmentInformation.setDepartureTime(departure.toString());
                    airSegmentInformation.setArrivalTime(arrival.toString());
                    airSegmentInformation.setArrivalDate(arrival.toDate());
                    airSegmentInformation.setAirline(Airline.getAirlineByCode(journeyData.getOperatorCode(), redisTemplate));
                    airSegmentInformation.setOperatingAirline(Airline.getAirlineByCode(journeyData.getOperatorCode(), redisTemplate));
                    airSegmentInformation.setCabinClass(journeyData.getCabinClass());
                    if (journeyData.getAttr() != null) {
                        airSegmentInformation.setFareBasis(journeyData.getAttr().getAirlineRemark());
                        airSegmentInformation.setCabinBaggage(journeyData.getAttr().getCabinBaggage());
                        String baggage = journeyData.getAttr().getBaggage();
                        String output = updateBaggeUnits(baggage);
                        airSegmentInformation.setBaggage(output);
                        airSegmentInformation.setAvailbleSeats(journeyData.getAttr().getAvailableSeats());
                    }
                    groupingKey.append(airSegmentInformation.getFromLocation());
                    groupingKey.append(airSegmentInformation.getToLocation());
                    groupingKey.append(airSegmentInformation.getFlightNumber());
                    groupingKey.append(airSegmentInformation.getCarrierCode());
                    groupingKey.append(airSegmentInformation.getDepartureDate());
                    if (toAirport.getAirportName() != null && fromAirport.getAirportName() != null)
                        airSegmentInformationList.add(airSegmentInformation);
                }
            }
            Journey asJourney = new Journey();
            if (!arrivalFlag){
                asJourney.setProvider(TraveloMatrixConstants.provider);
            asJourney.setAirSegmentList(airSegmentInformationList);
            asJourney.setNoOfStops(airSegmentInformationList.size() - 1);
            if (journyeType.equalsIgnoreCase(JourneyType.ONE_WAY.name())) {
                asJourney.setGroupingKey(groupingKey.toString());
            }
            //Convert minutes to milliseconds
            if (layOver == 0 && airSegmentInformationList.size() > 1) {
                for (int index = airSegmentInformationList.size() - 1; index > 0; index--) {
                    String timestamp1 = airSegmentInformationList.get(index).getFromDate();
                    String timestamp2 = airSegmentInformationList.get(index - 1).getToDate();
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    LocalDateTime dateTime1 = LocalDateTime.parse(timestamp1, formatter);
                    LocalDateTime dateTime2 = LocalDateTime.parse(timestamp2, formatter);

                    Duration duration = Duration.between(dateTime2, dateTime1);

                    Long minutes = duration.toMinutes();
                    layOver += minutes;
                    airSegmentInformationList.get(index - 1).setConnectionTime(minutes.intValue());
                }

            }
            Long totalTravelTime = durationTime * 60000 + layOver * 60000;

            asJourney.setTravelTimeMillis(totalTravelTime);
            asJourney.setTravelTime(DateUtility.convertMillistoString(totalTravelTime));
            if (concurrentHashMap.containsKey(groupingKey.toString())) {
                List<Integer> mapList = concurrentHashMap.get(groupingKey.toString());
                mapList.add(flightHash);
                concurrentHashMap.put(groupingKey.toString(), mapList);
            } else {
                List<Integer> hashList = new ArrayList<>();
                hashList.add(flightHash);
                concurrentHashMap.put(groupingKey.toString(), hashList);
            }
            journeyList.add(asJourney);
            }
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