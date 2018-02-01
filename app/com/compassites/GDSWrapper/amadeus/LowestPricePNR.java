
package com.compassites.GDSWrapper.amadeus;

import com.amadeus.xml.pnracc_11_3_1a.PNRReply;
import com.amadeus.xml.tplprq_12_4_1a.*;
import com.compassites.model.AirSegmentInformation;
import com.compassites.model.FareJourney;
import com.compassites.model.FlightItinerary;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Yaseen
 */
public class LowestPricePNR {

    public FarePricePNRWithLowestFare getPNRPricingOption(String carrierCode, PNRReply pnrReply, boolean isSeamen,
                                                          boolean isDomesticFlight, FlightItinerary flightItinerary,
                                                          List<AirSegmentInformation> airSegmentList, boolean isSegmentWisePricing){

        FarePricePNRWithLowestFare pricepnr = null;
        if(isSeamen) {
            pricepnr = new PricePNRLowestFare().getPricePNRWithLowestSeamenFare();
        } else {
            pricepnr = new PricePNRLowestFare().getPricePNRWithLowestNonSeamenFare();
        }

        CodedAttributeType overrideInformation = new CodedAttributeType();
        ReferenceInformationTypeI94606S paxSegReference = new ReferenceInformationTypeI94606S();
        ReferencingDetailsTypeI refDetails = new ReferencingDetailsTypeI();

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
                                refDetails = new ReferencingDetailsTypeI();
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
            int i = 1;
//            for(Journey journey : flightItinerary.getJourneys(isSeamen))  {
            for(AirSegmentInformation airSegment : airSegmentList)  {
                refDetails = new ReferencingDetailsTypeI();
                refDetails.setRefQualifier("S");
                refDetails.setRefNumber(BigInteger.valueOf(i));
                paxSegReference.getRefDetails().add(refDetails);
                i = i + 1;
            }

            pricepnr.setPaxSegReference(paxSegReference);
        }

       List<String> codeAttributes = Arrays.asList("RU","PTC","RW");
        //attributeDetails.setAttributeType("BK");
        //attributeDetails.setAttributeType("NOP");
        //attributeDetails.setAttributeDescription("XN");
        List<CodedAttributeInformationType> codedAttribute = new ArrayList<>();
        if(isSeamen) {
            for(String code : codeAttributes){
                CodedAttributeInformationType codeAttrInfoType = new CodedAttributeInformationType();
                codeAttrInfoType.setAttributeType(code);
                if(code.equals("RW")){
                    codeAttrInfoType.setAttributeDescription("029608");
                }
                codedAttribute.add(codeAttrInfoType);
            }
            overrideInformation.getAttributeDetails().addAll(codedAttribute);
        }else {
            overrideInformation.getAttributeDetails().addAll(addPricingOptions(isDomesticFlight));
        }



        pricepnr.setOverrideInformation(overrideInformation);

        TransportIdentifierType validatingCarrier = new TransportIdentifierType();
        CompanyIdentificationTypeI carrierInformation = new CompanyIdentificationTypeI();

        carrierInformation.setCarrierCode(carrierCode);
        validatingCarrier.setCarrierInformation(carrierInformation);
//        pricepnr.setValidatingCarrier(validatingCarrier);

        if(isDomesticFlight){
            List<FareJourney> fareJourneys = flightItinerary.getPricingInformation(isSeamen).getPaxFareDetailsList().get(0).getFareJourneyList();
            int journeyIndex = 1;
            for(FareJourney fareJourney : fareJourneys){
                FarePricePNRWithLowestFare.PricingFareBase pricingFareBase = new FarePricePNRWithLowestFare.PricingFareBase();
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
                pricingFareBase.setFareBasisOptions(fareBasisOptions);

                ReferenceInformationTypeI94606S fareBasisSegReferenc = new ReferenceInformationTypeI94606S();
                ReferencingDetailsTypeI referencingDetails = new ReferencingDetailsTypeI();
                referencingDetails.setRefNumber(BigInteger.valueOf(journeyIndex));
                referencingDetails.setRefQualifier("S");
                fareBasisSegReferenc.getRefDetails().add(referencingDetails);
                pricingFareBase.setFareBasisSegReference(fareBasisSegReferenc);

                journeyIndex = journeyIndex + 1;
                pricepnr.getPricingFareBase().add(pricingFareBase);
            }
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

        /*codedAttributeInformationType = new CodedAttributeInformationType();
        if (isDomestic) {
            codedAttributeInformationType.setAttributeType("FBA");
        }else {
            codedAttributeInformationType.setAttributeType("RLO");
        }*/
        attributeList.add(codedAttributeInformationType);


        return attributeList;
    }


}
