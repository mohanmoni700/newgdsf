package services;

import com.amadeus.xml.fmptbr_14_2_1a.*;
import com.compassites.GDSWrapper.amadeus.ServiceHandler;
import com.compassites.exceptions.IncompleteDetailsMessage;
import com.compassites.exceptions.RetryException;
import com.compassites.model.*;
import com.sun.xml.ws.client.ClientTransportException;
import com.sun.xml.ws.fault.ServerSOAPFaultException;
import com.thoughtworks.xstream.XStream;
import ennum.ConfigMasterConstants;
import models.Airline;
import models.Airport;
import models.AmadeusSessionWrapper;
import models.FlightSearchOffice;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Minutes;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import play.libs.Json;
import utils.AmadeusSessionManager;
import utils.ErrorMessageHelper;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SplitTicketAmadeusSearch implements SplitTicketSearch{

    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private ConfigurationMasterService configurationMasterService;
    static org.slf4j.Logger amadeusLogger = LoggerFactory.getLogger("amadeus");

    @Autowired
    private ServiceHandler serviceHandler;

    @Autowired
    private AmadeusSessionManager amadeusSessionManager;

    @Autowired
    private AmadeusSourceOfficeService sourceOfficeService;

    public RedisTemplate getRedisTemplate() {
        return redisTemplate;
    }

    public void setRedisTemplate(RedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    //private String searchOfficeID = play.Play.application().configuration().getString("split.ticket.officeId");
    private static String searchOfficeID = "";
    static org.slf4j.Logger logger = LoggerFactory.getLogger("gds");
    @Override
    public List<SearchResponse> splitSearch(List<SearchParameters> searchParameters, ConcurrentHashMap<String, List<FlightItinerary>> concurrentHashMap, boolean isDomestic) throws Exception {
        List<SearchResponse> responses = new ArrayList<>();
        searchOfficeID = configurationMasterService.getConfig(ConfigMasterConstants.SPLIT_TICKET_AMADEUS_OFFICE_ID_GLOBAL.getKey());
        for (SearchParameters searchParameters1: searchParameters)  {
            FlightSearchOffice searchOffice = new FlightSearchOffice();
            searchOffice.setOfficeId(searchOfficeID);
            searchOffice.setName("");
            SearchResponse searchResponse = null;
            if (isDomestic) {
                searchResponse = this.findNextSegmentDeparture(searchParameters1, searchOffice);
            } else {
                searchResponse = this.search(searchParameters1, searchOffice);
            }
            if (concurrentHashMap.containsKey(searchParameters1.getJourneyList().get(0).getOrigin())) {
                concurrentHashMap.get(searchParameters1.getJourneyList().get(0).getOrigin()).addAll(new ArrayList<FlightItinerary>(searchResponse.getAirSolution().getSeamenHashMap().values()));
                concurrentHashMap.get(searchParameters1.getJourneyList().get(0).getOrigin()).addAll(new ArrayList<FlightItinerary>(searchResponse.getAirSolution().getNonSeamenHashMap().values()));
                System.out.println("Size of non seamen if "+searchResponse.getAirSolution().getNonSeamenHashMap().values().size());
            } else {
                concurrentHashMap.put(searchParameters1.getJourneyList().get(0).getOrigin(), new ArrayList<FlightItinerary>(searchResponse.getAirSolution().getSeamenHashMap().values()));
                System.out.println("Size of non seamen else "+searchResponse.getAirSolution().getNonSeamenHashMap().values().size());
                //concurrentHashMap.put(searchParameters1.getJourneyList().get(0).getOrigin(), new ArrayList<FlightItinerary>(searchResponse.getAirSolution().getNonSeamenHashMap().values()));
            }
            responses.add(searchResponse);
        }
        for (Map.Entry<String, List<FlightItinerary>> flightItineraryEntry : concurrentHashMap.entrySet()) {
            logger.debug("flightItineraryEntry size: "+flightItineraryEntry.getKey()+"  -  "+flightItineraryEntry.getValue().size());
            System.out.println("flightItineraryEntry size: "+flightItineraryEntry.getKey()+"  -  "+flightItineraryEntry.getValue().size());
            if(flightItineraryEntry.getValue().size() == 0) {
                concurrentHashMap.remove(flightItineraryEntry.getKey());
            }
        }
        System.out.println("responses "+responses.size());
        return responses;
    }

    public SearchResponse findNextSegmentDeparture(SearchParameters searchParameters, FlightSearchOffice office) throws Exception {
        logger.debug("#####################AmadeusFlightSearch started  : ");
        logger.debug("#####################SearchParameters: \n"+ Json.toJson(searchParameters));
        SearchResponse searchResponse = new SearchResponse();
        AmadeusSessionWrapper amadeusSessionWrapper = null;
        searchResponse.setProvider("Amadeus");
        searchResponse.setFlightSearchOffice(office);
        String from = searchParameters.getJourneyList().get(0).getOrigin();
        String  to = searchParameters.getJourneyList().get(searchParameters.getJourneyList().size()-1).getDestination();
        searchResponse.setAirSegmentKey(from+to);
        FareMasterPricerTravelBoardSearchReply fareMasterPricerTravelBoardSearchReply = null;
        FareMasterPricerTravelBoardSearchReply seamenReply = null;

        try {
            long startTime = System.currentTimeMillis();
            amadeusSessionWrapper = amadeusSessionManager.getSession(office);
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            logger.debug("...................................Amadeus Search Session used: " + Json.toJson(amadeusSessionWrapper.getmSession().value));
            logger.debug("Execution time in getting session:: " + duration/1000 + " seconds");//to be removed
            if (searchParameters.getBookingType() == BookingType.SEAMEN) {
                seamenReply = serviceHandler.searchSplitAirlines(searchParameters, amadeusSessionWrapper,true);
                amadeusLogger.debug("AmadeusSearchRes "+ new Date()+" ------->>"+ new XStream().toXML(seamenReply));
            } else {
                fareMasterPricerTravelBoardSearchReply = serviceHandler.searchSplitAirlines(searchParameters, amadeusSessionWrapper,true);
                amadeusLogger.debug("AmadeusSearchRes "+ new Date()+" ------->>"+ new XStream().toXML(fareMasterPricerTravelBoardSearchReply));
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
            ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage("partialResults", ErrorMessage.ErrorType.ERROR, "Amadeus");
            searchResponse.getErrorMessageList().add(errorMessage);
            return searchResponse;
        }finally {
            amadeusSessionManager.updateAmadeusSession(amadeusSessionWrapper);
        }

        FareMasterPricerTravelBoardSearchReply.ErrorMessage seamenErrorMessage = null;
        FareMasterPricerTravelBoardSearchReply.ErrorMessage errorMessage = null;
        if (seamenReply != null) {
            seamenErrorMessage = seamenReply.getErrorMessage();
            if(seamenErrorMessage != null)
                logger.debug("seamenErrorMessage :" + seamenErrorMessage.getErrorMessageText().getDescription() + "  officeId:"+ office.getOfficeId());
        }

        if(fareMasterPricerTravelBoardSearchReply !=null) {
            errorMessage = fareMasterPricerTravelBoardSearchReply.getErrorMessage();
        }


        AirSolution airSolution = new AirSolution();
        logger.debug("#####################errorMessage is null");
        if (searchParameters.getBookingType() == BookingType.SEAMEN && seamenErrorMessage == null) {
            airSolution.setSeamenHashMap(getFlightItineraryHashmap(seamenReply,office, true));
        }

        if (searchParameters.getBookingType() == BookingType.NON_MARINE && errorMessage == null) {
            airSolution.setNonSeamenHashMap(getFlightItineraryHashmap(fareMasterPricerTravelBoardSearchReply, office, false));
        }
        searchResponse.setAirSolution(airSolution);
        searchResponse.setProvider(provider());
        searchResponse.setFlightSearchOffice(office);
        return searchResponse;
    }

    public String provider() {
        return "Amadeus";
    }
    @RetryOnFailure(attempts = 2, delay = 2000, exception = RetryException.class)
    public SearchResponse search(SearchParameters searchParameters, FlightSearchOffice office) throws Exception {
        logger.debug("#####################AmadeusFlightSearch started  : ");
        logger.debug("#####################SearchParameters: \n"+ Json.toJson(searchParameters));
        SearchResponse searchResponse = new SearchResponse();
        AmadeusSessionWrapper amadeusSessionWrapper = null;
        searchResponse.setProvider("Amadeus");
        searchResponse.setFlightSearchOffice(office);
        String from = searchParameters.getJourneyList().get(0).getOrigin();
        String  to = searchParameters.getJourneyList().get(searchParameters.getJourneyList().size()-1).getDestination();
        searchResponse.setAirSegmentKey(from+to);
        FareMasterPricerTravelBoardSearchReply fareMasterPricerTravelBoardSearchReply = null;
        FareMasterPricerTravelBoardSearchReply seamenReply = null;

        try {
            long startTime = System.currentTimeMillis();
            amadeusSessionWrapper = amadeusSessionManager.getSession(office);
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            logger.debug("...................................Amadeus Search Session used: " + Json.toJson(amadeusSessionWrapper.getmSession().value));
            logger.debug("Execution time in getting session:: " + duration/1000 + " seconds");//to be removed
            if (searchParameters.getBookingType() == BookingType.SEAMEN) {
                seamenReply = serviceHandler.searchSplitAirlines(searchParameters, amadeusSessionWrapper,false);
                amadeusLogger.debug("AmadeusSearchRes "+ new Date()+" ------->>"+ new XStream().toXML(seamenReply));
            } else {
                fareMasterPricerTravelBoardSearchReply = serviceHandler.searchSplitAirlines(searchParameters, amadeusSessionWrapper,false);
                amadeusLogger.debug("AmadeusSearchRes "+ new Date()+" ------->>"+ new XStream().toXML(fareMasterPricerTravelBoardSearchReply));
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
            ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage("partialResults", ErrorMessage.ErrorType.ERROR, "Amadeus");
            searchResponse.getErrorMessageList().add(errorMessage);
            return searchResponse;
        }finally {
            amadeusSessionManager.updateAmadeusSession(amadeusSessionWrapper);
        }

        FareMasterPricerTravelBoardSearchReply.ErrorMessage seamenErrorMessage = null;
        FareMasterPricerTravelBoardSearchReply.ErrorMessage errorMessage = null;
        if (seamenReply != null) {
            seamenErrorMessage = seamenReply.getErrorMessage();
            if(seamenErrorMessage != null)
                logger.debug("seamenErrorMessage :" + seamenErrorMessage.getErrorMessageText().getDescription() + "  officeId:"+ office.getOfficeId());
        }

        if (fareMasterPricerTravelBoardSearchReply != null) {
            errorMessage = fareMasterPricerTravelBoardSearchReply.getErrorMessage();
        }

        AirSolution airSolution = new AirSolution();
        logger.debug("#####################errorMessage is null");
        if (searchParameters.getBookingType() == BookingType.SEAMEN && seamenErrorMessage == null) {
            airSolution.setSeamenHashMap(getFlightItineraryHashmap(seamenReply,office, true));
        }

        if(searchParameters.getBookingType() == BookingType.NON_MARINE && errorMessage == null) {
            airSolution.setNonSeamenHashMap(getFlightItineraryHashmap(fareMasterPricerTravelBoardSearchReply, office,false));
        }
        searchResponse.setAirSolution(airSolution);
        searchResponse.setProvider(provider());
        searchResponse.setFlightSearchOffice(office);
        return searchResponse;
    }

    private ConcurrentHashMap<Integer, FlightItinerary> getFlightItineraryHashmap(FareMasterPricerTravelBoardSearchReply fareMasterPricerTravelBoardSearchReply, FlightSearchOffice office, boolean isSeamen) {
        ConcurrentHashMap<Integer, FlightItinerary> flightItineraryHashMap = new ConcurrentHashMap<>();
        try{
            String currency = fareMasterPricerTravelBoardSearchReply.getConversionRate().getConversionRateDetail().get(0).getCurrency();
            List<FareMasterPricerTravelBoardSearchReply.FlightIndex> flightIndexList = fareMasterPricerTravelBoardSearchReply.getFlightIndex();
            for (FareMasterPricerTravelBoardSearchReply.Recommendation recommendation : fareMasterPricerTravelBoardSearchReply.getRecommendation()) {
                for (ReferenceInfoType segmentRef : recommendation.getSegmentFlightRef()) {
                    FlightItinerary flightItinerary = new FlightItinerary();
                    flightItinerary.setPassportMandatory(false);
                    flightItinerary.setPricingInformation(getPricingInformation(recommendation));
                    flightItinerary.getPricingInformation().setGdsCurrency(currency);
                    flightItinerary.getPricingInformation().setPricingOfficeId(office.getOfficeId());
                    List<String> contextList = getAvailabilityCtx(segmentRef, recommendation.getSpecificRecDetails());
                    flightItinerary = createJourneyInformation(segmentRef, flightItinerary, flightIndexList, recommendation, contextList,isSeamen);
                    flightItinerary.getPricingInformation().setPaxFareDetailsList(createFareDetails(recommendation, flightItinerary.getJourneyList()));
                    flightItineraryHashMap.put(flightItinerary.hashCode(), flightItinerary);
                }
            }
            return flightItineraryHashMap;
        }catch (Exception e){
            logger.debug("error in getFlightItineraryHashmap :"+ e.getMessage());
        }
        return flightItineraryHashMap;
    }

    private FlightItinerary createJourneyInformation(ReferenceInfoType segmentRef, FlightItinerary flightItinerary, List<FareMasterPricerTravelBoardSearchReply.FlightIndex> flightIndexList, FareMasterPricerTravelBoardSearchReply.Recommendation recommendation, List<String> contextList,boolean isSeamen){
        int flightIndexNumber = 0;
        int segmentIndex = 0;
        for(ReferencingDetailsType191583C referencingDetailsType : segmentRef.getReferencingDetail()) {
            //0 is for forward journey and refQualifier should be S for segment
            if (referencingDetailsType.getRefQualifier().equalsIgnoreCase("S") ) {
                Journey journey = new Journey();
                journey = setJourney(journey, flightIndexList.get(flightIndexNumber).getGroupOfFlights().get(referencingDetailsType.getRefNumber().intValue()-1),recommendation);
                journey.setSeamen(isSeamen);
                if(contextList.size() > 0 ){
                    setContextInformation(contextList, journey, segmentIndex);
                }
                flightItinerary.setFromLocation(journey.getFromLocation());
                flightItinerary.setToLocation(journey.getToLocation());
                flightItinerary.getJourneyList().add(journey);
                flightItinerary.getNonSeamenJourneyList().add(journey);
                ++flightIndexNumber;
            }


        }
        return flightItinerary;
    }


    private  void setContextInformation(List<String> contextList, Journey journey, int segmentIndex){
        for (AirSegmentInformation airSegmentInformation : journey.getAirSegmentList()){
            airSegmentInformation.setContextType(contextList.get(segmentIndex));
            segmentIndex = segmentIndex + 1;
        }
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

    private Journey setJourney(Journey journey, FareMasterPricerTravelBoardSearchReply.FlightIndex.GroupOfFlights groupOfFlight, FareMasterPricerTravelBoardSearchReply.Recommendation recommendation){
        //no of stops
        journey.setNoOfStops(groupOfFlight.getFlightDetails().size()-1);

        //set travel time
        for(ProposedSegmentDetailsType proposedSegmentDetailsType : groupOfFlight.getPropFlightGrDetail().getFlightProposal()){
            if(proposedSegmentDetailsType.getUnitQualifier() != null && proposedSegmentDetailsType.getUnitQualifier().equals("EFT")){
                journey.setTravelTime(setTravelDuraion(proposedSegmentDetailsType.getRef()));
            }
        }
        //get farebases
        String fareBasis = getFareBasis(recommendation.getPaxFareProduct().get(0).getFareDetails().get(0));
        //set segments information

        String validatingCarrierCode = null;
        if(recommendation.getPaxFareProduct().get(0).getPaxFareDetail().getCodeShareDetails().get(0).getTransportStageQualifier().equals("V")) {
            validatingCarrierCode = recommendation.getPaxFareProduct().get(0).getPaxFareDetail().getCodeShareDetails().get(0).getCompany();
        }

        StringBuilder fullSegmentBuilder = new StringBuilder();
        for(FareMasterPricerTravelBoardSearchReply.FlightIndex.GroupOfFlights.FlightDetails flightDetails : groupOfFlight.getFlightDetails()){
            AirSegmentInformation airSegmentInformation = setSegmentInformation(flightDetails, fareBasis, validatingCarrierCode);
            if(airSegmentInformation.getToAirport().getAirportName() != null && airSegmentInformation.getFromAirport().getAirportName() != null) {
                journey.getAirSegmentList().add(airSegmentInformation);
                fullSegmentBuilder.append(airSegmentInformation.getFromLocation());
                fullSegmentBuilder.append(airSegmentInformation.getToLocation());
                journey.setProvider("Amadeus");
            }
        }
        List<AirSegmentInformation> airSegmentInformations = journey.getAirSegmentList();
        StringBuilder segmentBuilder = new StringBuilder();
        if(airSegmentInformations.size() > 0) {
            segmentBuilder.append(airSegmentInformations.get(0).getFromLocation());
            segmentBuilder.append(airSegmentInformations.get(airSegmentInformations.size()-1).getToLocation());
            journey.setFromLocation(airSegmentInformations.get(0).getFromLocation());
            journey.setToLocation(airSegmentInformations.get(airSegmentInformations.size()-1).getToLocation());
        }
        journey.setSegmentKey(segmentBuilder.toString());
        journey.setFullSegmentKey(fullSegmentBuilder.toString());
        getConnectionTime(journey.getAirSegmentList());
        return journey;
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

    private AirSegmentInformation setSegmentInformation(FareMasterPricerTravelBoardSearchReply.FlightIndex.GroupOfFlights.FlightDetails flightDetails, String fareBasis, String validatingCarrierCode){
        AirSegmentInformation airSegmentInformation = new AirSegmentInformation();
        TravelProductType flightInformation=flightDetails.getFlightInformation();
        airSegmentInformation.setCarrierCode(flightInformation.getCompanyId().getMarketingCarrier());
        if(flightInformation.getCompanyId().getOperatingCarrier() != null)
            airSegmentInformation.setOperatingCarrierCode(flightInformation.getCompanyId().getOperatingCarrier());
        airSegmentInformation.setFlightNumber(flightInformation.getFlightOrtrainNumber());
        airSegmentInformation.setEquipment(flightInformation.getProductDetail().getEquipmentType());
        airSegmentInformation.setValidatingCarrierCode(validatingCarrierCode);
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
        fromAirport = Airport.getAirport(airSegmentInformation.getFromLocation(), redisTemplate);
        toAirport = Airport.getAirport(airSegmentInformation.getToLocation(), redisTemplate);

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
        airSegmentInformation.setArrivalDate(arrivalDate.toDate());
        airSegmentInformation.setFromAirport(fromAirport);
        airSegmentInformation.setToAirport(toAirport);
        Minutes diff = Minutes.minutesBetween(departureDate, arrivalDate);

        airSegmentInformation.setFareBasis(fareBasis);

        airSegmentInformation.setTravelTime("" + diff.getMinutes());
        if (flightInformation.getCompanyId() != null && flightInformation.getCompanyId().getMarketingCarrier() != null && flightInformation.getCompanyId().getMarketingCarrier().length() >= 2) {
            airSegmentInformation.setAirline(Airline.getAirlineByCode(flightInformation.getCompanyId().getMarketingCarrier(), redisTemplate));
            airSegmentInformation.setOperatingAirline(Airline.getAirlineByCode(flightInformation.getCompanyId().getOperatingCarrier(), redisTemplate));
        }

        //hopping
        if(flightDetails.getTechnicalStop()!=null){
            List<HoppingFlightInformation> hoppingFlightInformations = null;
            for (DateAndTimeInformationType dateAndTimeInformationType :flightDetails.getTechnicalStop()){
                //Arrival
                HoppingFlightInformation hop =new HoppingFlightInformation();
                hop.setLocation(dateAndTimeInformationType.getStopDetails().get(0).getLocationId());
                hop.setStartTime(new StringBuilder(dateAndTimeInformationType.getStopDetails().get(0).getFirstTime()).insert(2, ":").toString());
                SimpleDateFormat dateParser = new SimpleDateFormat("ddMMyy");
                Date startDate = null;
                Date endDate = null;
                try {
                    startDate = dateParser.parse(dateAndTimeInformationType.getStopDetails().get(0).getDate());
                    endDate = dateParser.parse(dateAndTimeInformationType.getStopDetails().get(1).getDate());
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy");
                hop.setStartDate(dateFormat.format(startDate));
                //Departure
                hop.setEndTime(new StringBuilder(dateAndTimeInformationType.getStopDetails().get(1).getFirstTime()).insert(2, ":").toString());
                hop.setEndDate(dateFormat.format(endDate));
                if(hoppingFlightInformations==null){
                    hoppingFlightInformations = new ArrayList<HoppingFlightInformation>();
                }
                hoppingFlightInformations.add(hop);
            }
            airSegmentInformation.setHoppingFlightInformations(hoppingFlightInformations);
        }
        return airSegmentInformation;
    }

    private PricingInformation getPricingInformation(FareMasterPricerTravelBoardSearchReply.Recommendation recommendation) {
        PricingInformation pricingInformation = new PricingInformation();
        pricingInformation.setProvider("Amadeus");
        List<MonetaryInformationDetailsType> monetaryDetails = recommendation.getRecPriceInfo().getMonetaryDetail();
        BigDecimal totalFare = monetaryDetails.get(0).getAmount();
        BigDecimal totalTax = monetaryDetails.get(1).getAmount();
        pricingInformation.setBasePrice(totalFare.subtract(totalTax));
        pricingInformation.setTax(totalTax);
        pricingInformation.setTotalPrice(totalFare);
        pricingInformation.setTotalPriceValue(totalFare);
        List<PassengerTax> passengerTaxes= new ArrayList<>();
        for(FareMasterPricerTravelBoardSearchReply.Recommendation.PaxFareProduct paxFareProduct : recommendation.getPaxFareProduct()) {
            PassengerTax passengerTax = new PassengerTax();
            int paxCount = paxFareProduct.getPaxReference().get(0).getTraveller().size();
            String paxType = paxFareProduct.getPaxReference().get(0).getPtc().get(0);
            PricingTicketingSubsequentType144401S fareDetails = paxFareProduct.getPaxFareDetail();
            BigDecimal amount = fareDetails.getTotalFareAmount();
            BigDecimal tax = fareDetails.getTotalTaxAmount();
            BigDecimal baseFare = amount.subtract(tax);
            if(paxType.equalsIgnoreCase("ADT") || paxType.equalsIgnoreCase("SEA")) {
//        		pricingInformation.setAdtBasePrice(baseFare.multiply(new BigDecimal(paxCount)));
                pricingInformation.setAdtBasePrice(baseFare);
                pricingInformation.setAdtTotalPrice(amount);
                passengerTax.setPassengerType("ADT");
                passengerTax.setTotalTax(tax);
                passengerTax.setPassengerCount(paxCount);
            } else if(paxType.equalsIgnoreCase("CHD")) {
//				pricingInformation.setChdBasePrice(baseFare.multiply(new BigDecimal(paxCount)));
                pricingInformation.setChdBasePrice(baseFare);
                pricingInformation.setChdTotalPrice(amount);
                passengerTax.setPassengerType("CHD");
                passengerTax.setTotalTax(tax);
                passengerTax.setPassengerCount(paxCount);
            } else if(paxType.equalsIgnoreCase("INF")) {
//				pricingInformation.setInfBasePrice(baseFare.multiply(new BigDecimal(paxCount)));
                pricingInformation.setInfBasePrice(baseFare);
                pricingInformation.setInfTotalPrice(amount);
                passengerTax.setPassengerType("INF");
                passengerTax.setTotalTax(tax);
                passengerTax.setPassengerCount(paxCount);
            }
            passengerTaxes.add(passengerTax);
        }
        pricingInformation.setPassengerTaxes(passengerTaxes);
        return pricingInformation;
    }

    public String getFareBasis(FareMasterPricerTravelBoardSearchReply.Recommendation.PaxFareProduct.FareDetails fareDetails){
        for(FareMasterPricerTravelBoardSearchReply.Recommendation.PaxFareProduct.FareDetails.GroupOfFares groupOfFares : fareDetails.getGroupOfFares()){
            return groupOfFares.getProductInformation().getFareProductDetail().getFareBasis();

        }
        return  null;
    }

    private List getAvailabilityCtx(ReferenceInfoType segmentRef, List<FareMasterPricerTravelBoardSearchReply.Recommendation.SpecificRecDetails> specificRecDetails){
        List<String> contextList = new ArrayList<>();
        for(ReferencingDetailsType191583C referencingDetailsType : segmentRef.getReferencingDetail()) {


            if(referencingDetailsType.getRefQualifier().equalsIgnoreCase("A")){
                BigInteger refNumber = referencingDetailsType.getRefNumber();
                for(FareMasterPricerTravelBoardSearchReply.Recommendation.SpecificRecDetails specificRecDetail : specificRecDetails){
                    if(refNumber.equals(specificRecDetail.getSpecificRecItem().getRefNumber())){
                        for(FareMasterPricerTravelBoardSearchReply.Recommendation.SpecificRecDetails.SpecificProductDetails specificProductDetails : specificRecDetail.getSpecificProductDetails()){
                            for(FareMasterPricerTravelBoardSearchReply.Recommendation.SpecificRecDetails.SpecificProductDetails.FareContextDetails fareContextDetails : specificProductDetails.getFareContextDetails()){
                                for(FareMasterPricerTravelBoardSearchReply.Recommendation.SpecificRecDetails.SpecificProductDetails.FareContextDetails.CnxContextDetails cnxContextDetails : fareContextDetails.getCnxContextDetails()){
                                    contextList.addAll(cnxContextDetails.getFareCnxInfo().getContextDetails().getAvailabilityCnxType());
                                }

                            }
                        }
                    }
                }
            }

        }

        return contextList;
    }

    private List<PAXFareDetails> createFareDetails(FareMasterPricerTravelBoardSearchReply.Recommendation recommendation, List<Journey> journeys){
        List<PAXFareDetails> paxFareDetailsList = new ArrayList<>();
        for(FareMasterPricerTravelBoardSearchReply.Recommendation.PaxFareProduct paxFareProduct :recommendation.getPaxFareProduct()){
            PAXFareDetails paxFareDetails = new PAXFareDetails();
            for(FareMasterPricerTravelBoardSearchReply.Recommendation.PaxFareProduct.FareDetails fareDetails :paxFareProduct.getFareDetails()){
                FareJourney fareJourney = new FareJourney();
                for(FareMasterPricerTravelBoardSearchReply.Recommendation.PaxFareProduct.FareDetails.GroupOfFares groupOfFares: fareDetails.getGroupOfFares()){
                    FareSegment fareSegment = new FareSegment();
                    fareSegment.setBookingClass(groupOfFares.getProductInformation().getCabinProduct().getRbd());
                    fareSegment.setCabinClass(groupOfFares.getProductInformation().getCabinProduct().getCabin());
                    paxFareDetails.setPassengerTypeCode(PassengerTypeCode.valueOf(groupOfFares.getProductInformation().getFareProductDetail().getPassengerType()));

                    fareSegment.setFareBasis(groupOfFares.getProductInformation().getFareProductDetail().getFareBasis());
                    fareJourney.getFareSegmentList().add(fareSegment);
                }
                paxFareDetails.getFareJourneyList().add(fareJourney);
            }
            paxFareDetailsList.add(paxFareDetails);
        }
        return paxFareDetailsList;
    }
}
