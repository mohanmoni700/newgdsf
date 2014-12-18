package com.compassites.GDSWrapper.mystifly;

import java.rmi.RemoteException;

import onepoint.mystifly.OnePointStub;
import onepoint.mystifly.TripDetailsDocument;
import onepoint.mystifly.TripDetailsDocument.TripDetails;
import onepoint.mystifly.TripDetailsResponseDocument;

import org.datacontract.schemas._2004._07.mystifly_onepoint.AirTripDetailsRQ;
import org.datacontract.schemas._2004._07.mystifly_onepoint.AirTripDetailsRS;
import org.datacontract.schemas._2004._07.mystifly_onepoint.SessionCreateRS;
import org.datacontract.schemas._2004._07.mystifly_onepoint.Target;

/**
 * @author Santhosh
 */
public class AirTripDetailsClient {

	public AirTripDetailsRS getAirTripDetails(String pnr)
			throws RemoteException {
		SessionsHandler sessionsHandler = new SessionsHandler();
		SessionCreateRS sessionRS = sessionsHandler.login();
		OnePointStub onePointStub = sessionsHandler.getOnePointStub();

		TripDetailsDocument tripDetailsDoc = TripDetailsDocument.Factory
				.newInstance();
		TripDetails tripDetails = tripDetailsDoc.addNewTripDetails();
		AirTripDetailsRQ airRQ = tripDetails.addNewRq();

		airRQ.setSessionId(sessionRS.getSessionId());
		airRQ.setTarget(Target.TEST);
		airRQ.setUniqueID(pnr);
		airRQ.setSendOnlyTicketed(true);

		TripDetailsResponseDocument tripDetailsRSDoc = onePointStub
				.tripDetails(tripDetailsDoc);
		return tripDetailsRSDoc.getTripDetailsResponse().getTripDetailsResult();
	}
}
