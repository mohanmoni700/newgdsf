/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.compassites.GDSWrapper.amadeus;

import com.amadeus.xml.fmptbq_14_2_1a.*;
import com.compassites.model.*;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import utils.CorporateCodeHelper;

import java.math.BigInteger;
import java.util.*;

/**
 * @author mahendra-singh
 */
public class SearchFlights {

    static int itineraryRef = 1;

    static Logger amadeusLogger = LoggerFactory.getLogger("amadeus");
    String vistaraAirlineStr = play.Play.application().configuration().getString("vistara.airline.code");

    //using deprecated methods
    //change to calendar dates everywhere
    private String mapDate(Date date, DateTime dateTime) {
        String amadeusDate = "";
        Calendar calDate = Calendar.getInstance();
        calDate.setTime(date);
        String day = String.valueOf(calDate.get(Calendar.DAY_OF_MONTH));
        String month = String.valueOf(calDate.get(Calendar.MONTH) + 1);
        String year = String.valueOf(calDate.get(Calendar.YEAR));
        day = day.length() == 1 ? "0" + day : day;
        month = month.length() == 1 ? "0" + month : month;
        year = year.length() == 1 ? "0" + year : year;
        amadeusDate = day + month + year.substring(2);


        amadeusLogger.debug(">>>>>>>>>>>>>>>>>>>>>>>>  Date : " + date + " DateTIme : " + dateTime);
        day = "" + dateTime.getDayOfMonth();
        month = "" + dateTime.getMonthOfYear();
        year = "" + dateTime.getYearOfCentury();

        day = day.length() == 1 ? "0" + day : day;
        month = month.length() == 1 ? "0" + month : month;
        year = year.length() == 1 ? "0" + year : year;
        amadeusDate = day + month + year;


        return amadeusDate;
    }

    //search flights with 2 cities- faremasterpricertravelboardsearch service
    public FareMasterPricerTravelBoardSearch createSearchQuery(SearchParameters searchParameters) {
        FareMasterPricerTravelBoardSearch se = new FareMasterPricerTravelBoardSearch();
        if(searchParameters.getBookingType() == BookingType.SEAMEN){
            se.setNumberOfUnit(createNumberOfUnits(searchParameters.getChildCount() + searchParameters.getAdultCount() + searchParameters.getInfantCount()));
        }else {
            se.setNumberOfUnit(createNumberOfUnits(searchParameters.getChildCount() + searchParameters.getAdultCount()));
        }

       //se.getPaxReference().addAll(createPassengers(searchParameters));

        createSeamenPassengers(se, searchParameters);

        se.getItinerary().addAll(createItinerary(searchParameters));

        TravelFlightInformationType165052S travelFlightInfo = new TravelFlightInformationType165052S();

        if (searchParameters.getPreferredAirlinesList() != null &&searchParameters.getPreferredAirlinesList().size()>0) {
            setPreferredAirlines(travelFlightInfo,searchParameters.getPreferredAirlinesList());
        }

        if (searchParameters.getDirectFlights()) {
            setDirectFlights(travelFlightInfo);
        }

        if (searchParameters.getBookingType() == BookingType.SEAMEN) {
            se.setTravelFlightInfo(travelFlightInfo);
        } else {
            se.setTravelFlightInfo(new TravelFlightInformationType165052S());
        }

        FareMasterPricerTravelBoardSearch.FareOptions fe = new FareMasterPricerTravelBoardSearch.FareOptions();
        PricingTicketingDetailsType pdt = new PricingTicketingDetailsType();
        PricingTicketingInformationType pit = new PricingTicketingInformationType();
        pdt.setPricingTicketing(pit);
        fe.setPricingTickInfo(pdt);

        if (searchParameters.getBookingType() !=BookingType.SEAMEN) {
            CodedAttributeType codedAttributeType = new CodedAttributeType();
            CodedAttributeInformationType247829C codedAttributeInformationType247829C = new CodedAttributeInformationType247829C();
            codedAttributeInformationType247829C.setFeeType("FFI");
            codedAttributeInformationType247829C.setFeeIdNumber("3");
            codedAttributeType.getFeeId().add(codedAttributeInformationType247829C);

            CodedAttributeInformationType247829C codedAttributeInformationType247829C1 = new CodedAttributeInformationType247829C();
            codedAttributeInformationType247829C1.setFeeType("UPH");
            codedAttributeInformationType247829C1.setFeeIdNumber("3");
            codedAttributeType.getFeeId().add(codedAttributeInformationType247829C1);

            fe.setFeeIdDescription(codedAttributeType);
        }

        if (searchParameters.getRefundableFlights()) {
            setRefundableFlights(pit);
        }

        setCabinClass(searchParameters.getCabinClass(),travelFlightInfo);

        if (searchParameters.getBookingType() == BookingType.SEAMEN) {
            createSeamenFareOptions(pit);
        } else {
            createFareOptions(pit);
        }

        //se.setFareOptions(fareOptions);

        if (searchParameters.getBookingType() == BookingType.SEAMEN) {
            CorporateIdentificationType corporateIdentificationType = createCorporateCode(pit, searchParameters);
            fe.setCorporate(corporateIdentificationType);
//            FareMasterPricerTravelBoardSearch.FareOptions fe1 = new FareMasterPricerTravelBoardSearch.FareOptions();
//            PricingTicketingDetailsType pdt1 = new PricingTicketingDetailsType();
//            PricingTicketingInformationType pit1 = new PricingTicketingInformationType();
            pit.getPriceType().add("PTC");

            /*pit.getPriceType().add("RW");
            CorporateIdentificationType corporateIdentificationType = new CorporateIdentificationType();
            CorporateIdentityType corporateIdentityType = new CorporateIdentityType();
            corporateIdentityType.setCorporateQualifier("RW");
            corporateIdentityType.getIdentity().add("061724");
            corporateIdentityType.getIdentity().add("752375");
            corporateIdentificationType.getCorporateId().add(corporateIdentityType);
            fe.setCorporate(corporateIdentificationType);*/

//            pdt1.setPricingTicketing(pit1);
//            fe1.setPricingTickInfo(pdt1);
//            se.setFareOptions(fe1);
        }
        se.setFareOptions(fe);
        /*FareMasterPricerTravelBoardSearch.FareOptions fareOptions = new FareMasterPricerTravelBoardSearch.FareOptions();
        PricingTicketingDetailsType pricingTicketingDetailsType = new PricingTicketingDetailsType();
        PricingTicketingInformationType pricingTicketingInformationType = new PricingTicketingInformationType();
        pricingTicketingInformationType.getPriceType().add("IAV");
        pricingTicketingDetailsType.setPricingTicketing(pricingTicketingInformationType);
        fareOptions.setPricingTickInfo(pricingTicketingDetailsType);
        se.setFareOptions(fareOptions);*/
//        XMLFileUtility.createXMLFile(se, "AmadeusSearchReq.xml");

//        amadeusLogger.debug("AmadeusSearchReq " + new Date() + " ---->" + new XStream().toXML(se));
        return se;
    }

    private CorporateIdentificationType createCorporateCode(PricingTicketingInformationType pricingTicketingInformationType,SearchParameters searchParameters){
        CorporateIdentificationType corporateIdentificationType = new CorporateIdentificationType();
        CorporateIdentityType corporateIdentityType = new CorporateIdentityType();
        corporateIdentityType.setCorporateQualifier("RW");
        corporateIdentityType.getIdentity().add("061724");
        if(!searchParameters.getPreferredAirlinesList().isEmpty()){
            for(int i= 0; i < searchParameters.getPreferredAirlinesList().size(); i++) {
                String airlineCorporateCode = CorporateCodeHelper.getAirlineCorporateCode(searchParameters.getBookingType() + "." + searchParameters.getPreferredAirlinesList().get(i));
                if (airlineCorporateCode != null) {
                    corporateIdentityType.getIdentity().add(airlineCorporateCode);
                }
            }
        } else {
            corporateIdentityType.getIdentity().add(CorporateCodeHelper.getAirlineCorporateCode(searchParameters.getBookingType() + "." + vistaraAirlineStr));
        }
        corporateIdentificationType.getCorporateId().add(corporateIdentityType);
        return corporateIdentificationType;
    }

    private void createSeamenPassengers(FareMasterPricerTravelBoardSearch search, SearchParameters searchParameters){
        List<TravellerReferenceInformationType> passengers = new ArrayList<TravellerReferenceInformationType>();
        List<TravellerReferenceInformationType> passengers1 = new ArrayList<TravellerReferenceInformationType>();
        List<TravellerReferenceInformationType> passengers2 = new ArrayList<TravellerReferenceInformationType>();
        List<TravellerReferenceInformationType> passengers3 = new ArrayList<TravellerReferenceInformationType>();
        Stack<BigInteger> adultReferenceNumbers = new Stack<>();
        int reference = 0;
        int infReference = 0;
        int infIndicator = 0;

        List<TravellerDetailsType> adtTravellerDetailsTypeList = new ArrayList<>();
        List<TravellerDetailsType> chdTravellerDetailsTypeList = new ArrayList<>();
        List<TravellerDetailsType> infTravellerDetailsTypeList = new ArrayList<>();
        List<TravellerDetailsType> seamenTravellerDetailsTypeList = new ArrayList<>();
        for (Passenger passenger : searchParameters.getPassengers()) {
            if (searchParameters.getBookingType() != BookingType.SEAMEN) {
                if (passenger.getPassengerType().name().equals("ADT")) {
                    TravellerDetailsType tdt = new TravellerDetailsType();
                    tdt.setRef(new BigInteger(Integer.toString(++reference)));
                    adtTravellerDetailsTypeList.add(tdt);
                } else if (passenger.getPassengerType().name().equals("CHD")) {
                    TravellerDetailsType tdt1 = new TravellerDetailsType();
                    tdt1.setRef(new BigInteger(Integer.toString(++reference)));
                    chdTravellerDetailsTypeList.add(tdt1);
                } else if (passenger.getPassengerType().name().equals("IN") || passenger.getPassengerType().name().equals("INF")) {
                    TravellerDetailsType tdt2 = new TravellerDetailsType();
                    tdt2.setRef(new BigInteger(Integer.toString(++infReference)));
                    tdt2.setInfantIndicator(BigInteger.valueOf(++infIndicator));
                    infTravellerDetailsTypeList.add(tdt2);
                }
            } else {
                TravellerDetailsType tdt3 = new TravellerDetailsType();
                tdt3.setRef(new BigInteger(Integer.toString(++reference)));
                seamenTravellerDetailsTypeList.add(tdt3);
            }
        }
        TravellerReferenceInformationType adtTraveller = new TravellerReferenceInformationType();
        if (adtTravellerDetailsTypeList.size() > 0){
            adtTraveller.getTraveller().addAll(adtTravellerDetailsTypeList);
            passengers.add(adtTraveller);
            adtTraveller.getPtc().add(PassengerTypeCode.ADT.toString());
            search.getPaxReference().addAll(passengers);
        }

        TravellerReferenceInformationType chdTraveller = new TravellerReferenceInformationType();
        if (chdTravellerDetailsTypeList.size() > 0){
            chdTraveller.getTraveller().addAll(chdTravellerDetailsTypeList);
            passengers1.add(chdTraveller);
            chdTraveller.getPtc().add(PassengerTypeCode.CHD.toString());
            search.getPaxReference().addAll(passengers1);
        }

        TravellerReferenceInformationType infTraveller = new TravellerReferenceInformationType();
        if (infTravellerDetailsTypeList.size() > 0){
            infTraveller.getTraveller().addAll(infTravellerDetailsTypeList);
            passengers2.add(infTraveller);
            infTraveller.getPtc().add(PassengerTypeCode.INF.toString());
            search.getPaxReference().addAll(passengers2);
        }

        TravellerReferenceInformationType seamenTraveller = new TravellerReferenceInformationType();
        if (seamenTravellerDetailsTypeList.size() > 0){
            seamenTraveller.getTraveller().addAll(seamenTravellerDetailsTypeList);
            passengers3.add(seamenTraveller);
            seamenTraveller.getPtc().add(PassengerTypeCode.SEA.toString());
            search.getPaxReference().addAll(passengers3);
        }

    }
    private void setPreferredAirlines(TravelFlightInformationType165052S travelFlightInfo,List<String> carrier){
        CompanyIdentificationType233548C cid = new CompanyIdentificationType233548C();
        /*
        * use the below lines of code to include the selected airline along with the
        * other connecting airlines incase of multi segments
        * */
        //cid.getCarrierId().add(carrier);
        //cid.getCarrierId().add("YY");
        //cid.setCarrierQualifier("M");
        cid.getCarrierId().addAll(carrier);
        cid.setCarrierQualifier("M");
        travelFlightInfo.getCompanyIdentity().add(cid);
    }

    private void setDirectFlights(TravelFlightInformationType165052S travelFlightInfo){
        ProductTypeDetailsType120801C ptd = new ProductTypeDetailsType120801C();
        ptd.getFlightType().add("D");
        ptd.getFlightType().add("N");
        travelFlightInfo.setFlightDetail(ptd);
    }

    private void setRefundableFlights(PricingTicketingInformationType pit){
        //FareMasterPricerTravelBoardSearch.FareOptions fe = new FareMasterPricerTravelBoardSearch.FareOptions();
        //PricingTicketingInformationType pit = new PricingTicketingInformationType();
        pit.getPriceType().add("RF");
        //fe.setPricingTickInfo(pdt);
        //se.setFareOptions(fe);
    }

    private void createSeamenFareOptions(PricingTicketingInformationType pit) {
        pit.getPriceType().add("TAC");
        pit.getPriceType().add("RU");
        pit.getPriceType().add("RP");
        pit.getPriceType().add("ET");
        pit.getPriceType().add("RW");
        pit.getPriceType().add("MNR");
    }

    private void createFareOptions(PricingTicketingInformationType pit) {
        //FareMasterPricerTravelBoardSearch.FareOptions fe = new FareMasterPricerTravelBoardSearch.FareOptions();
        //PricingTicketingDetailsType pdt = new PricingTicketingDetailsType();
        //PricingTicketingInformationType pit = new PricingTicketingInformationType();
        //pit.getPriceType().add("TAC");
        pit.getPriceType().add("RU");
        pit.getPriceType().add("RP");

        pit.getPriceType().add("ET");
        //pit.getPriceType().add("RW");
       // pit.getPriceType().add("MNR");
       /* pit.getPriceType().add("PTC");
        pit.getPriceType().add("ET");
        pit.getPriceType().add("NSD");*/

        //pdt.setPricingTicketing(pit);

       /* CodedAttributeType fid = new CodedAttributeType();
        CodedAttributeInformationType247829C cid = new CodedAttributeInformationType247829C();
        cid.setFeeType("NPS");
        cid.setFeeIdNumber("0");
        fid.getFeeId().add(cid);
        fe.setFeeIdDescription(fid);*/
        //fe.setPricingTickInfo(pdt);
        //return fe;
    }

    private NumberOfUnitsType createNumberOfUnits(int noOfPassengers) {
        NumberOfUnitsType nu = new NumberOfUnitsType();
        NumberOfUnitDetailsType191580C nudt = new NumberOfUnitDetailsType191580C();
        nudt.setNumberOfUnits(new BigInteger(Integer.toString(noOfPassengers)));
        nudt.setTypeOfUnit("PX");

        NumberOfUnitDetailsType191580C nudt1 = new NumberOfUnitDetailsType191580C();
        String noOfSearchResults =  play.Play.application().configuration().getString("amadeus.noOfSearchResults");
        nudt1.setNumberOfUnits(new BigInteger(noOfSearchResults));
        nudt1.setTypeOfUnit("RC");
        nu.getUnitNumberDetail().add(nudt);
        nu.getUnitNumberDetail().add(nudt1);
        return nu;
    }

    private List<TravellerReferenceInformationType> createPassengers(SearchParameters searchParameters) {
        List<TravellerReferenceInformationType> passengers = new ArrayList<TravellerReferenceInformationType>();
        Stack<BigInteger> adultReferenceNumbers = new Stack<>();
        int reference = 0;
        //adultReferenceNumbers.push(BigInteger.ONE);

        for (Passenger passenger : searchParameters.getPassengers()) {
            if ((passenger.getPassengerType().equals("INF")||passenger.getPassengerType().equals("IN")) && (searchParameters.getBookingType() == BookingType.SEAMEN)) {
                continue;
            }
            TravellerReferenceInformationType traveller = new TravellerReferenceInformationType();
            TravellerDetailsType tdt = new TravellerDetailsType();
            tdt.setRef(new BigInteger(Integer.toString(++reference)));

            if (searchParameters.getBookingType() != BookingType.SEAMEN) {

                switch (passenger.getPassengerType()) {
                    case ADT:
                        adultReferenceNumbers.push(new BigInteger(Integer.toString(reference)));
                        break;
                    case INF:
                    case IN:
                        tdt.setInfantIndicator(BigInteger.valueOf(1));
                        tdt.setRef(adultReferenceNumbers.pop());
                        break;
                }
                traveller.getPtc().add(passenger.getPassengerType().toString());
            } else {
                traveller.getPtc().add(PassengerTypeCode.SEA.toString());
            }
            traveller.getTraveller().add(tdt);
            passengers.add(traveller);
        }

        return passengers;
    }

    private DepartureLocationType setDepartureLocationType(String origin, String originAirportCityQualifier){
        DepartureLocationType dlt = new DepartureLocationType();
        MultiCityOptionType mcot = new MultiCityOptionType();
        ArrivalLocationDetailsType120834C arrivalLocationDetailsType120834C = new ArrivalLocationDetailsType120834C();
        arrivalLocationDetailsType120834C.setLocationId(origin);
        mcot.setLocationId(origin);
        mcot.setAirportCityQualifier(originAirportCityQualifier);
       // dlt.getDepMultiCity().add(mcot);
        dlt.setDeparturePoint(arrivalLocationDetailsType120834C);
        return dlt;
    }

    private ArrivalLocalizationType setArrivalLocalizationType(String destination, String destinationAirportCityQualifier){
        ArrivalLocalizationType alt=new ArrivalLocalizationType();
        ArrivalLocationDetailsType arrivalLocationDetailsType = new ArrivalLocationDetailsType();
        MultiCityOptionType mcot1=new MultiCityOptionType();
        mcot1.setLocationId(destination);
        mcot1.setAirportCityQualifier(destinationAirportCityQualifier);
       // alt.getArrivalMultiCity().add(mcot1);
        arrivalLocationDetailsType.setLocationId(destination);
        alt.setArrivalPointDetails(arrivalLocationDetailsType);
        return alt;
    }

    private DateAndTimeInformationType181295S setDateAndTimeInformationType(DateType dateType,String fromDate){
        DateAndTimeInformationType181295S dti = new DateAndTimeInformationType181295S();
        DateAndTimeDetailsTypeI dtit = new DateAndTimeDetailsTypeI();
        if (dateType== DateType.ARRIVAL) {
            dtit.setTimeQualifier("TA");
            dtit.setTime("2359");
        }else if (dateType== DateType.DEPARTURE){
            dtit.setTimeQualifier("TD");
            dtit.setTime("0000");
        }
        dtit.setDate(fromDate);
        dti.setFirstDateTimeDetail(dtit);
        return dti;
    }

    private FareMasterPricerTravelBoardSearch.Itinerary setItineraryLocationDetails(FareMasterPricerTravelBoardSearch.Itinerary itinerary,BigInteger referenceNumber,String origin,String destination, String oacQualifier, String dacQualifier){
        OriginAndDestinationRequestType forwardOrdt=new OriginAndDestinationRequestType();
        forwardOrdt.setSegRef(referenceNumber);
        itinerary.setRequestedSegmentRef(forwardOrdt);

//        itinerary.setDepartureLocalization(setDepartureLocationType("NYC"));
//        itinerary.setArrivalLocalization(setArrivalLocalizationType("BLR"));
        itinerary.setDepartureLocalization(setDepartureLocationType(origin, oacQualifier));
        itinerary.setArrivalLocalization(setArrivalLocalizationType(destination, dacQualifier));
        return itinerary;
    }

    private List<FareMasterPricerTravelBoardSearch.Itinerary> createItinerary(SearchParameters searchParameters){
        List<FareMasterPricerTravelBoardSearch.Itinerary> itineraryList=new ArrayList<>();
        int counter=1;
        for(SearchJourney searchJourney:searchParameters.getJourneyList()){
            FareMasterPricerTravelBoardSearch.Itinerary itinerary=new FareMasterPricerTravelBoardSearch.Itinerary();
            setItineraryLocationDetails(itinerary, new BigInteger(Integer.toString(counter++)), searchJourney.getOrigin(), searchJourney.getDestination(), searchJourney.getOriginAirportCityQualifier(), searchJourney.getDestinationAirportCityQualifier());
            DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
            DateTime dateTime = fmt.parseDateTime(searchJourney.getTravelDateStr());
            itinerary.setTimeDetails(setDateAndTimeInformationType(searchParameters.getDateType(),mapDate(searchJourney.getTravelDate(), dateTime)));
            if(StringUtils.hasText(searchParameters.getTransit()) && !searchParameters.getDirectFlights()){
                TravelFlightInformationType165053S fi=new TravelFlightInformationType165053S();
                setTransitPoint(searchParameters.getTransit(),fi,itinerary);
            }
            itineraryList.add(itinerary);
        }
        return itineraryList;
    }

    private void setTransitPoint(String transitPoint,TravelFlightInformationType165053S fi,FareMasterPricerTravelBoardSearch.Itinerary idt){
        ConnectPointDetailsType195492C connectingPoint = new ConnectPointDetailsType195492C();
        connectingPoint.setInclusionIdentifier("M");
        connectingPoint.setLocationId(transitPoint);
        fi.getInclusionDetail().add(connectingPoint);
        idt.setFlightInfo(fi);
    }

    private void setCabinClass(CabinClass cabinClass, TravelFlightInformationType165052S fi){
        String cabinQualifier="";
        if(CabinClass.BUSINESS.equals(cabinClass)){
            cabinQualifier = "C";
        }else if(CabinClass.FIRST.equals(cabinClass)){
            cabinQualifier = "F";
        }else if(CabinClass.PREMIUM_ECONOMY.equals(cabinClass)){
            cabinQualifier = "W";
        }else{
            cabinQualifier = "Y";
        }
        /*switch (cabinClass){
            case BUSINESS:cabinQualifier="C";
            case FIRST:cabinQualifier="F";
            default:cabinQualifier="Y";
        }*/
        CabinIdentificationType233500C cit=new CabinIdentificationType233500C();
        cit.getCabin().add(cabinQualifier);
        fi.setCabinId(cit);
    }
}
