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
import com.amadeus.xml.itareq_05_2_ia.AirSellFromRecommendation.ItineraryDetails.SegmentInformation.TravelProductInformation.BoardPointDetails;
import com.amadeus.xml.itareq_05_2_ia.AirSellFromRecommendation.ItineraryDetails.SegmentInformation.TravelProductInformation.CompanyDetails;
import com.amadeus.xml.itareq_05_2_ia.AirSellFromRecommendation.ItineraryDetails.SegmentInformation.TravelProductInformation.FlightDate;
import com.amadeus.xml.itareq_05_2_ia.AirSellFromRecommendation.ItineraryDetails.SegmentInformation.TravelProductInformation.FlightIdentification;
import com.amadeus.xml.itareq_05_2_ia.AirSellFromRecommendation.ItineraryDetails.SegmentInformation.TravelProductInformation.OffpointDetails;
import com.amadeus.xml.itareq_05_2_ia.AirSellFromRecommendation.MessageActionDetails;
import java.math.BigDecimal;

/**
 *
 * @author mahendra-singh
 */
public class BookFlights {
    public AirSellFromRecommendation sellFromRecommendation(){
        AirSellFromRecommendation sfr=new AirSellFromRecommendation(); 
        sfr.setMessageActionDetails(createMessageActionDetails());
        sfr.getItineraryDetails().add(createItineraryDetails());
        return sfr;
    }
    
    public MessageActionDetails createMessageActionDetails(){
        MessageActionDetails mad=new MessageActionDetails();
        MessageFunctionDetails mfd=new MessageFunctionDetails();
        mfd.getAdditionalMessageFunction().add("M1");
        mfd.setMessageFunction("183");
        return mad;
    }
    
    public ItineraryDetails createItineraryDetails(){
        ItineraryDetails id=new ItineraryDetails();
        OriginDestinationDetails odd=new  OriginDestinationDetails();
        odd.setOrigin("SIN");
        odd.setDestination("MNL");
        Message message=new Message();
        MessageFunctionDetails mfd=new MessageFunctionDetails();
        mfd.setMessageFunction("183");
        message.setMessageFunctionDetails(mfd);        
        
        id.setMessage(message);
        id.setOriginDestinationDetails(odd);         
        id.getSegmentInformation().add(createSegmentInformation());
        //id.getSegmentInformation().add(createSegmentInformation1());
        return id;
    }
    
    public SegmentInformation createSegmentInformation(){
        SegmentInformation si=new SegmentInformation();
        TravelProductInformation tpi=new TravelProductInformation();
        FlightDate fd=new FlightDate();
        fd.setDepartureDate("300614");
        BoardPointDetails bpd=new BoardPointDetails();
        bpd.setTrueLocationId("SIN");
        OffpointDetails opd=new OffpointDetails();
        opd.setTrueLocationId("MNL");
        CompanyDetails cd=new CompanyDetails();
        cd.setMarketingCompany("SQ");
        FlightIdentification fi=new FlightIdentification();
        fi.setFlightNumber("910");
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
