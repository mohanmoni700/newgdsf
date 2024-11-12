package services.reissue;

import com.amadeus.xml.fatcer_13_1_1a.AttributeInformationType;
import com.amadeus.xml.fatcer_13_1_1a.TicketCheckEligibilityReply;
import com.amadeus.xml.fatcer_13_1_1a.TravelFlightInformationType;
import com.amadeus.xml.pnracc_11_3_1a.PNRReply;
import com.amadeus.xml.tatres_20_1_1a.CouponInformationDetailsTypeI;
import com.amadeus.xml.tatres_20_1_1a.TicketProcessEDocReply;
import com.compassites.GDSWrapper.amadeus.ServiceHandler;
import com.compassites.model.ErrorMessage;
import com.compassites.model.SearchResponse;
import dto.reissue.ReIssueSearchRequest;
import models.AmadeusSessionWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AmadeusReissueServiceImpl implements AmadeusReissueService {

    private static final Logger logger = LoggerFactory.getLogger("gds");

    @Autowired
    private ReIssueFlightSearch reIssueFlightSearch;

    @Override
    public SearchResponse reIssueTicket(ReIssueSearchRequest reIssueSearchRequest) {

        ServiceHandler serviceHandler = null;
        AmadeusSessionWrapper amadeusSessionWrapper = null;

        SearchResponse reissueSearchResponse = null;

        try {
            serviceHandler = new ServiceHandler();
//            amadeusSessionWrapper = serviceHandler.logIn();
            amadeusSessionWrapper = serviceHandler.logIn("DELVS38LF");

            //1. Retrieving the PNR
            PNRReply pnrReply = serviceHandler.retrivePNR(reIssueSearchRequest.getGdsPNR(), amadeusSessionWrapper);

            //2. Checking the status of the ticket here
            TicketProcessEDocReply reIssueCheckTicketStatus = serviceHandler.reIssueCheckTicketStatus(reIssueSearchRequest, amadeusSessionWrapper);
            reissueSearchResponse = checkIfTicketStatusIsOpen(reIssueCheckTicketStatus, reIssueSearchRequest.getGdsPNR());

            //3. Checking the Eligibility if ticket status is open, else sending error message;
            TicketCheckEligibilityReply checkEligibilityReply;
            if (reissueSearchResponse.getErrorMessageList().isEmpty()) {
                checkEligibilityReply = serviceHandler.reIssueTicketCheckEligibility(reIssueSearchRequest, amadeusSessionWrapper);
            } else {
                return reissueSearchResponse;
            }

            //Checking the eligibility here
            reissueSearchResponse = checkIfTicketIsAllowedForReIssuance(checkEligibilityReply, reIssueSearchRequest.getGdsPNR());

            //4. Initiated ATC search for allowed carriers
            if (reissueSearchResponse.getErrorMessageList().isEmpty()) {
                TravelFlightInformationType allowedCarriers = checkEligibilityReply.getAllowedCarriers();
                reissueSearchResponse = reIssueFlightSearch.reIssueFlightSearch(reIssueSearchRequest, allowedCarriers, amadeusSessionWrapper);
            } else {
                return reissueSearchResponse;
            }


        } catch (Exception e) {
            logger.debug("Error with reissue : {}", e.getMessage(), e);
            serviceHandler.logOut(amadeusSessionWrapper);
            if (e.getMessage().contains("Application|NO MATCH FOR RECORD LOCATOR")) {
                reissueSearchResponse = new SearchResponse();
                ErrorMessage errorMessage = new ErrorMessage();
                errorMessage.setProvider("Amadeus");
                errorMessage.setType(ErrorMessage.ErrorType.ERROR);
                errorMessage.setGdsPNR(reIssueSearchRequest.getGdsPNR());
                errorMessage.setMessage("PNR Not On Record");
                reissueSearchResponse.getErrorMessageList().add(errorMessage);
            }
        }

        return reissueSearchResponse;

    }


    //TODO: Below logic for all pax and all ticket reissue hence checking the coupon eligibility for all pax and each segment (for date change), will make the necessary changes for new requirements
    private static SearchResponse checkIfTicketStatusIsOpen(TicketProcessEDocReply reIssueCheckTicketStatus, String gdsPnr) {

        SearchResponse reissueSearchResponse = new SearchResponse();

        List<TicketProcessEDocReply.DocGroup> docGroupList = reIssueCheckTicketStatus.getDocGroup();

        outerLoop:
        for (TicketProcessEDocReply.DocGroup docGroup : docGroupList) {
            List<TicketProcessEDocReply.DocGroup.DocDetailsGroup> docDetailsGroupList = docGroup.getDocDetailsGroup();

            for (TicketProcessEDocReply.DocGroup.DocDetailsGroup docDetailsGroup : docDetailsGroupList) {
                String ticketNumber = docDetailsGroup.getDocInfo().getDocumentDetails().getNumber();
                List<TicketProcessEDocReply.DocGroup.DocDetailsGroup.CouponGroup> couponGroupList = docDetailsGroup.getCouponGroup();

                for (TicketProcessEDocReply.DocGroup.DocDetailsGroup.CouponGroup couponGroup : couponGroupList) {
                    List<CouponInformationDetailsTypeI> couponDetailsList = couponGroup.getCouponInfo().getCouponDetails();

                    for (CouponInformationDetailsTypeI couponDetails : couponDetailsList) {

                        String couponStatus = couponDetails.getCpnStatus();

                        // If the coupon status is not "I", the ticket is not open for reissue (I = Open Ticket)
                        if (!"I".equalsIgnoreCase(couponStatus)) {
                            ErrorMessage errorMessage = new ErrorMessage();
                            errorMessage.setProvider("Amadeus");
                            errorMessage.setType(ErrorMessage.ErrorType.ERROR);
                            errorMessage.setGdsPNR(gdsPnr);
                            errorMessage.setTicketNumber(ticketNumber);

                            switch (couponStatus) {
                                case "AL":
                                    errorMessage.setMessage("Status of Ticket Number : " + ticketNumber + " is : " + "AirPort Controlled");
                                    break;
                                case "RF":
                                    errorMessage.setMessage("Status of Ticket Number : " + ticketNumber + " is : " + "Refunded");
                                    break;
                                case "V":
                                    errorMessage.setMessage("Status of Ticket Number : " + ticketNumber + " is : " + "Voided");
                                    break;
                                default:
                                    errorMessage.setMessage("Status of Ticket Number : " + ticketNumber + " is : " + couponStatus);
                                    break;
                            }

                            reissueSearchResponse.getErrorMessageList().add(errorMessage);
//                            break outerLoop;
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
            List<AttributeInformationType> eligibilityIds = eligibilityInfo.getGeneralEligibilityInfo().getEligibilityId();

            boolean isChangeNotAllowed = false;
            boolean isChangeGeographyNotAllowed = false;

            for (AttributeInformationType eligibilityId : eligibilityIds) {

                String eligibilityType = eligibilityId.getEligibilityType();
                String eligibilityValue = eligibilityId.getEligibilityValue();

                isChangeNotAllowed = "CH".equals(eligibilityType) && !"Y".equalsIgnoreCase(eligibilityValue);
                isChangeGeographyNotAllowed = "CHGEO".equalsIgnoreCase(eligibilityType) && !"Y".equalsIgnoreCase(eligibilityValue);

                if (isChangeNotAllowed || isChangeGeographyNotAllowed) {

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
