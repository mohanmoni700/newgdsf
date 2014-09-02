package com.compassites.service.search;

import com.compassites.model.AirSegmentInformation;
import com.compassites.model.FlightItinerary;
import com.compassites.model.Journey;
import com.compassites.model.traveller.PersonalDetails;
import com.compassites.model.traveller.Traveller;
import com.compassites.model.traveller.TravellerMasterInfo;
import org.junit.Test;
import services.AmadeusBookinServiceImpl;
import services.BookingServiceWrapper;

import java.text.SimpleDateFormat;
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
        segmentInformation.setFromLocation("BLR");
        segmentInformation.setToLocation("MAA");
        segmentInformation.setArrivalTime("2014-09-03T23:30:00.000+05:30");
        segmentInformation.setDepartureTime("2014-09-03T22:20:00.000+05:30");
        segmentInformation.setFlightNumber("2736");
        segmentInformation.setCarrierCode("9W");
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        segmentInformation.setDepartureDate(dateFormat.parse("2014-09-03"));
        segmentInformation.setToTerminal("D");
        segmentInformation.setBookingClass("V");
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
           /* ServiceHandler serviceHandler = new ServiceHandler();
            serviceHandler.logIn();*/
            AmadeusBookinServiceImpl amadeusBookinService = new AmadeusBookinServiceImpl();
            BookingServiceWrapper bookingService = new BookingServiceWrapper();
            bookingService.setAmadeusBookinService(amadeusBookinService);
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
