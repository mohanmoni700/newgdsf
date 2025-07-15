package com.compassites.GDSWrapper.amadeus;

import com.amadeus.xml._2010._06.session_v3.Session;

import javax.xml.ws.Holder;

public class SearchSessionHandler {

	private Holder<Session> mSession;

	public SearchSessionHandler() {
		mSession = new Holder<Session>();
		resetSession();
	}

    public SearchSessionHandler(Holder<Session> mSession) {
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
