/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.compassites.GDSWrapper.amadeus;

import com.amadeus.xml.itareq_05_2_ia.AirSellFromRecommendation.ItineraryDetails.Message;
import com.amadeus.xml.itareq_05_2_ia.AirSellFromRecommendation.ItineraryDetails.Message.MessageFunctionDetails;
import com.amadeus.xml.tpcbrq_07_3_1a.FarePricePNRWithBookingClass;
import com.amadeus.xml.tpcbrq_07_3_1a.FarePricePNRWithBookingClass.CurrencyOverride;
import com.amadeus.xml.tpcbrq_07_3_1a.FarePricePNRWithBookingClass.CurrencyOverride.FirstRateDetail;
import com.amadeus.xml.tpcbrq_07_3_1a.FarePricePNRWithBookingClass.OverrideInformation;
import com.amadeus.xml.tpcbrq_07_3_1a.FarePricePNRWithBookingClass.OverrideInformation.AttributeDetails;

/**
 *
 * @author mahendra-singh
 */
public class PricePNR {
    public FarePricePNRWithBookingClass getPNRPricingOption(){
        FarePricePNRWithBookingClass pricepnr=new FarePricePNRWithBookingClass();
        OverrideInformation oi=new OverrideInformation();
        AttributeDetails ad=new AttributeDetails();
        //ad.setAttributeType("BK");
        ad.setAttributeType("NOP");
        //ad.setAttributeDescription("XN");
        oi.getAttributeDetails().add(ad);
        pricepnr.setOverrideInformation(oi);
        return pricepnr;
    }
    
    public void helper(){
        FarePricePNRWithBookingClass pricepnr=new FarePricePNRWithBookingClass();

        CurrencyOverride co=new CurrencyOverride();
        FirstRateDetail frd=new FirstRateDetail();
        frd.setCurrencyCode("INR");
        co.setFirstRateDetail(frd);
        OverrideInformation oi=new OverrideInformation();
        AttributeDetails ad=new AttributeDetails();
        
        ad.setAttributeType("AC");
        
        oi.getAttributeDetails().add(ad);
        pricepnr.setOverrideInformation(oi);
        pricepnr.setCurrencyOverride(co);  
    }   
}
