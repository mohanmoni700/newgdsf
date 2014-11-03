package com.compassites.GDSWrapper.mystifly;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import onepoint.mystifly.FareRules11Document;
import onepoint.mystifly.FareRules11Document.FareRules11;
import onepoint.mystifly.FareRules11ResponseDocument;
import onepoint.mystifly.FareRulesDocument;
import onepoint.mystifly.FareRulesDocument.FareRules;
import onepoint.mystifly.FareRulesResponseDocument;
import onepoint.mystifly.OnePointStub;

import org.datacontract.schemas._2004._07.mystifly_onepoint.AirRulesFareInfo;
import org.datacontract.schemas._2004._07.mystifly_onepoint.ArrayOfAirRulesFareInfo;
import org.datacontract.schemas._2004._07.mystifly_onepoint.SessionCreateRS;
import org.datacontract.schemas._2004._07.mystifly_onepoint_airrules1_1.AirRulesRQ;
import org.datacontract.schemas._2004._07.mystifly_onepoint_airrules1_1.AirRulesRS;
import org.datacontract.schemas._2004._07.mystifly_onepoint_airrules1_1.BaggageInfo;

/**
 * @author Santhosh
 */
public class AirRulesClient {
	
	public org.datacontract.schemas._2004._07.mystifly_onepoint.AirRulesRS getAirRules(String fareSourceCode) {
		SessionsHandler sessionsHandler = new SessionsHandler();
		SessionCreateRS sessionRS = sessionsHandler.login();
		OnePointStub onePointStub = sessionsHandler.getOnePointStub();
		
		FareRulesDocument fareRulesDoc = FareRulesDocument.Factory
				.newInstance();
		FareRules fareRules = fareRulesDoc.addNewFareRules();
		org.datacontract.schemas._2004._07.mystifly_onepoint.AirRulesRQ airRulesRQ = fareRules.addNewRq();
		airRulesRQ.setSessionId(sessionRS.getSessionId());
		airRulesRQ.setFareSourceCode(fareSourceCode);
		airRulesRQ.setTarget(Mystifly.TARGET);
		
		ArrayOfAirRulesFareInfo fareInfos = airRulesRQ.addNewFareInfos();
		AirRulesFareInfo airRulesInfo = fareInfos.addNewAirRulesFareInfo();
		Calendar calendar = Calendar.getInstance();
		
		airRulesInfo.setArrivalAirportLocationCode("DXB");
		airRulesInfo.setDepartureAirportLocationCode("BOM");
		calendar.set(2014, 11, 25, 11, 25);
		airRulesInfo.setDepartureDateTime(calendar);
		airRulesInfo.setFareBasisCode("A6E64C");
		airRulesInfo.setMarketingAirlineCode("6E");
		airRulesInfo.setOperatingAirlineCode("6E");
		
		
		airRulesRQ.setFareInfos(fareInfos);
		FareRulesResponseDocument fareRulesRSDoc = null;
		try {
			fareRulesRSDoc = onePointStub.fareRules(fareRulesDoc);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		String filename = "/home/santhosh/AirRulesRQ-" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(
		        Calendar.getInstance().getTime()) + ".xml";
		File logFile = new File(filename);
	    BufferedWriter writer;
		try {
			writer = new BufferedWriter(new FileWriter(logFile));
			writer.write(fareRulesDoc.xmlText());
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
		
		org.datacontract.schemas._2004._07.mystifly_onepoint.AirRulesRS airRulesRS = fareRulesRSDoc.getFareRulesResponse().getFareRulesResult();
		return airRulesRS;
	}
	
	public AirRulesRS getAirRules11(String fareSourceCode) {
		SessionsHandler sessionsHandler = new SessionsHandler();
		SessionCreateRS sessionRS = sessionsHandler.login();
		OnePointStub onePointStub = sessionsHandler.getOnePointStub();
		FareRules11Document fareRulesDoc = FareRules11Document.Factory
				.newInstance();
		FareRules11 fareRules = fareRulesDoc.addNewFareRules11();
		AirRulesRQ airRulesRQ = fareRules.addNewRq();
		airRulesRQ.setSessionId(sessionRS.getSessionId());
		airRulesRQ.setFareSourceCode(fareSourceCode);
		airRulesRQ.setTarget(Mystifly.TARGET);
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
