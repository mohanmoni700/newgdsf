package com.compassites.GDSWrapper.mystifly;

import static org.junit.Assert.assertTrue;

import org.datacontract.schemas._2004._07.mystifly_onepoint.AirRevalidateRS;
import org.datacontract.schemas._2004._07.mystifly_onepoint.PricedItinerary;
import org.junit.Test;

import com.compassites.helpers.MystiflyFlightItineraryHelper;

public class AirRevalidateClientTest {

	@Test
	public void testRevvalidateFare() {
		PricedItinerary itinerary = MystiflyFlightItineraryHelper.getFlightItinerary();
		String fareSourceCode = itinerary.getAirItineraryPricingInfo().getFareSourceCode();
		
		AirRevalidateClient airClient = new AirRevalidateClient();
		AirRevalidateRS response = airClient.revalidate(fareSourceCode);
		assertTrue(response.getSuccess());
	}

}
