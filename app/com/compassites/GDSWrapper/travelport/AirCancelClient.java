package com.compassites.GDSWrapper.travelport;

import com.travelport.service.air_v26_0.AirService;
import com.travelport.service.universal_v26_0.AirCancelPortType;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;

public class AirCancelClient {
	
	public static final String SERVICE_NAME = "/AirService";
	public static final String WSDL_URL = "http://localhost:9000/wsdl/galileo/air_v26_0/Air.wsdl";
	static AirService airService = null;
	static AirCancelPortType airCancelPortType = null;

    static org.slf4j.Logger logger = LoggerFactory.getLogger("gds");

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
			logger.debug("Initializing AirTicketingPortType....");
//			setRequestContext((BindingProvider) airTicketingPortType,
//					SERVICE_NAME);
			logger.debug("Initialized");
		}
	}

}
