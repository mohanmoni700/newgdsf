package com.compassites.GDSWrapper.amadeus;

import com.amadeus.xml.fatceq_13_1_1a.*;
import com.amadeus.xml.fatceq_13_1_1a.NumberOfUnitDetailsTypeI;
import com.amadeus.xml.fatceq_13_1_1a.NumberOfUnitsType;
import com.amadeus.xml.fatceq_13_1_1a.PricingTicketingDetailsType;
import com.amadeus.xml.fatceq_13_1_1a.PricingTicketingInformationType;
import com.amadeus.xml.fatceq_13_1_1a.TicketNumberDetailsTypeI;
import com.amadeus.xml.fatceq_13_1_1a.TicketNumberTypeI;
import com.amadeus.xml.fatceq_13_1_1a.TravellerDetailsType;
import com.amadeus.xml.fatceq_13_1_1a.TravellerReferenceInformationType;
import com.amadeus.xml.fmtctq_18_2_1a.*;
import com.amadeus.xml.fmtctq_18_2_1a.CorporateIdentificationType;
import com.amadeus.xml.fmtctq_18_2_1a.CorporateIdentityType;
import com.amadeus.xml.tatreq_20_1_1a.MessageActionDetailsType;
import com.amadeus.xml.tatreq_20_1_1a.MessageFunctionBusinessDetailsType;
import com.amadeus.xml.tatreq_20_1_1a.TicketProcessEDoc;
import com.compassites.model.*;
import dto.reissue.ReIssueSearchParameters;
import dto.reissue.ReIssueTicketRequest;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.CorporateCodeHelper;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static com.compassites.model.BookingType.NON_MARINE;
import static com.compassites.model.BookingType.SEAMEN;


//All request Bodies for Reissue flows are created here
public class ReIssueTicket {

    private static final Logger logger = LoggerFactory.getLogger("gds");

    static String vistaraAirlineStr = play.Play.application().configuration().getString("vistara.airline.code");


    /**
     * This inner class is responsible for creating a ReIssueCheckStatus request.
     */
    public static class ReIssueCheckTicketStatus {

        /**
         * Creates a TicketProcessEDoc for checking the status of a reissued ticket.
         *
         * @param reIssueTicketRequest The object containing details for the ticket to be reissued.
         * @return A TicketProcessEDoc object configured for the reissue ticket status check.
         */
        public static TicketProcessEDoc createReissueTicketStatusCheck(ReIssueTicketRequest reIssueTicketRequest) {

            TicketProcessEDoc ticketProcessEDoc = new TicketProcessEDoc();

            try {

                //Setting message function to 131 as we are using Display an ETKT instead of EMD
                MessageActionDetailsType msgActionDetails = new MessageActionDetailsType();
                MessageFunctionBusinessDetailsType messageFunctionDetails = new MessageFunctionBusinessDetailsType();

                messageFunctionDetails.setMessageFunction("131");
                msgActionDetails.setMessageFunctionDetails(messageFunctionDetails);
                ticketProcessEDoc.setMsgActionDetails(msgActionDetails);

                //Setting ticket numbers here
                List<TicketProcessEDoc.InfoGroup> infoGroup = getInfoGroups(reIssueTicketRequest);
                ticketProcessEDoc.getInfoGroup().addAll(infoGroup);
            } catch (Exception e) {
                logger.debug("Error Creating Reissue - TicketStatusCheck - msgActionDetails request \n{} ", e.getMessage(), e);
            }

            return ticketProcessEDoc;
        }

        private static List<TicketProcessEDoc.InfoGroup> getInfoGroups(ReIssueTicketRequest reIssueTicketRequest) {
            List<TicketProcessEDoc.InfoGroup> infoGroup = new ArrayList<>();

            try {
                for (Passenger passenger : reIssueTicketRequest.getPassengers()) {

                    TicketProcessEDoc.InfoGroup infGroup = new TicketProcessEDoc.InfoGroup();
                    com.amadeus.xml.tatreq_20_1_1a.TicketNumberTypeI docInfo = new com.amadeus.xml.tatreq_20_1_1a.TicketNumberTypeI();
                    com.amadeus.xml.tatreq_20_1_1a.TicketNumberDetailsTypeI documentDetails = new com.amadeus.xml.tatreq_20_1_1a.TicketNumberDetailsTypeI();

                    documentDetails.setNumber(passenger.getTicketNumber());
                    docInfo.setDocumentDetails(documentDetails);
                    infGroup.setDocInfo(docInfo);

                    infoGroup.add(infGroup);
                }
            } catch (Exception e) {
                logger.debug("Error Creating Reissue - TicketStatusCheck - infoGroup request \n{} ", e.getMessage(), e);
            }
            return infoGroup;
        }
    }


    //This inner class for creating ReIssueCheckEligibility request
    public static class ReIssueCheckEligibility {

        public static TicketCheckEligibility createCheckEligibilityRequest(ReIssueTicketRequest reIssueTicketRequest, String searchOfficeId) {
            TicketCheckEligibility checkEligibility = new TicketCheckEligibility();

            //Creating Number of units here
            try {
                createNumberOfUnitsForEligibilityCheck(checkEligibility, reIssueTicketRequest);
            } catch (Exception e) {
                logger.debug("Error Creating Reissue - TicketCheckEligibility - NumberOfUnits request \n{} ", e.getMessage(), e);
            }

            //Creating Pax reference and ticketChangeInfo here.
            try {
                createPaxReferenceForEligibilityCheck(checkEligibility, reIssueTicketRequest);
            } catch (Exception e) {
                logger.debug("Error Creating Reissue - TicketCheckEligibility - PaxReference request \n{} ", e.getMessage(), e);
            }

            //Creating the maximum number of allowed carriers to be returned here.
            try {
                createTravelFlightInfo(checkEligibility);
            } catch (Exception e) {
                logger.debug("Error Creating Reissue - TicketCheckEligibility - TravelFlight request \n{} ", e.getMessage(), e);
            }

            //Creating fareOptions here
            try {
                createFareOptionsForEligibilityCheck(checkEligibility, reIssueTicketRequest, searchOfficeId);
            } catch (Exception e) {
                logger.debug("Error Creating Reissue - TicketCheckEligibility - FareOptions request \n{} ", e.getMessage(), e);
            }

            return checkEligibility;
        }


        private static void createNumberOfUnitsForEligibilityCheck(TicketCheckEligibility ticketCheckEligibility, ReIssueTicketRequest reIssueTicketRequest) {

            NumberOfUnitsType numberOfUnitsType = new NumberOfUnitsType();
            NumberOfUnitDetailsType191580C numberOfUnitDetailsType191580C = new NumberOfUnitDetailsType191580C();
            numberOfUnitDetailsType191580C.setTypeOfUnit("PX");
            if (reIssueTicketRequest.isSeaman()) {
                numberOfUnitDetailsType191580C.setNumberOfUnits(new BigInteger(String.valueOf(reIssueTicketRequest.getAdultCount() + reIssueTicketRequest.getChildCount() + reIssueTicketRequest.getInfantCount())));
            } else {
                numberOfUnitDetailsType191580C.setNumberOfUnits(new BigInteger(String.valueOf(reIssueTicketRequest.getAdultCount() + reIssueTicketRequest.getChildCount())));
            }
            numberOfUnitsType.getUnitNumberDetail().add(numberOfUnitDetailsType191580C);
            ticketCheckEligibility.setNumberOfUnit(numberOfUnitsType);

        }

        private static void createTravelFlightInfo(TicketCheckEligibility ticketCheckEligibility) {

            TravelFlightInformationType165052S travelFlightInfo = new TravelFlightInformationType165052S();
            List<NumberOfUnitDetailsTypeI> unitNumberDetail = new ArrayList<>();

            NumberOfUnitDetailsTypeI numberOfUnitDetailsTypeI = new NumberOfUnitDetailsTypeI();
            numberOfUnitDetailsTypeI.setNumberOfUnits(new BigInteger("20"));
            numberOfUnitDetailsTypeI.setTypeOfUnit("F");

            unitNumberDetail.add(numberOfUnitDetailsTypeI);
            travelFlightInfo.getUnitNumberDetail().addAll(unitNumberDetail);

            ticketCheckEligibility.setTravelFlightInfo(travelFlightInfo);

        }

        private static void createPaxReferenceForEligibilityCheck(TicketCheckEligibility ticketCheckEligibility, ReIssueTicketRequest reIssueTicketRequest) {

            List<TravellerReferenceInformationType> adtPassenger = new ArrayList<>();
            List<TravellerReferenceInformationType> chdPassenger = new ArrayList<>();
            List<TravellerReferenceInformationType> infPassenger = new ArrayList<>();
            List<TravellerReferenceInformationType> seamanPassenger = new ArrayList<>();

            List<TravellerDetailsType> adtTravellerDetailsTypeList = new ArrayList<>();
            List<TravellerDetailsType> chdTravellerDetailsTypeList = new ArrayList<>();
            List<TravellerDetailsType> infTravellerDetailsTypeList = new ArrayList<>();
            List<TravellerDetailsType> seamenTravellerDetailsTypeList = new ArrayList<>();

            TicketCheckEligibility.TicketChangeInfo ticketChangeInfo = new TicketCheckEligibility.TicketChangeInfo();
            TicketNumberTypeI ticketNumberDetails = new TicketNumberTypeI();
            List<TicketNumberDetailsTypeI> documentDetails = new ArrayList<>();

            int reference = 0;
            int infReference = 0;
            int infIndicator = 0;

            for (Passenger passenger : reIssueTicketRequest.getPassengers()) {
                if (!reIssueTicketRequest.isSeaman()) {
                    switch (passenger.getPassengerType().name()) {
                        case "ADT":
                            TravellerDetailsType adultDetails = new TravellerDetailsType();
                            adultDetails.setRef(new BigInteger(Integer.toString(++reference)));
                            adtTravellerDetailsTypeList.add(adultDetails);
                            TicketNumberDetailsTypeI ticketNumberDetailsTypeIAdt = new TicketNumberDetailsTypeI();
                            ticketNumberDetailsTypeIAdt.setNumber(passenger.getTicketNumber());
                            documentDetails.add(ticketNumberDetailsTypeIAdt);
                            break;
                        case "CHD":
                            TravellerDetailsType childDetails = new TravellerDetailsType();
                            childDetails.setRef(new BigInteger(Integer.toString(++reference)));
                            chdTravellerDetailsTypeList.add(childDetails);
                            TicketNumberDetailsTypeI ticketNumberDetailsTypeIChd = new TicketNumberDetailsTypeI();
                            ticketNumberDetailsTypeIChd.setNumber(passenger.getTicketNumber());
                            documentDetails.add(ticketNumberDetailsTypeIChd);
                            break;
                        case "IN":
                        case "INF":
                            TravellerDetailsType infantDetails = new TravellerDetailsType();
                            infantDetails.setRef(new BigInteger(Integer.toString(++infReference)));
                            infantDetails.setInfantIndicator(BigInteger.valueOf(++infIndicator));
                            infTravellerDetailsTypeList.add(infantDetails);
                            TicketNumberDetailsTypeI ticketNumberDetailsTypeIInf = new TicketNumberDetailsTypeI();
                            ticketNumberDetailsTypeIInf.setNumber(passenger.getTicketNumber());
                            documentDetails.add(ticketNumberDetailsTypeIInf);
                            break;
                    }
                } else {
                    TravellerDetailsType seamanDetails = new TravellerDetailsType();
                    seamanDetails.setRef(new BigInteger(Integer.toString(++reference)));
                    seamenTravellerDetailsTypeList.add(seamanDetails);
                    TicketNumberDetailsTypeI ticketNumberDetailsTypeISea = new TicketNumberDetailsTypeI();
                    ticketNumberDetailsTypeISea.setNumber(passenger.getTicketNumber());
                    documentDetails.add(ticketNumberDetailsTypeISea);
                }
            }

            ticketNumberDetails.getDocumentDetails().addAll(documentDetails);
            ticketChangeInfo.setTicketNumberDetails(ticketNumberDetails);
            ticketCheckEligibility.setTicketChangeInfo(ticketChangeInfo);

            TravellerReferenceInformationType adtTraveller = new TravellerReferenceInformationType();
            if (!adtTravellerDetailsTypeList.isEmpty()) {
                adtTraveller.getTraveller().addAll(adtTravellerDetailsTypeList);
                adtPassenger.add(adtTraveller);
                adtTraveller.getPtc().add(PassengerTypeCode.ADT.toString());
                ticketCheckEligibility.getPaxReference().addAll(adtPassenger);
            }

            TravellerReferenceInformationType chdTraveller = new TravellerReferenceInformationType();
            if (!chdTravellerDetailsTypeList.isEmpty()) {
                chdTraveller.getTraveller().addAll(chdTravellerDetailsTypeList);
                chdPassenger.add(chdTraveller);
                chdTraveller.getPtc().add(PassengerTypeCode.CHD.toString());
                ticketCheckEligibility.getPaxReference().addAll(chdPassenger);
            }

            TravellerReferenceInformationType infTraveller = new TravellerReferenceInformationType();
            if (!infTravellerDetailsTypeList.isEmpty()) {
                infTraveller.getTraveller().addAll(infTravellerDetailsTypeList);
                infPassenger.add(infTraveller);
                infTraveller.getPtc().add(PassengerTypeCode.INF.toString());
                ticketCheckEligibility.getPaxReference().addAll(infPassenger);
            }

            TravellerReferenceInformationType seamenTraveller = new TravellerReferenceInformationType();
            if (!seamenTravellerDetailsTypeList.isEmpty()) {
                seamenTraveller.getTraveller().addAll(seamenTravellerDetailsTypeList);
                seamanPassenger.add(seamenTraveller);
                seamenTraveller.getPtc().add(PassengerTypeCode.SEA.toString());
                ticketCheckEligibility.getPaxReference().addAll(seamanPassenger);
            }

        }

        private static void createFareOptionsForEligibilityCheck(TicketCheckEligibility ticketCheckEligibility, ReIssueTicketRequest reIssueTicketRequest, String searchOfficeId) {

            TicketCheckEligibility.FareOptions fareOptions = new TicketCheckEligibility.FareOptions();
            PricingTicketingDetailsType pricingTickInfo = new PricingTicketingDetailsType();
            PricingTicketingInformationType pricingTicketing = new PricingTicketingInformationType();

            if (reIssueTicketRequest.isSeaman()) {
                pricingTicketing.getPriceType().add("PTC");
            }

            pricingTicketing.getPriceType().add("RU");
            pricingTicketing.getPriceType().add("RP");
            pricingTicketing.getPriceType().add("ET");


            //Setting corporate code here
            pricingTicketing.getPriceType().add("RW");
            com.amadeus.xml.fatceq_13_1_1a.CorporateIdentificationType corporate = new com.amadeus.xml.fatceq_13_1_1a.CorporateIdentificationType();
            com.amadeus.xml.fatceq_13_1_1a.CorporateIdentityType corporateId = new com.amadeus.xml.fatceq_13_1_1a.CorporateIdentityType();
            corporateId.setCorporateQualifier("RW");
            corporateId.getIdentity().add("061724");
            corporateId.getIdentity().add(CorporateCodeHelper.getAirlineCorporateCode(reIssueTicketRequest.getBookingType() + "." + vistaraAirlineStr));
            corporate.getCorporateId().add(corporateId);

            pricingTickInfo.setPricingTicketing(pricingTicketing);
            fareOptions.setPricingTickInfo(pricingTickInfo);
            fareOptions.setCorporate(corporate);

            ticketCheckEligibility.setFareOptions(fareOptions);

        }

    }

    public static class ReIssueATCSearch {

        public static TicketATCShopperMasterPricerTravelBoardSearch createReissueATCSearchRequest(ReIssueTicketRequest reIssueTicketRequest, com.amadeus.xml.fatcer_13_1_1a.TravelFlightInformationType allowedCarriers, String searchOfficeId) {

            TicketATCShopperMasterPricerTravelBoardSearch reIssueSearch = new TicketATCShopperMasterPricerTravelBoardSearch();

            // Number of Units here, both recommendation numbers and passenger numbers set here
            try {
                createNumberOfUnitsForSearch(reIssueSearch, reIssueTicketRequest);
            } catch (Exception e) {
                logger.debug("Error Creating Reissue - TicketATCShopperMasterPricerTravelBoardSearch - NumberOfUnits request \n{} ", e.getMessage(), e);
            }

            // Pax Info set here
            try {
                createPaxReferenceForReissueSearch(reIssueSearch, reIssueTicketRequest);
            } catch (Exception e) {
                logger.debug("Error Creating Reissue - TicketATCShopperMasterPricerTravelBoardSearch - PaxReference request \n{} ", e.getMessage(), e);
            }

            // Fare options set here
            try {
                createFareOptionsForReissueSearch(reIssueSearch, reIssueTicketRequest, searchOfficeId);
            } catch (Exception e) {
                logger.debug("Error Creating Reissue - TicketATCShopperMasterPricerTravelBoardSearch - FareOptions request \n{} ", e.getMessage(), e);
            }

            //Allowed Carriers and Cabin class is being set here
            try {
                createTravelFightInfoForReissueSearch(reIssueSearch, reIssueTicketRequest, allowedCarriers);
            } catch (Exception e) {
                logger.debug("Error Creating Reissue - TicketATCShopperMasterPricerTravelBoardSearch - TravelFightInfo request \n{} ", e.getMessage(), e);
            }

            //New Requested Itinerary set here
            try {
                createReIssueRequestedItineraryInfo(reIssueSearch, reIssueTicketRequest);
            } catch (Exception e) {
                logger.debug("Error Creating Reissue - TicketATCShopperMasterPricerTravelBoardSearch - RequestedItineraryInfo request \n{} ", e.getMessage(), e);
            }

            // Old/Original itinerary set here
            try {
                createOriginalItineraryAndTicketNumber(reIssueSearch, reIssueTicketRequest);
            } catch (Exception e) {
                logger.debug("Error Creating Reissue - TicketATCShopperMasterPricerTravelBoardSearch - OriginalItineraryAndTicketNumber request \n{} ", e.getMessage(), e);
            }

            return reIssueSearch;

        }

        private static void createNumberOfUnitsForSearch(TicketATCShopperMasterPricerTravelBoardSearch reIssueSearch, ReIssueTicketRequest reIssueTicketRequest) {

            com.amadeus.xml.fmtctq_18_2_1a.NumberOfUnitsType numberOfUnit = new com.amadeus.xml.fmtctq_18_2_1a.NumberOfUnitsType();
            List<NumberOfUnitDetailsType303181C> unitNumberDetail = new ArrayList<>();

            //Search results number to be set here
            String noOfSearchResultsReissue = play.Play.application().configuration().getString("amadeus.reIssue.noOfSearchResults");
            NumberOfUnitDetailsType303181C searchUnits = new NumberOfUnitDetailsType303181C();
            searchUnits.setTypeOfUnit("RC");
            searchUnits.setNumberOfUnits(new BigInteger(noOfSearchResultsReissue));
            unitNumberDetail.add(searchUnits);

            //Pax units set here
            NumberOfUnitDetailsType303181C paxUnits = new NumberOfUnitDetailsType303181C();
            paxUnits.setTypeOfUnit("PX");
            if (reIssueTicketRequest.isSeaman()) {
                paxUnits.setNumberOfUnits(new BigInteger(String.valueOf(reIssueTicketRequest.getAdultCount() + reIssueTicketRequest.getChildCount() + reIssueTicketRequest.getInfantCount())));
            } else {
                paxUnits.setNumberOfUnits(new BigInteger(String.valueOf(reIssueTicketRequest.getAdultCount() + reIssueTicketRequest.getChildCount())));
            }
            unitNumberDetail.add(paxUnits);

            numberOfUnit.getUnitNumberDetail().addAll(unitNumberDetail);

            reIssueSearch.setNumberOfUnit(numberOfUnit);
        }

        private static void createPaxReferenceForReissueSearch(TicketATCShopperMasterPricerTravelBoardSearch reIssueSearch, ReIssueTicketRequest reIssueTicketRequest) {

            List<com.amadeus.xml.fmtctq_18_2_1a.TravellerReferenceInformationType> adtPassengerList = new ArrayList<>();
            List<com.amadeus.xml.fmtctq_18_2_1a.TravellerReferenceInformationType> chdPassengerList = new ArrayList<>();
            List<com.amadeus.xml.fmtctq_18_2_1a.TravellerReferenceInformationType> infPassengerList = new ArrayList<>();
            List<com.amadeus.xml.fmtctq_18_2_1a.TravellerReferenceInformationType> seamanPassengerList = new ArrayList<>();

            List<com.amadeus.xml.fmtctq_18_2_1a.TravellerDetailsType> adtTravellerDetailsTypeList = new ArrayList<>();
            List<com.amadeus.xml.fmtctq_18_2_1a.TravellerDetailsType> chdTravellerDetailsTypeList = new ArrayList<>();
            List<com.amadeus.xml.fmtctq_18_2_1a.TravellerDetailsType> infTravellerDetailsTypeList = new ArrayList<>();
            List<com.amadeus.xml.fmtctq_18_2_1a.TravellerDetailsType> seamenTravellerDetailsTypeList = new ArrayList<>();

            int reference = 0;
            int infReference = 0;
            int infIndicator = 0;

            for (Passenger passenger : reIssueTicketRequest.getPassengers()) {
                if (!reIssueTicketRequest.isSeaman()) {
                    switch (passenger.getPassengerType().name()) {
                        case "ADT":
                            com.amadeus.xml.fmtctq_18_2_1a.TravellerDetailsType adultDetails = new com.amadeus.xml.fmtctq_18_2_1a.TravellerDetailsType();
                            adultDetails.setRef(new BigInteger(Integer.toString(++reference)));
                            adtTravellerDetailsTypeList.add(adultDetails);
                            break;
                        case "CHD":
                            com.amadeus.xml.fmtctq_18_2_1a.TravellerDetailsType childDetails = new com.amadeus.xml.fmtctq_18_2_1a.TravellerDetailsType();
                            childDetails.setRef(new BigInteger(Integer.toString(++reference)));
                            chdTravellerDetailsTypeList.add(childDetails);
                            break;
                        case "IN":
                        case "INF":
                            com.amadeus.xml.fmtctq_18_2_1a.TravellerDetailsType infantDetails = new com.amadeus.xml.fmtctq_18_2_1a.TravellerDetailsType();
                            infantDetails.setRef(new BigInteger(Integer.toString(++infReference)));
                            infantDetails.setInfantIndicator(BigInteger.valueOf(++infIndicator));
                            infTravellerDetailsTypeList.add(infantDetails);
                            break;
                    }
                } else {
                    com.amadeus.xml.fmtctq_18_2_1a.TravellerDetailsType seamanDetails = new com.amadeus.xml.fmtctq_18_2_1a.TravellerDetailsType();
                    seamanDetails.setRef(new BigInteger(Integer.toString(++reference)));
                    seamenTravellerDetailsTypeList.add(seamanDetails);
                }
            }

            com.amadeus.xml.fmtctq_18_2_1a.TravellerReferenceInformationType adtTraveller = new com.amadeus.xml.fmtctq_18_2_1a.TravellerReferenceInformationType();
            if (!adtTravellerDetailsTypeList.isEmpty()) {
                adtTraveller.getTraveller().addAll(adtTravellerDetailsTypeList);
                adtPassengerList.add(adtTraveller);
                adtTraveller.getPtc().add(PassengerTypeCode.ADT.toString());
                reIssueSearch.getPaxReference().addAll(adtPassengerList);
            }

            com.amadeus.xml.fmtctq_18_2_1a.TravellerReferenceInformationType chdTraveller = new com.amadeus.xml.fmtctq_18_2_1a.TravellerReferenceInformationType();
            if (!chdTravellerDetailsTypeList.isEmpty()) {
                chdTraveller.getTraveller().addAll(chdTravellerDetailsTypeList);
                chdPassengerList.add(chdTraveller);
                chdTraveller.getPtc().add(PassengerTypeCode.CHD.toString());
                reIssueSearch.getPaxReference().addAll(chdPassengerList);
            }

            com.amadeus.xml.fmtctq_18_2_1a.TravellerReferenceInformationType infTraveller = new com.amadeus.xml.fmtctq_18_2_1a.TravellerReferenceInformationType();
            if (!infTravellerDetailsTypeList.isEmpty()) {
                infTraveller.getTraveller().addAll(infTravellerDetailsTypeList);
                infPassengerList.add(infTraveller);
                infTraveller.getPtc().add(PassengerTypeCode.INF.toString());
                reIssueSearch.getPaxReference().addAll(infPassengerList);
            }

            com.amadeus.xml.fmtctq_18_2_1a.TravellerReferenceInformationType seamenTraveller = new com.amadeus.xml.fmtctq_18_2_1a.TravellerReferenceInformationType();
            if (!seamenTravellerDetailsTypeList.isEmpty()) {
                seamenTraveller.getTraveller().addAll(seamenTravellerDetailsTypeList);
                seamanPassengerList.add(seamenTraveller);
                seamenTraveller.getPtc().add(PassengerTypeCode.SEA.toString());
                reIssueSearch.getPaxReference().addAll(seamanPassengerList);
            }

        }

        private static void createFareOptionsForReissueSearch(TicketATCShopperMasterPricerTravelBoardSearch reIssueSearch, ReIssueTicketRequest reIssueTicketRequest, String searchOfficeId) {

            TicketATCShopperMasterPricerTravelBoardSearch.FareOptions fareOptions = new TicketATCShopperMasterPricerTravelBoardSearch.FareOptions();
            com.amadeus.xml.fmtctq_18_2_1a.PricingTicketingDetailsType pricingTickInfo = new com.amadeus.xml.fmtctq_18_2_1a.PricingTicketingDetailsType();
            com.amadeus.xml.fmtctq_18_2_1a.PricingTicketingInformationType pricingTicketing = new com.amadeus.xml.fmtctq_18_2_1a.PricingTicketingInformationType();

            if (reIssueTicketRequest.isSeaman()) {
                pricingTicketing.getPriceType().add("PTC");
            }

            pricingTicketing.getPriceType().add("RU");
            pricingTicketing.getPriceType().add("RP");
            pricingTicketing.getPriceType().add("ET");

            //Setting corporate code here
            pricingTicketing.getPriceType().add("RW");
            CorporateIdentificationType corporate = new CorporateIdentificationType();
            CorporateIdentityType corporateId = new CorporateIdentityType();
            corporateId.setCorporateQualifier("RW");
            corporateId.getIdentity().add("061724");
            corporateId.getIdentity().add(CorporateCodeHelper.getAirlineCorporateCode(reIssueTicketRequest.getBookingType() + "." + vistaraAirlineStr));
            corporate.getCorporateId().add(corporateId);
            fareOptions.setCorporate(corporate);

            pricingTickInfo.setPricingTicketing(pricingTicketing);
            fareOptions.setPricingTickInfo(pricingTickInfo);

            //TODO: May or may not be needed
//            if (!reIssueTicketRequest.isSeaman() && searchOfficeId.equalsIgnoreCase("BOMVS34C3")) {
//                CodedAttributeType222439S feeIdDescription = new CodedAttributeType222439S();
//                CodedAttributeInformationType305810C feeIdFFI = new CodedAttributeInformationType305810C();
//                feeIdFFI.setFeeType("FFI");
//                feeIdFFI.setFeeIdNumber("3");
//                feeIdDescription.getFeeId().add(feeIdFFI);
//
//                CodedAttributeInformationType305810C feeIdUPH = new CodedAttributeInformationType305810C();
//                feeIdUPH.setFeeType("UPH");
//                feeIdUPH.setFeeIdNumber("3");
//                feeIdDescription.getFeeId().add(feeIdUPH);
//
//                fareOptions.setFeeIdDescription(feeIdDescription);
//            }

            reIssueSearch.setFareOptions(fareOptions);

        }

        private static void createTravelFightInfoForReissueSearch(TicketATCShopperMasterPricerTravelBoardSearch reIssueSearch, ReIssueTicketRequest reIssueTicketRequest, com.amadeus.xml.fatcer_13_1_1a.TravelFlightInformationType allowedCarriers) {

            TravelFlightInformationType218017S travelFlightInfo = new TravelFlightInformationType218017S();

            //Setting Allowed carriers here
            if (allowedCarriers != null) {
                CompanyIdentificationType275415C companyIdentity = new CompanyIdentificationType275415C();
                companyIdentity.setCarrierQualifier("M");

                for (com.amadeus.xml.fatcer_13_1_1a.CompanyIdentificationType companyIdentificationType : allowedCarriers.getCompanyIdentity()) {
                    companyIdentity.getCarrierId().add(companyIdentificationType.getOtherCompany());
                }

                travelFlightInfo.getCompanyIdentity().add(companyIdentity);
            }

            //Setting cabin class here
            CabinClass cabinClass = reIssueTicketRequest.getCabinClass();
            com.amadeus.xml.fmtctq_18_2_1a.CabinIdentificationType233500C cabinId = new com.amadeus.xml.fmtctq_18_2_1a.CabinIdentificationType233500C();

            String cabinQualifier = "";
            if (CabinClass.BUSINESS.equals(cabinClass)) {
                cabinQualifier = "C";
            } else if (CabinClass.FIRST.equals(cabinClass)) {
                cabinQualifier = "F";
            } else if (CabinClass.PREMIUM_ECONOMY.equals(cabinClass)) {
                cabinQualifier = "W";
            } else {
                cabinQualifier = "Y";
            }

            cabinId.getCabin().add(cabinQualifier);
            travelFlightInfo.setCabinId(cabinId);

            reIssueSearch.setTravelFlightInfo(travelFlightInfo);

        }


        private static void createReIssueRequestedItineraryInfo(TicketATCShopperMasterPricerTravelBoardSearch reIssueSearch, ReIssueTicketRequest reIssueTicketRequest) {

            List<TicketATCShopperMasterPricerTravelBoardSearch.Itinerary> itineraryList = new ArrayList<>();

            int counter = 1;

            for (ReIssueSearchParameters requestedItinerary : reIssueTicketRequest.getRequestedChange()) {
                TicketATCShopperMasterPricerTravelBoardSearch.Itinerary itinerary = new TicketATCShopperMasterPricerTravelBoardSearch.Itinerary();

                //Setting requested segment reference here
                com.amadeus.xml.fmtctq_18_2_1a.OriginAndDestinationRequestType requestedSegmentRef = new com.amadeus.xml.fmtctq_18_2_1a.OriginAndDestinationRequestType();
                requestedSegmentRef.setSegRef(new BigInteger(Integer.toString(counter++)));
                itinerary.setRequestedSegmentRef(requestedSegmentRef);

                //Setting departure/origin here
                com.amadeus.xml.fmtctq_18_2_1a.DepartureLocationType departureLocalization = new com.amadeus.xml.fmtctq_18_2_1a.DepartureLocationType();
                com.amadeus.xml.fmtctq_18_2_1a.ArrivalLocationDetailsType120834C departurePoint = new com.amadeus.xml.fmtctq_18_2_1a.ArrivalLocationDetailsType120834C();
                departurePoint.setLocationId(requestedItinerary.getOrigin());
                departureLocalization.setDeparturePoint(departurePoint);
                itinerary.setDepartureLocalization(departureLocalization);


                //Setting arrival/destination here
                com.amadeus.xml.fmtctq_18_2_1a.ArrivalLocalizationType arrivalLocalization = new com.amadeus.xml.fmtctq_18_2_1a.ArrivalLocalizationType();
                com.amadeus.xml.fmtctq_18_2_1a.ArrivalLocationDetailsType arrivalPointDetails = new com.amadeus.xml.fmtctq_18_2_1a.ArrivalLocationDetailsType();
                arrivalPointDetails.setLocationId(requestedItinerary.getDestination());
                arrivalLocalization.setArrivalPointDetails(arrivalPointDetails);
                itinerary.setArrivalLocalization(arrivalLocalization);


                //Setting time details here
                DateTime changeDate = new DateTime(requestedItinerary.getChangeDate());
                com.amadeus.xml.fmtctq_18_2_1a.DateAndTimeInformationType181295S timeDetails = new com.amadeus.xml.fmtctq_18_2_1a.DateAndTimeInformationType181295S();
                com.amadeus.xml.fmtctq_18_2_1a.DateAndTimeDetailsTypeI firstDateTimeDetail = new com.amadeus.xml.fmtctq_18_2_1a.DateAndTimeDetailsTypeI();
                firstDateTimeDetail.setDate(mapDate(changeDate));
                firstDateTimeDetail.setTimeQualifier("TD");
                firstDateTimeDetail.setTime("0000");
                timeDetails.setFirstDateTimeDetail(firstDateTimeDetail);
                itinerary.setTimeDetails(timeDetails);

                itineraryList.add(itinerary);

            }

            reIssueSearch.getItinerary().addAll(itineraryList);

        }


        private static String mapDate(DateTime dateTime) {
            String amadeusDate = "";

            String day;
            String month;
            String year;

            day = "" + dateTime.getDayOfMonth();
            month = "" + dateTime.getMonthOfYear();
            year = "" + dateTime.getYearOfCentury();

            day = day.length() == 1 ? "0" + day : day;
            month = month.length() == 1 ? "0" + month : month;
            year = year.length() == 1 ? "0" + year : year;
            amadeusDate = day + month + year;


            return amadeusDate;
        }


        private static void createOriginalItineraryAndTicketNumber(TicketATCShopperMasterPricerTravelBoardSearch reIssueSearch, ReIssueTicketRequest reIssueTicketRequest) {

            TicketATCShopperMasterPricerTravelBoardSearch.TicketChangeInfo ticketChangeInfo = new TicketATCShopperMasterPricerTravelBoardSearch.TicketChangeInfo();

            //Setting ticket numbers in order here
            com.amadeus.xml.fmtctq_18_2_1a.TicketNumberTypeI ticketNumberDetails = new com.amadeus.xml.fmtctq_18_2_1a.TicketNumberTypeI();
            List<com.amadeus.xml.fmtctq_18_2_1a.TicketNumberDetailsTypeI> documentDetails = new ArrayList<>();

            for (Passenger passenger : reIssueTicketRequest.getPassengers()) {
                com.amadeus.xml.fmtctq_18_2_1a.TicketNumberDetailsTypeI ticketNumber = new com.amadeus.xml.fmtctq_18_2_1a.TicketNumberDetailsTypeI();
                ticketNumber.setNumber(passenger.getTicketNumber());
                documentDetails.add(ticketNumber);
            }

            ticketNumberDetails.getDocumentDetails().addAll(documentDetails);
            ticketChangeInfo.setTicketNumberDetails(ticketNumberDetails);

            //Setting the requested segments here
            List<Journey> journeyList;
            if (reIssueTicketRequest.isSeaman()) {
                journeyList = reIssueTicketRequest.getFlightItinerary().getJourneyList();
            } else {
                journeyList = reIssueTicketRequest.getFlightItinerary().getNonSeamenJourneyList();
            }

            List<TicketATCShopperMasterPricerTravelBoardSearch.TicketChangeInfo.TicketRequestedSegments> ticketRequestedSegments = new ArrayList<>();


            for (Journey journey : journeyList) {
                TicketATCShopperMasterPricerTravelBoardSearch.TicketChangeInfo.TicketRequestedSegments ticketRequestedSegment = new TicketATCShopperMasterPricerTravelBoardSearch.TicketChangeInfo.TicketRequestedSegments();

                //TODO: Handling only one way hence going with C will make it dynamic for future segment wise reissue implementation
                com.amadeus.xml.fmtctq_18_2_1a.ActionIdentificationType actionIdentification = new com.amadeus.xml.fmtctq_18_2_1a.ActionIdentificationType();
                actionIdentification.setActionRequestCode("C");
                ticketRequestedSegment.setActionIdentification(actionIdentification);

                //Adding original segment details here
                com.amadeus.xml.fmtctq_18_2_1a.ConnectionTypeI connectPointDetails = new com.amadeus.xml.fmtctq_18_2_1a.ConnectionTypeI();
                connectPointDetails.getConnectionDetails().addAll(getConnectionDetailsTypeIS(journey));
                ticketRequestedSegment.setConnectPointDetails(connectPointDetails);

                ticketRequestedSegments.add(ticketRequestedSegment);
            }
            ticketChangeInfo.getTicketRequestedSegments().addAll(ticketRequestedSegments);

            reIssueSearch.setTicketChangeInfo(ticketChangeInfo);

        }

        //Method to Set origin and destination here for original itinerary
        private static List<com.amadeus.xml.fmtctq_18_2_1a.ConnectionDetailsTypeI> getConnectionDetailsTypeIS(Journey journey) {
            List<com.amadeus.xml.fmtctq_18_2_1a.ConnectionDetailsTypeI> connectionDetails = new ArrayList<>();
            com.amadeus.xml.fmtctq_18_2_1a.ConnectionDetailsTypeI fromLocation = new com.amadeus.xml.fmtctq_18_2_1a.ConnectionDetailsTypeI();
            fromLocation.setLocation(journey.getAirSegmentList().get(0).getFromLocation());
            connectionDetails.add(fromLocation);
            for (AirSegmentInformation airSegmentInformation : journey.getAirSegmentList()) {
                com.amadeus.xml.fmtctq_18_2_1a.ConnectionDetailsTypeI toLocation = new com.amadeus.xml.fmtctq_18_2_1a.ConnectionDetailsTypeI();
                toLocation.setLocation(airSegmentInformation.getToLocation());
                connectionDetails.add(toLocation);
            }
            return connectionDetails;
        }
    }

}
