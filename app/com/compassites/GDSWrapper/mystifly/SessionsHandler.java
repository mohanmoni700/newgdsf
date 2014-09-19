package com.compassites.GDSWrapper.mystifly;

import java.rmi.RemoteException;

import onepoint.mystifly.CreateSessionDocument;
import onepoint.mystifly.CreateSessionDocument.CreateSession;
import onepoint.mystifly.CreateSessionResponseDocument;
import onepoint.mystifly.OnePointStub;

import org.apache.axis2.AxisFault;
import org.apache.axis2.transport.http.HTTPConstants;
import org.datacontract.schemas._2004._07.mystifly_onepoint.SessionCreateRQ;
import org.datacontract.schemas._2004._07.mystifly_onepoint.SessionCreateRS;

public class SessionsHandler {

	public static final String ENDPOINT_ADDRESS = "http://apidemo.myfarebox.com/V2/OnePoint.svc?singleWsdl";
	public static final String ACCOUNT_NUMBER = "MCN004030";
	public static final String USERNAME = "FlyHiXML";
	public static final String PASSWORD = "FH2014_xml";

	private SessionCreateRS sessionRS = null;
	private OnePointStub onePointStub = null;

	public void login() {
		try {
			onePointStub = new OnePointStub(ENDPOINT_ADDRESS);
			onePointStub
					._getServiceClient()
					.getOptions()
					.setProperty(HTTPConstants.HTTP_PROTOCOL_VERSION,
							HTTPConstants.HEADER_PROTOCOL_10);
			CreateSessionDocument createSessionDoc = CreateSessionDocument.Factory
					.newInstance();
			CreateSession createSession = createSessionDoc
					.addNewCreateSession();
			SessionCreateRQ sessionRQ = createSession.addNewRq();
			sessionRQ.setAccountNumber(ACCOUNT_NUMBER);
			sessionRQ.setUserName(USERNAME);
			sessionRQ.setPassword(PASSWORD);
			CreateSessionResponseDocument createSessionResponseDocument = onePointStub
					.createSession(createSessionDoc);

			sessionRS = createSessionResponseDocument
					.getCreateSessionResponse().getCreateSessionResult();

		} catch (AxisFault e) {
			e.printStackTrace();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	public OnePointStub getOnePointStub() {
		return onePointStub;
	}

	public void setOnePointStub(OnePointStub onePointStub) {
		this.onePointStub = onePointStub;
	}

	public SessionCreateRS getSessionRS() {
		return sessionRS;
	}

	public void setSessionRS(SessionCreateRS sessionRS) {
		this.sessionRS = sessionRS;
	}

}
