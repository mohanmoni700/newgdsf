/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.compassites.GDSWrapper.amadeus;

import com.amadeus.xml.tpcbrq_07_3_1a.FarePricePNRWithBookingClass;
import com.amadeus.xml.tpcbrq_07_3_1a.FarePricePNRWithBookingClass.CurrencyOverride;
import com.amadeus.xml.tpcbrq_07_3_1a.FarePricePNRWithBookingClass.CurrencyOverride.FirstRateDetail;
import com.amadeus.xml.tpcbrq_07_3_1a.FarePricePNRWithBookingClass.OverrideInformation;
import com.amadeus.xml.tpcbrq_07_3_1a.FarePricePNRWithBookingClass.OverrideInformation.AttributeDetails;
import com.compassites.model.traveller.TravellerMasterInfo;

import java.math.BigDecimal;

/**
 *
 * @author mahendra-singh
 */
public class PricePNR {
    public FarePricePNRWithBookingClass getPNRPricingOption(TravellerMasterInfo travellerMasterInfo){
        FarePricePNRWithBookingClass pricepnr=new FarePricePNRWithBookingClass();
        OverrideInformation oi=new OverrideInformation();
        FarePricePNRWithBookingClass.PaxSegReference paxSegReference = new FarePricePNRWithBookingClass.PaxSegReference();
        FarePricePNRWithBookingClass.PaxSegReference.RefDetails refDetails = new FarePricePNRWithBookingClass.PaxSegReference.RefDetails();

        refDetails.setRefQualifier("S");
        refDetails.setRefNumber(new BigDecimal(1));
        paxSegReference.getRefDetails().add(refDetails);
        pricepnr.setPaxSegReference(paxSegReference);

        AttributeDetails ad=new AttributeDetails();
        //ad.setAttributeType("BK");
        //ad.setAttributeType("NOP");
        //ad.setAttributeDescription("XN");
        ad.setAttributeType("PTC");
        oi.getAttributeDetails().add(ad);
        pricepnr.setOverrideInformation(oi);

        FarePricePNRWithBookingClass.ValidatingCarrier validatingCarrier = new FarePricePNRWithBookingClass.ValidatingCarrier();
        FarePricePNRWithBookingClass.ValidatingCarrier.CarrierInformation carrierInformation = new FarePricePNRWithBookingClass.ValidatingCarrier.CarrierInformation();

        carrierInformation.setCarrierCode(travellerMasterInfo.getItinerary().getJourneyList().get(0).getAirSegmentList().get(0).getCarrierCode());
        validatingCarrier.setCarrierInformation(carrierInformation);
        pricepnr.setValidatingCarrier(validatingCarrier);
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
