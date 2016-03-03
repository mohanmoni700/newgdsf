package com.compassites.GDSWrapper.travelport;

import com.thoughtworks.xstream.XStream;
import com.travelport.schema.common_v26_0.BillingPointOfSaleInfo;
import com.travelport.schema.universal_v26_0.UniversalRecordImportReq;
import com.travelport.schema.universal_v26_0.UniversalRecordImportRsp;
import com.travelport.schema.universal_v26_0.UniversalRecordRetrieveReq;
import com.travelport.schema.universal_v26_0.UniversalRecordRetrieveRsp;
import com.travelport.service.universal_v26_0.*;
import org.apache.commons.logging.LogFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.XMLFileUtility;

import javax.xml.ws.BindingProvider;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Date;

/**
 * Created by yaseen on 29-02-2016.
 */
public class UniversalRecordImportClient extends TravelPortClient{

    static final String ServiceName =  "/UniversalRecordService";
    static UniversalRecordImportService universalRecordImportService = null;
    static UniversalRecordImportServicePortType universalRecordImportServicePortType = null;
    static Logger travelportLogger = LoggerFactory.getLogger("travelport");

    static Logger logger = LoggerFactory.getLogger("gds");

    static void  init(){
        if (universalRecordImportService == null){
            try {
                String path = new File(".").getCanonicalPath();
                universalRecordImportService = new UniversalRecordImportService(new java.net.URL("http://localhost:9000/wsdl/galileo/universal_v26_0/UniversalRecord.wsdl"));
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (universalRecordImportServicePortType == null){
            universalRecordImportServicePortType  = universalRecordImportService.getUniversalRecordImportServicePort();
            LogFactory.getLog(UniversalRecordImportClient.class).info("Initializing universalRecordImportServicePortType....");
            setRequestContext((BindingProvider) universalRecordImportServicePortType, ServiceName);
            LogFactory.getLog(UniversalRecordImportClient.class).info("Initialized");
        }
    }

    public static UniversalRecordImportRsp importPNR(String locatorCode) {
        logger.debug("Travelport UniversalRecordClient.retrievePNR called :" );
        UniversalRecordImportReq recordRetrieveReq = new UniversalRecordImportReq();
        recordRetrieveReq.setAuthorizedBy("TEST");
        recordRetrieveReq.setTargetBranch(BRANCH);

        BillingPointOfSaleInfo billingPointOfSaleInfo = new BillingPointOfSaleInfo();
        billingPointOfSaleInfo.setOriginApplication(UAPI);
        recordRetrieveReq.setBillingPointOfSaleInfo(billingPointOfSaleInfo);
        UniversalRecordImportRsp recordRetrieveRsp = null;
        recordRetrieveReq.setProviderCode(GDS);
        recordRetrieveReq.setProviderLocatorCode(locatorCode);
        try {
            init();
            XMLFileUtility.createXMLFile(recordRetrieveReq, "UniversalRecordImportReq.xml");
            travelportLogger.debug("UniversalRecordImportReq " + new Date() + " ------>> " + new XStream().toXML(recordRetrieveReq));
            recordRetrieveRsp = universalRecordImportServicePortType.service(
                    recordRetrieveReq, null);
            XMLFileUtility.createXMLFile(recordRetrieveRsp, "UniversalRecordImportRes.xml");
            travelportLogger.debug("UniversalRecordImportRes " + new Date() + " ------>> " + new XStream().toXML(recordRetrieveRsp));
            logger.debug("Travelport UniversalRecordImport.retrievePNR response received :" );

        } catch (UniversalRecordFaultMessage universalRecordFaultMessage) {
            logger.debug("Error in UniversalRecordImport.retrievePNR " + universalRecordFaultMessage.getMessage());
            logger.error("Error in UniversalRecordImport.retrievePNR ", universalRecordFaultMessage);
            universalRecordFaultMessage.printStackTrace();
        }
        return recordRetrieveRsp;
    }
}
