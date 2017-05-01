package com.compassites.GDSWrapper.mystifly;

import java.rmi.RemoteException;
import java.util.Date;

import onepoint.mystifly.OnePointStub;
import onepoint.mystifly.TripDetailsDocument;
import onepoint.mystifly.TripDetailsDocument.TripDetails;
import onepoint.mystifly.TripDetailsResponseDocument;

import org.datacontract.schemas._2004._07.mystifly_onepoint.AirTripDetailsRQ;
import org.datacontract.schemas._2004._07.mystifly_onepoint.AirTripDetailsRS;
import org.datacontract.schemas._2004._07.mystifly_onepoint.SessionCreateRS;
import org.datacontract.schemas._2004._07.mystifly_onepoint.Target;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.XMLFileUtility;

/**
 * @author Santhosh
 */
public class AirTripDetailsClient {

	static Logger mystiflyLogger = LoggerFactory.getLogger("mystifly");

	public AirTripDetailsRS getAirTripDetails(String pnr)
			throws RemoteException {
		SessionsHandler sessionsHandler = new SessionsHandler();
		//SessionCreateRS sessionRS = sessionsHandler.login();
		String sessoinId = sessionsHandler.mystiflySessionHandler();
		OnePointStub onePointStub = sessionsHandler.getOnePointStub();

		TripDetailsDocument tripDetailsDoc = TripDetailsDocument.Factory
				.newInstance();
		TripDetails tripDetails = tripDetailsDoc.addNewTripDetails();
		AirTripDetailsRQ airRQ = tripDetails.addNewRq();

		airRQ.setSessionId(sessoinId);
		airRQ.setTarget(Mystifly.TARGET);
		airRQ.setUniqueID(pnr);
//		airRQ.setSendOnlyTicketed(true);
		//XMLFileUtility.createFile(tripDetailsDoc.xmlText(), "TripDetailsRQ.xml");
		mystiflyLogger.debug("TripDetailsRQ "+ new Date() +" ----->>" + tripDetailsDoc.xmlText());
		TripDetailsResponseDocument tripDetailsRSDoc = onePointStub
				.tripDetails(tripDetailsDoc);
		mystiflyLogger.debug("TripDetailsRS "+ new Date() +" ----->>" + tripDetailsRSDoc.xmlText());
	//	XMLFileUtility.createFile(tripDetailsRSDoc.xmlText(), "TripDetailsRS.xml");
		return tripDetailsRSDoc.getTripDetailsResponse().getTripDetailsResult();
	}
}
