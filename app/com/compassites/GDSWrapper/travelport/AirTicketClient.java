package com.compassites.GDSWrapper.travelport;

import com.thoughtworks.xstream.XStream;
import com.travelport.schema.air_v26_0.*;
import com.travelport.schema.air_v26_0.AirTicketingReq.AirPricingInfoRef;
import com.travelport.schema.common_v26_0.BillingPointOfSaleInfo;
import com.travelport.schema.common_v26_0.BookingTravelerRef;
import com.travelport.schema.common_v26_0.FormOfPayment;
import com.travelport.schema.universal_v26_0.UniversalRecord;
import com.travelport.service.air_v26_0.AirFaultMessage;
import com.travelport.service.air_v26_0.AirService;
import com.travelport.service.air_v26_0.AirTicketingPortType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.XMLFileUtility;

import javax.xml.ws.BindingProvider;
import java.net.MalformedURLException;
import java.util.Date;

/**
 * @author Santhosh
 */
public class AirTicketClient extends TravelPortClient {

	public static final String SERVICE_NAME = "/AirService";
	public static final String WSDL_URL = "http://localhost:9000/wsdl/galileo/air_v26_0/Air.wsdl";
	static AirService airService = null;
	static AirTicketingPortType airTicketingPortType = null;

    static Logger logger = LoggerFactory.getLogger("gds");

    static Logger travelportLogger = LoggerFactory.getLogger("travelport");


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
			logger.debug("Initializing AirTicketingPortType....");
			setRequestContext((BindingProvider) airTicketingPortType,
					SERVICE_NAME);
			logger.debug("Initialized");
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

		UniversalRecord uniRcd = UniversalRecordClient.retrievePNR(pnrNumber).getUniversalRecord();
		AirReservation airReservation = uniRcd.getAirReservation().get(0);

		AirPricingInfoRef airPricingInfoRef = null;

        if(airReservation.getAirPricingInfo() != null && airReservation.getAirPricingInfo().size() > 0){
			for(AirPricingInfo airPricingInfo: airReservation.getAirPricingInfo()){
				airPricingInfoRef = new AirPricingInfoRef();
				airPricingInfoRef.setKey(airPricingInfo.getKey());
				for (BookingTravelerRef btr : airPricingInfo.getBookingTravelerRef()){
					airPricingInfoRef.getBookingTravelerRef().add(btr);
				}
				request.getAirPricingInfoRef().add(airPricingInfoRef);
			}

        }


		AirReservationLocatorCode airResLocatorCode = new AirReservationLocatorCode();
		airResLocatorCode.setValue(airReservation.getLocatorCode());
		request.setAirReservationLocatorCode(airResLocatorCode);

		FormOfPayment fop = new FormOfPayment();
		fop.setType("Cash");
		AirTicketingModifiers airTicketingModifiers = new AirTicketingModifiers();
		airTicketingModifiers.setFormOfPayment(fop);
		request.getAirTicketingModifiers().add(airTicketingModifiers);

		XMLFileUtility.createXMLFile(request, "AirTicketingReq.xml");
        travelportLogger.debug("AirTicketingReq " + new Date() +" ------>> "+ new XStream().toXML(request));
		try {
			response = airTicketingPortType.service(request);
		} catch (AirFaultMessage e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.error("Error in AirTicketClient issueTicket : ", e);
		}
		XMLFileUtility.createXMLFile(response, "AirTicketingRsp.xml");
        travelportLogger.debug("AirTicketingRsp " + new Date() +" ------>> "+ new XStream().toXML(response));
		return response;
	}

}
