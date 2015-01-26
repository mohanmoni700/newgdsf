package com.compassites.GDSWrapper.travelport;

import static org.junit.Assert.*;

import org.junit.Test;

import com.travelport.schema.air_v26_0.FlightDetailsRsp;

public class FlightDetailsTest {

	@Test
	public void testGetFlightDetails() {
		FlightDetailsClient flightDetailsClient = new FlightDetailsClient();
		FlightDetailsRsp response = flightDetailsClient.getFlightDetails();
		assertNotNull(response);
	}

}
