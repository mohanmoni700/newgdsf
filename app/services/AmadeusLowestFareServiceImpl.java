package services;

import com.amadeus.xml.pnracc_11_3_1a.PNRReply;
import com.amadeus.xml.tplprr_12_4_1a.BaggageDetailsTypeI;
import com.amadeus.xml.tplprr_12_4_1a.FarePricePNRWithLowestFareReply;
import com.compassites.GDSWrapper.amadeus.ServiceHandler;
import com.compassites.model.*;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import utils.AmadeusBookingHelper;
import utils.AmadeusHelper;
import utils.LowestFareHelper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.amadeus.xml.tplprr_12_4_1a.FarePricePNRWithLowestFareReply.FareList.SegmentInformation;

/**
 * Created by yaseen on 24-09-2016.
 */
@Service
public class AmadeusLowestFareServiceImpl implements LowestFareService{

    static org.slf4j.Logger logger = LoggerFactory.getLogger("gds");


    public LowFareResponse getLowestFare(IssuanceRequest issuanceRequest) {
        LowFareResponse lowestFare = new LowFareResponse();
        ServiceHandler serviceHandler = null;
        boolean isSeamen = issuanceRequest.isSeamen();
        try {

            serviceHandler = new ServiceHandler();
            serviceHandler.logIn();
            PNRReply gdsPNRReply = serviceHandler.retrivePNR(issuanceRequest.getGdsPNR());


            String carrierCode = "";

            carrierCode = issuanceRequest.getFlightItinerary().getJourneys(isSeamen).get(0).getAirSegmentList().get(0).getCarrierCode();
            boolean isDomestic = AmadeusHelper.checkAirportCountry("India", issuanceRequest.getFlightItinerary().getJourneys(isSeamen));
            FarePricePNRWithLowestFareReply pricePNRReply = new FarePricePNRWithLowestFareReply();

            List<AirSegmentInformation> airSegmentList = new ArrayList<>();
            for(Journey journey : issuanceRequest.getFlightItinerary().getJourneys(isSeamen)){
                for(AirSegmentInformation airSegmentInformation : journey.getAirSegmentList()){
                    airSegmentList.add(airSegmentInformation);
                }
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
                    pricePNRReply = serviceHandler.getLowestFare(carrierCode, gdsPNRReply,
                            issuanceRequest.isSeamen(), isDomestic, issuanceRequest.getFlightItinerary(), airSegment, isSegmentWisePricing);
                    List<FarePricePNRWithLowestFareReply.FareList> tempPricePNRReplyFareList = pricePNRReply.getFareList();

                    int numberOfTst = (issuanceRequest.isSeamen()) ? 1
                            : AmadeusBookingHelper.getNumberOfTST(issuanceRequest.getTravellerList());

                    for(int i = 0; i< numberOfTst ; i++){
                        pricePNRReplyFareList.add(tempPricePNRReplyFareList.get(i));
                    }
                }

            } else {
                pricePNRReply = serviceHandler.getLowestFare(carrierCode,gdsPNRReply, isSeamen,
                        isDomestic, issuanceRequest.getFlightItinerary(), airSegmentList, isSegmentWisePricing);
                pricePNRReplyFareList = pricePNRReply.getFareList();
                int numberOfTst = (issuanceRequest.isSeamen()) ? 1
                        : AmadeusBookingHelper.getNumberOfTST(issuanceRequest.getTravellerList());

                if(pricePNRReply.getFareList().size() != numberOfTst){
                    pricePNRReplyFareList = pricePNRReplyFareList.subList(0, numberOfTst);
                }

            }



			PricingInformation pricingInformation = LowestFareHelper.getPricingInfo(pricePNRReplyFareList, issuanceRequest.getAdultCount(),
					issuanceRequest.getChildCount(), issuanceRequest.getInfantCount());

            BigDecimal bookedPrice = issuanceRequest.getFlightItinerary().getPricingInformation(issuanceRequest.isSeamen()).getTotalPriceValue();
            BigDecimal newPrice = pricingInformation.getTotalPriceValue();

            /************************************************************/

            /*FarePricePNRWithLowestFareReply farePricePNRWithLowestFareReply = serviceHandler.getLowestFare(carrierCode,gdsPNRReply, isSeamen,
                    isDomestic, issuanceRequest.getFlightItinerary(), airSegmentList, isSegmentWisePricing);

            com.amadeus.xml.tplprr_12_4_1a.FarePricePNRWithLowestFareReply.FareList fareList = farePricePNRWithLowestFareReply.getFareList().get(0);
            MonetaryInformationType157202S monetaryinfo = fareList.getFareDataInformation();
            BigDecimal totalFare = null;
            for(MonetaryInformationDetailsType223844C monetaryDetails : monetaryinfo.getFareDataSupInformation()) {
                if(AmadeusConstants.TOTAL_FARE_IDENTIFIER.equals(monetaryDetails.getFareDataQualifier())) {
                    totalFare = new BigDecimal(monetaryDetails.getFareAmount());
                    break;
                }
            }
*/
            String bookingClassStr = "";

            for(FarePricePNRWithLowestFareReply.FareList fareList : pricePNRReplyFareList){
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



            lowestFare.setBookingClass(bookingClassStr);
            lowestFare.setGdsPnr(issuanceRequest.getGdsPNR());
            lowestFare.setAmount(newPrice);

            serviceHandler.logOut();
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Error in getLowestFare", e);
        }
        return lowestFare;
    }
}
