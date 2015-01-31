package com.compassites.GDSWrapper.mystifly;

import java.rmi.RemoteException;

import onepoint.mystifly.CancelBookingDocument;
import onepoint.mystifly.CancelBookingDocument.CancelBooking;
import onepoint.mystifly.OnePointStub;

import org.datacontract.schemas._2004._07.mystifly_onepoint.AirCancelRQ;
import org.datacontract.schemas._2004._07.mystifly_onepoint.SessionCreateRS;

/**
 * @author Santhosh
 */
public class AirCancelClient {
	
	public void cancelBooking(String pnr) throws RemoteException {
		SessionsHandler sessionsHandler = new SessionsHandler();
		SessionCreateRS sessionRS = sessionsHandler.login();
		OnePointStub onePointStub = sessionsHandler.getOnePointStub();
		
		CancelBookingDocument cancelBookingDoc = CancelBookingDocument.Factory.newInstance();
		CancelBooking cancelBooking = cancelBookingDoc.getCancelBooking();
		AirCancelRQ airCancelRQ = cancelBooking.addNewRq();
		
		airCancelRQ.setSessionId(sessionRS.getSessionId());
		airCancelRQ.setTarget(Mystifly.TARGET);
		airCancelRQ.setUniqueID(pnr);
		
		onePointStub.cancelBooking(cancelBookingDoc);
	}

}
