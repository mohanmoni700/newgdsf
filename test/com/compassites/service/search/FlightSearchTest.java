package com.compassites.service.search;

import com.compassites.GDSWrapper.amadeus.ServiceHandler;
import com.compassites.model.Passenger;
import com.compassites.model.SearchParameters;
import com.compassites.model.SearchResponse;
import com.compassites.model.*;
import org.junit.Test;
import services.AmadeusFlightSearch;
import services.FlightSearch;
import services.TravelPortFlightSearch;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import static junit.framework.TestCase.assertNotNull;

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

        searchParameters.setCabinClass(CabinClass.ECONOMY);
        searchParameters.setCurrency("INR");
        searchParameters.setDestination("DEL");
        searchParameters.setOrigin("BLR");

        Date onwardDate = new SimpleDateFormat("dd/MM/yyyy").parse("15/08/2014");

        Date returnDate = new SimpleDateFormat("dd/MM/yyyy").parse("14/07/2014");
        searchParameters.setFromDate(onwardDate);


        searchParameters.setWithReturnJourney(true);
        //searchParameters.setNoOfStops(new Integer("0"));

        //searchParameters.setDirectFlights(true);

        //searchParameters.setRefundableFlights(true);

        //searchParameters.setPreferredAirlineCode("SQ");
        searchParameters.setDateType(DateType.DEPARTURE);

        Passenger passenger = new Passenger();

        searchParameters.getPassengers().add(passenger);

        searchParameters.setTransit("BOM");
        searchParameters.setAdultCount(1);
        searchParameters.setChildCount(0);
        searchParameters.setInfantCount(0);
        ServiceHandler serviceHandler=new ServiceHandler();
        FlightSearch flightSearch = new AmadeusFlightSearch();
        SearchResponse response =  flightSearch.search(searchParameters);
        assertNotNull(response);
        assertNotNull(response.getAirSolution());
        assertNotNull(response.getAirSolution().getFlightItineraryList());

    }

    @Test
    public void TravelportFlightSearchPassingTest() throws ParseException,Exception {
        SearchParameters searchParameters = new SearchParameters();

        searchParameters.setCabinClass(CabinClass.ECONOMY);
        searchParameters.setCurrency("INR");

        searchParameters.setDestination("BLR");
        searchParameters.setOrigin("LAX");

        searchParameters.setWithReturnJourney(false);

        Calendar cal = Calendar.getInstance();
        cal.set(2014, 8, 24 );
        Date onwardDate = new SimpleDateFormat("MM/dd/yyyy").parse("09/04/2014");
        //searchParameters.setOnwardDate(Date.valueOf("24/06/2014"));
        Date returnDate = new SimpleDateFormat("MM/dd/yyyy").parse("09/14/2014");
        searchParameters.setFromDate(onwardDate);
        //searchParameters.setOnwardDate(Date.valueOf("24/06/2014"));
        searchParameters.setReturnDate(returnDate);
        //searchParameters.setBookingType("nonseamen");
        //searchParameters.setNoOfStops(new Integer("1"));
        //searchParameters.setDirectFlights(true);

        //searchParameters.setRefundableFlights(true);

        //searchParameters.setPreferredAirlineCode("9W");

        searchParameters.setDateType(DateType.DEPARTURE);

        Passenger passenger = new Passenger();
        passenger.setPassengerType(PassengerTypeCode.ADT);
        searchParameters.setTransit("AUH");
        searchParameters.getPassengers().add(passenger);
        FlightSearch flightSearch = new TravelPortFlightSearch();
        SearchResponse response =  flightSearch.search(searchParameters);

        //passenger.setPassengerType("ADT");
        //SearchResponse responseADT =  flightSearch.search(searchParameters);
        assertNotNull(response);
        assertNotNull(response.getAirSolution());
        assertNotNull(response.getAirSolution().getFlightItineraryList());
    }
}
