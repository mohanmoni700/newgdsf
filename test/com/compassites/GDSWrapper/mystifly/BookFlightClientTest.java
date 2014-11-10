package com.compassites.GDSWrapper.mystifly;

import static org.junit.Assert.assertTrue;

import org.datacontract.schemas._2004._07.mystifly_onepoint.AirBookRS;
import org.datacontract.schemas._2004._07.mystifly_onepoint.PricedItinerary;
import org.junit.Test;

import com.compassites.helpers.MystiflyFlightItineraryHelper;
import com.compassites.helpers.TravellerMasterInfoHelper;
import com.compassites.model.traveller.TravellerMasterInfo;

public class BookFlightClientTest {

	@Test
	public void testBookFlight() {
		AirBookRS bookRS = bookFlight(MystiflyFlightItineraryHelper.getFlightItinerary());
    	assertTrue(bookRS.getSuccess());
	}
	
//	@Test
//	public void testMultiCityBookFlight() {
//		AirBookRS bookRS = bookFlight(MystiflyFlightItineraryHelper.getMultiCityFlightItinerary());
//    	assertTrue(bookRS.getSuccess());
//	}
	
	private AirBookRS bookFlight(PricedItinerary pricedItinerary) {
		String fareSourceCode = pricedItinerary.getAirItineraryPricingInfo().getFareSourceCode();
		TravellerMasterInfo travellerMaster = TravellerMasterInfoHelper.getTravellerMasterInfo();
		travellerMaster.getItinerary().setFareSourceCode(fareSourceCode);
		
		BookFlightClient bookFlightClient = new BookFlightClient();
		return bookFlightClient.bookFlight(travellerMaster);
	}

}
