package com.compassites.GDSWrapper.amadeus;

import com.amadeus.xml.ws._2009._01.wbs_session_2_0.Session;

import javax.xml.ws.Holder;

public class SessionHandler {

	private Holder<Session> mSession;
	
	public SessionHandler() {
		mSession = new Holder<Session>();
		resetSession();
	}

    public SessionHandler(Holder<Session> mSession) {
        this.mSession = mSession;
    }

    public Holder<Session> getSession() {
		return mSession;
	}
	
	// create an empty header where all elements are set to empty one by
	// one. necessary to make JaxWS send a valid empty header.
	public void resetSession() {
		mSession.value = new Session();
		mSession.value.setSecurityToken("");
		mSession.value.setSequenceNumber("");
		mSession.value.setSessionId("");
	}
	
	protected void incrementSequenceNumber() {
		Integer sequenceNumber = Integer.parseInt(mSession.value
				.getSequenceNumber());
		sequenceNumber++;
		mSession.value.setSequenceNumber(sequenceNumber.toString());
	}

	public String getSessionId() {
		return mSession.value.getSessionId();
	}
	
}
