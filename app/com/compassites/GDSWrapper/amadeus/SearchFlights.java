/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.compassites.GDSWrapper.amadeus;

import com.amadeus.xml.fmptbq_12_4_1a.*;
import com.compassites.model.Passenger;
import com.compassites.model.SearchParameters;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 *
 * @author mahendra-singh
 */
public class SearchFlights {

    static int itineraryRef=1;


    //using deprecated methods
    //change to calendar dates everywhere

    private String mapDate(Date date){
        String amadeusDate="";
        Calendar calDate=Calendar.getInstance();
        calDate.setTime(date);
        String day= String.valueOf(calDate.get(Calendar.DAY_OF_MONTH));
        String month= String.valueOf(calDate.get(Calendar.MONTH)+1);
        String year= String.valueOf(calDate.get(Calendar.YEAR));
        day=day.length()==1?"0"+day:day;
        month=month.length()==1?"0"+month:month;
        year=year.length()==1?"0"+year:year;
        amadeusDate=day+month+year.substring(2);
        return amadeusDate;
    }

    //search flights with 2 cities- faremastertravelboard service
    public FareMasterPricerTravelBoardSearch createSearchQuery(SearchParameters searchParameters){
        FareMasterPricerTravelBoardSearch se=new FareMasterPricerTravelBoardSearch();
        se.setNumberOfUnit(createNumberOfUnits(searchParameters.getPassengers().size()));

        se.getPaxReference().addAll(createPassengers(searchParameters.getPassengers()));
        se.getItinerary().add(createItinerary(searchParameters.getOrigin(),searchParameters.getDestination(),mapDate(searchParameters.getOnwardJourney().getJourneyDate())));
        TravelFlightInformationType148734S travelFlightInfo = new TravelFlightInformationType148734S();

        if (searchParameters.getPreferredAirlineCode() != null){
            CompanyIdentificationType214105C cid =new CompanyIdentificationType214105C();
            cid.getCarrierId().add(searchParameters.getPreferredAirlineCode());
            cid.setCarrierQualifier("X");
            travelFlightInfo.getCompanyIdentity().add(cid);
        }

        if (searchParameters.getDirectFlights()){
            ProductTypeDetailsType120801C ptd=new ProductTypeDetailsType120801C();
            ptd.getFlightType().add("D");
            ptd.getFlightType().add("N");
            travelFlightInfo.setFlightDetail(ptd);
        }

        se.setTravelFlightInfo(travelFlightInfo);

        if (searchParameters.getRefundableFlights()){
            FareMasterPricerTravelBoardSearch.FareOptions fe=new FareMasterPricerTravelBoardSearch.FareOptions();
            PricingTicketingDetailsType pdt=new PricingTicketingDetailsType();
            PricingTicketingInformationType pit=new PricingTicketingInformationType();
            pit.getPriceType().add("RF");
            pdt.setPricingTicketing(pit);
            fe.setPricingTickInfo(pdt);
            se.setFareOptions(fe);
        }


        if(searchParameters.getWithReturnJourney())
            se.getItinerary().add(createItinerary(searchParameters.getDestination(), searchParameters.getOrigin(), mapDate(searchParameters.getReturnJourney().getJourneyDate())));
        //se.setFareOptions(createFareOptions());
//        TravelFlightInformationType148734S tfi=new TravelFlightInformationType148734S();
//        CompanyIdentificationType214105C cid=new CompanyIdentificationType214105C();
//        cid.getCarrierId().add("QR");
//        cid.setCarrierQualifier("M");
//        tfi.getCompanyIdentity().add(cid);
//        se.setTravelFlightInfo(tfi);
        return se;
    }

    private FareMasterPricerTravelBoardSearch.FareOptions createFareOptions(){
        FareMasterPricerTravelBoardSearch.FareOptions fe=new FareMasterPricerTravelBoardSearch.FareOptions();
        PricingTicketingDetailsType pdt=new PricingTicketingDetailsType();
        PricingTicketingInformationType pit=new PricingTicketingInformationType();
        pit.getPriceType().add("RP");
        pit.getPriceType().add("RU");
        pit.getPriceType().add("TAC");
        pit.getPriceType().add("PTC");
        pit.getPriceType().add("ET");
        pit.getPriceType().add("NSD");

        pdt.setPricingTicketing(pit);
        
        CodedAttributeType78503S fid=new CodedAttributeType78503S();
        CodedAttributeInformationType120700C cid=new CodedAttributeInformationType120700C();
        cid.setFeeType("NPS");
        cid.setFeeIdNumber("0");
        fid.getFeeId().add(cid);
        fe.setFeeIdDescription(fid);
        fe.setPricingTickInfo(pdt);
        return fe;
    }

    private NumberOfUnitsType createNumberOfUnits(int noOfPassengers){
        NumberOfUnitsType nu=new NumberOfUnitsType();
        NumberOfUnitDetailsType191580C nudt=new NumberOfUnitDetailsType191580C();
        nudt.setNumberOfUnits(new BigInteger(Integer.toString(noOfPassengers)));
        nudt.setTypeOfUnit("PX");

        NumberOfUnitDetailsType191580C nudt1=new NumberOfUnitDetailsType191580C();
        nudt1.setNumberOfUnits(new BigInteger("200"));
        nudt1.setTypeOfUnit("RC");
        nu.getUnitNumberDetail().add(nudt);
        nu.getUnitNumberDetail().add(nudt1);
        return nu;
    }

    private List<TravellerReferenceInformationType> createPassengers(List<Passenger> listOfPassengers){
        List<TravellerReferenceInformationType> passengers=new ArrayList<TravellerReferenceInformationType>();
        int reference=1;
        for(Passenger passenger:listOfPassengers){
            TravellerReferenceInformationType traveller=new TravellerReferenceInformationType();
            TravellerDetailsType tdt=new TravellerDetailsType();
            tdt.setRef(new BigInteger(Integer.toString(reference++)));
            traveller.getPtc().add(passenger.getPassengerType());
            traveller.getTraveller().add(tdt);
            passengers.add(traveller);
        }
        return passengers;
    }

    private FareMasterPricerTravelBoardSearch.Itinerary createItinerary(String origin,String destination,String date){
        FareMasterPricerTravelBoardSearch.Itinerary idt=new FareMasterPricerTravelBoardSearch.Itinerary();
        OriginAndDestinationRequestType odrt=new OriginAndDestinationRequestType();
        odrt.setSegRef(new BigInteger(Integer.toString(itineraryRef++)));
        idt.setRequestedSegmentRef(odrt);
        DepartureLocationType dlt=new DepartureLocationType();
        
        MultiCityOptionType mcot=new MultiCityOptionType();
        mcot.setLocationId(origin);
        dlt.getDepMultiCity().add(mcot);
        idt.setDepartureLocalization(dlt);        
        ArrivalLocalizationType alt=new ArrivalLocalizationType();
        
        MultiCityOptionType mcot1=new MultiCityOptionType();
        mcot1.setLocationId(destination);
        alt.getArrivalMultiCity().add(mcot1);
        idt.setArrivalLocalization(alt);
        DateAndTimeInformationType dti=new DateAndTimeInformationType();
        DateAndTimeDetailsTypeI dtit=new DateAndTimeDetailsTypeI();
        dtit.setDate(date);
        dti.setFirstDateTimeDetail(dtit);
        
        TravelFlightInformationType141002S fi=new TravelFlightInformationType141002S();
        ProductTypeDetailsType120801C ptd=new ProductTypeDetailsType120801C();
        ptd.getFlightType().add("D");
        fi.setFlightDetail(ptd);
        
        //idt.setFlightInfo(fi);
        idt.setTimeDetails(dti);
        return idt;
    } 
}
