package services.reissue;

import com.amadeus.xml._2010._06.fareinternaltypes_v2.PricingRecordType;
import com.amadeus.xml._2010._06.retailing_types_v2.*;
import com.amadeus.xml._2010._06.ticket_rebookandrepricepnr_v1.AMATicketRebookAndRepricePNRRS;
import com.amadeus.xml.pnracc_14_1_1a.PNRReply;
import com.amadeus.xml.ttstrr_13_1_1a.TicketDisplayTSTReply;
import com.compassites.GDSWrapper.amadeus.ReIssueConfirmationHandler;
import com.compassites.GDSWrapper.amadeus.ServiceHandler;
import com.compassites.model.*;
import dto.reissue.ReIssueConfirmationRequest;
import models.AmadeusSessionWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import utils.*;

import java.util.*;

@Component
public class ReIssueBookingServiceImpl implements ReIssueBookingService {

    private final ReIssueConfirmationHandler reIssueConfirmationHandler;
    private final ServiceHandler commonServiceHandler;
    private final AmadeusSessionManager amadeusSessionManager;
    private final ReIssueHelper reIssueHelper;

    @Autowired
    public ReIssueBookingServiceImpl(ReIssueConfirmationHandler reIssueConfirmationHandler, ServiceHandler commonServiceHandler,
                                     AmadeusSessionManager amadeusSessionManager, ReIssueHelper reIssueHelper) {
        this.reIssueConfirmationHandler = reIssueConfirmationHandler;
        this.commonServiceHandler = commonServiceHandler;
        this.amadeusSessionManager = amadeusSessionManager;
        this.reIssueHelper = reIssueHelper;
    }

    static Logger logger = LoggerFactory.getLogger("gds");

    @Override
    public PNRResponse confirmReissue(ReIssueConfirmationRequest reIssueConfirmationRequest, String officeId, PNRResponse pnrResponse) {

        AmadeusSessionWrapper amadeusSessionWrapper;
        boolean isReissueSuccess = true;

        String pnrToBeReissued = pnrResponse.getPnrNumber();
        try {
            amadeusSessionWrapper = commonServiceHandler.logIn("DELVS38LF", true);

            //Retrieving PNR here for stateful operation
            commonServiceHandler.retrievePNR(pnrToBeReissued, amadeusSessionWrapper);

            //Getting SegmentWiseClassInfo
            PAXFareDetails paxFareDetailsForSegmentInfo = reIssueConfirmationRequest.getNewTravellerMasterInfo().getItinerary().getReIssuePricingInformation().getPaxWisePricing().get(0).getPaxFareDetails();
            List<String> segmentWiseClassInfo = reIssueHelper.getBookingClassForSegmentsToBeReissued(paxFareDetailsForSegmentInfo, reIssueConfirmationRequest.getSelectedSegmentList());

            //ATC Book Call her
            AMATicketRebookAndRepricePNRRS ticketRebookAndRepricePNRRS = reIssueConfirmationHandler.rebookAndRepricePNR(reIssueConfirmationRequest, pnrToBeReissued, segmentWiseClassInfo, amadeusSessionWrapper);

            //Handling Failure from ATC API here
            AMATicketRebookAndRepricePNRRS.Failure failure = ticketRebookAndRepricePNRRS.getFailure();
            if (failure != null) {
                commonServiceHandler.logOut(amadeusSessionWrapper);

                ErrorsType errors = failure.getErrors();
                List<ErrorType> error = errors.getError();
                reIssueHelper.createErrorMessage(error, pnrResponse);

                if (!reIssueConfirmationRequest.isToSplit()) {
                    return pnrResponse;
                }

                reIssueHelper.createPNRResponseForReIssueFailedBooking(officeId, commonServiceHandler, pnrResponse, amadeusSessionManager, reIssueConfirmationRequest.isSeaman());
                return pnrResponse;
            }

            //ATC API Success flow here
            AMATicketRebookAndRepricePNRRS.Success success = ticketRebookAndRepricePNRRS.getSuccess();
            if (success != null) {

                //Handling warnings on success here
                WarningsType successWarnings = success.getWarnings();
                if (successWarnings != null) {

                    commonServiceHandler.logOut(amadeusSessionWrapper);

                    List<ErrorType> warning = successWarnings.getWarning();
                    reIssueHelper.createErrorMessage(warning, pnrResponse);

                    if (!reIssueConfirmationRequest.isToSplit()) {
                        return pnrResponse;
                    }

                    reIssueHelper.createPNRResponseForReIssueFailedBooking(officeId, commonServiceHandler, pnrResponse, amadeusSessionManager, reIssueConfirmationRequest.isSeaman());
                    return pnrResponse;
                }

                String reIssuedPnr = success.getReservation().getBookingIdentifier();
                pnrResponse.setPnrNumber(reIssuedPnr);

                //PNR is saved only if the reissue is successful else the newly split/Original PNR is unmodified
                commonServiceHandler.savePNR(amadeusSessionWrapper);

                commonServiceHandler.logOut(amadeusSessionWrapper);
            }

            //Creating PNR response here
            reIssueHelper.createPNRResponseForReIssuedBooking(officeId, commonServiceHandler, pnrResponse, reIssueConfirmationRequest.getNewTravellerMasterInfo(), amadeusSessionManager, success);
            pnrResponse.setReIssueSuccess(isReissueSuccess);

            return pnrResponse;
        } catch (Exception e) {
            logger.debug("Error when trying to book the flight for reissue {}", e.getMessage(), e);
        }

        return null;
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