package com.compassites.GDSWrapper.mystifly;

import java.rmi.RemoteException;
import java.util.Date;

import onepoint.mystifly.OnePointStub;
import onepoint.mystifly.TicketOrderDocument;
import onepoint.mystifly.TicketOrderDocument.TicketOrder;
import onepoint.mystifly.TicketOrderResponseDocument;

import org.datacontract.schemas._2004._07.mystifly_onepoint.AirOrderTicketRQ;
import org.datacontract.schemas._2004._07.mystifly_onepoint.AirOrderTicketRS;
import org.datacontract.schemas._2004._07.mystifly_onepoint.SessionCreateRS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.XMLFileUtility;

/**
 * @author Santhosh
 */
public class AirOrderTicketClient {
	static Logger mystiflyLogger = LoggerFactory.getLogger("mystifly");
	public AirOrderTicketRS issueTicket(String pnr) throws RemoteException {
		SessionsHandler sessionsHandler = new SessionsHandler();
		//SessionCreateRS sessionRS = sessionsHandler.login();
		String sessoinId = sessionsHandler.mystiflySessionHandler();
		OnePointStub onePointStub = sessionsHandler.getOnePointStub();
		
		TicketOrderDocument ticketOrderDoc = TicketOrderDocument.Factory.newInstance();
		
		TicketOrder ticketOrder = ticketOrderDoc.addNewTicketOrder();
		AirOrderTicketRQ airOrderTicketRQ = ticketOrder.addNewRq();
		
		airOrderTicketRQ.setSessionId(sessoinId);
		airOrderTicketRQ.setTarget(Mystifly.TARGET);
		airOrderTicketRQ.setUniqueID(pnr);
//		airOrderTicketRQ.setFareSourceCode("");
		XMLFileUtility.createFile(ticketOrderDoc.xmlText(), "AirTicketOrderRQ.xml");
		mystiflyLogger.debug("AirTicketOrderRQ "+ new Date() +" ----->>" + ticketOrderDoc.xmlText());
		TicketOrderResponseDocument ticketOrderRSDoc = onePointStub.ticketOrder(ticketOrderDoc);
		mystiflyLogger.debug("AirTicketOrderRS "+ new Date() +" ----->>" + ticketOrderRSDoc.xmlText());
		XMLFileUtility.createFile(ticketOrderRSDoc.xmlText(), "AirTicketOrderRS.xml");
		return ticketOrderRSDoc.getTicketOrderResponse().getTicketOrderResult();
	}

}
