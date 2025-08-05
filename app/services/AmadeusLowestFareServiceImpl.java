package services;

import com.amadeus.xml.pnracc_14_1_1a.PNRReply;
import com.amadeus.xml.tplprr_12_4_1a.FarePricePNRWithLowestFareReply;
import com.compassites.GDSWrapper.amadeus.ServiceHandler;
import com.compassites.model.*;
import models.AmadeusSessionWrapper;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import play.libs.Json;
import utils.AmadeusBookingHelper;
import utils.AmadeusHelper;
import utils.LowestFareHelper;
import utils.XMLFileUtility;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.amadeus.xml.tplprr_12_4_1a.FarePricePNRWithLowestFareReply.FareList;

/**
 * Created by yaseen on 24-09-2016.
 */
@Service
public class AmadeusLowestFareServiceImpl implements LowestFareService{

    static org.slf4j.Logger logger = LoggerFactory.getLogger("gds");


    public LowFareResponse getLowestFare(IssuanceRequest issuanceRequest) {
        LowFareResponse lowestFare = new LowFareResponse();
        ServiceHandler serviceHandler = null;
        AmadeusSessionWrapper amadeusSessionWrapper = null;
        boolean isSeamen = issuanceRequest.isSeamen();
        try {

            serviceHandler = new ServiceHandler();
            amadeusSessionWrapper = serviceHandler.logIn(true);
            PNRReply gdsPNRReply = serviceHandler.retrievePNR(issuanceRequest.getGdsPNR(), amadeusSessionWrapper);


            String carrierCode = "";

            carrierCode = issuanceRequest.getFlightItinerary().getJourneys(isSeamen).get(0).getAirSegmentList().get(0).getCarrierCode();
            boolean isDomestic = AmadeusHelper.checkAirportCountry("India", issuanceRequest.getFlightItinerary().getJourneys(isSeamen));
            FarePricePNRWithLowestFareReply pricePNRReply = new FarePricePNRWithLowestFareReply();

            List<AirSegmentInformation> airSegmentList = new ArrayList<>();
            for(Journey journey : issuanceRequest.getFlightItinerary().getJourneys(isSeamen)){
                airSegmentList.addAll(journey.getAirSegmentList());
                /*for(AirSegmentInformation airSegmentInformation : journey.getAirSegmentList()){
                    airSegmentList.add(airSegmentInformation);
                }*/
            }

            List<FarePricePNRWithLowestFareReply.FareList> pricePNRReplyFareList = new ArrayList<>();
            boolean isSegmentWisePricing = issuanceRequest.getFlightItinerary().getPricingInformation(issuanceRequest.isSeamen()).isSegmentWisePricing();

            if(isSegmentWisePricing){
                List<SegmentPricing> segmentPricingList = issuanceRequest.getFlightItinerary().getPricingInformation(issuanceRequest.isSeamen()).getSegmentPricingList();

                Map<String,AirSegmentInformation> segmentsInfo = new HashMap<>();
                for(Journey journey : issuanceRequest.getFlightItinerary().getJourneys(issuanceRequest.isSeamen())){
                    for(AirSegmentInformation airSegmentInformation : journey.getAirSegmentList()){
                        String key = airSegmentInformation.getFromLocation() + airSegmentInformation.getToLocation();
                        segmentsInfo.put(key, airSegmentInformation);
                    }
                }
                for(SegmentPricing segmentPricing : segmentPricingList) {
                    List<String> segmentKeysList = segmentPricing.getSegmentKeysList();
                    List<AirSegmentInformation> airSegment = new ArrayList<>();
                    for(String segmentKey : segmentKeysList){
                        airSegment.add(segmentsInfo.get(segmentKey));
                    }
                    carrierCode = airSegment.get(0).getCarrierCode();
                    pricePNRReply = serviceHandler.getLowestFare(carrierCode, gdsPNRReply,
                            issuanceRequest.isSeamen(), isDomestic, issuanceRequest.getFlightItinerary(), airSegment, isSegmentWisePricing, amadeusSessionWrapper);
                    List<FarePricePNRWithLowestFareReply.FareList> tempPricePNRReplyFareList = pricePNRReply.getFareList();
                    int numberOfTst = (issuanceRequest.isSeamen()) ? 1
                            : AmadeusBookingHelper.getNumberOfTST(issuanceRequest.getTravellerList());

                    if (tempPricePNRReplyFareList.size() > 0) {
                        for(int i = 0; i< numberOfTst ; i++){
                            pricePNRReplyFareList.add(tempPricePNRReplyFareList.get(i));
                        }
                    }
                }

            } else {
                pricePNRReply = serviceHandler.getLowestFare(carrierCode,gdsPNRReply, isSeamen,
                        isDomestic, issuanceRequest.getFlightItinerary(), airSegmentList, isSegmentWisePricing, amadeusSessionWrapper);
                pricePNRReplyFareList = pricePNRReply.getFareList();
                int numberOfTst = (issuanceRequest.isSeamen()) ? 1
                        : AmadeusBookingHelper.getNumberOfTST(issuanceRequest.getTravellerList());

                if(pricePNRReply.getFareList().size() != numberOfTst){
                    pricePNRReplyFareList = pricePNRReplyFareList.subList(0, numberOfTst);
                }

            }

            XMLFileUtility.createXMLFile(pricePNRReplyFareList, "lowestFareList.xml");
            Map<String,TSTLowestFare> tstLowestFareMap = new HashMap<>();
			PricingInformation pricingInformation = LowestFareHelper.getPricingInfo(pricePNRReplyFareList, issuanceRequest.getAdultCount(),
					issuanceRequest.getChildCount(), issuanceRequest.getInfantCount(), gdsPNRReply, tstLowestFareMap);

            BigDecimal newPrice = pricingInformation.getTotalPriceValue();

            String bookingClassStr = "";

            /*for(FareList fareList : pricePNRReplyFareList){
                for(SegmentInformation segmentInfo : fareList.getSegmentInformation()) {
                    BaggageDetailsTypeI bagAllowance = segmentInfo.getBagAllowanceInformation().getBagAllowanceDetails();
                    if(bagAllowance.getBaggageType().equalsIgnoreCase("w")) {
                        if(lowestFare.getMaxBaggageWeight() == 0 || lowestFare.getMaxBaggageWeight() > bagAllowance.getBaggageWeight().intValue()) {
                            lowestFare.setMaxBaggageWeight(bagAllowance.getBaggageWeight().intValue());
                        }
                    } else {
                        if(lowestFare.getBaggageCount() == 0 || lowestFare.getBaggageCount() > bagAllowance.getBaggageQuantity().intValue()) {
                            lowestFare.setBaggageCount(bagAllowance.getBaggageQuantity().intValue());
                        }
                    }

                    //reading booking class(RBD)
                    String bookingClass = segmentInfo.getSegDetails().getSegmentDetail().getClassOfService();
                    if(!bookingClassStr.contains(bookingClass)){
                        bookingClassStr = bookingClassStr + (bookingClassStr.length() == 0 ? bookingClass : "," + bookingClass);

                    }
                }

            }

            lowestFare.setBookingClass(bookingClassStr);*/
            lowestFare.setTstLowestFareMap(tstLowestFareMap);
            lowestFare.setGdsPnr(issuanceRequest.getGdsPNR());
            lowestFare.setAmount(newPrice);
            logger.debug("lowestFare: " + Json.toJson(lowestFare));
            serviceHandler.logOut(amadeusSessionWrapper);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Error in getLowestFare", e);
        }
        return lowestFare;
    }

    private void getTSTFare(FareList fare){

    }
}
