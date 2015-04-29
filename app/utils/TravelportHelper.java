package utils;

import com.compassites.model.AirSegmentInformation;
import com.compassites.model.Journey;
import com.compassites.model.PassengerTax;
import com.compassites.model.PricingInformation;
import com.travelport.schema.air_v26_0.*;
import com.travelport.schema.universal_v26_0.UniversalRecordRetrieveRsp;
import models.Airline;
import models.Airport;

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

                airSegmentInformation.setFromDate(arrivalDateTime);
                airSegmentInformation.setToDate(departureDateTime);

                try {
                    airSegmentInformation.setDepartureDate(format.parse(departureDateTime));
                    airSegmentInformation.setArrivalDate(format.parse(arrivalDateTime));
                } catch (ParseException e) {
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
                        flightInfo.setBaggageUnit(fareInfo.getBaggageAllowance().getMaxWeight().getUnit().toString());
                        flightInfo.setBaggageAllowance(fareInfo.getBaggageAllowance().getMaxWeight().getValue());
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
        for (AirPricingInfo airPricingInfo : airPricingInfoList) {
            PassengerTax passengerTax = new PassengerTax();
//			passengerTax.setBaseFare(StringUtility.getDecimalFromString(airPricingInfo.getBasePrice()));
            passengerTax.setPassengerCount(airPricingInfo.getPassengerType().size());
            PassengerType paxType = airPricingInfo.getPassengerType().get(0);
            if(paxType.getCode().equalsIgnoreCase("ADT") || paxType.getCode().equalsIgnoreCase("SEA")) {
                pricingInfo.setAdtBasePrice(StringUtility.getDecimalFromString(airPricingInfo.getBasePrice()));
            } else if(paxType.getCode().equalsIgnoreCase("CHD") || paxType.getCode().equalsIgnoreCase("CNN")) {
                pricingInfo.setChdBasePrice(StringUtility.getDecimalFromString(airPricingInfo.getBasePrice()));
            } else if(paxType.getCode().equalsIgnoreCase("INF")) {
                pricingInfo.setInfBasePrice(StringUtility.getDecimalFromString(airPricingInfo.getBasePrice()));
            }
            passengerTax.setPassengerType(paxType.getCode());
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
        pricingInfo.setPassengerTaxes(passengerTaxes);
    }


}
