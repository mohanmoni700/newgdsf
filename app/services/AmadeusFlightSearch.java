package services;

import com.amadeus.xml.fmptbr_14_2_1a.*;
import com.amadeus.xml.fmptbr_14_2_1a.FareMasterPricerTravelBoardSearchReply.Recommendation.PaxFareProduct;
import com.amadeus.xml.fmptbr_14_2_1a.FareMasterPricerTravelBoardSearchReply.Recommendation.SpecificRecDetails;
import com.amadeus.xml.fmtctr_18_2_1a.ReferencingDetailsType195563C;
import com.amadeus.xml.fmtctr_18_2_1a.TicketATCShopperMasterPricerTravelBoardSearchReply;
import com.compassites.GDSWrapper.amadeus.ServiceHandler;
import com.compassites.constants.AmadeusConstants;
import com.compassites.exceptions.IncompleteDetailsMessage;
import com.compassites.exceptions.RetryException;
import com.compassites.model.*;
import com.sun.xml.ws.client.ClientTransportException;
import com.sun.xml.ws.fault.ServerSOAPFaultException;
import com.thoughtworks.xstream.XStream;
import models.Airline;
import models.Airport;
import models.AmadeusSessionWrapper;
import models.FlightSearchOffice;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Minutes;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import play.Play;
import play.libs.Json;
import utils.AmadeusSessionManager;
import utils.ErrorMessageHelper;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.amadeus.xml.fmptbr_14_2_1a.FareMasterPricerTravelBoardSearchReply.FlightIndex;
import static com.amadeus.xml.fmptbr_14_2_1a.FareMasterPricerTravelBoardSearchReply.Recommendation;
import static com.compassites.model.PROVIDERS.AMADEUS;
import static java.lang.String.valueOf;

/**
 * Created with IntelliJ IDEA.
 * User: Renu
 * Date: 5/23/14
 * Time: 3:50 PM
 * To change this template use File | Settings | File Templates.
 */
@Service
public class AmadeusFlightSearch implements FlightSearch {

    static Logger logger = LoggerFactory.getLogger("gds");

    static Logger amadeusLogger = LoggerFactory.getLogger("amadeus");

    private final ServiceHandler serviceHandler;

    private final AmadeusSessionManager amadeusSessionManager;

    @Autowired
    private AmadeusSourceOfficeService sourceOfficeService;

    @Autowired
    private RedisTemplate redisTemplate;

    public RedisTemplate getRedisTemplate() {
        return redisTemplate;
    }

    public void setRedisTemplate(RedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Autowired
    public AmadeusFlightSearch(ServiceHandler serviceHandler, AmadeusSessionManager amadeusSessionManager) {
        this.serviceHandler = serviceHandler;
        this.amadeusSessionManager = amadeusSessionManager;
    }

    @RetryOnFailure(attempts = 2, delay = 2000, exception = RetryException.class)
    public SearchResponse search(SearchParameters searchParameters, FlightSearchOffice office) throws Exception {
        logger.debug("#####################AmadeusFlightSearch started  : ");
        logger.debug("#####################SearchParameters: \n" + Json.toJson(searchParameters));
        SearchResponse searchResponse = new SearchResponse();
        AmadeusSessionWrapper amadeusSessionWrapper = null;
        searchResponse.setProvider("Amadeus");
        searchResponse.setFlightSearchOffice(office);
        FareMasterPricerTravelBoardSearchReply fareMasterPricerTravelBoardSearchReply = null;
        FareMasterPricerTravelBoardSearchReply seamenReply = null;

        try {
            long startTime = System.currentTimeMillis();
            amadeusSessionWrapper = amadeusSessionManager.getSession(office);
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            logger.debug("...................................Amadeus Search Session used: " + Json.toJson(amadeusSessionWrapper.getmSession().value));
            //System.out.println("Execution time in getting session: " + duration/1000 + " seconds");
            logger.debug("Execution time in getting session:: " + duration / 1000 + " seconds");//to be removed
//            serviceHandler.logIn();
            if (searchParameters.getBookingType() == BookingType.SEAMEN) {
                seamenReply = serviceHandler.searchAirlines(searchParameters, amadeusSessionWrapper, "Marine");
//                logger.debug("#####################seamenReply: \n"+Json.toJson(seamenReply));

                amadeusLogger.debug("AmadeusSearchRes Marine{} Office ID : {} ------->>{}", new Date(), amadeusSessionWrapper.getOfficeId(), new XStream().toXML(seamenReply));

                searchParameters.setBookingType(BookingType.NON_MARINE);
                fareMasterPricerTravelBoardSearchReply = serviceHandler.searchAirlines(searchParameters, amadeusSessionWrapper, "Corporate");
//                logger.debug("fareMasterPricerTravelBoardSearchReply: \n"+Json.toJson(fareMasterPricerTravelBoardSearchReply));
                searchParameters.setBookingType(BookingType.SEAMEN);
//                XMLFileUtility.createXMLFile(seamenReply, "AmadeusSeamenSearchRes.xml");

//                amadeusLogger.debug("AmadeusSeamenSearchRes "+ new Date()+" ------->>"+ new XStream().toXML(seamenReply));
                amadeusLogger.debug("AmadeusSearchRes Corporate {} Office ID : {} Corporate  ------->>{}", new Date(), amadeusSessionWrapper.getOfficeId(), new XStream().toXML(fareMasterPricerTravelBoardSearchReply));
//                XMLFileUtility.createXMLFile(fareMasterPricerTravelBoardSearchReply, "AmadeusSearchRes.xml");
            } else {
                fareMasterPricerTravelBoardSearchReply = serviceHandler.searchAirlines(searchParameters, amadeusSessionWrapper, "Corporate");

//                XMLFileUtility.createXMLFile(fareMasterPricerTravelBoardSearchReply, "AmadeusSearchRes.xml");
                amadeusLogger.debug("AmadeusSearchRes Corp{} Office ID : {} ------->>{}", new Date(), amadeusSessionWrapper.getOfficeId(), new XStream().toXML(fareMasterPricerTravelBoardSearchReply));
            }

//            serviceHandler.logOut();
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
        } finally {
            amadeusSessionManager.updateAmadeusSession(amadeusSessionWrapper);
        }

        // logger.debug("AmadeusFlightSearch search reponse at : " + new Date());
        FareMasterPricerTravelBoardSearchReply.ErrorMessage seamenErrorMessage = null;
        FareMasterPricerTravelBoardSearchReply.ErrorMessage errorMessage = fareMasterPricerTravelBoardSearchReply.getErrorMessage();
        if (seamenReply != null) {
            seamenErrorMessage = seamenReply.getErrorMessage();
            if (seamenErrorMessage != null)
                logger.debug("seamenErrorMessage :" + seamenErrorMessage.getErrorMessageText().getDescription() + "  officeId:" + office.getOfficeId());
        }

        AirSolution airSolution = new AirSolution();
        ConcurrentHashMap<Integer, List<FlightItinerary>> integerListConcurrentHashMap = new ConcurrentHashMap<>();
        if (errorMessage != null) {
            logger.debug("#####################errorMessage is not null");
            String errorCode = errorMessage.getApplicationError().getApplicationErrorDetail().getError();
            if (!AmadeusConstants.NO_ITINERARY_ERROR_CODE.contains(errorCode)) {
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
            }

        } else {
            //return flight
            logger.debug("#####################errorMessage is null");

            ConcurrentHashMap<String, List<Integer>> groupingKeyMap = new ConcurrentHashMap<>();
            airSolution.setNonSeamenHashMap(getFlightItineraryHashmap(fareMasterPricerTravelBoardSearchReply, office, groupingKeyMap, false, office.getOfficeId(), integerListConcurrentHashMap));
            if (searchParameters.getJourneyType().equals(JourneyType.ONE_WAY)) {
                airSolution.setGroupingKeyMap(groupingKeyMap);
            }

            if (Play.application().configuration().getBoolean("amadeus.DEBUG_SEARCH_LOG")) {
                printHashmap(airSolution.getNonSeamenHashMap(), false);//to be removed
            }
            //printHashmap(airSolution.getNonSeamenHashMap(), false);
            if (searchParameters.getBookingType() == BookingType.SEAMEN && seamenErrorMessage == null) {
                ///AirSolution seamenSolution = new AirSolution();
                ///seamenSolution = createAirSolutionFromRecommendation(seamenReply);
                ///airSolution.setSeamenHashMap(seamenSolution.getNonSeamenHashMap());
                ConcurrentHashMap<String, List<Integer>> seamenMap = new ConcurrentHashMap<>();
                ConcurrentHashMap<Integer, List<FlightItinerary>> seamenIntegerListConcurrentHashMap = new ConcurrentHashMap<>();
                airSolution.setSeamenHashMap(getFlightItineraryHashmap(seamenReply, office, seamenMap, true, office.getOfficeId(), seamenIntegerListConcurrentHashMap));
                //System.out.println("airSolution Seamen "+Json.toJson(airSolution.getSeamenHashMap()));
                if (Play.application().configuration().getBoolean("amadeus.DEBUG_SEARCH_LOG")) {
                    printHashmap(airSolution.getSeamenHashMap(), true);//to be removed
                }
                ///seamenSolution.setNonSeamenHashMap(null);
                //addSeamenFareToSolution(airSolution, seamenSolution);
            }
        }
        searchResponse.setAirSolution(airSolution);
        searchResponse.setProvider(provider());
        searchResponse.setFlightSearchOffice(office);

        if (searchParameters.getJourneyType().equals(JourneyType.ONE_WAY)) {

            searchResponse.setGroupingItinerary(integerListConcurrentHashMap);
        }
        return searchResponse;
    }

    //todo to be removed
    public static void printHashmap(ConcurrentHashMap<Integer, FlightItinerary> hashMap, boolean iSeaman) {
        try {
            System.out.println("Is Seaman :" + iSeaman + "  count:" + hashMap.values().size());
            logger.debug("Is Seaman :" + iSeaman + "  count:" + hashMap.values().size());
            boolean isMarine = false;
            if (iSeaman)
                isMarine = true;

            for (Map.Entry<Integer, FlightItinerary> entry : hashMap.entrySet()) {
                FlightItinerary value = entry.getValue();
                if (entry.getKey() == 1521758370 || entry.getKey() == 1521756448) {
                    //System.out.println("");
                }
                if (value.getSeamanPricingInformation() != null && value.getSeamanPricingInformation().getTotalPriceValue() != null) {
                    isMarine = true;
                }
                String v = ", " + isMarine + ", " + value.getPricingInformation().getPricingOfficeId() + ", " + value.getPricingInformation().getTotalPriceValue() +
                        ", " + value.getJourneyList().get(0).getAirSegmentList().get(0).getCarrierCode() + ", " + value.getJourneyList().get(0).getAirSegmentList().get(0).getFlightNumber() + ",  " + value.getJourneyList().get(0).getTravelTimeStr();
                System.out.println(entry.getKey() + ",  " + v);
                //logger.debug(entry.getKey() + ",  " + v);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String provider() {
        return "Amadeus";
    }

    @Override
    public List<FlightSearchOffice> getOfficeList() {
        return sourceOfficeService.getAllOffices();
    }

    //for round trip and refactored
//    private AirSolution createAirSolutionFromRecommendation(FareMasterPricerTravelBoardSearchReply fareMasterPricerTravelBoardSearchReply) {
//        AirSolution airSolution = new AirSolution();
//        //airSolution.setFlightItineraryList(createFlightItineraryList(airSolution, fareMasterPricerTravelBoardSearchReply));
//        airSolution.setNonSeamenHashMap(getFlightItineraryHashmap( fareMasterPricerTravelBoardSearchReply, new FlightSearchOffice()));
//        return airSolution;
//    }

    //list with all flight informaition
//    private List<FareMasterPricerTravelBoardSearchReply.FlightIndex> flightIndexList=new ArrayList<>();


    private ConcurrentHashMap<Integer, FlightItinerary> getFlightItineraryHashmap(FareMasterPricerTravelBoardSearchReply fareMasterPricerTravelBoardSearchReply, FlightSearchOffice office, ConcurrentHashMap<String, List<Integer>> groupingKeyMap, boolean isSeamen, String officeId, ConcurrentHashMap<Integer, List<FlightItinerary>> integerListConcurrentHashMap) {
        ConcurrentHashMap<Integer, FlightItinerary> flightItineraryHashMap = new ConcurrentHashMap<>();
        try {

            List<FareMasterPricerTravelBoardSearchReply.FlightIndex> flightIndexList = fareMasterPricerTravelBoardSearchReply.getFlightIndex();

            FareMasterPricerTravelBoardSearchReply.MnrGrp mnrGrp = fareMasterPricerTravelBoardSearchReply.getMnrGrp();
            List<FareMasterPricerTravelBoardSearchReply.ServiceFeesGrp> baggageList = fareMasterPricerTravelBoardSearchReply.getServiceFeesGrp();
            String currency = fareMasterPricerTravelBoardSearchReply.getConversionRate().getConversionRateDetail().get(0).getCurrency();


            int k = 300;

            for (Recommendation recommendation : fareMasterPricerTravelBoardSearchReply.getRecommendation()) {
                for (ReferenceInfoType segmentRef : recommendation.getSegmentFlightRef()) {
                    FlightItinerary flightItinerary = new FlightItinerary();
                    flightItinerary.setPassportMandatory(false);

                    PricingInformation pricingInformation = null;
                    try {
                        pricingInformation = getPricingInformation(recommendation, office.getOfficeId(), segmentRef, mnrGrp, baggageList);
                    } catch (Exception e) {
                        logger.debug("Error at setting pricing information while flight search {} ", e.getMessage(), e);
                    }
                    flightItinerary.setPricingInformation(pricingInformation);

                    flightItinerary.getPricingInformation().setGdsCurrency(currency);
                    flightItinerary.getPricingInformation().setPricingOfficeId(office.getOfficeId());

                    flightItinerary.getPricingInformation().setPaxFareDetailsList(createFareDetails(recommendation, flightItinerary.getJourneyList()));


                    List<String> contextList = getAvailabilityCtx(segmentRef, recommendation.getSpecificRecDetails());

                    int flightHash = flightItinerary.hashCode() + k;
                    flightItinerary = createJourneyInformation(segmentRef, flightItinerary, flightIndexList, recommendation, contextList, groupingKeyMap, flightHash, isSeamen, mnrGrp, baggageList, officeId);

                    if (!isSeamen) {
                        if (recommendation.getFareFamilyRef() != null && !recommendation.getFareFamilyRef().getReferencingDetail().isEmpty()) {
                            BigInteger ref = recommendation.getFareFamilyRef().getReferencingDetail().get(0).getRefNumber();
                            getFareType(flightItinerary, fareMasterPricerTravelBoardSearchReply, ref);
                        }
                    }
                    flightItineraryHashMap.put(flightItinerary.hashCode(), flightItinerary);
                    if (!isSeamen) {
                        createMappingKey(flightItinerary, groupingKeyMap, isSeamen, k);
                        createGroupItinerary(flightItinerary, integerListConcurrentHashMap, isSeamen);
                    }
                    k++;
                }
            }
            //System.out.println("Result size "+Json.toJson(flightItineraryHashMap));
            return flightItineraryHashMap;
        } catch (Exception e) {
            e.printStackTrace();
            logger.debug("error in getFlightItineraryHashmap :" + e.getMessage());
        }
        return flightItineraryHashMap;
    }


    private void createGroupItinerary(FlightItinerary flightItinerary, ConcurrentHashMap<Integer, List<FlightItinerary>> integerListConcurrentHashMap, boolean isSeamen) {
        if (!isSeamen) {
            if (integerListConcurrentHashMap.containsKey(flightItinerary.hashCode())) {
                List<FlightItinerary> mapList = integerListConcurrentHashMap.get(flightItinerary.hashCode());
                mapList.add(flightItinerary);
                integerListConcurrentHashMap.put(flightItinerary.hashCode(), mapList);
            } else {
                List<FlightItinerary> hashList = new ArrayList<>();
                hashList.add(flightItinerary);
                integerListConcurrentHashMap.put(flightItinerary.hashCode(), hashList);
            }
        }
    }

    private void createMappingKey(FlightItinerary flightItinerary, ConcurrentHashMap<String, List<Integer>> groupingKeyMap, boolean isSeamen, int k) {
        for (Journey journey : flightItinerary.getJourneyList()) {
            StringBuilder groupingKey = new StringBuilder();
            for (AirSegmentInformation airSegmentInformation : journey.getAirSegmentList()) {
                groupingKey.append(airSegmentInformation.getFromLocation());
                groupingKey.append(airSegmentInformation.getToLocation());
                groupingKey.append(airSegmentInformation.getFlightNumber());
                groupingKey.append(airSegmentInformation.getCarrierCode());
                groupingKey.append(airSegmentInformation.getDepartureDate());
            }
            if (!isSeamen) {
                journey.setGroupingKey(groupingKey.toString());
                if (groupingKeyMap.containsKey(groupingKey.toString())) {
                    List<Integer> mapList = groupingKeyMap.get(groupingKey.toString());
                    mapList.add(flightItinerary.hashCode() + k);
                    groupingKeyMap.put(groupingKey.toString(), mapList);
                } else {
                    List<Integer> hashList = new ArrayList<>();
                    hashList.add(flightItinerary.hashCode() + k);
                    groupingKeyMap.put(groupingKey.toString(), hashList);
                }
            }
        }
    }


    private void getFareType(FlightItinerary flightItinerary, FareMasterPricerTravelBoardSearchReply fareMasterPricerTravelBoardSearchReply, BigInteger refNumber) {

        try {
            //System.out.println(fareMasterPricerTravelBoardSearchReply.getFamilyInformation().get(refNumber.intValue() - 1).getFareFamilyname());
            flightItinerary.setFareType(fareMasterPricerTravelBoardSearchReply.getFamilyInformation().get(refNumber.intValue() - 1).getDescription());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private FlightItinerary createJourneyInformation(ReferenceInfoType segmentRef, FlightItinerary flightItinerary, List<FlightIndex> flightIndexList, Recommendation recommendation, List<String> contextList, ConcurrentHashMap<String, List<Integer>> groupingKeyMap, int flightHash, boolean isSeamen, FareMasterPricerTravelBoardSearchReply.MnrGrp mnrGrp, List<FareMasterPricerTravelBoardSearchReply.ServiceFeesGrp> baggageList, String officeId) {
        int flightIndexNumber = 0;
        int segmentIndex = 0;

        for (ReferencingDetailsType191583C referencingDetailsType : segmentRef.getReferencingDetail()) {
            //0 is for forward journey and refQualifier should be S for segment


            if (referencingDetailsType.getRefQualifier().equalsIgnoreCase("S")) {
                StringBuilder groupingKey = new StringBuilder();
                Journey journey = new Journey();
                journey = setJourney(journey, flightIndexList.get(flightIndexNumber).getGroupOfFlights().get(referencingDetailsType.getRefNumber().intValue() - 1), recommendation, groupingKeyMap, flightHash, groupingKey, officeId);
                if (contextList.size() > 0) {

                    setContextInformation(contextList, journey, segmentIndex);
                }
                flightItinerary.getJourneyList().add(journey);
                flightItinerary.getNonSeamenJourneyList().add(journey);
                /*if(!isSeamen) {
                    journey.setGroupingKey(groupingKey.toString());
                    if (groupingKeyMap.containsKey(groupingKey.toString())) {
                        List<Integer> mapList = groupingKeyMap.get(groupingKey.toString());
                        mapList.add(flightHash);
                        groupingKeyMap.put(groupingKey.toString(), mapList);
                    } else {
                        List<Integer> hashList = new ArrayList<>();
                        hashList.add(flightHash);
                        groupingKeyMap.put(groupingKey.toString(), hashList);
                    }
                }*/
                ++flightIndexNumber;
            }
        }
        return flightItinerary;
    }


    public void setContextInformation(List<String> contextList, Journey journey, int segmentIndex) {
        for (AirSegmentInformation airSegmentInformation : journey.getAirSegmentList()) {
            airSegmentInformation.setContextType(contextList.get(segmentIndex));
            segmentIndex = segmentIndex + 1;
        }
    }

    private Duration setTravelDuraion(String totalElapsedTime) {
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

    private Journey setJourney(Journey journey, FlightIndex.GroupOfFlights groupOfFlight, Recommendation recommendation, ConcurrentHashMap<String, List<Integer>> concurrentHashMap, int flightHash, StringBuilder groupingKey, String officeId) {
        //no of stops

        journey.setNoOfStops(groupOfFlight.getFlightDetails().size() - 1);


        //set travel time
        for (ProposedSegmentDetailsType proposedSegmentDetailsType : groupOfFlight.getPropFlightGrDetail().getFlightProposal()) {
            if (proposedSegmentDetailsType.getUnitQualifier() != null && proposedSegmentDetailsType.getUnitQualifier().equals("EFT")) {
                journey.setTravelTime(setTravelDuraion(proposedSegmentDetailsType.getRef()));
            }
        }
        //get farebases
        String fareBasis = getFareBasis(recommendation.getPaxFareProduct().get(0).getFareDetails().get(0));
        //set segments information

        journey.setFareDescription(getFareDescription(recommendation.getPaxFareProduct().get(0).getFare()));
        journey.setLastTktDate(getLastTktDate(recommendation.getPaxFareProduct().get(0).getFare()));
        String validatingCarrierCode = null;
        if (recommendation.getPaxFareProduct().get(0).getPaxFareDetail().getCodeShareDetails().get(0).getTransportStageQualifier().equals("V")) {
            validatingCarrierCode = recommendation.getPaxFareProduct().get(0).getPaxFareDetail().getCodeShareDetails().get(0).getCompany();
        }

        for (FlightIndex.GroupOfFlights.FlightDetails flightDetails : groupOfFlight.getFlightDetails()) {
            AirSegmentInformation airSegmentInformation = setSegmentInformation(flightDetails, fareBasis, validatingCarrierCode);
            groupingKey.append(airSegmentInformation.getFromLocation());
            groupingKey.append(airSegmentInformation.getToLocation());
            groupingKey.append(airSegmentInformation.getFlightNumber());
            groupingKey.append(airSegmentInformation.getCarrierCode());
            groupingKey.append(airSegmentInformation.getDepartureDate());
            if (airSegmentInformation.getToAirport().getAirportName() != null && airSegmentInformation.getFromAirport().getAirportName() != null) {
                journey.getAirSegmentList().add(airSegmentInformation);
                journey.setProvider("Amadeus");
            }
        }
        getConnectionTime(journey.getAirSegmentList());
        return journey;
    }

    public List<String> getLastTktDate(List<Recommendation.PaxFareProduct.Fare> fares) {
        List<String> lastTktDate = new ArrayList<>();
        for (Recommendation.PaxFareProduct.Fare fare : fares) {
            if (fare.getPricingMessage().getFreeTextQualification().getTextSubjectQualifier().equalsIgnoreCase("LTD")) {
                //System.out.println(fare.getPricingMessage().getDescription().size());
                if (fare.getPricingMessage().getDescription().size() > 0) {
                    //System.out.println("fare"+fare.getPricingMessage().getDescription().get(0).toString());
                    lastTktDate.addAll(fare.getPricingMessage().getDescription());
                    return lastTktDate;
                }
            }
        }
        return lastTktDate;
    }

    public String getFareDescription(List<Recommendation.PaxFareProduct.Fare> fares) {
        String fareDesc = "";
        for (Recommendation.PaxFareProduct.Fare fare : fares) {
            if (fare.getPricingMessage().getFreeTextQualification().getTextSubjectQualifier().equalsIgnoreCase("PEN")) {
                fareDesc = fare.getPricingMessage().getDescription().get(0).toString();
            }
            return fareDesc;
        }
        return fareDesc;
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

    private AirSegmentInformation setSegmentInformation(FlightIndex.GroupOfFlights.FlightDetails flightDetails, String fareBasis, String validatingCarrierCode) {
        AirSegmentInformation airSegmentInformation = new AirSegmentInformation();
        TravelProductType flightInformation = flightDetails.getFlightInformation();
        airSegmentInformation.setCarrierCode(flightInformation.getCompanyId().getMarketingCarrier());
        if (flightInformation.getCompanyId().getOperatingCarrier() != null)
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

        /*if (prevArrivalTime.equalsIgnoreCase("")) {
            airSegmentInformation.setConnectionTime(0);
        } else {
            String pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
            DateTimeFormatter dtf = DateTimeFormat.forPattern(pattern);
            DateTime prevArrivalDateTime = dtf.parseDateTime(prevArrivalTime);
            Minutes diffDeparture = Minutes.minutesBetween(prevArrivalDateTime, departureDate);
            airSegmentInformation.setConnectionTime(diffDeparture.getMinutes());
        }*/

        airSegmentInformation.setFareBasis(fareBasis);

        airSegmentInformation.setTravelTime("" + diff.getMinutes());
        if (flightInformation.getCompanyId() != null && flightInformation.getCompanyId().getMarketingCarrier() != null && flightInformation.getCompanyId().getMarketingCarrier().length() >= 2) {
            airSegmentInformation.setAirline(Airline.getAirlineByCode(flightInformation.getCompanyId().getMarketingCarrier(), redisTemplate));
            airSegmentInformation.setOperatingAirline(Airline.getAirlineByCode(flightInformation.getCompanyId().getOperatingCarrier(), redisTemplate));
        }

        //hopping

        if (flightDetails.getTechnicalStop() != null) {
            List<HoppingFlightInformation> hoppingFlightInformations = null;
            for (DateAndTimeInformationType dateAndTimeInformationType : flightDetails.getTechnicalStop()) {
                //Arrival
                HoppingFlightInformation hop = new HoppingFlightInformation();
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
                if (hoppingFlightInformations == null) {
                    hoppingFlightInformations = new ArrayList<HoppingFlightInformation>();
                }
                hoppingFlightInformations.add(hop);
            }
            airSegmentInformation.setHoppingFlightInformations(hoppingFlightInformations);
        }
        return airSegmentInformation;
    }

    private PricingInformation getPricingInformation(Recommendation recommendation, String officeId, ReferenceInfoType segmentRef,
                                                     FareMasterPricerTravelBoardSearchReply.MnrGrp mnrGrp,
                                                     List<FareMasterPricerTravelBoardSearchReply.ServiceFeesGrp> baggageList) {

        PricingInformation pricingInformation = new PricingInformation();
        pricingInformation.setProvider("Amadeus");
        List<MonetaryInformationDetailsType> monetaryDetails = recommendation.getRecPriceInfo().getMonetaryDetail();
        BigDecimal totalFare = monetaryDetails.get(0).getAmount();
        BigDecimal totalTax = monetaryDetails.get(1).getAmount();
        pricingInformation.setBasePrice(totalFare.subtract(totalTax));
        pricingInformation.setTax(totalTax);
        pricingInformation.setTotalPrice(totalFare);
        pricingInformation.setTotalPriceValue(totalFare);
        List<PassengerTax> passengerTaxes = new ArrayList<>();
        for (Recommendation.PaxFareProduct paxFareProduct : recommendation.getPaxFareProduct()) {
            PassengerTax passengerTax = new PassengerTax();
            int paxCount = paxFareProduct.getPaxReference().get(0).getTraveller().size();
            String paxType = paxFareProduct.getPaxReference().get(0).getPtc().get(0);
            PricingTicketingSubsequentType144401S fareDetails = paxFareProduct.getPaxFareDetail();
            BigDecimal amount = fareDetails.getTotalFareAmount();
            BigDecimal tax = fareDetails.getTotalTaxAmount();
            BigDecimal baseFare = amount.subtract(tax);
            if (paxType.equalsIgnoreCase("ADT") || paxType.equalsIgnoreCase("SEA")) {
                pricingInformation.setAdtBasePrice(baseFare);
                pricingInformation.setAdtTotalPrice(amount);
                passengerTax.setPassengerType("ADT");
                passengerTax.setTotalTax(tax);
                passengerTax.setPassengerCount(paxCount);
            } else if (paxType.equalsIgnoreCase("CHD")) {
                pricingInformation.setChdBasePrice(baseFare);
                pricingInformation.setChdTotalPrice(amount);
                passengerTax.setPassengerType("CHD");
                passengerTax.setTotalTax(tax);
                passengerTax.setPassengerCount(paxCount);
            } else if (paxType.equalsIgnoreCase("INF")) {
                pricingInformation.setInfBasePrice(baseFare);
                pricingInformation.setInfTotalPrice(amount);
                passengerTax.setPassengerType("INF");
                passengerTax.setTotalTax(tax);
                passengerTax.setPassengerCount(paxCount);
            }
            passengerTaxes.add(passengerTax);
        }
        pricingInformation.setPassengerTaxes(passengerTaxes);

        if (!officeId.equalsIgnoreCase("BOMAK38SN")) {
            pricingInformation.setMnrSearchFareRules(createSearchFareRules(segmentRef, mnrGrp));
        }

        pricingInformation.setMnrSearchBaggage(createBaggageInformation(segmentRef, baggageList));

        return pricingInformation;
    }

    public static SimpleDateFormat searchFormat = new SimpleDateFormat("ddMMyy-kkmm");

    private AirSegmentInformation createSegment(TravelProductType flightInformation, String prevArrivalTime) {

        AirSegmentInformation airSegmentInformation = new AirSegmentInformation();
        airSegmentInformation.setCarrierCode(flightInformation.getCompanyId().getMarketingCarrier());
        if (flightInformation.getCompanyId().getOperatingCarrier() != null)
            airSegmentInformation.setOperatingCarrierCode(flightInformation.getCompanyId().getOperatingCarrier());
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
            airSegmentInformation.setAirline(Airline.getAirlineByCode(flightInformation.getCompanyId().getMarketingCarrier(), redisTemplate));
            airSegmentInformation.setOperatingAirline(Airline.getAirlineByCode(flightInformation.getCompanyId().getOperatingCarrier(), redisTemplate));
        }

        return airSegmentInformation;
    }

    private SearchResponse addSeamenFareToSolution(AirSolution allSolution, AirSolution seamenSolution) {
        logger.debug("==============================================================================");
        logger.debug("[Amadeus] All Solution Length:::::" + allSolution.getFlightItineraryList().size());
        logger.debug("[Amadeus] Seamen Solution Length:::::" + seamenSolution.getFlightItineraryList().size());
        logger.debug("==============================================================================");
        SearchResponse searchResponse = new SearchResponse();
        ConcurrentHashMap<Integer, FlightItinerary> allFaresHash = new ConcurrentHashMap<>();
        ConcurrentHashMap<Integer, FlightItinerary> seamenFareHash = new ConcurrentHashMap<>();
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


    private List<PAXFareDetails> createFareDetails(Recommendation recommendation, List<Journey> journeys) {
        List<PAXFareDetails> paxFareDetailsList = new ArrayList<>();
        for (Recommendation.PaxFareProduct paxFareProduct : recommendation.getPaxFareProduct()) {
            PAXFareDetails paxFareDetails = new PAXFareDetails();
            for (Recommendation.PaxFareProduct.FareDetails fareDetails : paxFareProduct.getFareDetails()) {
                FareJourney fareJourney = new FareJourney();
                for (Recommendation.PaxFareProduct.FareDetails.GroupOfFares groupOfFares : fareDetails.getGroupOfFares()) {
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

    public String getFareBasis(Recommendation.PaxFareProduct.FareDetails fareDetails) {
        for (PaxFareProduct.FareDetails.GroupOfFares groupOfFares : fareDetails.getGroupOfFares()) {
            return groupOfFares.getProductInformation().getFareProductDetail().getFareBasis();

        }
        return null;
    }

    public List getAvailabilityCtx(ReferenceInfoType segmentRef, List<SpecificRecDetails> specificRecDetails) {
        List<String> contextList = new ArrayList<>();
        for (ReferencingDetailsType191583C referencingDetailsType : segmentRef.getReferencingDetail()) {
            if (referencingDetailsType.getRefQualifier().equalsIgnoreCase("A")) {
                BigInteger refNumber = referencingDetailsType.getRefNumber();
                for (SpecificRecDetails specificRecDetail : specificRecDetails) {
                    if (refNumber.equals(specificRecDetail.getSpecificRecItem().getRefNumber())) {
                        for (SpecificRecDetails.SpecificProductDetails specificProductDetails : specificRecDetail.getSpecificProductDetails()) {
                            for (SpecificRecDetails.SpecificProductDetails.FareContextDetails fareContextDetails : specificProductDetails.getFareContextDetails()) {
                                for (SpecificRecDetails.SpecificProductDetails.FareContextDetails.CnxContextDetails cnxContextDetails : fareContextDetails.getCnxContextDetails()) {
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


    // Fare rules at search level set here
    private MnrSearchFareRules createSearchFareRules(ReferenceInfoType segmentRef, FareMasterPricerTravelBoardSearchReply.MnrGrp mnrGrp) {
        MnrSearchFareRules mnrSearchFareRules = new MnrSearchFareRules();

        try {
            BigDecimal changeFeeBeforeDeparture = null;
            BigDecimal cancellationFeeBeforeDeparture = null;
            Boolean isChangeAllowedBeforeDeparture = false;
            Boolean isCancellationAllowedBeforeDeparture = false;

            // Getting the reference Number here for 'M'
            String referenceNumber = segmentRef.getReferencingDetail().stream()
                    .filter(detail -> "M".equalsIgnoreCase(detail.getRefQualifier()))
                    .map(detail -> valueOf(detail.getRefNumber()))
                    .findFirst()
                    .orElse(null);


            if (referenceNumber == null) {
                return mnrSearchFareRules;
            }

            // Mapping Cancellation and Change here wrt to referenceNumber
            for (FareMasterPricerTravelBoardSearchReply.MnrGrp.MnrDetails mnrDetail : mnrGrp.getMnrDetails()) {
                if (!referenceNumber.equals(mnrDetail.getMnrRef().getItemNumberDetails().get(0).getNumber())) {
                    continue;
                }

                for (FareMasterPricerTravelBoardSearchReply.MnrGrp.MnrDetails.CatGrp catGrp : mnrDetail.getCatGrp()) {
                    BigInteger catRefNumber = catGrp.getCatInfo().getDescriptionInfo().getNumber();
                    List<StatusDetailsType256255C> statusInformation = catGrp.getStatusInfo().getStatusInformation();

                    // Change Fee (Category 31)
                    if (catRefNumber.equals(BigInteger.valueOf(31))) {

                        if (catGrp.getMonInfo() != null) {
                            MonetaryInformationType174241S monInfo = catGrp.getMonInfo();
                            changeFeeBeforeDeparture = (monInfo == null) ? null : getFeeAmount(monInfo);
                        }

                        isChangeAllowedBeforeDeparture = isAllowed(statusInformation, "BDJ", "1");
                    }

                    // Cancellation Fee (Category 33)
                    if (catRefNumber.equals(BigInteger.valueOf(33))) {

                        if (catGrp.getMonInfo() != null) {
                            MonetaryInformationType174241S monInfo = catGrp.getMonInfo();
                            cancellationFeeBeforeDeparture = (monInfo == null) ? null : getFeeAmount(monInfo);
                        }

                        isCancellationAllowedBeforeDeparture = isAllowed(statusInformation, "BDJ", "1");
                    }

                    if (changeFeeBeforeDeparture != null && cancellationFeeBeforeDeparture != null) {
                        break;
                    }
                }
                if (changeFeeBeforeDeparture != null && cancellationFeeBeforeDeparture != null) {
                    break;
                }
            }

            mnrSearchFareRules.setProvider(AMADEUS.toString());
            mnrSearchFareRules.setChangeFee(changeFeeBeforeDeparture);
            mnrSearchFareRules.setCancellationFee(cancellationFeeBeforeDeparture);
            mnrSearchFareRules.setChangeBeforeDepartureAllowed(isChangeAllowedBeforeDeparture);
            mnrSearchFareRules.setCancellationBeforeDepartureAllowed(isCancellationAllowedBeforeDeparture);

            return mnrSearchFareRules;
        } catch (Exception e) {
            logger.error("Mini rule error " + e.getMessage());
            return null;
        }
    }

    // Change and Cancellation fee extraction logic
    private BigDecimal getFeeAmount(MonetaryInformationType174241S monInfo) {

        if (monInfo.getMonetaryDetails() != null && "BDT".equalsIgnoreCase(monInfo.getMonetaryDetails().getTypeQualifier())) {

            return monInfo.getMonetaryDetails().getAmount();
        } else if (monInfo.getOtherMonetaryDetails() != null) {

            return monInfo.getOtherMonetaryDetails().stream()
                    .filter(details -> "BDT".equalsIgnoreCase(details.getTypeQualifier()))
                    .map(MonetaryInformationDetailsType245528C::getAmount)
                    .findFirst()
                    .orElse(null);
        }

        return null;
    }

    //Cancellation and Change Allowed or Not set here
    private boolean isAllowed(List<StatusDetailsType256255C> statusInformation, String indicator, String action) {
        return statusInformation.stream()
                .anyMatch(status -> indicator.equalsIgnoreCase(status.getIndicator()) && action.equalsIgnoreCase(status.getAction()));
    }


    //Baggage Information at search level here
    private MnrSearchBaggage createBaggageInformation(ReferenceInfoType segmentRef, List<FareMasterPricerTravelBoardSearchReply.ServiceFeesGrp> baggageListInfo) {

        try {
            MnrSearchBaggage mnrSearchBaggage = new MnrSearchBaggage();
            mnrSearchBaggage.setProvider(AMADEUS.toString());


            // Baggage reference number
            String baggageReferenceNumber = segmentRef.getReferencingDetail().stream()
                    .filter(referencingDetail -> "B".equalsIgnoreCase(referencingDetail.getRefQualifier()))
                    .map(referencingDetail -> valueOf(referencingDetail.getRefNumber()))
                    .findFirst()
                    .orElse(null);


            if (baggageReferenceNumber == null) {
                return mnrSearchBaggage;
            }


            // Finding the FBA reference from service group
            String fbaRefValue = null;
            outerForLoop:
            for (FareMasterPricerTravelBoardSearchReply.ServiceFeesGrp serviceFeesGrp : baggageListInfo) {

                if (!"FBA".equalsIgnoreCase(serviceFeesGrp.getServiceTypeInfo().getCarrierFeeDetails().getType())) {
                    continue;
                }

                List<FareMasterPricerTravelBoardSearchReply.ServiceFeesGrp.ServiceCoverageInfoGrp> serviceCoverageInfoGrpList = serviceFeesGrp.getServiceCoverageInfoGrp();
                for (FareMasterPricerTravelBoardSearchReply.ServiceFeesGrp.ServiceCoverageInfoGrp serviceCoverageInfoGrp : serviceCoverageInfoGrpList) {
                    String serviceGroupRef = serviceCoverageInfoGrp.getItemNumberInfo().getItemNumber().getNumber();
                    if (!serviceGroupRef.equalsIgnoreCase(baggageReferenceNumber)) {
                        continue;
                    }

                    List<FareMasterPricerTravelBoardSearchReply.ServiceFeesGrp.ServiceCoverageInfoGrp.ServiceCovInfoGrp> serviceCovInfoGrpList = serviceCoverageInfoGrp.getServiceCovInfoGrp();
                    for (FareMasterPricerTravelBoardSearchReply.ServiceFeesGrp.ServiceCoverageInfoGrp.ServiceCovInfoGrp serviceCovInfoGrp : serviceCovInfoGrpList) {

                        List<ReferencingDetailsType195561C> referencingDetailList = serviceCovInfoGrp.getRefInfo().getReferencingDetail();
                        for (ReferencingDetailsType195561C referencingDetails : referencingDetailList) {

                            if ("F".equalsIgnoreCase(referencingDetails.getRefQualifier())) {
                                fbaRefValue = String.valueOf(referencingDetails.getRefNumber());
                                break outerForLoop;
                            }
                        }
                    }
                }
            }

            // Find the baggage allowance info
            String finalFbaRefValue = fbaRefValue;
            String baggageAllowed = baggageListInfo.stream()
                    .filter(serviceFeesGrp -> serviceFeesGrp.getServiceTypeInfo().getCarrierFeeDetails().getType().equalsIgnoreCase("FBA"))
                    .flatMap(serviceFeesGrp -> serviceFeesGrp.getFreeBagAllowanceGrp().stream())
                    .filter(freeBagAllowance ->
                            freeBagAllowance.getItemNumberInfo().getItemNumberDetails().get(0).getNumber().toString().equals(finalFbaRefValue))
                    .map(freeBagAllowance -> {
                        BigInteger baggageValue = freeBagAllowance.getFreeBagAllownceInfo().getBaggageDetails().getFreeAllowance();
                        String baggageUnit = freeBagAllowance.getFreeBagAllownceInfo().getBaggageDetails().getQuantityCode();
                        return baggageValue + " " + MnrSearchBaggage.baggageCodes.get(baggageUnit);
                    })
                    .findFirst()
                    .orElse(null);

            mnrSearchBaggage.setAllowedBaggage(baggageAllowed);

            return mnrSearchBaggage;
        } catch (Exception e) {
            logger.debug("Error with baggage information at Search level", e);
            return null;
        }

    }

}
