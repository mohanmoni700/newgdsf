package com.compassites.service.search;

import com.avaje.ebean.Ebean;
import com.compassites.GDSWrapper.amadeus.ServiceHandler;
import com.compassites.model.Passenger;
import com.compassites.model.SearchParameters;
import com.compassites.model.SearchResponse;
import com.compassites.model.*;
import models.AirlineCode;
import models.Airport;
import org.junit.Before;
import org.junit.Test;
import play.libs.Yaml;
import play.test.Helpers;
import play.test.WithApplication;
import services.AmadeusFlightSearch;
import services.FlightSearch;
import services.TravelPortFlightSearch;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static junit.framework.TestCase.assertNotNull;
import static play.test.Helpers.*;

/**
 * Created with IntelliJ IDEA.
 * User: Renu
 * Date: 5/23/14
 * Time: 4:03 PM
 * To change this template use File | Settings | File Templates.
 */
public class FlightSearchTest extends WithApplication {
    @Before
    public void setUp() {
        final HashMap<String,String> postgres = new HashMap<String, String>();
        postgres.put("db.default.driver","com.mysql.jdbc.Driver");
        postgres.put("db.default.url","jdbc:mysql://localhost/jocdb_development");
        postgres.put("db.default.user", "jocdbuser");
        postgres.put("db.default.password", "jocdbpassword");

        start(fakeApplication(postgres));
    }


    @Test
    public void AmadeusFlightSearchPassingTest() throws ParseException,Exception {
        SearchParameters searchParameters = new SearchParameters();

        Properties properties=new Properties();
        properties.load(getClass().getClassLoader().getResourceAsStream("amadeusFlightDetails.properties"));
        String dateFormat=properties.getProperty("dateFormat");
        searchParameters.setCabinClass(CabinClass.ECONOMY);
        searchParameters.setCurrency(properties.getProperty("currency"));
        searchParameters.setDestination(properties.getProperty("destination"));
        searchParameters.setOrigin(properties.getProperty("origin"));

        Date onwardDate = new SimpleDateFormat(dateFormat).parse(properties.getProperty("onwardDate"));

        Date returnDate = new SimpleDateFormat(dateFormat).parse(properties.getProperty("returnDate"));
        searchParameters.setFromDate(onwardDate);


        searchParameters.setWithReturnJourney(true);
        //searchParameters.setNoOfStops(new Integer("0"));

        //searchParameters.setDirectFlights(true);

        //searchParameters.setRefundableFlights(true);

        //searchParameters.setPreferredAirlineCode("SQ");
        searchParameters.setDateType(DateType.DEPARTURE);

        Passenger passenger = new Passenger();

        searchParameters.getPassengers().add(passenger);

        searchParameters.setTransit(properties.getProperty("transit"));
        searchParameters.setAdultCount(Integer.parseInt(properties.getProperty("adultCount")));
        searchParameters.setChildCount(Integer.parseInt(properties.getProperty("childCount")));
        searchParameters.setInfantCount(Integer.parseInt(properties.getProperty("infantCount")));
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

        Properties properties=new Properties();
        properties.load(getClass().getClassLoader().getResourceAsStream("travelportFlightDetails.properties"));
        String dateFormat=properties.getProperty("dateFormat");

        searchParameters.setCabinClass(CabinClass.ECONOMY);
        searchParameters.setCurrency(properties.getProperty("currency"));

        searchParameters.setDestination(properties.getProperty("destination"));
        searchParameters.setOrigin(properties.getProperty("origin"));

        searchParameters.setWithReturnJourney(false);

        Calendar cal = Calendar.getInstance();
        cal.set(2014, 8, 24 );
        Date onwardDate = new SimpleDateFormat(dateFormat).parse(properties.getProperty("onwardDate"));
        //searchParameters.setOnwardDate(Date.valueOf("24/06/2014"));
        Date returnDate = new SimpleDateFormat(dateFormat).parse(properties.getProperty("returnDate"));
        searchParameters.setFromDate(onwardDate);
        //searchParameters.setOnwardDate(Date.valueOf("24/06/2014"));
        //searchParameters.setReturnDate(returnDate);
        //searchParameters.setBookingType("nonseamen");
        //searchParameters.setNoOfStops(new Integer("1"));
        //searchParameters.setDirectFlights(true);

        //searchParameters.setRefundableFlights(true);

        //searchParameters.setPreferredAirlineCode("9W");

        searchParameters.setDateType(DateType.DEPARTURE);

        searchParameters.setAdultCount(Integer.parseInt(properties.getProperty("adultCount")));
        searchParameters.setChildCount(Integer.parseInt(properties.getProperty("childCount")));
        searchParameters.setInfantCount(Integer.parseInt(properties.getProperty("infantCount")));


        Passenger passenger = new Passenger();
        passenger.setPassengerType(PassengerTypeCode.ADT);
        //searchParameters.setTransit(properties.getProperty("transit"));
        searchParameters.getPassengers().add(passenger);
        TravelPortFlightSearch flightSearch = new TravelPortFlightSearch();
        SearchResponse response =  flightSearch.search(searchParameters);

        //passenger.setPassengerType("ADT");
        //SearchResponse responseADT =  flightSearch.search(searchParameters);
        assertNotNull(response);
        assertNotNull(response.getAirSolution());
        assertNotNull(response.getAirSolution().getFlightItineraryList());
    }


}
