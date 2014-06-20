package com.compassites.GDSWrapper.travelport;

import com.travelport.schema.common_v12_0.BillingPointOfSaleInfo;
import com.travelport.schema.system_v8_0.PingReq;
import com.travelport.schema.system_v8_0.PingRsp;
import com.travelport.service.system_v8_0.SystemFaultMessage;
import com.travelport.service.system_v8_0.SystemPingPortType;
import com.travelport.service.system_v8_0.SystemService;
import org.apache.commons.logging.LogFactory;

import javax.xml.ws.BindingProvider;


/**
 * Created with IntelliJ IDEA.
 * User: Renu
 * Date: 5/15/14
 * Time: 2:06 PM
 * To change this template use File | Settings | File Templates.
 */
public class PingReqClient extends TravelPortClient {
    static final String ServiceName =  "/SystemService";

    static SystemService systemService = new SystemService();
    static SystemPingPortType port =   null;

    static void  init(){
        if (port == null){
            LogFactory.getLog(PingReqClient.class).info("Initializing SystemPingPortType....");
            port  = systemService.getSystemPingPort();
            setRequestContext((BindingProvider) port, ServiceName);
            LogFactory.getLog(PingReqClient.class).info("Initialized");
        }
    }


    public static String ping(String payload){
        PingReq pingReq = new PingReq();
        pingReq.setTargetBranch(BRANCH);
        pingReq.setTraceId("Random1121");
        pingReq.setTokenId("SomeTokenId");
        pingReq.setPayload(payload);
        BillingPointOfSaleInfo billingPointOfSaleInfo= new BillingPointOfSaleInfo();
        billingPointOfSaleInfo.setOriginApplication("JustOneClick");
        pingReq.setBillingPointOfSaleInfo(billingPointOfSaleInfo);
        try {
            init();
            PingRsp pingRsp = port.service(pingReq);
            String payloadRsp = pingRsp.getPayload();
            String transactionId = pingRsp.getTransactionId();
            LogFactory.getLog(PingReqClient.class).info("The response [" + transactionId + "] contained the payload '" + payloadRsp + "'");
            return payloadRsp;
        } catch (SystemFaultMessage systemFaultMessage) {
            systemFaultMessage.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        return "";
    }

}
