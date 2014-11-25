package com.compassites.GDSWrapper.mystifly;

import java.rmi.RemoteException;

import onepoint.mystifly.OnePointStub;
import onepoint.mystifly.TripDetailsDocument;
import onepoint.mystifly.TripDetailsDocument.TripDetails;
import onepoint.mystifly.TripDetailsResponseDocument;
import onepoint.mystifly.TripDetailsResponseDocument.TripDetailsResponse;

import org.datacontract.schemas._2004._07.mystifly_onepoint.AirTripDetailsRQ;
import org.datacontract.schemas._2004._07.mystifly_onepoint.AirTripDetailsRS;
import org.datacontract.schemas._2004._07.mystifly_onepoint.SessionCreateRS;
import org.datacontract.schemas._2004._07.mystifly_onepoint.Target;

/**
 * @author Santhosh
 */
public class AirTripDetailsClient {
	
	public static final String UNIQUE_ID = "MF06598414";
	
	public AirTripDetailsRS getAirTripDetails() {
		SessionsHandler sessionsHandler = new SessionsHandler();
		SessionCreateRS sessionRS = sessionsHandler.login();
		OnePointStub onePointStub = sessionsHandler.getOnePointStub();
		
		TripDetailsDocument tripDetailsDoc = TripDetailsDocument.Factory.newInstance();
		TripDetails tripDetails = tripDetailsDoc.addNewTripDetails();
		AirTripDetailsRQ airRQ = tripDetails.addNewRq();
		
//		AirTripDetailsRQDocument airDetailsRQDoc = AirTripDetailsRQDocument.Factory.newInstance();
//		AirTripDetailsRQ airRQ = airDetailsRQDoc.addNewAirTripDetailsRQ();
		
		airRQ.setSessionId(sessionRS.getSessionId());
		airRQ.setTarget(Target.TEST);
		airRQ.setUniqueID(UNIQUE_ID);
		TripDetailsResponseDocument tripDetailsRSDoc = null;
		try {
			tripDetailsRSDoc = onePointStub.tripDetails(tripDetailsDoc);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		TripDetailsResponse tripDetailsRS = tripDetailsRSDoc.getTripDetailsResponse();
		return tripDetailsRS.getTripDetailsResult();
	}
}
