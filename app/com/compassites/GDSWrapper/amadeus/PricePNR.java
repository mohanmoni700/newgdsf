/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.compassites.GDSWrapper.amadeus;

import com.amadeus.xml.pnracc_14_1_1a.PNRReply;
import com.amadeus.xml.tpcbrq_12_4_1a.*;
import com.compassites.model.AirSegmentInformation;
import com.compassites.model.FareJourney;
import com.compassites.model.FlightItinerary;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author mahendra-singh
 */
public class PricePNR {
    public FarePricePNRWithBookingClass getPNRPricingOption(String carrierCode, PNRReply pnrReply, boolean isSeamen,
                                                            boolean isDomesticFlight, FlightItinerary flightItinerary,
                                                            List<AirSegmentInformation> airSegmentList, boolean isSegmentWisePricing, boolean isAddBooking, boolean isSplitTicket, int journeyIndex){

        FarePricePNRWithBookingClass pricepnr=new FarePricePNRWithBookingClass();
        CodedAttributeType overrideInformation = new CodedAttributeType();
        ReferenceInformationTypeI94605S paxSegReference = new ReferenceInformationTypeI94605S();
        ReferencingDetailsTypeI142222C refDetails = new ReferencingDetailsTypeI142222C();
        String airlineStr = play.Play.application().configuration().getString("vistara.airline.code");
        if(isSplitTicket) {
            isSeamen = flightItinerary.getJourneyList().get(journeyIndex).isSeamen();
            System.out.println("isSeamen "+isSeamen);
        }
        if(isSegmentWisePricing){
            for(AirSegmentInformation airSegment : airSegmentList)  {
                String key = airSegment.getFromLocation() + airSegment.getToLocation();
                for(PNRReply.OriginDestinationDetails originDestinationDetails : pnrReply.getOriginDestinationDetails()) {
                    for (PNRReply.OriginDestinationDetails.ItineraryInfo itineraryInfo : originDestinationDetails.getItineraryInfo()) {
                        String segType = itineraryInfo.getElementManagementItinerary().getSegmentName();
                        if(segType.equalsIgnoreCase("AIR")) {
                            String segments = itineraryInfo.getTravelProduct().getBoardpointDetail().getCityCode()
                                    + itineraryInfo.getTravelProduct().getOffpointDetail().getCityCode();
                            //TODO for multicity the starting and ending segments may be same
                            if (segments.equals(key)) {
                                refDetails = new ReferencingDetailsTypeI142222C();
                                refDetails.setRefQualifier("S");
                                refDetails.setRefNumber(itineraryInfo.getElementManagementItinerary().getReference().getNumber());
                                paxSegReference.getRefDetails().add(refDetails);
                            }
                        }
                    }
                }
            }
            pricepnr.setPaxSegReference(paxSegReference);
        }


        if(isDomesticFlight && !isSegmentWisePricing){
            //int i = 1;
//            for(Journey journey : flightItinerary.getJourneys(isSeamen))  {
            for(AirSegmentInformation airSegment : airSegmentList)  {
                /*refDetails = new ReferencingDetailsTypeI142222C();
                refDetails.setRefQualifier("S");
                refDetails.setRefNumber(BigInteger.valueOf(i));
                paxSegReference.getRefDetails().add(refDetails);
                i = i + 1;*/
                String key = airSegment.getFromLocation() + airSegment.getToLocation();
                StringBuilder stringBuilder = new StringBuilder();
                for(PNRReply.OriginDestinationDetails originDestinationDetails : pnrReply.getOriginDestinationDetails()) {
                    for (PNRReply.OriginDestinationDetails.ItineraryInfo itineraryInfo : originDestinationDetails.getItineraryInfo()) {
                        String segType = itineraryInfo.getElementManagementItinerary().getSegmentName();
                        if(segType.equalsIgnoreCase("AIR")) {
                            String segments = itineraryInfo.getTravelProduct().getBoardpointDetail().getCityCode()
                                    + itineraryInfo.getTravelProduct().getOffpointDetail().getCityCode();
                            if (segments.equals(key)) {
                                refDetails = new ReferencingDetailsTypeI142222C();
                                refDetails.setRefQualifier("S");
                                refDetails.setRefNumber(itineraryInfo.getElementManagementItinerary().getReference().getNumber());
                                paxSegReference.getRefDetails().add(refDetails);
                            }
                        }
                    }
                }
            }

            pricepnr.setPaxSegReference(paxSegReference);
        }

        if(isAddBooking && !isDomesticFlight && !isSegmentWisePricing) {
            for(AirSegmentInformation airSegment : airSegmentList)  {
                String key = airSegment.getFromLocation() + airSegment.getToLocation();
                for(PNRReply.OriginDestinationDetails originDestinationDetails : pnrReply.getOriginDestinationDetails()) {
                    for (PNRReply.OriginDestinationDetails.ItineraryInfo itineraryInfo : originDestinationDetails.getItineraryInfo()) {
                        String segType = itineraryInfo.getElementManagementItinerary().getSegmentName();
                        if(segType.equalsIgnoreCase("AIR")) {
                            String segments = itineraryInfo.getTravelProduct().getBoardpointDetail().getCityCode()
                                    + itineraryInfo.getTravelProduct().getOffpointDetail().getCityCode();
                            if (segments.equals(key)) {
                                refDetails = new ReferencingDetailsTypeI142222C();
                                refDetails.setRefQualifier("S");
                                refDetails.setRefNumber(itineraryInfo.getElementManagementItinerary().getReference().getNumber());
                                paxSegReference.getRefDetails().add(refDetails);
                            }
                        }
                    }
                }
            }

            pricepnr.setPaxSegReference(paxSegReference);
        }

        for(AirSegmentInformation airSegment : airSegmentList)  {
            String key = airSegment.getFromLocation() + airSegment.getToLocation();
            for(PNRReply.OriginDestinationDetails originDestinationDetails : pnrReply.getOriginDestinationDetails()) {
                for (PNRReply.OriginDestinationDetails.ItineraryInfo itineraryInfo : originDestinationDetails.getItineraryInfo()) {
                    String segType = itineraryInfo.getElementManagementItinerary().getSegmentName();
                    if(segType.equalsIgnoreCase("AIR")) {
                        String segments = itineraryInfo.getTravelProduct().getBoardpointDetail().getCityCode()
                                + itineraryInfo.getTravelProduct().getOffpointDetail().getCityCode();
                        if (segments.equals(key)) {
                            refDetails = new ReferencingDetailsTypeI142222C();
                            refDetails.setRefQualifier("S");
                            refDetails.setRefNumber(itineraryInfo.getElementManagementItinerary().getReference().getNumber());
                            paxSegReference.getRefDetails().add(refDetails);
                        }
                    }
                }
            }
        }

        pricepnr.setPaxSegReference(paxSegReference);


        CodedAttributeInformationType attributeDetails=new CodedAttributeInformationType();
        List<CodedAttributeInformationType> attributeList = new ArrayList<>();
        //attributeDetails.setAttributeType("BK");
        //attributeDetails.setAttributeType("NOP");
        //attributeDetails.setAttributeDescription("XN");
        if(isSeamen) {
            if(carrierCode!=null && !carrierCode.equalsIgnoreCase(airlineStr)) {
                attributeDetails.setAttributeType("ptc");
                //overrideInformation.getAttributeDetails().add(attributeDetails);
                attributeList.add(attributeDetails);
            }
            attributeDetails=new CodedAttributeInformationType();
            attributeDetails.setAttributeType("RP");
            attributeList.add(attributeDetails);
            attributeDetails=new CodedAttributeInformationType();
            attributeDetails.setAttributeType("RU");
            attributeList.add(attributeDetails);

            if(carrierCode!=null && carrierCode.equalsIgnoreCase(airlineStr)) {
                attributeDetails=new CodedAttributeInformationType();
                attributeDetails.setAttributeType("RW");
                attributeDetails.setAttributeDescription("029608");
                attributeList.add(attributeDetails);

            } else {
                attributeDetails=new CodedAttributeInformationType();
                attributeDetails.setAttributeType("RLO");
                attributeList.add(attributeDetails);
            }
            overrideInformation.getAttributeDetails().addAll(attributeList);


        }else {
            overrideInformation.getAttributeDetails().addAll(addPricingOptions(isDomesticFlight));
        }



        pricepnr.setOverrideInformation(overrideInformation);

        TransportIdentifierType validatingCarrier = new TransportIdentifierType();
        CompanyIdentificationTypeI carrierInformation = new CompanyIdentificationTypeI();

        carrierInformation.setCarrierCode(carrierCode);
        validatingCarrier.setCarrierInformation(carrierInformation);
        pricepnr.setValidatingCarrier(validatingCarrier);

        if(isDomesticFlight && flightItinerary.getPricingInformation(isSeamen).getPaxFareDetailsList() != null && !flightItinerary.getPricingInformation(isSeamen).getPaxFareDetailsList().isEmpty()){
            List<FareJourney> fareJourneys = flightItinerary.getPricingInformation(isSeamen).getPaxFareDetailsList().get(0).getFareJourneyList();
            for(FareJourney fareJourney : fareJourneys){
                FarePricePNRWithBookingClass.PricingFareBase pricingFareBase = new FarePricePNRWithBookingClass.PricingFareBase();
                FareQualifierDetailsTypeI fareBasisOptions = new FareQualifierDetailsTypeI();
                AdditionalFareQualifierDetailsTypeI fareBasisDetails = new AdditionalFareQualifierDetailsTypeI();
                String fareBasis = fareJourney.getFareSegmentList().get(0).getFareBasis();
                String primaryCode = fareBasis.substring(0, 3);
                fareBasisDetails.setPrimaryCode(primaryCode);
                if(fareBasis.length() > 3){
                    String basisCode = fareBasis.substring(3);
                    fareBasisDetails.setFareBasisCode(basisCode);
                }
                fareBasisOptions.setFareBasisDetails(fareBasisDetails);
                /*pricingFareBase.setFareBasisOptions(fareBasisOptions);

                ReferenceInformationTypeI94606S fareBasisSegReferenc = new ReferenceInformationTypeI94606S();
                ReferencingDetailsTypeI142223C referencingDetails = new ReferencingDetailsTypeI142223C();
                referencingDetails.setRefNumber(BigInteger.valueOf(journeyIndex));
                referencingDetails.setRefQualifier("S");
                fareBasisSegReferenc.getRefDetails().add(referencingDetails);
                pricingFareBase.setFareBasisSegReference(fareBasisSegReferenc);

                journeyIndex = journeyIndex + 1;
                pricepnr.getPricingFareBase().add(pricingFareBase);*/
            }
        }
        if(isSeamen) {
            FarePricePNRWithBookingClass.DiscountInformation discountInfo = new FarePricePNRWithBookingClass.DiscountInformation();
            DiscountAndPenaltyInformationTypeI penDisInfo = new DiscountAndPenaltyInformationTypeI();
            penDisInfo.setInfoQualifier("701");
            DiscountPenaltyMonetaryInformationTypeI penDisData = new DiscountPenaltyMonetaryInformationTypeI();
            if(carrierCode.equalsIgnoreCase("UK")) {
                penDisData.setDiscountCode("SC");
            } else {
                penDisData.setDiscountCode("SEA");
            }
            penDisInfo.getPenDisData().add(penDisData);
            discountInfo.setPenDisInformation(penDisInfo);
            pricepnr.getDiscountInformation().add(discountInfo);
        }

        return pricepnr;
    }


    public List<CodedAttributeInformationType> addPricingOptions(boolean isDomestic){
        List<CodedAttributeInformationType> attributeList = new ArrayList<>();
        CodedAttributeInformationType codedAttributeInformationType = new CodedAttributeInformationType();
        codedAttributeInformationType.setAttributeType("RP");
        attributeList.add(codedAttributeInformationType);
        codedAttributeInformationType = new CodedAttributeInformationType();
        codedAttributeInformationType.setAttributeType("RU");
        attributeList.add(codedAttributeInformationType);

        codedAttributeInformationType = new CodedAttributeInformationType();
        if (isDomestic) {
            codedAttributeInformationType.setAttributeType("FBA");
        }else {
            codedAttributeInformationType.setAttributeType("RLO");
        }
        attributeList.add(codedAttributeInformationType);


        return attributeList;
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
