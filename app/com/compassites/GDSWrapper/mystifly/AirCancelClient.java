package com.compassites.GDSWrapper.mystifly;

import java.rmi.RemoteException;
import java.util.Date;

import onepoint.mystifly.CancelBookingDocument;
import onepoint.mystifly.CancelBookingDocument.CancelBooking;
import onepoint.mystifly.OnePointStub;

import org.datacontract.schemas._2004._07.mystifly_onepoint.AirCancelRQ;
import org.datacontract.schemas._2004._07.mystifly_onepoint.SessionCreateRS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.XMLFileUtility;

/**
 * @author Santhosh
 */
public class AirCancelClient {

	static Logger mystiflyLogger = LoggerFactory.getLogger("mystifly");

	public void cancelBooking(String pnr) throws RemoteException {
		SessionsHandler sessionsHandler = new SessionsHandler();
		//SessionCreateRS sessionRS = sessionsHandler.login();
		String sessoinId = sessionsHandler.mystiflySessionHandler();
		OnePointStub onePointStub = sessionsHandler.getOnePointStub();
		
		CancelBookingDocument cancelBookingDoc = CancelBookingDocument.Factory.newInstance();
		CancelBooking cancelBooking = cancelBookingDoc.getCancelBooking();
		AirCancelRQ airCancelRQ = cancelBooking.addNewRq();
		
		airCancelRQ.setSessionId(sessoinId);
		airCancelRQ.setTarget(Mystifly.TARGET);
		airCancelRQ.setUniqueID(pnr);
		XMLFileUtility.createFile(cancelBookingDoc.xmlText(), "CancelBookingRQ.xml");
		mystiflyLogger.debug("CancelBookingRQ "+ new Date() +" ----->>" + cancelBookingDoc.xmlText());
		onePointStub.cancelBooking(cancelBookingDoc);
		mystiflyLogger.debug("CancelBookingRS "+ new Date() +" ----->>" + cancelBookingDoc.xmlText());
		XMLFileUtility.createFile(cancelBookingDoc.xmlText(), "CancelBookingRS.xml");
	}

}
