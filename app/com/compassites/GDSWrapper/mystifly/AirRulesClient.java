package com.compassites.GDSWrapper.mystifly;

import java.rmi.RemoteException;

import onepoint.mystifly.FareRules11Document;
import onepoint.mystifly.FareRules11Document.FareRules11;
import onepoint.mystifly.FareRules11ResponseDocument;
import onepoint.mystifly.OnePointStub;

import org.datacontract.schemas._2004._07.mystifly_onepoint.SessionCreateRS;
import org.datacontract.schemas._2004._07.mystifly_onepoint_airrules1_1.AirRulesRQ;
import org.datacontract.schemas._2004._07.mystifly_onepoint_airrules1_1.AirRulesRS;

/**
 * @author Santhosh
 */
public class AirRulesClient {
	
	public AirRulesRS getAirRules(String fareSourceCode) {
		SessionsHandler sessionsHandler = new SessionsHandler();
		SessionCreateRS sessionRS = sessionsHandler.login();
		OnePointStub onePointStub = sessionsHandler.getOnePointStub();
		FareRules11Document fareRulesDoc = FareRules11Document.Factory
				.newInstance();
		FareRules11 fareRules = fareRulesDoc.addNewFareRules11();
		AirRulesRQ airRulesRQ = fareRules.addNewRq();
		airRulesRQ.setSessionId(sessionRS.getSessionId());
		airRulesRQ.setTarget(Mystifly.TARGET);
		airRulesRQ.setFareSourceCode(fareSourceCode);
		FareRules11ResponseDocument fareRulesRSDoc = null;
		try {
			fareRulesRSDoc = onePointStub.fareRules1_1(fareRulesDoc);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		AirRulesRS airRulesRS = fareRulesRSDoc.getFareRules11Response().getFareRules11Result();
		return airRulesRS;
	}

}
