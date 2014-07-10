package com.compassites.GDSWrapper.travelport;

import com.compassites.model.Passenger;
import com.travelport.schema.air_v26_0.*;
import com.travelport.schema.common_v26_0.*;
import com.travelport.service.air_v26_0.AirAvailabilitySearchPortType;
import com.travelport.service.air_v26_0.AirFaultMessage;
import com.travelport.service.air_v26_0.AirPricePortType;
import com.travelport.service.air_v26_0.AirService;
import org.apache.commons.logging.LogFactory;

import javax.xml.ws.BindingProvider;
import java.io.IOException;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Renu
 * Date: 5/15/14
 * Time: 5:07 PM
 * To change this template use File | Settings | File Templates.
 */
public class AirRequestClient extends TravelPortClient {
    static final String ServiceName =  "/AirService";

    static AirService airService = null;
    static AirAvailabilitySearchPortType airAvailabilitySearchPort = null;
    static AirPricePortType airPricePortType = null;

    static void  init(){
        if (airService == null){
            java.net.URL url = null;
            try {
                java.net.URL baseUrl;
                baseUrl = AirService.class.getResource(".");
                url = new java.net.URL(baseUrl, "http://localhost:9000/wsdl/galileo/air_v26_0/Air.wsdl");
                airService = new AirService(url);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (airAvailabilitySearchPort == null){
            airAvailabilitySearchPort  = airService.getAirAvailabilitySearchPort();
            LogFactory.getLog(AirRequestClient.class).info("Initializing AirAvailabilitySearchPortType....");
            setRequestContext((BindingProvider) airAvailabilitySearchPort, ServiceName);
            LogFactory.getLog(AirRequestClient.class).info("Initialized");
        }
    }

    static void  initPricePort(){
        if (airService == null){
            java.net.URL url = null;
            try {
                java.net.URL baseUrl;
                baseUrl = AirService.class.getResource(".");
                url = new java.net.URL(baseUrl, "http://localhost:9000/wsdl/galileo/air_v26_0/Air.wsdl");
                airService = new AirService(url);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (airPricePortType == null){
            LogFactory.getLog(AirRequestClient.class).info("Initializing airPricePortType....");
            airPricePortType  = airService.getAirPricePort();
            setRequestContext((BindingProvider) airPricePortType, ServiceName);
            LogFactory.getLog(AirRequestClient.class).info("Initialized");
        }
    }

    public static void addPointOfSale(BaseReq req, String appName ) {
        BillingPointOfSaleInfo posInfo = new BillingPointOfSaleInfo();
        posInfo.setOriginApplication(appName);
        req.setBillingPointOfSaleInfo(posInfo);
    }


    public static SearchAirLeg createLeg(String originAirportCode,
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

    public static SearchAirLeg createLeg(TypeSearchLocation originLoc,
                                         TypeSearchLocation destLoc, TypeCabinClass cabinClass) {
        SearchAirLeg leg = new SearchAirLeg();

        // add the origin and dest to the leg
        leg.getSearchDestination().add(destLoc);
        leg.getSearchOrigin().add(originLoc);
        addPreferredCabinClass(leg, cabinClass);


        return leg;
    }

    public static void addPreferredCabinClass(SearchAirLeg leg, TypeCabinClass cabinClass) {
        AirLegModifiers modifiers = new AirLegModifiers();
        AirLegModifiers.PreferredCabins cabins = new AirLegModifiers.PreferredCabins();
        CabinClass econ = new CabinClass();
        econ.setType(cabinClass);

        cabins.setCabinClass(econ);
        modifiers.setPreferredCabins(cabins);
        modifiers.setOrderBy("JourneyTime");
        leg.setAirLegModifiers(modifiers);
    }

    public static void addDepartureDate(SearchAirLeg leg, String departureDate) {
        // flexible time spec is flexible in that it allows you to say
        // days before or days after
        TypeFlexibleTimeSpec noFlex = new TypeFlexibleTimeSpec();
        noFlex.setPreferredTime(departureDate);
        leg.getSearchDepTime().add(noFlex);
    }

    public static AirSearchModifiers createModifiersWithProviders(String ... providerCode) {
        AirSearchModifiers modifiers = new AirSearchModifiers();
        AirSearchModifiers.PreferredProviders providers = new AirSearchModifiers.PreferredProviders();
        for (int i=0; i<providerCode.length;++i) {
            Provider p = new Provider();
            // set the code for the provider
            p.setCode(providerCode[i]);
            // can be many providers, but we just use one
            providers.getProvider().add(p);
        }
        modifiers.setPreferredProviders(providers);
        return modifiers;
    }


    public AirRequestClient(){
        init();
    }

    public AvailabilitySearchRsp search(String origin,
                                        String destination, String dateOut,
                                        String dateBack, Boolean returnJourney,
                                        TypeCabinClass cabinClass) throws AirFaultMessage {
        AvailabilitySearchReq request = new AvailabilitySearchReq();
        AvailabilitySearchRsp response;
        request.setTargetBranch(BRANCH);
        addPointOfSale(request, "JustOneClick");

        AirSearchModifiers modifiers = createModifiersWithProviders(GDS);
        request.setAirSearchModifiers(modifiers);
        SearchAirLeg outbound = createLeg(origin, destination, cabinClass);
        addDepartureDate(outbound, dateOut);
        //addEconomyPreferred(outbound);
        //put traveller in econ
        //addEconomyPreferred(ret);
        //put the legs in the request

        List<SearchAirLeg> legs = request.getSearchAirLeg();
        legs.add(outbound);
        if (returnJourney){

            SearchAirLeg returnLeg = createLeg(destination, origin, cabinClass);
            addDepartureDate(returnLeg, dateBack);
            legs.add(returnLeg);
        }
        response = airAvailabilitySearchPort.service(request);
        //print out any messages that the GDS sends back
        for (ResponseMessage message : response.getResponseMessage())
            System.out.println("MESSAGE:" + message.getProviderCode() + " [" + message.getType()
                    + "] " + message.getValue());

        return response;
    }

    public static AirPricingSolution getPriceSolution(LowFareSearchRsp priceRsp, AirPricingSolution airPricingSolution) {

        AirPricingSolution soln = airPricingSolution;

        if (soln==null) {
            throw new RuntimeException("Unable to find any Pricing Solutions!");
        }

        Helper.AirSegmentMap allSegs =
                Helper.createAirSegmentMap(priceRsp.getAirSegmentList().getAirSegment());

        //Adjust segment refs to be real segments
        List<AirSegmentRef> refs = airPricingSolution.getJourney().get(0).getAirSegmentRef();
        for (Iterator<AirSegmentRef> refIter = refs.iterator(); refIter.hasNext();) {
            AirSegmentRef ref = (AirSegmentRef) refIter.next();
            soln.getAirSegment().add(allSegs.getByRef(ref));
        }
        soln.getAirSegmentRef().clear();


        return soln;
    }

    public static AirItinerary getItinerary(LowFareSearchRsp response, AirPricingSolution airPricingSolution){
        Helper.AirSegmentMap allSegments =
                Helper.createAirSegmentMap(response.getAirSegmentList().getAirSegment());
        Helper.FlightDetailsMap allDetails = Helper.createFlightDetailsMap(
                response.getFlightDetailsList().getFlightDetails());
        /*
        List<AirItinerarySolution> allSoln = response.getAirItinerarySolution();
        AirItinerarySolution outSoln = allSoln.get(0);
        AirItinerarySolution retSoln = allSoln.get(1);
        */
        List<AirItinerary> out = buildRoutings(airPricingSolution, 0, allSegments,
                allDetails);

        return out.get(0);
    }

    public static List<AirItinerary> buildRoutings(AirPricingSolution soln,
                                                   int resultingGroupNumber,
                                                   Helper.AirSegmentMap segmentMap, Helper.FlightDetailsMap detailMap) {
        ArrayList<AirItinerary> result = new ArrayList<AirItinerary>();

        //walk the list of segments in this itinerary... but convert them from
        //references to real segments for use in pricing
        List<AirSegmentRef> legs = soln.getJourney().get(0).getAirSegmentRef();
        ArrayList<TypeBaseAirSegment> segs = new ArrayList<TypeBaseAirSegment>();
        //when this loop is done, we have a list of segments that are good to
        //go for use in a pricing request...
        for (Iterator<AirSegmentRef> segIter = legs.iterator(); segIter.hasNext();) {
            AirSegmentRef ref = segIter.next();
            TypeBaseAirSegment realSegment = segmentMap.getByRef(ref);
            segs.add(cloneAndFixFlightDetails(realSegment, resultingGroupNumber, detailMap));
        }

        //a connection indicates that elements in the list of segs have to
        //be put together to make a routing
        List<Connection> conns = soln.getConnection();

        for (Iterator<Connection> connIter = conns.iterator(); connIter.hasNext();) {
            Connection connection = (Connection) connIter.next();
            AirItinerary itin = new AirItinerary();
            int idx = connection.getSegmentIndex();
            itin.getAirSegment().add(segs.get(idx));
            itin.getAirSegment().add(segs.get(idx+1));
            result.add(itin);
            segs.set(idx, null);
            segs.set(idx+1, null);
            //what happens when there is a double connection?
        }

        //those that are left are direct flights (no connections)
        for (int i=0; i<segs.size();++i) {
            TypeBaseAirSegment segment = segs.get(i);
            if (segment!=null) {
                AirItinerary itin = new AirItinerary();
                itin.getAirSegment().add(segment);
                result.add(itin);
            }
        }
        return result;
    }

    public static AirPricingSolution getPriceSolution(AirPriceRsp priceRsp) {
        List<AirPriceResult> r = priceRsp.getAirPriceResult();

        AirPricingSolution soln = null;

        for (Iterator<AirPriceResult> resultIter = r.iterator(); resultIter.hasNext();) {
            AirPriceResult airPriceResult = (AirPriceResult) resultIter.next();
            if (airPriceResult.getAirPricingSolution()!=null) {
                soln=airPriceResult.getAirPricingSolution().get(0);
                break;
            }
        }
        if (soln==null) {
            throw new RuntimeException("Unable to find any Pricing Solutions!");
        }

        Helper.AirSegmentMap allSegs =
                Helper.createAirSegmentMap(priceRsp.getAirItinerary().getAirSegment());

        //Adjust segment refs to be real segments
        List<AirSegmentRef> refs = soln.getAirSegmentRef();
        for (Iterator<AirSegmentRef> refIter = refs.iterator(); refIter.hasNext();) {
            AirSegmentRef ref = (AirSegmentRef) refIter.next();
            soln.getAirSegment().add(allSegs.getByRef(ref));
        }
        soln.getAirSegmentRef().clear();


        return soln;
    }

    public List<AirItinerary> getAllItinerary( BaseAvailabilitySearchRsp rsp){
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

    public static List<AirItinerary> buildRoutings(AirItinerarySolution airItinerarySolution,
                                                   int resultingGroupNumber,
                                                   Helper.AirSegmentMap segmentMap, Helper.FlightDetailsMap detailMap) {
        ArrayList<AirItinerary> result = new ArrayList<AirItinerary>();

        //walk the list of segments in this itinerary... but convert them from
        //references to real segments for use in pricing
        List<AirSegmentRef> legs = airItinerarySolution.getAirSegmentRef();
        ArrayList<TypeBaseAirSegment> airSegments = new ArrayList<TypeBaseAirSegment>();
        //when this loop is done, we have a list of segments that are good to
        //go for use in a pricing request...
        for (Iterator<AirSegmentRef> airSegmentRefIterator = legs.iterator(); airSegmentRefIterator.hasNext();) {
            AirSegmentRef airSegmentRef = airSegmentRefIterator.next();
            TypeBaseAirSegment realSegment = segmentMap.getByRef(airSegmentRef);
            airSegments.add(cloneAndFixFlightDetails(realSegment, resultingGroupNumber, detailMap));
        }

        //a connection indicates that elements in the list of segs have to
        //be put together to make a routing
        List<Connection> connections = airItinerarySolution.getConnection();

        for (Iterator<Connection> connectionsIterator = connections.iterator(); connectionsIterator.hasNext();) {
            Connection connection = (Connection) connectionsIterator.next();
            AirItinerary airItinerary = new AirItinerary();
            int idx = connection.getSegmentIndex();
            airItinerary.getAirSegment().add(airSegments.get(idx));
            airItinerary.getAirSegment().add(airSegments.get(idx+1));
            result.add(airItinerary);
            airSegments.set(idx, null);
            airSegments.set(idx+1, null);
            //what happens when there is a double connection?
        }

        //those that are left are direct flights (no connections)
        for (int i=0; i< airSegments.size();++i) {
            TypeBaseAirSegment segment = airSegments.get(i);
            if (segment!=null) {
                AirItinerary itin = new AirItinerary();
                itin.getAirSegment().add(segment);
                result.add(itin);
            }
        }
        return result;
    }

    public static TypeBaseAirSegment cloneAndFixFlightDetails(TypeBaseAirSegment original,
                                                              int resultingGroupNumber, Helper.FlightDetailsMap detailMap) {
        TypeBaseAirSegment result = new TypeBaseAirSegment();
        result.setCarrier(original.getCarrier());
        result.setClassOfService(original.getClassOfService());
        result.setFlightNumber(original.getFlightNumber());
        result.setKey(original.getKey());
        result.setDepartureTime(original.getDepartureTime());
        result.setArrivalTime(original.getArrivalTime());
        result.setDestination(original.getDestination());
        result.setOrigin(original.getOrigin());
        result.setProviderCode(GDS);
        result.setGroup(resultingGroupNumber);

        //adjust flight detail references to be REAL flight details
        List<FlightDetailsRef> flightDetailsRefs = original.getFlightDetailsRef();
        for (Iterator<FlightDetailsRef> flightDetailsRefIterator = flightDetailsRefs.iterator(); flightDetailsRefIterator.hasNext();) {
            FlightDetailsRef flightDetailsRef = (FlightDetailsRef) flightDetailsRefIterator.next();
            FlightDetails flightDetails = detailMap.getByRef(flightDetailsRef);
            result.getFlightDetails().add(flightDetails);
        }
        return result;
    }

    public static List<AirItinerary> mergeOutboundAndInbound(List<AirItinerary> out,
                                                             List<AirItinerary> in) {

        List<AirItinerary> result = new ArrayList<AirItinerary>();

        //each of the inbounds
        for (Iterator<AirItinerary> airItineraryIterator = in.iterator(); airItineraryIterator.hasNext();) {
            AirItinerary inAirIntinerary = (AirItinerary) airItineraryIterator.next();

            List<TypeBaseAirSegment> inSegments = inAirIntinerary.getAirSegment();

            //each of the outbounds
            for (Iterator<AirItinerary> iterator = out.iterator(); iterator.hasNext();) {
                AirItinerary outAirItinerary = (AirItinerary) iterator.next();

                List<TypeBaseAirSegment> outSegments = outAirItinerary.getAirSegment();

                //create a new merged itin with the in + out segmens
                AirItinerary merged = new AirItinerary();

                //note the ORDER is important here... we want to end up
                //with the inSegs before the outSegs and addAll puts the
                //new objects at the front
                merged.getAirSegment().addAll(outSegments);
                merged.getAirSegment().addAll(inSegments);
                result.add(merged);
            }
        }

        return result;
    }
    public static void displayItineraryPrice(AirItinerary airItinerary, String passengerType, String currency, TypeCabinClass cabinClass) throws AirFaultMessage {
        AirPriceRsp priceRsp = priceItinerary(airItinerary, passengerType, currency, cabinClass, null);

        //print price result
        List<AirPriceResult> prices = priceRsp.getAirPriceResult();
        for (Iterator<AirPriceResult> i = prices.iterator(); i.hasNext();) {
            AirPriceResult result = (AirPriceResult) i.next();
            if (result.getAirPriceError()!=null) {
                TypeResultMessage msg= result.getAirPriceError();
                System.err.println("Error during pricing operation:"+
                        msg.getType()+":"+msg.getValue());
            } else {
                List<AirPricingSolution> airPricingSolutions =  result.getAirPricingSolution();
                for (Iterator<AirPricingSolution> airPricingSolutionIterator = airPricingSolutions.iterator(); airPricingSolutionIterator.hasNext();){
                    AirPricingSolution airPricingSolution = (AirPricingSolution)airPricingSolutionIterator.next();
                    System.out.print("Price:"+ airPricingSolution.getTotalPrice());
                    System.out.print(" [BasePrice "+airPricingSolution.getBasePrice() +", ");
                    System.out.print("Taxes "+airPricingSolution.getTaxes()+"]");
                    System.out.println("CabinClass "+airPricingSolution.getAirPricingInfo().get(0).getBookingInfo().get(0).getCabinClass());
                }
            }
        }
    }

    /**
     * This just does the price computation so it is easy to re-use.
     *
     * @param
     * @return
     * @throws com.travelport.service.air_v26_0.AirFaultMessage
     */
    private static void setSeamanPassengerList(AirPriceReq request, List<Passenger> passengers, String passengerType){

        if (passengers == null){
            SearchPassenger adult = new SearchPassenger();
            adult.setCode(passengerType);
            adult.setKey("COMPASS");
            adult.setAge(new BigInteger(String.valueOf(30)));
            request.getSearchPassenger().add(adult);
            return;
        }
        for (Iterator<Passenger> passengerIterator = passengers.iterator(); passengerIterator.hasNext();) {
            Passenger passenger = passengerIterator.next();
            SearchPassenger searchPassenger = new SearchPassenger();
            if (passengerType == ""){
                searchPassenger.setCode(passenger.getPassengerType());
            }
            else{
                searchPassenger.setCode(passengerType);
            }
            searchPassenger.setKey("COMPASS");
            if (passenger.getAge() != null)
                searchPassenger.setAge(new BigInteger(String.valueOf(passenger.getAge())));
            request.getSearchPassenger().add(searchPassenger);
        }
    }
    public static AirPriceRsp priceItinerary(AirItinerary airItinerary, String passengerType, String currency, TypeCabinClass cabinClass, List<Passenger> passengers ) throws AirFaultMessage {
        //now lets try to price it
        AirPriceReq priceRequest = new AirPriceReq();

        //price the itinerary provided
        priceRequest.setAirItinerary(airItinerary);

        /*set cabin*/
        AirPricingCommand command1 = new AirPricingCommand();
        command1.setCabinClass(cabinClass);

        priceRequest.getAirPricingCommand().add(command1);

        priceRequest.setTargetBranch(BRANCH);


        setSeamanPassengerList(priceRequest, passengers, passengerType);

        BillingPointOfSaleInfo billingPointOfSaleInfo= new BillingPointOfSaleInfo();
        billingPointOfSaleInfo.setOriginApplication("Test-app");
        priceRequest.setBillingPointOfSaleInfo(billingPointOfSaleInfo);
        AirPricingModifiers airPricingModifiers = new AirPricingModifiers();
        airPricingModifiers.setCurrencyType(currency);
        priceRequest.setAirPricingModifiers(airPricingModifiers);

        initPricePort();
        return airPricePortType.service(priceRequest);

    }


    public void displayFlightDetailsAndItinerary(List<AirItinerary> allItineraries, String passengerType, String currency, TypeCabinClass cabinClass){
        for (Iterator<AirItinerary> allItinerariesIterator = allItineraries.iterator(); allItinerariesIterator.hasNext();) {
            AirItinerary itinerary = allItinerariesIterator.next();
            try {
                displayItineraryPrice(itinerary, passengerType, currency, cabinClass);
            } catch (AirFaultMessage e) {
                System.err.println("*** Unable to price itinerary:"+e.getMessage());
            }
            List<TypeBaseAirSegment> segments = itinerary.getAirSegment();
            for (Iterator<TypeBaseAirSegment> itineraryIterator = segments.iterator(); itineraryIterator.hasNext();) {
                TypeBaseAirSegment airSegment =  itineraryIterator.next();
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

}
