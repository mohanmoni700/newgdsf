package services.reissue;

import com.amadeus.xml._2010._06.fareinternaltypes_v2.PricingRecordType;
import com.amadeus.xml._2010._06.retailing_types_v2.ErrorType;
import com.amadeus.xml._2010._06.retailing_types_v2.ErrorsType;
import com.amadeus.xml._2010._06.retailing_types_v2.WarningsType;
import com.amadeus.xml._2010._06.ticket_rebookandrepricepnr_v1.AMATicketRebookAndRepricePNRRS;
import com.amadeus.xml.pnracc_11_3_1a.PNRReply;
import com.compassites.GDSWrapper.amadeus.ReIssueConfirmationHandler;
import com.compassites.GDSWrapper.amadeus.ServiceHandler;
import com.compassites.constants.AmadeusConstants;
import com.compassites.constants.StaticConstatnts;
import com.compassites.exceptions.BaseCompassitesException;
import com.compassites.model.*;
import com.compassites.model.traveller.TravellerMasterInfo;
import com.compassites.model.amadeus.AmadeusPaxInformation;
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
    public PNRResponse confirmReissue(String newPnrToBeReIssued, ReIssueConfirmationRequest reIssueConfirmationRequest, FlightSearchOffice officeId) {

        AmadeusSessionWrapper amadeusSessionWrapper = null;
        boolean isReissueSuccess = true;
        PNRResponse finalPnrResponse = new PNRResponse();

        try {

            amadeusSessionWrapper = serviceHandler.logIn(officeId);

            //Retrieving PNR here for stateful operation
            serviceHandler.retrivePNR(newPnrToBeReIssued, amadeusSessionWrapper);

            //Getting SegmentWiseClassInfo
            PAXFareDetails paxFareDetails = reIssueConfirmationRequest.getNewTravellerMasterInfo().getItinerary().getReIssuePricingInformation().getPaxWisePricing().get(0).getPaxFareDetails();
            List<String> segmentWiseClassInfo = getBookingClassForSegments(paxFareDetails);

            AMATicketRebookAndRepricePNRRS ticketRebookAndRepricePNRRS = reIssueConfirmationHandler.rebookAndRepricePNR(reIssueConfirmationRequest, newPnrToBeReIssued, segmentWiseClassInfo, amadeusSessionWrapper);

            AMATicketRebookAndRepricePNRRS.Failure failure = ticketRebookAndRepricePNRRS.getFailure();

            if (failure != null) {
                ErrorMessage errorMessage = new ErrorMessage();

                ErrorsType errors = failure.getErrors();
                List<ErrorType> error = errors.getError();
                for (ErrorType errorType : error) {
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
                        errorMessage.setGdsPNR(newPnrToBeReIssued);
                        finalPnrResponse.setReIssueSuccess(false);

                    }
                    finalPnrResponse.setErrorMessage(errorMessage);
                    break;
                }
                return finalPnrResponse;
            }

            AMATicketRebookAndRepricePNRRS.Success success = ticketRebookAndRepricePNRRS.getSuccess();
            String reIssuedPnr = newPnrToBeReIssued;
            if (success != null) {
                reIssuedPnr = success.getReservation().getBookingIdentifier();

                success = ticketRebookAndRepricePNRRS.getSuccess();
                WarningsType warnings = success.getWarnings();

                if (warnings != null) {
                    ErrorMessage errorMessage = new ErrorMessage();

                    List<ErrorType> warning = warnings.getWarning();
                    for (ErrorType errorType : warning) {
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
                            errorMessage.setGdsPNR(reIssuedPnr);
                            finalPnrResponse.setReIssueSuccess(false);

                        }
                        finalPnrResponse.setErrorMessage(errorMessage);
                        break;
                    }
                    return finalPnrResponse;
                }
                serviceHandler.savePNR(amadeusSessionWrapper);
                serviceHandler.logOut(amadeusSessionWrapper);
            }

            createPNRResponseForReIssuedBooking(reIssuedPnr, officeId, serviceHandler, finalPnrResponse, reIssueConfirmationRequest.getNewTravellerMasterInfo(), amadeusSessionManager, success);

            finalPnrResponse.setReIssueSuccess(isReissueSuccess);
            finalPnrResponse.setPnrSplit(reIssueConfirmationRequest.isToSplit());

            return finalPnrResponse;

        } catch (Exception e) {
            logger.debug("Error when trying to book the flight for reissue {}", e.getMessage(), e);
        }

        return null;
    }


    private static List<String> getBookingClassForSegments(PAXFareDetails paxFareDetails) {

        List<String> bookingClassList = new ArrayList<>();
        List<FareJourney> fareJourneyList = paxFareDetails.getFareJourneyList();

        for (FareJourney fareJourney : fareJourneyList) {
            List<FareSegment> fareSegmentList = fareJourney.getFareSegmentList();
            for (FareSegment fareSegment : fareSegmentList) {
                bookingClassList.add(fareSegment.getBookingClass());
            }
        }

        return bookingClassList;
    }


    private void createPNRResponseForReIssuedBooking(String gdsPnr, FlightSearchOffice officeId, ServiceHandler serviceHandler, PNRResponse pnrResponse, TravellerMasterInfo travellerMasterInfo, AmadeusSessionManager amadeusSessionManager, AMATicketRebookAndRepricePNRRS.Success success) throws BaseCompassitesException, InterruptedException {

        PNRReply gdsPNRReply = null;
        AmadeusSessionWrapper amadeusSessionWrapper = null;
        try {

            try {
                amadeusSessionWrapper = serviceHandler.logIn(officeId);
                gdsPNRReply = serviceHandler.retrivePNR(gdsPnr, amadeusSessionWrapper);
            } catch (NullPointerException e) {
                logger.error("error in Retrieve PNR{}", e.getMessage());
            } catch (Exception ex) {
                if (ex.getMessage().contains("IGNORE")) {
                    gdsPNRReply = serviceHandler.ignoreAndRetrievePNR(amadeusSessionWrapper);
                }
            }

            Date lastPNRAddMultiElements = new Date();
            gdsPNRReply = readAirlinePNR(gdsPNRReply, lastPNRAddMultiElements, pnrResponse, amadeusSessionWrapper, serviceHandler);
            checkSegmentStatus(gdsPNRReply);

            List<String> segmentNumbers = new ArrayList<>();
            for (PNRReply.OriginDestinationDetails originDestinationDetails : gdsPNRReply.getOriginDestinationDetails()) {
                for (PNRReply.OriginDestinationDetails.ItineraryInfo itineraryInfo : originDestinationDetails.getItineraryInfo()) {
                    segmentNumbers.add("" + itineraryInfo.getElementManagementItinerary().getReference().getNumber());
                }
            }

            Map<String, String> travellerMap = new HashMap<>();
            for (PNRReply.TravellerInfo travellerInfo : gdsPNRReply.getTravellerInfo()) {
                String keyNo = "" + travellerInfo.getElementManagementPassenger().getReference().getNumber();
                String lastName = travellerInfo.getPassengerData().get(0).getTravellerInformation().getTraveller().getSurname();
                String name = travellerInfo.getPassengerData().get(0).getTravellerInformation().getPassenger().get(0).getFirstName();
                String travellerName = name + lastName;
                travellerName = travellerName.replaceAll("\\s+", "");
                travellerName = travellerName.toLowerCase();
                travellerMap.put(travellerName, keyNo);
            }

            //TODO: Null Pointer here (Or when split is not done(New PNR not created, do we need to add SSR again?)) Discuss
//            if (!pnrResponse.isPnrSplit()) {
//                addSSRDetailsToPNR(travellerMasterInfo, 1, lastPNRAddMultiElements, segmentNumbers, travellerMap, amadeusSessionWrapper);
//            }
//            Thread.sleep(5000);
//            gdsPNRReply = serviceHandler.retrivePNR(gdsPnr, amadeusSessionWrapper);
            createPNRResponse(gdsPNRReply, pnrResponse, travellerMasterInfo, success);


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
                ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage("error", ErrorMessage.ErrorType.ERROR, PROVIDERS.AMADEUS.toString());
                pnrResponse.setErrorMessage(errorMessage);
            }
        } finally {
            if (amadeusSessionWrapper != null) {
                amadeusSessionManager.removeActiveSession(amadeusSessionWrapper.getmSession().value);
                serviceHandler.logOut(amadeusSessionWrapper);
            }
        }

        logger.info("generatePNR :" + Json.stringify(Json.toJson(pnrResponse)));
    }


    private PNRReply readAirlinePNR(PNRReply pnrReply, Date lastPNRAddMultiElements, PNRResponse pnrResponse, AmadeusSessionWrapper amadeusSessionWrapper, ServiceHandler serviceHandler) throws BaseCompassitesException, InterruptedException {

        List<PNRReply.OriginDestinationDetails.ItineraryInfo> itineraryInfos = pnrReply.getOriginDestinationDetails().get(0).getItineraryInfo();
        String airlinePnr = null;
        if (itineraryInfos != null && !itineraryInfos.isEmpty()) {
            for (PNRReply.OriginDestinationDetails.ItineraryInfo itineraryInfo : itineraryInfos) {
                if (itineraryInfo.getItineraryReservationInfo() != null && itineraryInfo.getItineraryReservationInfo().getReservation() != null) {
                    airlinePnr = itineraryInfo.getItineraryReservationInfo().getReservation().getControlNumber();
                }
            }
            pnrResponse.setAirlinePNR(airlinePnr);
            pnrResponse.setAirlinePNRMap(AmadeusHelper.readMultipleAirlinePNR(pnrReply));
        }
        if (airlinePnr == null) {
            Period period = new Period(new DateTime(lastPNRAddMultiElements), new DateTime(), PeriodType.seconds());
            if (period.getSeconds() >= 12) {
                pnrResponse.setAirlinePNRError(true);
                for (PNRReply.PnrHeader pnrHeader : pnrReply.getPnrHeader()) {
                    pnrResponse.setPnrNumber(pnrHeader.getReservationInfo().getReservation().getControlNumber());
                }
                throw new BaseCompassitesException("Simultaneous changes Error");
            } else {
                Thread.sleep(3000);
                pnrReply = serviceHandler.ignoreAndRetrievePNR(amadeusSessionWrapper);
                readAirlinePNR(pnrReply, lastPNRAddMultiElements, pnrResponse, amadeusSessionWrapper, serviceHandler);
            }
        }

        return pnrReply;
    }


    private void checkSegmentStatus(PNRReply pnrReply) throws BaseCompassitesException {
        for (PNRReply.OriginDestinationDetails originDestinationDetails : pnrReply.getOriginDestinationDetails()) {
            for (PNRReply.OriginDestinationDetails.ItineraryInfo itineraryInfo : originDestinationDetails.getItineraryInfo()) {
                for (String status : itineraryInfo.getRelatedProduct().getStatus()) {
                    if (!AmadeusConstants.SEGMENT_HOLDING_CONFIRMED.equalsIgnoreCase(status)) {
                        logger.debug("No Seats Available as segment status is : " + status);
                        throw new BaseCompassitesException(BaseCompassitesException.ExceptionCode.NO_SEAT.getExceptionCode());
                    }
                }
            }
        }
    }

//    private void simultaneousChangeAction(PNRReply addSSRResponse, Date lastPNRAddMultiElements, TravellerMasterInfo travellerMasterInfo, int iteration, List<String> segmentNumbers, Map<String, String> travellerMap, AmadeusSessionWrapper amadeusSessionWrapper) throws InterruptedException, BaseCompassitesException {
//
//        boolean simultaneousChangeToPNR = AmadeusBookingHelper.checkForSimultaneousChange(addSSRResponse);
//        if (simultaneousChangeToPNR) {
//            Period p = new Period(new DateTime(lastPNRAddMultiElements), new DateTime(), PeriodType.seconds());
//            if (p.getSeconds() >= 12) {
//                serviceHandler.ignorePNRAddMultiElement(amadeusSessionWrapper);
//                throw new BaseCompassitesException("Simultaneous changes Error");
//            } else {
//                Thread.sleep(3000);
//                PNRReply pnrReply = serviceHandler.ignoreAndRetrievePNR(amadeusSessionWrapper);
//                lastPNRAddMultiElements = new Date();
//                iteration = iteration + 1;
//                addSSRDetailsToPNR(travellerMasterInfo, iteration, lastPNRAddMultiElements, segmentNumbers, travellerMap, amadeusSessionWrapper);
//            }
//        }
//    }
//
//    private void addSSRDetailsToPNR(TravellerMasterInfo travellerMasterInfo, int iteration, Date lastPNRAddMultiElements, List<String> segmentNumbers, Map<String, String> travellerMap, AmadeusSessionWrapper amadeusSessionWrapper) throws BaseCompassitesException, InterruptedException {
//
//        if (iteration <= 3) {
//            PNRReply addSSRResponse = serviceHandler.addSSRDetailsToPNR(travellerMasterInfo, segmentNumbers, travellerMap, amadeusSessionWrapper);
//            simultaneousChangeAction(addSSRResponse, lastPNRAddMultiElements, travellerMasterInfo, iteration, segmentNumbers, travellerMap, amadeusSessionWrapper);
//            PNRReply savePNRReply = serviceHandler.savePNR(amadeusSessionWrapper);
//            simultaneousChangeAction(savePNRReply, lastPNRAddMultiElements, travellerMasterInfo, iteration, segmentNumbers, travellerMap, amadeusSessionWrapper);
//        } else {
//            serviceHandler.ignorePNRAddMultiElement(amadeusSessionWrapper);
//            throw new BaseCompassitesException("Simultaneous changes Error");
//        }
//    }

    public PNRResponse createPNRResponse(PNRReply gdsPNRReply, PNRResponse pnrResponse, TravellerMasterInfo travellerMasterInfo, AMATicketRebookAndRepricePNRRS.Success success) {

        pnrResponse.setPnrNumber(gdsPNRReply.getPnrHeader().get(0).getReservationInfo().getReservation().getControlNumber());

        //Creating Amadeus Pax Reference and Line number here
        pnrResponse.setAmadeusPaxReference(createAmadeusPaxRefInfo(gdsPNRReply));

        //Setting Valid Till date here
//        getValidTillDate(success,pnrResponse);

        pnrResponse.setFlightAvailable(true);
        if (gdsPNRReply.getSecurityInformation() != null && gdsPNRReply.getSecurityInformation().getSecondRpInformation() != null) {
            pnrResponse.setCreationOfficeId(gdsPNRReply.getSecurityInformation().getSecondRpInformation().getCreationOfficeId());
        }


        return pnrResponse;
    }

    private static List<AmadeusPaxInformation> createAmadeusPaxRefInfo(PNRReply gdsPNRReply) {

        List<AmadeusPaxInformation> amadeusPaxInformationList = new ArrayList<>();
        List<PNRReply.TravellerInfo> travellerInfoList = gdsPNRReply.getTravellerInfo();
        for (PNRReply.TravellerInfo travellerInfo : travellerInfoList) {
            amadeusPaxInformationList.add(AmadeusBookingHelper.extractPassengerData(travellerInfo));
        }

        return amadeusPaxInformationList;
    }

    //TODO: Have it in ddMMyyyy format
    private static void getValidTillDate(AMATicketRebookAndRepricePNRRS.Success success, PNRResponse pnrResponse) {

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

//        pnrResponse.setValidTillDate(validTill);
    }

}
