package utils;

import com.compassites.GDSWrapper.mystifly.Mystifly;
import com.compassites.model.AirSegmentInformation;
import com.compassites.model.Journey;
import com.compassites.model.PassengerTax;
import com.compassites.model.PricingInformation;
import models.Airline;
import models.Airport;
import org.datacontract.schemas._2004._07.mystifly_onepoint.*;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.data.redis.core.RedisTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by yaseen on 30-03-2016.
 */
public class MystiflyHelper {

    public static PricingInformation setPricingInformtions(
            AirItineraryPricingInfo airlinePricingInfo) {
        ItinTotalFare itinTotalFare = airlinePricingInfo.getItinTotalFare();
        PricingInformation pricingInfo = new PricingInformation();
        pricingInfo.setProvider(Mystifly.PROVIDER);
        pricingInfo
                .setLCC(airlinePricingInfo.getFareType() == FareType.WEB_FARE);
        pricingInfo.setCurrency(itinTotalFare.getBaseFare().getCurrencyCode());
        String baseFare = itinTotalFare.getEquivFare().getAmount();
        pricingInfo.setBasePrice(new BigDecimal(baseFare));
        String totalTax = itinTotalFare.getTotalTax().getAmount();
        pricingInfo.setTax(new BigDecimal(totalTax));
        String total = itinTotalFare.getTotalFare().getAmount();
        pricingInfo.setTotalPrice(new BigDecimal(total));
        pricingInfo.setGdsCurrency("INR");
        pricingInfo.setTotalPriceValue(pricingInfo.getTotalPrice());
        pricingInfo.setFareSourceCode(airlinePricingInfo.getFareSourceCode());
        setFareBeakup(pricingInfo, airlinePricingInfo);
        return pricingInfo;
    }

    public static void setFareBeakup(PricingInformation pricingInfo,
                               AirItineraryPricingInfo airlinePricingInfo) {
        List<PassengerTax> passengerTaxes = new ArrayList<>();
        for (PTCFareBreakdown ptcFareBreakdown : airlinePricingInfo
                .getPTCFareBreakdowns().getPTCFareBreakdownArray()) {

            PassengerTax passengerTax = new PassengerTax();
            PassengerFare paxFare = ptcFareBreakdown.getPassengerFare();
            // passengerTax.setBaseFare(new BigDecimal(paxFare.getBaseFare()
            // .getAmount()));
            PassengerTypeQuantity passenger = ptcFareBreakdown
                    .getPassengerTypeQuantity();
            passengerTax.setPassengerCount(passenger.getQuantity());
            passengerTax.setPassengerType(passenger.getCode().toString());

            Map<String, BigDecimal> taxes = new HashMap<>();
            for (Tax tax : ptcFareBreakdown.getPassengerFare().getTaxes()
                    .getTaxArray()) {
                if (taxes.containsKey(tax.getTaxCode())) {
                    taxes.put(tax.getTaxCode(), taxes.get(tax.getTaxCode())
                            .add(new BigDecimal(tax.getAmount())));
                } else {
                    taxes.put(tax.getTaxCode(), new BigDecimal(tax.getAmount()));
                }
            }
            passengerTax.setTaxes(taxes);
            passengerTaxes.add(passengerTax);
            String paxCode = ptcFareBreakdown.getPassengerTypeQuantity()
                    .getCode().toString();
            BigDecimal amount = new BigDecimal(paxFare.getEquivFare()
                    .getAmount());
            BigDecimal totalAmount = new BigDecimal(paxFare.getTotalFare().getAmount());
            if (paxCode.equalsIgnoreCase("ADT")) {
                pricingInfo.setAdtBasePrice(amount);
                pricingInfo.setAdtTotalPrice(totalAmount);
            } else if (paxCode.equalsIgnoreCase("CHD")) {
                pricingInfo.setChdBasePrice(amount);
                pricingInfo.setChdTotalPrice(totalAmount);
            } else if (paxCode.equalsIgnoreCase("INF")) {
                pricingInfo.setInfBasePrice(amount);
                pricingInfo.setInfTotalPrice(totalAmount);
            }
        }
        pricingInfo.setPassengerTaxes(passengerTaxes);
    }

    public static List<Journey> getJourneyListFromPNRResponse(AirTripDetailsRS tripDetailsRS , HashMap<String, String> baggageMap, HashMap<String, String> airlinePNRMap, RedisTemplate redisTemplate){
        List<Journey> journeyList = new ArrayList<>();
        List<AirSegmentInformation> airSegmentList = new ArrayList<>();
        Journey journey = new Journey();
        int segmentSeq = 1;
        ArrayOfReservationItem reservationItemArray = tripDetailsRS.getTravelItinerary().getItineraryInfo().getReservationItems();
        for(ReservationItem reservationItem : reservationItemArray.getReservationItemArray())   {
            AirSegmentInformation airSegmentInformation = new AirSegmentInformation();
            String fromLoc = reservationItem.getDepartureAirportLocationCode();
            String toLoc = reservationItem.getArrivalAirportLocationCode();
            airSegmentInformation.setFromLocation(fromLoc);
            airSegmentInformation.setToLocation(toLoc);
            Airport fromAirport = Airport
                    .getAirport(airSegmentInformation.getFromLocation(), redisTemplate);
            Airport toAirport = Airport.getAirport(airSegmentInformation
                    .getToLocation(), redisTemplate);
            airSegmentInformation.setFromAirport(fromAirport);
            airSegmentInformation.setToAirport(toAirport);
            airSegmentInformation.setFlightNumber(reservationItem.getFlightNumber());
            String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
            DateTimeFormatter DATETIME_FORMATTER = DateTimeFormat.forPattern(DATE_FORMAT);
            DateTimeZone dateTimeZone = DateTimeZone.forID(fromAirport.getTime_zone());
            DateTime departureDateTime = DATETIME_FORMATTER.withZone(dateTimeZone).parseDateTime(reservationItem.getDepartureDateTime().toString());
            dateTimeZone = DateTimeZone.forID(toAirport.getTime_zone());
            DateTime arrivalDate = DATETIME_FORMATTER.withZone(dateTimeZone).parseDateTime(reservationItem.getArrivalDateTime().toString());
            airSegmentInformation.setDepartureDate(reservationItem.getDepartureDateTime().getTime());
            airSegmentInformation.setDepartureTime(departureDateTime.toString());
            airSegmentInformation.setArrivalTime(arrivalDate.toString());
            airSegmentInformation.setArrivalDate(reservationItem.getArrivalDateTime().getTime());
            String depTerminal = reservationItem.getDepartureTerminal();
            String arrTerminal = reservationItem.getArrivalTerminal();
            depTerminal = depTerminal.replaceAll("\\D+","");
            arrTerminal = arrTerminal.replaceAll("\\D+","");
            airSegmentInformation.setCarrierCode(reservationItem.getOperatingAirlineCode());
            airSegmentInformation.setOperatingCarrierCode(reservationItem.getOperatingAirlineCode());
            airSegmentInformation.setValidatingCarrierCode(reservationItem.getMarketingAirlineCode());
            airSegmentInformation.setEquipment(reservationItem.getAirEquipmentType());
            airSegmentInformation.setTravelTime(reservationItem.getJourneyDuration());
            airSegmentInformation.setConnectionTimeStr();
            airSegmentInformation.setFromTerminal(depTerminal);
            airSegmentInformation.setToTerminal(arrTerminal);
            airSegmentInformation.setAirLinePnr(reservationItem.getAirlinePNR());
            airSegmentInformation.setBookingClass(reservationItem.getResBookDesigCode());
            airSegmentInformation.setAirline(Airline.getAirlineByCode(reservationItem.getOperatingAirlineCode(), redisTemplate));
            String key = airSegmentInformation.getFromLocation().concat(airSegmentInformation.getToLocation());
            String baggage = reservationItem.getBaggage();
            baggageMap.put(key, baggage);
            key = key.concat(String.valueOf(reservationItem.getItemRPH()));
            airlinePNRMap.put(key.toLowerCase() , reservationItem.getAirlinePNR());
            airSegmentList.add(airSegmentInformation);
        }
        journey.setAirSegmentList(airSegmentList);
        journeyList.add(journey);
        return journeyList;
    }

    /**
     * @param tripDetailsRS
     * @return pricingInformation
     */
    public static PricingInformation getPricingInfoFromPNRResponse(AirTripDetailsRS tripDetailsRS){
        ItineraryPricing itineraryPricing = tripDetailsRS.getTravelItinerary().getItineraryInfo().getItineraryPricing();
        TripDetailsPTCFareBreakdown[] tripDetailsPTCFareBreakdown = tripDetailsRS.getTravelItinerary().getItineraryInfo().getTripDetailsPTCFareBreakdowns().getTripDetailsPTCFareBreakdownArray();
        PricingInformation pricingInformation = new PricingInformation();
        pricingInformation.setProvider(Mystifly.PROVIDER);
       // TODO: pricingInformation.setLCC(airlinePricingInfo.getFareType() == FareType.WEB_FARE);
        pricingInformation.setCurrency(itineraryPricing.getEquiFare().getCurrencyCode());
        String baseFare = itineraryPricing.getEquiFare().getAmount();
        pricingInformation.setBasePrice(new BigDecimal(baseFare));
        String totalTax = itineraryPricing.getTax().getAmount();
        pricingInformation.setTax(new BigDecimal(totalTax));
        String totalPrice = itineraryPricing.getTotalFare().getAmount();
        pricingInformation.setTotalPrice(new BigDecimal(totalPrice));
        pricingInformation.setGdsCurrency("INR");
        pricingInformation.setTotalPriceValue(pricingInformation.getTotalPrice());
        //TODO: pricingInformation.setFareSourceCode(airlinePricingInfo.getFareSourceCode());
        setFareBeakupForPnrUpload(pricingInformation, tripDetailsPTCFareBreakdown);
        return pricingInformation;
    }

    /**
     *@param tripDetailsPTCFareBreakdown
     *
     */
    public static void setFareBeakupForPnrUpload(PricingInformation pricingInformation,
                                                 TripDetailsPTCFareBreakdown[] tripDetailsPTCFareBreakdown) {
        List<PassengerTax> passengerTaxes = new ArrayList<>();
        for (TripDetailsPTCFareBreakdown ptcFareBreakdown : tripDetailsPTCFareBreakdown) {
            PassengerTax passengerTax = new PassengerTax();
            TripDetailsPassengerFare paxFare = ptcFareBreakdown.getTripDetailsPassengerFare();
            PassengerTypeQuantity passengerType = ptcFareBreakdown
                    .getPassengerTypeQuantity();
            passengerTax.setPassengerCount(passengerType.getQuantity());
            passengerTax.setPassengerType(passengerType.getCode().toString());
            passengerTax.setTotalTax(new BigDecimal(ptcFareBreakdown.getTripDetailsPassengerFare().getTax().getAmount()));
            passengerTaxes.add(passengerTax);
            String paxCode = ptcFareBreakdown.getPassengerTypeQuantity()
                    .getCode().toString();
            BigDecimal amount = new BigDecimal(paxFare.getEquiFare().getAmount());
            BigDecimal totalAmount = new BigDecimal(paxFare.getTotalFare().getAmount());
            if (paxCode.equalsIgnoreCase("ADT")) {
                pricingInformation.setAdtBasePrice(amount);
                pricingInformation.setAdtTotalPrice(totalAmount);
            } else if (paxCode.equalsIgnoreCase("CHD")) {
                pricingInformation.setChdBasePrice(amount);
                pricingInformation.setChdTotalPrice(totalAmount);
            } else if (paxCode.equalsIgnoreCase("INF")) {
                pricingInformation.setInfBasePrice(amount);
                pricingInformation.setInfTotalPrice(totalAmount);
            }
        }
        pricingInformation.setPassengerTaxes(passengerTaxes);
    }



}
