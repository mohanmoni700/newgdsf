package com.compassites.GDSWrapper.mystifly;

import java.rmi.RemoteException;
import java.util.Date;

import onepoint.mystifly.FareRules11Document;
import onepoint.mystifly.FareRules11Document.FareRules11;
import onepoint.mystifly.FareRules11ResponseDocument;
import onepoint.mystifly.OnePointStub;

import org.datacontract.schemas._2004._07.mystifly_onepoint.SessionCreateRS;
import org.datacontract.schemas._2004._07.mystifly_onepoint_airrules1_1.AirRulesRQ;
import org.datacontract.schemas._2004._07.mystifly_onepoint_airrules1_1.AirRulesRS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.XMLFileUtility;

/**
 * @author Santhosh
 */
public class AirRulesClient {

	static Logger mystiflyLogger = LoggerFactory.getLogger("mystifly");

	public AirRulesRS getAirRules(String fareSourceCode) {
		SessionsHandler sessionsHandler = new SessionsHandler();
		//SessionCreateRS sessionRS = sessionsHandler.login();
		String sessoinId = sessionsHandler.mystiflySessionHandler();
		OnePointStub onePointStub = sessionsHandler.getOnePointStub();
		FareRules11Document fareRulesDoc = FareRules11Document.Factory.newInstance();
		FareRules11 fareRules = fareRulesDoc.addNewFareRules11();
		AirRulesRQ airRulesRQ = fareRules.addNewRq();
		airRulesRQ.setSessionId(sessoinId);
		airRulesRQ.setTarget(Mystifly.TARGET);
		airRulesRQ.setFareSourceCode(fareSourceCode);
		FareRules11ResponseDocument fareRulesRSDoc = null;
		try {
			XMLFileUtility.createFile(airRulesRQ.xmlText(), "airRulesRQ.xml");
			mystiflyLogger.debug("AirRulesClientReq " + new Date() +" ---->" + airRulesRQ);
			fareRulesRSDoc = onePointStub.fareRules1_1(fareRulesDoc);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			mystiflyLogger.error("============= Error in getAirRules Mystifly ", e);
			e.printStackTrace();
		}
		AirRulesRS airRulesRS = fareRulesRSDoc.getFareRules11Response().getFareRules11Result();
		mystiflyLogger.debug("AirRulesClientRs " + new Date() +" ---->" + airRulesRS);
		XMLFileUtility.createFile(fareRulesRSDoc.xmlText(), "airRulesRS.xml");

		return airRulesRS;
	}

}
