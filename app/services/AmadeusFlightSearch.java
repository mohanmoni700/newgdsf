package services;

import com.amadeus.xml.fmptbr_14_2_1a.*;
import com.amadeus.xml.fmptbr_14_2_1a.FareMasterPricerTravelBoardSearchReply.Recommendation.PaxFareProduct;
import com.amadeus.xml.fmptbr_14_2_1a.FareMasterPricerTravelBoardSearchReply.Recommendation.SpecificRecDetails;
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

/**
 * Created with IntelliJ IDEA.
 * User: Renu
 * Date: 5/23/14
 * Time: 3:50 PM
 * To change this template use File | Settings | File Templates.
 */
@Service
public class AmadeusFlightSearch implements FlightSearch{

    static org.slf4j.Logger logger = LoggerFactory.getLogger("gds");

    static org.slf4j.Logger amadeusLogger = LoggerFactory.getLogger("amadeus");

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
        logger.debug("#####################SearchParameters: \n"+Json.toJson(searchParameters));
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
            logger.debug("Execution time in getting session:: " + duration/1000 + " seconds");//to be removed
//            serviceHandler.logIn();
            if (searchParameters.getBookingType() == BookingType.SEAMEN) {
                seamenReply = serviceHandler.searchAirlines(searchParameters, amadeusSessionWrapper);
//                logger.debug("#####################seamenReply: \n"+Json.toJson(seamenReply));
                
                searchParameters.setBookingType(BookingType.NON_MARINE);
                fareMasterPricerTravelBoardSearchReply = serviceHandler.searchAirlines(searchParameters, amadeusSessionWrapper);
//                logger.debug("fareMasterPricerTravelBoardSearchReply: \n"+Json.toJson(fareMasterPricerTravelBoardSearchReply));
                searchParameters.setBookingType(BookingType.SEAMEN);
//                XMLFileUtility.createXMLFile(seamenReply, "AmadeusSeamenSearchRes.xml");
                
//                amadeusLogger.debug("AmadeusSeamenSearchRes "+ new Date()+" ------->>"+ new XStream().toXML(seamenReply));
                amadeusLogger.debug("AmadeusSearchRes "+ new Date()+" ------->>"+ new XStream().toXML(fareMasterPricerTravelBoardSearchReply));
//                XMLFileUtility.createXMLFile(fareMasterPricerTravelBoardSearchReply, "AmadeusSearchRes.xml");
            } else {
                fareMasterPricerTravelBoardSearchReply = serviceHandler.searchAirlines(searchParameters, amadeusSessionWrapper);

//                XMLFileUtility.createXMLFile(fareMasterPricerTravelBoardSearchReply, "AmadeusSearchRes.xml");
                amadeusLogger.debug("AmadeusSearchRes "+ new Date()+" ------->>"+ new XStream().toXML(fareMasterPricerTravelBoardSearchReply));
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
        }finally {
            amadeusSessionManager.updateAmadeusSession(amadeusSessionWrapper);
        }

       // logger.debug("AmadeusFlightSearch search reponse at : " + new Date());
        FareMasterPricerTravelBoardSearchReply.ErrorMessage seamenErrorMessage = null;
        FareMasterPricerTravelBoardSearchReply.ErrorMessage errorMessage = fareMasterPricerTravelBoardSearchReply.getErrorMessage();
        if (seamenReply != null) {
            seamenErrorMessage = seamenReply.getErrorMessage();
            if(seamenErrorMessage != null)
                logger.debug("seamenErrorMessage :" + seamenErrorMessage.getErrorMessageText().getDescription() + "  officeId:"+ office.getOfficeId());
        }

        AirSolution airSolution = new AirSolution();
        if (errorMessage != null) {
        	logger.debug("#####################errorMessage is not null");
            String errorCode = errorMessage.getApplicationError().getApplicationErrorDetail().getError();
            if(!AmadeusConstants.NO_ITINERARY_ERROR_CODE.contains(errorCode)){
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
            airSolution.setNonSeamenHashMap(getFlightItineraryHashmap(fareMasterPricerTravelBoardSearchReply,office));
            //printHashmap(airSolution.getNonSeamenHashMap(), false);
            if (searchParameters.getBookingType() == BookingType.SEAMEN && seamenErrorMessage == null) {
                ///AirSolution seamenSolution = new AirSolution();
                ///seamenSolution = createAirSolutionFromRecommendation(seamenReply);
                ///airSolution.setSeamenHashMap(seamenSolution.getNonSeamenHashMap());
                airSolution.setSeamenHashMap(getFlightItineraryHashmap(seamenReply,office));
                if(Play.application().configuration().getBoolean("amadeus.DEBUG_SEARCH_LOG")) {
                    printHashmap(airSolution.getSeamenHashMap(), true);//to be removed
                }
                ///seamenSolution.setNonSeamenHashMap(null);
                //addSeamenFareToSolution(airSolution, seamenSolution);
            }
        }
        searchResponse.setAirSolution(airSolution);
        searchResponse.setProvider(provider());
        searchResponse.setFlightSearchOffice(office);
        return searchResponse;
    }

    //todo to be removed
    public static void printHashmap(ConcurrentHashMap<Integer, FlightItinerary> hashMap, boolean iSeaman){
        System.out.println("Is Seaman :"+ iSeaman + "  count:"+ hashMap.values().size() );
        logger.debug("Is Seaman :"+ iSeaman + "  count:"+ hashMap.values().size());
        boolean isMarine = false;
        if(iSeaman)
            isMarine = true;

        for (Map.Entry<Integer, FlightItinerary> entry : hashMap.entrySet()) {
            FlightItinerary value = entry.getValue();
            if(entry.getKey() == 1521758370 || entry.getKey() == 1521756448){
                //System.out.println("");
            }
            if(value.getSeamanPricingInformation() != null && value.getSeamanPricingInformation().getTotalPriceValue() != null){
                isMarine = true;
            }
            String v = ", " + isMarine+", "+ value.getPricingInformation().getPricingOfficeId() + ", "+ value.getPricingInformation().getTotalPriceValue() +
                    ", " + value.getJourneyList().get(0).getAirSegmentList().get(0).getCarrierCode() +", "+ value.getJourneyList().get(0).getAirSegmentList().get(0).getFlightNumber()+ ",  "+ value.getJourneyList().get(0).getTravelTimeStr();
            System.out.println(entry.getKey() + ",  " + v);
            //logger.debug(entry.getKey() + ",  " + v);
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

    private ConcurrentHashMap<Integer, FlightItinerary> getFlightItineraryHashmap(FareMasterPricerTravelBoardSearchReply fareMasterPricerTravelBoardSearchReply, FlightSearchOffice office) {
        ConcurrentHashMap<Integer, FlightItinerary> flightItineraryHashMap = new ConcurrentHashMap<>();
        try{
            String currency = fareMasterPricerTravelBoardSearchReply.getConversionRate().getConversionRateDetail().get(0).getCurrency();
            List<FareMasterPricerTravelBoardSearchReply.FlightIndex> flightIndexList = fareMasterPricerTravelBoardSearchReply.getFlightIndex();
            for (Recommendation recommendation : fareMasterPricerTravelBoardSearchReply.getRecommendation()) {
                for (ReferenceInfoType segmentRef : recommendation.getSegmentFlightRef()) {
                    FlightItinerary flightItinerary = new FlightItinerary();
                    flightItinerary.setPassportMandatory(false);
                    flightItinerary.setPricingInformation(getPricingInformation(recommendation));
                    flightItinerary.getPricingInformation().setGdsCurrency(currency);
                    flightItinerary.getPricingInformation().setPricingOfficeId(office.getOfficeId());
                    List<String> contextList = getAvailabilityCtx(segmentRef, recommendation.getSpecificRecDetails());
                    flightItinerary = createJourneyInformation(segmentRef, flightItinerary, flightIndexList, recommendation, contextList);
                    flightItinerary.getPricingInformation().setPaxFareDetailsList(createFareDetails(recommendation, flightItinerary.getJourneyList()));
                    //flightItinerary.setAmadeusOfficeId(office.getOfficeId());
                    flightItineraryHashMap.put(flightItinerary.hashCode(), flightItinerary);
                }
            }
            return flightItineraryHashMap;
        }catch (Exception e){
            logger.debug("error in getFlightItineraryHashmap :"+ e.getMessage());
        }
        return flightItineraryHashMap;
    }

    private FlightItinerary createJourneyInformation(ReferenceInfoType segmentRef, FlightItinerary flightItinerary, List<FlightIndex> flightIndexList, Recommendation recommendation, List<String> contextList){
        int flightIndexNumber = 0;
        int segmentIndex = 0;
        for(ReferencingDetailsType191583C referencingDetailsType : segmentRef.getReferencingDetail()) {
            //0 is for forward journey and refQualifier should be S for segment
            if (referencingDetailsType.getRefQualifier().equalsIgnoreCase("S") ) {
                Journey journey = new Journey();
                journey = setJourney(journey, flightIndexList.get(flightIndexNumber).getGroupOfFlights().get(referencingDetailsType.getRefNumber().intValue()-1),recommendation);
                if(contextList.size() > 0 ){
                    setContextInformation(contextList, journey, segmentIndex);
                }
                flightItinerary.getJourneyList().add(journey);
                flightItinerary.getNonSeamenJourneyList().add(journey);
                //flightItinerary.getJourneyList().add(setJourney(flightIndexNumber == 0 ? forwardJourney : returnJourney, flightIndexList.get(flightIndexNumber).getGroupOfFlights().get(referencingDetailsType.getRefNumber().intValue()-1)));
                //flightIndexNumber = ++flightIndexNumber % 2;
                ++flightIndexNumber;
            }


        }
        return flightItinerary;
    }


    public  void setContextInformation(List<String> contextList, Journey journey, int segmentIndex){
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

    private Journey setJourney(Journey journey,FlightIndex.GroupOfFlights groupOfFlight, Recommendation recommendation){
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
        for(FlightIndex.GroupOfFlights.FlightDetails flightDetails : groupOfFlight.getFlightDetails()){
            AirSegmentInformation airSegmentInformation = setSegmentInformation(flightDetails, fareBasis, validatingCarrierCode);
            if(airSegmentInformation.getToAirport().getAirportName() != null && airSegmentInformation.getFromAirport().getAirportName() != null) {
                journey.getAirSegmentList().add(airSegmentInformation);
                journey.setProvider("Amadeus");
            }
        }
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
    
    private AirSegmentInformation setSegmentInformation(FlightIndex.GroupOfFlights.FlightDetails flightDetails, String fareBasis,String validatingCarrierCode){
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

    private PricingInformation getPricingInformation(Recommendation recommendation) {
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
        for(Recommendation.PaxFareProduct paxFareProduct : recommendation.getPaxFareProduct()) {
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

    public static SimpleDateFormat searchFormat = new SimpleDateFormat("ddMMyy-kkmm");

    private AirSegmentInformation createSegment(TravelProductType flightInformation, String prevArrivalTime) {

        AirSegmentInformation airSegmentInformation = new AirSegmentInformation();
        airSegmentInformation.setCarrierCode(flightInformation.getCompanyId().getMarketingCarrier());
        if(flightInformation.getCompanyId().getOperatingCarrier() != null)
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


    private List<PAXFareDetails> createFareDetails(Recommendation recommendation, List<Journey> journeys){
        List<PAXFareDetails> paxFareDetailsList = new ArrayList<>();
        for(Recommendation.PaxFareProduct paxFareProduct :recommendation.getPaxFareProduct()){
            PAXFareDetails paxFareDetails = new PAXFareDetails();
            for(Recommendation.PaxFareProduct.FareDetails fareDetails :paxFareProduct.getFareDetails()){
                FareJourney fareJourney = new FareJourney();
                for(Recommendation.PaxFareProduct.FareDetails.GroupOfFares groupOfFares: fareDetails.getGroupOfFares()){
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

    public String getFareBasis(Recommendation.PaxFareProduct.FareDetails fareDetails){
        for(PaxFareProduct.FareDetails.GroupOfFares groupOfFares : fareDetails.getGroupOfFares()){
            return groupOfFares.getProductInformation().getFareProductDetail().getFareBasis();

        }
        return  null;
    }

    public List getAvailabilityCtx(ReferenceInfoType segmentRef, List<SpecificRecDetails> specificRecDetails){
        List<String> contextList = new ArrayList<>();
        for(ReferencingDetailsType191583C referencingDetailsType : segmentRef.getReferencingDetail()) {


            if(referencingDetailsType.getRefQualifier().equalsIgnoreCase("A")){
                BigInteger refNumber = referencingDetailsType.getRefNumber();
                for(SpecificRecDetails specificRecDetail : specificRecDetails){
                    if(refNumber.equals(specificRecDetail.getSpecificRecItem().getRefNumber())){
                        for(SpecificRecDetails.SpecificProductDetails specificProductDetails : specificRecDetail.getSpecificProductDetails()){
                            for(SpecificRecDetails.SpecificProductDetails.FareContextDetails fareContextDetails : specificProductDetails.getFareContextDetails()){
                                for(SpecificRecDetails.SpecificProductDetails.FareContextDetails.CnxContextDetails cnxContextDetails : fareContextDetails.getCnxContextDetails()){
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

}
