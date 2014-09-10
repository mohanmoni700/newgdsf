package services;

import com.amadeus.xml.itares_05_2_ia.AirSellFromRecommendationReply;
import com.amadeus.xml.pnracc_10_1_1a.PNRReply;
import com.amadeus.xml.tautcr_04_1_1a.TicketCreateTSTFromPricingReply;
import com.amadeus.xml.tpcbrr_07_3_1a.FarePricePNRWithBookingClassReply;
import com.compassites.GDSWrapper.amadeus.ServiceHandler;
import com.compassites.model.ErrorMessage;
import com.compassites.model.PNRResponse;
import com.compassites.model.traveller.TravellerMasterInfo;
import org.springframework.stereotype.Service;
import utils.ErrorMessageHelper;

import java.util.List;


@Service
public class AmadeusBookingServiceImpl implements BookingService {

    private final String amadeusFlightAvailibilityCode = "OK";

    private final String totalFareIdentifier = "712";


    @Override
    public PNRResponse generatePNR(TravellerMasterInfo travellerMasterInfo) {

        ServiceHandler serviceHandler = null;
        PNRResponse pnrResponse = new PNRResponse();
        try {
            serviceHandler = new ServiceHandler();
        } catch (Exception e) {
            e.printStackTrace();
        }
        serviceHandler.logIn();
        AirSellFromRecommendationReply sellFromRecommendation = serviceHandler.sellFromRecommendation(travellerMasterInfo.getItinerary());

        boolean flightAvailable = checkFlightAvailability(sellFromRecommendation);

        FarePricePNRWithBookingClassReply pricePNRReply = null;
        if(flightAvailable){
            PNRReply gdsPNRReply = serviceHandler.addMultiElementsToPNR1(travellerMasterInfo);
            pricePNRReply = serviceHandler.pricePNR();
            boolean isFareValid = checkFare(pricePNRReply,travellerMasterInfo);
            if(isFareValid){
                TicketCreateTSTFromPricingReply ticketCreateTSTFromPricingReply = serviceHandler.createTST();
                gdsPNRReply = serviceHandler.savePNR();
                String tstRefNo = gdsPNRReply.getPnrHeader().get(0).getReservationInfo().getReservation().getControlNumber();
                gdsPNRReply = serviceHandler.retrivePNR(tstRefNo);
                pnrResponse.setPnrNumber(gdsPNRReply.getPnrHeader().get(0).getReservationInfo().getReservation().getControlNumber());
            }else{

                ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage("partial", ErrorMessage.ErrorType.ERROR, "Amadeus");
                System.out.println("Response : " + sellFromRecommendation.getMessage());
            }



        }  else {
            String errorCode = sellFromRecommendation.getErrorAtMessageLevel().get(0).getErrorSegment().getErrorDetails().getErrorCode();
            errorCode = "amadeus." + errorCode;
            ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage("partialResults", ErrorMessage.ErrorType.ERROR, "Amadeus");
            System.out.println("Response : " + sellFromRecommendation.getMessage());
            pnrResponse.setErrorMessage(errorMessage);
        }
        FarePricePNRWithBookingClassReply.FareList.LastTktDate.DateTime dateTime = pricePNRReply.getFareList().get(0).getLastTktDate().getDateTime();
        pnrResponse.setValidTillDate(""+dateTime.getDay()+dateTime.getMonth()+dateTime.getYear());
        return pnrResponse;
    }


    public boolean checkFlightAvailability(AirSellFromRecommendationReply sellFromRecommendation){
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


    public boolean checkFare(FarePricePNRWithBookingClassReply pricePNRReply,TravellerMasterInfo travellerMasterInfo){
        Long totalFare = 0L;
        List<FarePricePNRWithBookingClassReply.FareList.FareDataInformation.FareDataSupInformation> fareList = pricePNRReply.getFareList().get(0).getFareDataInformation().getFareDataSupInformation();
        for (FarePricePNRWithBookingClassReply.FareList.FareDataInformation.FareDataSupInformation fareData : fareList){

            if(totalFareIdentifier.equals(fareData.getFareDataQualifier())){
                totalFare = new Long(fareData.getFareAmount());
            }
            break;
        }
        if(totalFare == travellerMasterInfo.getItinerary().getPricingInformation().getTotalPriceValue()) {
            return true;
        }

        return false;

    }
}
