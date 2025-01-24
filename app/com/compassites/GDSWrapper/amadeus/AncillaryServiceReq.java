package com.compassites.GDSWrapper.amadeus;

import com.amadeus.xml.tpicgq_17_1_1a.AttributeInformationTypeU;
import com.amadeus.xml.tpicgq_17_1_1a.AttributeType;
import com.amadeus.xml.tpicgq_17_1_1a.PricingOptionKeyType;
import com.amadeus.xml.tpicgq_17_1_1a.ServiceIntegratedCatalogue;
import com.amadeus.xml.tpscgq_17_1_1a.*;
import com.compassites.model.AirSegmentInformation;
import com.compassites.model.FlightItinerary;
import com.compassites.model.Journey;
import models.AncillaryServiceRequest;

import java.math.BigInteger;
import java.util.*;

public class AncillaryServiceReq {

    public static class AdditionalPaidBaggage {

        //This Created the request body for amadeus for additional baggage information
        public static ServiceIntegratedCatalogue createShowAdditionalBaggageInformationRequest() {

            ServiceIntegratedCatalogue serviceIntegratedCatalogue = new ServiceIntegratedCatalogue();
            List<ServiceIntegratedCatalogue.PricingOption> pricingOption = new ArrayList<>();

            //Requesting only baggage related information here
            ServiceIntegratedCatalogue.PricingOption groupBaggagePricingOption = new ServiceIntegratedCatalogue.PricingOption();

            PricingOptionKeyType groupPricingOptionKey = new PricingOptionKeyType();
            groupPricingOptionKey.setPricingOptionKey("GRP");
            groupBaggagePricingOption.setPricingOptionKey(groupPricingOptionKey);

            AttributeType optionDetail = new AttributeType();
            AttributeInformationTypeU criteriaDetails = new AttributeInformationTypeU();
            criteriaDetails.setAttributeType("BG");
            optionDetail.getCriteriaDetails().add(criteriaDetails);
            groupBaggagePricingOption.setOptionDetail(optionDetail);

            pricingOption.add(groupBaggagePricingOption);


            //MIF - > To identify special airlines
            ServiceIntegratedCatalogue.PricingOption mifPricingOption = new ServiceIntegratedCatalogue.PricingOption();
            PricingOptionKeyType mifPricingOptionKey = new PricingOptionKeyType();
            mifPricingOptionKey.setPricingOptionKey("MIF");
            mifPricingOption.setPricingOptionKey(mifPricingOptionKey);

            pricingOption.add(mifPricingOption);


            //OIS -> Show Only Issuable recommendation
            ServiceIntegratedCatalogue.PricingOption oisPricingOption = new ServiceIntegratedCatalogue.PricingOption();
            PricingOptionKeyType oisPricingOptionKey = new PricingOptionKeyType();
            oisPricingOptionKey.setPricingOptionKey("OIS");
            oisPricingOption.setPricingOptionKey(oisPricingOptionKey);

            pricingOption.add(oisPricingOption);


            //SCD -> Show Commercial Description
            ServiceIntegratedCatalogue.PricingOption scdPricingOption = new ServiceIntegratedCatalogue.PricingOption();
            PricingOptionKeyType scdPricingOptionKey = new PricingOptionKeyType();
            scdPricingOptionKey.setPricingOptionKey("SCD");
            scdPricingOption.setPricingOptionKey(scdPricingOptionKey);

            pricingOption.add(scdPricingOption);

            //BGR
            ServiceIntegratedCatalogue.PricingOption bgrPricingOption = new ServiceIntegratedCatalogue.PricingOption();
            PricingOptionKeyType bgrPricingOptionKey = new PricingOptionKeyType();
            bgrPricingOptionKey.setPricingOptionKey("BGR");
            bgrPricingOption.setPricingOptionKey(bgrPricingOptionKey);

            pricingOption.add(bgrPricingOption);

            serviceIntegratedCatalogue.getPricingOption().addAll(pricingOption);

            return serviceIntegratedCatalogue;
        }

        public static ServiceStandaloneCatalogue createShowAdditionalBaggageInformationRequestStandalone(AncillaryServiceRequest ancillaryServiceRequest) {

            ServiceStandaloneCatalogue serviceStandaloneCatalogue = new ServiceStandaloneCatalogue();

//            List<PassengerInfoType> passengerInfoGroup = new ArrayList<>();

            //Passenger Info Being Set here
            PassengerInfoType passengerInfoType = new PassengerInfoType();
            SpecificTravellerTypeI specificTravellerDetails = new SpecificTravellerTypeI();
            SpecificTravellerDetailsTypeI travellerDetails = new SpecificTravellerDetailsTypeI();

            travellerDetails.setReferenceNumber("1");
            specificTravellerDetails.setTravellerDetails(travellerDetails);
            passengerInfoType.setSpecificTravellerDetails(specificTravellerDetails);

            FareInformationType fareInfo = new FareInformationType();
            fareInfo.setValueQualifier("ADT");
            passengerInfoType.setFareInfo(fareInfo);

            serviceStandaloneCatalogue.getPassengerInfoGroup().add(passengerInfoType);


            //Flight Info Being set here
            List<ServiceStandaloneCatalogue.FlightInfo> flightInfoList = new ArrayList<>();

            FlightItinerary flightItinerary = ancillaryServiceRequest.getFlightItinerary();

            List<Journey> journeyList;
            if (ancillaryServiceRequest.isSeamen()) {
                journeyList = flightItinerary.getJourneyList();
            } else {
                journeyList = flightItinerary.getNonSeamenJourneyList();
            }

            String fareBasis = null;

            int counter = 0;
            for (Journey journey : journeyList) {

                List<AirSegmentInformation> airSegmentList = journey.getAirSegmentList();

                fareBasis = airSegmentList.get(0).getFareBasis();

                for (AirSegmentInformation airSegmentInformation : airSegmentList) {
                    ++counter;
                    ServiceStandaloneCatalogue.FlightInfo flightInfo = new ServiceStandaloneCatalogue.FlightInfo();
                    TravelProductInformationType flightDetails = new TravelProductInformationType();

                    ProductDateTimeType flightDate = new ProductDateTimeType();

                    // Departure date
                    String departureDate = airSegmentInformation.getToDate();
                    if(departureDate.length() > 6){
                        departureDate = departureDate.substring(0,6);
                    }
                    flightDate.setDepartureDate(departureDate);
                    flightDetails.setFlightDate(flightDate);

                    //Origin
                    LocationType boardPointDetails = new LocationType();
                    boardPointDetails.setTrueLocationId(airSegmentInformation.getFromLocation());
                    flightDetails.setBoardPointDetails(boardPointDetails);

                    //Destination
                    LocationType offPointDetails = new LocationType();
                    offPointDetails.setTrueLocationId(airSegmentInformation.getToLocation());
                    flightDetails.setOffpointDetails(offPointDetails);

                    //Operating company --> Marketing Carrier Code
                    CompanyIdentificationType companyDetails = new CompanyIdentificationType();
                    companyDetails.setOperatingCompany(airSegmentInformation.getOperatingCarrierCode());
                    companyDetails.setMarketingCompany(airSegmentInformation.getOperatingCarrierCode());
                    flightDetails.setCompanyDetails(companyDetails);

                    // Flight Number and Booking Class
                    ProductIdentificationDetailsType flightIdentification = new ProductIdentificationDetailsType();
                    flightIdentification.setFlightNumber(airSegmentInformation.getFlightNumber());
                    flightIdentification.setBookingClass(airSegmentInformation.getBookingClass());
                    flightDetails.setFlightIdentification(flightIdentification);

                    //Item / segment sequence number
                    ProductTypeDetailsType219501C flightTypeDetails = new ProductTypeDetailsType219501C();
                    flightTypeDetails.getFlightIndicator().add(String.valueOf(counter));
                    flightDetails.setFlightTypeDetails(flightTypeDetails);
                    flightDetails.setItemNumber(BigInteger.valueOf(counter));

                    flightInfo.setFlightDetails(flightDetails);

                    //Cabin Designator
                    TravelItineraryInformationTypeI travelItineraryInfo = new TravelItineraryInformationTypeI();
                    travelItineraryInfo.setCabinDesignator(airSegmentInformation.getCabinClass());
                    flightInfo.setTravelItineraryInfo(travelItineraryInfo);

                    //Leg Details
//                    AdditionalProductDetailsTypeI additionalFlightInfo = new AdditionalProductDetailsTypeI();
//                    AdditionalProductTypeI legDetails = new AdditionalProductTypeI();
//                    legDetails.setEquipment(airSegmentInformation.getEquipment());
//                    flightInfo.setAdditionalFlightInfo(additionalFlightInfo);

                    flightInfoList.add(flightInfo);
                }
            }

            serviceStandaloneCatalogue.getFlightInfo().addAll(flightInfoList);



            //Pricing option being set here
            List<ServiceStandaloneCatalogue.PricingOption> pricingOption = new ArrayList<>();


            ServiceStandaloneCatalogue.PricingOption grpPricingOption = new ServiceStandaloneCatalogue.PricingOption();

            com.amadeus.xml.tpscgq_17_1_1a.PricingOptionKeyType pricingOptionKey = new com.amadeus.xml.tpscgq_17_1_1a.PricingOptionKeyType();
            pricingOptionKey.setPricingOptionKey("GRP");
            grpPricingOption.setPricingOptionKey(pricingOptionKey);

            com.amadeus.xml.tpscgq_17_1_1a.AttributeType optionDetail = new com.amadeus.xml.tpscgq_17_1_1a.AttributeType();
            com.amadeus.xml.tpscgq_17_1_1a.AttributeInformationTypeU baggageCriteriaDetails = new com.amadeus.xml.tpscgq_17_1_1a.AttributeInformationTypeU();
            baggageCriteriaDetails.setAttributeType("BG");
            optionDetail.getCriteriaDetails().add(baggageCriteriaDetails);

            grpPricingOption.setOptionDetail(optionDetail);

            pricingOption.add(grpPricingOption);

            //MIF - > To identify special airlines
            ServiceStandaloneCatalogue.PricingOption mifPricingOption = new ServiceStandaloneCatalogue.PricingOption();
            com.amadeus.xml.tpscgq_17_1_1a.PricingOptionKeyType mifPricingOptionKey = new com.amadeus.xml.tpscgq_17_1_1a.PricingOptionKeyType();
            mifPricingOptionKey.setPricingOptionKey("MIF");
            mifPricingOption.setPricingOptionKey(mifPricingOptionKey);

            pricingOption.add(mifPricingOption);

            // OIS -> Show Only Issuable recommendation
            ServiceStandaloneCatalogue.PricingOption oisPricingOption = new ServiceStandaloneCatalogue.PricingOption();
            com.amadeus.xml.tpscgq_17_1_1a.PricingOptionKeyType oisPricingOptionKey = new com.amadeus.xml.tpscgq_17_1_1a.PricingOptionKeyType();
            oisPricingOptionKey.setPricingOptionKey("OIS");
            oisPricingOption.setPricingOptionKey(oisPricingOptionKey);

            pricingOption.add(oisPricingOption);

            //SCD -> Show Commercial Description
            ServiceStandaloneCatalogue.PricingOption scdPricingOption = new ServiceStandaloneCatalogue.PricingOption();
            com.amadeus.xml.tpscgq_17_1_1a.PricingOptionKeyType scdPricingOptionKey = new com.amadeus.xml.tpscgq_17_1_1a.PricingOptionKeyType();
            scdPricingOptionKey.setPricingOptionKey("SCD");
            scdPricingOption.setPricingOptionKey(scdPricingOptionKey);

            pricingOption.add(scdPricingOption);

            //BGR -> Baggage Ready is being set here
            ServiceStandaloneCatalogue.PricingOption bgrPricingOption = new ServiceStandaloneCatalogue.PricingOption();
            com.amadeus.xml.tpscgq_17_1_1a.PricingOptionKeyType bgrPricingOptionKey = new com.amadeus.xml.tpscgq_17_1_1a.PricingOptionKeyType();
            bgrPricingOptionKey.setPricingOptionKey("BGR");
            bgrPricingOption.setPricingOptionKey(bgrPricingOptionKey);

            pricingOption.add(bgrPricingOption);


            //FAR -> Fare Information is being set here
            ServiceStandaloneCatalogue.PricingOption farPricingOption = new ServiceStandaloneCatalogue.PricingOption();
            com.amadeus.xml.tpscgq_17_1_1a.PricingOptionKeyType farPricingOptionKey = new com.amadeus.xml.tpscgq_17_1_1a.PricingOptionKeyType();
            farPricingOptionKey.setPricingOptionKey("FAR");

            com.amadeus.xml.tpscgq_17_1_1a.AttributeType farOptionDetail = new com.amadeus.xml.tpscgq_17_1_1a.AttributeType();
            com.amadeus.xml.tpscgq_17_1_1a.AttributeInformationTypeU farCriteriaDetails = new com.amadeus.xml.tpscgq_17_1_1a.AttributeInformationTypeU();
            farCriteriaDetails.setAttributeType("B");
            farCriteriaDetails.setAttributeDescription(fareBasis);
            farOptionDetail.getCriteriaDetails().add(farCriteriaDetails);

            farPricingOption.setPricingOptionKey(farPricingOptionKey);
            farPricingOption.setOptionDetail(farOptionDetail);

            pricingOption.add(farPricingOption);

            serviceStandaloneCatalogue.getPricingOption().addAll(pricingOption);

            return serviceStandaloneCatalogue;
        }

        //This Created the request body for amadeus for meals information
        public static ServiceStandaloneCatalogue createShowMealsInformationRequestStandalone(AncillaryServiceRequest ancillaryServiceRequest) {

            ServiceStandaloneCatalogue serviceStandaloneCatalogue = new ServiceStandaloneCatalogue();

            //Passenger Info Being Set here
            PassengerInfoType passengerInfoType = new PassengerInfoType();
            SpecificTravellerTypeI specificTravellerDetails = new SpecificTravellerTypeI();
            SpecificTravellerDetailsTypeI travellerDetails = new SpecificTravellerDetailsTypeI();

            travellerDetails.setReferenceNumber("1");
            specificTravellerDetails.setTravellerDetails(travellerDetails);
            passengerInfoType.setSpecificTravellerDetails(specificTravellerDetails);

            FareInformationType fareInfo = new FareInformationType();
            fareInfo.setValueQualifier("ADT");
            passengerInfoType.setFareInfo(fareInfo);

            serviceStandaloneCatalogue.getPassengerInfoGroup().add(passengerInfoType);


            //Flight Info Being set here
            List<ServiceStandaloneCatalogue.FlightInfo> flightInfoList = new ArrayList<>();

            FlightItinerary flightItinerary = ancillaryServiceRequest.getFlightItinerary();

            List<Journey> journeyList;
            if (ancillaryServiceRequest.isSeamen()) {
                journeyList = flightItinerary.getJourneyList();
            } else {
                journeyList = flightItinerary.getNonSeamenJourneyList();
            }

            String fareBasis = null;

            int counter = 0;
            for (Journey journey : journeyList) {

                List<AirSegmentInformation> airSegmentList = journey.getAirSegmentList();


                fareBasis = airSegmentList.get(0).getFareBasis();

                for (AirSegmentInformation airSegmentInformation : airSegmentList) {
                    ++counter;
                    ServiceStandaloneCatalogue.FlightInfo flightInfo = new ServiceStandaloneCatalogue.FlightInfo();
                    TravelProductInformationType flightDetails = new TravelProductInformationType();

                    ProductDateTimeType flightDate = new ProductDateTimeType();

                    // Departure date
                    String departureDate = airSegmentInformation.getToDate();
                    if(departureDate.length() > 6){
                        departureDate = departureDate.substring(0,6);
                    }
                    flightDate.setDepartureDate(departureDate);
                    flightDetails.setFlightDate(flightDate);

                    //Origin
                    LocationType boardPointDetails = new LocationType();
                    boardPointDetails.setTrueLocationId(airSegmentInformation.getFromLocation());
                    flightDetails.setBoardPointDetails(boardPointDetails);

                    //Destination
                    LocationType offPointDetails = new LocationType();
                    offPointDetails.setTrueLocationId(airSegmentInformation.getToLocation());
                    flightDetails.setOffpointDetails(offPointDetails);

                    //Operating company --> Marketing Carrier Code
                    CompanyIdentificationType companyDetails = new CompanyIdentificationType();
                    companyDetails.setOperatingCompany(airSegmentInformation.getOperatingCarrierCode());
                    companyDetails.setMarketingCompany(airSegmentInformation.getOperatingCarrierCode());
                    flightDetails.setCompanyDetails(companyDetails);

                    // Flight Number and Booking Class
                    ProductIdentificationDetailsType flightIdentification = new ProductIdentificationDetailsType();
                    flightIdentification.setFlightNumber(airSegmentInformation.getFlightNumber());
                    flightIdentification.setBookingClass(airSegmentInformation.getBookingClass());
                    flightDetails.setFlightIdentification(flightIdentification);

                    //Item / segment sequence number
                    ProductTypeDetailsType219501C flightTypeDetails = new ProductTypeDetailsType219501C();
                    flightTypeDetails.getFlightIndicator().add(String.valueOf(counter));
                    flightDetails.setFlightTypeDetails(flightTypeDetails);
                    flightDetails.setItemNumber(BigInteger.valueOf(counter));

                    flightInfo.setFlightDetails(flightDetails);

                    //Cabin Designator
                    TravelItineraryInformationTypeI travelItineraryInfo = new TravelItineraryInformationTypeI();
                    travelItineraryInfo.setCabinDesignator(airSegmentInformation.getCabinClass());
                    flightInfo.setTravelItineraryInfo(travelItineraryInfo);

                    //Leg Details
//                    AdditionalProductDetailsTypeI additionalFlightInfo = new AdditionalProductDetailsTypeI();
//                    AdditionalProductTypeI legDetails = new AdditionalProductTypeI();
//                    legDetails.setEquipment(airSegmentInformation.getEquipment());
//                    flightInfo.setAdditionalFlightInfo(additionalFlightInfo);

                    flightInfoList.add(flightInfo);
                }
            }

            serviceStandaloneCatalogue.getFlightInfo().addAll(flightInfoList);


            //Pricing option being set here
            List<ServiceStandaloneCatalogue.PricingOption> pricingOption = new ArrayList<>();


            ServiceStandaloneCatalogue.PricingOption grpPricingOption = new ServiceStandaloneCatalogue.PricingOption();

            com.amadeus.xml.tpscgq_17_1_1a.PricingOptionKeyType pricingOptionKey = new com.amadeus.xml.tpscgq_17_1_1a.PricingOptionKeyType();
            pricingOptionKey.setPricingOptionKey("GRP");
            grpPricingOption.setPricingOptionKey(pricingOptionKey);

            com.amadeus.xml.tpscgq_17_1_1a.AttributeType optionDetail = new com.amadeus.xml.tpscgq_17_1_1a.AttributeType();
            com.amadeus.xml.tpscgq_17_1_1a.AttributeInformationTypeU baggageCriteriaDetails = new com.amadeus.xml.tpscgq_17_1_1a.AttributeInformationTypeU();
            baggageCriteriaDetails.setAttributeType("ML");
            optionDetail.getCriteriaDetails().add(baggageCriteriaDetails);

            grpPricingOption.setOptionDetail(optionDetail);

            pricingOption.add(grpPricingOption);

            //MIF - > To identify special airlines
            ServiceStandaloneCatalogue.PricingOption mifPricingOption = new ServiceStandaloneCatalogue.PricingOption();
            com.amadeus.xml.tpscgq_17_1_1a.PricingOptionKeyType mifPricingOptionKey = new com.amadeus.xml.tpscgq_17_1_1a.PricingOptionKeyType();
            mifPricingOptionKey.setPricingOptionKey("MIF");
            mifPricingOption.setPricingOptionKey(mifPricingOptionKey);

            pricingOption.add(mifPricingOption);

            // OIS -> Show Only Issuable recommendation
            ServiceStandaloneCatalogue.PricingOption oisPricingOption = new ServiceStandaloneCatalogue.PricingOption();
            com.amadeus.xml.tpscgq_17_1_1a.PricingOptionKeyType oisPricingOptionKey = new com.amadeus.xml.tpscgq_17_1_1a.PricingOptionKeyType();
            oisPricingOptionKey.setPricingOptionKey("OIS");
            oisPricingOption.setPricingOptionKey(oisPricingOptionKey);

            pricingOption.add(oisPricingOption);

            //SCD -> Show Commercial Description
            ServiceStandaloneCatalogue.PricingOption scdPricingOption = new ServiceStandaloneCatalogue.PricingOption();
            com.amadeus.xml.tpscgq_17_1_1a.PricingOptionKeyType scdPricingOptionKey = new com.amadeus.xml.tpscgq_17_1_1a.PricingOptionKeyType();
            scdPricingOptionKey.setPricingOptionKey("SCD");
            scdPricingOption.setPricingOptionKey(scdPricingOptionKey);

            pricingOption.add(scdPricingOption);

            //FAR -> Fare Information is being set here
            ServiceStandaloneCatalogue.PricingOption farPricingOption = new ServiceStandaloneCatalogue.PricingOption();
            com.amadeus.xml.tpscgq_17_1_1a.PricingOptionKeyType farPricingOptionKey = new com.amadeus.xml.tpscgq_17_1_1a.PricingOptionKeyType();
            farPricingOptionKey.setPricingOptionKey("FAR");

            com.amadeus.xml.tpscgq_17_1_1a.AttributeType farOptionDetail = new com.amadeus.xml.tpscgq_17_1_1a.AttributeType();
            com.amadeus.xml.tpscgq_17_1_1a.AttributeInformationTypeU farCriteriaDetails = new com.amadeus.xml.tpscgq_17_1_1a.AttributeInformationTypeU();
            farCriteriaDetails.setAttributeType("B");
            farCriteriaDetails.setAttributeDescription(fareBasis);
            farOptionDetail.getCriteriaDetails().add(farCriteriaDetails);

            farPricingOption.setPricingOptionKey(farPricingOptionKey);
            farPricingOption.setOptionDetail(farOptionDetail);

            pricingOption.add(farPricingOption);

            serviceStandaloneCatalogue.getPricingOption().addAll(pricingOption);

            return serviceStandaloneCatalogue;

        }


    }

}
