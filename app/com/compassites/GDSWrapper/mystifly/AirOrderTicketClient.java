package com.compassites.GDSWrapper.mystifly;

import java.rmi.RemoteException;

import onepoint.mystifly.OnePointStub;
import onepoint.mystifly.TicketOrderDocument;
import onepoint.mystifly.TicketOrderDocument.TicketOrder;
import onepoint.mystifly.TicketOrderResponseDocument;

import org.datacontract.schemas._2004._07.mystifly_onepoint.AirOrderTicketRQ;
import org.datacontract.schemas._2004._07.mystifly_onepoint.AirOrderTicketRS;
import org.datacontract.schemas._2004._07.mystifly_onepoint.SessionCreateRS;

/**
 * @author Santhosh
 */
public class AirOrderTicketClient {
	
	public AirOrderTicketRS issueTicket(String pnr) throws RemoteException {
		SessionsHandler sessionsHandler = new SessionsHandler();
		SessionCreateRS sessionRS = sessionsHandler.login();
		OnePointStub onePointStub = sessionsHandler.getOnePointStub();
		
		TicketOrderDocument ticketOrderDoc = TicketOrderDocument.Factory.newInstance();
		
		TicketOrder ticketOrder = ticketOrderDoc.addNewTicketOrder();
		AirOrderTicketRQ airOrderTicketRQ = ticketOrder.addNewRq();
		
		airOrderTicketRQ.setSessionId(sessionRS.getSessionId());
		airOrderTicketRQ.setTarget(Mystifly.TARGET);
		airOrderTicketRQ.setUniqueID(pnr);
//		airOrderTicketRQ.setFareSourceCode("");
		
		TicketOrderResponseDocument ticketOrderRSDoc = onePointStub.ticketOrder(ticketOrderDoc);
		return ticketOrderRSDoc.getTicketOrderResponse().getTicketOrderResult();
	}

}
