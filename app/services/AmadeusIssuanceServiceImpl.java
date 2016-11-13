package services;

import com.amadeus.xml.pnracc_11_3_1a.PNRReply;
import com.amadeus.xml.tautcr_04_1_1a.TicketCreateTSTFromPricingReply;
import com.amadeus.xml.tpcbrr_12_4_1a.FarePricePNRWithBookingClassReply.FareList;
import com.amadeus.xml.tpcbrr_12_4_1a.FarePricePNRWithBookingClassReply;
import com.amadeus.xml.ttktir_09_1_1a.DocIssuanceIssueTicketReply;
import com.amadeus.xml.ws._2009._01.wbs_session_2_0.Session;
import com.compassites.GDSWrapper.amadeus.ServiceHandler;
import com.compassites.constants.AmadeusConstants;
import com.compassites.model.*;
import com.thoughtworks.xstream.XStream;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.PeriodType;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import utils.*;

import java.math.BigDecimal;
import java.util.*;
/*
*
 * Created by yaseen on 19-01-2016.*/

@Service
public class AmadeusIssuanceServiceImpl {


    static org.slf4j.Logger amadeusLogger = LoggerFactory.getLogger("amadeus");

    static org.slf4j.Logger logger = LoggerFactory.getLogger("gds");

    private AmadeusSessionManager amadeusSessionManager;

    @Autowired
    public AmadeusIssuanceServiceImpl(AmadeusSessionManager amadeusSessionManager) {
        this.amadeusSessionManager = amadeusSessionManager;
    }

    public IssuanceResponse priceBookedPNR(IssuanceRequest issuanceRequest){
        logger.debug("=======================  pricePNR called =========================");
        ServiceHandler serviceHandler = null;
        IssuanceResponse issuanceResponse = new IssuanceResponse();
        issuanceResponse.setPnrNumber(issuanceRequest.getGdsPNR());
        boolean isSeamen = issuanceRequest.isSeamen();
        try {
            serviceHandler = new ServiceHandler();
            serviceHandler.logIn();
            PNRReply gdsPNRReply = serviceHandler.retrivePNR(issuanceRequest.getGdsPNR());
            String carrierCode = "";

            carrierCode = issuanceRequest.getFlightItinerary().getJourneys(isSeamen).get(0).getAirSegmentList().get(0).getCarrierCode();

            boolean isDomestic = AmadeusHelper.checkAirportCountry("India", issuanceRequest.getFlightItinerary().getJourneys(isSeamen));
            FarePricePNRWithBookingClassReply pricePNRReply = new FarePricePNRWithBookingClassReply();

            List<AirSegmentInformation> airSegmentList = new ArrayList<>();
            for(Journey journey : issuanceRequest.getFlightItinerary().getJourneys(issuanceRequest.isSeamen())){
                for(AirSegmentInformation airSegmentInformation : journey.getAirSegmentList()){
                    airSegmentList.add(airSegmentInformation);
                }
            }

            List<FareList> pricePNRReplyFareList = new ArrayList<>();
            boolean isSegmentWisePricing = issuanceRequest.getFlightItinerary().getPricingInformation(issuanceRequest.isSeamen()).isSegmentWisePricing();

            if(isSegmentWisePricing){
                List<SegmentPricing> segmentPricingList = issuanceRequest.getFlightItinerary().getPricingInformation(issuanceRequest.isSeamen()).getSegmentPricingList();

                Map<String,AirSegmentInformation> segmentsInfo = new HashMap<>();
                for(Journey journey : issuanceRequest.getFlightItinerary().getJourneys(issuanceRequest.isSeamen())){
                    for(AirSegmentInformation airSegmentInformation : journey.getAirSegmentList()){
                        String key = airSegmentInformation.getFromLocation() + airSegmentInformation.getToLocation();
                        segmentsInfo.put(key, airSegmentInformation);
                    }
                }

                for(SegmentPricing segmentPricing : segmentPricingList) {
                    List<String> segmentKeysList = segmentPricing.getSegmentKeysList();
                    List<AirSegmentInformation> airSegment = new ArrayList<>();
                    for(String segmentKey : segmentKeysList){
                        airSegment.add(segmentsInfo.get(segmentKey));
                    }
                    pricePNRReply = serviceHandler.pricePNR(carrierCode, gdsPNRReply,
                            issuanceRequest.isSeamen(), isDomestic, issuanceRequest.getFlightItinerary(), airSegment, isSegmentWisePricing);
                    List<FareList> tempPricePNRReplyFareList = pricePNRReply.getFareList();

                    int numberOfTst = (issuanceRequest.isSeamen()) ? 1
                            : AmadeusBookingHelper.getNumberOfTST(issuanceRequest.getTravellerList());

                    for(int i = 0; i< numberOfTst ; i++){
                        pricePNRReplyFareList.add(tempPricePNRReplyFareList.get(i));
                    }

                    TicketCreateTSTFromPricingReply ticketCreateTSTFromPricingReply = serviceHandler
                            .createTST(numberOfTst);

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

            } else {
                pricePNRReply = serviceHandler.pricePNR(carrierCode, gdsPNRReply,
                        issuanceRequest.isSeamen(), isDomestic, issuanceRequest.getFlightItinerary(), airSegmentList, isSegmentWisePricing);
                pricePNRReplyFareList = pricePNRReply.getFareList();
                if(pricePNRReplyFareList.isEmpty()){
                    issuanceResponse.setSuccess(false);
                    logger.error("Fare list is null : ", pricePNRReplyFareList);
                    return issuanceResponse;
                }

                int numberOfTst = (issuanceRequest.isSeamen()) ? 1
                        : AmadeusBookingHelper.getNumberOfTST(issuanceRequest.getTravellerList());

                TicketCreateTSTFromPricingReply ticketCreateTSTFromPricingReply = serviceHandler
                        .createTST(numberOfTst);

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

                if(pricePNRReply.getFareList().size() != numberOfTst){
                    pricePNRReplyFareList = pricePNRReplyFareList.subList(0, numberOfTst);
                }
            }



//            PricingInformation pricingInformation = AmadeusBookingHelper.getPricingInfo(pricePNRReplyFareList, issuanceRequest.getAdultCount(),
//                    issuanceRequest.getChildCount(), issuanceRequest.getInfantCount());
            PricingInformation pricingInformation = AmadeusBookingHelper.getPricingInfoWithSegmentPricing(gdsPNRReply,pricePNRReplyFareList,
                    issuanceRequest.isSeamen(), airSegmentList);
            BigDecimal bookedPrice = issuanceRequest.getFlightItinerary().getPricingInformation(issuanceRequest.isSeamen()).getTotalPriceValue();
            BigDecimal newPrice = pricingInformation.getTotalPriceValue();


            if(bookedPrice.compareTo(newPrice) != 0) {
                logger.debug("Price of the PNR : " + issuanceRequest.getGdsPNR() + "changed to "  + newPrice );
                issuanceResponse.setIsPriceChanged(true);
                issuanceResponse.setFlightItinerary(issuanceRequest.getFlightItinerary());
                issuanceResponse.getFlightItinerary().setPricingInformation(isSeamen, pricingInformation);

            }else {
                issuanceResponse.setIsPriceChanged(false);

                //todo added for testing need to remove
                issuanceResponse.setFlightItinerary(issuanceRequest.getFlightItinerary());
                issuanceResponse.getFlightItinerary().setPricingInformation(isSeamen, pricingInformation);
            }
            issuanceResponse.setSuccess(true);

            String sessionId = amadeusSessionManager.storeActiveSession(serviceHandler.getSession(), issuanceRequest.getGdsPNR());
            issuanceResponse.setSessionIdRef(sessionId);

            logger.debug("=======================  pricePNR end =========================");
        } catch (Exception e) {
            issuanceResponse.setSuccess(false);
            XMLFileUtility.createXMLFile(e, "PNRException.xml");
            logger.error("Amadeus priceBookedPNR error : ", e);
            e.printStackTrace();
        }
        return issuanceResponse;
    }

    public IssuanceResponse issueTicket(IssuanceRequest issuanceRequest) {
        logger.debug("=======================  Issuance called =========================");
        ServiceHandler serviceHandler = null;
        IssuanceResponse issuanceResponse = new IssuanceResponse();
        issuanceResponse.setPnrNumber(issuanceRequest.getGdsPNR());
        Session session = null;
        try {
            serviceHandler = new ServiceHandler();
            session = amadeusSessionManager.getActiveSessionByGdsPNR(issuanceRequest.getGdsPNR());
            serviceHandler.setSession(session);


            PNRReply gdsPNRReply = serviceHandler.savePNR();

            gdsPNRReply = serviceHandler.retrivePNR(issuanceRequest.getGdsPNR());

            amadeusLogger.debug("retrievePNRRes1 "+ new Date()+" ------->>"+ new XStream().toXML(gdsPNRReply));

            issuanceResponse = docIssuance(serviceHandler, issuanceRequest, issuanceResponse, gdsPNRReply);

            logger.debug("=======================  Issuance end =========================");
        } catch (Exception e) {
            XMLFileUtility.createXMLFile(e, "PNRRetrieveException.xml");
            e.printStackTrace();
            logger.error("docIssuance Exception ", e);
        }finally {
            serviceHandler.logOut();
            amadeusSessionManager.removeActiveSession(session);
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
                                        IssuanceResponse issuanceResponse, PNRReply gdsPNRReply1) throws InterruptedException {
        String pnr = issuanceRequest.getGdsPNR();
        logger.debug(pnr + " amadeus docIssuance called " );
        Date pnrResponseReceivedAt = new Date();
        boolean sendTSTDataForIssuance = checkForMultipleValidatingCarriers(gdsPNRReply1);
        List<String> tstReferenceList = new ArrayList<>();
        if(sendTSTDataForIssuance){
            tstReferenceList = getTSTList(gdsPNRReply1);
        }
        DocIssuanceIssueTicketReply issuanceIssueTicketReply = serviceHandler.issueTicket(sendTSTDataForIssuance, tstReferenceList);
        if (AmadeusConstants.ISSUANCE_OK_STATUS.equals(issuanceIssueTicketReply.getProcessingStatus().getStatusCode())) {
            Thread.sleep(3000L);
            PNRReply gdsPNRReply = serviceHandler.retrivePNR(issuanceRequest.getGdsPNR());
            boolean allTicketsReceived = AmadeusBookingHelper.createTickets(issuanceResponse,issuanceRequest, gdsPNRReply);
            logger.debug(pnr + " Amadeus issuance all tickets received : " + allTicketsReceived);
            if(allTicketsReceived){
                issuanceResponse.setSuccess(true);
                return issuanceResponse;
            }else {
                issuanceResponse = ignoreAndRetrievePNR(serviceHandler, issuanceRequest, issuanceResponse , pnrResponseReceivedAt);
            }
        } else {
            logger.debug(pnr + " Amadeus docIssuance  failed status returned " + issuanceIssueTicketReply.getProcessingStatus().getStatusCode());
            String errorDescription = issuanceIssueTicketReply
                    .getErrorGroup().getErrorWarningDescription()
                    .getFreeText();
            if (errorDescription.contains(AmadeusConstants.CAPPING_LIMIT_STRING)) {
                logger.debug("Send Email to operator saying capping limit is reached");
                issuanceResponse.setCappingLimitReached(true);
            }
        }

        return issuanceResponse;
    }

    public IssuanceResponse ignoreAndRetrievePNR(ServiceHandler serviceHandler, IssuanceRequest issuanceRequest, IssuanceResponse issuanceResponse,  Date pnrResponseReceivedAt) throws InterruptedException {
        String pnr = issuanceRequest.getGdsPNR();
        logger.debug(pnr + "ignoreAndRetrievePNR called");
        Thread.sleep(3000L);
        PNRReply gdsPNRReply = serviceHandler.ignoreAndRetrievePNR();
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
            ignoreAndRetrievePNR(serviceHandler, issuanceRequest, issuanceResponse, pnrResponseReceivedAt);
        }
        return issuanceResponse;
    }
}
