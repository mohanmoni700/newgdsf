package com.compassites.GDSWrapper.mystifly;

import java.rmi.RemoteException;

import onepoint.mystifly.AirRevalidateDocument;
import onepoint.mystifly.AirRevalidateResponseDocument;
import onepoint.mystifly.AirRevalidateResponseDocument.AirRevalidateResponse;
import onepoint.mystifly.OnePointStub;

import org.apache.axis2.AxisFault;
import org.datacontract.schemas._2004._07.mystifly_onepoint.AirRevalidateRQ;
import org.datacontract.schemas._2004._07.mystifly_onepoint.AirRevalidateRS;
import org.datacontract.schemas._2004._07.mystifly_onepoint.SessionCreateRS;

/**
 * @author Santhosh
 */
public class AirRevalidateClient {

	/**
	 * @param String
	 *            A unique code for the itinerary selected
	 * @return AirRevalidateResponse
	 * @throws AxisFault
	 * @throws RemoteException
	 */
	public AirRevalidateRS revalidate(String fareSourceCode) throws AxisFault,
			RemoteException {
		SessionsHandler sessionsHandler = new SessionsHandler();
		SessionCreateRS sessionRS = sessionsHandler.login();
		OnePointStub onePointStub = sessionsHandler.getOnePointStub();
		AirRevalidateDocument airRevalidateDocument = AirRevalidateDocument.Factory
				.newInstance();
		AirRevalidateRQ airRevalidateRQ = airRevalidateDocument
				.addNewAirRevalidate().addNewRq();
		airRevalidateRQ.setTarget(Mystifly.TARGET);
		airRevalidateRQ.setFareSourceCode(fareSourceCode);
		airRevalidateRQ.setSessionId(sessionRS.getSessionId());
		AirRevalidateResponseDocument airRevalidateRSDoc = onePointStub
				.airRevalidate(airRevalidateDocument);
		AirRevalidateResponse airRevalidateResponse = airRevalidateRSDoc
				.getAirRevalidateResponse();
		return airRevalidateResponse.getAirRevalidateResult();
	}

}
