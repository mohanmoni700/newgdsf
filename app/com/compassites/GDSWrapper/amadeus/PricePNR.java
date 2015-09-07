/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.compassites.GDSWrapper.amadeus;

import com.amadeus.xml.pnracc_11_3_1a.PNRReply;
import com.amadeus.xml.tpcbrq_12_4_1a.*;
import com.compassites.model.Journey;

import java.util.List;

/**
 *
 * @author mahendra-singh
 */
public class PricePNR {
    public FarePricePNRWithBookingClass getPNRPricingOption(String carrierCode, PNRReply pnrReply){
        FarePricePNRWithBookingClass pricepnr=new FarePricePNRWithBookingClass();
        CodedAttributeType overrideInformation = new CodedAttributeType();
        ReferenceInformationTypeI94605S paxSegReference = new ReferenceInformationTypeI94605S();
        ReferencingDetailsTypeI142222C refDetails = new ReferencingDetailsTypeI142222C();


        for(PNRReply.OriginDestinationDetails originatorDetails : pnrReply.getOriginDestinationDetails())  {
            for(PNRReply.OriginDestinationDetails.ItineraryInfo itineraryInfo : originatorDetails.getItineraryInfo()){
                refDetails = new ReferencingDetailsTypeI142222C();
                refDetails.setRefQualifier(itineraryInfo.getElementManagementItinerary().getReference().getQualifier());
                refDetails.setRefNumber(itineraryInfo.getElementManagementItinerary().getReference().getNumber());
                paxSegReference.getRefDetails().add(refDetails);

            }
        }
       /* refDetails.setRefQualifier("S");
        refDetails.setRefNumber(new BigDecimal(1));
        paxSegReference.getRefDetails().add(refDetails);
        refDetails = new FarePricePNRWithBookingClass.PaxSegReference.RefDetails();
        refDetails.setRefQualifier("S");
        refDetails.setRefNumber(new BigDecimal(2));
        paxSegReference.getRefDetails().add(refDetails);*/
        pricepnr.setPaxSegReference(paxSegReference);

        CodedAttributeInformationType attributeDetails=new CodedAttributeInformationType();
        //attributeDetails.setAttributeType("BK");
        //attributeDetails.setAttributeType("NOP");
        //attributeDetails.setAttributeDescription("XN");
        attributeDetails.setAttributeType("ptc");
        overrideInformation.getAttributeDetails().add(attributeDetails);
        pricepnr.setOverrideInformation(overrideInformation);

        TransportIdentifierType validatingCarrier = new TransportIdentifierType();
        CompanyIdentificationTypeI carrierInformation = new CompanyIdentificationTypeI();
        List<Journey> journeyList = null;

        carrierInformation.setCarrierCode(carrierCode);
        validatingCarrier.setCarrierInformation(carrierInformation);
        pricepnr.setValidatingCarrier(validatingCarrier);
        return pricepnr;
    }
    
    public void helper(){
        FarePricePNRWithBookingClass pricepnr=new FarePricePNRWithBookingClass();

        ConversionRateTypeI currencyOverride = new ConversionRateTypeI();
        ConversionRateDetailsTypeI firstRateDetail=new ConversionRateDetailsTypeI();
        firstRateDetail.setCurrencyCode("INR");
        currencyOverride.setFirstRateDetail(firstRateDetail);
        CodedAttributeType overrideInformation=new CodedAttributeType();
        CodedAttributeInformationType attributeDetails=new CodedAttributeInformationType();

        attributeDetails.setAttributeType("AC");

        overrideInformation.getAttributeDetails().add(attributeDetails);
        pricepnr.setOverrideInformation(overrideInformation);
        pricepnr.setCurrencyOverride(currencyOverride);
    }

    public FarePricePNRWithBookingClass lpf(){
        FarePricePNRWithBookingClass pricepnr = new FarePricePNRWithBookingClass();
        CodedAttributeType overrideInformation = new CodedAttributeType();
        CodedAttributeInformationType attributeDetails = new CodedAttributeInformationType();
        attributeDetails.setAttributeType("RLO");
        overrideInformation.getAttributeDetails().add(attributeDetails);
        pricepnr.setOverrideInformation(overrideInformation);

        return  pricepnr;

    }
}
