package com.compassites.GDSWrapper.travelport;

import com.compassites.model.Passenger;
import com.compassites.model.SearchParameters;
import com.travelport.schema.air_v26_0.*;
import com.travelport.schema.common_v26_0.*;
import com.travelport.service.air_v26_0.AirFaultMessage;
import com.travelport.service.air_v26_0.AirLowFareSearchPortType;
import com.travelport.service.air_v26_0.AirService;
import org.apache.commons.logging.LogFactory;

import javax.xml.ws.BindingProvider;
import java.io.IOException;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.util.Iterator;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Renu
 * Date: 5/20/14
 * Time: 11:18 AM
 * To change this template use File | Settings | File Templates.
 */
public class LowFareRequestClient extends TravelPortClient {
    static final String ServiceName =  "/AirService";

    static AirService airService = null;
    static AirLowFareSearchPortType airLowFareSearchPortTypePort = null;

    static void  init(){
        if (airService == null){
            java.net.URL url = null;
            try {
                //String path = new File(".").getCanonicalPath();
                //airService = new AirService(new java.net.URL("file:"+path+"/wsdl/galileo/air_v26_0/Air.wsdl"));
                java.net.URL baseUrl;
                baseUrl = AirService.class.getResource(".");
                url = new java.net.URL(baseUrl, "http://localhost:9000/wsdl/galileo/air_v26_0/Air.wsdl");
                //url = new java.net.URL(baseUrl, "Air.wsdl");
                airService = new AirService(url);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }


        }
        if (airLowFareSearchPortTypePort == null){
            airLowFareSearchPortTypePort  = airService.getAirLowFareSearchPort();
            LogFactory.getLog(AirRequestClient.class).info("Initializing AirAvailabilitySearchPortType....");
            setRequestContext((BindingProvider) airLowFareSearchPortTypePort, ServiceName);
            LogFactory.getLog(AirRequestClient.class).info("Initialized");
        }
    }
    public static void addAdultPassengers(BaseLowFareSearchReq request, int n) {
        for (int i = 0; i < n; ++i) {
            SearchPassenger adult = new SearchPassenger();
            adult.setCode("ADT");
            request.getSearchPassenger().add(adult);
        }
    }

    public static void addSeamanPassengers(BaseLowFareSearchReq request, int n) {
        for (int i = 0; i < n; ++i) {
            SearchPassenger adult = new SearchPassenger();
            adult.setCode("SEA");
            request.getSearchPassenger().add(adult);
        }
    }

    public static LowFareSearchRsp search(String origin,
                                              String destination, String dateOut,
                                              String dateBack, Boolean returnJourney,
                                              TypeCabinClass cabinClass, String passengerType, String currency) throws AirFaultMessage {
        LowFareSearchReq request = new LowFareSearchReq();
        LowFareSearchRsp response;
        request.setTargetBranch(BRANCH);
        AirRequestClient.addPointOfSale(request, "JustOneClick");

        //AirSearchModifiers modifiers = AirRequestClient.createModifiersWithProviders(GDS);
        //request.setAirSearchModifiers(modifiers);
        TypeSearchAirLeg outbound = createLeg(origin, destination, cabinClass);
        addDepartureDate(outbound, dateOut);
        //addEconomyPreferred(outbound);
        //put traveller in econ
        //addEconomyPreferred(ret);
        //put the legs in the request
        //AirLegModifiers airLegModifiers = new AirLegModifiers();
        //airLegModifiers.setOrderBy("JourneyTime");
        //outbound.setAirLegModifiers(airLegModifiers);
        SearchPassenger searchPassenger = new SearchPassenger();
        searchPassenger.setCode(passengerType);
        searchPassenger.setKey("COMPASS");
        searchPassenger.setAge(new BigInteger(String.valueOf(30)));
        request.getSearchPassenger().add(searchPassenger);
        List<TypeSearchAirLeg> legs = request.getSearchAirLeg();
        legs.add(outbound);
        if (returnJourney){

            TypeSearchAirLeg returnLeg = createLeg(destination, origin, cabinClass);
            addDepartureDate(returnLeg, dateBack);
            //returnLeg.setAirLegModifiers(airLegModifiers);
            legs.add(returnLeg);
        }


        AirPricingModifiers airPricingModifiers = new AirPricingModifiers();
        airPricingModifiers.setCurrencyType(currency);
        request.setAirPricingModifiers(airPricingModifiers);

        init();
        response = airLowFareSearchPortTypePort.service(request);
        return response;

    }

    private static void setDefaultValues(LowFareSearchReq request){
        request.setTargetBranch(BRANCH);
        AirRequestClient.addPointOfSale(request, "JustOneClick");
    }

    private static TypeSearchAirLeg buildLeg(String origin, String destination, SearchParameters.JourneySpecificParameters journeySpecificParameters){
        TypeCabinClass cabinClass = TypeCabinClass.valueOf(journeySpecificParameters.getCabinClass().upperValue());
        TypeSearchAirLeg airLeg = createLeg(origin, destination, cabinClass);
        String journeyDate = searchFormat.format(journeySpecificParameters.getJourneyDate());
        addDepartureDate(airLeg, journeyDate);

        return airLeg;
    }

    private static void setPassengerList(LowFareSearchReq request, List<Passenger> passengers){

        for (Iterator<Passenger> passengerIterator = passengers.iterator(); passengerIterator.hasNext();) {
            Passenger passenger = passengerIterator.next();
            SearchPassenger searchPassenger = new SearchPassenger();
            searchPassenger.setCode(passenger.getPassengerType());
            searchPassenger.setKey("COMPASS");
            if (passenger.getAge() != null)
                searchPassenger.setAge(new BigInteger(String.valueOf(passenger.getAge())));
            request.getSearchPassenger().add(searchPassenger);
        }
    }
    private static LowFareSearchReq buildQuery(SearchParameters searchParameters){
        LowFareSearchReq request = new LowFareSearchReq();
        setDefaultValues(request);
        setPassengerList(request, searchParameters.getPassengers());

        TypeSearchAirLeg outbound = buildLeg(searchParameters.getOrigin(), searchParameters.getDestination(), searchParameters.getOnwardJourney());
        List<TypeSearchAirLeg> legs = request.getSearchAirLeg();
        legs.add(outbound);

        if (searchParameters.getWithReturnJourney()){
            TypeSearchAirLeg returnLeg = buildLeg(searchParameters.getDestination(), searchParameters.getOrigin(), searchParameters.getReturnJourney());
            legs.add(returnLeg);
        }


        //AirSearchModifiers airSearchModifiers = new AirSearchModifiers();

        //airSearchModifiers.setPreferNonStop(searchParameters.getDirectFlights());

        //if (searchParameters.getPreferredAirlineCode() != null) {
        //    AirSearchModifiers.PreferredCarriers preferredCarriers = new AirSearchModifiers.PreferredCarriers();
        //    Carrier carrier = new Carrier();
        //    carrier.setCode(searchParameters.getPreferredAirlineCode());
        //    preferredCarriers.getCarrier().add(carrier);
        //    airSearchModifiers.setPreferredCarriers(preferredCarriers);
        //}

        //request.setAirSearchModifiers(airSearchModifiers);

        AirPricingModifiers airPricingModifiers = new AirPricingModifiers();

        airPricingModifiers.setCurrencyType(searchParameters.getCurrency());

        // By default, both refundable and non-refundable fares are returned.
        // If the ProhibitNonRefundableFares attribute is set to 'True', only fully refundable fares are returned in the response.
        //airPricingModifiers.setProhibitNonRefundableFares(searchParameters.getRefundableFlights());

        request.setAirPricingModifiers(airPricingModifiers);

        return request;
    }

    public static LowFareSearchRsp search(SearchParameters searchParameters) throws AirFaultMessage {
        LowFareSearchReq request = buildQuery(searchParameters);
        LowFareSearchRsp response;
        init();
        response = airLowFareSearchPortTypePort.service(request);
        return response;
    }

    public List<AirItinerary> getAllItinerary( LowFareSearchRsp rsp){
        Helper.AirSegmentMap allSegments = Helper.createAirSegmentMap(
                rsp.getAirSegmentList().getAirSegment());
        Helper.FlightDetailsMap allDetails = Helper.createFlightDetailsMap(
                rsp.getFlightDetailsList().getFlightDetails());

        //Each "solution" is for a particular part of the journey... on
        //a round trip there will be two of thes
        List<AirItinerarySolution> solutions = rsp.getAirItinerarySolution();
        AirItinerarySolution outboundSolution = solutions.get(0);
        List<AirItinerary> out = AirRequestClient.buildRoutings(outboundSolution, 0, allSegments, allDetails);
        List<AirItinerary> allItins = null;

        if (solutions.size() > 1){
            AirItinerarySolution inboundSolution = solutions.get(1);
            //bound the routings by using the connections
            List<AirItinerary> in = AirRequestClient.buildRoutings(inboundSolution, 1, allSegments, allDetails);

            //merge in and out itins so we can get pricing for whole deal
            allItins = AirRequestClient.mergeOutboundAndInbound(out, in);
        }
        else {
            allItins = out;
        }

        return allItins;
    }


    public static void displayPriceSolution(LowFareSearchRsp response){
        Helper.AirSegmentMap allSegments = Helper.createAirSegmentMap(
                response.getAirSegmentList().getAirSegment());
        Helper.FlightDetailsMap allDetails = Helper.createFlightDetailsMap(
                response.getFlightDetailsList().getFlightDetails());
        List<AirPricingSolution> airPricingSolutions =  response.getAirPricingSolution();
        for (Iterator<AirPricingSolution> airPricingSolutionIterator = airPricingSolutions.iterator(); airPricingSolutionIterator.hasNext();){
            AirPricingSolution airPricingSolution = (AirPricingSolution)airPricingSolutionIterator.next();
            System.out.print("Price:"+ airPricingSolution.getTotalPrice());
            System.out.print(" [BasePrice "+airPricingSolution.getBasePrice() +", ");
            System.out.print("Taxes "+airPricingSolution.getTaxes()+"]");
            System.out.println("CabinClass " + airPricingSolution.getAirPricingInfo().get(0).getBookingInfo().get(0).getCabinClass());
            List<AirSegmentRef> airSegmentRefList = airPricingSolution.getJourney().get(0).getAirSegmentRef();
            for (Iterator<AirSegmentRef> airSegmentRefIterator = airSegmentRefList.iterator(); airSegmentRefIterator.hasNext();){
                AirSegmentRef airSegmentRef = airSegmentRefIterator.next();
                TypeBaseAirSegment airSegment =  allSegments.getByRef(airSegmentRef);
                String carrier="??";
                String flightNum="???";
                if (airSegment!=null) {
                    if (airSegment.getCarrier()!=null) {
                        carrier = airSegment.getCarrier();
                    }
                    if (airSegment.getFlightNumber()!=null) {
                        flightNum = airSegment.getFlightNumber();
                    }
                }
                System.out.print(carrier+"#"+flightNum);
                String o="???",d="???";
                if (airSegment!=null) {
                    if (airSegment.getOrigin()!=null) {
                        o=airSegment.getOrigin();
                    }
                    if (airSegment.getDestination()!=null) {
                        d=airSegment.getDestination();
                    }
                }
                System.out.print(" from "+o+" to "+ d);
                String dtime = "??:??";
                if (airSegment!=null) {
                    if (airSegment.getDepartureTime()!=null) {
                        dtime = airSegment.getDepartureTime();
                    }
                }
                System.out.print(" at "+dtime);
                if ((airSegment!=null) && (airSegment.getFlightTime()!=null)) {
                    System.out.println(" (flight time "+airSegment.getFlightTime()+" minutes)");
                } else {
                    System.out.println();
                }
            }
            System.out.println("-----------");

        }
    }
    public static TypeSearchAirLeg createLeg(String originAirportCode,
                                         String destAirportCode, TypeCabinClass cabinClass) {
        TypeSearchLocation originLoc = new TypeSearchLocation();
        TypeSearchLocation destLoc = new TypeSearchLocation();

        // airport objects are just wrappers for their codes
        Airport origin = new Airport(), dest = new Airport();
        origin.setCode(originAirportCode);
        dest.setCode(destAirportCode);

        // search locations can be things other than airports but we are using
        // the airport version...
        originLoc.setAirport(origin);
        destLoc.setAirport(dest);

        return createLeg(originLoc, destLoc, cabinClass);
    }

    public static TypeSearchAirLeg createLeg(TypeSearchLocation originLoc,
                                         TypeSearchLocation destLoc, TypeCabinClass cabinClass) {
        TypeSearchAirLeg leg = new TypeSearchAirLeg();

        // add the origin and dest to the leg
        leg.getSearchDestination().add(destLoc);
        leg.getSearchOrigin().add(originLoc);
        addPreferredCabinClass(leg, cabinClass);


        return leg;
    }

    public static void addPreferredCabinClass(TypeSearchAirLeg leg, TypeCabinClass cabinClass) {
        AirLegModifiers modifiers = new AirLegModifiers();
        AirLegModifiers.PreferredCabins cabins = new AirLegModifiers.PreferredCabins();
        CabinClass cabinClass1 = new CabinClass();
        cabinClass1.setType(cabinClass);

        cabins.setCabinClass(cabinClass1);
        modifiers.setPreferredCabins(cabins);
        modifiers.setOrderBy("JourneyTime");
        leg.setAirLegModifiers(modifiers);
    }

    public static void addDepartureDate(TypeSearchAirLeg leg, String departureDate) {
        // flexible time spec is flexible in that it allows you to say
        // days before or days after
        TypeFlexibleTimeSpec noFlex = new TypeFlexibleTimeSpec();
        noFlex.setPreferredTime(departureDate);
        leg.getSearchDepTime().add(noFlex);
    }


}
