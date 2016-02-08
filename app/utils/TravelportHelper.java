package utils;

import com.compassites.model.*;
import com.compassites.model.Journey;
import com.travelport.schema.air_v26_0.*;
import com.travelport.schema.universal_v26_0.UniversalRecordRetrieveRsp;
import models.Airline;
import models.Airport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Yaseen on 14-04-2015.
 */
public class TravelportHelper {

    static Logger logger = LoggerFactory.getLogger("gds");

    public static List<Journey> getJourneyListFromPNR(UniversalRecordRetrieveRsp universalRecordRetrieveRsp){
        List<Journey> journeyList = new ArrayList<>();
        List<AirSegmentInformation> airSegmentList = new ArrayList<>();
        Journey journey = new Journey();
        for (AirReservation airReservation : universalRecordRetrieveRsp.getUniversalRecord().getAirReservation()) {


            for (TypeBaseAirSegment airSegment : airReservation.getAirSegment()) {
                AirSegmentInformation airSegmentInformation = new AirSegmentInformation();
                String carrierCode = airSegment.getCarrier();
                airSegmentInformation.setCarrierCode(carrierCode);
                airSegmentInformation.setFlightNumber(airSegment.getFlightNumber());
                SimpleDateFormat format= new SimpleDateFormat("yyyy-MM-dd");
					/*Date arrvalDate = format.parse(airSegment.getArrivalTime());
					airSegmentInformation.setArrivalDate(arrvalDate);

					Date depDate = format.parse(airSegment.getDepartureTime());
					airSegmentInformation.setDepartureDate(depDate);*/
                String arrivalDateTime = airSegment.getArrivalTime();
                String departureDateTime = airSegment.getDepartureTime();

                String fromLoc = airSegment.getOrigin();
                String toLoc = airSegment.getDestination();
                airSegmentInformation.setFromLocation(fromLoc);
                airSegmentInformation.setToLocation(airSegment.getDestination());
                if(airSegment.getTravelTime() != null){
                    airSegmentInformation.setTravelTime(airSegment.getTravelTime().toString());
                }
                if(airSegment.getDistance() != null){
                    airSegmentInformation.setDistanceTravelled(airSegment.getDistance().toString());
                }

                airSegmentInformation.setDepartureTime(departureDateTime);
                airSegmentInformation.setArrivalTime(arrivalDateTime);

                airSegmentInformation.setFromDate(arrivalDateTime);
                airSegmentInformation.setToDate(departureDateTime);

                try {
                    airSegmentInformation.setDepartureDate(format.parse(departureDateTime));
                    airSegmentInformation.setArrivalDate(format.parse(arrivalDateTime));
                } catch (ParseException e) {
                    logger.error("TravelportHelper getJourneyListFromPNR" ,e);
                    e.printStackTrace();
                }

                airSegmentInformation.setAirline(Airline.getAirlineByCode(carrierCode));

                airSegmentInformation.setFromAirport(Airport.getAiport(fromLoc));
                airSegmentInformation.setToAirport(Airport.getAiport(toLoc));
                airSegmentInformation.setBookingClass(airSegment.getCabinClass().toString());

                for (FlightDetails flightDetails : airSegment.getFlightDetails()) {
                    if (flightDetails.getOriginTerminal() != null) {
                        airSegmentInformation.setFromTerminal(flightDetails.getOriginTerminal());
                    }
                    if (flightDetails.getDestinationTerminal() != null) {
                        airSegmentInformation.setToTerminal(flightDetails.getDestinationTerminal());
                    }
                    airSegmentInformation.setEquipment(flightDetails.getEquipment());
                }
                //airSegmentList.add(airSegmentInformation);
                for (AirPricingInfo airPricingInfo : airReservation.getAirPricingInfo()) {
                    com.compassites.model.FlightInfo flightInfo = new com.compassites.model.FlightInfo();
                    for (FareInfo fareInfo : airPricingInfo.getFareInfo()) {
                        if(fareInfo.getBaggageAllowance().getMaxWeight() != null && fareInfo.getBaggageAllowance().getMaxWeight().getUnit() != null){
                            flightInfo.setBaggageUnit(fareInfo.getBaggageAllowance().getMaxWeight().getUnit().toString());
                            flightInfo.setBaggageAllowance(fareInfo.getBaggageAllowance().getMaxWeight().getValue());
                        }
                        if(fareInfo.getBaggageAllowance().getNumberOfPieces() != null){
                            flightInfo.setBaggageAllowance(fareInfo.getBaggageAllowance().getNumberOfPieces());
                            flightInfo.setBaggageUnit("Number of pieces");
                        }

                    }
                    airSegmentInformation.setFlightInfo(flightInfo);
                }
                airSegmentList.add(airSegmentInformation);
            }

            journey.setAirSegmentList(airSegmentList);
            journeyList.add(journey);
        }

        return journeyList;
    }

    public static void getPassengerTaxes(PricingInformation pricingInfo, List<AirPricingInfo> airPricingInfoList) {
        List<PassengerTax> passengerTaxes = new ArrayList<>();
        
        AirPricingInfo airPriceInfo = airPricingInfoList.get(0);
        if("SEA".equalsIgnoreCase(airPriceInfo.getPassengerType().get(0).getCode())) {
//        	pricingInfo.setAdtBasePrice(StringUtility.getDecimalFromString(airPriceInfo.getBasePrice()));
        	PassengerTax passengerTax = new PassengerTax();
        	passengerTax.setPassengerType("ADT");
        	Map<String, BigDecimal> taxes = new HashMap<>();
        	for (TypeTaxInfo taxInfo : airPriceInfo.getTaxInfo()) {
                BigDecimal amount = StringUtility.getDecimalFromString(taxInfo.getAmount());
                if(taxes.containsKey(taxInfo.getCategory())) {
                    taxes.put(taxInfo.getCategory(), taxes.get(taxInfo.getCategory()).add(amount));
                } else {
                    taxes.put(taxInfo.getCategory(), amount);
                }
            }
        	int count = 0;
        	for (AirPricingInfo airPricingInfo : airPricingInfoList) {
        		count+= airPricingInfo.getPassengerType().size();
        	}
        	passengerTax.setPassengerCount(count);
            passengerTax.setTaxes(taxes);
            passengerTaxes.add(passengerTax);
        } else {
        	for (AirPricingInfo airPricingInfo : airPricingInfoList) {
                PassengerTax passengerTax = new PassengerTax();
                passengerTax.setPassengerCount(airPricingInfo.getPassengerType().size());
                PassengerType paxType = airPricingInfo.getPassengerType().get(0);
                String paxCode = null;
                if(paxType.getCode().equalsIgnoreCase("ADT")) {
//                    pricingInfo.setAdtBasePrice(StringUtility.getDecimalFromString(airPricingInfo.getBasePrice()));
                    paxCode = "ADT";
                } else if(paxType.getCode().equalsIgnoreCase("CHD") || paxType.getCode().equalsIgnoreCase("CNN")) {
//                    pricingInfo.setChdBasePrice(StringUtility.getDecimalFromString(airPricingInfo.getBasePrice()));
                    paxCode = "CHD";
                } else if(paxType.getCode().equalsIgnoreCase("INF")) {
//                    pricingInfo.setInfBasePrice(StringUtility.getDecimalFromString(airPricingInfo.getBasePrice()));
                    paxCode = "INF";
                }
                passengerTax.setPassengerType(paxCode);
                Map<String, BigDecimal> taxes = new HashMap<>();
                for (TypeTaxInfo taxInfo : airPricingInfo.getTaxInfo()) {
                    BigDecimal amount = StringUtility.getDecimalFromString(taxInfo.getAmount());
                    if(taxes.containsKey(taxInfo.getCategory())) {
                        taxes.put(taxInfo.getCategory(), taxes.get(taxInfo.getCategory()).add(amount));
                    } else {
                        taxes.put(taxInfo.getCategory(), amount);
                    }
                }
                passengerTax.setTaxes(taxes);
                passengerTaxes.add(passengerTax);
            }
        }
        pricingInfo.setPassengerTaxes(passengerTaxes);
    }



    public static PassengerTax getTaxDetailsList(List<TypeTaxInfo> typeTaxInfoList, String paxType, int passengerCount){
        PassengerTax passengerTax = new PassengerTax();
        Map<String,BigDecimal> taxes = new HashMap<>();
        for(TypeTaxInfo taxInfo : typeTaxInfoList){
            BigDecimal amount = StringUtility.getDecimalFromString(taxInfo.getAmount());
            if(taxes.containsKey(taxInfo.getCategory())) {
                taxes.put(taxInfo.getCategory(), taxes.get(taxInfo.getCategory()).add(amount));
            } else {
                taxes.put(taxInfo.getCategory(), amount);
            }
        }
        passengerTax.setPassengerCount(passengerCount);
        passengerTax.setPassengerType(paxType);
        passengerTax.setTaxes(taxes);
        return passengerTax;
    }

    public static PricingInformation getPriceDetails(PricingInformation pricingInfo, List<AirPricingInfo> airPricingInfoList){
        BigDecimal totalFare = new BigDecimal(0);
        BigDecimal totalBaseFare = new BigDecimal(0);
        BigDecimal totalTax = new BigDecimal(0);

        BigDecimal adtBaseFare = new BigDecimal(0);
        BigDecimal chdBaseFare = new BigDecimal(0);
        BigDecimal infBaseFare = new BigDecimal(0);
        BigDecimal adtTotalFare = new BigDecimal(0);
        BigDecimal chdTotalFare = new BigDecimal(0);
        BigDecimal infTotalFare = new BigDecimal(0);


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
        }

        pricingInfo.setTax(totalTax);
        pricingInfo.setBasePrice(totalBaseFare);
        pricingInfo.setTotalPrice(totalFare);
        pricingInfo.setTotalPriceValue(totalFare);
        pricingInfo.setProvider("Travelport");

        pricingInfo.setAdtBasePrice(adtBaseFare);
        pricingInfo.setAdtTotalPrice(adtTotalFare);
        pricingInfo.setChdBasePrice(chdBaseFare);
        pricingInfo.setChdTotalPrice(chdTotalFare);
        pricingInfo.setInfBasePrice(infBaseFare);
        pricingInfo.setInfTotalPrice(infTotalFare);

        return pricingInfo;
    }
}
