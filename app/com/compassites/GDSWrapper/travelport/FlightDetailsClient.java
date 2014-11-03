package com.compassites.GDSWrapper.travelport;

import java.io.IOException;
import java.net.MalformedURLException;

import javax.xml.ws.BindingProvider;

import org.apache.commons.logging.LogFactory;

import com.travelport.schema.air_v26_0.FareInfo;
import com.travelport.schema.air_v26_0.FlightDetailsReq;
import com.travelport.service.air_v26_0.AirLowFareSearchPortType;
import com.travelport.service.air_v26_0.AirService;
import com.travelport.service.air_v26_0.FlightDetailsPortType;
import com.travelport.service.air_v26_0.FlightInfoPortType;

public class FlightDetailsClient extends TravelPortClient {
	
    static final String ServiceName =  "/AirService";

    static AirService airService = null;
    static FlightDetailsPortType flightDetailsPortType = null;

//    static void  init() {
//        if (airService == null) {
//            java.net.URL url = null;
//            try {
//                java.net.URL baseUrl;
//                baseUrl = AirService.class.getResource(".");
//                url = new java.net.URL(baseUrl, "http://localhost:9000/wsdl/galileo/air_v26_0/Air.wsdl");
//                airService = new AirService(url);
//            } catch (MalformedURLException e) {
//                e.printStackTrace();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//
//
//        }
//        if (flightDetailsPortType == null) {
//        	FareInfo fareInfo;
//        	fareInfo.getBaggageAllowance();
//        	FlightDetailsReq flightDetailsReq;
//        	flightDetailsPortType = airService.
//            LogFactory.getLog(AirRequestClient.class).info("Initializing AirAvailabilitySearchPortType....");
//            setRequestContext((BindingProvider) airLowFareSearchPortTypePort, ServiceName);
//            LogFactory.getLog(AirRequestClient.class).info("Initialized");
//        }
//    }
//    
//    public void getFlightDetails() {
//    	FlightDetailsReq flightDetailsReq = new FlightDetailsReq();
//    	flightDetailsReq.
//    }

}
