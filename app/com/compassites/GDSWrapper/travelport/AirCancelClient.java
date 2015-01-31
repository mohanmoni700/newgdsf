package com.compassites.GDSWrapper.travelport;

import java.net.MalformedURLException;

import javax.xml.ws.BindingProvider;

import play.Logger;

import com.travelport.service.air_v26_0.AirService;
import com.travelport.service.air_v26_0.AirTicketingPortType;
import com.travelport.service.universal_v26_0.AirCancelPortType;

public class AirCancelClient {
	
	public static final String SERVICE_NAME = "/AirService";
	public static final String WSDL_URL = "http://localhost:9000/wsdl/galileo/air_v26_0/Air.wsdl";
	static AirService airService = null;
	static AirCancelPortType airCancelPortType = null;

	public AirCancelClient() {
		init();
	}
	
	void init() {
		if (airService == null) {
			java.net.URL url = null;
			try {
				java.net.URL baseUrl;
				baseUrl = AirService.class.getResource(".");
				url = new java.net.URL(baseUrl, WSDL_URL);
				airService = new AirService(url);
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}
		if (airCancelPortType == null) {
//			airCancelPortType = airService.getAir
			Logger.info("Initializing AirTicketingPortType....");
//			setRequestContext((BindingProvider) airTicketingPortType,
//					SERVICE_NAME);
			Logger.info("Initialized");
		}
	}

}
