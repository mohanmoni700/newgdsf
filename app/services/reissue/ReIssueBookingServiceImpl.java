package services.reissue;

import com.amadeus.xml._2010._06.fareinternaltypes_v2.PricingRecordType;
import com.amadeus.xml._2010._06.retailing_types_v2.*;
import com.amadeus.xml._2010._06.ticket_rebookandrepricepnr_v1.AMATicketRebookAndRepricePNRRS;
import com.amadeus.xml.pnracc_11_3_1a.PNRReply;
import com.amadeus.xml.ttstrr_13_1_1a.TicketDisplayTSTReply;
import com.compassites.GDSWrapper.amadeus.ReIssueConfirmationHandler;
import com.compassites.GDSWrapper.amadeus.ServiceHandler;
import com.compassites.constants.AmadeusConstants;
import com.compassites.constants.StaticConstatnts;
import com.compassites.exceptions.BaseCompassitesException;
import com.compassites.model.*;
import com.compassites.model.amadeus.AmadeusPaxInformation;
import com.compassites.model.traveller.TravellerMasterInfo;
import dto.reissue.ReIssueConfirmationRequest;
import models.AmadeusSessionWrapper;
import models.FlightSearchOffice;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.PeriodType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import play.libs.Json;
import utils.AmadeusBookingHelper;
import utils.AmadeusHelper;
import utils.AmadeusSessionManager;
import utils.ErrorMessageHelper;

import java.util.*;

@Component
public class ReIssueBookingServiceImpl implements ReIssueBookingService {

    private final ReIssueConfirmationHandler reIssueConfirmationHandler;
    private final ServiceHandler serviceHandler;
    private final AmadeusSessionManager amadeusSessionManager;

    @Autowired
    public ReIssueBookingServiceImpl(ReIssueConfirmationHandler reIssueConfirmationHandler, ServiceHandler serviceHandler, AmadeusSessionManager amadeusSessionManager) {
        this.reIssueConfirmationHandler = reIssueConfirmationHandler;
        this.serviceHandler = serviceHandler;
        this.amadeusSessionManager = amadeusSessionManager;
    }

    static Logger logger = LoggerFactory.getLogger("gds");

    @Override
    public PNRResponse confirmReissue(ReIssueConfirmationRequest reIssueConfirmationRequest, String officeId, PNRResponse finalPnrResponse) {

        AmadeusSessionWrapper session;
        boolean isReissueSuccess = true;

        String pnrToBeReissued = finalPnrResponse.getPnrNumber();
        try {

            session = serviceHandler.logIn("DELVS38LF");

            //Retrieving PNR here for stateful operation
            serviceHandler.retrivePNR(pnrToBeReissued, session);

            //Getting SegmentWiseClassInfo
            PAXFareDetails paxFareDetailsForSegmentInfo = reIssueConfirmationRequest.getNewTravellerMasterInfo().getItinerary().getReIssuePricingInformation().getPaxWisePricing().get(0).getPaxFareDetails();
            List<String> segmentWiseClassInfo = getBookingClassForSegmentsToBeReissued(paxFareDetailsForSegmentInfo);

            AMATicketRebookAndRepricePNRRS ticketRebookAndRepricePNRRS = reIssueConfirmationHandler.rebookAndRepricePNR(reIssueConfirmationRequest, pnrToBeReissued, segmentWiseClassInfo, session);

            //Handling Reissue failures here
            AMATicketRebookAndRepricePNRRS.Failure rebookAndRepricePNRRSFailure = ticketRebookAndRepricePNRRS.getFailure();
            if (rebookAndRepricePNRRSFailure != null) {
                serviceHandler.logOut(session);

                ErrorsType failureErrors = rebookAndRepricePNRRSFailure.getErrors();
                List<ErrorType> error = failureErrors.getError();
                createErrorMessage(error, finalPnrResponse);
                createPNRResponseForReIssueFailedBooking(officeId, serviceHandler, finalPnrResponse, amadeusSessionManager, reIssueConfirmationRequest.isSeaman());

                return finalPnrResponse;
            }


            //Handling Successful Reissue here
            AMATicketRebookAndRepricePNRRS.Success rebookAndRepricePNRRSSuccess = ticketRebookAndRepricePNRRS.getSuccess();
            if (rebookAndRepricePNRRSSuccess != null) {

                //Handling warnings on success here
                WarningsType successWarnings = rebookAndRepricePNRRSSuccess.getWarnings();
                if (successWarnings != null) {
                    serviceHandler.logOut(session);

                    List<ErrorType> warning = successWarnings.getWarning();
                    createErrorMessage(warning, finalPnrResponse);
                    createPNRResponseForReIssueFailedBooking(officeId, serviceHandler, finalPnrResponse, amadeusSessionManager, reIssueConfirmationRequest.isSeaman());

                    return finalPnrResponse;
                }

                String reIssuedPnr = rebookAndRepricePNRRSSuccess.getReservation().getBookingIdentifier();
                finalPnrResponse.setPnrNumber(reIssuedPnr);

                // Saving the PNR only if the reissue was successful without any warnings
                serviceHandler.savePNR(session);
                serviceHandler.logOut(session);

            }

            //Creating PNR response here
            createPNRResponseForReIssuedBooking(officeId, serviceHandler, finalPnrResponse, reIssueConfirmationRequest.getNewTravellerMasterInfo(), amadeusSessionManager, rebookAndRepricePNRRSSuccess);
            finalPnrResponse.setReIssueSuccess(isReissueSuccess);

            return finalPnrResponse;

        } catch (Exception e) {
            logger.debug("Error when trying to book the flight for reissue {}", e.getMessage(), e);
        }

        return null;
    }

    //Gets segment wise booking class
    private static List<String> getBookingClassForSegmentsToBeReissued(PAXFareDetails paxFareDetails) {

        List<String> segmentBookingClassList = new ArrayList<>();
        List<FareJourney> journeyFareList = paxFareDetails.getFareJourneyList();

        for (FareJourney journeyFares : journeyFareList) {
            List<FareSegment> segmentFareList = journeyFares.getFareSegmentList();
            for (FareSegment fareSegment : segmentFareList) {
                segmentBookingClassList.add(fareSegment.getBookingClass());
            }
        }

        return segmentBookingClassList;
    }

    //PNR Response for Reissued Bookings
    private void createPNRResponseForReIssuedBooking(String officeId, ServiceHandler serviceHandler, PNRResponse pnrResponse, TravellerMasterInfo travellerMasterInfo, AmadeusSessionManager amadeusSessionManager, AMATicketRebookAndRepricePNRRS.Success success) {

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

            createPNRResponseForReissuedPNR(gdsPNRReply, pnrResponse, success);

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
    private void createPNRResponseForReIssueFailedBooking(String officeID, ServiceHandler serviceHandler, PNRResponse pnrResponse, AmadeusSessionManager amadeusSessionManager, boolean isSeamen) {

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

            pnrResponse.setPricingInfo(pricingInfo);
            pnrResponse.setCreationOfficeId(gdsPNRReply.getSecurityInformation().getSecondRpInformation().getCreationOfficeId());
            pnrResponse.setAmadeusPaxReference(createAmadeusPaxRefInfo(gdsPNRReply));
            pnrResponse.setPnrNumber(gdsPNRReply.getPnrHeader().get(0).getReservationInfo().getReservation().getControlNumber());


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
    private PNRReply getAirlinePnr(PNRReply pnrReply, Date currentDate, PNRResponse pnrResponse, AmadeusSessionWrapper amadeusSessionWrapper, ServiceHandler serviceHandler) throws BaseCompassitesException, InterruptedException {

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

    //Creates PNR Response (Is this Needed?)
    private void createPNRResponseForReissuedPNR(PNRReply pnrReply, PNRResponse pnrResponse, AMATicketRebookAndRepricePNRRS.Success success) {



        pnrResponse.setPnrNumber(pnrReply.getPnrHeader().get(0).getReservationInfo().getReservation().getControlNumber());


        //Creating Amadeus Pax Reference and Line number here
        pnrResponse.setAmadeusPaxReference(createAmadeusPaxRefInfo(pnrReply));

        pnrResponse.setFlightAvailable(true);
        if (pnrReply.getSecurityInformation() != null && pnrReply.getSecurityInformation().getSecondRpInformation() != null) {
            pnrResponse.setCreationOfficeId(pnrReply.getSecurityInformation().getSecondRpInformation().getCreationOfficeId());
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

    //Creates Error Message for Reissue Success warnings and Reissue Failures
    private static void createErrorMessage(List<ErrorType> warnings, PNRResponse finalPnrResponse) {

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


    private static void getValidTillDate(AMATicketRebookAndRepricePNRRS.Success success) {

        AMATicketRebookAndRepricePNRRS.Success.Repricing.ItineraryRepricing.PricingDetails.PricingRecords pricingRecords = success.getRepricing().getItineraryRepricing().getPricingDetails().getPricingRecords();
        List<PricingRecordType> pricingRecordList = pricingRecords.getPricingRecord();

        String validTill = null;

        outerForLoop:
        for (PricingRecordType pricingRecord : pricingRecordList) {
            List<PricingRecordType.Coupon> coupons = pricingRecord.getCoupon();
            for (PricingRecordType.Coupon coupon : coupons) {
                List<PricingRecordType.Coupon.DateValidity> dateValidityList = coupon.getDateValidity();
                for (PricingRecordType.Coupon.DateValidity dateValidity : dateValidityList) {
                    String type = dateValidity.getType();
                    if (type.equalsIgnoreCase("NVA")) {
                        validTill = String.valueOf(dateValidity.getValue());
                        break outerForLoop;
                    }
                }
            }
        }
    }

}
