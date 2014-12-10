package com.compassites.helpers;

import java.rmi.RemoteException;

import org.datacontract.schemas._2004._07.mystifly_onepoint.AirBookRS;
import org.datacontract.schemas._2004._07.mystifly_onepoint.PricedItinerary;

import com.compassites.GDSWrapper.mystifly.BookFlightClient;
import com.compassites.model.traveller.TravellerMasterInfo;

public class MystiflyBookFlightHelper {
	
	public static AirBookRS bookFlight(PricedItinerary pricedItinerary) {
		String fareSourceCode = pricedItinerary.getAirItineraryPricingInfo().getFareSourceCode();
		TravellerMasterInfo travellerMaster = TravellerMasterInfoHelper.getTravellerMasterInfo();
		travellerMaster.getItinerary().setFareSourceCode(fareSourceCode);

		AirBookRS airBookRS = null;
		try {
			airBookRS = new BookFlightClient().bookFlight(travellerMaster);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		return airBookRS;
	}

}
