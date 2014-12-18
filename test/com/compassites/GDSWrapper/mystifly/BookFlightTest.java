package com.compassites.GDSWrapper.mystifly;

import static org.junit.Assert.assertTrue;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import org.apache.axis2.AxisFault;
import org.datacontract.schemas._2004._07.mystifly_onepoint.AirBookRS;
import org.datacontract.schemas._2004._07.mystifly_onepoint.AirRevalidateRS;
import org.datacontract.schemas._2004._07.mystifly_onepoint.FareType;
import org.datacontract.schemas._2004._07.mystifly_onepoint.PricedItinerary;
import org.junit.Test;

import com.compassites.helpers.MystiflyBookFlightHelper;
import com.compassites.helpers.MystiflyFlightItineraryHelper;

public class BookFlightTest {

//	@Test
//	public void testBookLccFlight() {
//		PricedItinerary[] itineraries = MystiflyFlightItineraryHelper.getFlightItinerary();
//		PricedItinerary itinerary = null;
//		for(PricedItinerary pi : itineraries) {
//			if(pi.getAirItineraryPricingInfo().getFareType() == FareType.WEB_FARE) {
//				itinerary = pi;
//				break;
//			}
//		}
//		AirBookRS bookRS = MystiflyBookFlightHelper.bookFlight(itinerary);
//    	assertTrue(bookRS.getSuccess());
//	}
	
	@Test
	public void testBookNonLccFlight() {
		PricedItinerary[] itineraries = MystiflyFlightItineraryHelper.getFlightItinerary();
		AirRevalidateRS revalidateRS = null;
		AirRevalidateClient revalidateClient = new AirRevalidateClient();
		try {
			for (PricedItinerary pi : getLccItineraries(itineraries)) {
				revalidateRS = revalidateClient.revalidate(pi.getAirItineraryPricingInfo().getFareSourceCode());
				if(revalidateRS.getIsValid()) {// && revalidateRS.getPricedItineraries().getPricedItineraryArray().length > 0)
					break;
				}
			}
		} catch (AxisFault e) {
			e.printStackTrace();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	
		System.out.println(revalidateRS.getPricedItineraries().getPricedItineraryArray().length);
		for(PricedItinerary pi : revalidateRS.getPricedItineraries().getPricedItineraryArray()) {
			AirBookRS bookRS = MystiflyBookFlightHelper.bookFlight(pi);
			System.out.println(pi.getAirItineraryPricingInfo().getFareSourceCode());
			System.out.println(bookRS.getSuccess());
		}
	}
	
	public List<PricedItinerary> getLccItineraries(PricedItinerary[] itineraries) {
		List<PricedItinerary> lccItineraries = new ArrayList<PricedItinerary>();
		for (PricedItinerary pi : itineraries) {
			if (pi.getAirItineraryPricingInfo().getFareType() != FareType.WEB_FARE)
				lccItineraries.add(pi);
		}
		return lccItineraries;
	}
	
//	@Test
//	public void testMultiCityBookLccFlight() {
//		PricedItinerary[] itineraries = MystiflyFlightItineraryHelper.getMultiCityFlightItinerary();
//		PricedItinerary itinerary = null;
//		for(PricedItinerary pi : itineraries) {
//			if(pi.getAirItineraryPricingInfo().getFareType() == FareType.WEB_FARE) {
//				itinerary = pi;
//				break;
//			}
//		}
//		AirBookRS bookRS = MystiflyBookFlightHelper.bookFlight(itinerary);
//    	assertTrue(bookRS.getSuccess());
//	}
	

}
