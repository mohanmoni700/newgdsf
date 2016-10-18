package com.compassites.GDSWrapper.mystifly;

import java.rmi.RemoteException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import onepoint.mystifly.CreateSessionDocument;
import onepoint.mystifly.CreateSessionDocument.CreateSession;
import onepoint.mystifly.CreateSessionResponseDocument;
import onepoint.mystifly.OnePointStub;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Options;
import org.apache.axis2.transport.http.HTTPConstants;
import org.datacontract.schemas._2004._07.mystifly_onepoint.SessionCreateRQ;
import org.datacontract.schemas._2004._07.mystifly_onepoint.SessionCreateRS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.mvc.Http;


/**
 * @author Santhosh
 */
public class SessionsHandler {

	private OnePointStub onePointStub = null;

	public SessionsHandler() {
		try {
			onePointStub = new OnePointStub(Mystifly.ENDPOINT_ADDRESS);
			Options options = onePointStub._getServiceClient().getOptions();
			options.setProperty(HTTPConstants.HTTP_PROTOCOL_VERSION,
					HTTPConstants.HEADER_PROTOCOL_10);
			options.setTimeOutInMilliSeconds(Mystifly.TIMEOUT);
		} catch (AxisFault e) {
			// TODO: Log Error
			e.printStackTrace();
		}
	}

	public SessionCreateRS login() {
		SessionCreateRS sessionRS = null;
		try {
			CreateSessionDocument sessionDoc = CreateSessionDocument.Factory
					.newInstance();
			CreateSession createSession = sessionDoc.addNewCreateSession();
			SessionCreateRQ sessionRQ = createSession.addNewRq();
			sessionRQ.setTarget(Mystifly.TARGET);
			sessionRQ.setAccountNumber(Mystifly.ACCOUNT_NUMBER);
			sessionRQ.setUserName(Mystifly.USERNAME);
			sessionRQ.setPassword(Mystifly.PASSWORD);
			CreateSessionResponseDocument createSessionResponseDocument = onePointStub
					.createSession(sessionDoc);
			sessionRS = createSessionResponseDocument
					.getCreateSessionResponse().getCreateSessionResult();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return sessionRS;
	}

	public String mystiflySessionHandler(){
		Http.Session session = Http.Context.current().session();
		String mSession = session.get("mSessionId");
		Long mSessionValidity = new Date().getTime();
		String mSessionCreatedTime = session.get("mSessionValidity");
		long diffInMinutes =0L;
		if(mSessionCreatedTime != null) {
			Double d = Double.parseDouble(mSessionCreatedTime.trim());
			Long sessionCreationTime = d.longValue();
			long diff = mSessionValidity - sessionCreationTime;
			diffInMinutes = TimeUnit.MILLISECONDS.toMinutes(diff);
			System.out.println("Time Difference " + diffInMinutes);
		}
		if((mSession =="" || mSession == null) || (diffInMinutes > 18)) {
			SessionsHandler sessionsHandler = new SessionsHandler();
			SessionCreateRS sessionRS = sessionsHandler.login();
			//XMLFileUtility.createFile(sessionRS.xmlText(),"MystiflySessionRS.xml");
			session.put("mSessionId",sessionRS.getSessionId());
			session.put("mSessionValidity",mSessionValidity.toString());
			System.out.println("Creating new Session "+sessionRS.getSessionId());
			return sessionRS.getSessionId();
		} else {
			System.out.println("Return Existing Session "+session.get("mSessionId"));
			return session.get("mSessionId");
		}
	}

	public OnePointStub getOnePointStub() {
		return onePointStub;
	}

}
