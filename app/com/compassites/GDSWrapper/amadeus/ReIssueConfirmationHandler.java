package com.compassites.GDSWrapper.amadeus;

import com.amadeus.wsdl.ticket_rebookandrepricepnr_v1_v2.TicketRebookAndRepricePNRPT;
import com.amadeus.wsdl.ticket_rebookandrepricepnr_v1_v2.TicketRebookAndRepricePNRService;
import com.amadeus.xml._2010._06.retailing_types_v2.CommitType;
import com.amadeus.xml._2010._06.retailing_types_v2.ReservationType;
import com.amadeus.xml._2010._06.ticket_rebookandrepricepnr_v1.AMATicketRebookAndRepricePNRRQ;
import com.amadeus.xml._2010._06.ticket_rebookandrepricepnr_v1.AMATicketRebookAndRepricePNRRS;
import com.thoughtworks.xstream.XStream;
import dto.reissue.ReIssueConfirmationRequest;
import models.AmadeusSessionWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.xml.ws.BindingProvider;
import javax.xml.ws.handler.MessageContext;
import java.net.URL;
import java.util.*;

@Service
public class ReIssueConfirmationHandler {

    TicketRebookAndRepricePNRPT ticketRebookAndRepricePNRPT;

    public static URL wsdlUrl;

    static Logger amadeusLogger = LoggerFactory.getLogger("amadeus");

    static Logger logger = LoggerFactory.getLogger("gds");

    public static String endPoint = null;

    static {
        URL url = null;
        try {
            endPoint = play.Play.application().configuration().getString("amadeus.endPointURL");
            url = ReIssueConfirmationHandler.class.getResource("/wsdl/amadeus/1ASIWFLYFYH_PDT_Ticket_RebookAndRepricePNR_1.0_2.0.wsdl");
        } catch (Exception e) {
            logger.debug("Error in loading Ticket_RebookAndRepricePNR URL : {} \n ", e.getMessage(), e);
        }
        wsdlUrl = url;
    }

    public ReIssueConfirmationHandler() throws Exception {

        URL wsdlUrl = getClass().getClassLoader().getResource("META-INF/wsdl/amadeus/1ASIWFLYFYH_PDT_Ticket_RebookAndRepricePNR_1.0_2.0.wsdl");
        TicketRebookAndRepricePNRService service = new TicketRebookAndRepricePNRService(wsdlUrl);
        ticketRebookAndRepricePNRPT = service.getTicketRebookAndRepricePNRPort();

        HashMap httpHeaders = new HashMap();
        httpHeaders.put("Content-Encoding", Collections.singletonList("gzip"));
        httpHeaders.put("Accept-Encoding", Collections.singletonList("gzip"));
        Map reqContext = ((BindingProvider) ticketRebookAndRepricePNRPT).getRequestContext();
        reqContext.put(MessageContext.HTTP_REQUEST_HEADERS, httpHeaders);
        reqContext.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endPoint);

    }


    public AMATicketRebookAndRepricePNRRS rebookAndRepricePNR(ReIssueConfirmationRequest reIssueConfirmationRequest, String splitChildPnr, List<String> cabinClassList, AmadeusSessionWrapper amadeusSessionWrapper) {

        amadeusSessionWrapper.incrementSequenceNumber(amadeusSessionWrapper);
        AMATicketRebookAndRepricePNRRQ amaTicketRebookAndRepricePNRRQ = ReIssueTicket.ReIssueConfirmation.createReIssueRebookAndRepriceReq(reIssueConfirmationRequest, splitChildPnr, cabinClassList);
        amadeusLogger.debug("ReIssue Confirmation AMATicketRebookAndRepricePNRRQ Request Body {} SessionId: {} \n {}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(amaTicketRebookAndRepricePNRRQ));
        AMATicketRebookAndRepricePNRRS amaTicketRebookAndRepricePNRRS = ticketRebookAndRepricePNRPT.ticketRebookAndRepricePNR(amaTicketRebookAndRepricePNRRQ, amadeusSessionWrapper.getmSession());
        amadeusLogger.debug("ReIssue Confirmation AMATicketRebookAndRepricePNRRS Response Body {} SessionId: {} \n {}", new Date(), amadeusSessionWrapper.getSessionId(), new XStream().toXML(amaTicketRebookAndRepricePNRRS));

        return amaTicketRebookAndRepricePNRRS;
    }


}
