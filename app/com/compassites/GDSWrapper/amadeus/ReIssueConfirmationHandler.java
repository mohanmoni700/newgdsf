package com.compassites.GDSWrapper.amadeus;

import com.amadeus.wsdl.ticket_rebookandrepricepnr_v1_v2.TicketRebookAndRepricePNRPT;
import com.amadeus.wsdl.ticket_rebookandrepricepnr_v1_v2.TicketRebookAndRepricePNRService;
import com.amadeus.xml._2010._06.retailing_types_v2.CommitType;
import com.amadeus.xml._2010._06.retailing_types_v2.ReservationType;
import com.amadeus.xml._2010._06.ticket_rebookandrepricepnr_v1.AMATicketRebookAndRepricePNRRQ;
import com.amadeus.xml._2010._06.ticket_rebookandrepricepnr_v1.AMATicketRebookAndRepricePNRRS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.ws.BindingProvider;
import javax.xml.ws.handler.MessageContext;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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


    public AMATicketRebookAndRepricePNRRQ rebookAndRepricePNR(){

        AMATicketRebookAndRepricePNRRQ amaTicketRebookAndRepricePNRRQ = new AMATicketRebookAndRepricePNRRQ();

        ReservationType reservation = new ReservationType();
        CommitType commit = new CommitType();
        AMATicketRebookAndRepricePNRRQ.Rebooking rebooking = new AMATicketRebookAndRepricePNRRQ.Rebooking();
        AMATicketRebookAndRepricePNRRQ.Repricing repricing = new AMATicketRebookAndRepricePNRRQ.Repricing();

        //Set the PNR here
//        reservation.setBookingIdentifier();




        return new AMATicketRebookAndRepricePNRRQ();
    }


}
