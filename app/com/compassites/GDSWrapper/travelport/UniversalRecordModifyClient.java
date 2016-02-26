package com.compassites.GDSWrapper.travelport;

import com.compassites.exceptions.BaseCompassitesException;
import com.thoughtworks.xstream.XStream;
import com.travelport.schema.air_v26_0.AirPricingInfo;
import com.travelport.schema.air_v26_0.AirPricingSolution;
import com.travelport.schema.common_v26_0.BillingPointOfSaleInfo;
import com.travelport.schema.common_v26_0.TypeElement;
import com.travelport.schema.universal_v26_0.*;
import com.travelport.service.universal_v26_0.*;
import org.apache.commons.logging.LogFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.XMLFileUtility;

import javax.xml.ws.BindingProvider;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.util.Date;
import java.util.List;

/**
 * Created by yaseen on 17-02-2016.
 */
public class UniversalRecordModifyClient extends TravelPortClient {

    static final String ServiceName =  "/UniversalRecordService";
    static UniversalRecordModifyService universalRecordModifyService = null;
    static UniversalRecordModifyServicePortType universalRecordModifyServicePortType = null;
    static Logger travelportLogger = LoggerFactory.getLogger("travelport");
    static Logger logger = LoggerFactory.getLogger("gds");

    static void  init(){
        if (universalRecordModifyService == null){
            try {
                String path = new File(".").getCanonicalPath();
                universalRecordModifyService = new UniversalRecordModifyService(new java.net.URL("http://localhost:9000/wsdl/galileo/universal_v26_0/UniversalRecord.wsdl"));
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (universalRecordModifyServicePortType == null){
            universalRecordModifyServicePortType  = universalRecordModifyService.getUniversalRecordModifyServicePort();
            LogFactory.getLog(UniversalRecordModifyClient.class).info("Initializing universalRecordModifyServicePortType....");
            setRequestContext((BindingProvider) universalRecordModifyServicePortType, ServiceName);
            LogFactory.getLog(UniversalRecordModifyClient.class).info("Initialized");
        }
    }


    public static UniversalRecordModifyRsp addPricingToPNR(String gdsPNR,String reservationLocatorCode, String universalRecordLocatorCode,AirPricingSolution airPricingSolution,
                                       BigInteger version) throws BaseCompassitesException{
        logger.debug("addPricingToPNR called............");
        UniversalRecordModifyReq universalRecordModifyReq = creatModifyRequestObject(version);
        version = version.add(BigInteger.valueOf(1));

        BillingPointOfSaleInfo billingPointOfSaleInfo = createBillingPOS();
        universalRecordModifyReq.setBillingPointOfSaleInfo(billingPointOfSaleInfo);

        RecordIdentifier recordIdentifier = createRecordIdentifier(gdsPNR, universalRecordLocatorCode);
        universalRecordModifyReq.setRecordIdentifier(recordIdentifier);

        UniversalModifyCmd universalModifyCmd = new UniversalModifyCmd();
        universalModifyCmd.setKey("");
        AirAdd airAdd = new AirAdd();
        airAdd.setReservationLocatorCode(reservationLocatorCode);
        airAdd.getAirPricingInfo().addAll(airPricingSolution.getAirPricingInfo());

        universalModifyCmd.setAirAdd(airAdd);
        universalRecordModifyReq.getUniversalModifyCmd().add(universalModifyCmd);
        UniversalRecordModifyRsp universalRecordModifyRsp = null;
        try {
            init();
            XMLFileUtility.createXMLFile(universalRecordModifyReq, "universalRecordModifyReq.xml");
            travelportLogger.debug("universalRecordModifyReq " + new Date() + " ------>> " + new XStream().toXML(universalRecordModifyReq));
            universalRecordModifyRsp = universalRecordModifyServicePortType.service(universalRecordModifyReq, null);
            XMLFileUtility.createXMLFile(universalRecordModifyRsp, "universalRecordModifyRsp.xml");

            travelportLogger.debug("universalRecordModifyRsp " + new Date() +" ------>> "+ new XStream().toXML(universalRecordModifyRsp));
        } catch (UniversalModifyFaultMessage | AvailabilityFaultMessage | UniversalRecordFaultMessage modifyFaultMessage) {
            logger.error("Error in addPricingToPNR",modifyFaultMessage);
            XMLFileUtility.createXMLFile(modifyFaultMessage, "universlModfiyException.xml");
            modifyFaultMessage.printStackTrace();
            throw new BaseCompassitesException(modifyFaultMessage.getMessage());
        }

        return universalRecordModifyRsp;

    }

    public static UniversalRecordModifyRsp cancelPricing(String gdsPNR,String reservationLocatorCode, String universalRecordLocatorCode,
                                                         BigInteger version, List<AirPricingInfo> airPricingInfoList) throws BaseCompassitesException {
        logger.debug("cancelPricing called .............");
        UniversalRecordModifyReq universalRecordModifyReq = creatModifyRequestObject(version);
        universalRecordModifyReq.setReturnRecord(true);

        BillingPointOfSaleInfo billingPointOfSaleInfo = createBillingPOS();
        universalRecordModifyReq.setBillingPointOfSaleInfo(billingPointOfSaleInfo);

        RecordIdentifier recordIdentifier = createRecordIdentifier(gdsPNR, universalRecordLocatorCode);
        universalRecordModifyReq.setRecordIdentifier(recordIdentifier);

        int i = 0;
        for(AirPricingInfo airPricingInfo : airPricingInfoList){
            UniversalModifyCmd universalModifyCmd = new UniversalModifyCmd();
            universalModifyCmd.setKey(""+ i++);
            AirDelete airDelete = new AirDelete();
            airDelete.setReservationLocatorCode(reservationLocatorCode);
            airDelete.setElement(TypeElement.AIR_PRICING_INFO);
            airDelete.setKey(airPricingInfo.getKey());

            universalModifyCmd.setAirDelete(airDelete);
            universalRecordModifyReq.getUniversalModifyCmd().add(universalModifyCmd);
        }

        UniversalRecordModifyRsp universalRecordModifyRsp = null;
        try {
            init();
            XMLFileUtility.createXMLFile(universalRecordModifyReq, "cancelPricingReq.xml");
            travelportLogger.debug("cancelPricingReq" + new Date() + " ------>> " + new XStream().toXML(universalRecordModifyReq));
            universalRecordModifyRsp = universalRecordModifyServicePortType.service(universalRecordModifyReq, null);
            XMLFileUtility.createXMLFile(universalRecordModifyRsp, "cancelPricingRsp.xml");
            travelportLogger.debug("cancelPricingRsp " + new Date() +" ------>> "+ new XStream().toXML(universalRecordModifyRsp));
        } catch (UniversalModifyFaultMessage | AvailabilityFaultMessage | UniversalRecordFaultMessage  universalModifyFaultMessage) {
            logger.error("Error in cancelPricing",universalModifyFaultMessage);
            XMLFileUtility.createXMLFile(universalModifyFaultMessage, "universlModfiyException.xml");
            universalModifyFaultMessage.printStackTrace();
            throw new BaseCompassitesException(universalModifyFaultMessage.getMessage());
        }

        return universalRecordModifyRsp;
    }

    public static BillingPointOfSaleInfo createBillingPOS(){
        BillingPointOfSaleInfo billingPointOfSaleInfo = new BillingPointOfSaleInfo();
        billingPointOfSaleInfo.setOriginApplication(UAPI);

        return billingPointOfSaleInfo;

    }
    public static UniversalRecordModifyReq creatModifyRequestObject(BigInteger version){
        UniversalRecordModifyReq universalRecordModifyReq = new UniversalRecordModifyReq();
        universalRecordModifyReq.setVersion(version);
        universalRecordModifyReq.setAuthorizedBy(AUTHORIZEDBY);
        universalRecordModifyReq.setTargetBranch(BRANCH);

        return universalRecordModifyReq;
    }

    public static RecordIdentifier createRecordIdentifier(String gdsPNR, String universalRecordLocatorCode){
        RecordIdentifier recordIdentifier = new RecordIdentifier();
        recordIdentifier.setProviderCode(GDS);
        recordIdentifier.setProviderLocatorCode(gdsPNR);
        recordIdentifier.setUniversalLocatorCode(universalRecordLocatorCode);

        return recordIdentifier;
    }

}
