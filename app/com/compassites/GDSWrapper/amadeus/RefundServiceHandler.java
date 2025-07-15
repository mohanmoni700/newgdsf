package com.compassites.GDSWrapper.amadeus;

import com.amadeus.wsdl.ticketgtp_v3_v4.TicketGTPPT;
import com.amadeus.wsdl.ticketgtp_v3_v4.TicketGTPService;

import com.amadeus.xml.AmadeusWebServices;
import com.amadeus.xml._2010._06.ticketgtp_v3.*;
import com.thoughtworks.xstream.XStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;


import javax.xml.ws.BindingProvider;

import javax.xml.ws.handler.Handler;
import javax.xml.ws.handler.MessageContext;

import java.net.URL;
import java.util.*;

@Service
public class RefundServiceHandler {

    TicketGTPPT ticketGTPPT;

    public static URL wsdlUrl;

    static Logger amadeusLogger = LoggerFactory.getLogger("amadeus");
    static Logger logger = LoggerFactory.getLogger("gds");

    public static String endPoint = null;

    private BindingProvider bindingProvider;

    static {
        URL url = null;
        try {
            endPoint = play.Play.application().configuration().getString("amadeus.endPointURL");
            url = RefundServiceHandler.class.getResource("/META-INF/wsdl/amadeus4/1ASIWFLYFYH_PDT_TicketGTP_3.1_4.0.wsdl");
        } catch (Exception e) {
            logger.debug("Error in loading Amadeus URL : ", e);
        }
        wsdlUrl = url;
    }

    public RefundServiceHandler() throws Exception {

        TicketGTPService service = new TicketGTPService(wsdlUrl);
        ticketGTPPT = service.getTicketGTPPort();
        bindingProvider = (BindingProvider) ticketGTPPT;

        HashMap<String, List<String>> httpHeaders = new HashMap<>();
        httpHeaders.put("Accept", Collections.singletonList("text/xml, multipart/related"));
        httpHeaders.put("Accept-Encoding", Collections.singletonList("gzip"));
        httpHeaders.put("Content-Encoding", Collections.singletonList("gzip"));

        Map<String, Object> reqContext = bindingProvider.getRequestContext();
        reqContext.put(MessageContext.HTTP_REQUEST_HEADERS, httpHeaders);
        reqContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endPoint);

        if (bindingProvider.getBinding().getHandlerChain().isEmpty()) {
            List<Handler> handlerChain = new ArrayList<>();
            handlerChain.add(new AmadeusSOAPHeaderHandler());
            bindingProvider.getBinding().setHandlerChain(handlerChain);
        }
    }

    public AMATicketInitRefundRS ticketInitRefund(List<String> tickets, models.AmadeusSessionWrapper amadeusSessionWrapper, String searchOfficeId) {
        logger.debug("ticketInitRefund called  at {}................Session Id: {}", new Date(), amadeusSessionWrapper.getSessionId());
        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper, bindingProvider);
        AMATicketInitRefundRQ amaTicketInitRefundRQ = new RefundTicket().getTicketInitRefundRQ(tickets, searchOfficeId);
        amadeusLogger.debug("amaTicketInitRefundRQ {} SessionId: {} ---->{}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(amaTicketInitRefundRQ));
        AMATicketInitRefundRS amaTicketInitRefundRS = ticketGTPPT.ticketInitRefund(amaTicketInitRefundRQ, amadeusSessionWrapper.getmSession(), amadeusSessionWrapper.getTransactionFlowLinkTypeHolder(), amadeusSessionWrapper.getAmaSecurityHostedUser());
        amadeusLogger.debug("ticketProcessEDocResponse {} SessionId: {} ---->{}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(amaTicketInitRefundRS));
        return amaTicketInitRefundRS;
    }

    public AMATicketIgnoreRefundRS ticketIgnoreRefundRQ(models.AmadeusSessionWrapper amadeusSessionWrapper) {
        logger.debug("Ticket Ignore Refund called  at {}................Session Id: {}", new Date(), amadeusSessionWrapper.getSessionId());
        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper, bindingProvider);
        AMATicketIgnoreRefundRQ amaTicketIgnoreRefundRQ = new RefundTicket().getTicketIgnoreRefundRQ();
        amadeusLogger.debug("amaTicketIgnoreRefundRQ {} SessionId: {} ---->{}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(amaTicketIgnoreRefundRQ));
        AMATicketIgnoreRefundRS amaTicketIgnoreRefundRS = ticketGTPPT.ticketIgnoreRefund(amaTicketIgnoreRefundRQ, amadeusSessionWrapper.getmSession(), amadeusSessionWrapper.getTransactionFlowLinkTypeHolder(), amadeusSessionWrapper.getAmaSecurityHostedUser());
        amadeusLogger.debug("amaTicketIgnoreRefundRS {} SessionId: {} ---->{}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(amaTicketIgnoreRefundRS));
        return amaTicketIgnoreRefundRS;
    }

    public AMATicketProcessRefundRS ticketProcessRefund(models.AmadeusSessionWrapper amadeusSessionWrapper) {
        logger.debug("Ticket Process Refund called  at {}................Session Id: {}", new Date(), amadeusSessionWrapper.getSessionId());
        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper, bindingProvider);
        AMATicketProcessRefundRQ amaTicketProcessRefundRQ = new RefundTicket().getTicketProcessRefundRQ();
        amadeusLogger.debug("amaTicketProcessRefundRQ {} SessionId: {} ---->{}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(amaTicketProcessRefundRQ));
        AMATicketProcessRefundRS amaTicketProcessRefundRS = ticketGTPPT.ticketProcessRefund(amaTicketProcessRefundRQ, amadeusSessionWrapper.getmSession(), amadeusSessionWrapper.getTransactionFlowLinkTypeHolder(), amadeusSessionWrapper.getAmaSecurityHostedUser());
        amadeusLogger.debug("amaTicketProcessRefundRS {} SessionId: {} ---->{}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(amaTicketProcessRefundRS));
        return amaTicketProcessRefundRS;
    }


}

