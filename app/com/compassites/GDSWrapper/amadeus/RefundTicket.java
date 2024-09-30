package com.compassites.GDSWrapper.amadeus;

import com.amadeus.xml._2010._06.ticketgtp_v3.AMATicketCancelTransactionRQ;
import com.amadeus.xml._2010._06.ticketgtp_v3.AMATicketIgnoreRefundRQ;
import com.amadeus.xml._2010._06.ticketgtp_v3.AMATicketInitRefundRQ;
import com.amadeus.xml._2010._06.ticketgtp_v3.AMATicketProcessRefundRQ;
import com.amadeus.xml.farqnq_07_1_1a.FareCheckRules;
import com.amadeus.xml.tatreq_20_1_1a.*;

import java.math.BigDecimal;
import java.util.List;

public class RefundTicket {

    public TicketProcessEDoc getTicketProcessEdocRQ(List<String> tickets){
        TicketProcessEDoc ticketProcessEDoc = new TicketProcessEDoc();
        MessageActionDetailsType messageActionDetailsType = new MessageActionDetailsType();
        MessageFunctionBusinessDetailsType messageFunctionBusinessDetailsType = new MessageFunctionBusinessDetailsType();
        messageFunctionBusinessDetailsType.setMessageFunction("131");
        messageActionDetailsType.setMessageFunctionDetails(messageFunctionBusinessDetailsType);
        ticketProcessEDoc.setMsgActionDetails(messageActionDetailsType);
        tickets.stream().forEach(ticket->{
            TicketProcessEDoc.InfoGroup infoGroup = new TicketProcessEDoc.InfoGroup();
            TicketNumberTypeI ticketNumberTypeI = new TicketNumberTypeI();
            TicketNumberDetailsTypeI ticketNumberDetailsTypeI = new TicketNumberDetailsTypeI();
            ticketNumberDetailsTypeI.setNumber(ticket);
            ticketNumberTypeI.setDocumentDetails(ticketNumberDetailsTypeI);
            infoGroup.setDocInfo(ticketNumberTypeI);
            ticketProcessEDoc.getInfoGroup().add(infoGroup);
        });

     return ticketProcessEDoc;
    }

    public AMATicketInitRefundRQ getTicketInitRefundRQ(List<String> tickets,String searchOfficeId){
        AMATicketInitRefundRQ amaTicketInitRefundRQ = new AMATicketInitRefundRQ();
        amaTicketInitRefundRQ.setVersion(new BigDecimal(3.000));
        AMATicketInitRefundRQ.Contracts contracts = new AMATicketInitRefundRQ.Contracts();
        tickets.stream().forEach(ticket ->{
            AMATicketInitRefundRQ.Contracts.Contract contract = new AMATicketInitRefundRQ.Contracts.Contract();
            contract.setNumber(ticket);
            contracts.getContract().add(contract);
        });
        amaTicketInitRefundRQ.setContracts(contracts);
        if(searchOfficeId != null && !searchOfficeId.equalsIgnoreCase("BOMAK38SN")) {
            AMATicketInitRefundRQ.ActionDetails actionDetails = new AMATicketInitRefundRQ.ActionDetails();
            AMATicketInitRefundRQ.ActionDetails.ActionDetail actionDetail = new AMATicketInitRefundRQ.ActionDetails.ActionDetail();
            actionDetail.setIndicator("ATC");
            actionDetails.getActionDetail().add(actionDetail);
            amaTicketInitRefundRQ.setActionDetails(actionDetails);
        }
        return amaTicketInitRefundRQ;
    }

    public AMATicketIgnoreRefundRQ getTicketIgnoreRefundRQ(){
       AMATicketIgnoreRefundRQ amaTicketIgnoreRefundRQ = new AMATicketIgnoreRefundRQ();
        amaTicketIgnoreRefundRQ.setVersion(new BigDecimal(3.000));
        return amaTicketIgnoreRefundRQ;
    }
    public AMATicketProcessRefundRQ getTicketProcessRefundRQ(){
        AMATicketProcessRefundRQ amaTicketProcessRefundRQ = new AMATicketProcessRefundRQ();
        amaTicketProcessRefundRQ.setVersion(new BigDecimal(3.000));
        return amaTicketProcessRefundRQ;
    }
}
