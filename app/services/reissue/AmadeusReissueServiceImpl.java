package services.reissue;

import com.amadeus.xml.fatcer_13_1_1a.AttributeInformationType;
import com.amadeus.xml.fatcer_13_1_1a.AttributeType94871S;
import com.amadeus.xml.fatcer_13_1_1a.TicketCheckEligibilityReply;
import com.amadeus.xml.fatcer_13_1_1a.TravelFlightInformationType;
import com.amadeus.xml.pnracc_11_3_1a.PNRReply;
import com.amadeus.xml.tatres_20_1_1a.CouponInformationDetailsTypeI;
import com.amadeus.xml.tatres_20_1_1a.CouponInformationTypeI;
import com.amadeus.xml.tatres_20_1_1a.TicketProcessEDocReply;
import com.compassites.GDSWrapper.amadeus.ServiceHandler;
import com.compassites.model.ErrorMessage;
import com.compassites.model.SearchResponse;
import dto.reissue.ReIssueTicketRequest;
import models.AmadeusSessionWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AmadeusReissueServiceImpl implements AmadeusReissueService {

    private static final Logger logger = LoggerFactory.getLogger("gds");

    @Autowired
    private ReIssueFlightSearch reIssueFlightSearch;

    @Override
    public SearchResponse reIssueTicket(ReIssueTicketRequest reIssueTicketRequest) {

        ServiceHandler serviceHandler = null;
        AmadeusSessionWrapper amadeusSessionWrapper = null;

        SearchResponse reissueSearchResponse = null;

        try {
            serviceHandler = new ServiceHandler();
//            amadeusSessionWrapper = serviceHandler.logIn();
            amadeusSessionWrapper = serviceHandler.logIn("DELVS38LF");

            //1. Retrieving the PNR
            PNRReply pnrReply = serviceHandler.retrivePNR(reIssueTicketRequest.getGdsPNR(), amadeusSessionWrapper);

            //2. Checking the status of the ticket here
            TicketProcessEDocReply reIssueCheckTicketStatus = serviceHandler.reIssueCheckTicketStatus(reIssueTicketRequest, amadeusSessionWrapper);
            reissueSearchResponse = checkIfTicketStatusIsOpen(reIssueCheckTicketStatus, reIssueTicketRequest.getGdsPNR());

            //3. Checking the Eligibility if ticket status is open, else sending error message;
            TicketCheckEligibilityReply checkEligibilityReply;
            if (reissueSearchResponse.getErrorMessageList().isEmpty()) {
                checkEligibilityReply = serviceHandler.reIssueTicketCheckEligibility(reIssueTicketRequest, amadeusSessionWrapper);
            } else {
                return reissueSearchResponse;
            }

            //Checking the eligibility here
            reissueSearchResponse = checkIfTicketIsAllowedForReIssuance(checkEligibilityReply, reIssueTicketRequest.getGdsPNR());

            //4. Initiated ATC search for allowed carriers
            if (reissueSearchResponse.getErrorMessageList().isEmpty()) {
                TravelFlightInformationType allowedCarriers = checkEligibilityReply.getAllowedCarriers();
                reissueSearchResponse = reIssueFlightSearch.reIssueFlightSearch(reIssueTicketRequest, allowedCarriers, amadeusSessionWrapper);
            } else {
                return reissueSearchResponse;
            }


        } catch (Exception e) {
            logger.debug("Error with reissue : {}", e.getMessage(), e);
            serviceHandler.logOut(amadeusSessionWrapper);
        }

        return reissueSearchResponse;

    }


    //TODO: Below logic for all pax and all ticket reissue hence checking the coupon eligibility for all pax and each segment (for date change), will make the necessary changes for new requirements
    private static SearchResponse checkIfTicketStatusIsOpen(TicketProcessEDocReply reIssueCheckTicketStatus, String gdsPnr) {

        SearchResponse reissueSearchResponse = new SearchResponse();

        outerLoop:
        for (TicketProcessEDocReply.DocGroup docGroup : reIssueCheckTicketStatus.getDocGroup()) {
            for (TicketProcessEDocReply.DocGroup.DocDetailsGroup docDetailsGroup : docGroup.getDocDetailsGroup()) {
                String ticketNumber = docDetailsGroup.getDocInfo().getDocumentDetails().getNumber();

                for (TicketProcessEDocReply.DocGroup.DocDetailsGroup.CouponGroup couponGroup : docDetailsGroup.getCouponGroup()) {
                    CouponInformationTypeI couponInfo = couponGroup.getCouponInfo();

                    for (CouponInformationDetailsTypeI couponDetails : couponInfo.getCouponDetails()) {

                        String couponStatus = couponDetails.getCpnStatus();

                        // If the coupon status is not "I", the ticket is not open for reissue
                        if (!"I".equalsIgnoreCase(couponStatus)) {

                            ErrorMessage errorMessage = new ErrorMessage();
                            errorMessage.setProvider("Amadeus");
                            errorMessage.setType(ErrorMessage.ErrorType.ERROR);
                            errorMessage.setGdsPNR(gdsPnr);
                            errorMessage.setTicketNumber(ticketNumber);
                            errorMessage.setMessage("Ticket status for ticketNumber : " + ticketNumber + " is : " + couponStatus);

                            reissueSearchResponse.getErrorMessageList().add(errorMessage);
                            break outerLoop;
                        }
                    }
                }
            }
        }

        return reissueSearchResponse;
    }


    //TODO: Below logic for all pax and all ticket reissue hence checking the coupon eligibility for all pax and each segment (for date change), will make the necessary changes for new requirements
    private static SearchResponse checkIfTicketIsAllowedForReIssuance(TicketCheckEligibilityReply checkEligibilityReply, String gdsPnr) {

        SearchResponse reissueSearchResponse = new SearchResponse();

        outerLoop:
        for (TicketCheckEligibilityReply.EligibilityInfo eligibilityInfo : checkEligibilityReply.getEligibilityInfo()) {
            AttributeType94871S generalEligibilityInfo = eligibilityInfo.getGeneralEligibilityInfo();

            for (AttributeInformationType eligibilityId : generalEligibilityInfo.getEligibilityId()) {

                String eligibilityType = eligibilityId.getEligibilityType();
                String eligibilityValue = eligibilityId.getEligibilityValue();

                if ("CH".equals(eligibilityType) && !"Y".equalsIgnoreCase(eligibilityValue)) {


                    ErrorMessage errorMessage = new ErrorMessage();
                    errorMessage.setProvider("Amadeus");
                    errorMessage.setType(ErrorMessage.ErrorType.ERROR);
                    errorMessage.setGdsPNR(gdsPnr);
                    errorMessage.setMessage("Ticket Not Eligible for ReIssue");

                    reissueSearchResponse.getErrorMessageList().add(errorMessage);
                    break outerLoop;
                }
            }
        }

        return reissueSearchResponse;
    }



}
