/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.compassites.GDSWrapper.amadeus;

import com.amadeus.xml.itareq_05_2_ia.AirSellFromRecommendation;
import com.amadeus.xml.itareq_05_2_ia.AirSellFromRecommendation.ItineraryDetails;
import com.amadeus.xml.itareq_05_2_ia.AirSellFromRecommendation.ItineraryDetails.Message;
import com.amadeus.xml.itareq_05_2_ia.AirSellFromRecommendation.ItineraryDetails.Message.MessageFunctionDetails;
import com.amadeus.xml.itareq_05_2_ia.AirSellFromRecommendation.ItineraryDetails.OriginDestinationDetails;
import com.amadeus.xml.itareq_05_2_ia.AirSellFromRecommendation.ItineraryDetails.SegmentInformation;
import com.amadeus.xml.itareq_05_2_ia.AirSellFromRecommendation.ItineraryDetails.SegmentInformation.RelatedproductInformation;
import com.amadeus.xml.itareq_05_2_ia.AirSellFromRecommendation.ItineraryDetails.SegmentInformation.TravelProductInformation;
import com.amadeus.xml.itareq_05_2_ia.AirSellFromRecommendation.ItineraryDetails.SegmentInformation.TravelProductInformation.*;
import com.amadeus.xml.itareq_05_2_ia.AirSellFromRecommendation.MessageActionDetails;
import com.compassites.model.AirSegmentInformation;
import com.compassites.model.FlightItinerary;
import com.compassites.model.Journey;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.math.BigDecimal;
import java.util.List;

/**
 *
 * @author mahendra-singh
 * @author yaseen
 */
public class BookFlights {

    public AirSellFromRecommendation sellFromRecommendation(FlightItinerary flightItinerary ){
        AirSellFromRecommendation sfr=new AirSellFromRecommendation();
        sfr.setMessageActionDetails(createMessageActionDetails());

        for(Journey journey: flightItinerary.getJourneyList()){
            if(journey.getAirSegmentList().size() > 0){
                sfr.getItineraryDetails().add(createItineraryDetails(journey));
            }
        }

        return sfr;
    }

    public MessageActionDetails createMessageActionDetails(){
        MessageActionDetails mad=new MessageActionDetails();
        com.amadeus.xml.itareq_05_2_ia.AirSellFromRecommendation.MessageActionDetails.MessageFunctionDetails mfd= new MessageActionDetails.MessageFunctionDetails();
        mfd.getAdditionalMessageFunction().add("M1");
        mfd.setMessageFunction("183");
        mad.setMessageFunctionDetails(mfd);
        return mad;
    }

    public ItineraryDetails createItineraryDetails(Journey journey){

        ItineraryDetails itineraryDetails = new ItineraryDetails();
        OriginDestinationDetails originDestinationDetails = new  OriginDestinationDetails();

        List<AirSegmentInformation> airSegmentList = journey.getAirSegmentList();
        originDestinationDetails.setOrigin(airSegmentList.get(0).getFromLocation());
        originDestinationDetails.setDestination(airSegmentList.get(airSegmentList.size() - 1).getToLocation());

        Message message=new Message();
        MessageFunctionDetails mfd=new MessageFunctionDetails();
        mfd.setMessageFunction("183");
        message.setMessageFunctionDetails(mfd);

        itineraryDetails.setMessage(message);
        itineraryDetails.setOriginDestinationDetails(originDestinationDetails);
        for(AirSegmentInformation airSegmentInformation : airSegmentList){
            itineraryDetails.getSegmentInformation().add(createSegmentInformation(airSegmentInformation));
        }

        //id.getSegmentInformation().add(createSegmentInformation1());
        return itineraryDetails;
    }

    public SegmentInformation createSegmentInformation(AirSegmentInformation airSegmentInformation){
        SegmentInformation segmentInformation=new SegmentInformation();
        TravelProductInformation travelProductInformation=new TravelProductInformation();

        FlightDate flightDate=new FlightDate();
        DateTimeFormatter fmt = DateTimeFormat.forPattern("ddMMyy");
        String date = fmt.print(new DateTime(airSegmentInformation.getDepartureDate()));
        //flightDate.setDepartureDate("300614");
        flightDate.setDepartureDate(date);

        BoardPointDetails boardPointDetails=new BoardPointDetails();
        boardPointDetails.setTrueLocationId(airSegmentInformation.getFromLocation());

        OffpointDetails offpointDetails =new OffpointDetails();
        offpointDetails.setTrueLocationId(airSegmentInformation.getToLocation());

        CompanyDetails companyDetails=new CompanyDetails();
        companyDetails.setMarketingCompany(airSegmentInformation.getCarrierCode());

        FlightIdentification flightIdentification=new FlightIdentification();
        flightIdentification.setFlightNumber(airSegmentInformation.getFlightNumber());
        //flightIdentification.setBookingClass("Y");
        flightIdentification.setBookingClass(airSegmentInformation.getBookingClass());

        RelatedproductInformation relatedproductInformation=new RelatedproductInformation();
        relatedproductInformation.getStatusCode().add("NN");
        relatedproductInformation.setQuantity(new BigDecimal("1"));

        travelProductInformation.setBoardPointDetails(boardPointDetails);
        travelProductInformation.setCompanyDetails(companyDetails);
        travelProductInformation.setFlightDate(flightDate);
        travelProductInformation.setFlightIdentification(flightIdentification);
        travelProductInformation.setOffpointDetails(offpointDetails);
        segmentInformation.setTravelProductInformation(travelProductInformation);
        segmentInformation.setRelatedproductInformation(relatedproductInformation);
        return segmentInformation;
    }

    public SegmentInformation createSegmentInformation1(){
        SegmentInformation si=new SegmentInformation();
        TravelProductInformation tpi=new TravelProductInformation();
        FlightDate fd=new FlightDate();
        fd.setDepartureDate("300614");
        BoardPointDetails bpd=new BoardPointDetails();
        bpd.setTrueLocationId("CAN");
        OffpointDetails opd=new OffpointDetails();
        opd.setTrueLocationId("SIN");
        CompanyDetails cd=new CompanyDetails();
        cd.setMarketingCompany("CZ");
        FlightIdentification fi=new FlightIdentification();
        fi.setFlightNumber("351");
        fi.setBookingClass("Y");
        RelatedproductInformation rpi=new RelatedproductInformation();
        rpi.getStatusCode().add("NN");
        rpi.setQuantity(new BigDecimal("1"));
        tpi.setBoardPointDetails(bpd);
        tpi.setCompanyDetails(cd);
        tpi.setFlightDate(fd);
        tpi.setFlightIdentification(fi);
        tpi.setOffpointDetails(opd);
        si.setTravelProductInformation(tpi);
        si.setRelatedproductInformation(rpi);
        return si;
    }
}
