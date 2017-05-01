package com.compassites.GDSWrapper.mystifly;

import java.rmi.RemoteException;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.compassites.constants.CacheConstants;
import models.MystiflySessionWrapper;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import play.libs.Json;
import play.mvc.Http;
import utils.XMLFileUtility;


/**
 * @author Santhosh
 */
@Service
public class SessionsHandler {

	private OnePointStub onePointStub = null;

	/*@Autowired
	private SessionsHandler sessionsHandler;*/

	@Autowired
	private RedisTemplate redisTemplate;

	static Logger mystiflyLogger = LoggerFactory.getLogger("mystifly");

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
			//XMLFileUtility.createFile(sessionDoc.xmlText(), "MystiflySessionRQ.xml");
			mystiflyLogger.debug("MystiflySessionRQ " + new Date() +" ---->" + sessionDoc);
			CreateSessionResponseDocument createSessionResponseDocument = onePointStub
					.createSession(sessionDoc);
			//XMLFileUtility.createFile(createSessionResponseDocument.xmlText(), "MystiflySessionRS.xml");
			mystiflyLogger.debug("MystiflySessionRS " + new Date() +" ---->" + createSessionResponseDocument);
			sessionRS = createSessionResponseDocument
					.getCreateSessionResponse().getCreateSessionResult();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return sessionRS;
	}

	public String mystiflySessionHandler(){
		MystiflySessionWrapper mystiflySessionWrappers = MystiflySessionWrapper.findByActiveSession();
		String mSession = "";
		String mSessionCreatedTime = "";
		Long mSessionValidity = new Date().getTime();
		if(mystiflySessionWrappers != null) {
			mSession = mystiflySessionWrappers.getSessionId();
			mSessionCreatedTime = mystiflySessionWrappers.getSessionCreatedTime();
		}
		long diffInMinutes =0L;
		if(mSessionCreatedTime != "") {
			Double d = Double.parseDouble(mSessionCreatedTime.trim());
			Long sessionCreationTime = d.longValue();
			long diff = mSessionValidity - sessionCreationTime;
			diffInMinutes = TimeUnit.MILLISECONDS.toMinutes(diff);
			System.out.println("Time Difference " + diffInMinutes);
		}
		if((mSession =="" || mSession == null) || (diffInMinutes > 18)) {
			List<MystiflySessionWrapper> mystiflySessionWrapperList = MystiflySessionWrapper.findByAllActiveSession();
			for(MystiflySessionWrapper mystiflySessionWrapper : mystiflySessionWrapperList){
				mystiflySessionWrapper.delete();
			}
			SessionsHandler sessionsHandler = new SessionsHandler();
			SessionCreateRS sessionRS = sessionsHandler.login();
			MystiflySessionWrapper mystiflySessionWrapper = new MystiflySessionWrapper();
			//if(sessionRS.isNilErrors()){
				mystiflySessionWrapper.setSessionId(sessionRS.getSessionId());
				mystiflySessionWrapper.setActiveContext(true);
				mystiflySessionWrapper.setSessionCreatedTime(mSessionValidity.toString());
				mystiflySessionWrapper.save();
			//}
			//session.put("mSessionId",sessionRS.getSessionId());
			//session.put("mSessionValidity",mSessionValidity.toString());
			System.out.println("Creating new Session "+sessionRS.getSessionId());
			return sessionRS.getSessionId();
		} else {
			System.out.println("Return Existing Session "+mystiflySessionWrappers.getSessionId());
			return mystiflySessionWrappers.getSessionId();
		}
	}

	public OnePointStub getOnePointStub() {
		return onePointStub;
	}

}
