package services;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.compassites.model.AirSolution;
import com.compassites.model.FlightItinerary;
import com.compassites.model.PricingInformation;
import com.compassites.model.SearchResponse;

public class FlightSearchWrapperTest {

	@Test
	public void testWithAllValues() {
		FlightSearchWrapper searchWrapper = new FlightSearchWrapper();
		HashMap<Integer, FlightItinerary> allItineraries = new HashMap<>();
		SearchResponse searchRS1 = getSearchRS1();
		searchWrapper.mergeResults(allItineraries, searchRS1);
		SearchResponse searchRS2 = getSearchRS2();
		searchWrapper.mergeResults(allItineraries, searchRS2);

		Assert.assertEquals(Long.valueOf(500), allItineraries.get(1).getPricingInformation().getTotalPriceValue());
		Assert.assertEquals(Long.valueOf(800), allItineraries.get(1).getSeamanPricingInformation().getTotalPriceValue());
		Assert.assertEquals(Long.valueOf(800), allItineraries.get(2).getPricingInformation().getTotalPriceValue());
		Assert.assertEquals(Long.valueOf(2000), allItineraries.get(2).getSeamanPricingInformation().getTotalPriceValue());
	}
	
	@Test
	public void testWithNullSeamenValuesValues() {
		FlightSearchWrapper searchWrapper = new FlightSearchWrapper();
		HashMap<Integer, FlightItinerary> allItineraries = new HashMap<>();
		SearchResponse searchRS1 = getSearchRSWithNullNonSeamenHashMap();
		searchWrapper.mergeResults(allItineraries, searchRS1);
		SearchResponse searchRS2 = getSearchRSWithNullSeamenHashMap();

		searchWrapper.mergeResults(allItineraries, searchRS2);
		Assert.assertEquals(Long.valueOf(600), allItineraries.get(1).getPricingInformation().getTotalPriceValue());
		Assert.assertEquals(Long.valueOf(1000), allItineraries.get(1).getSeamanPricingInformation().getTotalPriceValue());
		Assert.assertEquals(Long.valueOf(800), allItineraries.get(2).getPricingInformation().getTotalPriceValue());
		Assert.assertEquals(Long.valueOf(2000), allItineraries.get(2).getSeamanPricingInformation().getTotalPriceValue());
	}
	
	@Test
	public void testWithNullNonSeamenValuesValues() {
		FlightSearchWrapper searchWrapper = new FlightSearchWrapper();
		HashMap<Integer, FlightItinerary> allItineraries = new HashMap<>();
		SearchResponse searchRS1 = getSearchRSWithNullSeamenHashMap();
		searchWrapper.mergeResults(allItineraries, searchRS1);
		SearchResponse searchRS2 = getSearchRSWithNullNonSeamenHashMap();

		searchWrapper.mergeResults(allItineraries, searchRS2);
		Assert.assertEquals(Long.valueOf(600), allItineraries.get(1).getPricingInformation().getTotalPriceValue());
		Assert.assertEquals(Long.valueOf(1000), allItineraries.get(1).getSeamanPricingInformation().getTotalPriceValue());
		Assert.assertEquals(Long.valueOf(800), allItineraries.get(2).getPricingInformation().getTotalPriceValue());
		Assert.assertEquals(Long.valueOf(2000), allItineraries.get(2).getSeamanPricingInformation().getTotalPriceValue());
	}
	
	private SearchResponse getSearchRS1() {
		SearchResponse searchRS = new SearchResponse();
		AirSolution airSolution = new AirSolution();
		HashMap<Integer, FlightItinerary> seamenHashMap = new HashMap<>();
		HashMap<Integer, FlightItinerary> nonSeamenHashMap = new HashMap<>();
		
		List<FlightItinerary> seamenItineraries = getFlightItineraries(new int[] {1000, 2000});
		seamenHashMap.put(1, seamenItineraries.get(0));
		seamenHashMap.put(2, seamenItineraries.get(1));
		
		List<FlightItinerary> nonSeamenItineraries = getFlightItineraries(new int[] {500, 1000});
		nonSeamenHashMap.put(1, nonSeamenItineraries.get(0));
		nonSeamenHashMap.put(2, nonSeamenItineraries.get(1));
		
		airSolution.setSeamenHashMap(seamenHashMap);
		airSolution.setNonSeamenHashMap(nonSeamenHashMap);
		searchRS.setAirSolution(airSolution);
		return searchRS;
	}
	
	private SearchResponse getSearchRS2() {
		SearchResponse searchRS = new SearchResponse();
		AirSolution airSolution = new AirSolution();
		HashMap<Integer, FlightItinerary> seamenHashMap = new HashMap<>();
		HashMap<Integer, FlightItinerary> nonSeamenHashMap = new HashMap<>();
		
		List<FlightItinerary> seamenItineraries = getFlightItineraries(new int[] {3000, 800});
		seamenHashMap.put(2, seamenItineraries.get(0));
		seamenHashMap.put(1, seamenItineraries.get(1));
		
		List<FlightItinerary> NonSeamenItineraries = getFlightItineraries(new int[] {800, 600});
		nonSeamenHashMap.put(2, NonSeamenItineraries.get(0));
		nonSeamenHashMap.put(1, NonSeamenItineraries.get(1));
		
		airSolution.setSeamenHashMap(seamenHashMap);
		airSolution.setNonSeamenHashMap(nonSeamenHashMap);
		searchRS.setAirSolution(airSolution);
		return searchRS;
	}
	
	private SearchResponse getSearchRSWithNullNonSeamenHashMap() {
		SearchResponse searchRS = new SearchResponse();
		AirSolution airSolution = new AirSolution();
		HashMap<Integer, FlightItinerary> seamenHashMap = new HashMap<>();
		HashMap<Integer, FlightItinerary> nonSeamenHashMap = new HashMap<>();
		
		List<FlightItinerary> seamenItineraries = getFlightItineraries(new int[] {1000, 2000});
		seamenHashMap.put(1, seamenItineraries.get(0));
		seamenHashMap.put(2, seamenItineraries.get(1));
		
		airSolution.setSeamenHashMap(seamenHashMap);
		airSolution.setNonSeamenHashMap(nonSeamenHashMap);
		searchRS.setAirSolution(airSolution);
		return searchRS;
	}
	
	private SearchResponse getSearchRSWithNullSeamenHashMap() {
		SearchResponse searchRS = new SearchResponse();
		AirSolution airSolution = new AirSolution();
		HashMap<Integer, FlightItinerary> seamenHashMap = new HashMap<>();
		HashMap<Integer, FlightItinerary> nonSeamenHashMap = new HashMap<>();
	
		List<FlightItinerary> NonSeamenItineraries = getFlightItineraries(new int[] {800, 600});
		nonSeamenHashMap.put(2, NonSeamenItineraries.get(0));
		nonSeamenHashMap.put(1, NonSeamenItineraries.get(1));
		
		airSolution.setSeamenHashMap(seamenHashMap);
		airSolution.setNonSeamenHashMap(nonSeamenHashMap);
		searchRS.setAirSolution(airSolution);
		return searchRS;
	}
	
	private List<FlightItinerary> getFlightItineraries(int[] prices) {
		List<FlightItinerary> itineraries = new ArrayList<>();
		for(Integer price : prices) {
			FlightItinerary itinerary = new FlightItinerary();
			PricingInformation pricingInformation = new PricingInformation();
			pricingInformation.setTotalPriceValue(new BigDecimal(price));
			itinerary.setPricingInformation(pricingInformation);
			itineraries.add(itinerary);
		}
		return itineraries;
	}

}
