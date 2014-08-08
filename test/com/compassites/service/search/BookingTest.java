package com.compassites.service.search;

import com.compassites.GDSWrapper.amadeus.ServiceHandler;
import com.compassites.model.AirSegmentInformation;
import com.compassites.model.FlightItinerary;
import com.compassites.model.Journey;
import com.compassites.model.traveller.PersonalDetails;
import com.compassites.model.traveller.Traveller;
import com.compassites.model.traveller.TravellerMasterInfo;
import org.junit.Test;
import services.BookingService;
import services.BookingServiceImpl;

import java.util.Date;

/**
 * Created by user on 23-07-2014.
 */
public class BookingTest {
    @Test
    public void sellFormRecommendation()throws Exception{
        FlightItinerary fi1 = new FlightItinerary();
        fi1.setProvider("Amadeus");
        fi1.getPricingInformation().setBasePrice("INR8850");
        fi1.getPricingInformation().setTax("INR8850");
        fi1.getPricingInformation().setTotalPrice("INR8850");
        fi1.AddBlankJourney();
        Journey journey = fi1.getJourneyList().get(0);
        AirSegmentInformation segmentInformation = new AirSegmentInformation();
        segmentInformation.setFromLocation("SIN");
        segmentInformation.setToLocation("MNL");
        segmentInformation.setDepartureTime("2014-06-30T009:50:00.000+05:30");
        segmentInformation.setFlightNumber("910");
        segmentInformation.setCarrierCode("9W");
        journey.getAirSegmentList().add(segmentInformation);

        /*AirSegmentInformation segmentInformation1 = new AirSegmentInformation();
        segmentInformation1.setFromLocation("DEL");
        segmentInformation1.setToLocation("BOM");
        segmentInformation1.setDepartureTime("2014-08-30T021:08:00.000+05:30");
        segmentInformation1.setFlightNumber("7133");
        segmentInformation1.setCarrierCode("9W");*/

        //journey.getAirSegmentList().add(segmentInformation1);

        TravellerMasterInfo travellerMasterInfo = new TravellerMasterInfo();
        Traveller traveller = new Traveller();
        PersonalDetails personalDetails = new PersonalDetails();
        Date dob = new Date("Sat, 12 Aug 1995 13:30:00 GMT+0430");
        personalDetails.setDateOfBirth(dob);
        personalDetails.setLastName("DUPONT");
        personalDetails.setFirstName("MATHIEU");
        traveller.setPersonalDetails(personalDetails);
        travellerMasterInfo.getTravellersList().add(traveller);
        travellerMasterInfo.setItinerary(fi1);
        try {
            ServiceHandler serviceHandler = new ServiceHandler();
            serviceHandler.logIn();
            BookingService bookingService = new BookingServiceImpl();
            bookingService.generatePNR(travellerMasterInfo);
           /* AirSellFromRecommendationReply sellFromRecommendation = serviceHandler.sellFromRecommendation(fi1);
            PNRReply pnrReply = serviceHandler.addMultiElementsToPNR1(travellerMasterInfo);
            FarePricePNRWithBookingClassReply pricePNRReply = serviceHandler.pricePNR();
            TicketCreateTSTFromPricingReply ticketCreateTSTFromPricingReply = serviceHandler.createTST();
            pnrReply = serviceHandler.savePNR();
            String tstRefNo = pnrReply.getPnrHeader().get(0).getReservationInfo().getReservation().getControlNumber();
            pnrReply = serviceHandler.retrivePNR(tstRefNo);
            System.out.println("Response : " + sellFromRecommendation.getMessage());*/
        }catch (Exception e){
            e.printStackTrace();
        }



    }
}
