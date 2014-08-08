package services;

import com.amadeus.xml.itares_05_2_ia.AirSellFromRecommendationReply;
import com.amadeus.xml.pnracc_10_1_1a.PNRReply;
import com.amadeus.xml.tautcr_04_1_1a.TicketCreateTSTFromPricingReply;
import com.amadeus.xml.tpcbrr_07_3_1a.FarePricePNRWithBookingClassReply;
import com.compassites.GDSWrapper.amadeus.ServiceHandler;
import com.compassites.model.traveller.TravellerMasterInfo;
/**
 * Created by user on 07-08-2014.
 */
public class BookingServiceImpl implements BookingService{

    private final String amadeusFlightAvailibilityCode = "OK";
    @Override
    public String generatePNR(TravellerMasterInfo travellerMasterInfo) {

        ServiceHandler serviceHandler = null;
        try {
            serviceHandler = new ServiceHandler();
        } catch (Exception e) {
            e.printStackTrace();
        }
        serviceHandler.logIn();
        AirSellFromRecommendationReply sellFromRecommendation = serviceHandler.sellFromRecommendation(travellerMasterInfo.getItinerary());
        boolean flightAvailable = checkFlightAvailability(sellFromRecommendation);
        if(flightAvailable){
            PNRReply pnrReply = serviceHandler.addMultiElementsToPNR1(travellerMasterInfo);
            FarePricePNRWithBookingClassReply pricePNRReply = serviceHandler.pricePNR();
            TicketCreateTSTFromPricingReply ticketCreateTSTFromPricingReply = serviceHandler.createTST();
            pnrReply = serviceHandler.savePNR();
            String tstRefNo = pnrReply.getPnrHeader().get(0).getReservationInfo().getReservation().getControlNumber();
            pnrReply = serviceHandler.retrivePNR(tstRefNo);
            System.out.println("Response : " + sellFromRecommendation.getMessage());
        }

        return null;
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
}
