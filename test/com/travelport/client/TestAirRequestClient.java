package com.travelport.client;

import com.compassites.GDSWrapper.travelport.AirRequestClient;
import com.compassites.GDSWrapper.travelport.Helper;
import com.compassites.GDSWrapper.travelport.LowFareRequestClient;
import com.travelport.schema.air_v26_0.AirItinerary;
import com.travelport.schema.air_v26_0.AvailabilitySearchRsp;
import com.travelport.schema.air_v26_0.LowFareSearchRsp;
import com.travelport.schema.common_v26_0.TypeCabinClass;
import com.travelport.service.air_v26_0.AirFaultMessage;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Created with IntelliJ IDEA.
 * User: Renu
 * Date: 5/15/14
 * Time: 5:07 PM
 * To change this template use File | Settings | File Templates.
 */
public class TestAirRequestClient {
    @Test
    public void TestSearch(){
        AirRequestClient airRequestClient = new AirRequestClient();

        try {

            //AvailabilitySearchRsp response = airRequestClient.search("SFO", "SIN", Helper.daysInFuture(60), Helper.daysInFuture(67), true);
            //assertThat(response.getAirItinerarySolution().size(), is(2));
            //assertThat(response.getAirSegmentList().getAirSegment().size(), is(not(0)));

            AvailabilitySearchRsp responseTwo = airRequestClient.search("BOM", "BLR", Helper.daysInFuture(45), Helper.daysInFuture(67), false, TypeCabinClass.BUSINESS);
            Helper.writeXML(responseTwo);
            //AvailabilitySearchRsp responseTwo = airRequestClient.search("SIN", "MNL", Helper.daysInFuture(45), Helper.daysInFuture(67), false);
            assertThat(responseTwo.getAirItinerarySolution().size(), is(1));
            assertThat(responseTwo.getAirSegmentList().getAirSegment().size(), is(not(0)));
            List<AirItinerary> allItinerary = airRequestClient.getAllItinerary(responseTwo);
            System.out.println("**************************FOR Seamen Start Business******************************************");
            airRequestClient.displayFlightDetailsAndItinerary(allItinerary, "SEA", "INR", TypeCabinClass.BUSINESS);
            System.out.println("**************************FOR Seamen Business END  Economy Start*******************************");
            airRequestClient.displayFlightDetailsAndItinerary(allItinerary, "SEA", "INR", TypeCabinClass.ECONOMY);
            System.out.println("**************************FOR Seamen END******************************************");

            System.out.println("**************************FOR Adult Start Business******************************************");
            airRequestClient.displayFlightDetailsAndItinerary(allItinerary, "ADT", "INR", TypeCabinClass.BUSINESS);
            System.out.println("**************************FOR Adult Business END  Economy Start*******************************");
            airRequestClient.displayFlightDetailsAndItinerary(allItinerary, "ADT", "INR", TypeCabinClass.ECONOMY);
            System.out.println("**************************FOR Adult End******************************************");


        } catch (AirFaultMessage airFaultMessage) {
            airFaultMessage.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            assertTrue(false);
        }

    }

    @Test
    public void LowFareSearch(){
        LowFareRequestClient lowFareRequestClient = new LowFareRequestClient();

        try {

            //AvailabilitySearchRsp response = airRequestClient.search("SFO", "SIN", Helper.daysInFuture(60), Helper.daysInFuture(67), true);
            //assertThat(response.getAirItinerarySolution().size(), is(2));
            //assertThat(response.getAirSegmentList().getAirSegment().size(), is(not(0)));
            System.out.println("*****************Seaman FARES************************");
            LowFareSearchRsp responseOne = lowFareRequestClient.search("BOM", "SIN", Helper.daysInFuture(45), Helper.daysInFuture(67), false, TypeCabinClass.ECONOMY, "SEA", "INR");
            lowFareRequestClient.displayPriceSolution(responseOne);


            System.out.println("*****************ADULT FARES************************");

            LowFareSearchRsp responseTwo = lowFareRequestClient.search("BOM", "SIN", Helper.daysInFuture(45), Helper.daysInFuture(67), false, TypeCabinClass.ECONOMY, "ADT", "INR");
            lowFareRequestClient.displayPriceSolution(responseTwo);
            //AvailabilitySearchRsp responseTwo = airRequestClient.search("SIN", "MNL", Helper.daysInFuture(45), Helper.daysInFuture(67), false);
            assertThat(responseTwo.getAirPricingSolution().size(), is(not(0)));
            assertThat(responseTwo.getAirSegmentList().getAirSegment().size(), is(not(0)));


        } catch (AirFaultMessage airFaultMessage) {
            airFaultMessage.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            assertTrue(false);
        }

    }
}
