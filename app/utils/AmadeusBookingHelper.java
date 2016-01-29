package utils;

import com.amadeus.xml.itares_05_2_ia.AirSellFromRecommendationReply;
import com.amadeus.xml.pnracc_11_3_1a.PNRReply;
import com.amadeus.xml.pnracc_11_3_1a.PNRReply.TravellerInfo;
import com.amadeus.xml.pnracc_11_3_1a.PNRReply.TravellerInfo.PassengerData;
import com.amadeus.xml.pnracc_11_3_1a.ReferencingDetailsType111975C;
import com.amadeus.xml.tpcbrr_12_4_1a.FarePricePNRWithBookingClassReply;
import com.amadeus.xml.tpcbrr_12_4_1a.FarePricePNRWithBookingClassReply.FareList;
import com.amadeus.xml.tpcbrr_12_4_1a.FarePricePNRWithBookingClassReply.FareList.TaxInformation;
import com.amadeus.xml.tpcbrr_12_4_1a.MonetaryInformationDetailsType223826C;
import com.amadeus.xml.ttstrr_13_1_1a.MonetaryInformationDetailsTypeI211824C;
import com.amadeus.xml.ttstrr_13_1_1a.ReferencingDetailsTypeI;
import com.amadeus.xml.ttstrr_13_1_1a.TicketDisplayTSTReply;
import com.compassites.constants.AmadeusConstants;
import com.compassites.model.*;
import com.compassites.model.traveller.Traveller;
import com.compassites.model.traveller.TravellerMasterInfo;
import models.Airline;
import models.Airport;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Yaseen on 18-12-2014.
 */
public class AmadeusBookingHelper {

    static org.slf4j.Logger logger = LoggerFactory.getLogger("gds");

    public static List<TaxDetails> getTaxDetails(FarePricePNRWithBookingClassReply pricePNRReply){
        List<TaxDetails> taxDetailsList = new ArrayList<>();
        for(FarePricePNRWithBookingClassReply.FareList fareList : pricePNRReply.getFareList()){
            for(FarePricePNRWithBookingClassReply.FareList.TaxInformation taxInformation :fareList.getTaxInformation()){
                TaxDetails taxDetails = new TaxDetails();
                taxDetails.setTaxCode(taxInformation.getTaxDetails().getTaxType().getIsoCountry());
                taxDetails.setTaxAmount(new BigDecimal(taxInformation.getAmountDetails().getFareDataMainInformation().getFareAmount()));
                taxDetailsList.add(taxDetails);
            }
        }
        return taxDetailsList;
    }

    public static boolean validateFlightAvailability(AirSellFromRecommendationReply sellFromRecommendation,String amadeusFlightAvailibilityCode){
        boolean errors = true;
        for (AirSellFromRecommendationReply.ItineraryDetails itinerary : sellFromRecommendation.getItineraryDetails()){
            for(AirSellFromRecommendationReply.ItineraryDetails.SegmentInformation segmentInformation : itinerary.getSegmentInformation()){
                for(String statusCode : segmentInformation.getActionDetails().getStatusCode()){
                    if(!amadeusFlightAvailibilityCode.equals(statusCode)){
                        errors = false;
                    }
                }
            }
        }
        return errors;
    }

    public static void checkFare(FarePricePNRWithBookingClassReply pricePNRReply, PNRResponse pnrResponse, TravellerMasterInfo travellerMasterInfo){
    	
    	// TODO: re-factor
    	int adultCount = 0, childCount = 0, infantCount = 0;
		for (Traveller traveller : travellerMasterInfo.getTravellersList()) {
			PassengerTypeCode passengerType = DateUtility.getPassengerTypeFromDOB(traveller.getPassportDetails().getDateOfBirth());
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
        /*List<FareList> fareList = pricePNRReply.getFareList();
        for(FareList fare : fareList) {
        	int paxCount = 0;
        	String paxType = fare.getSegmentInformation().get(0).getFareQualifier().getFareBasisDetails().getDiscTktDesignator();
        	if(paxType.equalsIgnoreCase("ADT") || paxType.equalsIgnoreCase("SEA") || paxType.equalsIgnoreCase("SC")) {
                logger.debug("adultCount : " + adultCount);
                paxCount = adultCount;
        	} else if(paxType.equalsIgnoreCase("CHD") || paxType.equalsIgnoreCase("CH")) {
        		paxCount = childCount;
                logger.debug("adultCount : " + childCount);
        	} else if(paxType.equalsIgnoreCase("INF") || paxType.equalsIgnoreCase("IN")) {
        		paxCount = infantCount;
                logger.debug("adultCount : " + infantCount);
        	}
            logger.debug("passenger counts : " + paxCount);
            logger.debug("size of fareData : " + fare.getFareDataInformation().getFareDataSupInformation().size());
            for(FareDataSupInformation fareData : fare.getFareDataInformation().getFareDataSupInformation()) {
        		BigDecimal amount = new BigDecimal(fareData.getFareAmount());
        		if(totalFareIdentifier.equals(fareData.getFareDataQualifier())) {
        			totalFare = totalFare.add(amount.multiply(new BigDecimal(paxCount)));
                    logger.debug("=======================>> Setting new total fare: " + totalFare);
                } else {
                    baseFare = baseFare.add(amount.multiply(new BigDecimal(paxCount)));
        		}
        	}
        }*/

        PricingInformation pricingInformation = getPricingInfo(pricePNRReply, adultCount, childCount, infantCount);
        totalFare = pricingInformation.getTotalPrice();
        baseFare = pricingInformation.getBasePrice();
        pnrResponse.setPricingInfo(pricingInformation);

        BigDecimal searchPrice = new BigDecimal(0);
        if(travellerMasterInfo.isSeamen()) {
            searchPrice = travellerMasterInfo.getItinerary().getSeamanPricingInformation().getTotalPriceValue();
        } else {
            searchPrice = travellerMasterInfo.getItinerary().getPricingInformation().getTotalPriceValue();
        }


        logger.debug("Total price before comparing :" + searchPrice + " changed Price : " + totalFare);
        if(totalFare.compareTo(searchPrice) == 0) {
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


    public static boolean createTickets(IssuanceResponse issuanceResponse, IssuanceRequest issuanceRequest, PNRReply gdsPNRReply){
        int ticketsCount = 0;
        Map<String,Object> airSegmentRefMap = new HashMap<>();
        Map<String,Object> travellerMap = new HashMap<>();
        for(PNRReply.OriginDestinationDetails originDestination : gdsPNRReply.getOriginDestinationDetails()){
            for(PNRReply.OriginDestinationDetails.ItineraryInfo itineraryInfo : originDestination.getItineraryInfo()){
                String segmentRef = itineraryInfo.getElementManagementItinerary().getReference().getQualifier()+itineraryInfo.getElementManagementItinerary().getReference().getNumber();
                airSegmentRefMap.put(segmentRef,itineraryInfo);
            }
        }
        for(PNRReply.TravellerInfo travellerInfo : gdsPNRReply.getTravellerInfo()){
            String key = travellerInfo.getElementManagementPassenger().getReference().getQualifier() + travellerInfo.getElementManagementPassenger().getReference().getNumber();
            travellerMap.put(key,travellerInfo);
        }
        for(PNRReply.DataElementsMaster.DataElementsIndiv dataElementsDiv :gdsPNRReply.getDataElementsMaster().getDataElementsIndiv()){
            if("FA".equals(dataElementsDiv.getElementManagementData().getSegmentName())){
                String passengerRef = "";
                List<String> segmentRefList = new ArrayList<>();
                String travellerKey = "";
                for(ReferencingDetailsType111975C reference :dataElementsDiv.getReferenceForDataElement().getReference()){
                    travellerKey = reference.getQualifier()+ reference.getNumber();
                    if(travellerMap.containsKey(travellerKey)){
                        passengerRef = travellerKey;
                    }else {
                        segmentRefList.add(travellerKey);
                    }
                }
                PNRReply.TravellerInfo traveller = (PNRReply.TravellerInfo)travellerMap.get(passengerRef);
                String lastName = traveller.getPassengerData().get(0).getTravellerInformation().getTraveller().getSurname();
                String name = traveller.getPassengerData().get(0).getTravellerInformation().getPassenger().get(0).getFirstName();
               /* String[] nameArray = name.split(" ");
                String firstName = nameArray[0];
                String middleName = (nameArray.length > 1)? nameArray[1]: "";*/

                name = name.replaceAll("\\s+", "");
                lastName = lastName.replaceAll("\\s+", "");

                for(Traveller traveller1 : issuanceRequest.getTravellerList()){
                    String contactName = "";
                    if(traveller1.getPersonalDetails().getMiddleName() != null){
                        contactName = traveller1.getPersonalDetails().getFirstName() + traveller1.getPersonalDetails().getMiddleName();

                    }else {{
                        contactName = traveller1.getPersonalDetails().getFirstName();
                    }}
                    contactName = contactName.replaceAll("\\s+", "");
                    String contactLastName = traveller1.getPersonalDetails().getLastName();
                    contactLastName  = contactLastName.replaceAll("\\s+", "");
                    if(name.equalsIgnoreCase(contactName)
                            && lastName.equalsIgnoreCase(contactLastName)){
                    	String freeText = dataElementsDiv.getOtherDataFreetext().get(0).getLongFreetext();
                        String[] freeTextArr = freeText.split("/");
                        String ticketNumber = freeTextArr[0].substring(3);
                        Map<String,String> ticketMap = new HashMap<>();
                        for(String segmentRef : segmentRefList){
                            PNRReply.OriginDestinationDetails.ItineraryInfo itineraryInfo = (PNRReply.OriginDestinationDetails.ItineraryInfo)airSegmentRefMap.get(segmentRef);
                            String key = itineraryInfo.getTravelProduct().getBoardpointDetail().getCityCode()+
                                    itineraryInfo.getTravelProduct().getOffpointDetail().getCityCode() + traveller1.getContactId();
                            ticketMap.put(key.toLowerCase(),ticketNumber);
                            logger.debug("created ticket for " + key + "ticket count " + ticketsCount);
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
        if(issuanceRequest.isSeamen()){
            passengerCount = issuanceRequest.getAdultCount() + issuanceRequest.getChildCount() + issuanceRequest.getInfantCount();

        }else {
            passengerCount = issuanceRequest.getAdultCount() + issuanceRequest.getChildCount();
        }
        logger.debug("ticketCount : " + ticketsCount + " passengerCount " + passengerCount);
        if(ticketsCount >= passengerCount){
            return true;
        }

        return false;
    }


    public static List<Journey> getJourneyListFromPNRResponse(PNRReply gdsPNRReply){

        List<Journey> journeyList = new ArrayList<>();
        List<AirSegmentInformation> airSegmentList = new ArrayList<>();


        Journey journey = new Journey();
        for (PNRReply.OriginDestinationDetails originDestinationDetails : gdsPNRReply
                .getOriginDestinationDetails()) {
            for (PNRReply.OriginDestinationDetails.ItineraryInfo itineraryInfo : originDestinationDetails
                    .getItineraryInfo()) {
                AirSegmentInformation airSegmentInformation = new AirSegmentInformation();

                String fromLoc = itineraryInfo.getTravelProduct()
                        .getBoardpointDetail().getCityCode();
                String toLoc = itineraryInfo.getTravelProduct()
                        .getOffpointDetail().getCityCode();
                airSegmentInformation.setFromLocation(fromLoc);
                airSegmentInformation.setToLocation(toLoc);
                airSegmentInformation.setBookingClass(itineraryInfo.getTravelProduct().getProductDetails().getClassOfService());
                Airport fromAirport = Airport
                        .getAiport(airSegmentInformation.getFromLocation());
                Airport toAirport = Airport.getAiport(airSegmentInformation
                        .getToLocation());

                SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyHHmm");
                String DATE_FORMAT = "ddMMyyHHmm";
                DateTimeFormatter DATETIME_FORMATTER = DateTimeFormat
                        .forPattern(DATE_FORMAT);
                DateTimeZone dateTimeZone = DateTimeZone.forID(fromAirport
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
                dateTimeZone = DateTimeZone.forID(toAirport.getTime_zone());
                DateTime arrivalDate = DATETIME_FORMATTER.withZone(
                        dateTimeZone).parseDateTime(fromDateTime);

					/*
					 * Date fromDate =
					 * format.parse(itineraryInfo.getTravelProduct
					 * ().getProduct().getArrDate()); Date toDate =
					 * format.parse(
					 * itineraryInfo.getTravelProduct().getProduct()
					 * .getDepDate());
					 */

                airSegmentInformation.setFlightNumber(itineraryInfo
                        .getTravelProduct().getProductDetails()
                        .getIdentification());

                // airSegmentInformation.setDepartureDate(toDate);
                airSegmentInformation.setDepartureDate(departureDate
                        .toDate());
                airSegmentInformation.setDepartureTime(departureDate
                        .toString());
                airSegmentInformation
                        .setArrivalTime(arrivalDate.toString());
                airSegmentInformation.setArrivalDate(arrivalDate.toDate());

                // logger.debug("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>\n"+fromDateTime);
                // logger.debug("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>\n"+toDateTime);
                airSegmentInformation.setFromDate(fromDateTime);
                airSegmentInformation.setToDate(toDateTime);
					/*
					 * airSegmentInformation.setFromDate(format.format(fromDateTime
					 * ));
					 * airSegmentInformation.setToDate(format.format(toDateTime
					 * ));
					 */

                // String arrivalTer =
                // itineraryInfo.getFlightDetail().getArrivalStationInfo().getTerminal();
                if (itineraryInfo.getFlightDetail() != null
                        && itineraryInfo.getFlightDetail()
                        .getArrivalStationInfo() != null) {
                    airSegmentInformation.setFromTerminal(itineraryInfo
                            .getFlightDetail().getArrivalStationInfo()
                            .getTerminal());
                }
                if (itineraryInfo.getFlightDetail() != null
                        && itineraryInfo.getFlightDetail()
                        .getDepartureInformation() != null) {
                    airSegmentInformation.setToTerminal(itineraryInfo
                            .getFlightDetail().getDepartureInformation()
                            .getDepartTerminal());
                }
                airSegmentInformation.setCarrierCode(itineraryInfo.getTravelProduct().getCompanyDetail().getIdentification());
                airSegmentInformation.setAirline(Airline.getAirlineByCode(itineraryInfo.getTravelProduct().getCompanyDetail().getIdentification()));
                airSegmentInformation.setFromAirport(Airport.getAiport(fromLoc));
                airSegmentInformation.setToAirport(Airport.getAiport(toLoc));
                String responseEquipment = itineraryInfo.getFlightDetail()
                        .getProductDetails().getEquipment();
                if (responseEquipment != null) {
                    itineraryInfo.getFlightDetail().getProductDetails()
                            .getEquipment();
                }
                airSegmentInformation.setEquipment(responseEquipment);

                airSegmentList.add(airSegmentInformation);
            }
            journey.setAirSegmentList(airSegmentList);
            journeyList.add(journey);
        }

        return journeyList;
    }


    public static  PricingInformation getPricingForDownsellAndConversion(FarePricePNRWithBookingClassReply pricePNRReply, String totalFareIdentifier, int adultCount){
        BigDecimal totalFare = new BigDecimal(0);
        BigDecimal totalTax = new BigDecimal(0);
        BigDecimal baseFare = new BigDecimal(0);
        List<FareList> fareList = pricePNRReply.getFareList();
        PricingInformation pricingInformation = new PricingInformation();
        for(FareList fare : fareList) {
            int paxCount = 0;
            String paxType = fare.getSegmentInformation().get(0).getFareQualifier().getFareBasisDetails().getDiscTktDesignator();
            if(paxType.equalsIgnoreCase("ADT") || paxType.equalsIgnoreCase("SEA") || paxType.equalsIgnoreCase("SC")) {
                logger.debug("adultCount : " + adultCount);
                paxCount = adultCount;
            }
            logger.debug("passenger counts : " + paxCount);
            logger.debug("size of fareData : " + fare.getFareDataInformation().getFareDataSupInformation().size());
            for(MonetaryInformationDetailsType223826C fareData : fare.getFareDataInformation().getFareDataSupInformation()) {
                BigDecimal amount = new BigDecimal(fareData.getFareAmount());
                if(totalFareIdentifier.equals(fareData.getFareDataQualifier())) {
                    totalFare = totalFare.add(amount.multiply(new BigDecimal(paxCount)));
                    logger.debug("=======================>> Setting new total fare: " + totalFare);
                }
                //Todo --commented below line as the base fare string is B need to validate
//                if("E".equalsIgnoreCase(fareData.getFareDataQualifier())) {
                if("B".equalsIgnoreCase(fareData.getFareDataQualifier())) {
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
        pricingInformation.setPassengerTaxes(getTaxBreakup(pricePNRReply, passengerTypeMap));
        return pricingInformation;
    }

	public static PricingInformation getPricingInfo(
			FarePricePNRWithBookingClassReply pricePNRReply,
			int adultCount, int childCount,
			int infantCount) {
		BigDecimal totalFare = new BigDecimal(0);
        BigDecimal totalBaseFare = new BigDecimal(0);
        BigDecimal adtBaseFare = new BigDecimal(0);
        BigDecimal chdBaseFare = new BigDecimal(0);
        BigDecimal infBaseFare = new BigDecimal(0);
        String currency = null;
        
        List<FareList> fareList = pricePNRReply.getFareList();
        PricingInformation pricingInformation = new PricingInformation();
        for(FareList fare : fareList) {
            int paxCount = 0;
            String paxType = fare.getSegmentInformation().get(0).getFareQualifier().getFareBasisDetails().getDiscTktDesignator();
            if("chd".equalsIgnoreCase(paxType) || "ch".equalsIgnoreCase(paxType)) {
            	paxCount = childCount;
			} else if("inf".equalsIgnoreCase(paxType) || "in".equalsIgnoreCase(paxType)) {
				paxCount = infantCount;
			} else if("adt".equalsIgnoreCase(paxType)){
				paxCount = adultCount;
			}else {
                paxCount = adultCount + childCount + infantCount;
            }
//            currency = fare.getFareDataInformation().getFareDataSupInformation().get(0).getFareCurrency();
            boolean equivalentFareAvailable = false;
            BigDecimal baseFare = new BigDecimal(0);
            for(MonetaryInformationDetailsType223826C fareData : fare.getFareDataInformation().getFareDataSupInformation()) {
                BigDecimal amount = new BigDecimal(fareData.getFareAmount());
                if(AmadeusConstants.TOTAL_FARE_IDENTIFIER.equals(fareData.getFareDataQualifier())) {
                    totalFare = totalFare.add(amount.multiply(new BigDecimal(paxCount)));
                }
                if("B".equalsIgnoreCase(fareData.getFareDataQualifier()) || "E".equalsIgnoreCase(fareData.getFareDataQualifier())) {
                    if(!equivalentFareAvailable){
                        baseFare = amount;
                        currency = fareData.getFareCurrency();
                    }
                }
                if("E".equalsIgnoreCase(fareData.getFareDataQualifier())){
                    equivalentFareAvailable = true;
                }

            }

            totalBaseFare = totalBaseFare.add(baseFare.multiply(new BigDecimal(paxCount)));
            if("chd".equalsIgnoreCase(paxType) || "ch".equalsIgnoreCase(paxType)) {
                chdBaseFare = baseFare;
            } else if("inf".equalsIgnoreCase(paxType) || "in".equalsIgnoreCase(paxType)) {
                infBaseFare = baseFare;
            } else {
                adtBaseFare = baseFare;
            }
        }
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
        pricingInformation.setPassengerTaxes(getTaxBreakup(pricePNRReply, passengerTypeMap));
        return pricingInformation;
	}

    public static void setTaxBreakup(PNRResponse pnrResponse, TravellerMasterInfo travellerMasterInfo, FarePricePNRWithBookingClassReply pricePNRReply) {
        Map<String, Integer> passengerTypeMap = getPassengerTypeCount(travellerMasterInfo.getTravellersList());
        PricingInformation pricingInfo = travellerMasterInfo.isSeamen() ? travellerMasterInfo
                .getItinerary().getSeamanPricingInformation() : travellerMasterInfo
                .getItinerary().getPricingInformation();
        List<PassengerTax> passengerTaxes = getTaxBreakup(pricePNRReply, passengerTypeMap);
        pricingInfo.setPassengerTaxes(passengerTaxes);
        pnrResponse.setPricingInfo(pricingInfo);
    }

    public static List<PassengerTax> getTaxBreakup(FarePricePNRWithBookingClassReply pricePNRReply, Map<String, Integer> passengerTypeMap){
        List<PassengerTax> passengerTaxes = new ArrayList<>();
        for (FareList fare : pricePNRReply.getFareList()) {
            FareList.SegmentInformation segmentInfo = fare.getSegmentInformation().get(0);
            List<TaxInformation> taxInfos = fare.getTaxInformation();
            if (segmentInfo != null && taxInfos.size() > 0) {
                String paxType = segmentInfo.getFareQualifier().getFareBasisDetails().getDiscTktDesignator();
                PassengerTax passengerTax = null;
                if(paxType.equalsIgnoreCase("CH") || paxType.equalsIgnoreCase("CHD")) {
                	passengerTax = setTaxDetails(taxInfos, "CHD", passengerTypeMap.get("childCount"));
        		} else if(paxType.equalsIgnoreCase("IN") || paxType.equalsIgnoreCase("INF")) {
        			passengerTax = setTaxDetails(taxInfos, "INF", passengerTypeMap.get("infantCount"));
        		} else {
        			passengerTax = setTaxDetails(taxInfos, "ADT", passengerTypeMap.get("adultCount"));
        		}
                passengerTaxes.add(passengerTax);
            }
        }
        return  passengerTaxes;
    }

	private static PassengerTax setTaxDetails(List<TaxInformation> taxInfos, String passengerType, int count) {
		PassengerTax passengerTax = new PassengerTax();
		passengerTax.setPassengerType(passengerType);
		passengerTax.setPassengerCount(count);
		Map<String, BigDecimal> taxes = new HashMap<>();
		for(TaxInformation taxInfo : taxInfos) {
			String amount = taxInfo.getAmountDetails().getFareDataMainInformation().getFareAmount();
			String taxCode = taxInfo.getTaxDetails().getTaxType().getIsoCountry();
			if(taxes.containsKey(taxCode)) {
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
		for(TravellerInfo travellerInfo : travellerinfoList) {
			PassengerData paxData = travellerInfo.getPassengerData().get(0);
			String paxType = paxData.getTravellerInformation().getPassenger().get(0).getType();
			if("chd".equalsIgnoreCase(paxType) || "ch".equalsIgnoreCase(paxType)) {
				childCount++;
			} else if("inf".equalsIgnoreCase(paxType) || "in".equalsIgnoreCase(paxType)) {
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

    public static boolean checkForSimultaneousChange(PNRReply pnrReply){

        boolean simultaneousChange = false;
        if(pnrReply.getGeneralErrorInfo() != null && pnrReply.getGeneralErrorInfo().size() > 0){
            for(PNRReply.GeneralErrorInfo generalErrorInfo : pnrReply.getGeneralErrorInfo()){
                String errorText = StringUtils.join(generalErrorInfo.getMessageErrorText().getText());
                //todo check the string contains works with spaces and all other cases , use regular expression
                if(errorText.contains(AmadeusConstants.SIMULTANEOUS_PNR_CHANGE)){
                    simultaneousChange = true;
                }
            }
        }

        return simultaneousChange;
    }


    public static PricingInformation getPricingInfoFromTST(PNRReply gdsPNRReply, TicketDisplayTSTReply ticketDisplayTSTReply, boolean isSeamen) {

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

        Map<String,Object> airSegmentRefMap = new HashMap<>();
        Map<String,Object> travellerMap = new HashMap<>();
        Map<String, String> passengerType = new HashMap<>();

        for(PNRReply.OriginDestinationDetails originDestination : gdsPNRReply.getOriginDestinationDetails()){
            for(PNRReply.OriginDestinationDetails.ItineraryInfo itineraryInfo : originDestination.getItineraryInfo()){
                String segmentRef = "S"+itineraryInfo.getElementManagementItinerary().getReference().getNumber();
                String segments = itineraryInfo.getTravelProduct().getBoardpointDetail().getCityCode() + itineraryInfo.getTravelProduct().getOffpointDetail().getCityCode();
                airSegmentRefMap.put(segmentRef,segments);
            }
        }
        for(PNRReply.TravellerInfo travellerInfo : gdsPNRReply.getTravellerInfo()){
            String key = "P" + travellerInfo.getElementManagementPassenger().getReference().getNumber();
            travellerMap.put(key,travellerInfo);
            if(!isSeamen){
                PassengerData paxData = travellerInfo.getPassengerData().get(0);
                String paxType = paxData.getTravellerInformation().getPassenger().get(0).getType();
                String infantIndicator = paxData.getTravellerInformation().getPassenger().get(0).getInfantIndicator();
                if("chd".equalsIgnoreCase(paxType) || "ch".equalsIgnoreCase(paxType)) {
                    passengerType.put(key,"CHD");
                } else if("inf".equalsIgnoreCase(paxType) || "in".equalsIgnoreCase(paxType)) {
                    passengerType.put(key,"INF");
                }else {
                    passengerType.put(key,"ADT");
                }

                if(infantIndicator != null && "1".equalsIgnoreCase(infantIndicator)){
                    passengerType.put("PI"+ travellerInfo.getElementManagementPassenger().getReference().getNumber(), "INF");
                }
            }else {
                passengerType.put(key, "ADT");
            }
        }

        List<SegmentPricing> segmentPricingList = new ArrayList<>();
        List<PassengerTax> passengerTaxList = new ArrayList<>();
        boolean segmentWisePricing = false;
        for(TicketDisplayTSTReply.FareList fare : fareList) {
            BigDecimal totalFarePerPaxType = new BigDecimal(0);
            BigDecimal paxTotalFare = new BigDecimal(0);
            BigDecimal baseFareOfPerPaxType = new BigDecimal(0);

            SegmentPricing segmentPricing = new SegmentPricing();
            boolean equivalentFareAvailable = false;
            BigDecimal baseFare = new BigDecimal(0);
            for(MonetaryInformationDetailsTypeI211824C fareData : fare.getFareDataInformation().getFareDataSupInformation()) {
                BigDecimal amount = new BigDecimal(fareData.getFareAmount());
                if(AmadeusConstants.TOTAL_FARE_IDENTIFIER.equals(fareData.getFareDataQualifier())) {
                    paxTotalFare = amount;
                }
                if("B".equalsIgnoreCase(fareData.getFareDataQualifier()) || "E".equalsIgnoreCase(fareData.getFareDataQualifier())) {
                    if(!equivalentFareAvailable){
                        baseFare = amount;
                        currency = fareData.getFareCurrency();
                    }
                }
                if("E".equalsIgnoreCase(fareData.getFareDataQualifier())){
                    equivalentFareAvailable = true;
                }
            }

            int paxCount = fare.getPaxSegReference().getRefDetails().size();

            String paxTypeKey =  "P" + fare.getPaxSegReference().getRefDetails().get(0).getRefNumber();
            if("PI".equalsIgnoreCase(fare.getPaxSegReference().getRefDetails().get(0).getRefQualifier())){
                paxTypeKey = fare.getPaxSegReference().getRefDetails().get(0).getRefQualifier() + fare.getPaxSegReference().getRefDetails().get(0).getRefNumber();
            }

            String paxType = passengerType.get(paxTypeKey);
            totalFarePerPaxType = totalFarePerPaxType.add(paxTotalFare.multiply(new BigDecimal(paxCount)));
            baseFareOfPerPaxType = baseFareOfPerPaxType.add(baseFare.multiply(new BigDecimal(paxCount)));

            PassengerTax passengerTax = getTaxDetailsFromTST(fare.getTaxInformation(), paxType, paxCount);
            passengerTaxList.add(passengerTax);

            if(airSegmentRefMap.size() != fare.getSegmentInformation().size()){
                segmentWisePricing = true;
            }
            List<String> segmentKeys = new ArrayList<>();
            if(segmentWisePricing){
                for(TicketDisplayTSTReply.FareList.SegmentInformation segmentInformation : fare.getSegmentInformation()){
                    ReferencingDetailsTypeI referencingDetailsTypeI = segmentInformation.getSegmentReference().getRefDetails().get(0);
                    String key = referencingDetailsTypeI.getRefQualifier() + referencingDetailsTypeI.getRefNumber();
                    segmentKeys.add(airSegmentRefMap.get(key).toString().toLowerCase());
                }
            }
            segmentPricing.setSegmentKeysList(segmentKeys);
            segmentPricing.setTotalPrice(totalFarePerPaxType);
            segmentPricing.setBasePrice(baseFareOfPerPaxType);
            segmentPricing.setPassengerType(paxType);
            segmentPricing.setPassengerTax(passengerTax);
            segmentPricing.setPassengerCount(new Long(paxCount));
            segmentPricingList.add(segmentPricing);
            if("CHD".equalsIgnoreCase(paxType)){
                chdBaseFare = chdBaseFare.add(baseFare);
                chdTotalFare = chdTotalFare.add(paxTotalFare);
            }else if("INF".equalsIgnoreCase(paxType)){
                infBaseFare = infBaseFare.add(baseFare);
                infTotalFare = infTotalFare.add(paxTotalFare);
            }else {
                adtBaseFare = adtBaseFare.add(baseFare);
                adtTotalFare = adtTotalFare.add(paxTotalFare);
            }
            totalPriceOfBooking = totalPriceOfBooking.add(totalFarePerPaxType);
            basePriceOfBooking = basePriceOfBooking.add(baseFareOfPerPaxType);
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
        return pricingInformation;
    }


    public static PassengerTax getTaxDetailsFromTST(List<TicketDisplayTSTReply.FareList.TaxInformation> taxInformationList, String passengerType, int count){
        PassengerTax passengerTax = new PassengerTax();
        passengerTax.setPassengerType(passengerType);
        passengerTax.setPassengerCount(count);
        Map<String, BigDecimal> taxes = new HashMap<>();
        for(TicketDisplayTSTReply.FareList.TaxInformation taxInformation : taxInformationList){
            String amount = taxInformation.getAmountDetails().getFareDataMainInformation().getFareAmount();
            String taxCode = taxInformation.getTaxDetails().getTaxType().getIsoCountry();
            if(taxes.containsKey(taxCode)) {
                taxes.put(taxCode, taxes.get(taxCode).add(new BigDecimal(amount)));
            } else {
                taxes.put(taxCode, new BigDecimal(amount));
            }
        }

        passengerTax.setTaxes(taxes);

        return passengerTax;
    }


    public static int getNumberOfTST(List<Traveller> travellerList){

        int adultCount = 0, childCount = 0, infantCount = 0;
        int totalCount = 0;
        for(Traveller traveller : travellerList){
            PassengerTypeCode passengerTypeCode =DateUtility.getPassengerTypeFromDOB(traveller.getPassportDetails().getDateOfBirth());
            if(passengerTypeCode.name().equals(PassengerTypeCode.ADT.name())){
                adultCount = 1;
            }else if(passengerTypeCode.name().equals(PassengerTypeCode.CHD.name()))  {
                childCount = 1;
            }else {
                infantCount = 1;
            }

            totalCount = adultCount + childCount + infantCount;

        }
        return  totalCount;
    }
}
