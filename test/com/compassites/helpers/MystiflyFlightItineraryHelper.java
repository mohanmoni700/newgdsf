package com.compassites.helpers;

import org.datacontract.schemas._2004._07.mystifly_onepoint.AirLowFareSearchRS;
import org.datacontract.schemas._2004._07.mystifly_onepoint.PricedItinerary;

import com.compassites.GDSWrapper.mystifly.LowFareRequestClient;
import com.compassites.model.SearchParameters;

public class MystiflyFlightItineraryHelper {
	
	public static PricedItinerary getFlightItinerary() {
		SearchParameters searchParams = SearchParamsHelper.getSearchParams();
		LowFareRequestClient lowFareRequestClient = new LowFareRequestClient();
		AirLowFareSearchRS searchRS = lowFareRequestClient.search(searchParams);
		return searchRS.getPricedItineraries().getPricedItineraryArray()[0];
	}

}
