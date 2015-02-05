package com.compassites.GDSWrapper.travelport;

import com.compassites.model.DateType;
import com.compassites.model.Passenger;
import com.compassites.model.SearchJourney;
import com.compassites.model.SearchParameters;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.travelport.schema.air_v26_0.*;
import com.travelport.schema.common_v26_0.*;
import com.travelport.service.air_v26_0.AirFaultMessage;
import com.travelport.service.air_v26_0.AirLowFareSearchPortType;
import com.travelport.service.air_v26_0.AirService;

import org.apache.commons.logging.LogFactory;
import org.springframework.util.StringUtils;
import utils.XMLFileUtility;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.BindingProvider;

import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static javax.xml.bind.JAXBContext.newInstance;

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

    static void  init() {
        if (airService == null) {
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
        if (airLowFareSearchPortTypePort == null) {
            airLowFareSearchPortTypePort = airService.getAirLowFareSearchPort();
            LogFactory.getLog(AirRequestClient.class).info("Initializing AirAvailabilitySearchPortType....");
            setRequestContext((BindingProvider) airLowFareSearchPortTypePort, ServiceName);
            LogFactory.getLog(AirRequestClient.class).info("Initialized");
        }
    }

    //TODO-Remove the age hard coding
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
        TypeSearchAirLeg outbound = createLeg(origin, destination, cabinClass, false, null,null);
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

            TypeSearchAirLeg returnLeg = createLeg(destination, origin, cabinClass, false, null,null);
            addDepartureDate(returnLeg, dateBack);
            //returnLeg.setAirLegModifiers(airLegModifiers);
            legs.add(returnLeg);
        }


        AirPricingModifiers airPricingModifiers = new AirPricingModifiers();
        airPricingModifiers.setCurrencyType(currency);
        request.setAirPricingModifiers(airPricingModifiers);

        init();

        response = airLowFareSearchPortTypePort.service(request);
        try {
            Writer writer = new FileWriter("Response.json");
            Gson gson = new Gson();
            gson.toJson(response, writer);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;

    }

    private static void setDefaultValues(LowFareSearchReq request){
        request.setTargetBranch(BRANCH);
        AirRequestClient.addPointOfSale(request, "JustOneClick");
    }

    private static List<TypeSearchAirLeg> buildLeg(SearchParameters searchParameters){
        List<TypeSearchAirLeg> airLegList=new ArrayList<>();
        for(SearchJourney journey:searchParameters.getJourneyList()) {
            TypeCabinClass cabinClass = TypeCabinClass.valueOf(searchParameters.getCabinClass().upperValue());
            TypeSearchAirLeg airLeg = createLeg(journey.getOrigin(), journey.getDestination(), cabinClass, searchParameters.getDirectFlights(), searchParameters.getPreferredAirlines(), searchParameters.getTransit());
            String journeyDate = searchFormat.format(journey.getTravelDate());
            if (searchParameters.getDateType() == DateType.ARRIVAL)
                addArrivalDate(airLeg, journeyDate);
            else
                addDepartureDate(airLeg, journeyDate);
            airLegList.add(airLeg);
        }
        return airLegList;
    }

    private static void setPassengerList(LowFareSearchReq request, List<Passenger> passengers, String bookingType){

        for (Iterator<Passenger> passengerIterator = passengers.iterator(); passengerIterator.hasNext();) {
            Passenger passenger = passengerIterator.next();
            SearchPassenger searchPassenger = new SearchPassenger();
            if (bookingType.equalsIgnoreCase("seaman")) {
                searchPassenger.setCode("SEA");
                searchPassenger.setPricePTCOnly(true);
            }
            else
            {
                searchPassenger.setCode(passenger.getPassengerType().toString());
            }

            searchPassenger.setKey("COMPASS");

            if (passenger.getAge() != null)
                searchPassenger.setAge(new BigInteger(String.valueOf(passenger.getAge())));

            request.getSearchPassenger().add(searchPassenger);
        }
    }
    private static LowFareSearchReq buildQuery(SearchParameters searchParameters){
        LowFareSearchReq request = new LowFareSearchReq();
        setDefaultValues(request);
        setPassengerList(request, searchParameters.getPassengers(), searchParameters.getSearchBookingType());

        List<TypeSearchAirLeg> legs = request.getSearchAirLeg();
        legs.addAll(buildLeg(searchParameters));

        /*if (searchParameters.getWithReturnJourney()){
            SearchParameters returnParameters=new SearchParameters();
            returnParameters=searchParameters.clone();
            String origin=searchParameters.getOrigin();
            String destination=searchParameters.getDestination();
            returnParameters.setFromDate(searchParameters.getReturnDate());
            returnParameters.setReturnDate(searchParameters.getFromDate());
            returnParameters.setOrigin(destination);
            returnParameters.setDestination(origin);

            TypeSearchAirLeg returnLeg = buildLeg(returnParameters);
            legs.add(returnLeg);
        }*/

        AirSearchModifiers modifiers = AirRequestClient.createModifiersWithProviders(GDS);

        modifiers.setMaxSolutions(BigInteger.valueOf(10));

        request.setAirSearchModifiers(modifiers);

        AirSearchModifiers airSearchModifiers = new AirSearchModifiers();

        AirPricingModifiers airPricingModifiers = new AirPricingModifiers();

        airPricingModifiers.setCurrencyType(searchParameters.getCurrency());

        // By default, both refundable and non-refundable fares are returned.
        // If the ProhibitNonRefundableFares attribute is set to 'True', only fully refundable fares are returned in the response.
        airPricingModifiers.setProhibitNonRefundableFares(searchParameters.getRefundableFlights());

        request.setAirPricingModifiers(airPricingModifiers);

        return request;
    }
    
    public static LowFareSearchRsp search(SearchParameters searchParameters) throws AirFaultMessage {
        LowFareSearchReq request = buildQuery(searchParameters);
        XMLFileUtility.createXMLFile(request, "Travelport" + searchParameters.getSearchBookingType() + "LowFareSearchReq.xml");
        init();
        LowFareSearchRsp response = airLowFareSearchPortTypePort.service(request);
        XMLFileUtility.createXMLFile(response, "Travelport" + searchParameters.getSearchBookingType() + "LowFareSearchRes.xml");
        return response;
    }

    public static SOAPMessage encodeSOAPMessage(LowFareSearchReq request) throws JAXBException, SOAPException, IOException {
        JAXBContext context = newInstance("com.travelport.schema.air_v26_0");
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

        //Next, create the actual message
        MessageFactory messageFactory = MessageFactory.newInstance();
        SOAPMessage message = messageFactory.createMessage();
        marshaller.marshal(request, message.getSOAPBody());

        message.saveChanges();
        ByteArrayOutputStream in = new ByteArrayOutputStream();
        Writer writer = new FileWriter("Request.json");
        message.writeTo(in);
        writer.write(String.valueOf(in));
        writer.close();
        return message;
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
                                         String destAirportCode,
                                         TypeCabinClass cabinClass,
                                         Boolean directFlight,
                                         String preferredAirlineCode,
                                         String transit) {
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

        return createLeg(originLoc, destLoc, cabinClass, directFlight, preferredAirlineCode,transit);
    }

    public static TypeSearchAirLeg createLeg(TypeSearchLocation originLoc,
                                         TypeSearchLocation destLoc,
                                         TypeCabinClass cabinClass,
                                         Boolean directFlight,
                                         String preferredAirlineCode,
                                         String transit) {
        TypeSearchAirLeg leg = new TypeSearchAirLeg();

        // add the origin and dest to the leg
        leg.getSearchDestination().add(destLoc);
        leg.getSearchOrigin().add(originLoc);
        addPrefererences(leg, cabinClass, directFlight, preferredAirlineCode,transit);


        return leg;
    }

    public static void addPrefererences(TypeSearchAirLeg leg,
                                        TypeCabinClass cabinClass,
                                        Boolean directFlight,
                                        String preferredAirlineCode,
                                        String transit) {
        AirLegModifiers modifiers = new AirLegModifiers();
        AirLegModifiers.PreferredCabins cabins = new AirLegModifiers.PreferredCabins();
        CabinClass cabinClass1 = new CabinClass();
        cabinClass1.setType(cabinClass);

        cabins.setCabinClass(cabinClass1);
        modifiers.setPreferredCabins(cabins);
        modifiers.setOrderBy("JourneyTime");
        modifiers.setPreferNonStop(directFlight);
        FlightType flightType = new FlightType();
        flightType.setNonStopDirects(directFlight);
        modifiers.setFlightType(flightType);

        if (preferredAirlineCode != null && StringUtils.hasText(preferredAirlineCode)) {
            AirLegModifiers.PreferredCarriers preferredCarriers = new AirLegModifiers.PreferredCarriers();
            Carrier carrier = new Carrier();
            carrier.setCode(preferredAirlineCode);
            preferredCarriers.getCarrier().add(carrier);
            modifiers.setPreferredCarriers(preferredCarriers);
        }
        else if(transit != null){
            AirLegModifiers.PermittedConnectionPoints permittedConnectionPoints = new AirLegModifiers.PermittedConnectionPoints();
            TypeLocation typeLocation = new TypeLocation();
            Airport transitAirport = new Airport();
            transitAirport.setCode(transit);
            typeLocation.setAirport(transitAirport);
            permittedConnectionPoints.getConnectionPoint().add(typeLocation);
            modifiers.setPermittedConnectionPoints(permittedConnectionPoints);
        }
        leg.setAirLegModifiers(modifiers);
    }

    public static void addDepartureDate(TypeSearchAirLeg leg, String departureDate) {
        // flexible time spec is flexible in that it allows you to say
        // days before or days after
        TypeFlexibleTimeSpec noFlex = new TypeFlexibleTimeSpec();
        noFlex.setPreferredTime(departureDate);
        leg.getSearchDepTime().add(noFlex);
    }

    public static void addArrivalDate(TypeSearchAirLeg leg, String arrivalDate) {
        // flexible time spec is flexible in that it allows you to say
        // days before or days after
        TypeFlexibleTimeSpec noFlex = new TypeFlexibleTimeSpec();
        noFlex.setPreferredTime(arrivalDate);
        leg.getSearchArvTime().add(noFlex);
    }


}
