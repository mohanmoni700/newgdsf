package services;

import com.amadeus.xml.itares_05_2_ia.AirSellFromRecommendationReply;
import com.amadeus.xml.pnracc_10_1_1a.PNRReply;
import com.amadeus.xml.tautcr_04_1_1a.TicketCreateTSTFromPricingReply;
import com.amadeus.xml.tpcbrr_07_3_1a.FarePricePNRWithBookingClassReply;
import com.amadeus.xml.ttktir_09_1_1a.DocIssuanceIssueTicketReply;
import com.compassites.GDSWrapper.amadeus.ServiceHandler;
import com.compassites.model.ErrorMessage;
import com.compassites.model.PNRResponse;
import com.compassites.model.TaxDetails;
import com.compassites.model.traveller.TravellerMasterInfo;
import org.springframework.stereotype.Service;
import utils.ErrorMessageHelper;
import utils.JSONFileUtility;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


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
            AirSellFromRecommendationReply sellFromRecommendation = serviceHandler.checkFlightAvailability(travellerMasterInfo);

            if(sellFromRecommendation.getErrorAtMessageLevel() != null && sellFromRecommendation.getErrorAtMessageLevel().size() > 0){

                ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage("error", ErrorMessage.ErrorType.ERROR, "Amadeus");
                pnrResponse.setErrorMessage(errorMessage);
            }

            boolean flightAvailable = validateFlightAvailability(sellFromRecommendation);

            if (flightAvailable) {
                PNRReply gdsPNRReply = serviceHandler.addTravellerInfoToPNR(travellerMasterInfo);
                pricePNRReply = serviceHandler.pricePNR(travellerMasterInfo, gdsPNRReply);

                pnrResponse = checkFare(pricePNRReply, travellerMasterInfo);
                if (!pnrResponse.isPriceChanged()) {
                    TicketCreateTSTFromPricingReply ticketCreateTSTFromPricingReply = serviceHandler.createTST();
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

    @Override
    public PNRResponse priceChangePNR(TravellerMasterInfo travellerMasterInfo) {
            return generatePNR(travellerMasterInfo);
    }




    public boolean validateFlightAvailability(AirSellFromRecommendationReply sellFromRecommendation){
        boolean errors = true;
        for (AirSellFromRecommendationReply.ItineraryDetails itinerary : sellFromRecommendation.getItineraryDetails()){
            for(AirSellFromRecommendationReply.ItineraryDetails.SegmentInformation segmentInformation : itinerary.getSegmentInformation()){
                for(String statusCode : segmentInformation.getActionDetails().getStatusCode()){
                    if(!amadeusFlightAvailibilityCode.equals(statusCode)){
                        errors = false;
                    }
                }
            }
        }
        return errors;
    }


    public PNRResponse checkFare(FarePricePNRWithBookingClassReply pricePNRReply,TravellerMasterInfo travellerMasterInfo){
        Long totalFare = 0L;
        PNRResponse pnrResponse = new PNRResponse();
        List<FarePricePNRWithBookingClassReply.FareList.FareDataInformation.FareDataSupInformation> fareList = pricePNRReply.getFareList().get(0).getFareDataInformation().getFareDataSupInformation();
        for (FarePricePNRWithBookingClassReply.FareList.FareDataInformation.FareDataSupInformation fareData : fareList){

            if(totalFareIdentifier.equals(fareData.getFareDataQualifier())){
                totalFare = new Long(fareData.getFareAmount());
                break;
            }

        }
        Long searchPrice = 0L;
        if(travellerMasterInfo.isSeamen()){
            searchPrice = travellerMasterInfo.getItinerary().getSeamanPricingInformation().getTotalPriceValue();
        }else {
            searchPrice = travellerMasterInfo.getItinerary().getPricingInformation().getTotalPriceValue();
        }


        if(totalFare.equals(searchPrice)) {
            return pnrResponse;
        }
        pnrResponse.setChangedPrice(totalFare);
        pnrResponse.setOriginalPrice(searchPrice);
        pnrResponse.setPriceChanged(true);
        pnrResponse.setFlightAvailable(true);
        return pnrResponse;

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
        pnrResponse.setValidTillDate(lastTicketingDate.toString());
        pnrResponse.setFlightAvailable(true);
        pnrResponse.setTaxDetailsList(getTaxDetails(pricePNRReply));
        return pnrResponse;
    }

    public PNRResponse issueTicket(String pnrNumber) {
        ServiceHandler serviceHandler = null;
        PNRResponse pnrResponse = new PNRResponse();
        pnrResponse.setPnrNumber(pnrNumber);
        try {
            serviceHandler = new ServiceHandler();
            serviceHandler.logIn();
            PNRReply gdsPNRReply = serviceHandler.retrivePNR(pnrNumber);
            JSONFileUtility.createJsonFile(gdsPNRReply,"retrievePNRRes1.json");
            DocIssuanceIssueTicketReply issuanceIssueTicketReply = serviceHandler.issueTicket();
            if(issuenceOkStatus.equals(issuanceIssueTicketReply.getProcessingStatus().getStatusCode())){
                gdsPNRReply = serviceHandler.retrivePNR(pnrNumber);
            }else {
                String errorDescription = issuanceIssueTicketReply.getErrorGroup().getErrorWarningDescription().getFreeText();
                if(errorDescription.contains(cappingLimitString)){
                    System.out.println("Send Email to operator saying capping limit is reached");
                    pnrResponse.setCappingLimitReached(true);
                }
            }


            System.out.println("");
        } catch (Exception e) {
            e.printStackTrace();
        }

      return pnrResponse;
    }


    public List<TaxDetails> getTaxDetails(FarePricePNRWithBookingClassReply pricePNRReply){
        List<TaxDetails> taxDetailsList = new ArrayList<>();
        for(FarePricePNRWithBookingClassReply.FareList fareList : pricePNRReply.getFareList()){
            for(FarePricePNRWithBookingClassReply.FareList.TaxInformation taxInformation :fareList.getTaxInformation()){
                TaxDetails taxDetails = new TaxDetails();
                taxDetails.setTaxCode(taxInformation.getTaxDetails().getTaxType().getIsoCountry());
                taxDetails.setTaxAmount(new BigDecimal(taxInformation.getAmountDetails().getFareDataMainInformation().getFareAmount()));

                taxDetailsList.add(taxDetails);
            }
        }

        return taxDetailsList;
    }
}
