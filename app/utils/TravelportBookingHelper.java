package utils;

import com.compassites.constants.TravelportConstants;
import com.compassites.model.PassengerTax;
import com.compassites.model.PricingInformation;
import com.compassites.model.SegmentPricing;
import com.compassites.model.traveller.Traveller;
import com.travelport.schema.air_v26_0.AirPricingInfo;
import com.travelport.schema.air_v26_0.BookingInfo;
import com.travelport.schema.air_v26_0.PassengerType;
import com.travelport.schema.air_v26_0.TypeBaseAirSegment;
import com.travelport.schema.common_v26_0.BookingTraveler;
import com.travelport.schema.common_v26_0.BookingTravelerName;
import com.travelport.schema.universal_v26_0.UniversalRecord;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by yaseen on 25-02-2016.
 */
public class TravelportBookingHelper {

    public static PricingInformation getPriceFromPNRResponse(UniversalRecord universalRecord, Map<String,String> airSegmentRefMap){

        PricingInformation pricingInfo = new PricingInformation();
        List<AirPricingInfo> airPricingInfoList = universalRecord.getAirReservation().get(0).getAirPricingInfo();

        BigDecimal totalFare = new BigDecimal(0);
        BigDecimal totalBaseFare = new BigDecimal(0);
        BigDecimal totalTax = new BigDecimal(0);

        BigDecimal adtBaseFare = new BigDecimal(0);
        BigDecimal chdBaseFare = new BigDecimal(0);
        BigDecimal infBaseFare = new BigDecimal(0);
        BigDecimal adtTotalFare = new BigDecimal(0);
        BigDecimal chdTotalFare = new BigDecimal(0);
        BigDecimal infTotalFare = new BigDecimal(0);
        boolean segmentWisePricing = false;

        List<SegmentPricing> segmentPricingList = new ArrayList<>();
        List<PassengerTax> passengerTaxList = new ArrayList<>();

        for(AirPricingInfo airPricingInfo : airPricingInfoList) {
            BigDecimal total = StringUtility.getDecimalFromString(airPricingInfo.getApproximateTotalPrice());
            BigDecimal base = StringUtility.getDecimalFromString(airPricingInfo.getApproximateBasePrice());
            BigDecimal tax = StringUtility.getDecimalFromString(airPricingInfo.getTaxes());
            pricingInfo.setGdsCurrency(StringUtility.getCurrencyFromString(airPricingInfo.getTotalPrice()));
            List<PassengerType> passegerTypes = airPricingInfo.getPassengerType();
            String paxType = passegerTypes.get(0).getCode();

            int paxCount = passegerTypes.size();
            totalFare = totalFare.add(total.multiply(new BigDecimal(paxCount)));
            totalBaseFare = totalBaseFare.add(base.multiply(new BigDecimal(paxCount)));
            totalTax = totalTax.add(tax.multiply(new BigDecimal(paxCount)));

            if("CHD".equalsIgnoreCase(paxType) || "CNN".equalsIgnoreCase(paxType)) {
//				pricingInfo.setChdBasePrice(base);
                chdBaseFare = chdBaseFare.add(base);
                chdTotalFare = chdTotalFare.add(total);
            } else if("INF".equalsIgnoreCase(paxType)) {
//				pricingInfo.setInfBasePrice(base);
                infBaseFare = infBaseFare.add(base);
                infTotalFare = infTotalFare.add(total);
            } else {
//				pricingInfo.setAdtBasePrice(base);
                adtBaseFare = adtBaseFare.add(base);
                adtTotalFare = adtTotalFare.add(total);
            }

            if(airSegmentRefMap.size() != airPricingInfo.getBookingInfo().size()){
                segmentWisePricing = true;
            }

            List<String> segmentKeys = new ArrayList<>();
            SegmentPricing segmentPricing = new SegmentPricing();
            if(segmentWisePricing){
                for(BookingInfo bookingInfo : airPricingInfo.getBookingInfo()){
                    String key = airSegmentRefMap.get(bookingInfo.getSegmentRef());
                    segmentKeys.add(key);
                }
                PassengerTax passengerTax = TravelportHelper.getTaxDetailsList(airPricingInfo.getTaxInfo(), paxType, paxCount);
                segmentPricing.setSegmentKeysList(segmentKeys);
                segmentPricing.setTotalPrice(total.multiply(new BigDecimal(paxCount)));
                segmentPricing.setBasePrice(base.multiply(new BigDecimal(paxCount)));
                segmentPricing.setTax(total.subtract(base).multiply(new BigDecimal(paxCount)));
                segmentPricing.setPassengerType(paxType);
                segmentPricing.setPassengerTax(passengerTax);
                segmentPricing.setPassengerCount(new Long(paxCount));
                segmentPricingList.add(segmentPricing);
            }

        }

        pricingInfo.setTax(totalTax);
        pricingInfo.setBasePrice(totalBaseFare);
        pricingInfo.setTotalPrice(totalFare);
        pricingInfo.setTotalPriceValue(totalFare);
        pricingInfo.setProvider("Travelport");

        pricingInfo.setSegmentWisePricing(segmentWisePricing);
        pricingInfo.setAdtBasePrice(adtBaseFare);
        pricingInfo.setAdtTotalPrice(adtTotalFare);
        pricingInfo.setChdBasePrice(chdBaseFare);
        pricingInfo.setChdTotalPrice(chdTotalFare);
        pricingInfo.setInfBasePrice(infBaseFare);
        pricingInfo.setInfTotalPrice(infTotalFare);
        pricingInfo.setSegmentPricingList(segmentPricingList);

        TravelportHelper.getPassengerTaxes(pricingInfo, airPricingInfoList);
        return pricingInfo;
    }

    public static Map<String,List<BookingTraveler>> getTravellerTypeMap(List<Traveller> travellerList, List<BookingTraveler> bookingTravelerList, boolean isSeamen){

        Map<String,List<BookingTraveler>> bookingTravellerTypeMap = new HashMap<>();

        Map<String, String> travelerTypeNameMap = new HashMap<>();
        for (Traveller traveller : travellerList) {
            String contactFirstName = traveller.getPersonalDetails().getFirstName().replaceAll("\\s+", "");;
            String contactLastName = traveller.getPersonalDetails().getLastName().replaceAll("\\s+", "");;
            String fullName = contactFirstName + contactLastName;
            fullName = fullName.toLowerCase();
            String paxType = isSeamen ? "SEA": DateUtility.getPassengerTypeFromDOB(traveller.getPassportDetails().getDateOfBirth()).name();

            travelerTypeNameMap.put(fullName, paxType);
        }

        for (BookingTraveler bookingTraveler : bookingTravelerList) {
            BookingTravelerName bookingTravelerName = bookingTraveler.getBookingTravelerName();
            String firstName = bookingTravelerName.getFirst().replaceAll("\\s+", "");
            String lastName = bookingTravelerName.getLast().replaceAll("\\s+", "");
            String fullName = firstName + lastName;
            fullName = fullName.toLowerCase();
            String paxType = travelerTypeNameMap.get(fullName);
            List<BookingTraveler> travelerList = bookingTravellerTypeMap.get(paxType);
            if(travelerList == null){
                travelerList = new ArrayList<>();
            }
            travelerList.add(bookingTraveler);
            bookingTravellerTypeMap.put(paxType, travelerList);
        }
        return bookingTravellerTypeMap;
    }


    public static boolean checkSegmentStatus(List<TypeBaseAirSegment> typeBaseAirSegments){

        for(TypeBaseAirSegment airSegment : typeBaseAirSegments){
            String status = airSegment.getStatus();
            if(TravelportConstants.UNCONFIRMED_SEGMENT.equalsIgnoreCase(status)){
                return false;
            }
        }
        return false;
    }
}
