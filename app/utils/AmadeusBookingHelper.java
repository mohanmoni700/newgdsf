package utils;

import com.amadeus.xml.itares_05_2_ia.AirSellFromRecommendationReply;
import com.amadeus.xml.pnracc_10_1_1a.PNRReply;
import com.amadeus.xml.tpcbrr_07_3_1a.FarePricePNRWithBookingClassReply;
import com.amadeus.xml.tpcbrr_07_3_1a.FarePricePNRWithBookingClassReply.FareList;
import com.amadeus.xml.tpcbrr_07_3_1a.FarePricePNRWithBookingClassReply.FareList.FareDataInformation.FareDataSupInformation;
import com.compassites.model.*;
import com.compassites.model.traveller.Traveller;
import com.compassites.model.traveller.TravellerMasterInfo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import play.libs.Json;

/**
 * Created by Yaseen on 18-12-2014.
 */
public class AmadeusBookingHelper {

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

    public static void checkFare(FarePricePNRWithBookingClassReply pricePNRReply, PNRResponse pnrResponse, TravellerMasterInfo travellerMasterInfo,String totalFareIdentifier){
    	
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
		BigDecimal totalTax = new BigDecimal(0);
        List<FareList> fareList = pricePNRReply.getFareList();
        for(FareList fare : fareList) {
        	int paxCount = 0;
        	String paxType = fare.getSegmentInformation().get(0).getFareQualifier().getFareBasisDetails().getDiscTktDesignator();
        	if(paxType.equalsIgnoreCase("ADT") || paxType.equalsIgnoreCase("SEA") || paxType.equalsIgnoreCase("SC")) {
                System.out.println("adultCount : " + adultCount);
                paxCount = adultCount;
        	} else if(paxType.equalsIgnoreCase("CHD") || paxType.equalsIgnoreCase("CH")) {
        		paxCount = childCount;
                System.out.println("adultCount : " + childCount);
        	} else if(paxType.equalsIgnoreCase("INF") || paxType.equalsIgnoreCase("IN")) {
        		paxCount = infantCount;
                System.out.println("adultCount : " + infantCount);
        	}
            System.out.println("passenger counts : " + paxCount);
            System.out.println("size of fareData : " + fare.getFareDataInformation().getFareDataSupInformation().size());
            for(FareDataSupInformation fareData : fare.getFareDataInformation().getFareDataSupInformation()) {
        		BigDecimal amount = new BigDecimal(fareData.getFareAmount());
        		if(totalFareIdentifier.equals(fareData.getFareDataQualifier())) {
        			totalFare = totalFare.add(amount.multiply(new BigDecimal(paxCount)));
                    System.out.println("=======================>> Setting new total fare: "+ totalFare );
                } else {
        			totalTax = totalTax.add(amount.multiply(new BigDecimal(paxCount)));
        		}
        	}
        }
//        PNRResponse pnrResponse = new PNRResponse();
//        List<FarePricePNRWithBookingClassReply.FareList.FareDataInformation.FareDataSupInformation> fareList = pricePNRReply.getFareList().get(0).getFareDataInformation().getFareDataSupInformation();
//        for (FarePricePNRWithBookingClassReply.FareList.FareDataInformation.FareDataSupInformation fareData : fareList){
//
//            if(totalFareIdentifier.equals(fareData.getFareDataQualifier())){
//                totalFare = new BigDecimal(fareData.getFareAmount());
//                break;
//            }
//
//        }
        BigDecimal searchPrice = new BigDecimal(0);
        if(travellerMasterInfo.isSeamen()) {
            searchPrice = travellerMasterInfo.getItinerary().getSeamanPricingInformation().getTotalPriceValue();
        } else {
            searchPrice = travellerMasterInfo.getItinerary().getPricingInformation().getTotalPriceValue();
        }


        System.out.println("Total price before comparing :"+ searchPrice +" changed Price : " + totalFare);
        if(totalFare.compareTo(searchPrice) == 0) {
            System.out.println("inside comparison 1111111111111111");
            pnrResponse.setPriceChanged(false);
            return;
        }
        pnrResponse.setChangedPrice(totalFare);
        pnrResponse.setChangedBasePrice(totalFare.subtract(totalTax));
        pnrResponse.setOriginalPrice(searchPrice);
        pnrResponse.setPriceChanged(true);
        pnrResponse.setFlightAvailable(true);
    }


    public static void createTickets(IssuanceResponse issuanceResponse, IssuanceRequest issuanceRequest, PNRReply gdsPNRReply){
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
                for(PNRReply.DataElementsMaster.DataElementsIndiv.ReferenceForDataElement.Reference reference :dataElementsDiv.getReferenceForDataElement().getReference()){
                    travellerKey = reference.getQualifier()+ reference.getNumber();
                    if(travellerMap.containsKey(travellerKey)){
                        passengerRef = travellerKey;
                    }else {
                        segmentRefList.add(travellerKey);
                    }
                }
                PNRReply.TravellerInfo traveller = (PNRReply.TravellerInfo)travellerMap.get(travellerKey);
                String lastName = traveller.getPassengerData().get(0).getTravellerInformation().getTraveller().getSurname();
                String name = traveller.getPassengerData().get(0).getTravellerInformation().getPassenger().get(0).getFirstName();
                String[] nameArray = name.split(" ");
                String firstName = nameArray[0];
                String middleName = (nameArray.length > 1)? nameArray[1]: "";

                for(Traveller traveller1 : issuanceRequest.getTravellerList()){
                    if(firstName.equalsIgnoreCase(traveller1.getPersonalDetails().getFirstName())
                            && lastName.equalsIgnoreCase(traveller1.getPersonalDetails().getLastName())){
                    	String freeText = dataElementsDiv.getOtherDataFreetext().get(0).getLongFreetext();
                        String[] freeTextArr = freeText.split("/");
                        String ticketNumber = freeTextArr[0].substring(3);
                        Map<String,String> ticketMap = new HashMap<>();
                        for(String segmentRef : segmentRefList){
                            PNRReply.OriginDestinationDetails.ItineraryInfo itineraryInfo = (PNRReply.OriginDestinationDetails.ItineraryInfo)airSegmentRefMap.get(segmentRef);
                            String key = itineraryInfo.getTravelProduct().getBoardpointDetail().getCityCode()+
                                    itineraryInfo.getTravelProduct().getOffpointDetail().getCityCode() + traveller1.getContactId();
                            ticketMap.put(key.toLowerCase(),ticketNumber);
                        }
                        traveller1.setTicketNumberMap(ticketMap);
                    }
                }
                issuanceResponse.setTravellerList(issuanceRequest.getTravellerList());
                issuanceResponse.setSuccess(true);
            }
        }
    }


    public static int getPassengerTypeCount(List<Traveller> travellerList){

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
