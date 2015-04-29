package com.compassites.GDSWrapper.travelport;

import com.travelport.schema.air_v26_0.AirFareRulesReq;
import com.travelport.schema.air_v26_0.AirFareRulesRsp;
import com.travelport.schema.air_v26_0.FareRuleKey;
import com.travelport.schema.common_v26_0.BillingPointOfSaleInfo;
import com.travelport.service.air_v26_0.AirFareRulesPortType;
import com.travelport.service.air_v26_0.AirFaultMessage;
import com.travelport.service.air_v26_0.AirService;
import org.apache.commons.logging.LogFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.ws.BindingProvider;
import java.io.IOException;
import java.net.MalformedURLException;

/**
 * Created by Yaseen on 21-04-2015.
 */
public class FareRulesClient extends TravelPortClient {

    static final String ServiceName =  "/AirService";

    static AirService airService = null;

    static AirFareRulesPortType airFareRulesPortType = null;

    static Logger travelportLogger = LoggerFactory.getLogger("travelport");

    static Logger logger = LoggerFactory.getLogger("gds");


    static void  init() {
        if (airService == null) {
            java.net.URL url = null;
            try {
                //String path = new File(".").getCanonicalPath();
                //airService = new AirService(new java.net.URL("file:"+path+"/wsdl/galileo/air_v26_0/Air.wsdl"));
                java.net.URL baseUrl;
                baseUrl = AirService.class.getResource(".");
                url = new java.net.URL(baseUrl, "http://localhost:9000/wsdl/galileo/air_v31_0/Air.wsdl");
                //url = new java.net.URL(baseUrl, "Air.wsdl");
                airService = new AirService(url);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }


        }
        if (airFareRulesPortType == null) {
            airFareRulesPortType = airService.getAirFareRulesPort();
            LogFactory.getLog(FareRulesClient.class).info("Initializing airFareRulesPortType....");
            setRequestContext((BindingProvider) airFareRulesPortType, ServiceName);
            LogFactory.getLog(AirRequestClient.class).info("Initialized");
        }
    }


    public AirFareRulesRsp getFareRules(String fareRuleRef, String fareRuleKey){

        AirFareRulesReq airFareRulesReq = new AirFareRulesReq();
        airFareRulesReq.setAuthorizedBy("TEST");
        airFareRulesReq.setTargetBranch(BRANCH);
        BillingPointOfSaleInfo billInfo = new BillingPointOfSaleInfo();
        billInfo.setOriginApplication(UAPI);
        airFareRulesReq.setBillingPointOfSaleInfo(billInfo);
        FareRuleKey reqFareRuleKey = new FareRuleKey();
        reqFareRuleKey.setFareInfoRef(fareRuleRef);
        reqFareRuleKey.setValue(fareRuleKey);
        reqFareRuleKey.setProviderCode(GDS);
        AirFareRulesRsp airFareDisplayRsp = null;
        try {
            airFareDisplayRsp = airFareRulesPortType.service(airFareRulesReq);
        } catch (AirFaultMessage airFaultMessage) {
            airFaultMessage.printStackTrace();
        }

        return airFareDisplayRsp;
    }
}
