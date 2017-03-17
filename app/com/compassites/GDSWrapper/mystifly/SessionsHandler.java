package com.compassites.GDSWrapper.mystifly;

import java.rmi.RemoteException;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.compassites.constants.CacheConstants;
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
		Long mSessionValidity = new Date().getTime();
		String mSessionCreatedTime = "0";
		String mSession = "";

		if(redisTemplate != null){
			mSessionCreatedTime = (String)redisTemplate.opsForValue().get(":mystiflySessionTime");
			mSession = (String)redisTemplate.opsForValue().get(":mystiflySession");
		} else {
			System.out.println("0");
		}
		long diffInMinutes =0L;
		//mSessionCreatedTime != null ||
		if(mSessionCreatedTime !="0") {
			Double d = Double.parseDouble(mSessionCreatedTime.trim());
			Long sessionCreationTime = d.longValue();
			long diff = mSessionValidity - sessionCreationTime;
			diffInMinutes = TimeUnit.MILLISECONDS.toMinutes(diff);
			System.out.println("Time Difference " + diffInMinutes);
		}
		if((mSession =="" || mSession == null) || (diffInMinutes > 18)) {
			SessionsHandler sessionsHandler = new SessionsHandler();
			SessionCreateRS sessionRS = sessionsHandler.login();
			redisTemplate.opsForValue().set(":mystiflySession", Json.stringify(Json.toJson(sessionRS.getSessionId())));
			redisTemplate.opsForValue().set(":mystiflySessionTime", Json.stringify(Json.toJson(mSessionValidity.toString())));
			System.out.println("Creating new Session "+sessionRS.getSessionId());
			return sessionRS.getSessionId();
		} else {
			System.out.println("Return Existing Session "+(String)redisTemplate.opsForValue().get(":mystiflySessionTime"));
			return (String)redisTemplate.opsForValue().get(":mystiflySessionTime");
		}
	}

	public OnePointStub getOnePointStub() {
		return onePointStub;
	}

}
