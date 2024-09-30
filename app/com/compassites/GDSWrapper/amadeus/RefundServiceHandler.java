package com.compassites.GDSWrapper.amadeus;

import com.amadeus.wsdl.ticketgtp_v3_v2.TicketGTPPT;
import com.amadeus.wsdl.ticketgtp_v3_v2.TicketGTPService;
import com.amadeus.xml.AmadeusWebServices;

import com.amadeus.xml._2010._06.ticketgtp_v3.*;
import com.thoughtworks.xstream.XStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;


import javax.xml.ws.BindingProvider;

import javax.xml.ws.handler.MessageContext;

import java.net.URL;
import java.util.*;

@Service
public class RefundServiceHandler {

    TicketGTPPT ticketGTPPT;
    //SessionHandler mSession;

    public static URL wsdlUrl;

    static Logger amadeusLogger = LoggerFactory.getLogger("amadeus");

    static Logger logger = LoggerFactory.getLogger("gds");
    static Logger loggerTemp = LoggerFactory.getLogger("gds_search");

    public static String endPoint = null;

    static {
        URL url = null;
        try{
            endPoint = play.Play.application().configuration().getString("amadeus.endPointURL");
            url = RefundServiceHandler.class.getResource("/wsdl/amadeus/1ASIWFLYFYH_PDT_TicketGTP_3.1_2.0.wsdl");
        }catch (Exception e){
            logger.debug("Error in loading Amadeus URL : ", e);
        }
        wsdlUrl = url;
    }

    public RefundServiceHandler() throws Exception{
//        URL wsdlUrl=ServiceHandler.class.getResource("/wsdl/amadeus/1ASIWFLYFYH_PDT_20140429_052541.wsdl");
//        URL wsdlUrl = ServiceHandler.class.getResource("/wsdl/amadeus/amadeus.wsdl");
       // wsdlUrl = new URL("jar:file:/home/sri/Joco/gdsservice/lib/GDSWsdlWrapper.jar!/META-INF/wsdl/amadeus/1ASIWFLYFYH_PDT_TicketGTP_3.1_2.0.wsdl");
        URL wsdlUrl = getClass().getClassLoader().getResource("META-INF/wsdl/amadeus/1ASIWFLYFYH_PDT_TicketGTP_3.1_2.0.wsdl");
        TicketGTPService service = new TicketGTPService(wsdlUrl);
        ticketGTPPT = service.getTicketGTPPort();

        //gzip compression headers
        HashMap httpHeaders = new HashMap();
        httpHeaders.put("Content-Encoding", Collections.singletonList("gzip"));
        httpHeaders.put("Accept-Encoding", Collections.singletonList("gzip"));
        Map reqContext = ((BindingProvider) ticketGTPPT).getRequestContext();
        reqContext.put(MessageContext.HTTP_REQUEST_HEADERS, httpHeaders);
        reqContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endPoint);
        //mSession = new SessionHandler();
    }

//    public SessionHandler logIn(SessionHandler mSession) {
//        logger.debug("amadeus login called....................");
//        SecurityAuthenticate securityAuthenticateReq = MessageFactory.getInstance().getAuthenticationRequest();
//        logger.debug("amadeus login called at : " + new Date() + " " + mSession.getSessionId());
//        amadeusLogger.debug("securityAuthenticateReq " + new Date() + " ---->" + new XStream().toXML(securityAuthenticateReq));
//        SecurityAuthenticateReply securityAuthenticate = mPortType.securityAuthenticate(securityAuthenticateReq, mSession.getSession());
//        amadeusLogger.debug("securityAuthenticateRes " + new Date() + " ---->" + new XStream().toXML(securityAuthenticate));
//        return mSession;
//    }

    //todo

    public AMATicketInitRefundRS ticketInitRefund(List<String> tickets, models.AmadeusSessionWrapper
    amadeusSessionWrapper,String searchOfficeId){
        logger.debug("ticketInitRefund called  at " + new Date() + "................Session Id: "+ amadeusSessionWrapper.getSessionId());
        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper);
        AMATicketInitRefundRQ amaTicketInitRefundRQ = new RefundTicket().getTicketInitRefundRQ(tickets,searchOfficeId);
        amadeusLogger.debug("amaTicketInitRefundRQ " + new Date() + " SessionId: " + amadeusSessionWrapper.getSessionId()+ " ---->" + new XStream().toXML(amaTicketInitRefundRQ));
        AMATicketInitRefundRS amaTicketInitRefundRS =ticketGTPPT.ticketInitRefund(amaTicketInitRefundRQ,amadeusSessionWrapper.getmSession());
        amadeusLogger.debug("ticketProcessEDocResponse " + new Date() + " SessionId: " + amadeusSessionWrapper.getSessionId()+ " ---->" + new XStream().toXML(amaTicketInitRefundRS));
        return amaTicketInitRefundRS;
    }

    public AMATicketIgnoreRefundRS ticketIgnoreRefundRQ(models.AmadeusSessionWrapper amadeusSessionWrapper){
        logger.debug("ticketInitRefund called  at " + new Date() + "................Session Id: "+ amadeusSessionWrapper.getSessionId());
        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper);
        AMATicketIgnoreRefundRQ amaTicketIgnoreRefundRQ = new RefundTicket().getTicketIgnoreRefundRQ();
        amadeusLogger.debug("amaTicketIgnoreRefundRQ " + new Date() + " SessionId: " + amadeusSessionWrapper.getSessionId()+ " ---->" + new XStream().toXML(amaTicketIgnoreRefundRQ));
        AMATicketIgnoreRefundRS amaTicketIgnoreRefundRS = ticketGTPPT.ticketIgnoreRefund(amaTicketIgnoreRefundRQ,amadeusSessionWrapper.getmSession());
        amadeusLogger.debug("amaTicketIgnoreRefundRS " + new Date() + " SessionId: " + amadeusSessionWrapper.getSessionId()+ " ---->" + new XStream().toXML(amaTicketIgnoreRefundRS));
        return amaTicketIgnoreRefundRS;
    }

    public AMATicketProcessRefundRS ticketProcessRefund(models.AmadeusSessionWrapper amadeusSessionWrapper){
        logger.debug("ticketInitRefund called  at " + new Date() + "................Session Id: "+ amadeusSessionWrapper.getSessionId());
        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper);
        AMATicketProcessRefundRQ amaTicketProcessRefundRQ = new RefundTicket().getTicketProcessRefundRQ();
        amadeusLogger.debug("amaTicketProcessRefundRQ " + new Date() + " SessionId: " + amadeusSessionWrapper.getSessionId()+ " ---->" + new XStream().toXML(amaTicketProcessRefundRQ));
        AMATicketProcessRefundRS amaTicketProcessRefundRS = ticketGTPPT.ticketProcessRefund(amaTicketProcessRefundRQ,amadeusSessionWrapper.getmSession());
        amadeusLogger.debug("amaTicketProcessRefundRS " + new Date() + " SessionId: " + amadeusSessionWrapper.getSessionId()+ " ---->" + new XStream().toXML(amaTicketProcessRefundRS));
        return amaTicketProcessRefundRS;
    }


}

