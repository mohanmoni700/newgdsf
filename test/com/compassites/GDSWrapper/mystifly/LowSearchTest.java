package com.compassites.GDSWrapper.mystifly;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.datacontract.schemas._2004._07.mystifly_onepoint.AirLowFareSearchRS;
import org.junit.Test;

import com.compassites.helpers.SearchParamsHelper;
import com.compassites.model.SearchParameters;

public class LowSearchTest {

	@Test
	public void testLowFareSearch() {
		SearchParameters searchParams = SearchParamsHelper.getSearchParams();
		
		AirLowFareSearchClient lowFareRequestClient = new AirLowFareSearchClient();
		AirLowFareSearchRS searchRS = lowFareRequestClient.search(searchParams);
		assertNotNull(searchRS);
		assertTrue(searchRS.getSuccess());
	}
	
	@Test
	public void testMultiCitySearch() {
		SearchParameters searchParams = SearchParamsHelper.getMultiCitySearchParams();
		
		AirLowFareSearchClient lowFareRequestClient = new AirLowFareSearchClient();
		AirLowFareSearchRS searchRS = lowFareRequestClient.search(searchParams);
		assertNotNull(searchRS);
		assertTrue(searchRS.getSuccess());
	}

}
