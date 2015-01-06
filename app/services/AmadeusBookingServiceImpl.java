package services;

import com.amadeus.xml.farqnr_07_1_1a.FareCheckRulesReply;
import com.amadeus.xml.itares_05_2_ia.AirSellFromRecommendationReply;
import com.amadeus.xml.pnracc_10_1_1a.PNRReply;
import com.amadeus.xml.tautcr_04_1_1a.TicketCreateTSTFromPricingReply;
import com.amadeus.xml.tipnrr_12_4_1a.FareInformativePricingWithoutPNRReply;
import com.amadeus.xml.tpcbrr_07_3_1a.FarePricePNRWithBookingClassReply;
import com.amadeus.xml.ttktir_09_1_1a.DocIssuanceIssueTicketReply;
import com.compassites.GDSWrapper.amadeus.ServiceHandler;
import com.compassites.model.ErrorMessage;
import com.compassites.model.IssuanceRequest;
import com.compassites.model.IssuanceResponse;
import com.compassites.model.PNRResponse;
import com.compassites.model.traveller.TravellerMasterInfo;
import org.springframework.stereotype.Service;
import utils.AmadeusBookingHelper;
import utils.ErrorMessageHelper;
import utils.XMLFileUtility;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;


@Service
public class AmadeusBookingServiceImpl implements BookingService {

    private final String amadeusFlightAvailibilityCode = "OK";

    private final String totalFareIdentifier = "712";

    private final String issuenceOkStatus = "O";

    private final String cappingLimitString = "CT RJT";

    @Override
    public PNRResponse generatePNR(TravellerMasterInfo travellerMasterInfo) {

        ServiceHandler serviceHandler = null;
        PNRResponse pnrResponse = new PNRResponse();
        FarePricePNRWithBookingClassReply pricePNRReply = null;
        try {
            serviceHandler = new ServiceHandler();

            serviceHandler.logIn();

            /*AirSellFromRecommendationReply sellFromRecommendation = serviceHandler.checkFlightAvailability(travellerMasterInfo);

            if(sellFromRecommendation.getErrorAtMessageLevel() != null && sellFromRecommendation.getErrorAtMessageLevel().size() > 0 && (sellFromRecommendation.getItineraryDetails() == null)){

                ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage("error", ErrorMessage.ErrorType.ERROR, "Amadeus");
                pnrResponse.setErrorMessage(errorMessage);
            }

            boolean flightAvailable = validateFlightAvailability(sellFromRecommendation);*/

            checkFlightAvailibility(travellerMasterInfo, serviceHandler, pnrResponse);

            if (pnrResponse.isFlightAvailable()) {

                PNRReply gdsPNRReply = serviceHandler.addTravellerInfoToPNR(travellerMasterInfo);

                /*String carrierCode = "";
                if(travellerMasterInfo.isSeamen()) {
                    carrierCode = travellerMasterInfo.getItinerary().getJourneyList().get(0).getAirSegmentList().get(0).getCarrierCode();
                } else {
                    carrierCode = travellerMasterInfo.getItinerary().getNonSeamenJourneyList().get(0).getAirSegmentList().get(0).getCarrierCode();
                }
                pricePNRReply = serviceHandler.pricePNR(carrierCode, gdsPNRReply);

                pnrResponse = checkFare(pricePNRReply, travellerMasterInfo);*/

                pricePNRReply = checkPNRPricing(travellerMasterInfo, serviceHandler, gdsPNRReply, pricePNRReply, pnrResponse);

                if (!pnrResponse.isPriceChanged()) {
                    int numberOfTst = (travellerMasterInfo.isSeamen())? 1 : AmadeusBookingHelper.getPassengerTypeCount(travellerMasterInfo.getTravellersList());

                    TicketCreateTSTFromPricingReply ticketCreateTSTFromPricingReply = serviceHandler.createTST(numberOfTst);
                    if(ticketCreateTSTFromPricingReply.getApplicationError() != null){
                        String errorCode = ticketCreateTSTFromPricingReply.getApplicationError().getApplicationErrorInfo().getApplicationErrorDetail().getApplicationErrorCode();

                        ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage("error", ErrorMessage.ErrorType.ERROR, "Amadeus");
                        pnrResponse.setErrorMessage(errorMessage);
                        pnrResponse.setFlightAvailable(false);
                        return pnrResponse;
                    }
                    gdsPNRReply = serviceHandler.savePNR();
                    String tstRefNo = getPNRNoFromResponse(gdsPNRReply);
                    gdsPNRReply = serviceHandler.retrivePNR(tstRefNo);
                    //pnrResponse.setPnrNumber(gdsPNRReply.getPnrHeader().get(0).getReservationInfo().getReservation().getControlNumber());
                    pnrResponse = createPNRResponse(gdsPNRReply, pricePNRReply);

                    //getCancellationFee(travellerMasterInfo, serviceHandler);
                }
            } else {

                pnrResponse.setFlightAvailable(false);
            }


        } catch (Exception e) {
            e.printStackTrace();
            ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage("error", ErrorMessage.ErrorType.ERROR, "Amadeus");
            pnrResponse.setErrorMessage(errorMessage);
        }

        return pnrResponse;
    }


    public void checkFlightAvailibility(TravellerMasterInfo travellerMasterInfo, ServiceHandler serviceHandler, PNRResponse pnrResponse){

        AirSellFromRecommendationReply sellFromRecommendation = serviceHandler.checkFlightAvailability(travellerMasterInfo);

        if(sellFromRecommendation.getErrorAtMessageLevel() != null && sellFromRecommendation.getErrorAtMessageLevel().size() > 0 && (sellFromRecommendation.getItineraryDetails() == null)){

            ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage("error", ErrorMessage.ErrorType.ERROR, "Amadeus");
            pnrResponse.setErrorMessage(errorMessage);
        }
        boolean flightAvailable = AmadeusBookingHelper.validateFlightAvailability(sellFromRecommendation, amadeusFlightAvailibilityCode);
        pnrResponse.setFlightAvailable(flightAvailable);
    }

    public FarePricePNRWithBookingClassReply checkPNRPricing(TravellerMasterInfo travellerMasterInfo, ServiceHandler serviceHandler, PNRReply gdsPNRReply,
                                                             FarePricePNRWithBookingClassReply pricePNRReply, PNRResponse pnrResponse) {
        String carrierCode = "";
        if (travellerMasterInfo.isSeamen()) {
            carrierCode = travellerMasterInfo.getItinerary().getJourneyList().get(0).getAirSegmentList().get(0).getCarrierCode();
        } else {
            carrierCode = travellerMasterInfo.getItinerary().getNonSeamenJourneyList().get(0).getAirSegmentList().get(0).getCarrierCode();
        }
        pricePNRReply = serviceHandler.pricePNR(carrierCode, gdsPNRReply);

        pnrResponse = AmadeusBookingHelper.checkFare(pricePNRReply, travellerMasterInfo, totalFareIdentifier);

        return pricePNRReply;

    }



    @Override
    public PNRResponse priceChangePNR(TravellerMasterInfo travellerMasterInfo) {
            return generatePNR(travellerMasterInfo);
    }



    public String getPNRNoFromResponse(PNRReply gdsPNRReply){
        String pnrNumber = null;
        for(PNRReply.PnrHeader pnrHeader: gdsPNRReply.getPnrHeader()){
            pnrNumber = pnrHeader.getReservationInfo().getReservation().getControlNumber();
        }

        return pnrNumber;
    }

    public PNRResponse createPNRResponse(PNRReply gdsPNRReply,FarePricePNRWithBookingClassReply pricePNRReply){
        PNRResponse pnrResponse = new PNRResponse();

        for(PNRReply.PnrHeader pnrHeader: gdsPNRReply.getPnrHeader()){
            pnrResponse.setPnrNumber(pnrHeader.getReservationInfo().getReservation().getControlNumber());
        }
        FarePricePNRWithBookingClassReply.FareList.LastTktDate.DateTime dateTime = pricePNRReply.getFareList().get(0).getLastTktDate().getDateTime();
        String day = ((dateTime.getDay().toString().length() == 1) ? "0"+dateTime.getDay(): dateTime.getDay().toString());
        String month = ((dateTime.getMonth().toString().length() == 1) ? "0"+dateTime.getMonth(): dateTime.getMonth().toString());
        String year = dateTime.getYear().toString();
        //pnrResponse.setValidTillDate(""+dateTime.getDay()+dateTime.getMonth()+dateTime.getYear());
        SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy");
        Date lastTicketingDate = null;
        try {
           lastTicketingDate =  sdf.parse(day+month+year);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        pnrResponse.setValidTillDate(lastTicketingDate);
        pnrResponse.setFlightAvailable(true);
        pnrResponse.setTaxDetailsList(AmadeusBookingHelper.getTaxDetails(pricePNRReply));
        return pnrResponse;
    }

    public IssuanceResponse issueTicket(IssuanceRequest issuanceRequest) {
        ServiceHandler serviceHandler = null;
        IssuanceResponse issuanceResponse = new IssuanceResponse();
        issuanceResponse.setPnrNumber(issuanceRequest.getGdsPNR());
        try {
            serviceHandler = new ServiceHandler();
            serviceHandler.logIn();
            PNRReply gdsPNRReply = serviceHandler.retrivePNR(issuanceRequest.getGdsPNR());
            String carrierCode = "";
            if(issuanceRequest.isSeamen()) {
                carrierCode = issuanceRequest.getFlightItinerary().getJourneyList().get(0).getAirSegmentList().get(0).getCarrierCode();
            } else {
                carrierCode = issuanceRequest.getFlightItinerary().getNonSeamenJourneyList().get(0).getAirSegmentList().get(0).getCarrierCode();
            }
            FarePricePNRWithBookingClassReply pricePNRReply = serviceHandler.pricePNR(carrierCode, gdsPNRReply);

            int numberOfTst = (issuanceRequest.isSeamen())? 1 : AmadeusBookingHelper.getPassengerTypeCount(issuanceRequest.getTravellerList());

            TicketCreateTSTFromPricingReply ticketCreateTSTFromPricingReply = serviceHandler.createTST(numberOfTst);

            if(ticketCreateTSTFromPricingReply.getApplicationError() != null){
                String errorCode = ticketCreateTSTFromPricingReply.getApplicationError().getApplicationErrorInfo().getApplicationErrorDetail().getApplicationErrorCode();

                ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage("error", ErrorMessage.ErrorType.ERROR, "Amadeus");
                issuanceResponse.setErrorMessage(errorMessage);
                issuanceResponse.setSuccess(false);
                return issuanceResponse;
            }
            gdsPNRReply = serviceHandler.savePNR();
            gdsPNRReply = serviceHandler.retrivePNR(issuanceRequest.getGdsPNR());
            XMLFileUtility.createXMLFile(gdsPNRReply, "retrievePNRRes1.xml");
            DocIssuanceIssueTicketReply issuanceIssueTicketReply = serviceHandler.issueTicket();
            if(issuenceOkStatus.equals(issuanceIssueTicketReply.getProcessingStatus().getStatusCode())){
                gdsPNRReply = serviceHandler.retrivePNR(issuanceRequest.getGdsPNR());
                AmadeusBookingHelper.createTickets(issuanceResponse, issuanceRequest, gdsPNRReply);
            }else {
                String errorDescription = issuanceIssueTicketReply.getErrorGroup().getErrorWarningDescription().getFreeText();
                if(errorDescription.contains(cappingLimitString)){
                    System.out.println("Send Email to operator saying capping limit is reached");
                    issuanceResponse.setCappingLimitReached(true);
                }
            }


            System.out.println("");
        } catch (Exception e) {
            e.printStackTrace();
        }

      return issuanceResponse;
    }




    public void getCancellationFee(IssuanceRequest issuanceRequest,ServiceHandler serviceHandler){
        //ServiceHandler serviceHandler = null;
        try {
            //serviceHandler = new ServiceHandler();
            //serviceHandler.logIn();
            /*int adultCount  = 0,childCount  = 0,infantCount = 0;
            for(Traveller traveller : travellerMasterInfo.getTravellersList()){
                PassengerTypeCode passengerType = DateUtility.getPassengerTypeFromDOB(traveller.getPersonalDetails().getDateOfBirth());
                if(passengerType.equals(PassengerTypeCode.ADT)){
                    adultCount++;
                } else if(passengerType.equals(PassengerTypeCode.CHD)){
                    childCount++;
                } else {
                    infantCount++;
                }
            }*/
            FareInformativePricingWithoutPNRReply fareInfoReply = serviceHandler.getFareInfo(issuanceRequest.getFlightItinerary(), issuanceRequest.getAdultCount(), issuanceRequest.getChildCount(), issuanceRequest.getInfantCount());

            FareCheckRulesReply fareCheckRulesReply = serviceHandler.getFareRules();

            StringBuilder fareRule = new StringBuilder();
            for(FareCheckRulesReply.TariffInfo tariffInfo : fareCheckRulesReply.getTariffInfo()){
                if("(16)".equals(tariffInfo.getFareRuleInfo().getRuleCategoryCode())){
                    for(FareCheckRulesReply.TariffInfo.FareRuleText text : tariffInfo.getFareRuleText() ) {
                        fareRule.append(text.getFreeText().get(0));
                    }
                }
            }
            System.out.println("---------------------------------------Fare Rules------------------------------------\n"+fareRule.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public PNRResponse checkFareChangeAndAvailability(TravellerMasterInfo travellerMasterInfo){
        ServiceHandler serviceHandler = null;
        PNRResponse pnrResponse = new PNRResponse();
        FarePricePNRWithBookingClassReply pricePNRReply = null;
        try {
            serviceHandler = new ServiceHandler();

            serviceHandler.logIn();

            checkFlightAvailibility(travellerMasterInfo, serviceHandler, pnrResponse);

            if (pnrResponse.isFlightAvailable()) {

                PNRReply gdsPNRReply = serviceHandler.addTravellerInfoToPNR(travellerMasterInfo);

                pricePNRReply = checkPNRPricing(travellerMasterInfo, serviceHandler, gdsPNRReply, pricePNRReply, pnrResponse);

               return  pnrResponse;
            } else {

                pnrResponse.setFlightAvailable(false);
            }


        } catch (Exception e) {
            e.printStackTrace();
            ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage("error", ErrorMessage.ErrorType.ERROR, "Amadeus");
            pnrResponse.setErrorMessage(errorMessage);
        }

        return pnrResponse;
    }
}
