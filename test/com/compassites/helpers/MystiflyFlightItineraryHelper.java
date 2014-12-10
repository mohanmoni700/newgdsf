package com.compassites.helpers;

import org.datacontract.schemas._2004._07.mystifly_onepoint.AirLowFareSearchRS;
import org.datacontract.schemas._2004._07.mystifly_onepoint.PricedItinerary;

import com.compassites.GDSWrapper.mystifly.LowFareRequestClient;
import com.compassites.model.SearchParameters;

public class MystiflyFlightItineraryHelper {

	public static PricedItinerary[] getFlightItinerary() {
		return lowFareSearch(SearchParamsHelper.getSearchParams());
	}

	public static PricedItinerary[] getMultiCityFlightItinerary() {
		return lowFareSearch(SearchParamsHelper.getMultiCitySearchParams());
	}

	private static PricedItinerary[] lowFareSearch(SearchParameters searchParams) {
		LowFareRequestClient lowFareRequestClient = new LowFareRequestClient();
		AirLowFareSearchRS searchRS = lowFareRequestClient.search(searchParams);
		return searchRS.getPricedItineraries().getPricedItineraryArray();
	}

}
