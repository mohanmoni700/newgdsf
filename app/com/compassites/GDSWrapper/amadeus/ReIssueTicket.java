package com.compassites.GDSWrapper.amadeus;

import com.amadeus.xml._2010._06.etr_types_v4.FlightIdentifierType;
import com.amadeus.xml._2010._06.fareinternaltypes_v2.PricingOptionType;
import com.amadeus.xml._2010._06.pricingtypes_v4.PricingOptionBaseType;
import com.amadeus.xml._2010._06.retailing_types_v2.AirSegmentType;
import com.amadeus.xml._2010._06.retailing_types_v2.AssociationsType;
import com.amadeus.xml._2010._06.retailing_types_v2.CommitType;
import com.amadeus.xml._2010._06.retailing_types_v2.ReservationType;
import com.amadeus.xml._2010._06.ticket_rebookandrepricepnr_v1.AMATicketRebookAndRepricePNRRQ;
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
import com.amadeus.xml.fmtctq_18_2_1a.ConnectPointDetailsType195492C;
import com.amadeus.xml.pnrspl_11_3_1a.*;
import com.amadeus.xml.pnrspl_11_3_1a.PNRSplit;
import com.amadeus.xml.tarcpq_13_2_1a.TicketReissueConfirmedPricing;
import com.amadeus.xml.taripq_19_1_1a.*;
import com.amadeus.xml.taripq_19_1_1a.CompanyIdentificationTypeI;
import com.amadeus.xml.taripq_19_1_1a.ItemNumberIdentificationType;
import com.amadeus.xml.taripq_19_1_1a.ItemNumberType;
import com.amadeus.xml.taripq_19_1_1a.ReferencingDetailsType;
import com.amadeus.xml.taripq_19_1_1a.ReferenceInfoType;
import com.amadeus.xml.fmtctq_18_2_1a.ProductTypeDetailsType120801C;
import com.amadeus.xml.tatreq_20_1_1a.MessageActionDetailsType;
import com.amadeus.xml.tatreq_20_1_1a.MessageFunctionBusinessDetailsType;
import com.amadeus.xml.tatreq_20_1_1a.TicketProcessEDoc;
import com.compassites.model.*;
import com.compassites.model.traveller.TravellerMasterInfo;
import dto.reissue.AmadeusPaxRefAndTicket;
import dto.reissue.ReIssueConfirmationRequest;
import dto.reissue.ReIssueSearchParameters;
import dto.reissue.ReIssueSearchRequest;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;


//All request Bodies for Reissue flows are created here
public class ReIssueTicket {

    private static final Logger logger = LoggerFactory.getLogger("gds");

    private static final String amadeusReIssueEnvironment = play.Play.application().configuration().getString("amadeus.reIssue.environment");

    /**
     * This inner class is responsible for creating a ReIssueCheckStatus request.
     */
    public static class ReIssueCheckTicketStatus {

        /**
         * Creates a TicketProcessEDoc for checking the status of a reissued ticket.
         *
         * @param reIssueSearchRequest The object containing details for the ticket to be reissued.
         * @return A TicketProcessEDoc object configured for the reissue ticket status check.
         */
        public static TicketProcessEDoc createReissueTicketStatusCheck(ReIssueSearchRequest reIssueSearchRequest) {

            TicketProcessEDoc ticketProcessEDoc = new TicketProcessEDoc();

            try {

                //Setting message function to 131 as we are using Display an ETKT instead of EMD
                MessageActionDetailsType msgActionDetails = new MessageActionDetailsType();
                MessageFunctionBusinessDetailsType messageFunctionDetails = new MessageFunctionBusinessDetailsType();

                messageFunctionDetails.setMessageFunction("131");
                msgActionDetails.setMessageFunctionDetails(messageFunctionDetails);
                ticketProcessEDoc.setMsgActionDetails(msgActionDetails);

                //Setting ticket numbers here
                List<TicketProcessEDoc.InfoGroup> infoGroup = getInfoGroups(reIssueSearchRequest);
                ticketProcessEDoc.getInfoGroup().addAll(infoGroup);
            } catch (Exception e) {
                logger.debug("Error Creating Reissue - TicketStatusCheck - msgActionDetails request \n{} ", e.getMessage(), e);
            }

            return ticketProcessEDoc;
        }

        private static List<TicketProcessEDoc.InfoGroup> getInfoGroups(ReIssueSearchRequest reIssueSearchRequest) {
            List<TicketProcessEDoc.InfoGroup> infoGroupList = new ArrayList<>();

            try {
                for (Passenger passenger : reIssueSearchRequest.getPassengers()) {

                    TicketProcessEDoc.InfoGroup infGroup = new TicketProcessEDoc.InfoGroup();
                    com.amadeus.xml.tatreq_20_1_1a.TicketNumberTypeI docInfo = new com.amadeus.xml.tatreq_20_1_1a.TicketNumberTypeI();
                    com.amadeus.xml.tatreq_20_1_1a.TicketNumberDetailsTypeI documentDetails = new com.amadeus.xml.tatreq_20_1_1a.TicketNumberDetailsTypeI();

                    documentDetails.setNumber(passenger.getTicketNumber());
                    docInfo.setDocumentDetails(documentDetails);
                    infGroup.setDocInfo(docInfo);

                    infoGroupList.add(infGroup);
                }
            } catch (Exception e) {
                logger.debug("Error Creating Reissue - TicketStatusCheck - infoGroup request \n{} ", e.getMessage(), e);
            }
            return infoGroupList;
        }
    }

    //This inner class for creating ReIssueCheckEligibility request
    public static class ReIssueCheckEligibility {

        public static TicketCheckEligibility createCheckEligibilityRequest(ReIssueSearchRequest reIssueSearchRequest, String searchOfficeId) {
            TicketCheckEligibility checkEligibility = new TicketCheckEligibility();

            //Creating Number of units here
            try {
                createNumberOfUnitsForEligibilityCheck(checkEligibility, reIssueSearchRequest);
            } catch (Exception e) {
                logger.debug("Error Creating Reissue - TicketCheckEligibility - NumberOfUnits request \n{} ", e.getMessage(), e);
            }

            //Creating Pax reference and ticketChangeInfo here.
            try {
                createPaxReferenceForEligibilityCheck(checkEligibility, reIssueSearchRequest);
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
                createFareOptionsForEligibilityCheck(checkEligibility, reIssueSearchRequest, searchOfficeId);
            } catch (Exception e) {
                logger.debug("Error Creating Reissue - TicketCheckEligibility - FareOptions request \n{} ", e.getMessage(), e);
            }

            return checkEligibility;
        }

        private static void createNumberOfUnitsForEligibilityCheck(TicketCheckEligibility ticketCheckEligibility, ReIssueSearchRequest reIssueSearchRequest) {

            NumberOfUnitsType numberOfUnitsType = new NumberOfUnitsType();
            NumberOfUnitDetailsType191580C numberOfUnitDetailsType191580C = new NumberOfUnitDetailsType191580C();
            numberOfUnitDetailsType191580C.setTypeOfUnit("PX");
            if (reIssueSearchRequest.isSeaman()) {
                numberOfUnitDetailsType191580C.setNumberOfUnits(new BigInteger(String.valueOf(reIssueSearchRequest.getAdultCount() + reIssueSearchRequest.getChildCount() + reIssueSearchRequest.getInfantCount())));
            } else {
                numberOfUnitDetailsType191580C.setNumberOfUnits(new BigInteger(String.valueOf(reIssueSearchRequest.getAdultCount() + reIssueSearchRequest.getChildCount())));
            }
            numberOfUnitsType.getUnitNumberDetail().add(numberOfUnitDetailsType191580C);
            ticketCheckEligibility.setNumberOfUnit(numberOfUnitsType);

        }

        private static void createPaxReferenceForEligibilityCheck(TicketCheckEligibility ticketCheckEligibility, ReIssueSearchRequest reIssueSearchRequest) {

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

            for (Passenger passenger : reIssueSearchRequest.getPassengers()) {
                if (!reIssueSearchRequest.isSeaman()) {
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
                adtTraveller.getPtc().add(PassengerTypeCode.ADT.toString());
                adtPassenger.add(adtTraveller);
                ticketCheckEligibility.getPaxReference().addAll(adtPassenger);
            }

            TravellerReferenceInformationType chdTraveller = new TravellerReferenceInformationType();
            if (!chdTravellerDetailsTypeList.isEmpty()) {
                chdTraveller.getTraveller().addAll(chdTravellerDetailsTypeList);
                chdTraveller.getPtc().add(PassengerTypeCode.CHD.toString());
                chdPassenger.add(chdTraveller);
                ticketCheckEligibility.getPaxReference().addAll(chdPassenger);
            }

            TravellerReferenceInformationType infTraveller = new TravellerReferenceInformationType();
            if (!infTravellerDetailsTypeList.isEmpty()) {
                infTraveller.getTraveller().addAll(infTravellerDetailsTypeList);
                infTraveller.getPtc().add(PassengerTypeCode.INF.toString());
                infPassenger.add(infTraveller);
                ticketCheckEligibility.getPaxReference().addAll(infPassenger);
            }

            TravellerReferenceInformationType seamenTraveller = new TravellerReferenceInformationType();
            if (!seamenTravellerDetailsTypeList.isEmpty()) {
                seamenTraveller.getTraveller().addAll(seamenTravellerDetailsTypeList);
                seamenTraveller.getPtc().add(PassengerTypeCode.SEA.toString());
                seamanPassenger.add(seamenTraveller);
                ticketCheckEligibility.getPaxReference().addAll(seamanPassenger);
            }

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

        private static void createFareOptionsForEligibilityCheck(TicketCheckEligibility ticketCheckEligibility, ReIssueSearchRequest reIssueSearchRequest, String searchOfficeId) {

            TicketCheckEligibility.FareOptions fareOptions = new TicketCheckEligibility.FareOptions();
            PricingTicketingDetailsType pricingTickInfo = new PricingTicketingDetailsType();
            PricingTicketingInformationType pricingTicketing = new PricingTicketingInformationType();

            if (reIssueSearchRequest.isSeaman()) {
                pricingTicketing.getPriceType().add("PTC");
            }

            pricingTicketing.getPriceType().add("RU");
            pricingTicketing.getPriceType().add("RP");
            pricingTicketing.getPriceType().add("ET");


            //Setting corporate code here
//            pricingTicketing.getPriceType().add("RW");
//            com.amadeus.xml.fatceq_13_1_1a.CorporateIdentificationType corporate = new com.amadeus.xml.fatceq_13_1_1a.CorporateIdentificationType();
//            com.amadeus.xml.fatceq_13_1_1a.CorporateIdentityType corporateId = new com.amadeus.xml.fatceq_13_1_1a.CorporateIdentityType();
//            corporateId.setCorporateQualifier("RW");
//            corporateId.getIdentity().add("061724");
//            corporateId.getIdentity().add(CorporateCodeHelper.getAirlineCorporateCode(reIssueSearchRequest.getBookingType() + "." + vistaraAirlineStr));
//            corporate.getCorporateId().add(corporateId);

            pricingTickInfo.setPricingTicketing(pricingTicketing);
            fareOptions.setPricingTickInfo(pricingTickInfo);
//            fareOptions.setCorporate(corporate);

            ticketCheckEligibility.setFareOptions(fareOptions);

        }

    }

    //ATC search request is being created here
    public static class ReIssueATCSearch {

        public static TicketATCShopperMasterPricerTravelBoardSearch createReissueATCSearchRequest(ReIssueSearchRequest reIssueSearchRequest, com.amadeus.xml.fatcer_13_1_1a.TravelFlightInformationType allowedCarriers, String searchOfficeId) {

            TicketATCShopperMasterPricerTravelBoardSearch reIssueSearch = new TicketATCShopperMasterPricerTravelBoardSearch();

            // Number of Units here, both recommendation numbers and passenger numbers set here
            try {
                createNumberOfUnitsForSearch(reIssueSearch, reIssueSearchRequest);
            } catch (Exception e) {
                logger.debug("Error Creating Reissue - TicketATCShopperMasterPricerTravelBoardSearch - NumberOfUnits request \n{} ", e.getMessage(), e);
            }

            // Pax Info set here
            try {
                createPaxReferenceForReissueSearch(reIssueSearch, reIssueSearchRequest);
            } catch (Exception e) {
                logger.debug("Error Creating Reissue - TicketATCShopperMasterPricerTravelBoardSearch - PaxReference request \n{} ", e.getMessage(), e);
            }

            // Fare options set here
            try {
                createFareOptionsForReissueSearch(reIssueSearch, reIssueSearchRequest, searchOfficeId);
            } catch (Exception e) {
                logger.debug("Error Creating Reissue - TicketATCShopperMasterPricerTravelBoardSearch - FareOptions request \n{} ", e.getMessage(), e);
            }

            //Allowed Carriers and Cabin class is being set here
            try {
                createTravelFightInfoForReissueSearch(reIssueSearch, reIssueSearchRequest, allowedCarriers);
            } catch (Exception e) {
                logger.debug("Error Creating Reissue - TicketATCShopperMasterPricerTravelBoardSearch - TravelFightInfo request \n{} ", e.getMessage(), e);
            }

            //New Requested Itinerary set here
            try {
                createReIssueRequestedItineraryInfo(reIssueSearch, reIssueSearchRequest);
            } catch (Exception e) {
                logger.debug("Error Creating Reissue - TicketATCShopperMasterPricerTravelBoardSearch - RequestedItineraryInfo request \n{} ", e.getMessage(), e);
            }

            // Old/Original itinerary set here
            try {
                createOriginalItineraryAndTicketNumber(reIssueSearch, reIssueSearchRequest);
            } catch (Exception e) {
                logger.debug("Error Creating Reissue - TicketATCShopperMasterPricerTravelBoardSearch - OriginalItineraryAndTicketNumber request \n{} ", e.getMessage(), e);
            }

            return reIssueSearch;

        }

        private static void createNumberOfUnitsForSearch(TicketATCShopperMasterPricerTravelBoardSearch reIssueSearch, ReIssueSearchRequest reIssueSearchRequest) {

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
            if (reIssueSearchRequest.isSeaman()) {
                paxUnits.setNumberOfUnits(new BigInteger(String.valueOf(reIssueSearchRequest.getAdultCount() + reIssueSearchRequest.getChildCount() + reIssueSearchRequest.getInfantCount())));
            } else {
                paxUnits.setNumberOfUnits(new BigInteger(String.valueOf(reIssueSearchRequest.getAdultCount() + reIssueSearchRequest.getChildCount())));
            }
            unitNumberDetail.add(paxUnits);

            numberOfUnit.getUnitNumberDetail().addAll(unitNumberDetail);

            reIssueSearch.setNumberOfUnit(numberOfUnit);
        }

        private static void createPaxReferenceForReissueSearch(TicketATCShopperMasterPricerTravelBoardSearch reIssueSearch, ReIssueSearchRequest reIssueSearchRequest) {

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

            for (Passenger passenger : reIssueSearchRequest.getPassengers()) {
                if (!reIssueSearchRequest.isSeaman()) {
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
                adtTraveller.getPtc().add(PassengerTypeCode.ADT.toString());
                adtPassengerList.add(adtTraveller);
                reIssueSearch.getPaxReference().addAll(adtPassengerList);
            }

            com.amadeus.xml.fmtctq_18_2_1a.TravellerReferenceInformationType chdTraveller = new com.amadeus.xml.fmtctq_18_2_1a.TravellerReferenceInformationType();
            if (!chdTravellerDetailsTypeList.isEmpty()) {
                chdTraveller.getTraveller().addAll(chdTravellerDetailsTypeList);
                chdTraveller.getPtc().add(PassengerTypeCode.CHD.toString());
                chdPassengerList.add(chdTraveller);
                reIssueSearch.getPaxReference().addAll(chdPassengerList);
            }

            com.amadeus.xml.fmtctq_18_2_1a.TravellerReferenceInformationType infTraveller = new com.amadeus.xml.fmtctq_18_2_1a.TravellerReferenceInformationType();
            if (!infTravellerDetailsTypeList.isEmpty()) {
                infTraveller.getTraveller().addAll(infTravellerDetailsTypeList);
                infTraveller.getPtc().add(PassengerTypeCode.INF.toString());
                infPassengerList.add(infTraveller);
                reIssueSearch.getPaxReference().addAll(infPassengerList);
            }

            com.amadeus.xml.fmtctq_18_2_1a.TravellerReferenceInformationType seamenTraveller = new com.amadeus.xml.fmtctq_18_2_1a.TravellerReferenceInformationType();
            if (!seamenTravellerDetailsTypeList.isEmpty()) {
                seamenTraveller.getTraveller().addAll(seamenTravellerDetailsTypeList);
                seamenTraveller.getPtc().add(PassengerTypeCode.SEA.toString());
                seamanPassengerList.add(seamenTraveller);
                reIssueSearch.getPaxReference().addAll(seamanPassengerList);
            }

        }

        private static void createFareOptionsForReissueSearch(TicketATCShopperMasterPricerTravelBoardSearch reIssueSearch, ReIssueSearchRequest reIssueSearchRequest, String searchOfficeId) {

            TicketATCShopperMasterPricerTravelBoardSearch.FareOptions fareOptions = new TicketATCShopperMasterPricerTravelBoardSearch.FareOptions();
            com.amadeus.xml.fmtctq_18_2_1a.PricingTicketingDetailsType pricingTickInfo = new com.amadeus.xml.fmtctq_18_2_1a.PricingTicketingDetailsType();
            com.amadeus.xml.fmtctq_18_2_1a.PricingTicketingInformationType pricingTicketing = new com.amadeus.xml.fmtctq_18_2_1a.PricingTicketingInformationType();

            if (reIssueSearchRequest.isSeaman()) {
                pricingTicketing.getPriceType().add("PTC");
            }

            pricingTicketing.getPriceType().add("RU");
            pricingTicketing.getPriceType().add("RP");
            pricingTicketing.getPriceType().add("ET");

            //Setting corporate code here
//            pricingTicketing.getPriceType().add("RW");
//            CorporateIdentificationType corporate = new CorporateIdentificationType();
//            CorporateIdentityType corporateId = new CorporateIdentityType();
//            corporateId.setCorporateQualifier("RW");
//            corporateId.getIdentity().add("061724");
//            corporateId.getIdentity().add(CorporateCodeHelper.getAirlineCorporateCode(reIssueSearchRequest.getBookingType() + "." + vistaraAirlineStr));
//            corporate.getCorporateId().add(corporateId);
//            fareOptions.setCorporate(corporate);

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

        private static void createTravelFightInfoForReissueSearch(TicketATCShopperMasterPricerTravelBoardSearch reIssueSearch, ReIssueSearchRequest reIssueSearchRequest, com.amadeus.xml.fatcer_13_1_1a.TravelFlightInformationType allowedCarriers) {

            TravelFlightInformationType218017S travelFlightInfo = new TravelFlightInformationType218017S();

            //Setting Allowed carriers here
//            if (allowedCarriers != null) {
//                CompanyIdentificationType275415C companyIdentity = new CompanyIdentificationType275415C();
//                companyIdentity.setCarrierQualifier("M");
//
//                for (com.amadeus.xml.fatcer_13_1_1a.CompanyIdentificationType companyIdentificationType : allowedCarriers.getCompanyIdentity()) {
//                    companyIdentity.getCarrierId().add(companyIdentificationType.getOtherCompany());
//                }
//
//                travelFlightInfo.getCompanyIdentity().add(companyIdentity);
//            }

            //Setting cabin class here
            CabinClass cabinClass = reIssueSearchRequest.getCabinClass();
            com.amadeus.xml.fmtctq_18_2_1a.CabinIdentificationType233500C cabinId = new com.amadeus.xml.fmtctq_18_2_1a.CabinIdentificationType233500C();

            String cabinQualifier;
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

        private static void createReIssueRequestedItineraryInfo(TicketATCShopperMasterPricerTravelBoardSearch reIssueSearch, ReIssueSearchRequest reIssueSearchRequest) {

            List<TicketATCShopperMasterPricerTravelBoardSearch.Itinerary> itineraryList = new ArrayList<>();

            int counter = 0;

            for (ReIssueSearchParameters requestedItinerary : reIssueSearchRequest.getRequestedChange()) {

                TicketATCShopperMasterPricerTravelBoardSearch.Itinerary itinerary = new TicketATCShopperMasterPricerTravelBoardSearch.Itinerary();

                //Setting requested segment reference here
                com.amadeus.xml.fmtctq_18_2_1a.OriginAndDestinationRequestType requestedSegmentRef = new com.amadeus.xml.fmtctq_18_2_1a.OriginAndDestinationRequestType();
                requestedSegmentRef.setSegRef(new BigInteger(Integer.toString(++counter)));
                itinerary.setRequestedSegmentRef(requestedSegmentRef);

                //Setting departure/origin here
                com.amadeus.xml.fmtctq_18_2_1a.DepartureLocationType departureLocalization = new com.amadeus.xml.fmtctq_18_2_1a.DepartureLocationType();
                com.amadeus.xml.fmtctq_18_2_1a.ArrivalLocationDetailsType120834C departurePoint = new com.amadeus.xml.fmtctq_18_2_1a.ArrivalLocationDetailsType120834C();
                departurePoint.setLocationId(requestedItinerary.getOrigin());
                departureLocalization.setDeparturePoint(departurePoint);
                itinerary.setDepartureLocalization(departureLocalization);

                //Setting arrival/destination here
                com.amadeus.xml.fmtctq_18_2_1a.ArrivalLocalizationType arrivalLocalization = new com.amadeus.xml.fmtctq_18_2_1a.ArrivalLocalizationType();
                com.amadeus.xml.fmtctq_18_2_1a.ArrivalLocationDetailsType arrivalPoint = new com.amadeus.xml.fmtctq_18_2_1a.ArrivalLocationDetailsType();
                arrivalPoint.setLocationId(requestedItinerary.getDestination());
                arrivalLocalization.setArrivalPointDetails(arrivalPoint);
                itinerary.setArrivalLocalization(arrivalLocalization);

                //Setting Departure or Arrival Time details here
                com.amadeus.xml.fmtctq_18_2_1a.DateAndTimeInformationType181295S timeDetails = new com.amadeus.xml.fmtctq_18_2_1a.DateAndTimeInformationType181295S();
                com.amadeus.xml.fmtctq_18_2_1a.DateAndTimeDetailsTypeI firstDateTimeDetail = new com.amadeus.xml.fmtctq_18_2_1a.DateAndTimeDetailsTypeI();

                firstDateTimeDetail.setDate(mapDate(requestedItinerary.getTravelDate()));
                String timeQualifier = "TA";
                String time = "2359";

                //Only for round trip when the return journey is not changed
                if ("ARRIVAL".equalsIgnoreCase(requestedItinerary.getDateType().name()) && "KF".equalsIgnoreCase(requestedItinerary.getActionRequestedCode())) {

                    firstDateTimeDetail.setDate(reIssueSearchRequest.getFlightItinerary()
                            .getNonSeamenJourneyList().get(1)
                            .getAirSegmentList().get(0)
                            .getToDate());

                    timeQualifier = "TD";
                    time = "0000";
                } else if ("DEPARTURE".equalsIgnoreCase(requestedItinerary.getDateType().name())) {

                    timeQualifier = "TD";
                    time = "0000";
                }

                firstDateTimeDetail.setTimeQualifier(timeQualifier);
                firstDateTimeDetail.setTime(time);
                timeDetails.setFirstDateTimeDetail(firstDateTimeDetail);

                itinerary.setTimeDetails(timeDetails);


                if (requestedItinerary.isTransitPointAdded() || requestedItinerary.isNonStop()) {
                    TravelFlightInformationType218018S flightInfo = new TravelFlightInformationType218018S();

                    //Adding transit points to segments if exists here
                    if (requestedItinerary.isTransitPointAdded() && !requestedItinerary.isNonStop()) {
                        flightInfo.getInclusionDetail().addAll(getConnectPointDetailsType195492CS(requestedItinerary));
                    }

                    if (requestedItinerary.isNonStop() && !requestedItinerary.isTransitPointAdded()) {
                        ProductTypeDetailsType120801C flightDetail = new ProductTypeDetailsType120801C();
                        flightDetail.getFlightType().add("N");
                        flightDetail.getFlightType().add("D");
                        flightInfo.setFlightDetail(flightDetail);
                    }

                    itinerary.setFlightInfo(flightInfo);
                }

                itineraryList.add(itinerary);
            }

            reIssueSearch.getItinerary().addAll(itineraryList);

        }

        //Method to add transit point for requested itinerary
        private static List<ConnectPointDetailsType195492C> getConnectPointDetailsType195492CS(ReIssueSearchParameters requestedItinerary) {
            List<ConnectPointDetailsType195492C> inclusionDetails = new ArrayList<>();
            List<String> transitPoints = requestedItinerary.getTransitPointList();

            for (int i = 0; i < transitPoints.size(); i++) {
                ConnectPointDetailsType195492C inclusionDetail = new ConnectPointDetailsType195492C();
                inclusionDetail.setInclusionIdentifier("M");
                inclusionDetail.setLocationId(transitPoints.get(i));
                inclusionDetails.add(inclusionDetail);
            }
            return inclusionDetails;
        }

        //Mapping dates to amadeus required format
        private static String mapDate(Date changeDate) {
            DateTime dateTime = new DateTime(changeDate);
            String amadeusDate;

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

        private static void createOriginalItineraryAndTicketNumber(TicketATCShopperMasterPricerTravelBoardSearch reIssueSearch, ReIssueSearchRequest reIssueSearchRequest) {

            TicketATCShopperMasterPricerTravelBoardSearch.TicketChangeInfo ticketChangeInfo = new TicketATCShopperMasterPricerTravelBoardSearch.TicketChangeInfo();

            //Setting ticket numbers in order here
            com.amadeus.xml.fmtctq_18_2_1a.TicketNumberTypeI ticketNumberDetails = new com.amadeus.xml.fmtctq_18_2_1a.TicketNumberTypeI();
            List<com.amadeus.xml.fmtctq_18_2_1a.TicketNumberDetailsTypeI> documentDetails = new ArrayList<>();

            for (Passenger passenger : reIssueSearchRequest.getPassengers()) {
                com.amadeus.xml.fmtctq_18_2_1a.TicketNumberDetailsTypeI ticketNumber = new com.amadeus.xml.fmtctq_18_2_1a.TicketNumberDetailsTypeI();
                ticketNumber.setNumber(passenger.getTicketNumber());
                documentDetails.add(ticketNumber);
            }

            ticketNumberDetails.getDocumentDetails().addAll(documentDetails);
            ticketChangeInfo.setTicketNumberDetails(ticketNumberDetails);

            //Setting the requested segments here
            List<Journey> journeyList;
            if (reIssueSearchRequest.isSeaman()) {
                journeyList = reIssueSearchRequest.getFlightItinerary().getJourneyList();
            } else {
                journeyList = reIssueSearchRequest.getFlightItinerary().getNonSeamenJourneyList();
            }

            List<ReIssueSearchParameters> requestedChange = reIssueSearchRequest.getRequestedChange();
            List<TicketATCShopperMasterPricerTravelBoardSearch.TicketChangeInfo.TicketRequestedSegments> ticketRequestedSegments = new ArrayList<>();

            int journeyCounter = 0;
            for (Journey journey : journeyList) {
                TicketATCShopperMasterPricerTravelBoardSearch.TicketChangeInfo.TicketRequestedSegments ticketRequestedSegment = new TicketATCShopperMasterPricerTravelBoardSearch.TicketChangeInfo.TicketRequestedSegments();

                //Setting Action request Code here
                com.amadeus.xml.fmtctq_18_2_1a.ActionIdentificationType actionIdentification = new com.amadeus.xml.fmtctq_18_2_1a.ActionIdentificationType();
                actionIdentification.setActionRequestCode(requestedChange.get(journeyCounter).getActionRequestedCode());
                ticketRequestedSegment.setActionIdentification(actionIdentification);

                //Adding original segment details here
                com.amadeus.xml.fmtctq_18_2_1a.ConnectionTypeI connectPointDetails = new com.amadeus.xml.fmtctq_18_2_1a.ConnectionTypeI();
                connectPointDetails.getConnectionDetails().addAll(getConnectionDetailsTypeIS(journey));
                ticketRequestedSegment.setConnectPointDetails(connectPointDetails);

                ticketRequestedSegments.add(ticketRequestedSegment);
                journeyCounter++;
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

    public static class ReIssueConfirmation {

        public static PNRSplit splitPNRForReIssuedPax(ReIssueConfirmationRequest reIssueConfirmationRequest) {

            PNRSplit pnrSplit = new PNRSplit();

            //Setting PNR here
            ReservationControlInformationType reservationInfo = new ReservationControlInformationType();
            ReservationControlInformationDetailsTypeI reservation = new ReservationControlInformationDetailsTypeI();
            reservation.setControlNumber(reIssueConfirmationRequest.getOriginalGdsPnr());
            reservationInfo.setReservation(reservation);
            pnrSplit.setReservationInfo(reservationInfo);

            //Setting Passenger Tattoos here
            SplitPNRType splitDetails = new SplitPNRType();
            SplitPNRDetailsType passenger = new SplitPNRDetailsType();
            passenger.setType("PT"); //PT -> Passenger Tattoo
            List<AmadeusPaxRefAndTicket> amadeusPaxRefAndTicketList = reIssueConfirmationRequest.getPaxAndTicketList();
            for (AmadeusPaxRefAndTicket amadeusPaxRefAndTicket : amadeusPaxRefAndTicketList) {
                passenger.getTattoo().add(amadeusPaxRefAndTicket.getPaxRef());
            }
            splitDetails.setPassenger(passenger);
            pnrSplit.setSplitDetails(splitDetails);

            return pnrSplit;
        }


        public static AMATicketRebookAndRepricePNRRQ createReIssueRebookAndRepriceReq(ReIssueConfirmationRequest reIssueConfirmationRequest, String newSplitPnr, List<String> cabinClassList) {

            AMATicketRebookAndRepricePNRRQ amaTicketRebookAndRepricePNRRQ = new AMATicketRebookAndRepricePNRRQ();

            //Setting PNR here
            ReservationType reservation = new ReservationType();
            reservation.setBookingIdentifier(newSplitPnr);
            amaTicketRebookAndRepricePNRRQ.setReservation(reservation);

            //Setting Test/Prod environment and ignore warnings here
            CommitType commit = new CommitType();
            commit.setIgnoreWarningsOption(true);
            commit.setReceivedFrom(amadeusReIssueEnvironment);
            amaTicketRebookAndRepricePNRRQ.setCommit(commit);


            boolean isSeaman = reIssueConfirmationRequest.isSeaman();

            List<Journey> segmentsToBeAdded;
            if (isSeaman) {
                segmentsToBeAdded = reIssueConfirmationRequest.getNewTravellerMasterInfo().getItinerary().getJourneyList();
            } else {
                segmentsToBeAdded = reIssueConfirmationRequest.getNewTravellerMasterInfo().getItinerary().getNonSeamenJourneyList();
            }

            //Setting Segments to be old cancelled and new segments to be set here
            try {
                createReBookingRequest(reIssueConfirmationRequest, segmentsToBeAdded, amaTicketRebookAndRepricePNRRQ, cabinClassList);
            } catch (Exception e) {
                logger.debug("Error Creating Rebooking Request for ReIssue {}", e.getMessage(), e);
            }

            //Pricing codes set here
            try {
                createRePricingRequest(reIssueConfirmationRequest, segmentsToBeAdded, amaTicketRebookAndRepricePNRRQ);
            } catch (Exception e) {
                logger.debug("Error Creating Rebooking Request for ReIssue {}", e.getMessage(), e);
            }

            return amaTicketRebookAndRepricePNRRQ;
        }

        private static void createReBookingRequest(ReIssueConfirmationRequest reIssueConfirmationRequest, List<Journey> segmentsToBeAdded, AMATicketRebookAndRepricePNRRQ amaTicketRebookAndRepricePNRRQ, List<String> segmentWiseBookingClassList) {

            AMATicketRebookAndRepricePNRRQ.Rebooking rebooking = new AMATicketRebookAndRepricePNRRQ.Rebooking();

            //Segments to be cancelled set here
            AssociationsType cancellation = new AssociationsType();
            List<AssociationsType.Ref> refList = new ArrayList<>();
            List<Integer> segmentsToBeCancelled = reIssueConfirmationRequest.getSegmentsToBeCancelledAndNewlyAdded();
            for (Integer segment : segmentsToBeCancelled) {
                AssociationsType.Ref ref = new AssociationsType.Ref();
                ref.setTattooType("ST");
                ref.setTattooValue(String.valueOf(segment));
                refList.add(ref);
            }
            cancellation.getRef().addAll(refList);
            rebooking.setCancellation(cancellation);

            //New segments here
            int paxCount = reIssueConfirmationRequest.getPaxAndTicketList().size();

            AMATicketRebookAndRepricePNRRQ.Rebooking.Bounds bounds = new AMATicketRebookAndRepricePNRRQ.Rebooking.Bounds();
            List<AMATicketRebookAndRepricePNRRQ.Rebooking.Bounds.Bound> boundList = new ArrayList<>();

            List<Integer> selectedJourneyIndex = reIssueConfirmationRequest.getSelectedSegmentList();
            for (Integer selectedJourney : selectedJourneyIndex) {

                Journey journey = segmentsToBeAdded.get(selectedJourney - 1);

                AMATicketRebookAndRepricePNRRQ.Rebooking.Bounds.Bound bound = new AMATicketRebookAndRepricePNRRQ.Rebooking.Bounds.Bound();
                bound.setActionCode("NN");
                bound.setNIP(String.valueOf(paxCount));  //Number of seats to Book/ pax Count

                List<AirSegmentInformation> airSegmentInformationList = journey.getAirSegmentList();
                List<AirSegmentType> segment = new ArrayList<>();

                int segmentCounter = 0;
                for (AirSegmentInformation airSegmentInformation : airSegmentInformationList) {
                    String segIdRefString = "SEG" + (segmentsToBeCancelled.remove(segmentCounter));
                    String bookingClass = segmentWiseBookingClassList.get(segmentCounter++);
                    AirSegmentType airSegmentType = getNewSegmentWiseInfo(airSegmentInformation, bookingClass, segIdRefString);
                    segment.add(airSegmentType);
                }

                bound.getSegment().addAll(segment);
                boundList.add(bound);
            }

            bounds.getBound().addAll(boundList);
            rebooking.setBounds(bounds);

            amaTicketRebookAndRepricePNRRQ.setRebooking(rebooking);

        }

        private static void createRePricingRequest(ReIssueConfirmationRequest reIssueConfirmationRequest, List<Journey> segmentsToBeAdded, AMATicketRebookAndRepricePNRRQ amaTicketRebookAndRepricePNRRQ) {

            AMATicketRebookAndRepricePNRRQ.Repricing repricing = new AMATicketRebookAndRepricePNRRQ.Repricing();
            AMATicketRebookAndRepricePNRRQ.Repricing.ItineraryPricingOptions itineraryPricingOptions = new AMATicketRebookAndRepricePNRRQ.Repricing.ItineraryPricingOptions();

            List<PricingOptionType> itineraryPricingOptionList = new ArrayList<>();

            //Setting pax and tickets here
            List<AmadeusPaxRefAndTicket> amadeusPaxRefAndTicketList = reIssueConfirmationRequest.getPaxAndTicketList();
            for (AmadeusPaxRefAndTicket amadeusPaxRefAndTicket : amadeusPaxRefAndTicketList) {
                PricingOptionType paxInfo = new PricingOptionType();


                //Setting Ticket Number here
                PricingOptionBaseType.TicketingInfo ticketingInfo = new PricingOptionBaseType.TicketingInfo();
                ticketingInfo.setNumber(amadeusPaxRefAndTicket.getTicketNumber());
                paxInfo.getTicketingInfo().add(ticketingInfo);

                //Setting Operation here
                PricingOptionBaseType.Booking booking = new PricingOptionBaseType.Booking();
                booking.setOperation("SEL");
                paxInfo.getBooking().add(booking);

                //Setting pax reference here
                PricingOptionType.AssociatedPNRElement associatedPNRElement = new PricingOptionType.AssociatedPNRElement();
                associatedPNRElement.setType("PT");
                associatedPNRElement.setTattoo(amadeusPaxRefAndTicket.getPaxRef());
                paxInfo.getAssociatedPNRElement().add(associatedPNRElement);

                itineraryPricingOptionList.add(paxInfo);
            }

            PricingOptionType pricingRU = new PricingOptionType();
            PricingOptionBaseType.NegotiatedFare negotiatedFareRU = new PricingOptionBaseType.NegotiatedFare();
            negotiatedFareRU.setType("RU");
            pricingRU.getNegotiatedFare().add(negotiatedFareRU);
            itineraryPricingOptionList.add(pricingRU);

            PricingOptionType pricingRP = new PricingOptionType();
            PricingOptionBaseType.NegotiatedFare negotiatedFareRP = new PricingOptionBaseType.NegotiatedFare();
            negotiatedFareRP.setType("RP");
            pricingRP.getNegotiatedFare().add(negotiatedFareRP);
            itineraryPricingOptionList.add(pricingRP);

            if (reIssueConfirmationRequest.isSeaman()) {
                PricingOptionType pricingPTC = new PricingOptionType();
                PricingOptionBaseType.NegotiatedFare negotiatedFarePTC = new PricingOptionBaseType.NegotiatedFare();
                negotiatedFarePTC.setType("PTC");
                pricingPTC.getNegotiatedFare().add(negotiatedFarePTC);
                itineraryPricingOptionList.add(pricingPTC);
            }

//            PricingOptionType pricingRW = new PricingOptionType();
//            PricingOptionBaseType.NegotiatedFare negotiatedFareRW = new PricingOptionBaseType.NegotiatedFare();
//            negotiatedFareRW.setType("RW");
//            pricingRW.getNegotiatedFare().add(negotiatedFareRW);
//            negotiatedFareRW.getCorporate().add("061724");
//            itineraryPricingOptionList.add(pricingRW);

            //Validating carriers set here
            PricingOptionType pricingVC = new PricingOptionType();
            Map<String, PricingOptionBaseType.ServiceProvider> serviceProviderVCMap = new LinkedHashMap<>();
            for (Journey journey : segmentsToBeAdded) {
                List<AirSegmentInformation> airSegmentInformationList = journey.getAirSegmentList();
                for (AirSegmentInformation airSegmentInformation : airSegmentInformationList) {
                    String validatingCarrierCode = airSegmentInformation.getValidatingCarrierCode();
                    if (validatingCarrierCode != null) {
                        PricingOptionBaseType.ServiceProvider serviceProviderVC = new PricingOptionBaseType.ServiceProvider();
                        serviceProviderVC.setType("VC");
                        serviceProviderVC.setValue(validatingCarrierCode);
                        serviceProviderVCMap.putIfAbsent(validatingCarrierCode, serviceProviderVC);
                    }
                }
            }
            pricingVC.getServiceProvider().addAll(serviceProviderVCMap.values());
            itineraryPricingOptionList.add(pricingVC);


            itineraryPricingOptions.getItineraryPricingOption().addAll(itineraryPricingOptionList);

            repricing.setItineraryPricingOptions(itineraryPricingOptions);
            amaTicketRebookAndRepricePNRRQ.setRepricing(repricing);

        }

        private static AirSegmentType getNewSegmentWiseInfo(AirSegmentInformation airSegmentInformation, String bookingClass, String requestId) {

            AirSegmentType airSegmentType = new AirSegmentType();

            //Request ID is being set here
            airSegmentType.setRequestID(requestId);

            //Booking Class for the segment is being set here
            airSegmentType.setBkgClass(bookingClass);

            //Airline Code set here
            AirSegmentType.ServiceProvider serviceProvider = new AirSegmentType.ServiceProvider();
            serviceProvider.setCode(airSegmentInformation.getOperatingCarrierCode());
            airSegmentType.setServiceProvider(serviceProvider);

            //Flight Number set here
            FlightIdentifierType identifier = new FlightIdentifierType();
            identifier.setValue(airSegmentInformation.getFlightNumber());
            airSegmentType.setIdentifier(identifier);

            //Origin Details here
            AirSegmentType.Start start = new AirSegmentType.Start();
            start.setLocationCode(airSegmentInformation.getFromLocation());
            start.setDateTime(mapDateTimeToUTCDate(airSegmentInformation.getDepartureTime()));
            airSegmentType.setStart(start);

            //Destination Details here
            AirSegmentType.End end = new AirSegmentType.End();
            end.setLocationCode(airSegmentInformation.getToLocation());
            end.setDateTime(mapDateTimeToUTCDate(airSegmentInformation.getArrivalTime()));
            airSegmentType.setEnd(end);

            return airSegmentType;
        }

        //Converting date time to their respective zone Dates
        private static String mapDateTimeToUTCDate(String dateTimeString) {

            ZonedDateTime zonedDateTime = ZonedDateTime.parse(dateTimeString, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            ZonedDateTime utcDateTime = zonedDateTime.withZoneSameInstant(ZoneOffset.UTC);

            return utcDateTime.toLocalDate().toString();
        }

    }


    public static TicketReissueConfirmedPricing getTicketReissueConfirmedPricing(List<String> tickets) {
        TicketReissueConfirmedPricing reissueConfirmedPricing = new TicketReissueConfirmedPricing();
        List<TicketReissueConfirmedPricing.TicketInfo> ticketInfos = new ArrayList<>();
        for (String ticket : tickets) {
            com.amadeus.xml.tarcpq_13_2_1a.TicketNumberTypeI ticketNumberTypeI = new com.amadeus.xml.tarcpq_13_2_1a.TicketNumberTypeI();
            com.amadeus.xml.tarcpq_13_2_1a.TicketNumberDetailsTypeI ticketNumberDetailsTypeI = new com.amadeus.xml.tarcpq_13_2_1a.TicketNumberDetailsTypeI();
            ticketNumberDetailsTypeI.setNumber(ticket);
            ticketNumberDetailsTypeI.setType("ET");
            ticketNumberTypeI.setDocumentDetails(ticketNumberDetailsTypeI);
            TicketReissueConfirmedPricing.TicketInfo ticketInfo = new TicketReissueConfirmedPricing.TicketInfo();
            ticketInfo.setPaperticketDetailsFirstCoupon(ticketNumberTypeI);
            ticketInfos.add(ticketInfo);
        }
        reissueConfirmedPricing.getTicketInfo().addAll(ticketInfos);
        return reissueConfirmedPricing;
    }

    public static TicketRepricePNRWithBookingClass getTicketRepricePNRWithBookingClass(TravellerMasterInfo travellerMasterInfo, List<String> tickets) {
        TicketRepricePNRWithBookingClass ticketRepricePNRWithBookingClass = new TicketRepricePNRWithBookingClass();

        //exchangeInformationGroup
        List<TicketRepricePNRWithBookingClass.ExchangeInformationGroup> exchangeInformationGroups = new ArrayList<>();
        int i = 0;
        for (String ticket : tickets) {
            TicketRepricePNRWithBookingClass.ExchangeInformationGroup informationGroup = new TicketRepricePNRWithBookingClass.ExchangeInformationGroup();

            ItemNumberType itemNumberType = new ItemNumberType();
            ItemNumberIdentificationType numberIdentificationType = new ItemNumberIdentificationType();
            numberIdentificationType.setNumber(String.valueOf(i++));
            itemNumberType.getItemNumberDetails().add(numberIdentificationType);
            informationGroup.setTransactionIdentifier(itemNumberType);

            TicketRepricePNRWithBookingClass.ExchangeInformationGroup.DocumentInfoGroup documentInfoGroup = new TicketRepricePNRWithBookingClass.ExchangeInformationGroup.DocumentInfoGroup();
            com.amadeus.xml.taripq_19_1_1a.TicketNumberTypeI ticketNumberTypeI = new com.amadeus.xml.taripq_19_1_1a.TicketNumberTypeI();
            com.amadeus.xml.taripq_19_1_1a.TicketNumberDetailsTypeI ticketNumberDetailsTypeI = new com.amadeus.xml.taripq_19_1_1a.TicketNumberDetailsTypeI();
            ticketNumberDetailsTypeI.setNumber(ticket);
            ticketNumberDetailsTypeI.setType("ET");
            ticketNumberTypeI.setDocumentDetails(ticketNumberDetailsTypeI);
            documentInfoGroup.setPaperticketDetailsLastCoupon(ticketNumberTypeI);
            informationGroup.getDocumentInfoGroup().add(documentInfoGroup);
            exchangeInformationGroups.add(informationGroup);
        }
        ticketRepricePNRWithBookingClass.getExchangeInformationGroup().addAll(exchangeInformationGroups);

        //Pricing Options
        List<TicketRepricePNRWithBookingClass.PricingOption> pricingOptions = new ArrayList<>();
        List<String> pricingOption = Arrays.asList("RP", "RU", "VC", "SEL");
        pricingOption.stream().forEach(option -> {
            TicketRepricePNRWithBookingClass.PricingOption pricingOpt = new TicketRepricePNRWithBookingClass.PricingOption();
            PricingOptionKeyType pricingOptionKeyType = new PricingOptionKeyType();
            pricingOptionKeyType.setPricingOptionKey(option);
            pricingOpt.setPricingOptionKey(pricingOptionKeyType);
            if (option.equals("VC")) {
                //TODO update CarrierInformation
                TransportIdentifierType transportIdentifierType = new TransportIdentifierType();
                CompanyIdentificationTypeI companyIdentificationTypeI = new CompanyIdentificationTypeI();
                companyIdentificationTypeI.setOtherCompany("AI");
                transportIdentifierType.setCompanyIdentification(companyIdentificationTypeI);
                pricingOpt.setCarrierInformation(transportIdentifierType);
            }
            if (option.equals("SEL")) {
                List<ReferencingDetailsType> referencingDetailsTypes = new ArrayList<>();
                ReferencingDetailsType referencingDetailsTypeP = new ReferencingDetailsType();
                referencingDetailsTypeP.setType("P");
                referencingDetailsTypeP.setValue("1");
                ReferencingDetailsType referencingDetailsTypeE = new ReferencingDetailsType();
                referencingDetailsTypeE.setType("E");
                referencingDetailsTypeE.setValue("1");
                referencingDetailsTypes.add(referencingDetailsTypeP);
                referencingDetailsTypes.add(referencingDetailsTypeE);
                ReferenceInfoType referenceInfoType = new ReferenceInfoType();
                referenceInfoType.getReferenceDetails().addAll(referencingDetailsTypes);
                pricingOpt.setPaxSegTstReference(referenceInfoType);
            }
            pricingOptions.add(pricingOpt);
        });
        ticketRepricePNRWithBookingClass.getPricingOption().addAll(pricingOptions);
        return ticketRepricePNRWithBookingClass;
    }


}
