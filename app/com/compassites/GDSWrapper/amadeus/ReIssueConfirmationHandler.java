package com.compassites.GDSWrapper.amadeus;

import com.amadeus.wsdl.ticket_rebookandrepricepnr_v1_v4.TicketRebookAndRepricePNRPT;
import com.amadeus.wsdl.ticket_rebookandrepricepnr_v1_v4.TicketRebookAndRepricePNRService;
import com.amadeus.xml._2010._06.ticket_rebookandrepricepnr_v1.AMATicketRebookAndRepricePNRRQ;
import com.amadeus.xml._2010._06.ticket_rebookandrepricepnr_v1.AMATicketRebookAndRepricePNRRS;
import com.thoughtworks.xstream.XStream;
import dto.reissue.ReIssueConfirmationRequest;
import models.AmadeusSessionWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.xml.ws.BindingProvider;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.handler.MessageContext;
import java.net.URL;
import java.util.*;

@Service
public class ReIssueConfirmationHandler {

    TicketRebookAndRepricePNRPT ticketRebookAndRepricePNRPT;

    public static URL wsdlUrl;

    private final BindingProvider bindingProvider;

    static Logger amadeusLogger = LoggerFactory.getLogger("amadeus");

    static Logger logger = LoggerFactory.getLogger("gds");

    public static String endPoint = null;

    static {
        URL url = null;
        try {
            endPoint = play.Play.application().configuration().getString("amadeus.endPointURL");
            url = ReIssueConfirmationHandler.class.getResource("/META-INF/wsdl/amadeus4/1ASIWFLYFYH_PDT_Ticket_RebookAndRepricePNR_1.0_4.0.wsdl");
        } catch (Exception e) {
            logger.debug("Error in loading Ticket_RebookAndRepricePNR URL : {} \n ", e.getMessage(), e);
        }
        wsdlUrl = url;
    }

    public ReIssueConfirmationHandler() throws Exception {

        TicketRebookAndRepricePNRService service = new TicketRebookAndRepricePNRService(wsdlUrl);
        ticketRebookAndRepricePNRPT = service.getTicketRebookAndRepricePNRPort();
        bindingProvider = (BindingProvider) ticketRebookAndRepricePNRPT;

        HashMap<String, List<String>> httpHeaders = new HashMap<>();
        httpHeaders.put("Accept", Collections.singletonList("text/xml, multipart/related"));
        httpHeaders.put("Accept-Encoding", Collections.singletonList("gzip"));
        httpHeaders.put("Content-Encoding", Collections.singletonList("gzip"));

        Map<String, Object> reqContext = bindingProvider.getRequestContext();
        reqContext.put(MessageContext.HTTP_REQUEST_HEADERS, httpHeaders);
        reqContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endPoint);

        List<Handler> handlerChain = new ArrayList<>();
        handlerChain.add(new AmadeusSOAPHeaderHandler());
        bindingProvider.getBinding().setHandlerChain(handlerChain);

    }


    public AMATicketRebookAndRepricePNRRS rebookAndRepricePNR(ReIssueConfirmationRequest reIssueConfirmationRequest, String splitChildPnr, List<String> cabinClassList, AmadeusSessionWrapper amadeusSessionWrapper) {

        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper, bindingProvider);
        AMATicketRebookAndRepricePNRRQ amaTicketRebookAndRepricePNRRQ = ReIssueTicket.ReIssueConfirmation.createReIssueRebookAndRepriceReq(reIssueConfirmationRequest, splitChildPnr, cabinClassList);
        amadeusLogger.debug("ReIssue Confirmation AMATicketRebookAndRepricePNRRQ Request Body {} SessionId: {} \n {}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(amaTicketRebookAndRepricePNRRQ));
        AMATicketRebookAndRepricePNRRS amaTicketRebookAndRepricePNRRS = ticketRebookAndRepricePNRPT.ticketRebookAndRepricePNR(amaTicketRebookAndRepricePNRRQ, amadeusSessionWrapper.getmSession(), amadeusSessionWrapper.getTransactionFlowLinkTypeHolder(), amadeusSessionWrapper.getAmaSecurityHostedUser());
        amadeusLogger.debug("ReIssue Confirmation AMATicketRebookAndRepricePNRRS Response Body {} SessionId: {} \n {}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(amaTicketRebookAndRepricePNRRS));

        return amaTicketRebookAndRepricePNRRS;
    }


}
