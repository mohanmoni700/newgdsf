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
import java.util.Properties;

/**
 * Created by user on 23-07-2014.
 */
public class BookingTest {
    @Test
    public void sellFormRecommendation()throws Exception{
        Properties properties=new Properties();
        properties.load(getClass().getClassLoader().getResourceAsStream("bookingDetails.properties"));


        FlightItinerary fi1 = new FlightItinerary();
        fi1.setProvider(properties.getProperty("provider"));
        fi1.getPricingInformation().setBasePrice(properties.getProperty("price"));
        fi1.getPricingInformation().setTax(properties.getProperty("price"));
        fi1.getPricingInformation().setTotalPrice(properties.getProperty("price"));
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

        segmentInformation.setFromLocation(properties.getProperty("fromLocation"));
        segmentInformation.setToLocation(properties.getProperty("toLocation"));
        segmentInformation.setDepartureTime(properties.getProperty("departureTime"));
        segmentInformation.setFlightNumber(properties.getProperty("flightNumber"));
        segmentInformation.setCarrierCode(properties.getProperty("carrierCode"));
        // Test case input from properties files
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
        Date dob = new Date(properties.getProperty("dateOfBirth"));
        personalDetails.setDateOfBirth(dob);
        personalDetails.setLastName(properties.getProperty("lastName"));
        personalDetails.setFirstName(properties.getProperty("firstName"));
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
