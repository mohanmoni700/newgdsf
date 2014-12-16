package com.compassites.GDSWrapper.travelport;

import java.net.MalformedURLException;

import javax.xml.ws.BindingProvider;

import play.Logger;
import utils.XMLFileUtility;

import com.travelport.schema.air_v26_0.AirReservation;
import com.travelport.schema.air_v26_0.AirReservationLocatorCode;
import com.travelport.schema.air_v26_0.AirTicketingReq;
import com.travelport.schema.air_v26_0.AirTicketingReq.AirPricingInfoRef;
import com.travelport.schema.air_v26_0.AirTicketingRsp;
import com.travelport.schema.common_v26_0.BillingPointOfSaleInfo;
import com.travelport.schema.common_v26_0.BookingTravelerRef;
import com.travelport.schema.universal_v26_0.UniversalRecord;
import com.travelport.service.air_v26_0.AirFaultMessage;
import com.travelport.service.air_v26_0.AirService;
import com.travelport.service.air_v26_0.AirTicketingPortType;

/**
 * @author Santhosh
 */
public class AirTicketClient extends TravelPortClient {

	public static final String SERVICE_NAME = "/AirService";
	public static final String WSDL_URL = "http://localhost:9000/wsdl/galileo/air_v26_0/Air.wsdl";
	static AirService airService = null;
	static AirTicketingPortType airTicketingPortType = null;

	public AirTicketClient() {
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
		if (airTicketingPortType == null) {
			airTicketingPortType = airService.getAirTicketingPort();
			Logger.info("Initializing AirTicketingPortType....");
			setRequestContext((BindingProvider) airTicketingPortType,
					SERVICE_NAME);
			Logger.info("Initialized");
		}
	}

	public AirTicketingRsp issueTicket(String pnrNumber) {
		AirTicketingReq request = new AirTicketingReq();
		AirTicketingRsp response = null;
		request.setAuthorizedBy("TEST");
		request.setTargetBranch(BRANCH);
		BillingPointOfSaleInfo billInfo = new BillingPointOfSaleInfo();
		billInfo.setOriginApplication(UAPI);
		request.setBillingPointOfSaleInfo(billInfo);

		UniversalRecord uniRcd = UniversalRecordClient.retrievePNR(pnrNumber)
				.getUniversalRecord();
		AirReservation airReservation = uniRcd.getAirReservation().get(0);

		AirPricingInfoRef airPricingInfoRef = new AirPricingInfoRef();
		airPricingInfoRef.setKey(airReservation.getAirPricingInfo().get(0)
				.getKey());
		for (BookingTravelerRef btr : airReservation.getBookingTravelerRef())
			airPricingInfoRef.getBookingTravelerRef().add(btr);
		request.getAirPricingInfoRef().add(airPricingInfoRef);

		AirReservationLocatorCode airResLocatorCode = new AirReservationLocatorCode();
		airResLocatorCode.setValue(airReservation.getLocatorCode());
		request.setAirReservationLocatorCode(airResLocatorCode);
		XMLFileUtility.createXMLFile(request, "AirTicketingReq.xml");
		try {
			response = airTicketingPortType.service(request);
		} catch (AirFaultMessage e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		XMLFileUtility.createXMLFile(response, "AirTicketingRsp.xml");
		return response;
	}

}
