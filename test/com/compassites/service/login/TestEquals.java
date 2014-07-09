package com.compassites.service.login;

import com.compassites.model.AirSegmentInformation;
import com.compassites.model.FlightItinerary;
import org.junit.Test;
/**
 * Created by user on 25-06-2014.
 */
public class TestEquals {

    @Test
    public void testFlightItinaryEquals(){

        FlightItinerary fi1 = new FlightItinerary();
        fi1.setProvider("Travelport");
        fi1.getPricingInformation().setBasePrice("INR50500");
        fi1.getPricingInformation().setTax("INR41449");
        fi1.getPricingInformation().setTotalPrice("INR91949");
        fi1.AddBlankJourney();
        FlightItinerary.Journey journey = fi1.getJourneyList().get(0);
        AirSegmentInformation segmentInformation = new AirSegmentInformation();
        segmentInformation.setFromLocation("BOM");
        segmentInformation.setToLocation("BLR");
        segmentInformation.setDepartureTime("2014-08-15T015:08:00.000+05:30");
        segmentInformation.setFlightNumber("7178");
        segmentInformation.setCarrierCode("9W");

        fi1.AddBlankJourney();
        FlightItinerary.Journey returnJourney = fi1.getJourneyList().get(1);
        AirSegmentInformation segmentInformation1 = new AirSegmentInformation();
        segmentInformation1.setFromLocation("BLR");
        segmentInformation1.setToLocation("BOM");
        segmentInformation1.setDepartureTime("2014-08-16T015:08:00.000+05:30");
        segmentInformation1.setFlightNumber("7133");
        segmentInformation1.setCarrierCode("9W");
        journey.getAirSegmentList().add(segmentInformation);
        returnJourney.getAirSegmentList().add(segmentInformation1);


        FlightItinerary fi2 = new FlightItinerary();
        fi2.setProvider("Travelport");
        fi2.getPricingInformation().setBasePrice("INR85650");
        fi2.getPricingInformation().setTax("INR9528");
        fi2.getPricingInformation().setTotalPrice("INR95178");
        fi2.AddBlankJourney();
        FlightItinerary.Journey journey2 = fi2.getJourneyList().get(0);
        AirSegmentInformation segmentInformation3 = new AirSegmentInformation();
        segmentInformation3.setFromLocation("BOM");
        segmentInformation3.setToLocation("BLR");
        segmentInformation3.setDepartureTime("2014-08-15T015:08:00.000+05:30");
        segmentInformation3.setFlightNumber("7178");
        segmentInformation3.setCarrierCode("9W");

        fi2.AddBlankJourney();
        FlightItinerary.Journey returnJourney1 = fi2.getJourneyList().get(1);
        AirSegmentInformation segmentInformation4 = new AirSegmentInformation();
        segmentInformation4.setFromLocation("BLR");
        segmentInformation4.setToLocation("BOM");
        segmentInformation4.setDepartureTime("2014-08-16T015:08:00.000+05:30");
        segmentInformation4.setFlightNumber("7133");
        segmentInformation4.setCarrierCode("9W");
        journey2.getAirSegmentList().add(segmentInformation3);
        returnJourney1.getAirSegmentList().add(segmentInformation4);

        //assertThat(fi1.equals(fi2),is(false));
        System.out.println("Equals method using :" + fi1.equals(fi2));
        System.out.println("F1 hash code: "+ fi1.hashCode()+": "+fi1.toString());
        System.out.println("F2 hash code: "+ fi2.hashCode()+": "+fi2.toString());
    }
}
