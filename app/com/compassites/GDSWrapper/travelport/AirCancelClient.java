package com.compassites.GDSWrapper.travelport;

import com.compassites.exceptions.BaseCompassitesException;
import com.thoughtworks.xstream.XStream;
import com.travelport.schema.air_v26_0.AirReservationLocatorCode;
import com.travelport.schema.common_v26_0.BillingPointOfSaleInfo;
import com.travelport.schema.universal_v26_0.AirCancelReq;
import com.travelport.schema.universal_v26_0.AirCancelRsp;
import com.travelport.service.universal_v26_0.AirCancelPortType;
import com.travelport.service.universal_v26_0.AirFaultMessage;
import com.travelport.service.universal_v26_0.AirService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.XMLFileUtility;

import javax.xml.ws.BindingProvider;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.util.Date;

public class AirCancelClient extends TravelPortClient{
	
	public static final String SERVICE_NAME = "/AirService";
	public static final String WSDL_URL = "http://localhost:9000/wsdl/galileo/universal_v26_0/UniversalRecord.wsdl";
    static AirService airService = null;
	static AirCancelPortType airCancelPortType = null;

    static org.slf4j.Logger logger = LoggerFactory.getLogger("gds");

    static Logger travelportLogger = LoggerFactory.getLogger("travelport");

    public AirCancelClient() {
		init();
	}
	
	void init() {
		if (airService == null) {
			java.net.URL url = null;
			try {
                java.net.URL baseUrl;
                baseUrl = AirService.class.getResource(".");
                url = new java.net.URL(baseUrl, "http://localhost:9000/wsdl/galileo/universal_v26_0/UniversalRecord.wsdl");
                //url = new java.net.URL(baseUrl, "Air.wsdl");
                airService = new AirService(url);
			} catch (MalformedURLException e) {
                logger.error("Error in Travelport AirCancelClient ", e);
				e.printStackTrace();
			}
		}
		if (airCancelPortType == null) {
			airCancelPortType = airService.getAirCancelPort();
			logger.debug("Initializing airCancelPortType....");
			setRequestContext((BindingProvider) airCancelPortType, SERVICE_NAME);
			logger.debug("Initialized");
		}
	}



    public AirCancelRsp cancelPNR(String pnr) throws BaseCompassitesException{

        logger.debug("cancelPNR called");
        AirCancelReq airCancelReq = new AirCancelReq();
        airCancelReq.setAuthorizedBy("TEST");
        airCancelReq.setTargetBranch(BRANCH);
        airCancelReq.setVersion(BigInteger.valueOf(1));
        BillingPointOfSaleInfo billingPointOfSaleInfo = new BillingPointOfSaleInfo();
        billingPointOfSaleInfo.setOriginApplication(UAPI);
        airCancelReq.setBillingPointOfSaleInfo(billingPointOfSaleInfo);

        AirReservationLocatorCode airReservationLocatorCode = new AirReservationLocatorCode();
        airReservationLocatorCode.setValue(pnr);
        airCancelReq.setAirReservationLocatorCode(airReservationLocatorCode);
//        ContinuityCheckOverride override = new ContinuityCheckOverride();
//        override.setValue("yes");
//        airCancelReq.setContinuityCheckOverride(override);
        AirCancelRsp airCancelRsp = null;
        try {
//            init();
            XMLFileUtility.createXMLFile(airCancelReq, "airCancelReq.xml");
            travelportLogger.debug("airCancelReq " + new Date() + " ------>> " + new XStream().toXML(airCancelReq));
            airCancelRsp =  airCancelPortType.service(airCancelReq, null);
            XMLFileUtility.createXMLFile(airCancelRsp, "airCancelRsp.xml");
            travelportLogger.debug("airCancelRes " + new Date() +" ------>> "+ new XStream().toXML(airCancelRsp));
        } catch (AirFaultMessage airFaultMessage) {
            logger.error("error in AirCancelClient cancelPNR", airFaultMessage);
            airFaultMessage.printStackTrace();
            travelportLogger.debug("AirReserveResponseException " + new Date() +" ------>> "+ new XStream().toXML(airFaultMessage));
            BaseCompassitesException baseCompassitesException =  new BaseCompassitesException(airFaultMessage.getMessage());
            baseCompassitesException.setErrorCode(airFaultMessage.getFaultInfo().getCode());
            throw baseCompassitesException;
        }
        logger.debug("cancelPNR complete");
        return airCancelRsp;
    }
}
