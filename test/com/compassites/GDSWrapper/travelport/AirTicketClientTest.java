package com.compassites.GDSWrapper.travelport;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.travelport.schema.air_v26_0.AirTicketingRsp;

public class AirTicketClientTest {

	@Test
	public void test() {
		String pnr = "2HXT8A";
		AirTicketClient airTicketClient = new AirTicketClient();
		AirTicketingRsp response = airTicketClient.issueTicket(pnr);
		assertNotNull(response);
	}

}
