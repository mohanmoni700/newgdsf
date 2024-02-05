package services;

import com.amadeus.xml.pnracc_11_3_1a.PNRReply;
import com.amadeus.xml.pnrxcl_11_3_1a.PNRCancel;
import com.amadeus.xml.tautcr_04_1_1a.TicketCreateTSTFromPricingReply;
import com.amadeus.xml.tpcbrr_12_4_1a.FarePricePNRWithBookingClassReply;
import com.amadeus.xml.tpcbrr_12_4_1a.FarePricePNRWithBookingClassReply.FareList;
import com.amadeus.xml.ttktir_09_1_1a.DocIssuanceIssueTicketReply;
import com.amadeus.xml.ws._2009._01.wbs_session_2_0.Session;
import com.compassites.GDSWrapper.amadeus.PNRAddMultiElementsh;
import com.compassites.GDSWrapper.amadeus.ServiceHandler;
import com.compassites.constants.AmadeusConstants;
import com.compassites.model.*;
import com.compassites.model.traveller.TravellerMasterInfo;
import com.thoughtworks.xstream.XStream;
import models.AmadeusSessionWrapper;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.PeriodType;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import play.Configuration;
import play.Play;
import utils.AmadeusBookingHelper;
import utils.AmadeusHelper;
import utils.AmadeusSessionManager;
import utils.ErrorMessageHelper;

import java.math.BigDecimal;
import java.util.*;

import static services.AmadeusBookingServiceImpl.getNumberOfTST;
/*
*
 * Created by yaseen on 19-01-2016.*/

@Service
public class AmadeusIssuanceServiceImpl {

    static org.slf4j.Logger amadeusLogger = LoggerFactory.getLogger("amadeus");

    static org.slf4j.Logger logger = LoggerFactory.getLogger("gds");

    private AmadeusSessionManager amadeusSessionManager;

    @Autowired
    private ServiceHandler serviceHandler;

    @Autowired
    AmadeusBookingServiceImpl amadeusBookingService;

    @Autowired
    private AmadeusSourceOfficeService amadeusSourceOfficeService;

    @Autowired
    public AmadeusIssuanceServiceImpl(AmadeusSessionManager amadeusSessionManager) {
        this.amadeusSessionManager = amadeusSessionManager;
    }

    private String getSpecificOfficeIdforAirline(FlightItinerary itinerary){
        Configuration config = Play.application().configuration();
        Configuration airlineBookingOfficeConfig = config.getConfig("amadeus.AIRLINE_BOOKING_OFFICE");
        for(Journey journey: itinerary.getJourneyList() ){
            for( AirSegmentInformation segmentInfo : journey.getAirSegmentList()){
                String CarrierCode = segmentInfo.getCarrierCode();
                //String officeId = config.getString("amadeus.AIRLINE_BOOKING_OFFICE."+carcode);
                String officeId = airlineBookingOfficeConfig.getString(CarrierCode);
                if(officeId != null){
                    return officeId;
                }
            }
        }
        return null;
    }
    public IssuanceResponse priceBookedPNR(IssuanceRequest issuanceRequest){
        logger.debug("=======================  pricePNR called =========================");
        //ServiceHandler serviceHandler = null;
        IssuanceResponse issuanceResponse = new IssuanceResponse();
        issuanceResponse.setPnrNumber(issuanceRequest.getGdsPNR());

        TravellerMasterInfo travellerMasterInfo = amadeusBookingService.allPNRDetails(issuanceRequest,issuanceRequest.getGdsPNR());
        /*if(travellerMasterInfo.getTravellersList() != null) {
            int ticketSize = travellerMasterInfo.getTravellersList().get(0).getTicketNumberMap().size();
            if (ticketSize > 0) {
                issuanceResponse.setIssued(true);
                issuanceResponse.setChangedPriceLow(false);
                issuanceResponse.setSuccess(true);
                return issuanceResponse;
            }
        }*/
        boolean isSeamen = issuanceRequest.isSeamen();
        AmadeusSessionWrapper amadeusSessionWrapper = null;
        try {
            //serviceHandler = new ServiceHandler();
            amadeusSessionWrapper = serviceHandler.logIn();
            PNRReply gdsPNRReply = serviceHandler.retrivePNR(issuanceRequest.getGdsPNR(), amadeusSessionWrapper);

            List<String> segmentStatusList =  segmentStatus(gdsPNRReply);
            if(segmentStatusList.contains("HX")){
                issuanceResponse.setErrorCode("INFORMATIVE_SEGMENT");
                issuanceResponse.setSuccess(true);
                ErrorMessage errorMessage = ErrorMessageHelper
                        .createErrorMessage("error",
                                ErrorMessage.ErrorType.ERROR, "Amadeus");
                issuanceResponse.setErrorMessage(errorMessage);
                return issuanceResponse;
            }
            for(PNRReply.DataElementsMaster.DataElementsIndiv dataElementsDiv : gdsPNRReply.getDataElementsMaster().getDataElementsIndiv()) {
                if ("FA".equals(dataElementsDiv.getElementManagementData().getSegmentName())) {
                    logger.debug("Tickets are already issued cannot reprice the pnr: " + issuanceRequest.getGdsPNR());
                    issuanceResponse.setIssued(true);
                    issuanceResponse.setChangedPriceLow(false);
                    issuanceResponse.setSuccess(true);
                    return issuanceResponse;
                } else if ("FHM".equals(dataElementsDiv.getElementManagementData().getSegmentName())) {
                    logger.debug("Tickets are already issued cannot reprice the pnr: " + issuanceRequest.getGdsPNR());
                    issuanceResponse.setIssued(true);
                    issuanceResponse.setChangedPriceLow(false);
                    issuanceResponse.setSuccess(true);
                    return issuanceResponse;
                } else if ("FHE".equals(dataElementsDiv.getElementManagementData().getSegmentName())) {
                    logger.debug("Tickets are already issued cannot reprice the pnr: " + issuanceRequest.getGdsPNR());
                    issuanceResponse.setIssued(true);
                    issuanceResponse.setChangedPriceLow(false);
                    issuanceResponse.setSuccess(true);
                    return issuanceResponse;
                }
            }

            String carrierCode = "";

            //carrierCode = issuanceRequest.getFlightItinerary().getJourneys(isSeamen).get(0).getAirSegmentList().get(0).getValidatingCarrierCode();

            boolean isDomestic = AmadeusHelper.checkAirportCountry("India", issuanceRequest.getFlightItinerary().getJourneys(isSeamen));
            FarePricePNRWithBookingClassReply pricePNRReply = new FarePricePNRWithBookingClassReply();

            List<AirSegmentInformation> airSegmentList = new ArrayList<>();
            for (Journey journey : issuanceRequest.getFlightItinerary().getJourneys(issuanceRequest.isSeamen())) {
                for (AirSegmentInformation airSegmentInformation : journey.getAirSegmentList()) {
                    airSegmentList.add(airSegmentInformation);
                }
            }

            List<FareList> pricePNRReplyFareList = new ArrayList<>();
            boolean isSegmentWisePricing = issuanceRequest.getFlightItinerary().getPricingInformation(issuanceRequest.isSeamen()).isSegmentWisePricing();

            if (isSegmentWisePricing) {
                List<SegmentPricing> segmentPricingList = issuanceRequest.getFlightItinerary().getPricingInformation(issuanceRequest.isSeamen()).getSegmentPricingList();

                Map<String, AirSegmentInformation> segmentsInfo = new HashMap<>();
                for (Journey journey : issuanceRequest.getFlightItinerary().getJourneys(issuanceRequest.isSeamen())) {
                    for (AirSegmentInformation airSegmentInformation : journey.getAirSegmentList()) {
                        String key = airSegmentInformation.getFromLocation() + airSegmentInformation.getToLocation();
                        segmentsInfo.put(key, airSegmentInformation);
                    }
                }

                for (SegmentPricing segmentPricing : segmentPricingList) {
                    List<String> segmentKeysList = segmentPricing.getSegmentKeysList();
                    List<AirSegmentInformation> airSegment = new ArrayList<>();
                    for (String segmentKey : segmentKeysList) {
                        airSegment.add(segmentsInfo.get(segmentKey));
                    }
                    carrierCode = airSegment.get(airSegment.size()-1).getValidatingCarrierCode();
                    if(carrierCode == null) {
                        carrierCode = airSegment.get(airSegment.size()-1).getOperatingCarrierCode();
                    }
                    String officeId = "";
                    boolean isOfficeIdError = false;
                    boolean isFlightAvailable = true;
                    officeId = getSpecificOfficeIdforAirline(issuanceRequest.getFlightItinerary());
                    if(officeId == null) {
                        if(isSeamen){
                            officeId = issuanceRequest.getFlightItinerary().getSeamanPricingInformation().getPricingOfficeId();
                        }else{
                            officeId = issuanceRequest.getFlightItinerary().getPricingInformation().getPricingOfficeId();
                        }
                        if(officeId.equalsIgnoreCase(amadeusSourceOfficeService.getBenzySourceOffice().getOfficeId())){
                            officeId = amadeusSourceOfficeService.getPrioritySourceOffice().getOfficeId();
                        }
                    }


                    //isSegmentWisePricing ==TRUE
                    pricePNRReply = serviceHandler.pricePNR(carrierCode, gdsPNRReply,
                            issuanceRequest.isSeamen(), isDomestic, issuanceRequest.getFlightItinerary(), airSegment, isSegmentWisePricing, amadeusSessionWrapper);

                    if(pricePNRReply.getApplicationError() != null) {
                        if(pricePNRReply.getApplicationError().getErrorOrWarningCodeDetails().getErrorDetails().getErrorCode().equalsIgnoreCase("0")
                                && pricePNRReply.getApplicationError().getErrorOrWarningCodeDetails().getErrorDetails().getErrorCategory().equalsIgnoreCase("EC")) {
                            isOfficeIdError = true;
                        }else {
                            isFlightAvailable = false;
                        }
                    }
                    /*if (pricePNRReply.getApplicationError().getErrorWarningDescription().getFreeText().size() > 0) {
                        repricingErrors(issuanceResponse, pricePNRReply);
                        return issuanceResponse;
                    }*/
                    if (pricePNRReply.getApplicationError() != null && !isOfficeIdError) {
                        repricingErrors(issuanceResponse, pricePNRReply);
                        return issuanceResponse;
                    }
                    PNRReply gdsPNRReplyBenzy = null;
                    FarePricePNRWithBookingClassReply  pricePNRReplyBenzy = null;
                    if(isOfficeIdError) {
                        String tstRefNo = issuanceRequest.getGdsPNR();
                        int numberOfTst = (isSeamen) ? 1	: getNumberOfTST(travellerMasterInfo.getTravellersList());
                        gdsPNRReply = serviceHandler.retrivePNR(tstRefNo, amadeusSessionWrapper);
                        gdsPNRReply = serviceHandler.savePNRES(amadeusSessionWrapper, amadeusSourceOfficeService.getBenzySourceOffice().getOfficeId());


                        AmadeusSessionWrapper benzyAmadeusSessionWrapper = serviceHandler.logIn(amadeusSourceOfficeService.getBenzySourceOffice().getOfficeId());
                        System.out.println(tstRefNo);
                        serviceHandler.retrivePNR(tstRefNo,benzyAmadeusSessionWrapper);
                        pricePNRReply = serviceHandler.pricePNR(carrierCode, gdsPNRReply,
                                issuanceRequest.isSeamen(), isDomestic, issuanceRequest.getFlightItinerary(), airSegment, isSegmentWisePricing, benzyAmadeusSessionWrapper);

                        gdsPNRReplyBenzy = serviceHandler.savePNR(benzyAmadeusSessionWrapper);
                        PNRCancel pnrCancel = new PNRAddMultiElementsh().exitEsx(tstRefNo);
                        serviceHandler.exitESPnr(pnrCancel,amadeusSessionWrapper);
                        if(benzyAmadeusSessionWrapper != null){
                            amadeusSessionManager.removeActiveSession(benzyAmadeusSessionWrapper.getmSession().value);
                            serviceHandler.logOut(benzyAmadeusSessionWrapper);
                        }
                    }
                    List<FareList> tempPricePNRReplyFareList = pricePNRReply.getFareList();

                    int numberOfTst = (issuanceRequest.isSeamen()) ? 1
                            : AmadeusBookingHelper.getNumberOfTST(issuanceRequest.getTravellerList());
                    if(!isOfficeIdError) {
                        for (int i = 0; i < numberOfTst; i++) {
                            pricePNRReplyFareList.add(tempPricePNRReplyFareList.get(i));
                        }

                        TicketCreateTSTFromPricingReply ticketCreateTSTFromPricingReply = serviceHandler
                                .createTST(numberOfTst, amadeusSessionWrapper);

                        if (ticketCreateTSTFromPricingReply.getApplicationError() != null) {
                            String errorCode = ticketCreateTSTFromPricingReply
                                    .getApplicationError().getApplicationErrorInfo()
                                    .getApplicationErrorDetail().getApplicationErrorCode();
                            logger.debug("Amadeus Issuance TST creation error " + errorCode);
                            ErrorMessage errorMessage = ErrorMessageHelper
                                    .createErrorMessage("error",
                                            ErrorMessage.ErrorType.ERROR, "Amadeus");
                            issuanceResponse.setErrorMessage(errorMessage);
                            issuanceResponse.setSuccess(false);
                            return issuanceResponse;
                        }
                    }
                }
//is SegmentWisePricing ==false
            } else {
                String validatingcarrierCode = travellerMasterInfo.getItinerary().getJourneys(isSeamen).get(0).getAirSegmentList().get(0).getValidatingCarrierCode();
                if (validatingcarrierCode == null) {
                     validatingcarrierCode = issuanceRequest.getFlightItinerary().getJourneyList().get(0).getAirSegmentList().get(0).getOperatingCarrierCode();
                }

                String officeId = "";
                boolean isOfficeIdError = false;
                boolean isFlightAvailable = true;
                officeId = getSpecificOfficeIdforAirline(issuanceRequest.getFlightItinerary());
                if(officeId == null) {
                    if(isSeamen){
                        officeId = issuanceRequest.getFlightItinerary().getSeamanPricingInformation().getPricingOfficeId();
                    }else{
                        officeId = issuanceRequest.getFlightItinerary().getPricingInformation().getPricingOfficeId();
                    }
                    if(officeId.equalsIgnoreCase(amadeusSourceOfficeService.getBenzySourceOffice().getOfficeId())){
                        officeId = amadeusSourceOfficeService.getPrioritySourceOffice().getOfficeId();
                    }
                }


                pricePNRReply = serviceHandler.pricePNR(validatingcarrierCode, gdsPNRReply,
                        issuanceRequest.isSeamen(), isDomestic, issuanceRequest.getFlightItinerary(), airSegmentList, isSegmentWisePricing, amadeusSessionWrapper);

                if(pricePNRReply.getApplicationError() != null) {
                    if(pricePNRReply.getApplicationError().getErrorOrWarningCodeDetails().getErrorDetails().getErrorCode().equalsIgnoreCase("0")
                            && pricePNRReply.getApplicationError().getErrorOrWarningCodeDetails().getErrorDetails().getErrorCategory().equalsIgnoreCase("EC")) {
                        isOfficeIdError = true;
                    }else {
                        isFlightAvailable = false;
                    }
                }

                if (pricePNRReply.getApplicationError() != null && !isOfficeIdError) {
                    repricingErrors(issuanceResponse, pricePNRReply);
                    return issuanceResponse;
                }

                PNRReply gdsPNRReplyBenzy = null;
                FarePricePNRWithBookingClassReply  pricePNRReplyBenzy = null;
                if(isOfficeIdError) {
                    String tstRefNo = issuanceRequest.getGdsPNR();
                    int numberOfTst = (isSeamen) ? 1	: getNumberOfTST(travellerMasterInfo.getTravellersList());
                    gdsPNRReply = serviceHandler.retrivePNR(tstRefNo, amadeusSessionWrapper);
                    gdsPNRReply = serviceHandler.savePNRES(amadeusSessionWrapper, amadeusSourceOfficeService.getBenzySourceOffice().getOfficeId());


                    AmadeusSessionWrapper benzyAmadeusSessionWrapper = serviceHandler.logIn(amadeusSourceOfficeService.getBenzySourceOffice().getOfficeId());

                    serviceHandler.retrivePNR(tstRefNo,benzyAmadeusSessionWrapper);
                    pricePNRReply = serviceHandler.pricePNR(validatingcarrierCode, gdsPNRReply,
                            issuanceRequest.isSeamen(), isDomestic, issuanceRequest.getFlightItinerary(), airSegmentList, isSegmentWisePricing, benzyAmadeusSessionWrapper);

                    gdsPNRReplyBenzy = serviceHandler.savePNR(benzyAmadeusSessionWrapper);

                    PNRCancel pnrCancel = new PNRAddMultiElementsh().exitEsx(tstRefNo);
                    serviceHandler.exitESPnr(pnrCancel,amadeusSessionWrapper);
                    //serviceHandler.cancelPNR(tstRefNo, gdsPNRReplyBenzy,amadeusSessionWrapper);
                    if(benzyAmadeusSessionWrapper != null){
                        amadeusSessionManager.removeActiveSession(benzyAmadeusSessionWrapper.getmSession().value);
                        serviceHandler.logOut(benzyAmadeusSessionWrapper);
                    }
                }

                pricePNRReplyFareList = pricePNRReply.getFareList();
                    if (pricePNRReplyFareList.isEmpty()) {
                        issuanceResponse.setSuccess(false);
                        logger.error("Fare list is null : ", pricePNRReplyFareList);
                        return issuanceResponse;
                    }

                        int numberOfTst = (issuanceRequest.isSeamen()) ? 1
                                : AmadeusBookingHelper.getNumberOfTST(issuanceRequest.getTravellerList());
                 if(!isOfficeIdError) {
                        TicketCreateTSTFromPricingReply ticketCreateTSTFromPricingReply = serviceHandler
                                .createTST(numberOfTst, amadeusSessionWrapper);

                        if (ticketCreateTSTFromPricingReply.getApplicationError() != null) {
                            String errorCode = ticketCreateTSTFromPricingReply
                                    .getApplicationError().getApplicationErrorInfo()
                                    .getApplicationErrorDetail().getApplicationErrorCode();
                            logger.debug("Amadeus Issuance TST creation error " + errorCode);
                            ErrorMessage errorMessage = ErrorMessageHelper
                                    .createErrorMessage("error",
                                            ErrorMessage.ErrorType.ERROR, "Amadeus");
                            issuanceResponse.setErrorMessage(errorMessage);
                            issuanceResponse.setSuccess(false);
                            return issuanceResponse;
                        }
                    }

                    if (pricePNRReply.getFareList().size() != numberOfTst) {
                        pricePNRReplyFareList = pricePNRReplyFareList.subList(0, numberOfTst);
                    }



//            PricingInformation pricingInformation = AmadeusBookingHelper.getPricingInfo(pricePNRReplyFareList, issuanceRequest.getAdultCount(),
//                    issuanceRequest.getChildCount(), issuanceRequest.getInfantCount());
                PricingInformation pricingInformation = AmadeusBookingHelper.getPricingInfoWithSegmentPricing(gdsPNRReply, pricePNRReplyFareList,
                        issuanceRequest.isSeamen(), airSegmentList);
                BigDecimal bookedPrice = issuanceRequest.getFlightItinerary().getPricingInformation(issuanceRequest.isSeamen()).getTotalPriceValue();
                BigDecimal newPrice = pricingInformation.getTotalPriceValue();




                    if(bookedPrice.compareTo(newPrice) < 0) {
                        issuanceResponse.setIsPriceChanged(true);
                        issuanceResponse.setChangedPriceLow(false);
                        issuanceResponse.setFlightItinerary(issuanceRequest.getFlightItinerary());
                        issuanceResponse.getFlightItinerary().setPricingInformation(isSeamen, pricingInformation);
                    }

                    if(bookedPrice.compareTo(newPrice) > 0) {
                        issuanceResponse.setIsPriceChanged(false);
                        issuanceResponse.setChangedPriceLow(true);
                        BigDecimal newLowerPrice = pricingInformation.getTotalPriceValue();
                        issuanceResponse.setNewLowerPrice(newLowerPrice);
                    }
            }
            issuanceResponse.setSuccess(true);

            String sessionId = amadeusSessionManager.storeActiveSession(amadeusSessionWrapper, issuanceRequest.getGdsPNR());
            issuanceResponse.setSessionIdRef(sessionId);

                logger.debug("=======================  pricePNR end =========================");

        } catch (Exception e) {
            issuanceResponse.setSuccess(false);
//            XMLFileUtility.createXMLFile(e, "PNRException.xml");
            logger.error("Amadeus priceBookedPNR error : ", e);
            e.printStackTrace();
        }
        return issuanceResponse;
    }

    private List<String> segmentStatus(PNRReply pnrReply){
        List<String> segmentStatusList = new ArrayList<>();
        List<PNRReply.OriginDestinationDetails> originDestinationDetailsList = pnrReply.getOriginDestinationDetails();
        for(PNRReply.OriginDestinationDetails originDestinationDetails: originDestinationDetailsList){
            for(PNRReply.OriginDestinationDetails.ItineraryInfo itineraryInfo : originDestinationDetails.getItineraryInfo()){
                for(String statusList : itineraryInfo.getRelatedProduct().getStatus()){
                    segmentStatusList.add(statusList);
                }
            }
        }

        return segmentStatusList;
    }

    private boolean repricingErrors(IssuanceResponse issuanceResponse, FarePricePNRWithBookingClassReply pricePNRReply) {
        if (pricePNRReply.getApplicationError().getErrorWarningDescription().getFreeText().size() > 0) {
            logger.debug("Re-Pricing " + pricePNRReply.getApplicationError().getErrorWarningDescription().getFreeText().toString());
            String errorMessages = pricePNRReply.getApplicationError().getErrorWarningDescription().getFreeText().get(0);
            if (errorMessages.equals("NO FARE FOR BOOKING CODE-TRY OTHER PRICING OPTIONS"))
            {
                issuanceResponse.setErrorCode("NO_FARE");
                issuanceResponse.setSuccess(true);
                ErrorMessage errorMessage = ErrorMessageHelper
                        .createErrorMessage("error",
                                ErrorMessage.ErrorType.ERROR, "Amadeus");
                issuanceResponse.setErrorMessage(errorMessage);
                return true;
            } else if (errorMessages.equals("NO TICKETABLE VALIDATING CARRIER")) {
               issuanceResponse.setErrorCode("NO_FARE");
                issuanceResponse.setSuccess(true);
                ErrorMessage errorMessage = ErrorMessageHelper
                        .createErrorMessage("error",
                                ErrorMessage.ErrorType.ERROR, "Amadeus");
                issuanceResponse.setErrorMessage(errorMessage);

                return true;
            } else if (errorMessages.equals("NO CURRENT FARE IN SYSTEM")) {
                issuanceResponse.setErrorCode("NO_FARE");
                issuanceResponse.setSuccess(true);
                ErrorMessage errorMessage = ErrorMessageHelper
                        .createErrorMessage("error",
                                ErrorMessage.ErrorType.ERROR, "Amadeus");
                issuanceResponse.setErrorMessage(errorMessage);

                return true;
            } else if (errorMessages.equals("NO VALID FARE/RULE COMBINATIONS FOR PRICING")) {
                issuanceResponse.setErrorCode("NO_FARE");
                issuanceResponse.setSuccess(true);
                ErrorMessage errorMessage = ErrorMessageHelper
                        .createErrorMessage("error",
                                ErrorMessage.ErrorType.ERROR, "Amadeus");

                issuanceResponse.setErrorMessage(errorMessage);
                return true;
            }
        }
        return false;
    }

    public IssuanceResponse issueTicket(IssuanceRequest issuanceRequest) {
        logger.debug("=======================  Issuance called =========================");
        //ServiceHandler serviceHandler = null;
        IssuanceResponse issuanceResponse = new IssuanceResponse();
        issuanceResponse.setPnrNumber(issuanceRequest.getGdsPNR());
        //Session session = null;
        AmadeusSessionWrapper amadeusSessionWrapper = null;
        try {
            //serviceHandler = new ServiceHandler();
            amadeusSessionWrapper = amadeusSessionManager.getActiveSessionByGdsPNR(issuanceRequest.getGdsPNR());
            //serviceHandler.setSession(session);


            PNRReply gdsPNRReply = serviceHandler.savePNR(amadeusSessionWrapper);

            gdsPNRReply = serviceHandler.retrivePNR(issuanceRequest.getGdsPNR(), amadeusSessionWrapper);

            amadeusLogger.debug("retrievePNRRes1 "+ new Date()+" ------->>"+ new XStream().toXML(gdsPNRReply));

            issuanceResponse = docIssuance(serviceHandler, issuanceRequest, issuanceResponse, gdsPNRReply, amadeusSessionWrapper);

            logger.debug("=======================  Issuance end =========================");
        } catch (Exception e) {
//            XMLFileUtility.createXMLFile(e, "PNRRetrieveException.xml");
            e.printStackTrace();
            logger.error("docIssuance Exception ", e);
        }finally {
            amadeusSessionManager.removeActiveSession(amadeusSessionWrapper.getmSession().value);
            serviceHandler.logOut(amadeusSessionWrapper);
//			amadeusSessionManager.updateAmadeusSession(amadeusSessionWrapper);
        }
        return issuanceResponse;
    }

    public boolean checkForMultipleValidatingCarriers(PNRReply gdsPNRReply){
        Set<String> carrierSet = new HashSet<>();
        for(PNRReply.OriginDestinationDetails originDestinationDetails : gdsPNRReply.getOriginDestinationDetails()){
            for(PNRReply.OriginDestinationDetails.ItineraryInfo itineraryInfo : originDestinationDetails.getItineraryInfo()){
                String validatingCarrier = itineraryInfo.getTravelProduct().getCompanyDetail().getIdentification();
                carrierSet.add(validatingCarrier);
            }
        }

        if(carrierSet.size() > 1){
            return true;
        }

        return false;

    }

    public List<String> getTSTList(PNRReply gdsPNRReply){
        List<String> tstReferenceList = new ArrayList<>();
        for(PNRReply.TstData tstData : gdsPNRReply.getTstData()){
           String tstReference =  tstData.getTstGeneralInformation().getGeneralInformation().getTstReferenceNumber();
            tstReferenceList.add(tstReference);
        }

        return tstReferenceList;
    }
    public IssuanceResponse docIssuance(ServiceHandler serviceHandler, IssuanceRequest issuanceRequest,
                                        IssuanceResponse issuanceResponse, PNRReply gdsPNRReply1, AmadeusSessionWrapper amadeusSessionWrapper) throws InterruptedException {
        String pnr = issuanceRequest.getGdsPNR();
        logger.debug(pnr + " amadeus docIssuance called ");
        Date pnrResponseReceivedAt = new Date();
        boolean sendTSTDataForIssuance = checkForMultipleValidatingCarriers(gdsPNRReply1);
        List<String> tstReferenceList = new ArrayList<>();
        boolean ticketStatus = false;
        DocIssuanceIssueTicketReply issuanceIssueTicketReply = null;
        int count = 1;
        if (sendTSTDataForIssuance) {
            tstReferenceList = getTSTList(gdsPNRReply1);
            for (String tstReference : tstReferenceList) {
                List<String> tstList = new ArrayList<>();
                tstList.add(tstReference);
                issuanceIssueTicketReply = serviceHandler.issueTicket(sendTSTDataForIssuance, tstList, amadeusSessionWrapper);
                if (AmadeusConstants.ISSUANCE_OK_STATUS.equals(issuanceIssueTicketReply.getProcessingStatus().getStatusCode())) {
                    if(count == tstReferenceList.size()){
                        ticketStatus = true;
                    }else {
                        PNRReply gdsPNRReply = serviceHandler.retrivePNR(issuanceRequest.getGdsPNR(), amadeusSessionWrapper);
                    }
                }else {
                    break;
                }
                count = count + 1;
            }
        } else {
            issuanceIssueTicketReply = serviceHandler.issueTicket(sendTSTDataForIssuance, tstReferenceList, amadeusSessionWrapper);
            if (AmadeusConstants.ISSUANCE_OK_STATUS.equals(issuanceIssueTicketReply.getProcessingStatus().getStatusCode())) {
                ticketStatus = true;
            }
        }
        if (ticketStatus) {
            Thread.sleep(3000L);
            PNRReply gdsPNRReply = serviceHandler.retrivePNR(issuanceRequest.getGdsPNR(), amadeusSessionWrapper);
            boolean allTicketsReceived = AmadeusBookingHelper.createTickets(issuanceResponse,issuanceRequest, gdsPNRReply);
            logger.debug(pnr + " Amadeus issuance all tickets received : " + allTicketsReceived);
            if(allTicketsReceived){
                issuanceResponse.setSuccess(true);
                return issuanceResponse;
            }else {
                issuanceResponse = ignoreAndRetrievePNR(serviceHandler, issuanceRequest, issuanceResponse , pnrResponseReceivedAt, amadeusSessionWrapper);
            }
        } else {
            String errorDescription = issuanceIssueTicketReply
                    .getErrorGroup().getErrorWarningDescription()
                    .getFreeText();
            logger.debug(pnr + " Amadeus docIssuance  failed status returned " + issuanceIssueTicketReply.getProcessingStatus().getStatusCode() + " : " + errorDescription);

            if (errorDescription.contains(AmadeusConstants.CAPPING_LIMIT_STRING)) {
                logger.debug("Send Email to operator saying capping limit is reached");
                issuanceResponse.setCappingLimitReached(true);
            }
        }

        return issuanceResponse;
    }

    public IssuanceResponse ignoreAndRetrievePNR(ServiceHandler serviceHandler, IssuanceRequest issuanceRequest, IssuanceResponse issuanceResponse,  Date pnrResponseReceivedAt, AmadeusSessionWrapper amadeusSessionWrapper) throws InterruptedException {
        String pnr = issuanceRequest.getGdsPNR();
        logger.debug(pnr + "ignoreAndRetrievePNR called");
        Thread.sleep(3000L);
        PNRReply gdsPNRReply = serviceHandler.ignoreAndRetrievePNR(amadeusSessionWrapper);
        boolean allTicketsReceived = AmadeusBookingHelper.createTickets(issuanceResponse, issuanceRequest, gdsPNRReply);
        logger.debug(pnr + "ignoreAndRetrievePNR called allTicketsReceived: " + allTicketsReceived);
        if(allTicketsReceived){
            issuanceResponse.setSuccess(true);
            return issuanceResponse;
        }else{
            Period p = new Period(new DateTime(pnrResponseReceivedAt), new DateTime(), PeriodType.minutes());
            if(p.getMinutes() >= 2){
                logger.debug(pnr + "ignoreAndRetrievePNR time expired issuance failed");
                issuanceResponse.setSuccess(false);
                return issuanceResponse;
            }
            ignoreAndRetrievePNR(serviceHandler, issuanceRequest, issuanceResponse, pnrResponseReceivedAt, amadeusSessionWrapper);
        }
        return issuanceResponse;
    }
}
