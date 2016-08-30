package com.compassites.GDSWrapper.mystifly;

import java.rmi.RemoteException;

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

/**
 * @author Santhosh
 */
public class SessionsHandler {

	private OnePointStub onePointStub = null;

	static Logger logger = LoggerFactory.getLogger("gds");


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
			logger.error("Error in Mystifly SessionsHandler",e);
		}
		return sessionRS;
	}

	public OnePointStub getOnePointStub() {
		return onePointStub;
	}

}
