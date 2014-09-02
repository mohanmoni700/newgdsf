package com.travelport.client;

import com.compassites.GDSWrapper.travelport.AirRequestClient;
import com.compassites.GDSWrapper.travelport.AirReservationClient;
import com.compassites.GDSWrapper.travelport.Helper;
import com.compassites.GDSWrapper.travelport.LowFareRequestClient;
import com.travelport.schema.air_v26_0.AirItinerary;
import com.travelport.schema.air_v26_0.AirPriceRsp;
import com.travelport.schema.air_v26_0.AirPricingSolution;
import com.travelport.schema.air_v26_0.LowFareSearchRsp;
import com.travelport.schema.common_v26_0.TypeCabinClass;
import com.travelport.schema.universal_v26_0.AirCreateReservationRsp;
import com.travelport.service.air_v26_0.AirFaultMessage;
import org.junit.Test;

import javax.xml.datatype.DatatypeConfigurationException;

/**
 * Created with IntelliJ IDEA.
 * User: Renu
 * Date: 5/22/14
 * Time: 4:21 PM
 * To change this template use File | Settings | File Templates.
 */
public class TestAirReservationClient {
    @Test
    public void TestReservation(){
        LowFareRequestClient lowFareRequestClient = new LowFareRequestClient();
        try {
            LowFareSearchRsp responseTwo = lowFareRequestClient.search("BOM", "SIN", Helper.daysInFuture(45), Helper.daysInFuture(67), false, TypeCabinClass.ECONOMY, "SEA", "INR");
            AirItinerary airItinerary = AirRequestClient.getItinerary(responseTwo, responseTwo.getAirPricingSolution().get(0));
            try {
                AirPriceRsp priceRsp = AirRequestClient.priceItinerary(airItinerary, "SEA", "INR", TypeCabinClass.ECONOMY, null);
                AirPricingSolution airPriceSolution = AirReservationClient.stripNonXmitSections(AirRequestClient.getPriceSolution(priceRsp));
                AirCreateReservationRsp response = AirReservationClient.reserve(airPriceSolution);
                System.out.println("Results");
            } catch (DatatypeConfigurationException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        } catch (AirFaultMessage airFaultMessage) {
            airFaultMessage.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

    }

    }
