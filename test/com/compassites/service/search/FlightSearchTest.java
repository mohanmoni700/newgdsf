package com.compassites.service.search;

import com.compassites.model.CabinClass;
import com.compassites.model.Passenger;
import com.compassites.model.SearchParameters;
import com.compassites.model.SearchResponse;
import org.junit.Test;
import services.AmadeusFlightSearch;
import services.FlightSearch;
import services.TravelPortFlightSearch;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import static org.junit.Assert.assertNotNull;

/**
 * Created with IntelliJ IDEA.
 * User: Renu
 * Date: 5/23/14
 * Time: 4:03 PM
 * To change this template use File | Settings | File Templates.
 */
public class FlightSearchTest {
    @Test
    public void AmadeusFlightSearchPassingTest() throws ParseException,Exception {
        SearchParameters searchParameters = new SearchParameters();
        searchParameters.getOnwardJourney().setCabinClass(CabinClass.ECONOMY);
        searchParameters.setCurrency("INR");
        searchParameters.setDestination("BLR");
        searchParameters.setOrigin("BOM");

        Date onwardDate = new SimpleDateFormat("dd/MM/yyyy").parse("30/06/2014");
        searchParameters.getOnwardJourney().setJourneyDate(onwardDate);

        Date returnDate = new SimpleDateFormat("dd/MM/yyyy").parse("14/07/2014");
        searchParameters.getReturnJourney().setJourneyDate(returnDate);


        searchParameters.setWithReturnJourney(true);
        searchParameters.setNoOfStops(new Integer("1"));
        Passenger passenger = new Passenger();
        passenger.setPassengerType("SEA");
        searchParameters.getPassengers().add(passenger);
        FlightSearch flightSearch = new AmadeusFlightSearch();
        SearchResponse response =  flightSearch.search(searchParameters);
        assertNotNull(response);
        assertNotNull(response.getAirSolution());
        assertNotNull(response.getAirSolution().getFlightItineraryList());

    }

    @Test
    public void TravelportFlightSearchPassingTest() throws ParseException,Exception {
        SearchParameters searchParameters = new SearchParameters();
        searchParameters.getOnwardJourney().setCabinClass(CabinClass.ECONOMY);
        searchParameters.setCurrency("INR");
        searchParameters.setDestination("BLR");
        searchParameters.setOrigin("SIN");
        searchParameters.setWithReturnJourney(false);

        Calendar cal = Calendar.getInstance();
        cal.set(2014, 8, 24 );
        Date onwardDate = new SimpleDateFormat("MM/dd/yyyy").parse("08/18/2014");
        searchParameters.getOnwardJourney().setJourneyDate(onwardDate );
        //searchParameters.setOnwardDate(Date.valueOf("24/06/2014"));
        searchParameters.setNoOfStops(new Integer("1"));
        Passenger passenger = new Passenger();
        passenger.setPassengerType("SEA");
        searchParameters.getPassengers().add(passenger);
        FlightSearch flightSearch = new TravelPortFlightSearch();
        SearchResponse response =  flightSearch.search(searchParameters);
        assertNotNull(response);
        assertNotNull(response.getAirSolution());
        assertNotNull(response.getAirSolution().getFlightItineraryList());
    }
}
