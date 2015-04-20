package com.compassites.GDSWrapper.travelport;

import com.travelport.schema.common_v12_0.BillingPointOfSaleInfo;
import com.travelport.schema.common_v12_0.HostToken;
import com.travelport.schema.terminal_v8_0.CreateTerminalSessionReq;
import com.travelport.schema.terminal_v8_0.CreateTerminalSessionRsp;
import com.travelport.schema.terminal_v8_0.TerminalReq;
import com.travelport.schema.terminal_v8_0.TerminalRsp;
import com.travelport.service.terminal_v8_0.*;
import org.apache.commons.logging.LogFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.XMLFileUtility;

import javax.xml.ws.BindingProvider;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;

/**
 * Created by Yaseen on 19-03-2015.
 */
public class TerminalRequestClient extends TravelPortClient {

    static final String serviceName =  "/TerminalService";
    static TerminalService terminalService = null;
    static TerminalServicePortType terminalServicePortType = null;
    static CreateTerminalSessionServicePortType createTerminalSessionServicePortType = null;
    static EndTerminalSessionServicePortType endTerminalSessionServicePortType = null;
    static Logger travelportLogger = LoggerFactory.getLogger("travelport");

    public TerminalRequestClient() {
        init();
    }

    static void  init(){
        if (terminalService == null){
            try {
                String path = new File(".").getCanonicalPath();
                terminalService = new TerminalService(new java.net.URL("http://localhost:9000/wsdl/galileo/terminal_v8_0/Terminal.wsdl"));
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (terminalServicePortType == null){
            terminalServicePortType  = terminalService.getTerminalServicePort();
            LogFactory.getLog(TerminalRequestClient.class).info("Initializing terminalServicePortType....");
            setRequestContext((BindingProvider) terminalServicePortType, serviceName);
            LogFactory.getLog(TerminalRequestClient.class).info("Initialized");
        }
        if (createTerminalSessionServicePortType == null){
            createTerminalSessionServicePortType  = terminalService.getCreateTerminalSessionServicePort();
            LogFactory.getLog(TerminalRequestClient.class).info("Initializing createTerminalSessionServicePortType....");
            setRequestContext((BindingProvider) createTerminalSessionServicePortType, serviceName);
            LogFactory.getLog(TerminalRequestClient.class).info("Initialized");
        }

        if (endTerminalSessionServicePortType == null){
            endTerminalSessionServicePortType  = terminalService.getEndTerminalSessionServicePort();
            LogFactory.getLog(TerminalRequestClient.class).info("Initializing endTerminalSessionServicePortType....");
            setRequestContext((BindingProvider) endTerminalSessionServicePortType, serviceName);
            LogFactory.getLog(TerminalRequestClient.class).info("Initialized");
        }
    }


    public  String createTerminalSession(){
        CreateTerminalSessionReq createTerminalSessionReq = new CreateTerminalSessionReq();
        createTerminalSessionReq.setHost(GDS);
        createTerminalSessionReq.setTargetBranch(BRANCH);
        BillingPointOfSaleInfo billingPointOfSaleInfo = new BillingPointOfSaleInfo();
        billingPointOfSaleInfo.setOriginApplication(UAPI);
        createTerminalSessionReq.setBillingPointOfSaleInfo(billingPointOfSaleInfo);

        try {
            CreateTerminalSessionRsp createTerminalSessionRsp = createTerminalSessionServicePortType.service(createTerminalSessionReq);
            return  createTerminalSessionRsp.getHostToken().getValue();

        } catch (TerminalFaultMessage terminalFaultMessage) {
            terminalFaultMessage.printStackTrace();
        }

        return  null;
    }

    public List<String> getLowestFare(String token, boolean isSeamen, String pnr){

        retrievePNR(token, pnr);
        String lowestFareCommand = "";
        TerminalReq terminalReq = new TerminalReq();
        HostToken hostToken = new HostToken();
        hostToken.setValue(token);
        hostToken.setHost(GDS);
        terminalReq.setHostToken(hostToken);
        if(isSeamen){
            lowestFareCommand = "FQBA*SEA";
        }else {
            lowestFareCommand = "FQBA";
        }

        terminalReq.setTerminalCommand(lowestFareCommand);

        terminalReq.setTargetBranch(BRANCH);
        terminalReq.setAuthorizedBy("TEST");
        BillingPointOfSaleInfo billingPointOfSaleInfo = new BillingPointOfSaleInfo();
        billingPointOfSaleInfo.setOriginApplication(UAPI);
        terminalReq.setBillingPointOfSaleInfo(billingPointOfSaleInfo);
        TerminalRsp terminalRsp = null;

        try {
            XMLFileUtility.createXMLFile(terminalReq,"terminalLowestFareReq.xml");
            terminalRsp = terminalServicePortType.service(terminalReq);
            XMLFileUtility.createXMLFile(terminalRsp,"terminalLowestFareRes.xml");
        } catch (TerminalFaultMessage terminalFaultMessage) {
            terminalFaultMessage.printStackTrace();
        }

        List<String> terminalResponseList = terminalRsp.getTerminalCommandResponse().getText();

        return terminalResponseList;
    }

    public void retrievePNR(String token, String pnr){
        TerminalReq terminalReq = new TerminalReq();
        HostToken hostToken = new HostToken();
        hostToken.setValue(token);
        hostToken.setHost(GDS);
        terminalReq.setHostToken(hostToken);
//        String lowestFareCommand = "FQBA";
        terminalReq.setTerminalCommand("*"+pnr);

        terminalReq.setTargetBranch(BRANCH);
        terminalReq.setAuthorizedBy("FINE");
        BillingPointOfSaleInfo billingPointOfSaleInfo = new BillingPointOfSaleInfo();
        billingPointOfSaleInfo.setOriginApplication(UAPI);
        terminalReq.setBillingPointOfSaleInfo(billingPointOfSaleInfo);
        TerminalRsp terminalRsp = null;

        try {
            XMLFileUtility.createXMLFile(terminalReq,"terminalRetrievePNRReq.xml");
            terminalRsp = terminalServicePortType.service(terminalReq);
            XMLFileUtility.createXMLFile(terminalRsp,"terminalRetrievePNRRes.xml");
        } catch (TerminalFaultMessage terminalFaultMessage) {
            terminalFaultMessage.printStackTrace();
        }
    }
}


