package utils;

import com.amadeus.xml.pnracc_11_3_1a.PNRReply;
import com.amadeus.xml.tplprr_12_4_1a.BaggageDetailsTypeI;
import com.amadeus.xml.tplprr_12_4_1a.FarePricePNRWithLowestFareReply;
import com.amadeus.xml.tplprr_12_4_1a.FarePricePNRWithLowestFareReply.FareList;
import com.amadeus.xml.tplprr_12_4_1a.FarePricePNRWithLowestFareReply.FareList.TaxInformation;
import com.amadeus.xml.tplprr_12_4_1a.MonetaryInformationDetailsType223844C;
import com.amadeus.xml.ttstrr_13_1_1a.ReferencingDetailsTypeI;
import com.compassites.constants.AmadeusConstants;
import com.compassites.model.PassengerTax;
import com.compassites.model.PricingInformation;
import com.compassites.model.TSTLowestFare;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by yaseen on 24-09-2016.
 *
 * Had to replicate all the methods same as AmadeusBookingHelper because the Objects inside LowestFareResponse
 * are different
 */
public class LowestFareHelper {

    public static PricingInformation getPricingInfo(
            List<FareList> pricePNRReplyFareList,
            int adultCount, int childCount,
            int infantCount, PNRReply gdsPNRReply, Map<String,TSTLowestFare> tstLowestFareMap) {
        BigDecimal totalFare = new BigDecimal(0);
        BigDecimal totalBaseFare = new BigDecimal(0);
        BigDecimal adtBaseFare = new BigDecimal(0);
        BigDecimal chdBaseFare = new BigDecimal(0);
        BigDecimal infBaseFare = new BigDecimal(0);
        String currency = null;


        Map<String,Object> airSegmentRefMap = new HashMap<>();

        for(PNRReply.OriginDestinationDetails originDestination : gdsPNRReply.getOriginDestinationDetails()){
            for(PNRReply.OriginDestinationDetails.ItineraryInfo itineraryInfo : originDestination.getItineraryInfo()){
                String segmentRef = "S"+itineraryInfo.getElementManagementItinerary().getReference().getNumber();
                String segments = itineraryInfo.getTravelProduct().getBoardpointDetail().getCityCode() + itineraryInfo.getTravelProduct().getOffpointDetail().getCityCode();
                airSegmentRefMap.put(segmentRef,segments);
            }
        }
//        List<FareList> fareList = pricePNRReplyFareList;
        PricingInformation pricingInformation = new PricingInformation();
        for(FareList fare : pricePNRReplyFareList) {
            int paxCount = 0;
            BigDecimal paxTotalFare = BigDecimal.ZERO;
            String paxType = fare.getSegmentInformation().get(0).getFareQualifier().getFareBasisDetails().getDiscTktDesignator();
            if("chd".equalsIgnoreCase(paxType) || "ch".equalsIgnoreCase(paxType)) {
                paxCount = childCount;
            } else if("inf".equalsIgnoreCase(paxType) || "in".equalsIgnoreCase(paxType)) {
                paxCount = infantCount;
            } else if("adt".equalsIgnoreCase(paxType)){
                paxCount = adultCount;
            }else {
                paxCount = adultCount + childCount + infantCount; //for seamen fare
            }
//            currency = fare.getFareDataInformation().getFareDataSupInformation().get(0).getFareCurrency();
            boolean equivalentFareAvailable = false;
            BigDecimal baseFare = new BigDecimal(0);
            for(MonetaryInformationDetailsType223844C fareData : fare.getFareDataInformation().getFareDataSupInformation()) {
                BigDecimal amount = new BigDecimal(fareData.getFareAmount());
                if(AmadeusConstants.TOTAL_FARE_IDENTIFIER.equals(fareData.getFareDataQualifier())) {
                    paxTotalFare = amount;
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
            String passengerType = "";
            if("chd".equalsIgnoreCase(paxType) || "ch".equalsIgnoreCase(paxType)) {
                chdBaseFare = baseFare;
                passengerType = "CHD";
            } else if("inf".equalsIgnoreCase(paxType) || "in".equalsIgnoreCase(paxType)) {
                infBaseFare = baseFare;
                passengerType = "INF";
            } else {
                adtBaseFare = baseFare;
                passengerType = "ADT";
            }

            TSTLowestFare  tstLowestFare = getTSTFare(fare, paxTotalFare, paxType);
            for(FareList.SegmentInformation segmentInfo : fare.getSegmentInformation()) {
                if(segmentInfo.getSegmentReference() != null && segmentInfo.getSegmentReference().getRefDetails() != null){
                    com.amadeus.xml.tplprr_12_4_1a.ReferencingDetailsTypeI referencingDetailsTypeI = segmentInfo.getSegmentReference().getRefDetails().get(0);
                    String key = referencingDetailsTypeI.getRefQualifier() + referencingDetailsTypeI.getRefNumber();

                    String tstKey = airSegmentRefMap.get(key).toString() +passengerType;
                    tstLowestFareMap.put(tstKey.toLowerCase(), tstLowestFare);
                }
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
        pricingInformation.setPassengerTaxes(getTaxBreakup(pricePNRReplyFareList, passengerTypeMap));
        return pricingInformation;
    }

    private static TSTLowestFare getTSTFare(FareList fare, BigDecimal paxTotalFare, String paxType){
        TSTLowestFare tstLowestFare = new TSTLowestFare();
        for(FareList.SegmentInformation segmentInfo : fare.getSegmentInformation()) {
            BaggageDetailsTypeI bagAllowance = segmentInfo.getBagAllowanceInformation().getBagAllowanceDetails();
            if(bagAllowance.getBaggageType().equalsIgnoreCase("w")) {
                if(tstLowestFare.getMaxBaggageWeight() == 0 || tstLowestFare.getMaxBaggageWeight() > bagAllowance.getBaggageWeight().intValue()) {
                    tstLowestFare.setMaxBaggageWeight(bagAllowance.getBaggageWeight().intValue());
                }
            } else {
                if(tstLowestFare.getBaggageCount() == 0 || tstLowestFare.getBaggageCount() > bagAllowance.getBaggageQuantity().intValue()) {
                    tstLowestFare.setBaggageCount(bagAllowance.getBaggageQuantity().intValue());
                }
            }
            //reading booking class(RBD)
            String bookingClass = segmentInfo.getSegDetails().getSegmentDetail().getClassOfService();
            tstLowestFare.setBookingClass(bookingClass);
        }

        tstLowestFare.setAmount(paxTotalFare);
        tstLowestFare.setPassengerType(paxType);

        return tstLowestFare;
    }

    public static List<PassengerTax> getTaxBreakup(List<FareList> pricePNRReplyFareList, Map<String, Integer> passengerTypeMap){
        List<PassengerTax> passengerTaxes = new ArrayList<>();
        for (FareList fare : pricePNRReplyFareList) {
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
}
