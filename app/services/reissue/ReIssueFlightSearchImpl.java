package services.reissue;

import com.amadeus.xml.fatcer_13_1_1a.LocationTypeI;
import com.amadeus.xml.fatcer_13_1_1a.TravelFlightInformationType;
import com.amadeus.xml.fmtctq_18_2_1a.DateAndTimeInformationTypeI;
import com.amadeus.xml.fmtctr_18_2_1a.*;
import com.compassites.GDSWrapper.amadeus.ServiceHandler;
import com.compassites.constants.AmadeusConstants;
import com.compassites.exceptions.IncompleteDetailsMessage;
import com.compassites.exceptions.RetryException;
import com.compassites.model.*;
import com.compassites.model.amadeus.reissue.ReIssuePricingInformation;
import com.sun.xml.ws.client.ClientTransportException;
import com.sun.xml.ws.fault.ServerSOAPFaultException;
import dto.reissue.ReIssueTicketRequest;
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
import org.springframework.stereotype.Component;
import play.libs.Json;
import services.AmadeusSourceOfficeService;
import services.RetryOnFailure;
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
import java.util.*;
import java.util.concurrent.*;

@Component
public class ReIssueFlightSearchImpl implements ReIssueFlightSearch {

    static Logger logger = LoggerFactory.getLogger("gds");

    private static final Logger amadeusLogger = LoggerFactory.getLogger("amadeus");

    @Autowired
    private AmadeusSessionManager amadeusSessionManager;

    @Autowired
    private AmadeusSourceOfficeService amadeusSourceOfficeService;

    @Override
    public List<FlightSearchOffice> getOfficeList() {
        return amadeusSourceOfficeService.getAllOffices();
    }

    @Override
    public String provider() {
        return "Amadeus";
    }

    //Without Redis
    @Override
    public SearchResponse reIssueFlightSearch(ReIssueTicketRequest reIssueTicketRequest, TravelFlightInformationType allowedCarriers, AmadeusSessionWrapper amadeusSessionWrapper) {

        ReIssueFlightSearch reissueFlightSearch = this;
        List<FlightSearchOffice> officeList = reissueFlightSearch.getOfficeList();

        int maxThreads = (officeList != null) ? officeList.size() : 0;
        int queueSize = 10;

        ThreadPoolExecutor newExecutor = new ThreadPoolExecutor(maxThreads, maxThreads, Long.MAX_VALUE, TimeUnit.NANOSECONDS, new ArrayBlockingQueue<Runnable>(queueSize, true));
        List<Future<SearchResponse>> futureSearchResponseList = new ArrayList<>();
        List<ErrorMessage> errorMessageList = new ArrayList<>();
        ConcurrentHashMap<Integer, FlightItinerary> hashMap = new ConcurrentHashMap<>();

        try {
            for (FlightSearchOffice office : reissueFlightSearch.getOfficeList()) {

                logger.debug("**** Office: " + Json.stringify(Json.toJson(office)));
                if (!office.getOfficeId().equalsIgnoreCase("BOMAK38SN")) {

                    futureSearchResponseList.add(newExecutor.submit(new Callable<SearchResponse>() {
                        public SearchResponse call() throws Exception {

                            return reIssueSearch(reIssueTicketRequest, allowedCarriers, office, amadeusSessionWrapper);
                        }
                    }));
                }
            }
        } catch (Exception e) {
            logger.debug("Error In reissue search {}", e.getMessage(), e);
            e.printStackTrace();
        }

        int counter = 0;
        boolean loop = true;
        int searchResponseListSize = futureSearchResponseList.size();
        int exceptionCounter = 0;

        SearchResponse searchResponseFinal = null;
        while (loop) {
            ListIterator<Future<SearchResponse>> listIterator = futureSearchResponseList.listIterator();
            while (listIterator.hasNext()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Future<SearchResponse> future = listIterator.next();
                SearchResponse searchResponse = null;
                if (future.isDone()) {
                    listIterator.remove();
                    counter++;
                    try {
                        searchResponse = future.get();
                    } catch (RetryException retryOnFailure) {
                        exceptionCounter++;
                        if (exceptionCounter == officeList.size()) {
                            logger.debug("All providers gave error");
                        }
                        logger.error("retrialError in FlightSearchWrapper : ", retryOnFailure);
                        ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage("retrialError", ErrorMessage.ErrorType.ERROR, "Application");
                        errorMessageList.add(errorMessage);
                    } catch (Exception e) {
                        logger.error("Exception in FlightSearchWrapper : ", e);
                        ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage("partialResults", ErrorMessage.ErrorType.WARNING, "Application");
                        errorMessageList.add(errorMessage);
                        e.printStackTrace();
                    }


                    if (searchResponse != null && searchResponse.getErrorMessageList().isEmpty()) {
                        logger.debug("Received Response " + counter + "  | from : " + searchResponse.getProvider() + "   | office:" + searchResponse.getFlightSearchOffice().getOfficeId() + "  | Seaman size: " + searchResponse.getAirSolution().getSeamenHashMap().size() + " | normal size:" + searchResponse.getAirSolution().getNonSeamenHashMap().size());
                        searchResponseFinal = new SearchResponse();
                        searchResponseFinal.setFlightSearchOffice(searchResponse.getFlightSearchOffice());
                        searchResponseFinal.setProvider(searchResponse.getProvider());
                        logger.debug("counter :" + counter + "Search Response FligthItinary Size: " + searchResponse.getAirSolution().getFlightItineraryList().size());
                        logger.debug("\n\n----------- before MergeResults " + counter + "--------" + searchResponse.getFlightSearchOffice().getOfficeId());
                        mergeResults(hashMap, searchResponse);
                        logger.debug("----------- After MergeResults " + counter + "--------" + searchResponse.getFlightSearchOffice().getOfficeId());

                        errorMessageList.addAll(searchResponse.getErrorMessageList());

                        AirSolution airSolution = new AirSolution();
                        airSolution.setReIssueSearch(true);
                        airSolution.setFlightItineraryList(new ArrayList<FlightItinerary>(hashMap.values()));
                        searchResponseFinal.setAirSolution(airSolution);
                        searchResponseFinal.setReIssueSearch(true);
                        searchResponseFinal.getErrorMessageList().addAll(searchResponse.getErrorMessageList());
                        searchResponseFinal.setErrorMessageList(errorMessageList);

                        logger.debug("Added response to final hashmap" + counter + "  | from:" + searchResponseFinal.getProvider() + "  | office:" + searchResponseFinal.getFlightSearchOffice().getOfficeId() + "  | hashmap size: " + searchResponseFinal.getAirSolution().getFlightItineraryList().size() + " | search:" + searchResponse.getAirSolution().getNonSeamenHashMap().size() + " + " + searchResponse.getAirSolution().getNonSeamenHashMap().size());
                    } else {
                        logger.debug("Received Response " + counter + " Null");
                    }

                }
            }

            newExecutor.shutdown();
            if (counter == searchResponseListSize) {
                loop = false;
                if (hashMap.isEmpty() && !errorMessageList.isEmpty()) {
                    searchResponseFinal.setErrorMessageList(errorMessageList);
                }
            }
        }

        logger.debug("=================================>\n" + "=================================>\n" + String.format("[monitor] [%d/%d] Active: %d, Completed: %d, Task: %d, isShutdown: %s, isTerminated: %s", newExecutor.getPoolSize(), newExecutor.getCorePoolSize(), newExecutor.getActiveCount(), newExecutor.getCompletedTaskCount(), newExecutor.getTaskCount(), newExecutor.isShutdown(), newExecutor.isTerminated()));

        return searchResponseFinal;

    }


    @RetryOnFailure(attempts = 2, delay = 2000, exception = RetryException.class)
    public SearchResponse reIssueSearch(ReIssueTicketRequest reIssueTicketRequest, TravelFlightInformationType allowedCarriers, FlightSearchOffice office, AmadeusSessionWrapper amadeusSessionWrapper) throws Exception {
        logger.debug("#####################AmadeusReIssueFlightSearch started  : ");
        logger.debug("#####################ReIssueTicketRequest: \n" + Json.toJson(reIssueTicketRequest));
        SearchResponse searchResponse = new SearchResponse();
        ServiceHandler serviceHandler = null;
        searchResponse.setProvider("Amadeus");
        searchResponse.setFlightSearchOffice(office);
        TicketATCShopperMasterPricerTravelBoardSearchReply seamenReply = null;
        TicketATCShopperMasterPricerTravelBoardSearchReply nonSeamanReply = null;

        try {
            serviceHandler = new ServiceHandler();
            long startTime = System.currentTimeMillis();
            amadeusSessionWrapper = amadeusSessionManager.getSession(office);
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

//            logger.debug("...................................Amadeus Search Session used: " + Json.toJson(amadeusSessionWrapper.getmSession().value));
            logger.debug("Execution time in getting session:: " + duration / 1000 + " seconds");

            if (reIssueTicketRequest.isSeaman()) {
                seamenReply = serviceHandler.reIssueATCAirlineSearch(reIssueTicketRequest, allowedCarriers, amadeusSessionWrapper);
            } else {
                nonSeamanReply = serviceHandler.reIssueATCAirlineSearch(reIssueTicketRequest, allowedCarriers, amadeusSessionWrapper);
            }

        } catch (ServerSOAPFaultException soapFaultException) {
            soapFaultException.printStackTrace();
            throw new IncompleteDetailsMessage(soapFaultException.getMessage(), soapFaultException.getCause());
        } catch (ClientTransportException clientTransportException) {
            ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage("partialResults", ErrorMessage.ErrorType.ERROR, "Amadeus");
            searchResponse.getErrorMessageList().add(errorMessage);
            return searchResponse;
        } catch (Exception e) {
            e.printStackTrace();

            ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage("partialResults", ErrorMessage.ErrorType.ERROR, "Amadeus");
            searchResponse.getErrorMessageList().add(errorMessage);
            return searchResponse;
        } finally {
            if (amadeusSessionWrapper != null) {
                amadeusSessionManager.updateAmadeusSession(amadeusSessionWrapper);
            }
        }

        TicketATCShopperMasterPricerTravelBoardSearchReply.ErrorMessage seamenErrorMessage = null;

        TicketATCShopperMasterPricerTravelBoardSearchReply.ErrorMessage nonSeamanErrorMessage = null;
        if (!reIssueTicketRequest.isSeaman() && nonSeamanReply != null) {
            nonSeamanErrorMessage = nonSeamanReply.getErrorMessage();
        }

        if (reIssueTicketRequest.isSeaman() && seamenReply != null) {
            seamenErrorMessage = seamenReply.getErrorMessage();
            if (seamenErrorMessage != null)
                logger.debug("seamenErrorMessage :" + seamenErrorMessage.getErrorMessageText().getDescription() + "  officeId:" + office.getOfficeId());
        }

        AirSolution airSolution = new AirSolution();
        if (nonSeamanErrorMessage != null) {
            logger.debug("#####################errorMessage is not null");
            String errorCode = nonSeamanErrorMessage.getApplicationError().getApplicationErrorDetail().getError();
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
            logger.debug("#####################errorMessage is null");

            if (reIssueTicketRequest.isSeaman()) {
                airSolution.setSeamenHashMap(getFlightItineraryHashmap(reIssueTicketRequest, seamenReply, office));
            } else {
                airSolution.setNonSeamenHashMap(getFlightItineraryHashmap(reIssueTicketRequest, nonSeamanReply, office));
            }
        }

        searchResponse.setAirSolution(airSolution);
        searchResponse.setProvider(provider());
        searchResponse.setFlightSearchOffice(office);
        return searchResponse;
    }

    //TODO: Will add MNR search and baggage here
    private ConcurrentHashMap<Integer, FlightItinerary> getFlightItineraryHashmap(ReIssueTicketRequest reIssueTicketRequest, TicketATCShopperMasterPricerTravelBoardSearchReply TicketATCShopperMasterPricerTravelBoardSearchReply, FlightSearchOffice office) {
        ConcurrentHashMap<Integer, FlightItinerary> flightItineraryHashMap = new ConcurrentHashMap<>();
        try {
            String currency = TicketATCShopperMasterPricerTravelBoardSearchReply.getConversionRate().getConversionRateDetail().get(0).getCurrency();
            String officeId = office.getOfficeId();
            List<TicketATCShopperMasterPricerTravelBoardSearchReply.FlightIndex> flightIndexList = TicketATCShopperMasterPricerTravelBoardSearchReply.getFlightIndex();
            for (TicketATCShopperMasterPricerTravelBoardSearchReply.Recommendation recommendation : TicketATCShopperMasterPricerTravelBoardSearchReply.getRecommendation()) {
                for (ReferenceInfoType segmentRef : recommendation.getSegmentFlightRef()) {
                    FlightItinerary flightItinerary = new FlightItinerary();
                    //Journey Related Information
                    flightItinerary.setPassportMandatory(false);
                    flightItinerary = createJourneyInformation(reIssueTicketRequest, segmentRef, flightItinerary, flightIndexList, recommendation);

                    //Pricing Related Information
                    if (reIssueTicketRequest.isSeaman()) {
                        flightItinerary.setPriceOnlyPTC(true);
                    }
                    flightItinerary.setReIssuePricingInformation(getReIssuePricingInformation(recommendation, officeId, currency));

                    flightItineraryHashMap.put(flightItinerary.hashCode(), flightItinerary);
                }
            }
            return flightItineraryHashMap;
        } catch (Exception e) {
            logger.debug("error in getFlightItineraryHashmap :" + e.getMessage());
        }
        return flightItineraryHashMap;
    }


    private FlightItinerary createJourneyInformation(ReIssueTicketRequest reIssueTicketRequest, ReferenceInfoType segmentRef, FlightItinerary flightItinerary, List<TicketATCShopperMasterPricerTravelBoardSearchReply.FlightIndex> flightIndexList, TicketATCShopperMasterPricerTravelBoardSearchReply.Recommendation recommendation) {
        List<String> contextList = getAvailabilityCtx(segmentRef, recommendation.getSpecificRecDetails());
        int flightIndexNumber = 0;
        int segmentIndex = 0;
        for (ReferencingDetailsType191583C referencingDetailsType : segmentRef.getReferencingDetail()) {
            //0 is for forward journey and refQualifier should be S for segment
            if (referencingDetailsType.getRefQualifier().equalsIgnoreCase("S")) {
                Journey journey = new Journey();
                journey = setJourney(journey, flightIndexList.get(flightIndexNumber).getGroupOfFlights().get(referencingDetailsType.getRefNumber().intValue() - 1), recommendation);
                if (!contextList.isEmpty()) {
                    setContextInformation(contextList, journey, segmentIndex);
                }
                if (reIssueTicketRequest.isSeaman()) {
                    flightItinerary.getJourneyList().add(journey);
                } else {
                    flightItinerary.getNonSeamenJourneyList().add(journey);
                }
                ++flightIndexNumber;
            }
        }
        return flightItinerary;
    }

    public List<String> getAvailabilityCtx(ReferenceInfoType segmentRef, List<TicketATCShopperMasterPricerTravelBoardSearchReply.Recommendation.SpecificRecDetails> specificRecDetails) {
        List<String> contextList = new ArrayList<>();


        Map<BigInteger, TicketATCShopperMasterPricerTravelBoardSearchReply.Recommendation.SpecificRecDetails> recDetailsMap = new HashMap<>();
        for (TicketATCShopperMasterPricerTravelBoardSearchReply.Recommendation.SpecificRecDetails specificRecDetail : specificRecDetails) {
            recDetailsMap.put(specificRecDetail.getSpecificRecItem().getRefNumber(), specificRecDetail);
        }

        for (ReferencingDetailsType191583C referencingDetailsType : segmentRef.getReferencingDetail()) {
            if ("A".equalsIgnoreCase(referencingDetailsType.getRefQualifier())) {
                BigInteger refNumber = referencingDetailsType.getRefNumber();
                TicketATCShopperMasterPricerTravelBoardSearchReply.Recommendation.SpecificRecDetails specificRecDetail = recDetailsMap.get(refNumber);

                if (specificRecDetail != null) {
                    for (TicketATCShopperMasterPricerTravelBoardSearchReply.Recommendation.SpecificRecDetails.SpecificProductDetails specificProductDetails : specificRecDetail.getSpecificProductDetails()) {
                        for (TicketATCShopperMasterPricerTravelBoardSearchReply.Recommendation.SpecificRecDetails.SpecificProductDetails.FareContextDetails fareContextDetails : specificProductDetails.getFareContextDetails()) {
                            for (TicketATCShopperMasterPricerTravelBoardSearchReply.Recommendation.SpecificRecDetails.SpecificProductDetails.FareContextDetails.CnxContextDetails cnxContextDetails : fareContextDetails.getCnxContextDetails()) {
                                contextList.addAll(cnxContextDetails.getFareCnxInfo().getContextDetails().getAvailabilityCnxType());
                            }
                        }
                    }
                }
            }
        }

        return contextList;
    }

    public void setContextInformation(List<String> contextList, Journey journey, int segmentIndex) {
        for (AirSegmentInformation airSegmentInformation : journey.getAirSegmentList()) {
            airSegmentInformation.setContextType(contextList.get(segmentIndex));
            segmentIndex = segmentIndex + 1;
        }
    }

    private Journey setJourney(Journey journey, TicketATCShopperMasterPricerTravelBoardSearchReply.FlightIndex.GroupOfFlights groupOfFlight, TicketATCShopperMasterPricerTravelBoardSearchReply.Recommendation recommendation) {

        //Number of Stops set here
        journey.setNoOfStops(groupOfFlight.getFlightDetails().size() - 1);

        //Travel Time set here
        for (ProposedSegmentDetailsType proposedSegmentDetailsType : groupOfFlight.getPropFlightGrDetail().getFlightProposal()) {
            if (proposedSegmentDetailsType.getUnitQualifier() != null && proposedSegmentDetailsType.getUnitQualifier().equals("EFT")) {
                journey.setTravelTime(setTravelDuraion(proposedSegmentDetailsType.getRef()));
            }
        }

        //FareBasis set here
        String fareBasis = getFareBasis(recommendation.getPaxFareProduct().get(0).getFareDetails().get(0));

        //Segment information set here
        String validatingCarrierCode = null;
        if (recommendation.getPaxFareProduct().get(0).getPaxFareDetail().getCodeShareDetails().get(0).getTransportStageQualifier().equals("V")) {
            validatingCarrierCode = recommendation.getPaxFareProduct().get(0).getPaxFareDetail().getCodeShareDetails().get(0).getCompany();
        }
        for (TicketATCShopperMasterPricerTravelBoardSearchReply.FlightIndex.GroupOfFlights.FlightDetails flightDetails : groupOfFlight.getFlightDetails()) {
            AirSegmentInformation airSegmentInformation = setSegmentInformation(flightDetails, fareBasis, validatingCarrierCode);
            if (airSegmentInformation.getToAirport().getAirportName() != null && airSegmentInformation.getFromAirport().getAirportName() != null) {
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
                    arrivalTime = dateFormat.parse(airSegments.get(i - 1).getArrivalTime()).getTime();

                    Long departureTime = dateFormat.parse(airSegments.get(i).getDepartureTime()).getTime();
                    Long transit = departureTime - arrivalTime;
                    airSegments.get(i - 1).setConnectionTime(Integer.valueOf((int) (transit / 60000)));
                } catch (ParseException e) {
                    logger.debug("Error generating connectionTime in ");
                    e.printStackTrace();
                }
            }
        }
    }

    private AirSegmentInformation setSegmentInformation(TicketATCShopperMasterPricerTravelBoardSearchReply.FlightIndex.GroupOfFlights.FlightDetails flightDetails, String fareBasis, String validatingCarrierCode) {
        AirSegmentInformation airSegmentInformation = new AirSegmentInformation();
        TravelProductType flightInformation = flightDetails.getFlightInformation();

        // Cache frequently accessed values
        CompanyIdentificationType companyId = flightInformation.getCompanyId();

        airSegmentInformation.setCarrierCode(companyId.getMarketingCarrier());
        String operatingCarrier = companyId.getOperatingCarrier();
        if (operatingCarrier != null) {
            airSegmentInformation.setOperatingCarrierCode(operatingCarrier);
        }
        airSegmentInformation.setFlightNumber(flightInformation.getFlightOrtrainNumber());
        airSegmentInformation.setEquipment(flightInformation.getProductDetail().getEquipmentType());
        airSegmentInformation.setValidatingCarrierCode(validatingCarrierCode);

        List<LocationIdentificationDetailsType> locations = flightInformation.getLocation();
        ProductDateTimeType productDateTime = flightInformation.getProductDateTime();

        airSegmentInformation.setFromTerminal(locations.get(0).getTerminal());
        airSegmentInformation.setToTerminal(locations.get(1).getTerminal());
        airSegmentInformation.setToDate(productDateTime.getDateOfDeparture());
        airSegmentInformation.setFromDate(productDateTime.getDateOfArrival());
        airSegmentInformation.setToLocation(locations.get(1).getLocationId());
        airSegmentInformation.setFromLocation(locations.get(0).getLocationId());

        //Airports
        Airport fromAirport = Airport.getAirportByIataCode(airSegmentInformation.getFromLocation());
        Airport toAirport = Airport.getAirportByIataCode(airSegmentInformation.getToLocation());

        //DateTimeFormatter and DateTimeZone objects
        final String DATE_FORMAT = "ddMMyyHHmm";
        DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern(DATE_FORMAT);

        // Departure DateTime parsing
        DateTimeZone fromTimeZone = DateTimeZone.forID(fromAirport.getTime_zone());
        DateTime departureDate = dateTimeFormatter.withZone(fromTimeZone).parseDateTime(productDateTime.getDateOfDeparture() + productDateTime.getTimeOfDeparture());

        // Arrival DateTime parsing
        DateTimeZone toTimeZone = DateTimeZone.forID(toAirport.getTime_zone());
        DateTime arrivalDate = dateTimeFormatter.withZone(toTimeZone).parseDateTime(productDateTime.getDateOfArrival() + productDateTime.getTimeOfArrival());

        airSegmentInformation.setDepartureDate(departureDate.toDate());
        airSegmentInformation.setDepartureTime(departureDate.toString());
        airSegmentInformation.setArrivalTime(arrivalDate.toString());
        airSegmentInformation.setArrivalDate(arrivalDate.toDate());

        airSegmentInformation.setFromAirport(fromAirport);
        airSegmentInformation.setToAirport(toAirport);

        // Calculate the travel time in minutes
        Minutes travelTime = Minutes.minutesBetween(departureDate, arrivalDate);
        airSegmentInformation.setTravelTime(String.valueOf(travelTime.getMinutes()));

        airSegmentInformation.setFareBasis(fareBasis);

        // Airline information
        if (companyId != null && companyId.getMarketingCarrier() != null) {
            airSegmentInformation.setAirline(Airline.getAirlineByIataCode(companyId.getMarketingCarrier()));
            if (operatingCarrier != null) {
                airSegmentInformation.setOperatingAirline(Airline.getAirlineByIataCode(operatingCarrier));
            }
        }

        // Handle hopping flights
        List<HoppingFlightInformation> hoppingFlightInformations = null;
        if (flightDetails.getTechnicalStop() != null) {
            SimpleDateFormat dateParser = new SimpleDateFormat("ddMMyy");
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy");

            hoppingFlightInformations = new ArrayList<>();
            for (DateAndTimeInformationType dateAndTimeInformationType : flightDetails.getTechnicalStop()) {
                HoppingFlightInformation hop = new HoppingFlightInformation();

                List<DateAndTimeDetailsType> stopDetails = dateAndTimeInformationType.getStopDetails();

                hop.setLocation(stopDetails.get(0).getLocationId());
                hop.setStartTime(new StringBuilder(stopDetails.get(0).getFirstTime()).insert(2, ":").toString());

                // Date parsing and formatting
                try {
                    Date startDate = dateParser.parse(stopDetails.get(0).getDate());
                    Date endDate = dateParser.parse(stopDetails.get(1).getDate());
                    hop.setStartDate(dateFormat.format(startDate));
                    hop.setEndDate(dateFormat.format(endDate));
                } catch (ParseException e) {
                    e.printStackTrace();
                }

                hop.setEndTime(new StringBuilder(stopDetails.get(1).getFirstTime()).insert(2, ":").toString());

                hoppingFlightInformations.add(hop);
            }
            airSegmentInformation.setHoppingFlightInformations(hoppingFlightInformations);
        }

        return airSegmentInformation;
    }


    public String getFareBasis(TicketATCShopperMasterPricerTravelBoardSearchReply.Recommendation.PaxFareProduct.FareDetails fareDetails) {
        for (TicketATCShopperMasterPricerTravelBoardSearchReply.Recommendation.PaxFareProduct.FareDetails.GroupOfFares groupOfFares : fareDetails.getGroupOfFares()) {
            return groupOfFares.getProductInformation().getFareProductDetail().getFareBasis();
        }
        return null;
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

    private List<PAXFareDetails> createFareDetails(TicketATCShopperMasterPricerTravelBoardSearchReply.Recommendation recommendation) {
        List<PAXFareDetails> paxFareDetailsList = new ArrayList<>();

        for (TicketATCShopperMasterPricerTravelBoardSearchReply.Recommendation.PaxFareProduct paxFareProduct : recommendation.getPaxFareProduct()) {
            PAXFareDetails paxFareDetails = new PAXFareDetails();

            for (TicketATCShopperMasterPricerTravelBoardSearchReply.Recommendation.PaxFareProduct.FareDetails fareDetails : paxFareProduct.getFareDetails()) {
                FareJourney fareJourney = new FareJourney();

                PassengerTypeCode passengerTypeCode = PassengerTypeCode.valueOf(fareDetails.getGroupOfFares().get(0).getProductInformation().getFareProductDetail().getPassengerType());
                paxFareDetails.setPassengerTypeCode(passengerTypeCode);

                for (TicketATCShopperMasterPricerTravelBoardSearchReply.Recommendation.PaxFareProduct.FareDetails.GroupOfFares groupOfFares : fareDetails.getGroupOfFares()) {
                    FareSegment fareSegment = new FareSegment();
                    fareSegment.setBookingClass(groupOfFares.getProductInformation().getCabinProduct().getRbd());
                    fareSegment.setCabinClass(groupOfFares.getProductInformation().getCabinProduct().getCabin());
                    fareSegment.setFareBasis(groupOfFares.getProductInformation().getFareProductDetail().getFareBasis());

                    fareJourney.getFareSegmentList().add(fareSegment);
                }
                paxFareDetails.getFareJourneyList().add(fareJourney);
            }
            paxFareDetailsList.add(paxFareDetails);
        }

        return paxFareDetailsList;
    }


    private ReIssuePricingInformation getReIssuePricingInformation(TicketATCShopperMasterPricerTravelBoardSearchReply.Recommendation recommendation, String officeId, String gdsCurrency) {
        ReIssuePricingInformation reIssuePricingInformation = new ReIssuePricingInformation();

        reIssuePricingInformation.setPricingOfficeId(officeId);
        reIssuePricingInformation.setGdsCurrency(gdsCurrency);

        reIssuePricingInformation.setProvider("Amadeus");
        List<MonetaryInformationDetailsType> monetaryDetails = recommendation.getRecPriceInfo().getMonetaryDetail();
        BigDecimal totalFare = monetaryDetails.get(0).getAmount();
        BigDecimal totalTax = monetaryDetails.get(1).getAmount();
        reIssuePricingInformation.setBasePrice(totalFare.subtract(totalTax));
        reIssuePricingInformation.setTax(totalTax);
        reIssuePricingInformation.setTotalPrice(totalFare);
        reIssuePricingInformation.setTotalPriceValue(totalFare);

        for (MonetaryInformationDetailsType monetaryInformationDetailsType : monetaryDetails.subList(2, monetaryDetails.size())) {
            switch (monetaryInformationDetailsType.getAmountType().toUpperCase()) {
                case "D":
                    reIssuePricingInformation.setGrandTotalDifferenceAmountD(monetaryInformationDetailsType.getAmount());
                    break;
                case "B":
                    reIssuePricingInformation.setTotalTaxDifferenceB(monetaryInformationDetailsType.getAmount());
                    break;
                case "P":
                    reIssuePricingInformation.setTotalPenaltyAmountP(monetaryInformationDetailsType.getAmount());
                    break;
                case "A":
                    reIssuePricingInformation.setTotalAdditionalCollectionA(monetaryInformationDetailsType.getAmount());
                    break;
                case "C":
                    reIssuePricingInformation.setMcoResidualValueC(monetaryInformationDetailsType.getAmount());
                    break;
                case "M":
                    reIssuePricingInformation.setGrandTotalValueM(monetaryInformationDetailsType.getAmount());
                    break;
                case "N":
                    reIssuePricingInformation.setNewTaxN(monetaryInformationDetailsType.getAmount());
                    break;
            }
        }

        List<PassengerTax> passengerTaxes = new ArrayList<>();
        for (TicketATCShopperMasterPricerTravelBoardSearchReply.Recommendation.PaxFareProduct paxFareProduct : recommendation.getPaxFareProduct()) {
            PassengerTax passengerTax = new PassengerTax();
            int paxCount = paxFareProduct.getPaxReference().get(0).getTraveller().size();
            String paxType = paxFareProduct.getPaxReference().get(0).getPtc().get(0);
            PricingTicketingSubsequentType216944S paxFareDetail = paxFareProduct.getPaxFareDetail();
            BigDecimal amount = paxFareDetail.getTotalFareAmount();
            BigDecimal tax = paxFareDetail.getTotalTaxAmount();
            BigDecimal baseFare = amount.subtract(tax);
            switch (paxType.toUpperCase()) {
                case "ADT":
                case "SEA":
                    reIssuePricingInformation.setAdtBasePrice(baseFare);
                    reIssuePricingInformation.setAdtTotalPrice(amount);
                    passengerTax.setPassengerType("ADT");
                    passengerTax.setTotalTax(tax);
                    passengerTax.setPassengerCount(paxCount);
                    break;
                case "CHD":
                    reIssuePricingInformation.setChdBasePrice(baseFare);
                    reIssuePricingInformation.setChdTotalPrice(amount);
                    passengerTax.setPassengerType("CHD");
                    passengerTax.setTotalTax(tax);
                    passengerTax.setPassengerCount(paxCount);
                    break;
                case "INF":
                    reIssuePricingInformation.setInfBasePrice(baseFare);
                    reIssuePricingInformation.setInfTotalPrice(amount);
                    passengerTax.setPassengerType("INF");
                    passengerTax.setTotalTax(tax);
                    passengerTax.setPassengerCount(paxCount);
                    break;
            }
            passengerTaxes.add(passengerTax);
        }
        reIssuePricingInformation.setPassengerTaxes(passengerTaxes);

        reIssuePricingInformation.setPaxFareDetailsList(createFareDetails(recommendation));

        return reIssuePricingInformation;
    }

    private void mergeResults(ConcurrentHashMap<Integer, FlightItinerary> allFlightItineraries, SearchResponse searchResponse) {
        try {
            AirSolution airSolution = searchResponse.getAirSolution();
            if (airSolution != null) {
                if (airSolution.getNonSeamenHashMap() != null) {
                    allFlightItineraries.putAll(airSolution.getNonSeamenHashMap());
                }
                if (airSolution.getSeamenHashMap() != null) {
                    allFlightItineraries.putAll(airSolution.getSeamenHashMap());
                }
            }
        } catch (Exception e) {
            logger.error("MergeResults:: ex:{}", e.getMessage(), e);
        }
    }


    //    public SearchResponse reIssueFlightSearch(ReIssueTicketRequest reIssueTicketRequest, TravelFlightInformationType allowedCarriers, AmadeusSessionWrapper amadeusSessionWrapper) {
//        List<FlightSearchOffice> officeList = getOfficeList();
//        ConcurrentHashMap<Integer, FlightItinerary> hashMap = new ConcurrentHashMap<>();
//        List<ErrorMessage> errorMessages = new CopyOnWriteArrayList<>();
//
//        int maxThreads = officeList != null ? officeList.size() : 0;
//        int queueSize = 10;
//        ThreadPoolExecutor executor = new ThreadPoolExecutor(
//                maxThreads,
//                maxThreads,
//                Long.MAX_VALUE,
//                TimeUnit.NANOSECONDS,
//                new ArrayBlockingQueue<>(queueSize, true)
//        );
//
//        List<Future<SearchResponse>> futureResponses = new ArrayList<>();
//
//
//        for (FlightSearchOffice office : officeList) {
//            futureResponses.add(executor.submit(() -> {
//                try {
//                    return reIssueSearch(reIssueTicketRequest, allowedCarriers, office, amadeusSessionWrapper);
//                } catch (Exception e) {
//                    logger.error("Error during flight search for office " + office.getOfficeId(), e);
//                    errorMessages.add(ErrorMessageHelper.createErrorMessage("partialResults", ErrorMessage.ErrorType.WARNING, "Application"));
//                    return null;
//                }
//            }));
//        }
//
//
//        for (Future<SearchResponse> future : futureResponses) {
//            try {
//                SearchResponse searchResponse = future.get();
//                if (searchResponse != null) {
//                    if (searchResponse.getErrorMessageList().isEmpty()) {
//                        mergeResults(hashMap, searchResponse);
//                    } else {
//                        errorMessages.addAll(searchResponse.getErrorMessageList());
//                    }
//                }
//            } catch (InterruptedException | ExecutionException e) {
//                logger.error("Exception in FlightSearchWrapper: ", e);
//            }
//        }
//
//        executor.shutdown();
//
//        SearchResponse searchResponseFinal = new SearchResponse();
//        if (!hashMap.isEmpty() || !errorMessages.isEmpty()) {
//            AirSolution airSolution = new AirSolution();
//            airSolution.setReIssueSearch(true);
//            airSolution.setFlightItineraryList(new ArrayList<>(hashMap.values()));
//            searchResponseFinal.setAirSolution(airSolution);
//        }
//
//        if(!errorMessages.isEmpty()){
//            searchResponseFinal.setErrorMessageList(errorMessages);
//        }
//
//        logger.debug("Completed all flight searches for reissue");
//
//        logger.debug("=================================>\n" +
//                String.format("[monitor] [%d/%d] Active: %d, Completed: %d, Task: %d, isShutdown: %s, isTerminated: %s",
//                        executor.getPoolSize(),
//                        executor.getCorePoolSize(),
//                        executor.getActiveCount(),
//                        executor.getCompletedTaskCount(),
//                        executor.getTaskCount(),
//                        executor.isShutdown(),
//                        executor.isTerminated()));
//
//        return searchResponseFinal;
//    }


}
