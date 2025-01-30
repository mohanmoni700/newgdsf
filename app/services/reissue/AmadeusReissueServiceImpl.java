package services.reissue;

import com.amadeus.xml.fatcer_13_1_1a.AttributeInformationType;
import com.amadeus.xml.fatcer_13_1_1a.TicketCheckEligibilityReply;
import com.amadeus.xml.fatcer_13_1_1a.TravelFlightInformationType;
import com.amadeus.xml.itares_05_2_ia.AirSellFromRecommendationReply;
import com.amadeus.xml.pnracc_11_3_1a.PNRReply;
import com.amadeus.xml.tarcpr_13_2_1a.TicketReissueConfirmedPricingReply;
import com.amadeus.xml.taripr_19_1_1a.TicketRepricePNRWithBookingClassReply;
import com.amadeus.xml.tatres_20_1_1a.CouponInformationDetailsTypeI;
import com.amadeus.xml.tatres_20_1_1a.TicketProcessEDocReply;
import com.amadeus.xml.tpcbrr_12_4_1a.FarePricePNRWithBookingClassReply;
import com.compassites.GDSWrapper.amadeus.ServiceHandler;
import com.compassites.model.*;
import com.compassites.model.traveller.TravellerMasterInfo;
import com.compassites.model.ErrorMessage;
import com.compassites.model.SearchResponse;
import dto.reissue.ReIssueConfirmationRequest;
import dto.reissue.ReIssueSearchRequest;
import models.AmadeusSessionWrapper;
import models.FlightSearchOffice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import play.Configuration;
import play.Play;
import play.libs.Json;
import services.AmadeusSourceOfficeService;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class AmadeusReissueServiceImpl implements AmadeusReissueService {

    private static final Logger logger = LoggerFactory.getLogger("gds");

    private final ReIssueFlightSearch reIssueFlightSearch;
    private final ReIssueBookingService reIssueBookingService;
    private final ServiceHandler serviceHandler;
    private final AmadeusSourceOfficeService amadeusSourceOfficeService;

    @Autowired
    public AmadeusReissueServiceImpl(
            ReIssueFlightSearch reIssueFlightSearch,
            ReIssueBookingService reIssueBookingService,
            ServiceHandler serviceHandler,
            AmadeusSourceOfficeService amadeusSourceOfficeService
    ) {
        this.reIssueFlightSearch = reIssueFlightSearch;
        this.reIssueBookingService = reIssueBookingService;
        this.serviceHandler = serviceHandler;
        this.amadeusSourceOfficeService = amadeusSourceOfficeService;
    }


    @Override
    public SearchResponse reIssueTicket(ReIssueSearchRequest reIssueSearchRequest) {

        ServiceHandler serviceHandler = null;
        AmadeusSessionWrapper amadeusSessionWrapper = null;

        SearchResponse reissueSearchResponse = null;

        try {
            serviceHandler = new ServiceHandler();
//            amadeusSessionWrapper = serviceHandler.logIn();
            amadeusSessionWrapper = serviceHandler.logIn(amadeusSourceOfficeService.getDelhiSourceOffice());

            //1. Retrieving the PNR
            PNRReply pnrReply = serviceHandler.retrivePNR(reIssueSearchRequest.getGdsPNR(), amadeusSessionWrapper);

            //2. Checking the status of the ticket here
            TicketProcessEDocReply reIssueCheckTicketStatus = serviceHandler.reIssueCheckTicketStatus(reIssueSearchRequest, amadeusSessionWrapper);
            reissueSearchResponse = checkIfTicketStatusIsOpen(reIssueCheckTicketStatus, reIssueSearchRequest.getGdsPNR(), reIssueSearchRequest.getTicketNumberList());

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
    private static SearchResponse checkIfTicketStatusIsOpen(TicketProcessEDocReply reIssueCheckTicketStatus, String gdsPnr, List<String> ticketNumbers) {

        SearchResponse reissueSearchResponse = new SearchResponse();

        List<TicketProcessEDocReply.DocGroup> docGroupList = reIssueCheckTicketStatus.getDocGroup();


        for (TicketProcessEDocReply.DocGroup docGroup : docGroupList) {
            List<TicketProcessEDocReply.DocGroup.DocDetailsGroup> docDetailsGroupList = docGroup.getDocDetailsGroup();

            for (TicketProcessEDocReply.DocGroup.DocDetailsGroup docDetailsGroup : docDetailsGroupList) {
                String ticketNumber = docDetailsGroup.getDocInfo().getDocumentDetails().getNumber();

                if (ticketNumbers.contains(ticketNumber)) {
                    List<TicketProcessEDocReply.DocGroup.DocDetailsGroup.CouponGroup> couponGroupList = docDetailsGroup.getCouponGroup();

                    for (TicketProcessEDocReply.DocGroup.DocDetailsGroup.CouponGroup couponGroup : couponGroupList) {
                        List<CouponInformationDetailsTypeI> couponDetailsList = couponGroup.getCouponInfo().getCouponDetails();

                        for (CouponInformationDetailsTypeI couponDetails : couponDetailsList) {

                            String couponStatus = couponDetails.getCpnStatus();

                            //TODO: To handle Airport Control 24hrs status here as per new requirement.
                            //If the coupon status is not "I", the ticket is not open for reissue (I = Open Ticket)
                            if (!"I".equalsIgnoreCase(couponStatus)) {
                                ErrorMessage errorMessage = new ErrorMessage();
                                errorMessage.setProvider("Amadeus");
                                errorMessage.setType(ErrorMessage.ErrorType.ERROR);
                                errorMessage.setGdsPNR(gdsPnr);
                                errorMessage.setTicketNumber(ticketNumber);

                                switch (couponStatus) {
                                    case "AL":
                                        errorMessage.setMessage("Status of Ticket Number  " + ticketNumber + " is  " + "AirPort Controlled");
                                        break;
                                    case "RF":
                                        errorMessage.setMessage("Status of Ticket Number  " + ticketNumber + " is  " + "Refunded");
                                        break;
                                    case "V":
                                        errorMessage.setMessage("Status of Ticket Number  " + ticketNumber + " is  " + "Voided");
                                        break;
                                    default:
                                        errorMessage.setMessage("Status of Ticket Number  " + ticketNumber + " is  " + couponStatus);
                                        break;
                                }

                                if (!reissueSearchResponse.getErrorMessageList().contains(errorMessage)) {
                                    reissueSearchResponse.getErrorMessageList().add(errorMessage);
                                }
                            }
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

            boolean isChangeNotAllowed;
            boolean isChangeGeographyNotAllowed;

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

    @Override
    public PNRResponse confirmReIssue(ReIssueConfirmationRequest reIssueConfirmationRequest) {

        ServiceHandler serviceHandler = null;
        PNRResponse reIssuePNRResponse = null;

        try {

            serviceHandler = new ServiceHandler();
            FlightSearchOffice officeId = null;

            //Dynamic Office ID here
//            if (reIssueConfirmationRequest.getOfficeId().equalsIgnoreCase("BOMVS34C3")) {
//                officeId = amadeusSourceOfficeService.getPrioritySourceOffice();
//            } else if (reIssueConfirmationRequest.getOfficeId().equalsIgnoreCase("DELVS38LF")) {
            officeId = amadeusSourceOfficeService.getDelhiSourceOffice();
//            }
//            String pnrToBeReIssued;
//
//            pnrToBeReIssued = reIssueConfirmationRequest.getOriginalGdsPnr();
//
//            //Splitting and generating a new PNR to be reissued here (Login And LogOut as the Ticket_RebookAndRepricePNR is stateful and needs a fresh session)
//            if (reIssueConfirmationRequest.isToSplit()) {
//                pnrToBeReIssued = splitAndGetNewPnrToReissue(reIssueConfirmationRequest, officeId, serviceHandler);
//            }

            //Cancel, Rebook and reprice here and generate a new PNR Response
            reIssuePNRResponse = reIssueBookingService.confirmReissue("P6M2KO", reIssueConfirmationRequest, officeId);


            return reIssuePNRResponse;
        } catch (Exception e) {
            logger.debug("An Error occurred while processing reissue {}", e.getMessage(), e);
            return null;
        }
    }


    private static String splitAndGetNewPnrToReissue(ReIssueConfirmationRequest reIssueConfirmationRequest, FlightSearchOffice officeId, ServiceHandler serviceHandler) {

        String childPnr = null;
        AmadeusSessionWrapper amadeusSessionWrapper = null;
        try {

            amadeusSessionWrapper = serviceHandler.logIn(officeId);

            //Retrieving PNR here for stateful operation
            serviceHandler.retrivePNR(reIssueConfirmationRequest.getOriginalGdsPnr(), amadeusSessionWrapper);

            //Splitting the PNR
            serviceHandler.splitPNRForReissue(reIssueConfirmationRequest, amadeusSessionWrapper);

            //Saving the Original Booking after the Split
            serviceHandler.saveChildPNR("14", amadeusSessionWrapper);

            //Saving the Child PNR and retrieving the PNR
            PNRReply newSplitPnrReply = serviceHandler.saveChildPNR("11", amadeusSessionWrapper);
            List<PNRReply.DataElementsMaster.DataElementsIndiv> dataElementsDivList = newSplitPnrReply.getDataElementsMaster().getDataElementsIndiv();
            for (PNRReply.DataElementsMaster.DataElementsIndiv dataElementsDiv : dataElementsDivList) {
                if ("SP".equals(dataElementsDiv.getElementManagementData().getSegmentName())) {
                    if (dataElementsDiv.getReferencedRecord() != null) {
                        childPnr = dataElementsDiv.getReferencedRecord().getReferencedReservationInfo().getReservation().getControlNumber();
                    }
                }
            }

            return childPnr;
        } catch (Exception e) {
            logger.debug("Error Splitting the PNR : {} for Reissue \n {} ", reIssueConfirmationRequest.getOriginalGdsPnr(), e.getMessage(), e);
            return null;
        } finally {
            serviceHandler.logOut(amadeusSessionWrapper);
        }
    }

    public PNRResponse ticketRebookAndRepricePNR(TravellerMasterInfo travellerMasterInfo, ReIssueSearchRequest reIssueTicketRequest) {
        String officeId = null;
        AmadeusSessionWrapper amadeusSessionWrapper = null;
        PNRReply reply = null;
        PNRResponse pnrResponse = new PNRResponse();
        try {
            officeId = getSpecificOfficeIdforAirline(travellerMasterInfo.getItinerary());
            if (officeId == null) {
                if (travellerMasterInfo.isSeamen()) {
                    officeId = travellerMasterInfo.getItinerary().getSeamanPricingInformation().getPricingOfficeId();
                    amadeusSessionWrapper = serviceHandler.logIn(officeId);
                }
            }
            //retrive PNR
            PNRReply pnrReply = serviceHandler.retrivePNR(travellerMasterInfo.getGdsPNR(), amadeusSessionWrapper);
            List<String> ticketList = getTicketList(pnrReply.getDataElementsMaster().getDataElementsIndiv());

            /*PNRReply cancelPNRReply  = serviceHandler.cancelFullPNR(travellerMasterInfo.getGdsPNR(), pnrReply, amadeusSessionWrapper,false);
            com.amadeus.xml.pnracc_11_3_1a.PNRReply savePNRReply = serviceHandler.savePNR(amadeusSessionWrapper);
            PNRReply retrievePNRReply = serviceHandler.retrivePNR(travellerMasterInfo.getGdsPNR(), amadeusSessionWrapper);
*/

            //PNRReply cancelPNRReply = serviceHandler.cancelPNR(travellerMasterInfo.getGdsPNR(), pnrReply, amadeusSessionWrapper);
            AirSellFromRecommendationReply sellFromRecommendationReply = serviceHandler.checkFlightAvailability(travellerMasterInfo, amadeusSessionWrapper);
            TicketRepricePNRWithBookingClassReply repricePNRWithBookingClassReply = serviceHandler.repricePNRWithBookingClassReply(amadeusSessionWrapper, travellerMasterInfo, ticketList);
            TicketReissueConfirmedPricingReply ticketReissueConfirmedPricingReply = serviceHandler.ticketReissueConfirmedPricingReply(amadeusSessionWrapper, ticketList);
            reply = serviceHandler.addTravellerInfoToPNR(travellerMasterInfo, amadeusSessionWrapper);
            FarePricePNRWithBookingClassReply pricePNRReply = null;
            pnrResponse = createPNRResponse(reply, pricePNRReply, pnrResponse, travellerMasterInfo);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return pnrResponse;
    }

    public PNRResponse createPNRResponse(PNRReply gdsPNRReply,
                                         FarePricePNRWithBookingClassReply pricePNRReply, PNRResponse pnrResponse, TravellerMasterInfo travellerMasterInfo) {
//		PNRResponse pnrResponse = new PNRResponse();
        //for (PNRReply.PnrHeader pnrHeader : gdsPNRReply.getPnrHeader()) {
        pnrResponse.setPnrNumber(gdsPNRReply.getPnrHeader().get(0).getReservationInfo().getReservation().getControlNumber());
        //}
        /*if(pricePNRReply != null){
            setLastTicketingDate(pricePNRReply, pnrResponse, travellerMasterInfo);
        }*/
        pnrResponse.setFlightAvailable(true);
        if (gdsPNRReply.getSecurityInformation() != null && gdsPNRReply.getSecurityInformation().getSecondRpInformation() != null)
            pnrResponse.setCreationOfficeId(gdsPNRReply.getSecurityInformation().getSecondRpInformation().getCreationOfficeId());
//		pnrResponse.setTaxDetailsList(AmadeusBookingHelper
//				.getTaxDetails(pricePNRReply));
        logger.debug("todo createPNRResponse: " + Json.stringify(Json.toJson(pnrResponse)));
        return pnrResponse;
    }

    private String getSpecificOfficeIdforAirline(FlightItinerary itinerary) {
        Configuration config = Play.application().configuration();
        Configuration airlineBookingOfficeConfig = config.getConfig("amadeus.AIRLINE_BOOKING_OFFICE");
        for (Journey journey : itinerary.getJourneyList()) {
            for (AirSegmentInformation segmentInfo : journey.getAirSegmentList()) {
                //String officeId = config.getString("amadeus.AIRLINE_BOOKING_OFFICE."+carcode);
                String officeId = airlineBookingOfficeConfig.getString(segmentInfo.getCarrierCode());
                if (officeId != null) {
                    return officeId;
                }
            }
        }
        return null;
    }


    public List<String> getTicketList(List<PNRReply.DataElementsMaster.DataElementsIndiv> dataElementsIndivList) {
        List<String> ticketsList = dataElementsIndivList.stream().flatMap(dataElementsIndiv -> dataElementsIndiv.getOtherDataFreetext().stream()).
                filter(longFreeTextType -> longFreeTextType.getFreetextDetail().getType().equalsIgnoreCase("P06"))
                .map(longFreeTextType -> longFreeTextType.getLongFreetext().toString()).collect(Collectors.toList());

        List<String> finalticketsList = new ArrayList<>();
        for (String ticket : ticketsList) {
            String[] arraayStr = ticket.split("/");
            String[] data = arraayStr[0].split(" ");
            String ticketnumber = data[1].replace("-", "");
            finalticketsList.add(ticketnumber);
        }

        return finalticketsList;
    }


}
