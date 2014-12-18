package com.compassites.GDSWrapper.mystifly;

import static org.junit.Assert.assertTrue;

import org.datacontract.schemas._2004._07.mystifly_onepoint.PricedItinerary;
import org.datacontract.schemas._2004._07.mystifly_onepoint_airrules1_1.AirRulesRS;
import org.junit.Test;

import com.compassites.helpers.MystiflyFlightItineraryHelper;

public class AirRulesTest {
	
	@Test
	public void testGetAirRules() {
		PricedItinerary itinerary = MystiflyFlightItineraryHelper.getFlightItinerary()[0];
		String fareSourceCode = itinerary.getAirItineraryPricingInfo().getFareSourceCode();
		
		AirRulesClient airRulesClient = new AirRulesClient();
		AirRulesRS airRulesRS = airRulesClient.getAirRules(fareSourceCode);
		assertTrue(airRulesRS.getSuccess());
	}

}
