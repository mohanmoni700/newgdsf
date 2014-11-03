package com.compassites.GDSWrapper.travelport;

import com.compassites.model.traveller.Traveller;
import com.compassites.model.traveller.TravellerMasterInfo;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.thoughtworks.xstream.XStream;
import com.travelport.schema.air_v26_0.AirPricingSolution;
import com.travelport.schema.common_v26_0.*;
import com.travelport.schema.universal_v26_0.AirCreateReservationReq;
import com.travelport.schema.universal_v26_0.AirCreateReservationRsp;
import com.travelport.schema.universal_v26_0.TypeRetainReservation;
import com.travelport.service.universal_v26_0.AirCreateReservationPortType;
import com.travelport.service.universal_v26_0.AirService;
import com.travelport.service.universal_v26_0.AvailabilityFaultMessage;
import org.apache.commons.logging.LogFactory;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.ws.BindingProvider;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: Renu
 * Date: 5/22/14
 * Time: 3:34 PM
 * To change this template use File | Settings | File Templates.
 */
public class AirReservationClient  extends TravelPortClient {
    //https://support.travelport.com/webhelp/uapi/Content/Shared_Topics/TrueLastDateToTicket.htm

    static final String ServiceName =  "/AirService";

    static AirService airService = null;
    static AirCreateReservationPortType airCreateReservationPortType = null;


    static void  init(){
        if (airService == null){
            java.net.URL url = null;
            try {
                /*String path = new File(".").getCanonicalPath();
                airService = new AirService(new java.net.URL("http://localhost:9000/wsdl/galileo/universal_v26_0/UniversalRecord.wsdl"));
                */
                java.net.URL baseUrl;
                baseUrl = AirService.class.getResource(".");
                url = new java.net.URL(baseUrl, "http://localhost:9000/wsdl/galileo/universal_v26_0/UniversalRecord.wsdl");
                //url = new java.net.URL(baseUrl, "Air.wsdl");
                airService = new AirService(url);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (airCreateReservationPortType == null){
            airCreateReservationPortType  = airService.getAirCreateReservationPort();
            LogFactory.getLog(AirRequestClient.class).info("Initializing AirCreateReservationPortType....");
            setRequestContext((BindingProvider) airCreateReservationPortType, ServiceName);
            LogFactory.getLog(AirRequestClient.class).info("Initialized");
        }

    }

    public static AirCreateReservationRsp reserve(AirPricingSolution airPricingSolution,TravellerMasterInfo travellerMasterInfo) throws DatatypeConfigurationException {
        AirCreateReservationReq request = new AirCreateReservationReq();
        AirCreateReservationRsp response = new AirCreateReservationRsp();
        request.setAuthorizedBy("TEST");

        //if there are changes to schedule or price, tell us about it
        //in the returned result
        request.setRetainReservation(TypeRetainReservation.BOTH);

        //tell uapi where and for who the request is targeting
        request.setProviderCode(GDS);
        request.setTargetBranch("P7024203");
        request.setRuleName("ONLINE");


        //point of sale, YYY
        //PointOfSale pos=new PointOfSale();
        //pos.setPseudoCityCode("LON");
        //req.setPointOfSale(pos);
        BillingPointOfSaleInfo info = new BillingPointOfSaleInfo();
        //YYY
        info.setOriginApplication("UAPI");
        request.setBillingPointOfSaleInfo(info);

        //put traveller in request
        request.getBookingTraveler().addAll(createBookingTravellers(travellerMasterInfo));

        //provider
        request.setProviderCode("1G");

        //action status??

        //Need to get a factory
        DatatypeFactory factory = DatatypeFactory.newInstance();

        //payment
        /*
        FormOfPayment fop = new FormOfPayment();
        CreditCard cc = getFakeCreditCard(true);
        fop.setCreditCard(cc);
        fop.setKey(FORM_OF_PAYMENT_REF);

        //YYY
        //req.getFormOfPayment().add(fop);

        //hook payment to credit card
        Payment payment = new Payment();
        payment.setType("TicketFee");
        payment.setFormOfPaymentRef(FORM_OF_PAYMENT_REF);
        */

        //need to do avail/price workflow
        //===================================================
        //request.setAirPricingSolution(stripNonXmitSections(airPricingSolution));
        //===================================================

        request.setAirPricingSolution(airPricingSolution);
        //connect amount of payment to price solution
        //payment.setAmount(airPricingSolution.getTotalPrice());

        //this seems to be required
        ActionStatus actionStatus = new ActionStatus();
        //String ticketDate = factory.newXMLGregorianCalendarDate(2013, 10, 1,
        //        DatatypeConstants.FIELD_UNDEFINED).toString();
        XMLGregorianCalendar calendar = factory.newXMLGregorianCalendar();

        //we use a java calendar here because it can do arithemitic on dates
        //and stuff like that...
        Calendar javaCalendar = GregorianCalendar.getInstance();
        javaCalendar.setTime(new Date());//right now
        javaCalendar.add(Calendar.DAY_OF_MONTH, 2);//2 days from now

        calendar.setTime(23, 59, 0);//midnight day after tomorrow
        calendar.setTimezone(60);//paris is +1 hour from GMT

        //copy date from java calendar where we did the arithmetic
        //it is VERY odd that months start at 0 but days in the month start at 1!
        calendar.setMonth(javaCalendar.get(Calendar.MONTH)+1);
        calendar.setDay(javaCalendar.get(Calendar.DAY_OF_MONTH));
        calendar.setYear(javaCalendar.get(Calendar.YEAR));

        //convert to XML
        XMLGregorianCalendar greg = factory.newXMLGregorianCalendar();
        greg.setYear(calendar.getYear());
        greg.setMonth(calendar.getMonth());
        greg.setDay(calendar.getDay());
        greg.setHour(calendar.getHour());
        greg.setMinute(calendar.getMinute());
        greg.setSecond(calendar.getSecond());
        //now get the string out and set that to the ticket TIME, even though
        //it says "date" in the name
        actionStatus.setTicketDate(greg.toString());

        actionStatus.setProviderCode(GDS);
        actionStatus.setType("TAW");
        Remark remark = new Remark();
        //remark.setKey("testRemark");
        remark.setValue("TAUREMARKS");
        actionStatus.setRemark(remark);

        request.getActionStatus().add(actionStatus);

        //what is this check
        ContinuityCheckOverride override = new ContinuityCheckOverride();
        override.setValue("yes");
        request.setContinuityCheckOverride(override);

		/* if you want to set the versions instead of passing null here you
		 * need to be a bit careful as the server is quite particular about
		 * the values passed here...null seems to me "I accept the defauls"
		 */
        Writer writer = null;
        XStream xStream = new XStream();
        try {

            String xml=xStream.toXML(request);
            writer = new FileWriter("AirReserveRequest.xml");
            //Gson gson = new GsonBuilder().create();
            //gson.toJson(request, writer);
            writer.write(xml);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            init();
            AirCreateReservationReq manualRequest=new AirCreateReservationReq();

            //manualRequest=(AirCreateReservationReq)xStream.fromXML(new File("ManualAirReservationRequest"));
            response = airCreateReservationPortType.service(request, null);

        }
        catch (com.travelport.service.universal_v26_0.AirFaultMessage airFaultMessage) {
            airFaultMessage.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            airFaultMessage.getFaultInfo().getDescription();
            try {
                writer = new FileWriter("AirReserveResponseException.json");
                Gson gson = new GsonBuilder().create();
                gson.toJson(airFaultMessage, writer);
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        } catch (AvailabilityFaultMessage availabilityFaultMessage) {
            availabilityFaultMessage.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        try {
            writer = new FileWriter("AirReserveResponse.json");
            Gson gson = new GsonBuilder().create();
            gson.toJson(response, writer);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;

    }

    public static AirPricingSolution createAirPricing(AirPricingSolution airPricingSolution){
        return null;
    }

    public static AirPricingSolution stripNonXmitSections(AirPricingSolution airPricingSolution) {

	    /*
        <air:AirSegment Key="0T" Group="0" Carrier="AA" FlightNumber="789" ProviderCode="1G"                                                       8
                Origin="ORD" Destination="DEN" DepartureTime="2012-04-01T13:05:00.000-06:00"
                ArrivalTime="2012-04-02T14:35:00.000-07:00" Distance="903" ClassOfService="Y"
                ETicketability="Yes" Equipment="738" ChangeOfPlane="false"
                GuaranteedPaymentCarrier="No" TravelOrder="1"
                OptionalServicesIndicator="false" AvailabilitySource="StatusOverlaid"
                ParticipantLevel="Secure Sell" LinkAvailability="true"
                PolledAvailabilityOption="Polled avail exists">
        */
	    /*
	     <ns2:AirSegment Group="0" Carrier="AF" FlightNumber="84" ClassOfService="L"
	     ChangeOfPlane="false" OptionalServicesIndicator="false" FlightTime="670"
	     TravelTime="670" Distance="5573" Origin="CDG" Destination="SFO"
	     DepartureTime="2012-07-11T10:35:00.000+02:00"
	     ArrivalTime="2012-07-11T12:45:00.000-07:00" ProviderCode="1G" Key="0T"
	     TravelOrder="0"/>

	     */
       /* long travelOrder = 1;
        for (Iterator<TypeBaseAirSegment> iterator = airPricingSolution.getAirSegment().iterator(); iterator.hasNext();) {
            TypeBaseAirSegment seg = (TypeBaseAirSegment) iterator.next();
            //seg.setTravelOrder(BigInteger.valueOf(travelOrder));
            seg.getFlightDetails().clear();
            //travelOrder++;
            seg.setETicketability(TypeEticketability.YES);
            //seg.setEquipment("738");
            //seg.setParticipantLevel_0020("Secure Sell");
            seg.setGuaranteedPaymentCarrier("No");
            seg.setAvailabilitySource(TypeAvailabilitySource.SEAMLESS);
            //seg.setLinkAvailability(Boolean.TRUE);
            //seg.setPolledAvailabilityOption("Polled avail exists");
        }
        airPricingSolution.getAirPricingInfo().clear();
        airPricingSolution.getFareNote().clear();
        airPricingSolution.setOptionalServices(null);*/
        return airPricingSolution;
    }

    static public List<BookingTraveler> createBookingTravellers(TravellerMasterInfo travellerMasterInfo){
        List<BookingTraveler> bookingTravelerList =  new ArrayList<>();
        int i=1;
        //make the traveller info
        for(Traveller traveller : travellerMasterInfo.getTravellersList()){
            BookingTraveler bookingTraveler = new BookingTraveler();
            //traveller.setGender("Male");

            //home email
            Email email = new Email();
            email.setEmailID(travellerMasterInfo.getAdditionalInfo().getEmail());
            email.setType("Home");
            bookingTraveler.getEmail().add(email);

            PhoneNumber phone = new PhoneNumber();
            phone.setCountryCode("33");
            //phone.setAreaCode("6");
            phone.setNumber(travellerMasterInfo.getAdditionalInfo().getPhoneNumber());
            phone.setType("Mobile");
            bookingTraveler.getPhoneNumber().add(phone);
            //name
            BookingTravelerName name = new BookingTravelerName();

            name.setPrefix(traveller.getPersonalDetails().getGender());
            name.setFirst(traveller.getPersonalDetails().getFirstName());
            name.setMiddle(traveller.getPersonalDetails().getMiddleName());
            name.setLast(traveller.getPersonalDetails().getLastName());
            bookingTraveler.setBookingTravelerName(name);

            //address
            DeliveryInfo deliveryInfo=new DeliveryInfo();
            DeliveryInfo.ShippingAddress shippingAddress=new DeliveryInfo.ShippingAddress();
            shippingAddress.setAddressName("aaaa");
            shippingAddress.setCity("delhi");
            shippingAddress.setPostalCode("560078");
            shippingAddress.setCountry("IN");
            deliveryInfo.setShippingAddress(shippingAddress);
            bookingTraveler.getDeliveryInfo().add(deliveryInfo);

            bookingTraveler.setAge(new BigInteger("22"));
            //adult
            bookingTraveler.setTravelerType("SEA");
            bookingTraveler.setKey(i++ + "");
            bookingTravelerList.add(bookingTraveler);
        }
        return bookingTravelerList;
    }
}
