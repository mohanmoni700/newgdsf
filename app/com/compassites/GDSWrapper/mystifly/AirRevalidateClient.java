package com.compassites.GDSWrapper.mystifly;

import onepoint.mystifly.AirRevalidateDocument;
import onepoint.mystifly.AirRevalidateResponseDocument;
import onepoint.mystifly.AirRevalidateResponseDocument.AirRevalidateResponse;
import onepoint.mystifly.OnePointStub;
import org.apache.axis2.AxisFault;
import org.datacontract.schemas._2004._07.mystifly_onepoint.AirRevalidateRQ;
import org.datacontract.schemas._2004._07.mystifly_onepoint.AirRevalidateRS;
import org.datacontract.schemas._2004._07.mystifly_onepoint.SessionCreateRS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.RemoteException;
import java.util.Date;

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

    static Logger mystiflyLogger = LoggerFactory.getLogger("mystifly");

	public AirRevalidateRS revalidate(String fareSourceCode) throws AxisFault,
			RemoteException {
		SessionsHandler sessionsHandler = new SessionsHandler();
		SessionCreateRS sessionRS = sessionsHandler.login();
		OnePointStub onePointStub = sessionsHandler.getOnePointStub();

		AirRevalidateDocument airRevalidateDoc = AirRevalidateDocument.Factory
				.newInstance();
		AirRevalidateRQ airRevalidateRQ = airRevalidateDoc
				.addNewAirRevalidate().addNewRq();
		airRevalidateRQ.setTarget(Mystifly.TARGET);
		airRevalidateRQ.setFareSourceCode(fareSourceCode);
		airRevalidateRQ.setSessionId(sessionRS.getSessionId());

//		XMLFileUtility.createFile(airRevalidateRQ.xmlText(), "AirRevalidateRQ.xml");
        mystiflyLogger.debug("AirRevalidateRQ "+ new Date() +" ----->>" + airRevalidateRQ.xmlText());
		AirRevalidateResponseDocument airRevalidateRSDoc = onePointStub
				.airRevalidate(airRevalidateDoc);
		AirRevalidateResponse airRevalidateResponse = airRevalidateRSDoc
				.getAirRevalidateResponse();

//		XMLFileUtility.createFile(airRevalidateResponse.getAirRevalidateResult().xmlText(), "AirRevalidateRS.xml");
        mystiflyLogger.debug("AirRevalidateRS "+ new Date() +" ----->>" + airRevalidateResponse.getAirRevalidateResult().xmlText());
		return airRevalidateResponse.getAirRevalidateResult();
	}

}
