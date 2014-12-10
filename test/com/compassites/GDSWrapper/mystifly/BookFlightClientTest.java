package com.compassites.GDSWrapper.mystifly;

import static org.junit.Assert.assertTrue;

import org.datacontract.schemas._2004._07.mystifly_onepoint.AirBookRS;
import org.datacontract.schemas._2004._07.mystifly_onepoint.PricedItinerary;
import org.junit.Test;

import com.compassites.helpers.MystiflyBookFlightHelper;
import com.compassites.helpers.MystiflyFlightItineraryHelper;

public class BookFlightClientTest {

	@Test
	public void testBookFlight() {
		PricedItinerary itinerary = MystiflyFlightItineraryHelper.getFlightItinerary()[0];
		AirBookRS bookRS = MystiflyBookFlightHelper.bookFlight(itinerary);
    	assertTrue(bookRS.getSuccess());
	}
	
	@Test
	public void testMultiCityBookFlight() {
		PricedItinerary itinerary = MystiflyFlightItineraryHelper.getMultiCityFlightItinerary()[0];
		AirBookRS bookRS = MystiflyBookFlightHelper.bookFlight(itinerary);
    	assertTrue(bookRS.getSuccess());
	}

}
