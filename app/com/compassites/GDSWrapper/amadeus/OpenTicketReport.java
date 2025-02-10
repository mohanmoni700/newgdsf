package com.compassites.GDSWrapper.amadeus;

import com.amadeus.xml.tatreq_20_1_1a.*;
import dto.OpenTicketDTO;

import java.util.ArrayList;
import java.util.List;

public class OpenTicketReport {
    public static class OpenTicketReportRequest {
        public static TicketProcessEDoc createOpenTicketRequest(List<OpenTicketDTO> ticketDTOList) {
            TicketProcessEDoc ticketProcessEDoc = new TicketProcessEDoc();

            MessageActionDetailsType messageActionDetailsType = new MessageActionDetailsType();
            MessageFunctionBusinessDetailsType messageFunctionDetails = new MessageFunctionBusinessDetailsType();
            messageFunctionDetails.setMessageFunction(new String("131"));
            messageActionDetailsType.setMessageFunctionDetails(messageFunctionDetails);
            ticketProcessEDoc.setMsgActionDetails(messageActionDetailsType);

            List<TicketProcessEDoc.InfoGroup> infoGroup = new ArrayList<>();
            for (OpenTicketDTO openTicketDTO: ticketDTOList) {
                TicketProcessEDoc.InfoGroup infoGroup1 = new TicketProcessEDoc.InfoGroup();
                TicketNumberTypeI docInfo = new TicketNumberTypeI();
                TicketNumberDetailsTypeI documentDetails = new TicketNumberDetailsTypeI();
                documentDetails.setNumber(openTicketDTO.getTicketNo());
                docInfo.setDocumentDetails(documentDetails);
                infoGroup1.setDocInfo(docInfo);
                infoGroup.add(infoGroup1);
            }
            ticketProcessEDoc.getInfoGroup().addAll(infoGroup);
            return ticketProcessEDoc;
        }
    }
}
