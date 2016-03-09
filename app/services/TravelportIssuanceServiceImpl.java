package services;

import com.compassites.GDSWrapper.travelport.AirRequestClient;
import com.compassites.GDSWrapper.travelport.AirTicketClient;
import com.compassites.GDSWrapper.travelport.UniversalRecordClient;
import com.compassites.GDSWrapper.travelport.UniversalRecordModifyClient;
import com.compassites.constants.StaticConstatnts;
import com.compassites.constants.TravelportConstants;
import com.compassites.exceptions.BaseCompassitesException;
import com.compassites.model.*;
import com.compassites.model.traveller.Traveller;
import com.travelport.schema.air_v26_0.*;
import com.travelport.schema.common_v26_0.BookingTraveler;
import com.travelport.schema.common_v26_0.BookingTravelerName;
import com.travelport.schema.universal_v26_0.UniversalRecord;
import com.travelport.schema.universal_v26_0.UniversalRecordModifyRsp;
import com.travelport.service.air_v26_0.AirFaultMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import utils.TravelportBookingHelper;
import utils.TravelportHelper;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by yaseen on 26-01-2016.
 */
@Service
public class TravelportIssuanceServiceImpl {

    static Logger logger = LoggerFactory.getLogger("gds");

    @Autowired
    private RedisTemplate redisTemplate;


    public IssuanceResponse priceBookedPNR(IssuanceRequest issuanceRequest) {
        logger.debug("======================= travelport  priceBookedPNR called =========================");

        IssuanceResponse issuanceResponse = new IssuanceResponse();
        UniversalRecord uniRcd = UniversalRecordClient.retrievePNR(issuanceRequest.getGdsPNR()).getUniversalRecord();
        boolean allSegmentsAreValid = checkSegmentValidity(uniRcd);
        if(!allSegmentsAreValid){
            issuanceResponse.setSuccess(false);
            return issuanceResponse;
        }
        String universalRecordLocatorCode = uniRcd.getLocatorCode();
        AirReservation airReservation = uniRcd.getAirReservation().get(0);
        BigInteger version = uniRcd.getVersion();
        String reservationLocatorCode = airReservation.getLocatorCode();

        try {
            if(airReservation.getAirPricingInfo() != null && airReservation.getAirPricingInfo().size() > 0){
                List<AirPricingInfo> airPricingInfoList = airReservation.getAirPricingInfo();
                    for(AirPricingInfo airPricingInfo : airPricingInfoList){
                        UniversalRecordModifyRsp universalRecordModifyRsp = UniversalRecordModifyClient.cancelPricing(issuanceRequest.getGdsPNR(),
                                reservationLocatorCode, universalRecordLocatorCode, version, airPricingInfo);
                        version = version.add(BigInteger.valueOf(1));
                    }

            }

            Map<String,String> airSegmentRefMap = new HashMap<>();
            for (AirReservation airReservation1 : uniRcd.getAirReservation()) {
                for (TypeBaseAirSegment airSegment : airReservation1.getAirSegment()) {
                    airSegmentRefMap.put(airSegment.getKey(), (airSegment.getOrigin() + airSegment.getDestination()).toLowerCase());
                }
            }
            PricingInformation pricingInformation = null;

            priceAndModifyPNR(issuanceRequest, reservationLocatorCode, universalRecordLocatorCode, airReservation, version, uniRcd.getBookingTraveler());

            uniRcd = UniversalRecordClient.retrievePNR(issuanceRequest.getGdsPNR()).getUniversalRecord();
            pricingInformation = TravelportBookingHelper.getPriceFromPNRResponse(uniRcd, airSegmentRefMap);

            BigDecimal bookedPrice = issuanceRequest.getFlightItinerary().getPricingInformation(issuanceRequest.isSeamen()).getTotalPriceValue();
            BigDecimal newPrice = pricingInformation.getTotalPriceValue();
            if(bookedPrice.compareTo(newPrice) != 0){
                issuanceResponse.setIsPriceChanged(true);
                issuanceResponse.setFlightItinerary(issuanceRequest.getFlightItinerary());
                issuanceResponse.getFlightItinerary().setPricingInformation(issuanceRequest.isSeamen(), pricingInformation);
            }else {
                issuanceResponse.setIsPriceChanged(false);
                issuanceResponse.setFlightItinerary(issuanceRequest.getFlightItinerary());
                issuanceResponse.getFlightItinerary().setPricingInformation(issuanceRequest.isSeamen(), pricingInformation);
            }
            issuanceResponse.setSuccess(true);

        } catch (AirFaultMessage airFaultMessage) {
            logger.error("Exception in priceBookedPNR", airFaultMessage);
            airFaultMessage.printStackTrace();
        }catch (BaseCompassitesException e) {
            e.printStackTrace();
            logger.error("Exception in priceBookedPNR", e);
        }
        return issuanceResponse;
    }


    public void priceAndModifyPNR(IssuanceRequest issuanceRequest, String reservationLocatorCode, String universalRecordLocatorCode,
                                  AirReservation airReservation,BigInteger version, List<BookingTraveler> bookingTravelerList) throws AirFaultMessage, BaseCompassitesException {
        List<SegmentPricing> segmentPricingList = issuanceRequest.getFlightItinerary().getPricingInformation(issuanceRequest.isSeamen()).getSegmentPricingList();

        Map<String,TypeBaseAirSegment> bookedSegments = new HashMap<>();
//        for(com.compassites.model.Journey journey : issuanceRequest.getFlightItinerary().getJourneys(issuanceRequest.isSeamen())){
            for(TypeBaseAirSegment airSegmentInformation : airReservation.getAirSegment()){
                String key = airSegmentInformation.getOrigin() + airSegmentInformation.getDestination();
                airSegmentInformation.setProviderReservationInfoRef(null);
                bookedSegments.put(key, airSegmentInformation);
            }
//        }

        AirPriceRsp priceRsp = null;
        if(segmentPricingList.size() > 0 ){
            System.out.println("Multi pricing code will be written here");
//            List<AirPricingInfo> airPricingInfoList = new ArrayList<>();
            for(SegmentPricing segmentPricing : segmentPricingList){
                List<String> segmentKeysList = segmentPricing.getSegmentKeysList();
                List<TypeBaseAirSegment> pricedSegments = new ArrayList<>();
                for(String segmentKey : segmentKeysList)
                {
                    pricedSegments.add(bookedSegments.get(segmentKey));
                }
                List<Traveller> segmentTravellers = TravelportHelper.filterTravellerListByType(issuanceRequest.getTravellerList(), segmentPricing.getPassengerType(), issuanceRequest.isSeamen());
//                AirItinerary airItinerary = AirRequestClient.buildAirItinerary(pricedSegments);
                AirItinerary airItinerary = new AirItinerary();
                airItinerary.getAirSegment().addAll(pricedSegments);
                priceRsp = AirRequestClient.priceItinerary(airItinerary, StaticConstatnts.DEFAULT_CURRENCY, null, segmentTravellers,
                        issuanceRequest.isSeamen());

//                airPricingInfoList.addAll(priceRsp.getAirPriceResult().get(0).getAirPricingSolution().get(0).getAirPricingInfo());
                Map<String,List<BookingTraveler>> bookingTravellerMap = TravelportBookingHelper.getTravellerTypeMap(segmentTravellers,bookingTravelerList, issuanceRequest.isSeamen());
                UniversalRecordModifyClient.addPricingToPNR(issuanceRequest.getGdsPNR(),reservationLocatorCode,universalRecordLocatorCode,
                        AirRequestClient.getPriceSolutionForRePrice(priceRsp, airReservation, bookingTravellerMap), version);
                version = version.add(BigInteger.valueOf(1));
            }
        }else {
//            AirItinerary airItinerary = AirRequestClient.buildAirItinerary(issuanceRequest.getFlightItinerary(), issuanceRequest.isSeamen());
            AirItinerary airItinerary = new AirItinerary();
            airItinerary.getAirSegment().addAll(bookedSegments.values());

            priceRsp = AirRequestClient.priceItinerary(airItinerary, StaticConstatnts.DEFAULT_CURRENCY, null, issuanceRequest.getTravellerList(), issuanceRequest.isSeamen());
            Map<String,List<BookingTraveler>> bookingTravellerMap = TravelportBookingHelper.getTravellerTypeMap(issuanceRequest.getTravellerList(), bookingTravelerList, issuanceRequest.isSeamen());
            UniversalRecordModifyClient.addPricingToPNR(issuanceRequest.getGdsPNR(),reservationLocatorCode,universalRecordLocatorCode,
                    AirRequestClient.getPriceSolutionForRePrice(priceRsp, airReservation, bookingTravellerMap), version);
            version = version.add(BigInteger.valueOf(1));

        }
    }

    public IssuanceResponse issueTicket(IssuanceRequest issuanceRequest) {

        IssuanceResponse issuanceResponse = new IssuanceResponse();
        AirTicketClient airTicketClient = new AirTicketClient();
        AirTicketingRsp airTicketingRsp = airTicketClient
                .issueTicket(issuanceRequest.getGdsPNR());
        if (airTicketingRsp.getETR().size() > 0) {
            issuanceResponse.setSuccess(true);
            issuanceResponse.setPnrNumber(issuanceRequest.getGdsPNR());
            List<Traveller> travellerList = issuanceRequest.getTravellerList();
            for (Traveller traveller : travellerList) {
                String contactFirstName = traveller.getPersonalDetails().getFirstName().replaceAll("\\s+", "");;
                String contactLastName = traveller.getPersonalDetails().getLastName().replaceAll("\\s+", "");;

                List<Ticket> tickets = null;
                for (ETR etr : airTicketingRsp.getETR()) {
                    BookingTravelerName name = etr.getBookingTraveler()
                            .getBookingTravelerName();
                    String firstName = name.getFirst().replaceAll("\\s+", "");
                    String lastName = name.getLast().replaceAll("\\s+", "");
                    if (contactFirstName.equalsIgnoreCase(firstName)
                            && contactLastName.equalsIgnoreCase(lastName)) {
                        tickets = etr.getTicket();
                        break;
                    }
                }
                Map<String, String> ticketMap = new HashMap<>();
                for (Ticket ticket : tickets) {
                    for (Coupon coupon : ticket.getCoupon()) {
                        String key = coupon.getOrigin()
                                + coupon.getDestination()
                                + traveller.getContactId();
                        ticketMap.put(key.toLowerCase(),
                                ticket.getTicketNumber());
                    }
                }
                traveller.setTicketNumberMap(ticketMap);
            }
            issuanceResponse.setTravellerList(travellerList);
        } else {
            issuanceResponse.setSuccess(false);
        }

//        getCancellationFee(issuanceRequest);
        return issuanceResponse;
    }

    private boolean checkSegmentValidity(UniversalRecord uniRcd){

        for(AirReservation airReservation : uniRcd.getAirReservation()){
           for(TypeBaseAirSegment typeBaseAirSegment : airReservation.getAirSegment()){
               if(TravelportConstants.SEGMENT_CANCELLED.equalsIgnoreCase(typeBaseAirSegment.getStatus())){
                   return false;
               }
           }
        }
        return true;
    }

}


