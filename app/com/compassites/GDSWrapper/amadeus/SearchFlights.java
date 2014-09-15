/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.compassites.GDSWrapper.amadeus;

import com.amadeus.xml.fmptbq_12_4_1a.*;
import com.compassites.model.*;
import play.libs.Json;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.math.BigInteger;
import java.util.*;

/**
 * @author mahendra-singh
 */
public class SearchFlights {

    static int itineraryRef = 1;

    //using deprecated methods
    //change to calendar dates everywhere
    private String mapDate(Date date) {
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
        return amadeusDate;
    }

    //search flights with 2 cities- faremasterpricertravelboardsearch service
    public FareMasterPricerTravelBoardSearch createSearchQuery(SearchParameters searchParameters) {
        FareMasterPricerTravelBoardSearch se = new FareMasterPricerTravelBoardSearch();
        se.setNumberOfUnit(createNumberOfUnits(searchParameters.getChildCount() + searchParameters.getAdultCount()));

        se.getPaxReference().addAll(createPassengers(searchParameters));
        se.getItinerary().add(createItinerary(searchParameters));

        TravelFlightInformationType148734S travelFlightInfo = new TravelFlightInformationType148734S();

        if (searchParameters.getPreferredAirlines() != null) {
            setPreferredAirlines(travelFlightInfo,searchParameters.getPreferredAirlines());
        }

        if (searchParameters.getDirectFlights()) {
            setDirectFlights(travelFlightInfo);
        }

        se.setTravelFlightInfo(travelFlightInfo);

        if (searchParameters.getRefundableFlights()) {
            setRefundableFlights(se);
        }

        if (searchParameters.getBookingType() == BookingType.SEAMEN) {
            FareMasterPricerTravelBoardSearch.FareOptions fe1 = new FareMasterPricerTravelBoardSearch.FareOptions();
            PricingTicketingDetailsType pdt1 = new PricingTicketingDetailsType();
            PricingTicketingInformationType pit1 = new PricingTicketingInformationType();
            pit1.getPriceType().add("PTC");
            pdt1.setPricingTicketing(pit1);
            fe1.setPricingTickInfo(pdt1);
            se.setFareOptions(fe1);
        }

        if(searchParameters.getWithReturnJourney()){
            se.getItinerary().add(createItinerary(searchParameters));
        }

        //se.setFareOptions(createFareOptions());
//        TravelFlightInformationType148734S tfi=new TravelFlightInformationType148734S();
//        CompanyIdentificationType214105C cid=new CompanyIdentificationType214105C();
//        cid.getCarrierId().add("QR");
//        cid.setCarrierQualifier("M");
//        tfi.getCompanyIdentity().add(cid);
//        se.setTravelFlightInfo(tfi);
        //System.out.println("Amadeus Request : "+ Json.toJson(se));
        File file=new File(searchParameters.getBookingType()+"RequestPA.json");
        FileOutputStream os= null;
        try {
            os = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        PrintStream out = new PrintStream(os);
        out.print(Json.toJson(se));
        return se;
    }

    private void setPreferredAirlines(TravelFlightInformationType148734S travelFlightInfo,String carrier){
        CompanyIdentificationType214105C cid = new CompanyIdentificationType214105C();
        cid.getCarrierId().add(carrier);
        cid.getCarrierId().add("YY");
        cid.setCarrierQualifier("M");
        travelFlightInfo.getCompanyIdentity().add(cid);
    }

    private void setDirectFlights(TravelFlightInformationType148734S travelFlightInfo){
        ProductTypeDetailsType120801C ptd = new ProductTypeDetailsType120801C();
        ptd.getFlightType().add("D");
        ptd.getFlightType().add("N");
        travelFlightInfo.setFlightDetail(ptd);
    }

    private void setRefundableFlights(FareMasterPricerTravelBoardSearch se){
        FareMasterPricerTravelBoardSearch.FareOptions fe = new FareMasterPricerTravelBoardSearch.FareOptions();
        PricingTicketingDetailsType pdt = new PricingTicketingDetailsType();
        PricingTicketingInformationType pit = new PricingTicketingInformationType();
        pit.getPriceType().add("RF");
        pdt.setPricingTicketing(pit);
        fe.setPricingTickInfo(pdt);
        se.setFareOptions(fe);
    }

    private FareMasterPricerTravelBoardSearch.FareOptions createFareOptions() {
        FareMasterPricerTravelBoardSearch.FareOptions fe = new FareMasterPricerTravelBoardSearch.FareOptions();
        PricingTicketingDetailsType pdt = new PricingTicketingDetailsType();
        PricingTicketingInformationType pit = new PricingTicketingInformationType();
        pit.getPriceType().add("RP");
        pit.getPriceType().add("RU");
        pit.getPriceType().add("TAC");
        pit.getPriceType().add("PTC");
        pit.getPriceType().add("ET");
        pit.getPriceType().add("NSD");

        pdt.setPricingTicketing(pit);

        CodedAttributeType78503S fid = new CodedAttributeType78503S();
        CodedAttributeInformationType120700C cid = new CodedAttributeInformationType120700C();
        cid.setFeeType("NPS");
        cid.setFeeIdNumber("0");
        fid.getFeeId().add(cid);
        fe.setFeeIdDescription(fid);
        fe.setPricingTickInfo(pdt);
        return fe;
    }

    private NumberOfUnitsType createNumberOfUnits(int noOfPassengers) {
        NumberOfUnitsType nu = new NumberOfUnitsType();
        NumberOfUnitDetailsType191580C nudt = new NumberOfUnitDetailsType191580C();
        nudt.setNumberOfUnits(new BigInteger(Integer.toString(noOfPassengers)));
        nudt.setTypeOfUnit("PX");

        NumberOfUnitDetailsType191580C nudt1 = new NumberOfUnitDetailsType191580C();
        nudt1.setNumberOfUnits(new BigInteger("200"));
        nudt1.setTypeOfUnit("RC");
        nu.getUnitNumberDetail().add(nudt);
        nu.getUnitNumberDetail().add(nudt1);
        return nu;
    }

    private List<TravellerReferenceInformationType> createPassengers(SearchParameters searchParameters) {
        List<TravellerReferenceInformationType> passengers = new ArrayList<TravellerReferenceInformationType>();
        Stack<BigInteger> adultReferenceNumbers = new Stack<>();
        int reference = 1;

        for (Passenger passenger : searchParameters.getPassengers()) {
            if ((passenger.getPassengerType().equals("INF")) && (searchParameters.getBookingType() == BookingType.SEAMEN)) {
                continue;
            }
            adultReferenceNumbers.push(BigInteger.ONE);
            TravellerReferenceInformationType traveller = new TravellerReferenceInformationType();
            TravellerDetailsType tdt = new TravellerDetailsType();
            tdt.setRef(new BigInteger(Integer.toString(reference++)));

            if (searchParameters.getBookingType() != BookingType.SEAMEN) {

                switch (passenger.getPassengerType()) {
                    case ADT:
                        adultReferenceNumbers.push(new BigInteger(Integer.toString(reference)));
                        break;
                    case INF:
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

    private FareMasterPricerTravelBoardSearch.Itinerary createItinerary(SearchParameters searchParameters){
        FareMasterPricerTravelBoardSearch.Itinerary idt=new FareMasterPricerTravelBoardSearch.Itinerary();
        OriginAndDestinationRequestType odrt=new OriginAndDestinationRequestType();
        odrt.setSegRef(new BigInteger(Integer.toString(1)));
        idt.setRequestedSegmentRef(odrt);
        DepartureLocationType dlt = new DepartureLocationType();

        MultiCityOptionType mcot = new MultiCityOptionType();
        mcot.setLocationId(searchParameters.getOrigin());
        dlt.getDepMultiCity().add(mcot);
        idt.setDepartureLocalization(dlt);
        ArrivalLocalizationType alt=new ArrivalLocalizationType();
        
        MultiCityOptionType mcot1=new MultiCityOptionType();

        mcot1.setLocationId(searchParameters.getDestination());
        alt.getArrivalMultiCity().add(mcot1);

        idt.setArrivalLocalization(alt);
        DateAndTimeInformationType dti = new DateAndTimeInformationType();
        DateAndTimeDetailsTypeI dtit = new DateAndTimeDetailsTypeI();
        dtit.setTimeQualifier("TD");
        dtit.setTime("0000");
        dtit.setDate(mapDate(searchParameters.getFromDate()));

        if (searchParameters.getDateType()== DateType.ARRIVAL) {
            dtit.setTimeQualifier("TA");
            dtit.setTime("2359");
        }

        dti.setFirstDateTimeDetail(dtit);
        idt.setTimeDetails(dti);

        TravelFlightInformationType141002S fi=new TravelFlightInformationType141002S();
        //ProductTypeDetailsType120801C ptd=new ProductTypeDetailsType120801C();
        //ptd.getFlightType().add("D");
        //fi.setFlightDetail(ptd);
        if(searchParameters.getTransit() != null&&!searchParameters.getDirectFlights()){
            setTransitPoint(searchParameters.getTransit(),fi,idt);
        }

        return idt;
    }

    private void setTransitPoint(String transitPoint,TravelFlightInformationType141002S fi,FareMasterPricerTravelBoardSearch.Itinerary idt){
        ConnectPointDetailsType195492C connectingPoint = new ConnectPointDetailsType195492C();
        connectingPoint.setInclusionIdentifier("M");
        connectingPoint.setLocationId(transitPoint);
        fi.getInclusionDetail().add(connectingPoint);
        idt.setFlightInfo(fi);
    }
}
