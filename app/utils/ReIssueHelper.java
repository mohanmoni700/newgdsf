package utils;

import com.amadeus.xml._2010._06.retailing_types_v2.ErrorType;
import com.amadeus.xml._2010._06.ticket_rebookandrepricepnr_v1.AMATicketRebookAndRepricePNRRS;
import com.amadeus.xml.pnracc_11_3_1a.PNRReply;
import com.amadeus.xml.ttstrr_13_1_1a.TicketDisplayTSTReply;
import com.compassites.GDSWrapper.amadeus.ServiceHandler;
import com.compassites.constants.AmadeusConstants;
import com.compassites.constants.StaticConstatnts;
import com.compassites.exceptions.BaseCompassitesException;
import com.compassites.model.*;
import com.compassites.model.amadeus.AmadeusPaxInformation;
import com.compassites.model.traveller.TravellerMasterInfo;
import models.AmadeusSessionWrapper;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.PeriodType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import play.libs.Json;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

@Component
public class ReIssueHelper {

    static Logger logger = LoggerFactory.getLogger("gds");

    //Gets segment wise booking class
    public List<String> getBookingClassForSegmentsToBeReissued(PAXFareDetails paxFareDetails, List<Integer> selectedSegmentList) {

        List<String> segmentBookingClassList = new LinkedList<>();
        List<FareJourney> journeyFareList = paxFareDetails.getFareJourneyList();

        for (Integer selectedJourneyIndex : selectedSegmentList) {

            FareJourney fareJourney = journeyFareList.get(selectedJourneyIndex - 1);
            List<FareSegment> segmentFareList = fareJourney.getFareSegmentList();
            for (FareSegment fareSegment : segmentFareList) {
                segmentBookingClassList.add(fareSegment.getBookingClass());
            }
        }

        return segmentBookingClassList;
    }

    //Creates Error Message for Reissue Success warnings and Reissue Failures
    public void createErrorMessage(List<ErrorType> warnings, PNRResponse finalPnrResponse) {

        ErrorMessage errorMessage = new ErrorMessage();

        for (ErrorType errorType : warnings) {

            String warningOrError = errorType.getType();
            String errorDescription = errorType.getValue();
            String code = errorType.getCode();

            if (warningOrError.equalsIgnoreCase("E") || warningOrError.equalsIgnoreCase("F")) {

                errorMessage.setErrorCode("ReIssue Confirmation Error");
                errorMessage.setType(ErrorMessage.ErrorType.ERROR);
                errorMessage.setProvider(PROVIDERS.AMADEUS.toString());

                if (errorDescription != null) {
                    errorMessage.setMessage(errorDescription);
                } else {
                    errorMessage.setMessage(code);
                }
                errorMessage.setGdsPNR(finalPnrResponse.getPnrNumber());
                finalPnrResponse.setReIssueSuccess(false);

            }
            finalPnrResponse.setErrorMessage(errorMessage);
            break;
        }
    }

    //PNR Response for Reissued Bookings
    public void createPNRResponseForReIssuedBooking(String officeId, ServiceHandler serviceHandler, PNRResponse pnrResponse, TravellerMasterInfo travellerMasterInfo, AmadeusSessionManager amadeusSessionManager, AMATicketRebookAndRepricePNRRS.Success success) {

        PNRReply gdsPNRReply = null;
        AmadeusSessionWrapper amadeusSession = null;
        String gdsPnr = pnrResponse.getPnrNumber();
        try {

            try {
                amadeusSession = serviceHandler.logIn(officeId);
                gdsPNRReply = serviceHandler.retrivePNR(gdsPnr, amadeusSession);
            } catch (NullPointerException e) {
                logger.error("Error in Retrieving Reissued PNR {}", e.getMessage());
            } catch (Exception ex) {
                if (ex.getMessage().contains("IGNORE")) {
                    assert amadeusSession != null;
                    gdsPNRReply = serviceHandler.ignoreAndRetrievePNR(amadeusSession);
                }
            }

            Date lastPNRAddMultiElements = new Date();

            //Setting Airline PNR here
            assert gdsPNRReply != null;
            gdsPNRReply = getAirlinePnr(gdsPNRReply, lastPNRAddMultiElements, pnrResponse, amadeusSession, serviceHandler);

            //Checking Seat and Segment Availability here
            checkSegmentStatus(gdsPNRReply);


            String pnr = gdsPNRReply.getPnrHeader().get(0).getReservationInfo().getReservation().getControlNumber();
            pnrResponse.setPnrNumber(pnr);

            //Creating Amadeus Pax Reference and Line number here
            pnrResponse.setAmadeusPaxReference(createAmadeusPaxRefInfo(gdsPNRReply));
            pnrResponse.setSegmentRefMap(AmadeusBookingHelper.getSegmentRefMap(gdsPNRReply, pnr));
//            pnrResponse.setSegmentRefMap(AmadeusBookingHelper.(gdsPNRReply, pnr));

            pnrResponse.setFlightAvailable(true);
            if (gdsPNRReply.getSecurityInformation() != null && gdsPNRReply.getSecurityInformation().getSecondRpInformation() != null) {
                pnrResponse.setCreationOfficeId(gdsPNRReply.getSecurityInformation().getSecondRpInformation().getCreationOfficeId());
            }

        } catch (Exception e) {
            if (BaseCompassitesException.ExceptionCode.NO_SEAT.toString().equalsIgnoreCase(e.getMessage())) {
                ErrorMessage errorMessage = new ErrorMessage();
                errorMessage.setErrorCode(StaticConstatnts.NO_SEAT);
                errorMessage.setType(ErrorMessage.ErrorType.ERROR);
                errorMessage.setProvider(PROVIDERS.AMADEUS.toString());
                errorMessage.setMessage(e.getMessage());
                errorMessage.setGdsPNR(gdsPnr);
                pnrResponse.setErrorMessage(errorMessage);
            } else {
                ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage("error", ErrorMessage.ErrorType.ERROR, "AMADEUS");
                pnrResponse.setErrorMessage(errorMessage);
            }
        } finally {
            if (amadeusSession != null) {
                amadeusSessionManager.removeActiveSession(amadeusSession.getmSession().value);
                serviceHandler.logOut(amadeusSession);
            }
        }

        logger.info("ReIssued PNR Response :{}", Json.stringify(Json.toJson(pnrResponse)));
    }

    //Creates PNR Response for Split Success and Reissue Failed Scenarios
    public void createPNRResponseForReIssueFailedBooking(String officeID, ServiceHandler serviceHandler, PNRResponse pnrResponse, AmadeusSessionManager amadeusSessionManager, boolean isSeamen) {

        PNRReply gdsPNRReply = null;
        AmadeusSessionWrapper amadeusSession = null;
        List<Journey> journeyList;
        PricingInformation pricingInfo;

        String pnr = pnrResponse.getPnrNumber();
        try {

            try {
                amadeusSession = serviceHandler.logIn(officeID);
                gdsPNRReply = serviceHandler.retrivePNR(pnr, amadeusSession);
            } catch (NullPointerException e) {
                logger.error("Error in Retrieving Failed Reissue PNR {}", e.getMessage());
            } catch (Exception ex) {
                if (ex.getMessage().contains("IGNORE")) {
                    assert amadeusSession != null;
                    gdsPNRReply = serviceHandler.ignoreAndRetrievePNR(amadeusSession);
                }
            }

            //Checking if the pricing is still valid
            assert gdsPNRReply != null;
            journeyList = AmadeusBookingHelper.getJourneyListFromPNRResponse(gdsPNRReply, null);
            TicketDisplayTSTReply ticketDisplayTSTReply = serviceHandler.ticketDisplayTST(amadeusSession);

            if (ticketDisplayTSTReply.getFareList() == null || ticketDisplayTSTReply.getFareList().isEmpty()) {
                ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage("priceNotAvailable", ErrorMessage.ErrorType.ERROR, PROVIDERS.AMADEUS.toString());
                pnrResponse.setErrorMessage(errorMessage);
            }
            pricingInfo = AmadeusBookingHelper.getPricingInfoFromTST(gdsPNRReply, ticketDisplayTSTReply, isSeamen, journeyList);
            pricingInfo.setSegmentWisePricing(false);

            //Setting Airline PNR here
            Date lastPNRAddMultiElements = new Date();
            getAirlinePnr(gdsPNRReply, lastPNRAddMultiElements, pnrResponse, amadeusSession, serviceHandler);

            pnr = gdsPNRReply.getPnrHeader().get(0).getReservationInfo().getReservation().getControlNumber();

            pnrResponse.setPricingInfo(pricingInfo);
            pnrResponse.setCreationOfficeId(gdsPNRReply.getSecurityInformation().getSecondRpInformation().getCreationOfficeId());
            pnrResponse.setAmadeusPaxReference(createAmadeusPaxRefInfo(gdsPNRReply));
            pnrResponse.setPnrNumber(pnr);
            pnrResponse.setSegmentRefMap(AmadeusBookingHelper.getSegmentRefMap(gdsPNRReply, pnr));

        } catch (Exception e) {
            if (BaseCompassitesException.ExceptionCode.NO_SEAT.toString().equalsIgnoreCase(e.getMessage())) {
                ErrorMessage errorMessage = new ErrorMessage();
                errorMessage.setErrorCode(StaticConstatnts.NO_SEAT);
                errorMessage.setType(ErrorMessage.ErrorType.ERROR);
                errorMessage.setProvider(PROVIDERS.AMADEUS.toString());
                errorMessage.setMessage(e.getMessage());
                errorMessage.setGdsPNR(pnr);
                pnrResponse.setErrorMessage(errorMessage);
            } else {
                ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage("error", ErrorMessage.ErrorType.ERROR, "AMADEUS");
                pnrResponse.setErrorMessage(errorMessage);
            }
        } finally {
            if (amadeusSession != null) {
                amadeusSessionManager.removeActiveSession(amadeusSession.getmSession().value);
                serviceHandler.logOut(amadeusSession);
            }
        }

        logger.info("Reissue Failed PNR Response :{}", Json.stringify(Json.toJson(pnrResponse)));
    }

    //Gets the Airline PNR here
    public PNRReply getAirlinePnr(PNRReply pnrReply, Date currentDate, PNRResponse pnrResponse, AmadeusSessionWrapper amadeusSessionWrapper, ServiceHandler serviceHandler) throws BaseCompassitesException, InterruptedException {

        String airlinePnr = null;

        List<PNRReply.OriginDestinationDetails.ItineraryInfo> itineraryInfoList = pnrReply.getOriginDestinationDetails().get(0).getItineraryInfo();

        if (itineraryInfoList != null && !itineraryInfoList.isEmpty()) {
            for (PNRReply.OriginDestinationDetails.ItineraryInfo itineraryInfo : itineraryInfoList) {
                if (itineraryInfo.getItineraryReservationInfo() != null && itineraryInfo.getItineraryReservationInfo().getReservation() != null) {
                    airlinePnr = itineraryInfo.getItineraryReservationInfo().getReservation().getControlNumber();
                }
            }
            pnrResponse.setAirlinePNR(airlinePnr);
            pnrResponse.setAirlinePNRMap(AmadeusHelper.readMultipleAirlinePNR(pnrReply));
        }

        if (airlinePnr == null) {

            Period period = new Period(new DateTime(currentDate), new DateTime(), PeriodType.seconds());

            if (period.getSeconds() >= 14) {

                pnrResponse.setAirlinePNRError(true);
                for (PNRReply.PnrHeader pnrHeader : pnrReply.getPnrHeader()) {
                    pnrResponse.setPnrNumber(pnrHeader.getReservationInfo().getReservation().getControlNumber());
                }
                throw new BaseCompassitesException("Simultaneous Changes Error");
            } else {
                Thread.sleep(3000);
                pnrReply = serviceHandler.ignoreAndRetrievePNR(amadeusSessionWrapper);
                getAirlinePnr(pnrReply, currentDate, pnrResponse, amadeusSessionWrapper, serviceHandler);
            }
        }

        return pnrReply;
    }

    //Checks the Seat Availability
    private void checkSegmentStatus(PNRReply pnrReply) throws BaseCompassitesException {
        for (PNRReply.OriginDestinationDetails originDestinationDetails : pnrReply.getOriginDestinationDetails()) {
            for (PNRReply.OriginDestinationDetails.ItineraryInfo itineraryInfo : originDestinationDetails.getItineraryInfo()) {
                for (String status : itineraryInfo.getRelatedProduct().getStatus()) {
                    if (!AmadeusConstants.SEGMENT_HOLDING_CONFIRMED.equalsIgnoreCase(status)) {
                        logger.debug("No Seats Available as segment status is : {}", status);
                        throw new BaseCompassitesException(BaseCompassitesException.ExceptionCode.NO_SEAT.getExceptionCode());
                    }
                }
            }
        }
    }

    //Creates Amadeus Pax Reference
    private static List<AmadeusPaxInformation> createAmadeusPaxRefInfo(PNRReply gdsPNRReply) {

        List<AmadeusPaxInformation> amadeusPaxInformationList = new ArrayList<>();
        List<PNRReply.TravellerInfo> travellerInfoList = gdsPNRReply.getTravellerInfo();
        for (PNRReply.TravellerInfo travellerInfo : travellerInfoList) {
            amadeusPaxInformationList.add(AmadeusBookingHelper.extractPassengerData(travellerInfo));
        }

        return amadeusPaxInformationList;
    }

}
