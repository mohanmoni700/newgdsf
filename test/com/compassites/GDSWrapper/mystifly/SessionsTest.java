package com.compassites.GDSWrapper.mystifly;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

public class SessionsTest {

	@Test
	public void testLogin() {
		SessionsHandler sessionHandler = new SessionsHandler();
		sessionHandler.login();
		assertNotNull(sessionHandler.login().getSessionId());
	}

}
