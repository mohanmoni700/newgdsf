package utils;

import com.amadeus.xml.farqnr_07_1_1a.FareCheckRulesReply;
import com.amadeus.xml.itares_05_2_ia.AirSellFromRecommendationReply;
import com.amadeus.xml.pnracc_14_1_1a.*;
import com.amadeus.xml.pnracc_14_1_1a.PNRReply.TravellerInfo;
import com.amadeus.xml.pnracc_14_1_1a.PNRReply.TravellerInfo.PassengerData;
import com.amadeus.xml.pnracc_14_1_1a.PNRReply.OriginDestinationDetails;
import com.amadeus.xml.pnracc_14_1_1a.ReferenceInfoType;
import com.amadeus.xml.tipnrr_12_4_1a.FareInformativePricingWithoutPNRReply;
import com.amadeus.xml.tpcbrr_12_4_1a.*;
import com.amadeus.xml.tpcbrr_12_4_1a.BaggageDetailsTypeI;
import com.amadeus.xml.tpcbrr_12_4_1a.FarePricePNRWithBookingClassReply.FareList;
import com.amadeus.xml.tpcbrr_12_4_1a.FarePricePNRWithBookingClassReply.FareList.TaxInformation;
import com.amadeus.xml.tpcbrr_12_4_1a.ItemNumberIdentificationType;
import com.amadeus.xml.tpcbrr_12_4_1a.ItemNumberType;
import com.amadeus.xml.tpcbrr_12_4_1a.TravelProductInformationTypeI;
import com.amadeus.xml.ttstrr_13_1_1a.*;
import com.amadeus.xml.ttstrr_13_1_1a.ReferencingDetailsTypeI;
import com.amadeus.xml.ttstrr_13_1_1a.TransportIdentifierType;
import com.compassites.GDSWrapper.amadeus.FareRules;
import com.compassites.constants.AmadeusConstants;
import com.compassites.model.*;
import com.compassites.model.traveller.Traveller;
import com.compassites.model.traveller.TravellerMasterInfo;
import com.compassites.model.amadeus.AmadeusPaxInformation;

import dto.*;
import models.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Minutes;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import services.AmadeusSourceOfficeService;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by Yaseen on 18-12-2014.
 */
@Component
public class AmadeusBookingHelper {

    static org.slf4j.Logger logger = LoggerFactory.getLogger("gds");

    private static final String AIR_SEGMENT_QUALIFIER = "AIR";

    @Autowired
    private AmadeusSourceOfficeService amadeusSourceOfficeService;

    private static final Map<String, String> mealCodeNameMap = new HashMap<>();

    private static final Map<String, String> seatNameMap = new HashMap<>();

    static {
        mealCodeNameMap.put("AVML", "VEGETARIAN HINDU MEAL");
        mealCodeNameMap.put("BBML", "BABY MEAL");
        mealCodeNameMap.put("BLML", "BLAND MEAL");
        mealCodeNameMap.put("CHML", "CHILD MEAL");
        mealCodeNameMap.put("CNML", "CHICKEN MEAL (LY SPECIFIC)");
        mealCodeNameMap.put("DBML", "DIABETIC MEAL");
        mealCodeNameMap.put("FPML", "FRUIT PLATTER MEAL");
        mealCodeNameMap.put("FSML", "FISH MEAL");
        mealCodeNameMap.put("GFML", "GLUTEN INTOLERANT MEAL");
        mealCodeNameMap.put("HNML", "HINDU (NON VEGETARIAN) MEAL SPECIFIC");
        mealCodeNameMap.put("IVML", "INDIAN VEGETARIAN MEAL (UA SPECIFIC)");
        mealCodeNameMap.put("JPML", "JAPANESE MEAL (LH SPECIFIC)");
        mealCodeNameMap.put("KSML", "KOSHER MEAL");
        mealCodeNameMap.put("LCML", "LOW CALORIE MEAL");
        mealCodeNameMap.put("LFML", "LOW FAT MEAL");
        mealCodeNameMap.put("LSML", "LOW SALT MEAL");
        mealCodeNameMap.put("MOML", "MOSLEM MEAL");
        mealCodeNameMap.put("NFML", "NO FISH MEAL (LH SPECIFIC)");
        mealCodeNameMap.put("NLML", "LOW LACTOSE MEAL");
        mealCodeNameMap.put("OBML", "JAPANESE OBENTO MEAL (UA SPECIFIC)");
        mealCodeNameMap.put("RVML", "VEGETARIAN RAW MEAL");
        mealCodeNameMap.put("SFML", "SEA FOOD MEAL");
        mealCodeNameMap.put("SPML", "SPECIAL MEAL, SPECIFY FOOD");
        mealCodeNameMap.put("VGML", "VEGETARIAN VEGAN MEAL");
        mealCodeNameMap.put("VJML", "VEGETARIAN JAIN MEAL");
        mealCodeNameMap.put("VOML", "VEGETARIAN ORIENTAL MEAL");
        mealCodeNameMap.put("VLML", "VEGETARIAN LACTO-OVO MEAL");

    }

    static {
        seatNameMap.put("W", "WINDOW");
        seatNameMap.put("A", "ASILE");
    }

    @Autowired
    private FareRules fareRules;

    public static List<TaxDetails> getTaxDetails(FarePricePNRWithBookingClassReply pricePNRReply) {
        List<TaxDetails> taxDetailsList = new ArrayList<>();
        for (FarePricePNRWithBookingClassReply.FareList fareList : pricePNRReply.getFareList()) {
            for (FarePricePNRWithBookingClassReply.FareList.TaxInformation taxInformation : fareList.getTaxInformation()) {
                TaxDetails taxDetails = new TaxDetails();
                taxDetails.setTaxCode(taxInformation.getTaxDetails().getTaxType().getIsoCountry());
                taxDetails.setTaxAmount(new BigDecimal(taxInformation.getAmountDetails().getFareDataMainInformation().getFareAmount()));
                taxDetailsList.add(taxDetails);
            }
        }
        return taxDetailsList;
    }

    public static boolean validateFlightAvailability(AirSellFromRecommendationReply sellFromRecommendation, String amadeusFlightAvailibilityCode) {
        boolean errors = true;
        for (AirSellFromRecommendationReply.ItineraryDetails itinerary : sellFromRecommendation.getItineraryDetails()) {
            for (AirSellFromRecommendationReply.ItineraryDetails.SegmentInformation segmentInformation : itinerary.getSegmentInformation()) {
                for (String statusCode : segmentInformation.getActionDetails().getStatusCode()) {
                    if (!amadeusFlightAvailibilityCode.equals(statusCode)) {
                        errors = false;
                    }
                }
            }
        }
        return errors;
    }

    public static void checkFare(FarePricePNRWithBookingClassReply pricePNRReply, PNRResponse pnrResponse, TravellerMasterInfo travellerMasterInfo) {

        // TODO: re-factor
        int adultCount = 0, childCount = 0, infantCount = 0;
        for (Traveller traveller : travellerMasterInfo.getTravellersList()) {
            PassengerTypeCode passengerType;
            if (traveller.getPassportDetails() != null) {
                passengerType = DateUtility.getPassengerTypeFromDOB(traveller.getPassportDetails().getDateOfBirth());
            } else {
                passengerType = PassengerTypeCode.ADT;
            }
            if (passengerType.equals(PassengerTypeCode.ADT) || passengerType.equals(PassengerTypeCode.SEA)) {
                adultCount++;
            } else if (passengerType.equals(PassengerTypeCode.CHD)) {
                childCount++;
            } else {
                infantCount++;
            }
        }
        BigDecimal totalFare = new BigDecimal(0);
        BigDecimal baseFare = new BigDecimal(0);

        int numberOfTst = (travellerMasterInfo.isSeamen()) ? 1
                : AmadeusBookingHelper.getNumberOfTST(travellerMasterInfo.getTravellersList());
        List<FarePricePNRWithBookingClassReply.FareList> fareList = new ArrayList<>();
        if (pricePNRReply.getFareList().size() != numberOfTst) {
            fareList = pricePNRReply.getFareList().subList(0, numberOfTst);
        } else {
            fareList = pricePNRReply.getFareList();
        }
        PricingInformation pricingInformation = getPricingInfo(fareList, adultCount, childCount, infantCount);

        totalFare = pricingInformation.getTotalPrice();
        baseFare = pricingInformation.getBasePrice();
        pnrResponse.setPricingInfo(pricingInformation);

        BigDecimal searchPrice = new BigDecimal(0);
        if (travellerMasterInfo.isSeamen()) {
            searchPrice = travellerMasterInfo.getItinerary().getSeamanPricingInformation().getTotalPriceValue();
        } else {
            searchPrice = travellerMasterInfo.getItinerary().getPricingInformation().getTotalPriceValue();
        }
        if (totalFare.compareTo(searchPrice) > 0) {
            System.out.println("Changed price is high");
            pnrResponse.setPriceChanged(false);
            pnrResponse.setChangedPriceHigh(true);
            return;
        }

        logger.debug("Total price before comparing :" + searchPrice + " changed Price : " + totalFare);
        if (totalFare.compareTo(searchPrice) == 0) {
            logger.debug("inside comparison 1111111111111111");
            pnrResponse.setPriceChanged(false);
            return;
        }
        pnrResponse.setChangedPrice(totalFare);
        pnrResponse.setChangedBasePrice(baseFare);
        pnrResponse.setOriginalPrice(searchPrice);
        pnrResponse.setPriceChanged(true);
        pnrResponse.setFlightAvailable(true);

    }


    public static AmadeusPaxInformation extractPassengerData(PNRReply.TravellerInfo travellerInfo) {

        AmadeusPaxInformation amadeusPaxInformation = new AmadeusPaxInformation();

        com.amadeus.xml.pnracc_14_1_1a.ElementManagementSegmentType passengerReference = travellerInfo.getElementManagementPassenger();
        String referenceNumber = String.valueOf(passengerReference.getReference().getNumber());
        String lineNumber = String.valueOf(passengerReference.getLineNumber());
        PNRReply.TravellerInfo.PassengerData passengerData = travellerInfo.getPassengerData().get(0);

        String salutation = null;
        String firstName = null;
        String lastName = passengerData.getTravellerInformation().getTraveller().getSurname();
        String fullName = null;

        String firstNameResponse = passengerData.getTravellerInformation().getPassenger().get(0).getFirstName();
        String[] names = firstNameResponse.split("\\s");
        firstName = names[0];

        if (names.length > 0) {
            for (String name : names) {
                if (name.equalsIgnoreCase("Mr") || name.equalsIgnoreCase("Mrs") || name.equalsIgnoreCase("Ms") || name.equalsIgnoreCase("Miss") || name.equalsIgnoreCase("Master") || name.equalsIgnoreCase("Mstr") || name.equalsIgnoreCase("Capt")) {
                    salutation = WordUtils.capitalizeFully(name);
                }
            }
        }

        if (salutation != null) {
            fullName = salutation + firstName;
        } else {
            fullName = firstName;
        }

        for (String name : names) {
            if (!name.equalsIgnoreCase(salutation) && !name.equalsIgnoreCase(firstName)) {
                fullName += name;
            }
        }

        fullName += lastName;

        amadeusPaxInformation.setSalutation(salutation);
        amadeusPaxInformation.setFirstName(firstName);
        amadeusPaxInformation.setLastName(lastName);
        amadeusPaxInformation.setFullName(fullName.replace(" ", ""));
        amadeusPaxInformation.setPaxRef(referenceNumber);
        amadeusPaxInformation.setLineNumber(lineNumber);

        return amadeusPaxInformation;
    }

    public static boolean createOfflineTickets(IssuanceResponse issuanceResponse, IssuanceRequest issuanceRequest, PNRReply gdsPNRReply) {
        int ticketsCount = 0;
        Map<String, Object> airSegmentRefMap = new HashMap<>();
        Map<String, Object> travellerMap = new HashMap<>();
        Map<String, String> segmentSequenceMap = new HashMap<>();
        int segmentSequence = 1;
        //String segmentRef = "";
        String key1 = "";
        Object[] travelArray = null;
        for (PNRReply.OriginDestinationDetails originDestination : gdsPNRReply.getOriginDestinationDetails()) {
            for (PNRReply.OriginDestinationDetails.ItineraryInfo itineraryInfo : originDestination.getItineraryInfo()) {
                if (AIR_SEGMENT_QUALIFIER.equalsIgnoreCase(itineraryInfo.getElementManagementItinerary().getSegmentName())) {
                    String segmentRef = itineraryInfo.getElementManagementItinerary().getReference().getQualifier() + itineraryInfo.getElementManagementItinerary().getReference().getNumber();
                    airSegmentRefMap.put(segmentRef, itineraryInfo);
                    segmentSequenceMap.put(segmentRef, segmentSequence + "");
                    segmentSequence++;
                }
            }
        }
        for (PNRReply.TravellerInfo travellerInfo : gdsPNRReply.getTravellerInfo()) {
            key1 = travellerInfo.getElementManagementPassenger().getReference().getQualifier() + travellerInfo.getElementManagementPassenger().getReference().getNumber();
            travellerMap.put(key1, travellerInfo);
        }
        for (PNRReply.DataElementsMaster.DataElementsIndiv dataElementsDiv : gdsPNRReply.getDataElementsMaster().getDataElementsIndiv()) {
            if ("FHM".equals(dataElementsDiv.getElementManagementData().getSegmentName())) {
                String passengerRef = "";
                List<String> segmentRefList = new ArrayList<>();
                String travellerKey = "";
                // int count = dataElementsDiv.getReferenceForDataElement().getReference().size();

                if (dataElementsDiv.getReferenceForDataElement() != null) {
                    for (ReferencingDetailsType111975C reference : dataElementsDiv.getReferenceForDataElement().getReference()) {
                        travellerKey = reference.getQualifier() + reference.getNumber();
                        if (travellerMap.containsKey(travellerKey)) {
                            passengerRef = travellerKey;
                            //segmentRefList.add(travellerKey);
                        } else {
                            segmentRefList.add(travellerKey);
                        }
                    }
                } else {
                    passengerRef = key1;
                    segmentRefList.addAll(airSegmentRefMap.keySet());
                }
                PNRReply.TravellerInfo traveller = null;
                // Fix for one way multi tst ticket not getting uploaded
                if (passengerRef.isEmpty()) {
                    travelArray = (Object[]) travellerMap.keySet().toArray();
                    for (int i = 0; i < travelArray.length; i++) {
                        passengerRef = (String) travelArray[i];
                    }
                }
                traveller = (PNRReply.TravellerInfo) travellerMap.get(passengerRef);

                String lastName = traveller.getPassengerData().get(0).getTravellerInformation().getTraveller().getSurname();
                String name1 = traveller.getPassengerData().get(0).getTravellerInformation().getPassenger().get(0).getFirstName();
                String[] names = name1.split("\\s");
                String fstName = "";
                for (int i = 0; i < names.length - 1; i++) {
                    fstName = fstName + " " + names[i];
                }
                String name = fstName.trim();
                String ticketText = dataElementsDiv.getOtherDataFreetext().get(0).getLongFreetext();
                String infantIndicator = ticketText.substring(0, 3);

                //checking for infant passenger associated to adult
                if ("inf".equalsIgnoreCase(infantIndicator)) {
                    //passenger association when booked from online system
                    if (traveller.getPassengerData().size() > 1) {
                        String type = traveller.getPassengerData().get(1).getTravellerInformation().getPassenger().get(0).getType();
                        if ("inf".equalsIgnoreCase(type)) {
                            lastName = traveller.getPassengerData().get(1).getTravellerInformation().getTraveller().getSurname();
                            name = traveller.getPassengerData().get(1).getTravellerInformation().getPassenger().get(0).getFirstName();
                        }
                        //passenger association when booked from offline system
                    } else if (traveller.getPassengerData().get(0).getTravellerInformation().getPassenger().size() > 1) {
                        String type = traveller.getPassengerData().get(0).getTravellerInformation().getPassenger().get(1).getType();
                        if ("inf".equalsIgnoreCase(type)) {
                            lastName = traveller.getPassengerData().get(0).getTravellerInformation().getTraveller().getSurname();
                            name = traveller.getPassengerData().get(0).getTravellerInformation().getPassenger().get(1).getFirstName();
                        }
                    }
                }


               /* String[] nameArray = name.split(" ");
                String firstName = nameArray[0];
                String middleName = (nameArray.length > 1)? nameArray[1]: "";*/

                name = name.replaceAll("\\s+", "");
                int len = names.length - 1;
                if (!"inf".equalsIgnoreCase(infantIndicator)) {
                    if (name.equalsIgnoreCase("FNU")) {
                        name = names[len];
                    } else {
                        String salutatio = "";
                        if ("Ms".equalsIgnoreCase(names[len])) {
                            salutatio = "Mrs";
                        } else {
                            salutatio = names[len];
                        }
                        name = name + salutatio;
                    }
                }
                lastName = lastName.replaceAll("\\s+", "");
                if (lastName.equalsIgnoreCase("LNU")) {
                    lastName = "";
                }

                for (Traveller traveller1 : issuanceRequest.getTravellerList()) {
                    String contactName = "";
                    if (traveller1.getPersonalDetails().getMiddleName() != null) {
                        contactName = traveller1.getPersonalDetails().getFirstName() + traveller1.getPersonalDetails().getMiddleName();

                    } else {
                        contactName = traveller1.getPersonalDetails().getFirstName();
                    }
                    String salutation = traveller1.getPersonalDetails().getSalutation();
                    /*if("master".equalsIgnoreCase(traveller1.getPersonalDetails().getSalutation())){
                        salutation = "MSTR";
                    } else {
                        salutation = traveller1.getPersonalDetails().getSalutation();
                    }*/
                    if ("Ms".equalsIgnoreCase(salutation)) {
                        salutation = "Mrs";
                    }
                    contactName = contactName + salutation;
                    contactName = contactName.replaceAll("\\s+", "").replaceAll("\\.", "");
                    String contactLastName = traveller1.getPersonalDetails().getLastName();
                    contactLastName = contactLastName.replaceAll("\\s+", "");
                    if (name.equalsIgnoreCase(contactName)
                            && lastName.equalsIgnoreCase(contactLastName)) {
                        String freeText = "";
                        if ("inf".equalsIgnoreCase(infantIndicator)) {
                            if (dataElementsDiv.getOtherDataFreetext().get(0).getLongFreetext().toLowerCase().indexOf("INF".toLowerCase()) != -1) {
                                String freeText1[] = dataElementsDiv.getOtherDataFreetext().get(0).getLongFreetext().split("F", 3);
                                freeText = freeText1[1];
                            } else {
                                freeText = dataElementsDiv.getOtherDataFreetext().get(0).getLongFreetext();
                            }
                        } else {
                            freeText = dataElementsDiv.getOtherDataFreetext().get(0).getLongFreetext();
                        }
                        // String freeText = dataElementsDiv.getOtherDataFreetext().get(0).getLongFreetext();
                        String[] freeTextArr = freeText.split("/");
                        String ticketNumber = freeTextArr[0];
                        Map<String, String> ticketMap;
                        if (traveller1.getTicketNumberMap() != null) {
                            ticketMap = traveller1.getTicketNumberMap();
                        } else {
                            ticketMap = new HashMap<>();
                        }
                        //if(segmentRefList.size() != 0) {
                        for (String segmentRef : segmentRefList) {
                            PNRReply.OriginDestinationDetails.ItineraryInfo itineraryInfo = (PNRReply.OriginDestinationDetails.ItineraryInfo) airSegmentRefMap.get(segmentRef);
                            if (Objects.nonNull(itineraryInfo.getTravelProduct()) && Objects.nonNull(itineraryInfo.getTravelProduct().getBoardpointDetail())
                                    && Objects.nonNull(itineraryInfo.getTravelProduct().getOffpointDetail())) {
                                String key = itineraryInfo.getTravelProduct().getBoardpointDetail().getCityCode() +
                                        itineraryInfo.getTravelProduct().getOffpointDetail().getCityCode() + traveller1.getContactId()
                                        + segmentSequenceMap.get(segmentRef);
                                ticketMap.put(key.toLowerCase(), ticketNumber);
                                logger.debug("created ticket for " + key + "ticket count " + ticketsCount);
                            }
                        }
                        /*} else {
                            PNRReply.OriginDestinationDetails.ItineraryInfo itineraryInfo = (PNRReply.OriginDestinationDetails.ItineraryInfo) airSegmentRefMap.get(passengerRef);
                            String key = itineraryInfo.getTravelProduct().getBoardpointDetail().getCityCode() +
                                    itineraryInfo.getTravelProduct().getOffpointDetail().getCityCode() + traveller1.getContactId()
                                    + segmentSequenceMap.get(passengerRef);
                            ticketMap.put(key.toLowerCase(), ticketNumber);
                        }*/
                        traveller1.setTicketNumberMap(ticketMap);
                    }
                }
                ticketsCount++;
                issuanceResponse.setTravellerList(issuanceRequest.getTravellerList());
                issuanceResponse.setSuccess(true);
            }
        }
        int passengerCount = 0;
        if (issuanceRequest.isSeamen()) {
            passengerCount = issuanceRequest.getAdultCount() + issuanceRequest.getChildCount() + issuanceRequest.getInfantCount();

        } else {
            passengerCount = issuanceRequest.getAdultCount() + issuanceRequest.getChildCount();
        }
        logger.debug("ticketCount : " + ticketsCount + " passengerCount " + passengerCount);
        if (ticketsCount >= passengerCount) {
            return true;
        }

        return false;
    }

    public static boolean createOfflineTicketsinFHE(IssuanceResponse issuanceResponse, IssuanceRequest issuanceRequest, PNRReply gdsPNRReply) {
        int ticketsCount = 0;
        Map<String, Object> airSegmentRefMap = new HashMap<>();
        Map<String, Object> travellerMap = new HashMap<>();
        Map<String, String> segmentSequenceMap = new HashMap<>();
        int segmentSequence = 1;
        //String segmentRef = "";
        String key1 = "";
        Object[] travelArray = null;
        for (PNRReply.OriginDestinationDetails originDestination : gdsPNRReply.getOriginDestinationDetails()) {
            for (PNRReply.OriginDestinationDetails.ItineraryInfo itineraryInfo : originDestination.getItineraryInfo()) {
                if (AIR_SEGMENT_QUALIFIER.equalsIgnoreCase(itineraryInfo.getElementManagementItinerary().getSegmentName())) {
                    String segmentRef = itineraryInfo.getElementManagementItinerary().getReference().getQualifier() + itineraryInfo.getElementManagementItinerary().getReference().getNumber();
                    airSegmentRefMap.put(segmentRef, itineraryInfo);
                    segmentSequenceMap.put(segmentRef, segmentSequence + "");
                    segmentSequence++;
                }
            }
        }
        for (PNRReply.TravellerInfo travellerInfo : gdsPNRReply.getTravellerInfo()) {
            key1 = travellerInfo.getElementManagementPassenger().getReference().getQualifier() + travellerInfo.getElementManagementPassenger().getReference().getNumber();
            travellerMap.put(key1, travellerInfo);
        }
        for (PNRReply.DataElementsMaster.DataElementsIndiv dataElementsDiv : gdsPNRReply.getDataElementsMaster().getDataElementsIndiv()) {
            if ("FHE".equals(dataElementsDiv.getElementManagementData().getSegmentName())) {
                String passengerRef = "";
                List<String> segmentRefList = new ArrayList<>();
                String travellerKey = "";
                // int count = dataElementsDiv.getReferenceForDataElement().getReference().size();

                if (dataElementsDiv.getReferenceForDataElement() != null) {
                    for (ReferencingDetailsType111975C reference : dataElementsDiv.getReferenceForDataElement().getReference()) {
                        travellerKey = reference.getQualifier() + reference.getNumber();
                        if (travellerMap.containsKey(travellerKey)) {
                            passengerRef = travellerKey;
                            //segmentRefList.add(travellerKey);
                        } else {
                            segmentRefList.add(travellerKey);
                        }
                    }
                } else {
                    passengerRef = key1;
                }
                PNRReply.TravellerInfo traveller = null;
                // Fix for one way multi tst ticket not getting uploaded
                if (passengerRef.isEmpty()) {
                    travelArray = (Object[]) travellerMap.keySet().toArray();
                    for (int i = 0; i < travelArray.length; i++) {
                        passengerRef = (String) travelArray[i];
                    }
                }
                traveller = (PNRReply.TravellerInfo) travellerMap.get(passengerRef);

                String lastName = traveller.getPassengerData().get(0).getTravellerInformation().getTraveller().getSurname();
                String name1 = traveller.getPassengerData().get(0).getTravellerInformation().getPassenger().get(0).getFirstName();
                String[] names = name1.split("\\s");
                String fstName = "";
                for (int i = 0; i < names.length - 1; i++) {
                    fstName = fstName + " " + names[i];
                }
                String name = fstName.trim();
                String ticketText = dataElementsDiv.getOtherDataFreetext().get(0).getLongFreetext();
                String infantIndicator = ticketText.substring(0, 3);

                //checking for infant passenger associated to adult
                if ("inf".equalsIgnoreCase(infantIndicator)) {
                    //passenger association when booked from online system
                    if (traveller.getPassengerData().size() > 1) {
                        String type = traveller.getPassengerData().get(1).getTravellerInformation().getPassenger().get(0).getType();
                        if ("inf".equalsIgnoreCase(type)) {
                            lastName = traveller.getPassengerData().get(1).getTravellerInformation().getTraveller().getSurname();
                            name = traveller.getPassengerData().get(1).getTravellerInformation().getPassenger().get(0).getFirstName();
                        }
                        //passenger association when booked from offline system
                    } else if (traveller.getPassengerData().get(0).getTravellerInformation().getPassenger().size() > 1) {
                        String type = traveller.getPassengerData().get(0).getTravellerInformation().getPassenger().get(1).getType();
                        if ("inf".equalsIgnoreCase(type)) {
                            lastName = traveller.getPassengerData().get(0).getTravellerInformation().getTraveller().getSurname();
                            name = traveller.getPassengerData().get(0).getTravellerInformation().getPassenger().get(1).getFirstName();
                        }
                    }
                }


               /* String[] nameArray = name.split(" ");
                String firstName = nameArray[0];
                String middleName = (nameArray.length > 1)? nameArray[1]: "";*/

                name = name.replaceAll("\\s+", "");
                int len = names.length - 1;
                if (!"inf".equalsIgnoreCase(infantIndicator)) {
                    if (name.equalsIgnoreCase("FNU")) {
                        name = names[len];
                    } else {
                        String salutatio = "";
                        if ("Ms".equalsIgnoreCase(names[len])) {
                            salutatio = "Mrs";
                        } else {
                            salutatio = names[len];
                        }
                        name = name + salutatio;
                    }
                }
                lastName = lastName.replaceAll("\\s+", "");
                if (lastName.equalsIgnoreCase("LNU")) {
                    lastName = "";
                }

                for (Traveller traveller1 : issuanceRequest.getTravellerList()) {
                    String contactName = "";
                    if (traveller1.getPersonalDetails().getMiddleName() != null) {
                        contactName = traveller1.getPersonalDetails().getFirstName() + traveller1.getPersonalDetails().getMiddleName();

                    } else {
                        contactName = traveller1.getPersonalDetails().getFirstName();
                    }
                    String salutation = traveller1.getPersonalDetails().getSalutation();
                    ;
                    /*if("master".equalsIgnoreCase(traveller1.getPersonalDetails().getSalutation())){
                        salutation = "MSTR";
                    } else {
                        salutation = traveller1.getPersonalDetails().getSalutation();
                    }*/
                    if ("Ms".equalsIgnoreCase(salutation)) {
                        salutation = "Mrs";
                    }
                    contactName = contactName + salutation;
                    contactName = contactName.replaceAll("\\s+", "").replaceAll("\\.", "");
                    String contactLastName = traveller1.getPersonalDetails().getLastName();
                    contactLastName = contactLastName.replaceAll("\\s+", "");
                    if (name.equalsIgnoreCase(contactName)
                            && lastName.equalsIgnoreCase(contactLastName)) {
                        String freeText = "";
                        if ("inf".equalsIgnoreCase(infantIndicator)) {
                            if (dataElementsDiv.getOtherDataFreetext().get(0).getLongFreetext().toLowerCase().indexOf("INF".toLowerCase()) != -1) {
                                String freeText1[] = dataElementsDiv.getOtherDataFreetext().get(0).getLongFreetext().split("F", 3);
                                freeText = freeText1[1];
                            } else {
                                freeText = dataElementsDiv.getOtherDataFreetext().get(0).getLongFreetext();
                            }
                        } else {
                            freeText = dataElementsDiv.getOtherDataFreetext().get(0).getLongFreetext();
                        }
                        String[] freeTextArr = freeText.split("/");
                        String ticketNumber = freeTextArr[0].trim();
                        Map<String, String> ticketMap;
                        if (traveller1.getTicketNumberMap() != null) {
                            ticketMap = traveller1.getTicketNumberMap();
                        } else {
                            ticketMap = new HashMap<>();
                        }
                        //if(segmentRefList.size() != 0) {
                        for (String segmentRef : segmentRefList) {
                            PNRReply.OriginDestinationDetails.ItineraryInfo itineraryInfo = (PNRReply.OriginDestinationDetails.ItineraryInfo) airSegmentRefMap.get(segmentRef);
                            if (Objects.nonNull(itineraryInfo.getTravelProduct()) && Objects.nonNull(itineraryInfo.getTravelProduct().getBoardpointDetail())
                                    && Objects.nonNull(itineraryInfo.getTravelProduct().getOffpointDetail())) {
                                String key = itineraryInfo.getTravelProduct().getBoardpointDetail().getCityCode() +
                                        itineraryInfo.getTravelProduct().getOffpointDetail().getCityCode() + traveller1.getContactId()
                                        + segmentSequenceMap.get(segmentRef);
                                ticketMap.put(key.toLowerCase(), ticketNumber);
                                logger.debug("created ticket for " + key + "ticket count " + ticketsCount);
                            }
                        }
                        /*} else {
                            PNRReply.OriginDestinationDetails.ItineraryInfo itineraryInfo = (PNRReply.OriginDestinationDetails.ItineraryInfo) airSegmentRefMap.get(passengerRef);
                            String key = itineraryInfo.getTravelProduct().getBoardpointDetail().getCityCode() +
                                    itineraryInfo.getTravelProduct().getOffpointDetail().getCityCode() + traveller1.getContactId()
                                    + segmentSequenceMap.get(passengerRef);
                            ticketMap.put(key.toLowerCase(), ticketNumber);
                        }*/
                        traveller1.setTicketNumberMap(ticketMap);
                    }
                }
                ticketsCount++;
                issuanceResponse.setTravellerList(issuanceRequest.getTravellerList());
                issuanceResponse.setSuccess(true);
            }
        }
        int passengerCount = 0;
        if (issuanceRequest.isSeamen()) {
            passengerCount = issuanceRequest.getAdultCount() + issuanceRequest.getChildCount() + issuanceRequest.getInfantCount();

        } else {
            passengerCount = issuanceRequest.getAdultCount() + issuanceRequest.getChildCount();
        }
        logger.debug("ticketCount : " + ticketsCount + " passengerCount " + passengerCount);
        if (ticketsCount >= passengerCount) {
            return true;
        }

        return false;
    }

    public static boolean createTickets(IssuanceResponse issuanceResponse, IssuanceRequest issuanceRequest, PNRReply gdsPNRReply) {
        int ticketsCount = 0;
        Map<String, Object> airSegmentRefMap = new HashMap<>();
        Map<String, Object> travellerMap = new HashMap<>();
        Map<String, String> segmentSequenceMap = new HashMap<>();
        int segmentSequence = 1;
        for (PNRReply.OriginDestinationDetails originDestination : gdsPNRReply.getOriginDestinationDetails()) {
            for (PNRReply.OriginDestinationDetails.ItineraryInfo itineraryInfo : originDestination.getItineraryInfo()) {
                String segmentRef1 = itineraryInfo.getElementManagementItinerary().getReference().getQualifier() + itineraryInfo.getElementManagementItinerary().getReference().getNumber();
                airSegmentRefMap.put(segmentRef1, itineraryInfo);
                segmentSequenceMap.put(segmentRef1, segmentSequence + "");
                segmentSequence++;
            }
        }
        for (PNRReply.TravellerInfo travellerInfo : gdsPNRReply.getTravellerInfo()) {
            String key = travellerInfo.getElementManagementPassenger().getReference().getQualifier() + travellerInfo.getElementManagementPassenger().getReference().getNumber();
            travellerMap.put(key, travellerInfo);
        }
        for (PNRReply.DataElementsMaster.DataElementsIndiv dataElementsDiv : gdsPNRReply.getDataElementsMaster().getDataElementsIndiv()) {
            if ("FA".equals(dataElementsDiv.getElementManagementData().getSegmentName())) {
                String passengerRef = "";
                List<String> segmentRefList1 = new ArrayList<>();
                String travellerKey = "";
                for (ReferencingDetailsType111975C reference : dataElementsDiv.getReferenceForDataElement().getReference()) {
                    travellerKey = reference.getQualifier() + reference.getNumber();
                    if (travellerMap.containsKey(travellerKey)) {
                        passengerRef = travellerKey;
                    } else {
                        segmentRefList1.add(travellerKey);
                    }
                }
                PNRReply.TravellerInfo traveller = (PNRReply.TravellerInfo) travellerMap.get(passengerRef);
                String lastName = traveller.getPassengerData().get(0).getTravellerInformation().getTraveller().getSurname();
                String name1 = traveller.getPassengerData().get(0).getTravellerInformation().getPassenger().get(0).getFirstName();
                String[] names = name1.split("\\s");
                String fstName = "";
                for (int i = 0; i < names.length - 1; i++) {
                    fstName = fstName + " " + names[i];
                }
                String name = fstName.trim();

                String ticketText = dataElementsDiv.getOtherDataFreetext().get(0).getLongFreetext();
                String infantIndicator = ticketText.substring(0, 3);

                //checking for infant passenger associated to adult
                if ("inf".equalsIgnoreCase(infantIndicator)) {
                    //passenger association when booked from online system
                    if (traveller.getPassengerData().size() > 1) {
                        String type = traveller.getPassengerData().get(1).getTravellerInformation().getPassenger().get(0).getType();
                        if ("inf".equalsIgnoreCase(type)) {
                            lastName = traveller.getPassengerData().get(1).getTravellerInformation().getTraveller().getSurname();
                            name = traveller.getPassengerData().get(1).getTravellerInformation().getPassenger().get(0).getFirstName();
                        }
                        //passenger association when booked from offline system
                    } else if (traveller.getPassengerData().get(0).getTravellerInformation().getPassenger().size() > 1) {
                        String type = traveller.getPassengerData().get(0).getTravellerInformation().getPassenger().get(1).getType();
                        if ("inf".equalsIgnoreCase(type)) {
                            lastName = traveller.getPassengerData().get(0).getTravellerInformation().getTraveller().getSurname();
                            name = traveller.getPassengerData().get(0).getTravellerInformation().getPassenger().get(1).getFirstName();
                        }
                    }
                }


               /* String[] nameArray = name.split(" ");
                String firstName = nameArray[0];
                String middleName = (nameArray.length > 1)? nameArray[1]: "";*/

                name = name.replaceAll("\\s+", "");
                int len = names.length - 1;
                if (!"inf".equalsIgnoreCase(infantIndicator)) {
                    if (name.equalsIgnoreCase("FNU")) {
                        name = names[len];
                    } else {
                        String salutatio = "";
                        if ("Ms".equalsIgnoreCase(names[len])) {
                            salutatio = "Mrs";
                        } else {
                            salutatio = names[len];
                        }
                        name = name + salutatio;
                    }
                }
                lastName = lastName.replaceAll("\\s+", "");
                if (lastName.equalsIgnoreCase("LNU")) {
                    lastName = "";
                }
                for (Traveller traveller1 : issuanceRequest.getTravellerList()) {
                    String contactName = "";
                    if (traveller1.getPersonalDetails().getMiddleName() != null) {
                        contactName = traveller1.getPersonalDetails().getFirstName() + traveller1.getPersonalDetails().getMiddleName();

                    } else {
                        contactName = traveller1.getPersonalDetails().getFirstName();
                    }
                    String salutation = traveller1.getPersonalDetails().getSalutation();
                    /*if("master".equalsIgnoreCase(traveller1.getPersonalDetails().getSalutation())){
                        salutation = "MSTR";
                    } else {
                        salutation = traveller1.getPersonalDetails().getSalutation();
                    }*/
                    if ("Ms".equalsIgnoreCase(salutation)) {
                        salutation = "Mrs";
                    }
                    contactName = contactName + salutation;
                    contactName = contactName.replaceAll("\\s+", "").replaceAll("\\.", "");
                    String contactLastName = traveller1.getPersonalDetails().getLastName();
                    contactLastName = contactLastName.replaceAll("\\s+", "");
                    if (name.equalsIgnoreCase(contactName)
                            && lastName.equalsIgnoreCase(contactLastName)) {
                        //String freeText = dataElementsDiv.getOtherDataFreetext().get(0).getLongFreetext();
                        String freeText = "";
                        if ("inf".equalsIgnoreCase(infantIndicator)) {
                            if (dataElementsDiv.getOtherDataFreetext().get(0).getLongFreetext().toLowerCase().indexOf("INF".toLowerCase()) != -1) {
                                String freeText1[] = dataElementsDiv.getOtherDataFreetext().get(0).getLongFreetext().split("F", 3);
                                freeText = freeText1[1];
                            } else {
                                freeText = dataElementsDiv.getOtherDataFreetext().get(0).getLongFreetext();
                            }
                        } else {
                            freeText = dataElementsDiv.getOtherDataFreetext().get(0).getLongFreetext();
                        }
                        String[] freeTextArr = freeText.split("/");
                        String ticketNumber = freeTextArr[0].substring(3);
                        Map<String, String> ticketMap;
                        if (traveller1.getTicketNumberMap() != null) {
                            ticketMap = traveller1.getTicketNumberMap();
                        } else {
                            ticketMap = new HashMap<>();
                        }
                        for (String segmentRef : segmentRefList1) {
                            PNRReply.OriginDestinationDetails.ItineraryInfo itineraryInfo = (PNRReply.OriginDestinationDetails.ItineraryInfo) airSegmentRefMap.get(segmentRef);
                            if (itineraryInfo != null) {
                                String segType = itineraryInfo.getElementManagementItinerary().getSegmentName();
                                if (segType != null && segType.equalsIgnoreCase("AIR")) {
                                    String key = itineraryInfo.getTravelProduct().getBoardpointDetail().getCityCode() +
                                            itineraryInfo.getTravelProduct().getOffpointDetail().getCityCode() + traveller1.getContactId()
                                            + segmentSequenceMap.get(segmentRef);
                                    ticketMap.put(key.toLowerCase(), ticketNumber);
                                    logger.debug("created ticket for " + key + "ticket count " + ticketsCount);
                                }
                            }
                        }
                        traveller1.setTicketNumberMap(ticketMap);
                    }
                }
                ticketsCount++;
                issuanceResponse.setTravellerList(issuanceRequest.getTravellerList());
                issuanceResponse.setSuccess(true);
            }
        }
        int passengerCount = 0;
        if (issuanceRequest.isSeamen()) {
            passengerCount = issuanceRequest.getAdultCount() + issuanceRequest.getChildCount() + issuanceRequest.getInfantCount();

        } else {
            passengerCount = issuanceRequest.getAdultCount() + issuanceRequest.getChildCount();
        }
        logger.debug("ticketCount : " + ticketsCount + " passengerCount " + passengerCount);
        if (ticketsCount >= passengerCount) {
            return true;
        }

        return false;
    }


    public static List<Journey> getJourneyListFromPNRResponse(PNRReply gdsPNRReply, RedisTemplate redisTemplate) {

        List<Journey> journeyList = new ArrayList<>();
        List<AirSegmentInformation> airSegmentList = new ArrayList<>();

        Journey journey = new Journey();
        for (PNRReply.OriginDestinationDetails originDestinationDetails : gdsPNRReply
                .getOriginDestinationDetails()) {
            for (PNRReply.OriginDestinationDetails.ItineraryInfo itineraryInfo : originDestinationDetails
                    .getItineraryInfo()) {
                String segType = itineraryInfo.getElementManagementItinerary().getSegmentName();
                if (segType.equalsIgnoreCase("AIR")) {
                    AirSegmentInformation airSegmentInformation = new AirSegmentInformation();

                    ElementManagementSegmentType elementManagementItinerary = itineraryInfo.getElementManagementItinerary();
                    ReferencingDetailsType127526C reference = elementManagementItinerary.getReference();
                    airSegmentInformation.setAmadeusSegmentQualifier(reference.getQualifier());
                    airSegmentInformation.setAmadeusSegmentRefNumber(reference.getNumber());

                    String fromLoc = itineraryInfo.getTravelProduct()
                            .getBoardpointDetail().getCityCode();
                    String toLoc = itineraryInfo.getTravelProduct()
                            .getOffpointDetail().getCityCode();
                    airSegmentInformation.setFromLocation(fromLoc);
                    airSegmentInformation.setToLocation(toLoc);
                    airSegmentInformation.setBookingClass(itineraryInfo.getTravelProduct() != null ? itineraryInfo.getTravelProduct().getProductDetails().getClassOfService() : "");
                    airSegmentInformation.setCabinClass(itineraryInfo.getCabinDetails() != null ? itineraryInfo.getCabinDetails().getCabinDetails().getClassDesignator() : "");

                    Airport fromAirport;
                    Airport toAirport;
                    if (redisTemplate == null) {
                        fromAirport = Airport.getAirportByIataCode(airSegmentInformation.getFromLocation());
                        toAirport = Airport.getAirportByIataCode(airSegmentInformation.getToLocation());
                    } else {
                        fromAirport = Airport.getAirport(airSegmentInformation.getFromLocation(), redisTemplate);
                        toAirport = Airport.getAirport(airSegmentInformation.getToLocation(), redisTemplate);
                    }

                    SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyHHmm");
                    String DATE_FORMAT = "ddMMyyHHmm";
                    DateTimeFormatter DATETIME_FORMATTER = DateTimeFormat
                            .forPattern(DATE_FORMAT);
                    DateTimeZone dateTimeZone = DateTimeZone.forID(fromAirport
                            .getTime_zone());
                    DateTimeZone toDateTimeZone = DateTimeZone.forID(toAirport
                            .getTime_zone());
                    String fromDateTime = itineraryInfo.getTravelProduct()
                            .getProduct().getArrDate()
                            + itineraryInfo.getTravelProduct().getProduct()
                            .getArrTime();
                    String toDateTime = itineraryInfo.getTravelProduct()
                            .getProduct().getDepDate()
                            + itineraryInfo.getTravelProduct().getProduct()
                            .getDepTime();
                    DateTime departureDate = DATETIME_FORMATTER.withZone(
                            dateTimeZone).parseDateTime(toDateTime);
                    // dateTimeZone = DateTimeZone.forID(toAirport.getTime_zone());
                    DateTime arrivalDate = DATETIME_FORMATTER.withZone(
                            toDateTimeZone).parseDateTime(fromDateTime);

                    airSegmentInformation.setFlightNumber(itineraryInfo
                            .getTravelProduct().getProductDetails()
                            .getIdentification());


                    airSegmentInformation.setDepartureDate(departureDate
                            .toDate());
                    airSegmentInformation.setDepartureTime(departureDate
                            .toString());
                    airSegmentInformation
                            .setArrivalTime(arrivalDate.toString());
                    airSegmentInformation.setArrivalDate(arrivalDate.toDate());

                    airSegmentInformation.setFromDate(fromDateTime);
                    airSegmentInformation.setToDate(toDateTime);

                    if (itineraryInfo.getFlightDetail() != null
                            && itineraryInfo.getFlightDetail()
                            .getDepartureInformation() != null) {
                        airSegmentInformation.setFromTerminal(itineraryInfo
                                .getFlightDetail().getDepartureInformation()
                                .getDepartTerminal());
                    }
                    if (itineraryInfo.getFlightDetail() != null
                            && itineraryInfo.getFlightDetail()
                            .getArrivalStationInfo() != null) {
                        airSegmentInformation.setToTerminal(itineraryInfo
                                .getFlightDetail().getArrivalStationInfo().getTerminal());
                    }
                    airSegmentInformation.setCarrierCode(itineraryInfo.getTravelProduct().getCompanyDetail().getIdentification());
                    if (redisTemplate == null) {
                        airSegmentInformation.setFromAirport(Airport.getAirportByIataCode(fromLoc));
                        airSegmentInformation.setToAirport(Airport.getAirportByIataCode(toLoc));
                        airSegmentInformation.setAirline(Airline.getAirlineByIataCode(itineraryInfo.getTravelProduct().getCompanyDetail().getIdentification()));
                    } else {
                        airSegmentInformation.setFromAirport(Airport.getAirport(fromLoc, redisTemplate));
                        airSegmentInformation.setToAirport(Airport.getAirport(toLoc, redisTemplate));
                        airSegmentInformation.setAirline(Airline.getAirlineByCode(itineraryInfo.getTravelProduct().getCompanyDetail().getIdentification(), redisTemplate));
                    }
                    String responseEquipment = itineraryInfo.getFlightDetail()
                            .getProductDetails().getEquipment();
                    if (responseEquipment != null) {
                        itineraryInfo.getFlightDetail().getProductDetails()
                                .getEquipment();
                    }
                    airSegmentInformation.setEquipment(responseEquipment);

                    //duration
                    DateTimeFormatter DATETIME_FORMATTER1 = DateTimeFormat.forPattern(DATE_FORMAT);
                    DateTimeZone dateTimeZone1 = DateTimeZone.forID(fromAirport.getTime_zone());
                    DateTimeZone toDateTimeZone1 = DateTimeZone.forID(toAirport.getTime_zone());
                    DateTime departureDate1 = DATETIME_FORMATTER1.withZone(dateTimeZone1).parseDateTime(toDateTime);
                    dateTimeZone = DateTimeZone.forID(toAirport.getTime_zone());
                    DateTime arrivalDate1 = DATETIME_FORMATTER1.withZone(toDateTimeZone1).parseDateTime(fromDateTime);

                    airSegmentInformation.setDepartureDate(departureDate1.toDate());
                    airSegmentInformation.setDepartureTime(departureDate1.toString());
                    airSegmentInformation.setArrivalTime(arrivalDate1.toString());
                    airSegmentInformation.setArrivalDate(arrivalDate1.toDate());
                    airSegmentInformation.setFromAirport(fromAirport);
                    airSegmentInformation.setToAirport(toAirport);
                    Minutes diff = Minutes.minutesBetween(departureDate1, arrivalDate1);
                    airSegmentInformation.setTravelTime("" + diff.getMinutes());

                    //hopping
                    if (isHoppingStopExists(itineraryInfo)) {
                        if (itineraryInfo.getLegInfo() != null && itineraryInfo.getLegInfo().size() >= 2) {
                            itineraryInfo.getLegInfo().get(0).getLegTravelProduct().getFlightDate().getArrivalDate();
                            itineraryInfo.getLegInfo().get(0).getLegTravelProduct().getFlightDate().getArrivalTime();

                            itineraryInfo.getLegInfo().get(1).getLegTravelProduct().getFlightDate().getDepartureDate();
                            itineraryInfo.getLegInfo().get(1).getLegTravelProduct().getFlightDate().getDepartureTime();

                            itineraryInfo.getLegInfo().get(1).getLegTravelProduct().getBoardPointDetails().getTrueLocationId();

                            List<HoppingFlightInformation> hoppingFlightInformations = null;

                            //Arrival
                            HoppingFlightInformation hop = new HoppingFlightInformation();
                            hop.setLocation(itineraryInfo.getLegInfo().get(1).getLegTravelProduct().getBoardPointDetails().getTrueLocationId());
                            hop.setStartTime(new StringBuilder(itineraryInfo.getLegInfo().get(0).getLegTravelProduct().getFlightDate().getArrivalTime()).insert(2, ":").toString());
                            SimpleDateFormat dateParser = new SimpleDateFormat("ddMMyy");
                            Date startDate = null;
                            Date endDate = null;
                            try {
                                startDate = dateParser.parse(itineraryInfo.getLegInfo().get(0).getLegTravelProduct().getFlightDate().getArrivalDate());
                                endDate = dateParser.parse(itineraryInfo.getLegInfo().get(1).getLegTravelProduct().getFlightDate().getDepartureDate());
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy");
                            hop.setStartDate(dateFormat.format(startDate));
                            //Departure
                            hop.setEndTime(new StringBuilder(itineraryInfo.getLegInfo().get(1).getLegTravelProduct().getFlightDate().getDepartureTime()).insert(2, ":").toString());
                            hop.setEndDate(dateFormat.format(endDate));
                            if (hoppingFlightInformations == null) {
                                hoppingFlightInformations = new ArrayList<HoppingFlightInformation>();
                            }
                            hoppingFlightInformations.add(hop);

                            airSegmentInformation.setHoppingFlightInformations(hoppingFlightInformations);
                        }
                    }
                    airSegmentList.add(airSegmentInformation);
                }
            }
            journey.setAirSegmentList(airSegmentList);
            journeyList.add(journey);
        }

        return journeyList;
    }

    private static boolean isHoppingStopExists(PNRReply.OriginDestinationDetails.ItineraryInfo itineraryInfo) {
        boolean hoppingStopExits = false;
        if (itineraryInfo.getFlightDetail() != null && itineraryInfo.getFlightDetail().getProductDetails() != null && itineraryInfo.getFlightDetail().getProductDetails().getNumOfStops() != null && itineraryInfo.getFlightDetail().getProductDetails().getNumOfStops().intValue() > 0) {
            hoppingStopExits = true;
        }
        return hoppingStopExits;
    }


    public static PricingInformation getPricingForDownsellAndConversion(FarePricePNRWithBookingClassReply pricePNRReply, String totalFareIdentifier, int adultCount) {
        BigDecimal totalFare = new BigDecimal(0);
        BigDecimal totalTax = new BigDecimal(0);
        BigDecimal baseFare = new BigDecimal(0);
        List<FareList> fareList = pricePNRReply.getFareList();
        PricingInformation pricingInformation = new PricingInformation();

        for (FareList fare : fareList) {
            int paxCount = 0;
            String paxType = fare.getSegmentInformation().get(0).getFareQualifier().getFareBasisDetails().getDiscTktDesignator();
            if (paxType.equalsIgnoreCase("ADT") || paxType.equalsIgnoreCase("SEA") || paxType.equalsIgnoreCase("SC")) {
                logger.debug("adultCount : " + adultCount);
                paxCount = adultCount;
            }
            logger.debug("passenger counts : " + paxCount);
            logger.debug("size of fareData : " + fare.getFareDataInformation().getFareDataSupInformation().size());
            for (MonetaryInformationDetailsType223826C fareData : fare.getFareDataInformation().getFareDataSupInformation()) {
                BigDecimal amount = new BigDecimal(fareData.getFareAmount());
                if (totalFareIdentifier.equals(fareData.getFareDataQualifier())) {
                    totalFare = totalFare.add(amount.multiply(new BigDecimal(paxCount)));
                    logger.debug("=======================>> Setting new total fare: " + totalFare);
                }
                //Todo --commented below line as the base fare string is B need to validate
//                if("E".equalsIgnoreCase(fareData.getFareDataQualifier())) {
                if ("B".equalsIgnoreCase(fareData.getFareDataQualifier())) {
                    baseFare = amount;
                }
            }
        }

        pricingInformation.setTotalPrice(totalFare);
        pricingInformation.setTotalPriceValue(totalFare);
        pricingInformation.setBasePrice(baseFare);
        pricingInformation.setTax(totalFare.subtract(baseFare));
        Map<String, Integer> passengerTypeMap = new HashMap<>();

        passengerTypeMap.put("adultCount", adultCount);
        pricingInformation.setPassengerTaxes(getTaxBreakup(pricePNRReply.getFareList(), passengerTypeMap));
        return pricingInformation;
    }

    public static PricingInformation getPricingInfo(
            List<FareList> pricePNRReplyFareList,
            int adultCount, int childCount,
            int infantCount) {
        BigDecimal totalFare = new BigDecimal(0);
        BigDecimal totalBaseFare = new BigDecimal(0);
        BigDecimal adtBaseFare = new BigDecimal(0);
        BigDecimal chdBaseFare = new BigDecimal(0);
        BigDecimal infBaseFare = new BigDecimal(0);
        String currency = null;

//        List<FareList> fareList = pricePNRReplyFareList;
        PricingInformation pricingInformation = new PricingInformation();
        for (FareList fare : pricePNRReplyFareList) {
            int paxCount = 0;
            String paxType = fare.getSegmentInformation().get(0).getFareQualifier().getFareBasisDetails().getDiscTktDesignator();
            if ("chd".equalsIgnoreCase(paxType) || "ch".equalsIgnoreCase(paxType)) {
                paxCount = childCount;
            } else if ("inf".equalsIgnoreCase(paxType) || "in".equalsIgnoreCase(paxType)) {
                paxCount = infantCount;
            } else if ("adt".equalsIgnoreCase(paxType)) {
                paxCount = adultCount;
            } else {
                paxCount = adultCount + childCount + infantCount;
            }
            logger.info("paxCount...." + paxCount);
//            currency = fare.getFareDataInformation().getFareDataSupInformation().get(0).getFareCurrency();
            boolean equivalentFareAvailable = false;
            BigDecimal baseFare = new BigDecimal(0);
            for (MonetaryInformationDetailsType223826C fareData : fare.getFareDataInformation().getFareDataSupInformation()) {
                BigDecimal amount = new BigDecimal(fareData.getFareAmount());
                if (AmadeusConstants.TOTAL_FARE_IDENTIFIER.equals(fareData.getFareDataQualifier())) {
                    totalFare = totalFare.add(amount.multiply(new BigDecimal(paxCount)));
                }
                if ("B".equalsIgnoreCase(fareData.getFareDataQualifier()) || "E".equalsIgnoreCase(fareData.getFareDataQualifier())) {
                    if (!equivalentFareAvailable) {
                        baseFare = amount;
                        currency = fareData.getFareCurrency();
                    }
                    logger.info("baseFare...." + baseFare + "...." + amount);
                }

                if ("E".equalsIgnoreCase(fareData.getFareDataQualifier())) {
                    equivalentFareAvailable = true;
                }

            }

            totalBaseFare = totalBaseFare.add(baseFare.multiply(new BigDecimal(paxCount)));
            if ("chd".equalsIgnoreCase(paxType) || "ch".equalsIgnoreCase(paxType)) {
                chdBaseFare = baseFare;
            } else if ("inf".equalsIgnoreCase(paxType) || "in".equalsIgnoreCase(paxType)) {
                infBaseFare = baseFare;
            } else {
                adtBaseFare = baseFare;
            }
        }
        logger.info("totalFare...." + totalFare);
        logger.info("totalBaseFare...." + totalBaseFare);
        logger.info("adtBaseFare...." + adtBaseFare);
        pricingInformation.setGdsCurrency(currency);
        pricingInformation.setTotalPrice(totalFare);
        pricingInformation.setTotalPriceValue(totalFare);
        pricingInformation.setBasePrice(totalBaseFare);
        pricingInformation.setAdtBasePrice(adtBaseFare);
        pricingInformation.setChdBasePrice(chdBaseFare);
        pricingInformation.setInfBasePrice(infBaseFare);
        pricingInformation.setTax(totalFare.subtract(totalBaseFare));
        Map<String, Integer> passengerTypeMap = new HashMap<>();

        passengerTypeMap.put("adultCount", adultCount);
        passengerTypeMap.put("childCount", childCount);
        passengerTypeMap.put("infantCount", infantCount);
        pricingInformation.setProvider("Amadeus");
        pricingInformation.setPassengerTaxes(getTaxBreakup(pricePNRReplyFareList, passengerTypeMap));
        return pricingInformation;
    }

    public static void setTaxBreakup(PNRResponse pnrResponse, TravellerMasterInfo travellerMasterInfo, FarePricePNRWithBookingClassReply pricePNRReply) {
        Map<String, Integer> passengerTypeMap = getPassengerTypeCount(travellerMasterInfo.getTravellersList());
        PricingInformation pricingInfo = travellerMasterInfo.isSeamen() ? travellerMasterInfo
                .getItinerary().getSeamanPricingInformation() : travellerMasterInfo
                .getItinerary().getPricingInformation();
        List<PassengerTax> passengerTaxes = getTaxBreakup(pricePNRReply.getFareList(), passengerTypeMap);
        pricingInfo.setPassengerTaxes(passengerTaxes);
        pnrResponse.setPricingInfo(pricingInfo);
    }

    public static List<PassengerTax> getTaxBreakup(List<FareList> pricePNRReplyFareList, Map<String, Integer> passengerTypeMap) {
        List<PassengerTax> passengerTaxes = new ArrayList<>();
        for (FareList fare : pricePNRReplyFareList) {
            FareList.SegmentInformation segmentInfo = fare.getSegmentInformation().get(0);
            List<TaxInformation> taxInfos = fare.getTaxInformation();
            if (segmentInfo != null && taxInfos.size() > 0) {
                String paxType = segmentInfo.getFareQualifier().getFareBasisDetails().getDiscTktDesignator();
                PassengerTax passengerTax = null;
                if (paxType.equalsIgnoreCase("CH") || paxType.equalsIgnoreCase("CHD")) {
                    passengerTax = setTaxDetails(taxInfos, "CHD", passengerTypeMap.get("childCount"));
                } else if (paxType.equalsIgnoreCase("IN") || paxType.equalsIgnoreCase("INF")) {
                    passengerTax = setTaxDetails(taxInfos, "INF", passengerTypeMap.get("infantCount"));
                } else {
                    passengerTax = setTaxDetails(taxInfos, "ADT", passengerTypeMap.get("adultCount"));
                }
                passengerTaxes.add(passengerTax);
            }
        }
        return passengerTaxes;
    }

    private static PassengerTax setTaxDetails(List<TaxInformation> taxInfos, String passengerType, int count) {
        PassengerTax passengerTax = new PassengerTax();
        passengerTax.setPassengerType(passengerType);
        passengerTax.setPassengerCount(count);
        Map<String, BigDecimal> taxes = new HashMap<>();
        for (TaxInformation taxInfo : taxInfos) {
            String amount = taxInfo.getAmountDetails().getFareDataMainInformation().getFareAmount();
            String taxCode = taxInfo.getTaxDetails().getTaxType().getIsoCountry();
            if (taxes.containsKey(taxCode)) {
                taxes.put(taxCode, taxes.get(taxCode).add(new BigDecimal(amount)));
            } else {
                taxes.put(taxCode, new BigDecimal(amount));
            }
        }
        passengerTax.setTaxes(taxes);
        return passengerTax;
    }

    public static Map<String, Integer> getPassengerTypeCount(List<Traveller> travellerList) {
        int adultCount = 0, childCount = 0, infantCount = 0;
        Map<String, Integer> passengerTypeMap = new HashMap<>();
        for (Traveller traveller : travellerList) {
            PassengerTypeCode passengerType = DateUtility.getPassengerTypeFromDOB(traveller.getPassportDetails().getDateOfBirth());
            if (passengerType.equals(PassengerTypeCode.ADT)) {
                adultCount++;
            } else if (passengerType.equals(PassengerTypeCode.CHD)) {
                childCount++;
            } else {
                infantCount++;
            }
        }
        passengerTypeMap.put("adultCount", adultCount);
        passengerTypeMap.put("childCount", childCount);
        passengerTypeMap.put("infantCount", infantCount);
        return passengerTypeMap;
    }

    public static Map<String, Integer> getPaxTypeCount(List<TravellerInfo> travellerinfoList) {
        Map<String, Integer> paxTypeCounts = new HashMap<>();
        int adultCount = 0, childCount = 0, infantCount = 0;
        for (TravellerInfo travellerInfo : travellerinfoList) {
            PassengerData paxData = travellerInfo.getPassengerData().get(0);
            String paxType = paxData.getTravellerInformation().getPassenger().get(0).getType();
            if ("chd".equalsIgnoreCase(paxType) || "ch".equalsIgnoreCase(paxType)) {
                childCount++;
            } else if ("inf".equalsIgnoreCase(paxType) || "in".equalsIgnoreCase(paxType)) {
                infantCount++;
            } else {
                adultCount++;
            }
        }
        paxTypeCounts.put("adultCount", adultCount);
        paxTypeCounts.put("childCount", childCount);
        paxTypeCounts.put("infantCount", infantCount);
        return paxTypeCounts;
    }

    public static boolean checkForSimultaneousChange(PNRReply pnrReply) {

        boolean simultaneousChange = false;
        if (pnrReply.getGeneralErrorInfo() != null && !pnrReply.getGeneralErrorInfo().isEmpty()) {
            for (PNRReply.GeneralErrorInfo generalErrorInfo : pnrReply.getGeneralErrorInfo()) {
                String errorText = StringUtils.join(generalErrorInfo.getMessageErrorText().getText());
                String errorDesc = errorText.trim();
                //todo check the string contains works with spaces and all other cases , use regular expression
                if (errorDesc.contains(AmadeusConstants.SIMULTANEOUS_PNR_CHANGE) || errorDesc.equalsIgnoreCase("SIMULTANEOUS CHANGES TO PNR - USE WRA/RT TO PRINT OR IGNORE")) {
                    simultaneousChange = true;
                }
            }
        }

        return simultaneousChange;
    }


    public static PricingInformation getPricingInfoFromTST(PNRReply gdsPNRReply, TicketDisplayTSTReply ticketDisplayTSTReply, boolean isSeamen, List<Journey> journeyList) {
        BigDecimal totalPriceOfBooking = new BigDecimal(0);
        BigDecimal basePriceOfBooking = new BigDecimal(0);
        BigDecimal adtBaseFare = new BigDecimal(0);
        BigDecimal chdBaseFare = new BigDecimal(0);
        BigDecimal infBaseFare = new BigDecimal(0);
        BigDecimal adtTotalFare = new BigDecimal(0);
        BigDecimal chdTotalFare = new BigDecimal(0);
        BigDecimal infTotalFare = new BigDecimal(0);

        String currency = null;

        List<TicketDisplayTSTReply.FareList> fareList = ticketDisplayTSTReply.getFareList();
        PricingInformation pricingInformation = new PricingInformation();

        Map<String, TSTPrice> tstPriceMap = new HashMap<>();

        Map<String, Object> airSegmentRefMap = new HashMap<>();
        Map<String, Object> travellerMap = new HashMap<>();
        Map<String, String> passengerType = new HashMap<>();

        for (PNRReply.OriginDestinationDetails originDestination : gdsPNRReply.getOriginDestinationDetails()) {
            for (PNRReply.OriginDestinationDetails.ItineraryInfo itineraryInfo : originDestination.getItineraryInfo()) {
                String segType = itineraryInfo.getElementManagementItinerary().getSegmentName();
                if (segType.equalsIgnoreCase("AIR")) {
                    String segmentRef = "S" + itineraryInfo.getElementManagementItinerary().getReference().getNumber();
                    String segments = itineraryInfo.getTravelProduct().getBoardpointDetail().getCityCode() + itineraryInfo.getTravelProduct().getOffpointDetail().getCityCode();
                    airSegmentRefMap.put(segmentRef, segments);
                }
            }
        }
        for (PNRReply.TravellerInfo travellerInfo : gdsPNRReply.getTravellerInfo()) {
            String key = "P" + travellerInfo.getElementManagementPassenger().getReference().getNumber();
            String infantIndicator = travellerInfo.getPassengerData().get(0).getTravellerInformation().getPassenger().get(0).getInfantIndicator();
            travellerMap.put(key, travellerInfo);
            if (!isSeamen) {
                PassengerData paxData = travellerInfo.getPassengerData().get(0);
                String paxType = paxData.getTravellerInformation().getPassenger().get(0).getType();
                if ("chd".equalsIgnoreCase(paxType) || "ch".equalsIgnoreCase(paxType)) {
                    passengerType.put(key, "CHD");
                } else if ("inf".equalsIgnoreCase(paxType) || "in".equalsIgnoreCase(paxType)) {
                    passengerType.put(key, "INF");
                } else {
                    passengerType.put(key, "ADT");
                }

                if (infantIndicator != null && "1".equalsIgnoreCase(infantIndicator)) {
                    passengerType.put("PI" + travellerInfo.getElementManagementPassenger().getReference().getNumber(), "INF");
                }
            } else {
                passengerType.put(key, "ADT");
                if (infantIndicator != null && "1".equalsIgnoreCase(infantIndicator)) {
                    passengerType.put("PI" + travellerInfo.getElementManagementPassenger().getReference().getNumber(), "ADT");
                }
            }
        }

        Map<String, AirSegmentInformation> segmentMap = new HashMap<>();
        for (Journey journey : journeyList) {
            for (AirSegmentInformation airSegment : journey.getAirSegmentList()) {
                String key = airSegment.getFromLocation() + airSegment.getToLocation();
                segmentMap.put(key, airSegment);
            }
        }

        List<SegmentPricing> segmentPricingList = new ArrayList<>();
        List<PassengerTax> passengerTaxList = new ArrayList<>();
        boolean segmentWisePricing = false;
        for (TicketDisplayTSTReply.FareList fare : fareList) {
            BigDecimal totalFarePerPaxType = new BigDecimal(0);
            BigDecimal paxTotalFare = new BigDecimal(0);
            BigDecimal baseFareOfPerPaxType = new BigDecimal(0);

            SegmentPricing segmentPricing = new SegmentPricing();
            boolean equivalentFareAvailable = false;
            BigDecimal baseFare = new BigDecimal(0);
            for (MonetaryInformationDetailsTypeI211824C fareData : fare.getFareDataInformation().getFareDataSupInformation()) {
                BigDecimal amount = new BigDecimal(0);
                try {
                    amount = new BigDecimal(fareData.getFareAmount());
                } catch (Exception e) {
                    logger.debug("Fare Amount Not a Valid Decimal {} so taking the default value 0 as amount {} ", fareData.getFareAmount(), e.getMessage(), e);
                }

                if (AmadeusConstants.TOTAL_FARE_IDENTIFIER.equals(fareData.getFareDataQualifier())) {
                    paxTotalFare = amount;
                }
                if ("B".equalsIgnoreCase(fareData.getFareDataQualifier()) || "E".equalsIgnoreCase(fareData.getFareDataQualifier())) {
                    if (!equivalentFareAvailable) {
                        baseFare = amount;
                        currency = fareData.getFareCurrency();
                    }
                }
                if ("E".equalsIgnoreCase(fareData.getFareDataQualifier())) {
                    equivalentFareAvailable = true;
                }
            }

            int paxCount = fare.getPaxSegReference().getRefDetails().size();
            String paxTypeKey = "P" + fare.getPaxSegReference().getRefDetails().get(0).getRefNumber();
            if ("PI".equalsIgnoreCase(fare.getPaxSegReference().getRefDetails().get(0).getRefQualifier())) {
                paxTypeKey = fare.getPaxSegReference().getRefDetails().get(0).getRefQualifier() + fare.getPaxSegReference().getRefDetails().get(0).getRefNumber();
            }
            String paxType = passengerType.get(paxTypeKey);
            totalFarePerPaxType = totalFarePerPaxType.add(paxTotalFare.multiply(new BigDecimal(paxCount)));
            baseFareOfPerPaxType = baseFareOfPerPaxType.add(baseFare.multiply(new BigDecimal(paxCount)));
            PassengerTax passengerTax = getTaxDetailsFromTST(fare.getTaxInformation(), paxType, paxCount);
            passengerTaxList.add(passengerTax);

            if (airSegmentRefMap.size() != fare.getSegmentInformation().size()) {
                segmentWisePricing = true;
            }
            List<String> segmentKeys = new ArrayList<>();
            //if(segmentWisePricing){
            for (TicketDisplayTSTReply.FareList.SegmentInformation segmentInformation : fare.getSegmentInformation()) {
                if (segmentInformation.getSegmentReference() != null && segmentInformation.getSegmentReference().getRefDetails() != null) {
                    ReferencingDetailsTypeI referencingDetailsTypeI = segmentInformation.getSegmentReference().getRefDetails().get(0);
                    String key = referencingDetailsTypeI.getRefQualifier() + referencingDetailsTypeI.getRefNumber();
                    segmentKeys.add(airSegmentRefMap.get(key).toString().toLowerCase());
                }

            }
            //}
            segmentPricing.setSegmentKeysList(segmentKeys);
            segmentPricing.setTotalPrice(totalFarePerPaxType);
            segmentPricing.setBasePrice(baseFareOfPerPaxType);
            segmentPricing.setTax(totalFarePerPaxType.subtract(baseFareOfPerPaxType));
            segmentPricing.setPassengerType(paxType);
            segmentPricing.setPassengerTax(passengerTax);
            segmentPricing.setPassengerCount(new Long(paxCount));
            segmentPricing.setTstSequenceNumber(fare.getFareReference().getIDDescription().getIDSequenceNumber());
            segmentPricingList.add(segmentPricing);
            if ("CHD".equalsIgnoreCase(paxType)) {
                chdBaseFare = chdBaseFare.add(baseFare);
                chdTotalFare = chdTotalFare.add(paxTotalFare);
            } else if ("INF".equalsIgnoreCase(paxType)) {
                infBaseFare = infBaseFare.add(baseFare);
                infTotalFare = infTotalFare.add(paxTotalFare);
            } else {
                adtBaseFare = adtBaseFare.add(baseFare);
                adtTotalFare = adtTotalFare.add(paxTotalFare);
            }
            totalPriceOfBooking = totalPriceOfBooking.add(totalFarePerPaxType);
            basePriceOfBooking = basePriceOfBooking.add(baseFareOfPerPaxType);

            TSTPrice tstPrice = getTSTPrice(fare, paxTotalFare, baseFare, paxType, passengerTax);
            for (TicketDisplayTSTReply.FareList.SegmentInformation segmentInformation : fare.getSegmentInformation()) {
                for (String key : segmentMap.keySet()) {
                    for (Object airSegVal : airSegmentRefMap.values()) {
                        if (key.equals(airSegVal) && segmentInformation.getFareQualifier() != null && segmentInformation.getFareQualifier().size() > 0) {
                            String farebasis = segmentInformation.getFareQualifier().get(0).getFareBasisDetails().getPrimaryCode()
                                    + segmentInformation.getFareQualifier().get(0).getFareBasisDetails().getFareBasisCode();
                            segmentMap.get(key).setFareBasis(farebasis);
                        }
                    }
                }

                if (segmentInformation.getSegmentReference() != null && segmentInformation.getSegmentReference().getRefDetails() != null) {
                    ReferencingDetailsTypeI referencingDetailsTypeI = segmentInformation.getSegmentReference().getRefDetails().get(0);
                    String key = referencingDetailsTypeI.getRefQualifier() + referencingDetailsTypeI.getRefNumber();

                    String tstKey = airSegmentRefMap.get(key).toString() + paxType;
                    tstPriceMap.put(tstKey.toLowerCase(), tstPrice);
                }
            }

        }

        pricingInformation.setSegmentWisePricing(segmentWisePricing);
        pricingInformation.setSegmentPricingList(segmentPricingList);
        logger.debug("getPricingInfoFromTST begin....adtBaseFare..." + adtBaseFare);
        pricingInformation.setAdtBasePrice(adtBaseFare);
        pricingInformation.setAdtTotalPrice(adtTotalFare);
        pricingInformation.setChdBasePrice(chdBaseFare);
        pricingInformation.setChdTotalPrice(chdTotalFare);
        pricingInformation.setInfBasePrice(infBaseFare);
        pricingInformation.setInfTotalPrice(infTotalFare);
        logger.debug("getPricingInfoFromTST begin....currency..." + currency);

        pricingInformation.setGdsCurrency(currency);
        pricingInformation.setTotalPrice(totalPriceOfBooking);
        pricingInformation.setTotalPriceValue(totalPriceOfBooking);
        pricingInformation.setBasePrice(basePriceOfBooking);
        pricingInformation.setTax(totalPriceOfBooking.subtract(basePriceOfBooking));
        pricingInformation.setProvider(PROVIDERS.AMADEUS.toString());
        pricingInformation.setPassengerTaxes(passengerTaxList);

        pricingInformation.setTstPriceMap(tstPriceMap);
        return pricingInformation;
    }

    public static TSTPrice getTSTPrice(TicketDisplayTSTReply.FareList fare, BigDecimal paxTotalFare, BigDecimal paxBaseFare, String paxType, PassengerTax passengerTax) {
        TSTPrice tstPrice = new TSTPrice();
        for (TicketDisplayTSTReply.FareList.SegmentInformation segmentInfo : fare.getSegmentInformation()) {
            com.amadeus.xml.ttstrr_13_1_1a.BaggageDetailsTypeI bagAllowance = null;
            if (segmentInfo.getBagAllowanceInformation() != null) {
                bagAllowance = segmentInfo.getBagAllowanceInformation().getBagAllowanceDetails();
                if (bagAllowance.getBaggageType().equalsIgnoreCase("w")) {
                    if (tstPrice.getMaxBaggageWeight() == 0 || tstPrice.getMaxBaggageWeight() > bagAllowance.getBaggageWeight().intValue()) {
                        tstPrice.setMaxBaggageWeight(bagAllowance.getBaggageWeight().intValue());
                    }
                } else {
                    if (tstPrice.getBaggageCount() == 0 || tstPrice.getBaggageCount() > bagAllowance.getBaggageQuantity().intValue()) {
                        tstPrice.setBaggageCount(bagAllowance.getBaggageQuantity().intValue());
                    }
                }
            }

            //reading booking class(RBD)
            String bookingClass = null;
            if (segmentInfo.getSegDetails() != null && segmentInfo.getSegDetails().getSegmentDetail() != null) {
                bookingClass = segmentInfo.getSegDetails().getSegmentDetail().getClassOfService();
            }
            tstPrice.setBookingClass(bookingClass);
        }

        tstPrice.setTotalPrice(paxTotalFare);
        tstPrice.setBasePrice(paxBaseFare);
        tstPrice.setPassengerType(paxType);
        tstPrice.setPassengerTax(passengerTax);
        return tstPrice;
    }

    public static PassengerTax getTaxDetailsFromTST(List<TicketDisplayTSTReply.FareList.TaxInformation> taxInformationList, String passengerType, int count) {
        PassengerTax passengerTax = new PassengerTax();
        passengerTax.setPassengerType(passengerType);
        passengerTax.setPassengerCount(count);
        Map<String, BigDecimal> taxes = new HashMap<>();
        for (TicketDisplayTSTReply.FareList.TaxInformation taxInformation : taxInformationList) {
            String amount = taxInformation.getAmountDetails().getFareDataMainInformation().getFareAmount();
            String taxCode = taxInformation.getTaxDetails().getTaxType().getIsoCountry();
            if (taxes.containsKey(taxCode)) {
                taxes.put(taxCode, taxes.get(taxCode).add(new BigDecimal(amount)));
            } else {
                taxes.put(taxCode, new BigDecimal(amount));
            }
        }

        passengerTax.setTaxes(taxes);

        return passengerTax;
    }


    public static int getNumberOfTST(List<Traveller> travellerList) {

        int adultCount = 0, childCount = 0, infantCount = 0;
        int totalCount = 0;
        for (Traveller traveller : travellerList) {
            if (traveller.getPassportDetails() != null) {
                PassengerTypeCode passengerTypeCode = DateUtility.getPassengerTypeFromDOB(traveller.getPassportDetails().getDateOfBirth());
                if (passengerTypeCode.name().equals(PassengerTypeCode.ADT.name())) {
                    adultCount = 1;
                } else if (passengerTypeCode.name().equals(PassengerTypeCode.CHD.name())) {
                    childCount = 1;
                } else {
                    infantCount = 1;
                }
            } else {
                adultCount = 1;
            }
            totalCount = adultCount + childCount + infantCount;

        }
        return totalCount;
    }

    //Added to get segment wise pricing while re-pring at payment time
    public static PricingInformation getPricingInfoWithSegmentPricing(PNRReply gdsPNRReply, List<FareList> pricePNRReplyFareList,
                                                                      boolean isSeamen, List<AirSegmentInformation> airSegmentList) {

        BigDecimal totalPriceOfBooking = new BigDecimal(0);
        BigDecimal basePriceOfBooking = new BigDecimal(0);
        BigDecimal adtBaseFare = new BigDecimal(0);
        BigDecimal chdBaseFare = new BigDecimal(0);
        BigDecimal infBaseFare = new BigDecimal(0);
        BigDecimal adtTotalFare = new BigDecimal(0);
        BigDecimal chdTotalFare = new BigDecimal(0);
        BigDecimal infTotalFare = new BigDecimal(0);

        String currency = null;

        PricingInformation pricingInformation = new PricingInformation();

        Map<String, TSTPrice> tstPriceMap = new HashMap<>();

        Map<String, Object> airSegmentRefMap = new HashMap<>();
        Map<String, Object> travellerMap = new HashMap<>();
        Map<String, String> passengerType = new HashMap<>();

        for (PNRReply.OriginDestinationDetails originDestination : gdsPNRReply.getOriginDestinationDetails()) {
            for (PNRReply.OriginDestinationDetails.ItineraryInfo itineraryInfo : originDestination.getItineraryInfo()) {
                String segType = itineraryInfo.getElementManagementItinerary().getSegmentName();
                if (segType.equalsIgnoreCase("AIR")) {
                    String segmentRef = "S" + itineraryInfo.getElementManagementItinerary().getReference().getNumber();
                    String segments = itineraryInfo.getTravelProduct().getBoardpointDetail().getCityCode() + itineraryInfo.getTravelProduct().getOffpointDetail().getCityCode();
                    airSegmentRefMap.put(segmentRef, segments);
                }
            }
        }
        for (PNRReply.TravellerInfo travellerInfo : gdsPNRReply.getTravellerInfo()) {
            String key = "P" + travellerInfo.getElementManagementPassenger().getReference().getNumber();
            travellerMap.put(key, travellerInfo);
            if (!isSeamen) {
                PassengerData paxData = travellerInfo.getPassengerData().get(0);
                String paxType = paxData.getTravellerInformation().getPassenger().get(0).getType();
                String infantIndicator = paxData.getTravellerInformation().getPassenger().get(0).getInfantIndicator();
                if ("chd".equalsIgnoreCase(paxType) || "ch".equalsIgnoreCase(paxType)) {
                    passengerType.put(key, "CHD");
                } else if ("inf".equalsIgnoreCase(paxType) || "in".equalsIgnoreCase(paxType)) {
                    passengerType.put(key, "INF");
                } else {
                    passengerType.put(key, "ADT");
                }

                if (infantIndicator != null && "1".equalsIgnoreCase(infantIndicator)) {
                    passengerType.put("PI" + travellerInfo.getElementManagementPassenger().getReference().getNumber(), "INF");
                }
            } else {
                passengerType.put(key, "ADT");
            }
        }

        Map<String, AirSegmentInformation> segmentMap = new HashMap<>();

        for (AirSegmentInformation airSegment : airSegmentList) {
            String key = airSegment.getFromLocation() + airSegment.getToLocation();
            segmentMap.put(key, airSegment);
        }

        List<SegmentPricing> segmentPricingList = new ArrayList<>();
        List<PassengerTax> passengerTaxList = new ArrayList<>();
        boolean segmentWisePricing = false;
        for (FareList fare : pricePNRReplyFareList) {
            BigDecimal totalFarePerPaxType = new BigDecimal(0);
            BigDecimal paxTotalFare = new BigDecimal(0);
            BigDecimal baseFareOfPerPaxType = new BigDecimal(0);

            SegmentPricing segmentPricing = new SegmentPricing();
            boolean equivalentFareAvailable = false;
            BigDecimal baseFare = new BigDecimal(0);
            for (MonetaryInformationDetailsType223826C fareData : fare.getFareDataInformation().getFareDataSupInformation()) {
                BigDecimal amount = new BigDecimal(fareData.getFareAmount());
                if (AmadeusConstants.TOTAL_FARE_IDENTIFIER.equals(fareData.getFareDataQualifier())) {
                    paxTotalFare = amount;
                }
                if ("B".equalsIgnoreCase(fareData.getFareDataQualifier()) || "E".equalsIgnoreCase(fareData.getFareDataQualifier())) {
                    if (!equivalentFareAvailable) {
                        baseFare = amount;
                        currency = fareData.getFareCurrency();
                    }
                }
                if ("E".equalsIgnoreCase(fareData.getFareDataQualifier())) {
                    equivalentFareAvailable = true;
                }
            }

            int paxCount = fare.getPaxSegReference().getRefDetails().size();

            String paxTypeKey = "P" + fare.getPaxSegReference().getRefDetails().get(0).getRefNumber();
            if ("PI".equalsIgnoreCase(fare.getPaxSegReference().getRefDetails().get(0).getRefQualifier())) {
                paxTypeKey = fare.getPaxSegReference().getRefDetails().get(0).getRefQualifier() + fare.getPaxSegReference().getRefDetails().get(0).getRefNumber();
            }

            String paxType = passengerType.get(paxTypeKey);
            totalFarePerPaxType = totalFarePerPaxType.add(paxTotalFare.multiply(new BigDecimal(paxCount)));
            baseFareOfPerPaxType = baseFareOfPerPaxType.add(baseFare.multiply(new BigDecimal(paxCount)));

            PassengerTax passengerTax = getTaxDetailsWithSegmentPrice(fare.getTaxInformation(), paxType, paxCount);
            passengerTaxList.add(passengerTax);

            if (airSegmentRefMap.size() != fare.getSegmentInformation().size()) {
                segmentWisePricing = true;
            }
            List<String> segmentKeys = new ArrayList<>();
            if (segmentWisePricing) {
                for (FareList.SegmentInformation segmentInformation : fare.getSegmentInformation()) {
                    if (segmentInformation.getSegmentReference() != null && segmentInformation.getSegmentReference().getRefDetails() != null) {
                        com.amadeus.xml.tpcbrr_12_4_1a.ReferencingDetailsTypeI referencingDetailsTypeI = segmentInformation.getSegmentReference().getRefDetails().get(0);
                        String key = referencingDetailsTypeI.getRefQualifier() + referencingDetailsTypeI.getRefNumber();
                        segmentKeys.add(airSegmentRefMap.get(key).toString().toLowerCase());
                    }

                }
            }
            segmentPricing.setSegmentKeysList(segmentKeys);
            segmentPricing.setTotalPrice(totalFarePerPaxType);
            segmentPricing.setBasePrice(baseFareOfPerPaxType);
            segmentPricing.setTax(totalFarePerPaxType.subtract(baseFareOfPerPaxType));
            segmentPricing.setPassengerType(paxType);
            segmentPricing.setPassengerTax(passengerTax);
            segmentPricing.setPassengerCount(new Long(paxCount));
            segmentPricingList.add(segmentPricing);
            if ("CHD".equalsIgnoreCase(paxType)) {
                chdBaseFare = chdBaseFare.add(baseFare);
                chdTotalFare = chdTotalFare.add(paxTotalFare);
            } else if ("INF".equalsIgnoreCase(paxType)) {
                infBaseFare = infBaseFare.add(baseFare);
                infTotalFare = infTotalFare.add(paxTotalFare);
            } else {
                adtBaseFare = adtBaseFare.add(baseFare);
                adtTotalFare = adtTotalFare.add(paxTotalFare);
            }
            totalPriceOfBooking = totalPriceOfBooking.add(totalFarePerPaxType);
            basePriceOfBooking = basePriceOfBooking.add(baseFareOfPerPaxType);

            TSTPrice tstPrice = getTSTPriceWithSegmentPrice(fare, paxTotalFare, baseFare, paxType, passengerTax);
            for (FareList.SegmentInformation segmentInformation : fare.getSegmentInformation()) {
                for (String key : segmentMap.keySet()) {
                    for (Object airSegVal : airSegmentRefMap.values()) {
                        if (key.equals(airSegVal)) {
                            String farebasis = null;
                            if (segmentInformation.getFareQualifier() != null && segmentInformation.getFareQualifier().getFareBasisDetails() != null && segmentInformation.getFareQualifier().getFareBasisDetails().getPrimaryCode() != null) {
                                farebasis = segmentInformation.getFareQualifier().getFareBasisDetails().getPrimaryCode()
                                        + segmentInformation.getFareQualifier().getFareBasisDetails().getFareBasisCode();
                                segmentMap.get(key).setFareBasis(farebasis);
                            }
                        }
                    }
                }

                if (segmentInformation.getSegmentReference() != null && segmentInformation.getSegmentReference().getRefDetails() != null) {
                    com.amadeus.xml.tpcbrr_12_4_1a.ReferencingDetailsTypeI referencingDetailsTypeI = segmentInformation.getSegmentReference().getRefDetails().get(0);
                    String key = referencingDetailsTypeI.getRefQualifier() + referencingDetailsTypeI.getRefNumber();

                    String tstKey = airSegmentRefMap.get(key).toString() + paxType;
                    tstPriceMap.put(tstKey.toLowerCase(), tstPrice);
                }
            }

        }

        pricingInformation.setSegmentWisePricing(segmentWisePricing);
        pricingInformation.setSegmentPricingList(segmentPricingList);

        pricingInformation.setAdtBasePrice(adtBaseFare);
        pricingInformation.setAdtTotalPrice(adtTotalFare);
        pricingInformation.setChdBasePrice(chdBaseFare);
        pricingInformation.setChdTotalPrice(chdTotalFare);
        pricingInformation.setInfBasePrice(infBaseFare);
        pricingInformation.setInfTotalPrice(infTotalFare);

        pricingInformation.setGdsCurrency(currency);
        pricingInformation.setTotalPrice(totalPriceOfBooking);
        pricingInformation.setTotalPriceValue(totalPriceOfBooking);
        pricingInformation.setBasePrice(basePriceOfBooking);
        pricingInformation.setTax(totalPriceOfBooking.subtract(basePriceOfBooking));
        pricingInformation.setProvider(PROVIDERS.AMADEUS.toString());
        pricingInformation.setPassengerTaxes(passengerTaxList);

        pricingInformation.setTstPriceMap(tstPriceMap);
        return pricingInformation;
    }

    //Added to get segment wise pricing while re-pring at payment time
    public static PassengerTax getTaxDetailsWithSegmentPrice(List<FareList.TaxInformation> taxInformationList, String passengerType, int count) {
        PassengerTax passengerTax = new PassengerTax();
        passengerTax.setPassengerType(passengerType);
        passengerTax.setPassengerCount(count);
        Map<String, BigDecimal> taxes = new HashMap<>();
        for (TaxInformation taxInformation : taxInformationList) {
            String amount = taxInformation.getAmountDetails().getFareDataMainInformation().getFareAmount();
            String taxCode = taxInformation.getTaxDetails().getTaxType().getIsoCountry();
            if (taxes.containsKey(taxCode)) {
                taxes.put(taxCode, taxes.get(taxCode).add(new BigDecimal(amount)));
            } else {
                taxes.put(taxCode, new BigDecimal(amount));
            }
        }

        passengerTax.setTaxes(taxes);

        return passengerTax;
    }

    //Added to get segment wise pricing while re-pring at payment time
    public static TSTPrice getTSTPriceWithSegmentPrice(FareList fare, BigDecimal paxTotalFare, BigDecimal paxBaseFare, String paxType, PassengerTax passengerTax) {
        TSTPrice tstPrice = new TSTPrice();
        for (FareList.SegmentInformation segmentInfo : fare.getSegmentInformation()) {
            BaggageDetailsTypeI bagAllowance = null;
            if (segmentInfo.getBagAllowanceInformation() != null) {
                bagAllowance = segmentInfo.getBagAllowanceInformation().getBagAllowanceDetails();
                if (bagAllowance.getBaggageType().equalsIgnoreCase("w")) {
                    if (tstPrice.getMaxBaggageWeight() == 0 || tstPrice.getMaxBaggageWeight() > bagAllowance.getBaggageWeight().intValue()) {
                        if (bagAllowance.getBaggageWeight() != null) {
                            tstPrice.setMaxBaggageWeight(bagAllowance.getBaggageWeight().intValue());
                        }
                    }
                } else {
                    if (tstPrice.getBaggageCount() == 0 || tstPrice.getBaggageCount() > bagAllowance.getBaggageQuantity().intValue()) {
                        if (bagAllowance.getBaggageQuantity() != null) {
                            tstPrice.setBaggageCount(bagAllowance.getBaggageQuantity().intValue());
                        }
                    }
                }
                //reading booking class(RBD)
                String bookingClass = segmentInfo.getSegDetails().getSegmentDetail().getClassOfService();
                tstPrice.setBookingClass(bookingClass);
            }

        }

        tstPrice.setTotalPrice(paxTotalFare);
        tstPrice.setBasePrice(paxBaseFare);
        tstPrice.setPassengerType(paxType);
        tstPrice.setPassengerTax(passengerTax);
        return tstPrice;
    }

    public static void checkFarePrice(FarePricePNRWithBookingClassReply pricePNRReply, PNRResponse pnrResponse, TravellerMasterInfo travellerMasterInfo, Map<String, Object> travellerSegMap) {

        // TODO: re-factor
        int adultCount = 0, childCount = 0, infantCount = 0;
        adultCount = (int) travellerSegMap.get("adultCount");
        childCount = (int) travellerSegMap.get("childCount");
        infantCount = (int) travellerSegMap.get("infantCount");
        BigDecimal totalFare = new BigDecimal(0);
        BigDecimal baseFare = new BigDecimal(0);
        int numberOfTst = (travellerMasterInfo.isSeamen()) ? 1
                : (int) travellerSegMap.get("totalCount");

        List<FarePricePNRWithBookingClassReply.FareList> fareList = new ArrayList<>();
        fareList = pricePNRReply.getFareList();
        PricingInformation pricingInformation = getPricingInfo(fareList, adultCount, childCount, infantCount);

        totalFare = pricingInformation.getTotalPrice();
        baseFare = pricingInformation.getBasePrice();
        pnrResponse.setPricingInfo(pricingInformation);

        BigDecimal searchPrice = new BigDecimal(0);
        if (travellerMasterInfo.isSeamen()) {
            searchPrice = travellerMasterInfo.getItinerary().getSeamanPricingInformation().getTotalPriceValue();
        } else {
            searchPrice = travellerMasterInfo.getItinerary().getPricingInformation().getTotalPriceValue();
        }


        if (totalFare.compareTo(searchPrice) == 0) {
            pnrResponse.setPriceChanged(false);
            return;
        }
        pnrResponse.setChangedPrice(totalFare);
        pnrResponse.setChangedBasePrice(baseFare);
        pnrResponse.setOriginalPrice(searchPrice);
        pnrResponse.setPriceChanged(true);
        pnrResponse.setFlightAvailable(true);

    }

    public static List<PAXFareDetails> getPaxFareDetails(PNRReply gdsPNRReply) {


        List<FareSegment> fareSegmentList = new ArrayList<>();

        List<FareJourney> fareJourneyList = new ArrayList<>();
        PAXFareDetails paxFareDetails = new PAXFareDetails();
        List<PAXFareDetails> paxFareDetailsList = new ArrayList<>();

        List<PNRReply.TstData> tstDataList = gdsPNRReply.getTstData();

        List<String> fareBasesList = new ArrayList<>();
        for (PNRReply.TstData tstData : tstDataList) {
            List<FareElementType> fareElementTypeList = tstData.getFareBasisInfo().getFareElement();
            for (FareElementType fareElement : fareElementTypeList) {
                fareBasesList.add(fareElement.getFareBasis());
            }
        }


        List<PNRReply.OriginDestinationDetails.ItineraryInfo> itinerary = gdsPNRReply.getOriginDestinationDetails().get(0).getItineraryInfo();
        int index = 0;
        for (PNRReply.OriginDestinationDetails.ItineraryInfo itineraryInfo : itinerary) {

            FareSegment fareSegment = new FareSegment();
            FareJourney fareJourney = new FareJourney();
            if (itineraryInfo.getTravelProduct() != null && itineraryInfo.getTravelProduct().getProductDetails() != null) {
                fareSegment.setBookingClass(itineraryInfo.getTravelProduct() != null ? itineraryInfo.getTravelProduct().getProductDetails().getClassOfService() : "");
            }
            if (itineraryInfo.getCabinDetails() != null && itineraryInfo.getCabinDetails().getCabinDetails() != null) {
                fareSegment.setCabinClass(itineraryInfo.getCabinDetails() != null ? itineraryInfo.getCabinDetails().getCabinDetails().getClassDesignator() : "");
            }

            if (itineraryInfo.getTravelProduct().getBoardpointDetail() != null && itineraryInfo.getTravelProduct().getOffpointDetail() != null) {
                if (index < fareBasesList.size()) {
                    fareSegment.setFareBasis(fareBasesList.get(index));
                    index++;
                }
                fareSegmentList.add(fareSegment);
                fareJourney.setFareSegmentList(fareSegmentList);
                fareJourneyList.add(fareJourney);
            }

        }

        paxFareDetails.setPassengerTypeCode(PassengerTypeCode.ADT);
        paxFareDetails.setFareJourneyList(fareJourneyList);
        paxFareDetailsList.add(paxFareDetails);

        return paxFareDetailsList;


    }

    public static Map<String, String> getFareComponentMapFromFareInformativePricing(FareInformativePricingWithoutPNRReply fareInformativePricingPNRReply) {

        Map<String, String> fareComponentsMap = new LinkedHashMap<>();

        try {

            if (fareInformativePricingPNRReply != null && fareInformativePricingPNRReply.getMainGroup() != null && fareInformativePricingPNRReply.getErrorGroup() == null) {

                FareInformativePricingWithoutPNRReply.MainGroup.PricingGroupLevelGroup pricingGroup = fareInformativePricingPNRReply.getMainGroup().getPricingGroupLevelGroup().get(0);

                if (pricingGroup.getFareInfoGroup() != null) {
                    FareInformativePricingWithoutPNRReply.MainGroup.PricingGroupLevelGroup.FareInfoGroup fareInfo = pricingGroup.getFareInfoGroup();

                    List<FareInformativePricingWithoutPNRReply.MainGroup.PricingGroupLevelGroup.FareInfoGroup.FareComponentDetailsGroup> fareComponents = fareInfo.getFareComponentDetailsGroup();

                    if (fareComponents != null) {
                        for (FareInformativePricingWithoutPNRReply.MainGroup.PricingGroupLevelGroup.FareInfoGroup.FareComponentDetailsGroup fareComponent : fareComponents) {

                            FareInformativePricingWithoutPNRReply.MainGroup.PricingGroupLevelGroup.FareInfoGroup.FareComponentDetailsGroup.FareComponentID fareComponentID = fareComponent.getFareComponentID();

                            if (fareComponentID != null && fareComponentID.getItemNumberDetails() != null) {
                                for (FareInformativePricingWithoutPNRReply.MainGroup.PricingGroupLevelGroup.FareInfoGroup.FareComponentDetailsGroup.FareComponentID.ItemNumberDetails item : fareComponentID.getItemNumberDetails()) {

                                    if (item.getNumber() != null) {
                                        String itemNumber = item.getNumber();
                                        StringBuilder location = new StringBuilder();

                                        if (fareComponent.getMarketFareComponent() != null) {
                                            FareInformativePricingWithoutPNRReply.MainGroup.PricingGroupLevelGroup.FareInfoGroup.FareComponentDetailsGroup.MarketFareComponent marketFareComponent = fareComponent.getMarketFareComponent();

                                            String boardPoint = "";
                                            String offPoint = "";

                                            if (marketFareComponent.getBoardPointDetails() != null && marketFareComponent.getBoardPointDetails().getTrueLocationId() != null) {
                                                boardPoint = marketFareComponent.getBoardPointDetails().getTrueLocationId();
                                            }

                                            if (marketFareComponent.getOffpointDetails() != null && marketFareComponent.getOffpointDetails().getTrueLocationId() != null) {
                                                offPoint = marketFareComponent.getOffpointDetails().getTrueLocationId();
                                            }

                                            location.append(boardPoint).append("-").append(offPoint);
                                        }

                                        if (location.length() > 1) {
                                            fareComponentsMap.put(itemNumber, location.toString());
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            return fareComponentsMap;

        } catch (Exception e) {
            logger.debug("Error with getting Fare Component Group from fareInformativePricingPNRReply {} : ", e.getMessage(), e);
            return null;
        }
    }

    public static Map<String, String> getFareComponentMapFromPricePNRWithBookingClass(FarePricePNRWithBookingClassReply pricePNRWithBookingClassReply) {

        Map<String, String> fareComponentsMap = new LinkedHashMap<>();

        try {

            if (pricePNRWithBookingClassReply != null && pricePNRWithBookingClassReply.getFareList() != null) {
                List<FarePricePNRWithBookingClassReply.FareList> fareLists = pricePNRWithBookingClassReply.getFareList();

                for (FarePricePNRWithBookingClassReply.FareList fareList : fareLists) {
                    if (fareList.getFareComponentDetailsGroup() != null) {
                        List<FareComponentDetailsType> fareComponents = fareList.getFareComponentDetailsGroup();

                        for (FareComponentDetailsType fareComponent : fareComponents) {
                            ItemNumberType fareComponentID = fareComponent.getFareComponentID();

                            if (fareComponentID != null && fareComponentID.getItemNumberDetails() != null) {
                                for (ItemNumberIdentificationType item : fareComponentID.getItemNumberDetails()) {
                                    if (item.getNumber() != null) {
                                        String itemNumber = item.getNumber();
                                        StringBuilder location = new StringBuilder();

                                        TravelProductInformationTypeI marketFareComponent = fareComponent.getMarketFareComponent();

                                        if (marketFareComponent != null) {
                                            String boardPoint = "";
                                            String offPoint = "";

                                            if (marketFareComponent.getBoardPointDetails() != null && marketFareComponent.getBoardPointDetails().getTrueLocationId() != null) {
                                                boardPoint = marketFareComponent.getBoardPointDetails().getTrueLocationId();
                                            }

                                            if (marketFareComponent.getOffpointDetails() != null && marketFareComponent.getOffpointDetails().getTrueLocationId() != null) {
                                                offPoint = marketFareComponent.getOffpointDetails().getTrueLocationId();
                                            }

                                            location.append(boardPoint).append("-").append(offPoint);
                                        }

                                        if (location.length() > 1) {
                                            fareComponentsMap.put(itemNumber, location.toString());
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            return fareComponentsMap;

        } catch (Exception e) {

            logger.debug("Error with getting Fare Component Group from pricePNRWithBookingClassReply {} : ", e.getMessage(), e);
            return null;
        }
    }


    public static List<Journey> getJourneyDetails(List<Journey> journeyList, Map<String, String> fareComponentJourneyMap, RedisTemplate redisTemplate) {

        try {

            List<Journey> actualJourneyList = new ArrayList<>();
            List<AirSegmentInformation> pnrReplyJourneyList = journeyList.get(0).getAirSegmentList();

            int currentAirSegment = 0;

            for (Map.Entry<String, String> journeyMap : fareComponentJourneyMap.entrySet()) {

                String[] originDestinationList = journeyMap.getValue().split("-");
                String destination = originDestinationList[1].trim();

                Journey currentJourney = new Journey();
                List<AirSegmentInformation> actualAirSegmentList = new ArrayList<>();

                while (currentAirSegment < pnrReplyJourneyList.size()) {
                    AirSegmentInformation airSegmentInformation = pnrReplyJourneyList.get(currentAirSegment);

                    Airport toAirport;
                    if (redisTemplate == null) {
                        toAirport = Airport.getAirportByIataCode(airSegmentInformation.getToLocation());
                    } else {
                        toAirport = Airport.getAirport(airSegmentInformation.getToLocation(), redisTemplate);
                    }

                    actualAirSegmentList.add(airSegmentInformation);
                    pnrReplyJourneyList.remove(currentAirSegment);

                    if (airSegmentInformation.getToLocation().equalsIgnoreCase(destination) || toAirport.getCityCode().equalsIgnoreCase(destination)) {
                        currentJourney.setAirSegmentList(actualAirSegmentList);
                        actualJourneyList.add(currentJourney);
                        break;
                    }
                }
            }
            return actualJourneyList;
        } catch (Exception e) {
            logger.debug("Error fetching journey wise booking {}", e.getMessage());
            return null;
        }
    }


    public Map<String, FareCheckRulesResponse> getFareRuleTxtMapFromPricingAndFc(AmadeusSessionWrapper amadeusSessionWrapper, Map<String, String> fareComponents) {

        Map<String, FareCheckRulesResponse> fareCheckRulesResponseMap = new LinkedHashMap<>();

        try {

            Map<String, FareCheckRulesReply> fareReplies = fareRules.processFareRulesJourneyWise(amadeusSessionWrapper, fareComponents);

            if (fareReplies.isEmpty()) {
                logger.warn("No fare rules retrieved for components: {}", fareComponents);
                return null;
            }

            for (Map.Entry<String, FareCheckRulesReply> entry : fareReplies.entrySet()) {
                String originDestination = entry.getKey();
                FareCheckRulesReply fareCheckRulesReply = entry.getValue();

                Map<String, Map<String, List<String>>> fareRulesMap = AmadeusHelper.getFareCheckRulesBenzy(fareCheckRulesReply);
//                List<String> detailedFareRuleList = AmadeusHelper.getDetailedFareDetailsList(fareCheckRulesReply.getTariffInfo().get(0).getFareRuleText());
                List<String> detailedFareRuleList = AmadeusHelper.getDetailedFareRulesList(fareCheckRulesReply);

                String cabinClass = getCabinClassFromFareCheckRulesReply(fareCheckRulesReply);
                List<FareType> fareTypes = getCabinDetailsFromFareCheckRulesReply(fareCheckRulesReply);
                FareCheckRulesResponse fareCheckRulesResponse = new FareCheckRulesResponse();
                if (fareTypes != null && !fareTypes.isEmpty()) {
                    fareCheckRulesResponse.setFareTypes(fareTypes);
                }
                fareCheckRulesResponse.setRuleMap(fareRulesMap);
                fareCheckRulesResponse.setDetailedRuleList(detailedFareRuleList);
                fareCheckRulesResponse.setCabinClass(cabinClass);
                fareCheckRulesResponseMap.put(originDestination, fareCheckRulesResponse);
            }

            return fareCheckRulesResponseMap;

        } catch (Exception e) {

            logger.debug("Error with getting Fare Component Group from getFareRuleTxtMapFromPricingAndFc {} : ", e.getMessage(), e);
            return null;
        }

    }

    public String getCabinClassFromFareCheckRulesReply(FareCheckRulesReply fareCheckRulesReply) {

        String designator = null;

        try {
            List<FareCheckRulesReply.FlightDetails> flightDetails = fareCheckRulesReply.getFlightDetails();

            if (flightDetails != null) {
                for (FareCheckRulesReply.FlightDetails flightDetail : flightDetails) {

                    List<FareCheckRulesReply.FlightDetails.ProductInfo> productInfo = flightDetail.getProductInfo();

                    if (productInfo != null) {
                        for (FareCheckRulesReply.FlightDetails.ProductInfo productInfos : productInfo) {

                            FareCheckRulesReply.FlightDetails.ProductInfo.ProductDetails productDetails = productInfos.getProductDetails();
                            if (productDetails != null) {
                                List<FareCheckRulesReply.FlightDetails.ProductInfo.ProductDetails.BookingClassDetails> bookingClassDetails = productDetails.getBookingClassDetails();

                                if (bookingClassDetails != null) {
                                    for (FareCheckRulesReply.FlightDetails.ProductInfo.ProductDetails.BookingClassDetails bookingClassDetail : bookingClassDetails) {
                                        designator = bookingClassDetail.getDesignator();
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return designator;

        } catch (Exception e) {
            logger.debug("Error with getting designator from FareCheckRulesReply {} : ", e.getMessage(), e);
            return null;
        }
    }

    public List<FareType> getCabinDetailsFromFareCheckRulesReply(FareCheckRulesReply fareCheckRulesReply) {

        List<FareType> fareTypeList = new LinkedList<>();
        FareType fareType = new FareType();

        try {
            List<FareCheckRulesReply.FlightDetails> flightDetails = fareCheckRulesReply.getFlightDetails();

            if (flightDetails != null) {
                for (FareCheckRulesReply.FlightDetails flightDetail : flightDetails) {

                    //For FTC and FFD
                    List<FareCheckRulesReply.FlightDetails.FlightErrorCode> flightErrorCodes = flightDetail.getFlightErrorCode();

                    if (flightErrorCodes != null) {
                        for (FareCheckRulesReply.FlightDetails.FlightErrorCode flightErrorCode : flightErrorCodes) {
                            if (flightErrorCode.getFreeTextQualification() != null) {
                                String informationType = flightErrorCode.getFreeTextQualification().getInformationType();

                                if (informationType != null) {
                                    List<String> freeText = flightErrorCode.getFreeText();
                                    if (freeText != null && !freeText.isEmpty()) {
                                        String text = freeText.get(0);
                                        if (informationType.equalsIgnoreCase("FTC") && text != null && !text.isEmpty()) {
                                            fareType.setFtc(text);
                                        } else if (informationType.equalsIgnoreCase("FFD") && text != null && !text.isEmpty()) {
                                            fareType.setFfd(text);
                                        }
                                    }
                                }
                            }
                        }
                    }

                    //For Fare Family
                    List<FareCheckRulesReply.FlightDetails.QualificationFareDetails> qualificationFareDetailsList = flightDetail.getQualificationFareDetails();

                    if (qualificationFareDetailsList != null) {
                        for (FareCheckRulesReply.FlightDetails.QualificationFareDetails qualificationFareDetails : qualificationFareDetailsList) {

                            List<FareCheckRulesReply.FlightDetails.QualificationFareDetails.DiscountDetails> discountDetailsList = qualificationFareDetails.getDiscountDetails();

                            if (discountDetailsList != null) {
                                for (FareCheckRulesReply.FlightDetails.QualificationFareDetails.DiscountDetails discountDetails : discountDetailsList) {
                                    String fareQualifier = discountDetails.getFareQualifier();
                                    if (fareQualifier != null && fareQualifier.equalsIgnoreCase("FF")) {
                                        String rateCategory = discountDetails.getRateCategory();
                                        if (rateCategory != null && !rateCategory.isEmpty()) {
                                            fareType.setFareFamily(rateCategory);
                                        }
                                    }
                                }
                            }

                            // fareBasis
                            FareCheckRulesReply.FlightDetails.QualificationFareDetails.AdditionalFareDetails additionalFareDetails = qualificationFareDetails.getAdditionalFareDetails();
                            if (additionalFareDetails != null) {
                                String rateClass = additionalFareDetails.getRateClass();
                                if (rateClass != null && !rateClass.isEmpty()) {
                                    fareType.setFareBasis(rateClass);
                                }
                            }
                        }
                    }
                }
            }

            if ((fareType.getFtc() != null && !fareType.getFtc().isEmpty()) ||
                    (fareType.getFfd() != null && !fareType.getFfd().isEmpty()) ||
                    (fareType.getFareFamily() != null && !fareType.getFareFamily().isEmpty())) {

                fareTypeList.add(fareType);
            }

            return fareTypeList;

        } catch (Exception e) {
            logger.debug("Error with getting fare type from FareCheckRulesReply {} : ", e.getMessage(), e);
            return null;
        }
    }

    public static Map<String, String> getPaxRefAndNameMap(List<PNRReply.TravellerInfo> travellerInfoList) {

        Map<String, String> paxName = new LinkedHashMap<>();

        try {

            for (PNRReply.TravellerInfo travellerInfo : travellerInfoList) {

                ElementManagementSegmentType passengerReference = travellerInfo.getElementManagementPassenger();
                String referenceNumber = String.valueOf(passengerReference.getReference().getNumber());
                PNRReply.TravellerInfo.PassengerData passengerData = travellerInfo.getPassengerData().get(0);

                String salutation = null;
                String firstName = null;
                String lastName = passengerData.getTravellerInformation().getTraveller().getSurname();
                String fullName = null;

                String firstNameResponse = passengerData.getTravellerInformation().getPassenger().get(0).getFirstName();
                String[] names = firstNameResponse.split("\\s");
                firstName = names[0];

                if (names.length > 1) {
                    for (String name : names) {
                        if (name.equalsIgnoreCase("Mr") || name.equalsIgnoreCase("Mrs") || name.equalsIgnoreCase("Ms") || name.equalsIgnoreCase("Miss") || name.equalsIgnoreCase("Master") || name.equalsIgnoreCase("Mstr") || name.equalsIgnoreCase("Capt")) {
                            salutation = WordUtils.capitalizeFully(name);
                        }
                    }
                }

                if (salutation != null) {
                    fullName = salutation + " " + firstName + " " + lastName;
                }

                paxName.put(referenceNumber, fullName);
            }

            return paxName;

        } catch (Exception e) {
            logger.debug("Error with getting pax ref and name{} : ", e.getMessage(), e);
            return null;
        }
    }

    public static Map<String, String> getFareComponentFromTst(TicketDisplayTSTReply ticketDisplayTSTReply) {

        Map<String, String> fareComponentsMap = new LinkedHashMap<>();
        int itemNumber = 0;

        try {

            if (ticketDisplayTSTReply != null && ticketDisplayTSTReply.getFareList() != null && ticketDisplayTSTReply.getErrorGroup() == null) {

                List<TicketDisplayTSTReply.FareList> fareList = ticketDisplayTSTReply.getFareList();

                if (fareList != null && !fareList.isEmpty()) {

                    for (TicketDisplayTSTReply.FareList fare : fareList) {

                        List<TicketDisplayTSTReply.FareList.FareComponentDetailsGroup> fareComponentDetailsGroupList = fare.getFareComponentDetailsGroup();

                        if (fareComponentDetailsGroupList != null && !fareComponentDetailsGroupList.isEmpty()) {

                            for (TicketDisplayTSTReply.FareList.FareComponentDetailsGroup fareComponentDetailsGroup : fareComponentDetailsGroupList) {

                                if (fareComponentDetailsGroup != null) {

                                    com.amadeus.xml.ttstrr_13_1_1a.TravelProductInformationTypeI marketFareComponent = fareComponentDetailsGroup.getMarketFareComponent();

                                    if (marketFareComponent != null) {

                                        StringBuilder location = new StringBuilder();

                                        String boardPoint = "";
                                        String offPoint = "";

                                        if (marketFareComponent.getBoardPointDetails() != null && marketFareComponent.getBoardPointDetails().getTrueLocationId() != null) {
                                            boardPoint = marketFareComponent.getBoardPointDetails().getTrueLocationId();
                                        }

                                        if (marketFareComponent.getOffpointDetails() != null && marketFareComponent.getOffpointDetails().getTrueLocationId() != null) {
                                            offPoint = marketFareComponent.getOffpointDetails().getTrueLocationId();
                                        }

                                        location.append(boardPoint).append("-").append(offPoint);

                                        if (location.length() > 1) {
                                            ++itemNumber;
                                            fareComponentsMap.put(String.valueOf(itemNumber), location.toString());
                                        }

                                    }

                                }


                            }

                        }

                    }

                }

            }

            return fareComponentsMap;

        } catch (Exception e) {
            logger.debug("Error fetching farecomponent from tst display {}", e.getMessage());
            return null;
        }


    }


    public static Map<String, AmadeusSegmentRefDTO> getSegmentRefMap(PNRReply pnrReply, String gdsPnr) {

        Map<String, AmadeusSegmentRefDTO> segmetRefMap = new HashMap<>();

        try {
            List<PNRReply.OriginDestinationDetails> originDestinationDetailsList = pnrReply.getOriginDestinationDetails();
            for (PNRReply.OriginDestinationDetails originDestinationDetails : originDestinationDetailsList) {

                List<PNRReply.OriginDestinationDetails.ItineraryInfo> itineraryInfoList = originDestinationDetails.getItineraryInfo();
                for (OriginDestinationDetails.ItineraryInfo itineraryInfo : itineraryInfoList) {

                    AmadeusSegmentRefDTO amadeusSegmentRefDTO = new AmadeusSegmentRefDTO();

                    ElementManagementSegmentType elementManagementSegmentType = itineraryInfo.getElementManagementItinerary();
                    String segmentName = elementManagementSegmentType.getSegmentName();
                    String lineNumber = String.valueOf(elementManagementSegmentType.getLineNumber());

                    if (segmentName.equalsIgnoreCase("AIR")) {

                        ReferencingDetailsType127526C referencingDetailsType127526C = elementManagementSegmentType.getReference();
                        String segmentRefNo = String.valueOf(referencingDetailsType127526C.getNumber());

                        TravelProductInformationTypeI185722S travelProductInformationTypeI = itineraryInfo.getTravelProduct();
                        String originCityCode = travelProductInformationTypeI.getBoardpointDetail().getCityCode();
                        String destinationCityCode = travelProductInformationTypeI.getOffpointDetail().getCityCode();

                        String key = originCityCode + destinationCityCode;

                        amadeusSegmentRefDTO.setSegmentRefNo(segmentRefNo);
                        amadeusSegmentRefDTO.setLineNumber(lineNumber);

                        segmetRefMap.put(key, amadeusSegmentRefDTO);
                    }
                }
            }

            return segmetRefMap;
        } catch (Exception e) {
            logger.debug("Error getting segment Ref Map for PNR : {} --- {}", gdsPnr, e.getMessage(), e);
            return segmetRefMap;
        }
    }


    public static List<FreeMealsDetails> getFreeMealsInfoFromPnr(PNRReply pnrReply) {

        List<FreeMealsDetails> freeMealsDetailsList = new ArrayList<>();

        try {
            // Get passenger reference and name map
            List<TravellerInfo> travellerInfoList = pnrReply.getTravellerInfo();

            Map<String, String> paxRefAndNameMap = AmadeusBookingHelper.getPaxRefAndNameMap(travellerInfoList);

            // Get segment map
            Map<String, Map<String, String>> segmentMap = getSegmentNameMapFromPnr(pnrReply);

            // Process DataElementsIndiv for SSR
            List<PNRReply.DataElementsMaster.DataElementsIndiv> dataElementsIndivList = pnrReply.getDataElementsMaster().getDataElementsIndiv();


            if (!dataElementsIndivList.isEmpty()) {
                for (PNRReply.DataElementsMaster.DataElementsIndiv dataElementsIndiv : dataElementsIndivList) {

                    String segmentReferenceNumber = null;
                    String passengerReferenceNumber = null;

                    // Extract segmentReferenceNumber and passengerReferenceNumber
                    ReferenceInfoType referenceForDataElement = dataElementsIndiv.getReferenceForDataElement();

                    if (referenceForDataElement != null) {
                        List<ReferencingDetailsType111975C> reference = referenceForDataElement.getReference();

                        if (reference != null) {
                            for (ReferencingDetailsType111975C referencingDetailsType111975C : reference) {
                                String qualifier = referencingDetailsType111975C.getQualifier();
                                String referenceNumber = referencingDetailsType111975C.getNumber();

                                if (qualifier.equalsIgnoreCase("PT")) {
                                    passengerReferenceNumber = referenceNumber;
                                } else if (qualifier.equalsIgnoreCase("ST")) {
                                    segmentReferenceNumber = referenceNumber;
                                }
                            }
                        }
                    }

                    if (dataElementsIndiv.getElementManagementData() != null) {

                        ElementManagementSegmentType elementManagementData = dataElementsIndiv.getElementManagementData();
                        String segmentName = elementManagementData.getSegmentName();

                        if (segmentName != null && segmentName.equalsIgnoreCase("SSR")) {
                            SpecialRequirementsDetailsTypeI38284S serviceRequest = dataElementsIndiv.getServiceRequest();
                            if (serviceRequest != null && serviceRequest.getSsr() != null) {
                                SpecialRequirementsTypeDetailsTypeI ssr = serviceRequest.getSsr();

                                String ssrType = ssr.getType();

                                if (mealCodeNameMap.containsKey(ssrType)) {

                                    FreeMealsDetails freeMealsDetails = new FreeMealsDetails();

                                    // Match segmentReferenceNumber with segmentRef
                                    if (segmentReferenceNumber != null && segmentMap.containsKey(segmentReferenceNumber)) {
                                        Map<String, String> segmentDetails = segmentMap.get(segmentReferenceNumber);
                                        freeMealsDetails.setOrigin(segmentDetails.get("origin"));
                                        freeMealsDetails.setDestination(segmentDetails.get("destination"));
                                        freeMealsDetails.setAirlineCode(segmentDetails.get("airlineCode"));
                                        freeMealsDetails.setFlightNumber(segmentDetails.get("flightNumber"));
                                        freeMealsDetails.setDepartureDate(segmentDetails.get("departureDate"));
                                        freeMealsDetails.setAmadeusSegmentRef(segmentReferenceNumber);
                                    }

                                    freeMealsDetails.setMealCode(ssrType);
                                    freeMealsDetails.setMealName(mealCodeNameMap.get(ssrType));
                                    freeMealsDetails.setMealQuantity(ssr.getQuantity());
                                    freeMealsDetails.setMealStatus(ssr.getStatus());

                                    StringBuilder stringBuilder = new StringBuilder();
                                    for (String text : ssr.getFreeText()) {
                                        if (text != null && !text.trim().isEmpty()) {
                                            if (stringBuilder.length() > 0) {
                                                stringBuilder.append(". ");
                                            }
                                            stringBuilder.append(text.trim());
                                        }
                                    }
                                    if (stringBuilder.length() > 0) {
                                        stringBuilder.append(".");
                                    }
                                    freeMealsDetails.setComments(stringBuilder.toString());

                                    if (passengerReferenceNumber != null) {
                                        freeMealsDetails.setAmadeusPaxReference(passengerReferenceNumber);
                                        freeMealsDetails.setPaxName(paxRefAndNameMap.get(passengerReferenceNumber));
                                    }

                                    freeMealsDetailsList.add(freeMealsDetails);
                                }

                            }

                        }

                    }
                }
            }

            return freeMealsDetailsList;

        } catch (Exception e) {
            logger.debug("Error with getting free meals details from PNR {} : ", e.getMessage(), e);
            return null;
        }
    }


    public static List<FreeSeatDetails> getFreeSeatDetailsFromPnr(PNRReply pnrReply) {

        List<FreeSeatDetails> freeSeatList = new ArrayList<>();

        try {
            // Get passenger reference and name map
            List<TravellerInfo> travellerInfoList = pnrReply.getTravellerInfo();
            Map<String, String> paxRefAndNameMap = AmadeusBookingHelper.getPaxRefAndNameMap(travellerInfoList);

            // Get segment map
            Map<String, Map<String, String>> segmentMap = getSegmentNameMapFromPnr(pnrReply);

            // Process DataElementsIndiv for SSR
            List<PNRReply.DataElementsMaster.DataElementsIndiv> dataElementsIndivList = pnrReply.getDataElementsMaster().getDataElementsIndiv();

            if (!dataElementsIndivList.isEmpty()) {
                for (PNRReply.DataElementsMaster.DataElementsIndiv dataElementsIndiv : dataElementsIndivList) {
                    String segmentReferenceNumber = null;
                    String passengerReferenceNumber = null;

                    // Extract segmentReferenceNumber and passengerReferenceNumber
                    ReferenceInfoType referenceForDataElement = dataElementsIndiv.getReferenceForDataElement();
                    if (referenceForDataElement != null) {
                        List<ReferencingDetailsType111975C> reference = referenceForDataElement.getReference();
                        if (reference != null) {
                            for (ReferencingDetailsType111975C referencingDetailsType111975C : reference) {
                                String qualifier = referencingDetailsType111975C.getQualifier();
                                String referenceNumber = referencingDetailsType111975C.getNumber();
                                if (qualifier.equalsIgnoreCase("PT")) {
                                    passengerReferenceNumber = referenceNumber;
                                } else if (qualifier.equalsIgnoreCase("ST")) {
                                    segmentReferenceNumber = referenceNumber;
                                }
                            }
                        }
                    }

                    if (dataElementsIndiv.getElementManagementData() != null) {
                        ElementManagementSegmentType elementManagementData = dataElementsIndiv.getElementManagementData();
                        String segmentName = elementManagementData.getSegmentName();

                        if (segmentName != null && segmentName.equalsIgnoreCase("SSR")) {
                            SpecialRequirementsDetailsTypeI38284S serviceRequest = dataElementsIndiv.getServiceRequest();

                            if (!dataElementsIndiv.getSeatPaxInfo().isEmpty() && serviceRequest != null && serviceRequest.getSsr() != null) {
                                SpecialRequirementsTypeDetailsTypeI ssr = serviceRequest.getSsr();
                                String ssrType = ssr.getType();

                                FreeSeatDetails freeSeatDetails = new FreeSeatDetails();

                                // Match segmentReferenceNumber with segmentRef
                                if (segmentReferenceNumber != null && segmentMap.containsKey(segmentReferenceNumber)) {
                                    Map<String, String> segmentDetails = segmentMap.get(segmentReferenceNumber);
                                    freeSeatDetails.setOrigin(segmentDetails.get("origin"));
                                    freeSeatDetails.setDestination(segmentDetails.get("destination"));
                                    freeSeatDetails.setAirlineCode(segmentDetails.get("airlineCode"));
                                    freeSeatDetails.setFlightNumber(segmentDetails.get("flightNumber"));
                                    freeSeatDetails.setDepartureDate(segmentDetails.get("departureDate"));
                                    freeSeatDetails.setAmadeusSegmentRef(segmentReferenceNumber);
                                }

                                freeSeatDetails.setSeatStatus(ssr.getStatus());

                                //NSST is seatType
                                freeSeatDetails.setSeatType(ssrType);

                                if (serviceRequest.getSsrb() != null && !serviceRequest.getSsrb().isEmpty()) {
                                    SpecialRequirementsDataDetailsTypeI64826C ssrb = serviceRequest.getSsrb().get(0);
                                    freeSeatDetails.setSeatNumber(ssrb.getData());
                                    if (ssrb.getSeatType() != null && !ssrb.getSeatType().isEmpty()) {
                                        for (String seatType : ssrb.getSeatType()) {
                                            if (seatType != null && (seatType.equals("W") || seatType.equals("A"))) {
                                                // W/A is seatCode
                                                freeSeatDetails.setSeatCode(seatType);
                                                if (seatNameMap.containsKey(seatType)) {
                                                    freeSeatDetails.setSeatName(seatNameMap.get(seatType));

                                                }
                                                break;
                                            }
                                        }
                                    }
                                }

                                if (passengerReferenceNumber != null) {
                                    freeSeatDetails.setAmadeusPaxReference(passengerReferenceNumber);
                                    freeSeatDetails.setPaxName(paxRefAndNameMap.get(passengerReferenceNumber));
                                }

                                freeSeatList.add(freeSeatDetails);
                            }
                        }
                    }
                }
            }

            return freeSeatList;

        } catch (Exception e) {
            logger.debug("Error with getting free seat details from PNR {} : ", e.getMessage(), e);
            return null;
        }
    }


    private static Map<String, Map<String, String>> getSegmentNameMapFromPnr(PNRReply pnrReply) {

        Map<String, Map<String, String>> segmentMap = new HashMap<>();

        // Process itineraryInfo for all AIR segments dynamically
        List<PNRReply.OriginDestinationDetails> originDestinationDetailsList = pnrReply.getOriginDestinationDetails();

        if (!originDestinationDetailsList.isEmpty()) {
            for (PNRReply.OriginDestinationDetails originDestinationDetails : originDestinationDetailsList) {
                List<PNRReply.OriginDestinationDetails.ItineraryInfo> itineraryInfo = originDestinationDetails.getItineraryInfo();

                for (PNRReply.OriginDestinationDetails.ItineraryInfo itineraryInfo1 : itineraryInfo) {
                    ElementManagementSegmentType elementManagementItinerary = itineraryInfo1.getElementManagementItinerary();
                    if (elementManagementItinerary != null) {
                        String segmentName = elementManagementItinerary.getSegmentName();

                        if (segmentName != null && segmentName.equalsIgnoreCase("AIR")) {
                            Map<String, String> segmentDetails = new HashMap<>();
                            String segmentRef = null;

                            // Extract segmentRef
                            if (elementManagementItinerary.getReference() != null) {
                                segmentRef = elementManagementItinerary.getReference().getNumber().toString();
                            }

                            // Extract origin, destination, airlineCode, flightNumber
                            TravelProductInformationTypeI185722S travelProduct = itineraryInfo1.getTravelProduct();
                            if (travelProduct != null) {
                                LocationTypeI192814C boardPointDetail = travelProduct.getBoardpointDetail();
                                if (boardPointDetail != null) {
                                    segmentDetails.put("origin", boardPointDetail.getCityCode());
                                }

                                LocationTypeI192814C offPointDetail = travelProduct.getOffpointDetail();
                                if (offPointDetail != null) {
                                    segmentDetails.put("destination", offPointDetail.getCityCode());
                                }

                                CompanyIdentificationTypeI192810C companyDetail = travelProduct.getCompanyDetail();
                                if (companyDetail != null) {
                                    segmentDetails.put("airlineCode", companyDetail.getIdentification());
                                }

                                ProductIdentificationDetailsTypeI192811C productDetails = travelProduct.getProductDetails();
                                if (productDetails != null) {
                                    segmentDetails.put("flightNumber", productDetails.getIdentification());
                                }

                                ProductDateTimeTypeI260466C product = travelProduct.getProduct();
                                if (product != null) {
                                    String departureDate = product.getDepDate();
                                    segmentDetails.put("departureDate",departureDate);
                                }
                            }

                            // Store in segmentMap if segmentRef exists
                            if (segmentRef != null) {
                                segmentMap.put(segmentRef, segmentDetails);
                            }
                        }
                    }
                }
            }
        }
        return segmentMap;
    }

    public void getTicketEligibilityFromTicketDisplayTSTReply(TicketDisplayTSTReply ticketDisplayTSTReply, TravellerMasterInfo masterInfo) {

        boolean isSameValidatingCarrier = false;
        String validatingCarrierCode = null;

        try {
            if (ticketDisplayTSTReply != null) {

                List<TicketDisplayTSTReply.FareList> fareList = ticketDisplayTSTReply.getFareList();
                if (fareList != null && !fareList.isEmpty()) {

                    for (int i = 0; i < fareList.size(); i++) {

                        TicketDisplayTSTReply.FareList fareList1 = fareList.get(i);
                        TransportIdentifierType validatingCarrier = fareList1.getValidatingCarrier();

                        if (validatingCarrier != null) {
                            CompanyIdentificationTypeI27095C carrierInformation = validatingCarrier.getCarrierInformation();

                            if (carrierInformation != null) {
                                String carrierCode = carrierInformation.getCarrierCode();
                                if (i == 0) {
                                    validatingCarrierCode = carrierCode;
                                    isSameValidatingCarrier = true;
                                } else {
                                    if (!carrierCode.equals(validatingCarrierCode)) {
                                        isSameValidatingCarrier = false;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (isSameValidatingCarrier) {

                String ticketingOfficeId = masterInfo.getTicketingOfficeId();

                if (ticketingOfficeId != null) {

                    if (amadeusSourceOfficeService!=null && amadeusSourceOfficeService.getPrioritySourceOffice().getOfficeId().equalsIgnoreCase(ticketingOfficeId)) {

                        BOMVS34C3Eligibility bomvs34C3Eligibility = BOMVS34C3Eligibility.getEligibleAirlineCodeByValidatingCarrier(validatingCarrierCode);

                        if (bomvs34C3Eligibility != null) {
                            masterInfo.setAutoReIssue(bomvs34C3Eligibility.isReissue());
                            masterInfo.setAutoReFund(bomvs34C3Eligibility.isRefund());
                        }

                    } else if (amadeusSourceOfficeService!=null && amadeusSourceOfficeService.getDelhiSourceOffice() != null && amadeusSourceOfficeService.getDelhiSourceOffice().getOfficeId().equalsIgnoreCase(ticketingOfficeId)) {

                        DELVS38LFEligibility delvs38LFEligibility = DELVS38LFEligibility.getEligibleAirlineCodeByValidatingCarrier(validatingCarrierCode);

                        if (delvs38LFEligibility != null) {
                            masterInfo.setAutoReIssue(delvs38LFEligibility.isReissue());
                            masterInfo.setAutoReFund(delvs38LFEligibility.isRefund());
                        }

                    }
                }
            } else {
                masterInfo.setAutoReIssue(false);
                masterInfo.setAutoReFund(false);
            }

        } catch (Exception e) {
            logger.debug("Error with getTicketEligibilityFromTicketDisplayTSTReply {} : ", e.getMessage(), e);
        }

    }
}