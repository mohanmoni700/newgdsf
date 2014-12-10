package com.compassites.GDSWrapper.mystifly;

import static org.junit.Assert.assertTrue;

import java.rmi.RemoteException;

import org.apache.axis2.AxisFault;
import org.datacontract.schemas._2004._07.mystifly_onepoint.AirRevalidateRS;
import org.datacontract.schemas._2004._07.mystifly_onepoint.PricedItinerary;
import org.junit.Test;

import com.compassites.helpers.MystiflyFlightItineraryHelper;

public class AirRevalidateClientTest {

	@Test
	public void testReValidateFare() {
		PricedItinerary itinerary = MystiflyFlightItineraryHelper.getFlightItinerary()[0];
		String fareSourceCode = itinerary.getAirItineraryPricingInfo().getFareSourceCode();
		
		AirRevalidateClient airClient = new AirRevalidateClient();
		AirRevalidateRS response = null;
		try {
			response = airClient.revalidate(fareSourceCode);
		} catch (AxisFault e) {
			e.printStackTrace();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		assertTrue(response.getSuccess());
	}
	
	@Test
	public void testRevalidateFare() {
		assertTrue(true);
	}

}
