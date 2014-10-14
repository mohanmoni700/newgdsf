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

/**
 * @author Santhosh
 */
public class SessionsHandler {

	public static final String ENDPOINT_ADDRESS = "http://apidemo.myfarebox.com/V2/OnePoint.svc?singleWsdl";
	public static final String ACCOUNT_NUMBER = "MCN004030";
	public static final String USERNAME = "FlyHiXML";
	public static final String PASSWORD = "FH2014_xml";

	private OnePointStub onePointStub = null;

	public SessionsHandler() {
		try {
			onePointStub = new OnePointStub(ENDPOINT_ADDRESS);
			Options options = onePointStub._getServiceClient().getOptions();
			options.setProperty(HTTPConstants.HTTP_PROTOCOL_VERSION,
					HTTPConstants.HEADER_PROTOCOL_10);
			options.setTimeOutInMilliSeconds(180000);

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
			sessionRQ.setAccountNumber(ACCOUNT_NUMBER);
			sessionRQ.setUserName(USERNAME);
			sessionRQ.setPassword(PASSWORD);
			CreateSessionResponseDocument createSessionResponseDocument = onePointStub
					.createSession(sessionDoc);
			sessionRS = createSessionResponseDocument
					.getCreateSessionResponse().getCreateSessionResult();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return sessionRS;
	}

	public OnePointStub getOnePointStub() {
		return onePointStub;
	}

}
