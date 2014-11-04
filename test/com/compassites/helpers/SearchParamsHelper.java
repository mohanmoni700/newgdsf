package com.compassites.helpers;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import com.compassites.model.BookingType;
import com.compassites.model.CabinClass;
import com.compassites.model.DateType;
import com.compassites.model.JourneyType;
import com.compassites.model.SearchJourney;
import com.compassites.model.SearchParameters;

public class SearchParamsHelper {
	
	public static SearchParameters getSearchParams() {
		SearchParameters searchParams = new SearchParameters();
		
		searchParams.setAdultCount(1);
		searchParams.setBookingType(BookingType.NON_MARINE);
		searchParams.setCabinClass(CabinClass.ECONOMY);
		searchParams.setChildCount(0);
		searchParams.setDateType(DateType.DEPARTURE);
		searchParams.setDirectFlights(true);
		searchParams.setInfantCount(0);
		searchParams.setJourneyList(getSearchJourneyList());
		searchParams.setJourneyType(JourneyType.ONE_WAY);
		searchParams.setNationality("INDIA");
		searchParams.setNoOfStops(0);
		return searchParams;
	}
	
	public static List<SearchJourney> getSearchJourneyList() {
		List<SearchJourney> journies = new ArrayList<>();
		SearchJourney journey = new SearchJourney();
		journey.setDestination("BOM");
		journey.setOrigin("BLR");
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.MONTH, 2);
		journey.setTravelDate(calendar.getTime());
		journies.add(journey);
		
		return journies;
	}

}
