package com.compassites.GDSWrapper.amadeus;

import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import com.amadeus.xml.tipnrq_13_2_1a.*;
import com.amadeus.xml.tipnrq_13_2_1a.FareInformativePricingWithoutPNR.PassengersGroup;
import com.compassites.model.*;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

/**
 * @author Sathish
 */
public class FareInformation13_2 {

    public FareInformativePricingWithoutPNR getPriceInfo(
            List<Journey> journeys, boolean seamen, int adultCount, int childCount, int infantCount, List<PAXFareDetails> paxFareDetailsList) {

        FareInformativePricingWithoutPNR fareInfo = new FareInformativePricingWithoutPNR();

        List<PassengersGroup> passengers = fareInfo.getPassengersGroup();
        if(seamen){
            passengers.add(getPaxGroup(PassengerTypeCode.SEA,adultCount));
            if(childCount > 0){
                passengers.add(getPaxGroup(PassengerTypeCode.SEA,childCount));
            }
            if(infantCount > 0){
                passengers.add(getPaxGroup(PassengerTypeCode.SEA,infantCount));
            }
        }else{
            passengers.add(getPaxGroup(PassengerTypeCode.ADT,adultCount));
            if(childCount > 0){
                passengers.add(getPaxGroup(PassengerTypeCode.CHD,childCount));
            }
            if(infantCount > 0){
                passengers.add(getPaxGroup(PassengerTypeCode.INF,infantCount));
            }

        }

        List<AirSegmentInformation> airSegments = new ArrayList<>();
        List<FareSegment> fareSegments = new ArrayList<>();
        int i = 0 ;
        for (Journey journey : journeys) {
            FareJourney fareJourney = paxFareDetailsList.get(0).getFareJourneyList().get(i);
            int j = 0;
            for (AirSegmentInformation airSegment : journey.getAirSegmentList()) {

                airSegments.add(airSegment);
                fareSegments.add(fareJourney.getFareSegmentList().get(j));
                j++;
            }
            i++;
        }

        i = 0;
        int segmentCounter = 1;
        List<FareInformativePricingWithoutPNR.SegmentGroup> segmentGroups = new ArrayList<>();
        for (AirSegmentInformation airSegment : airSegments) {
            FareInformativePricingWithoutPNR.SegmentGroup segmentGroup = new FareInformativePricingWithoutPNR.SegmentGroup();
            TravelProductInformationTypeI segmentInformation = new TravelProductInformationTypeI();
            ProductDateTimeTypeI flightDate = new ProductDateTimeTypeI();
            LocationTypeI217754C boardPointDetails = new LocationTypeI217754C();
            LocationTypeI217754C offpointDetails = new LocationTypeI217754C();
            CompanyIdentificationTypeI217756C companyDetails = new CompanyIdentificationTypeI217756C();
            ProductIdentificationDetailsTypeI flightIdentification = new ProductIdentificationDetailsTypeI();
            ProductTypeDetailsTypeI flightTypeDetails = new ProductTypeDetailsTypeI();
            /*flight date */
            String departureDateStr = airSegment.getDepartureTime();
            String departureZone = airSegment.getFromAirport().getTime_zone();
            DateTimeZone dateTimeZone  = DateTimeZone.forID(departureZone);
            DateTime departureTime = new DateTime(departureDateStr).withZone(dateTimeZone);

            SimpleDateFormat dateFormat = new SimpleDateFormat("ddMMyy");
            dateFormat.setTimeZone(dateTimeZone.toTimeZone());
            String dateString = dateFormat.format(departureTime.toDate());
            flightDate.setDepartureDate(dateString);
            segmentInformation.setFlightDate(flightDate);

            /*Board point details*/
            boardPointDetails.setTrueLocationId(airSegment.getFromLocation());
            segmentInformation.setBoardPointDetails(boardPointDetails);

            /*Off point details*/
            offpointDetails.setTrueLocationId(airSegment.getToLocation());
            segmentInformation.setOffpointDetails(offpointDetails);

            /*Company details*/
            companyDetails.setMarketingCompany(airSegment.getCarrierCode());
            companyDetails.setOperatingCompany(airSegment.getOperatingCarrierCode());
            segmentInformation.setCompanyDetails(companyDetails);

            /*Flight identification*/
            flightIdentification.setFlightNumber(airSegment.getFlightNumber());
            flightIdentification.setBookingClass(fareSegments.get(i).getBookingClass());
            segmentInformation.setFlightIdentification(flightIdentification);

            /*Flight type details*/
            flightTypeDetails.getFlightIndicator().add("1");
            segmentInformation.setFlightTypeDetails(flightTypeDetails);

            segmentInformation.setItemNumber(BigInteger.valueOf(segmentCounter++));
            segmentGroup.setSegmentInformation(segmentInformation);
            segmentGroups.add(segmentGroup);
            i++;
        }
        fareInfo.getSegmentGroup().addAll(segmentGroups);
        /*
        List<FareInformativePricingWithoutPNR.PricingOptionGroup> pricingOptionGroup = fareInfo.getPricingOptionGroup();
        FareInformativePricingWithoutPNR.PricingOptionGroup pricingOption = new FareInformativePricingWithoutPNR.PricingOptionGroup();
        com.amadeus.xml.tipnrq_13_2_1a.PricingOptionKeyType pricingOptionKey = new com.amadeus.xml.tipnrq_13_2_1a.PricingOptionKeyType();
        pricingOptionKey.setPricingOptionKey("VC");
        pricingOption.setPricingOptionKey(pricingOptionKey);
        TransportIdentifierType transportIdentifier = new TransportIdentifierType();
        CompanyIdentificationTypeI companyIdentification = new CompanyIdentificationTypeI();
        companyIdentification.setOtherCompany(airSegments.get(airSegments.size()-1).getCarrierCode());
        transportIdentifier.setCompanyIdentification(companyIdentification);
        pricingOption.setCarrierInformation(transportIdentifier);
        //pricingOptionGroup.add(pricingOption);

        FareInformativePricingWithoutPNR.PricingOptionGroup pricingOption1 = new FareInformativePricingWithoutPNR.PricingOptionGroup();
        com.amadeus.xml.tipnrq_13_2_1a.PricingOptionKeyType pricingOptionKey1 = new com.amadeus.xml.tipnrq_13_2_1a.PricingOptionKeyType();
        pricingOptionKey1.setPricingOptionKey("RP");
        pricingOption1.setPricingOptionKey(pricingOptionKey1);
        //pricingOptionGroup.add(pricingOption1);

        FareInformativePricingWithoutPNR.PricingOptionGroup pricingOption2 = new FareInformativePricingWithoutPNR.PricingOptionGroup();
        com.amadeus.xml.tipnrq_13_2_1a.PricingOptionKeyType pricingOptionKey2 = new com.amadeus.xml.tipnrq_13_2_1a.PricingOptionKeyType();
        pricingOptionKey2.setPricingOptionKey("RU");
        pricingOption2.setPricingOptionKey(pricingOptionKey2);
        //pricingOptionGroup.add(pricingOption2);

        FareInformativePricingWithoutPNR.PricingOptionGroup pricingOption3 = new FareInformativePricingWithoutPNR.PricingOptionGroup();
        com.amadeus.xml.tipnrq_13_2_1a.PricingOptionKeyType pricingOptionKey3 = new com.amadeus.xml.tipnrq_13_2_1a.PricingOptionKeyType();
        pricingOptionKey3.setPricingOptionKey("RW");
        pricingOption3.setPricingOptionKey(pricingOptionKey3);
        CurrenciesType currenciesType = new CurrenciesType();
        CurrencyDetailsTypeU currencyDetailsTypeU = new CurrencyDetailsTypeU();
        currencyDetailsTypeU.setCurrencyQualifier("FCO");
        currencyDetailsTypeU.setCurrencyIsoCode("INR");
        currenciesType.setFirstCurrencyDetails(currencyDetailsTypeU);
        pricingOption3.setCurrency(currenciesType);
        //pricingOptionGroup.add(pricingOption3);
        */
        return fareInfo;
    }
    private PassengersGroup getPaxGroup(PassengerTypeCode passengerType,
                                              int passengerQuantity) {
        PassengersGroup passengerGroup = new PassengersGroup();

        SegmentRepetitionControlTypeI segmentRepetitionControl = new SegmentRepetitionControlTypeI();
        SegmentRepetitionControlDetailsTypeI segmentRepetitionControlDetails = new SegmentRepetitionControlDetailsTypeI();
        segmentRepetitionControlDetails.setQuantity(BigInteger.valueOf(1));
        segmentRepetitionControlDetails.setNumberOfUnits(BigInteger.valueOf(1));
        segmentRepetitionControl.getSegmentControlDetails().add(
                segmentRepetitionControlDetails);
        passengerGroup.setSegmentRepetitionControl(segmentRepetitionControl);

        SpecificTravellerTypeI specificTraveller = new SpecificTravellerTypeI();
        SpecificTravellerDetailsTypeI specificTravellerDetails = new SpecificTravellerDetailsTypeI();
        specificTravellerDetails.setMeasurementValue(BigInteger.valueOf(passengerQuantity));
        specificTraveller.getTravellerDetails().add(
                specificTravellerDetails);
        passengerGroup.setTravellersID(specificTraveller);

        FareInformationTypeI ptcGroup = new FareInformationTypeI();
        ptcGroup.setValueQualifier(passengerType.name());
        passengerGroup.setDiscountPtc(ptcGroup);

        return passengerGroup;
    }

}

