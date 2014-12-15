package com.compassites.GDSWrapper.travelport;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.travelport.schema.air_v26_0.AirTicketingRsp;

public class AirTicketClientTest {

	@Test
	public void test() {
		String pnr = "2FLN6Q";
		AirTicketClient.init();
		AirTicketingRsp response = AirTicketClient.issueTicket(pnr);
		assertNotNull(response);
	}

}
