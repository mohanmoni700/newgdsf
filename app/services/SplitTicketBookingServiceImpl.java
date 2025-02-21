package services;

import com.amadeus.xml.farqnr_07_1_1a.FareCheckRulesReply;
import com.amadeus.xml.itares_05_2_ia.AirSellFromRecommendationReply;
import com.amadeus.xml.pnracc_11_3_1a.PNRReply;
import com.amadeus.xml.pnrxcl_11_3_1a.PNRCancel;
import com.amadeus.xml.tautcr_04_1_1a.TicketCreateTSTFromPricingReply;
import com.amadeus.xml.tipnrr_12_4_1a.FareInformativePricingWithoutPNRReply;
import com.amadeus.xml.tpcbrr_12_4_1a.FarePricePNRWithBookingClassReply;
import com.amadeus.xml.tpcbrr_12_4_1a.StructuredDateTimeType;
import com.amadeus.xml.ttstrr_13_1_1a.MonetaryInformationDetailsTypeI211824C;
import com.amadeus.xml.ttstrr_13_1_1a.ReferencingDetailsTypeI;
import com.amadeus.xml.ttstrr_13_1_1a.TicketDisplayTSTReply;
import com.compassites.GDSWrapper.amadeus.PNRAddMultiElementsh;
import com.compassites.GDSWrapper.amadeus.ServiceHandler;
import com.compassites.constants.AmadeusConstants;
import com.compassites.constants.StaticConstatnts;
import com.compassites.exceptions.BaseCompassitesException;
import com.compassites.model.*;
import com.compassites.model.amadeus.AmadeusPaxInformation;
import com.compassites.model.traveller.Traveller;
import com.compassites.model.traveller.TravellerMasterInfo;
import models.AmadeusSessionWrapper;
import org.apache.commons.lang3.SerializationUtils;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.PeriodType;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import play.Configuration;
import play.Play;
import play.libs.Json;
import utils.*;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class SplitTicketBookingServiceImpl implements SplitTicketBookingService {

    static org.slf4j.Logger amadeusLogger = LoggerFactory.getLogger("amadeus");

    static org.slf4j.Logger logger = LoggerFactory.getLogger("gds");

    @Autowired
    private AmadeusSessionManager amadeusSessionManager;


    @Autowired
    private AmadeusTicketCancelDocumentServiceImpl amadeusTicketCancelDocumentServiceImpl;

    @Autowired
    private AmadeusSourceOfficeService amadeusSourceOfficeService;

    private static Map<String, String> baggageCodes = new HashMap<>();

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private ServiceHandler serviceHandler;

    static {
        baggageCodes.put("700", "KG");
        baggageCodes.put("K", "KG");
        baggageCodes.put("701", "Lb");
        baggageCodes.put("L", "Lb");
        baggageCodes.put("C", "Special Charge");
        baggageCodes.put("N", "PC");
        baggageCodes.put("S", "Size");
        baggageCodes.put("V", "Value");
        baggageCodes.put("W", "Weight");
    }

    @Autowired
    AmadeusFlightInfoServiceImpl amadeusFlightInfoService;

    public SplitTicketBookingServiceImpl() {
    }

    @Override
    public List<PNRResponse> generatePNR(List<TravellerMasterInfo> travellerMasterInfo) {
        return null;
    }

    @Override
    public List<PNRResponse> priceChangePNR(List<TravellerMasterInfo> travellerMasterInfo) {
        return null;
    }

    @Override
    public List<PNRResponse> generateSplitTicketPNR(List<TravellerMasterInfo> travellerMasterInfos) {
        List<PNRResponse> pnrResponseList = new ArrayList<>();
        for (TravellerMasterInfo travellerMasterInfo: travellerMasterInfos) {
            logger.debug("generatePNR called ........");
            PNRResponse pnrResponse = new PNRResponse();
            PNRReply gdsPNRReply = null;
            FarePricePNRWithBookingClassReply pricePNRReply = null;
            AmadeusSessionWrapper amadeusSessionWrapper = null;
            String tstRefNo = "";
            try {
                amadeusSessionWrapper = amadeusSessionManager.getActiveSessionByRef(travellerMasterInfo.getSessionIdRef());
                logger.debug("generatePNR called........"+ Json.stringify(Json.toJson(amadeusSessionWrapper)));
                tstRefNo = travellerMasterInfo.getGdsPNR();
                /*int numberOfTst = (travellerMasterInfo.isSeamen()) ? 1	: getNumberOfTST(travellerMasterInfo.getTravellersList());
                if( travellerMasterInfo.getGdsPNR()== null ) {
                    createTST(pnrResponse, amadeusSessionWrapper, numberOfTst);
                    gdsPNRReply = serviceHandler.savePNR(amadeusSessionWrapper);
                    tstRefNo = getPNRNoFromResponse(gdsPNRReply);
                    System.out.println(tstRefNo);
                    Thread.sleep(10000);
                }else{
                    tstRefNo = travellerMasterInfo.getGdsPNR();
                }
                if(travellerMasterInfo.getAdditionalInfo()!=null && travellerMasterInfo.getAdditionalInfo().getAddBooking()!=null && travellerMasterInfo.getAdditionalInfo().getAddBooking()) {
                    pnrResponse.setAddBooking(true);
                    pnrResponse.setOriginalPNR(tstRefNo);
                }*/
                try {
                    gdsPNRReply = serviceHandler.retrivePNR(tstRefNo, amadeusSessionWrapper);
                    TicketDisplayTSTReply ticketDisplayTSTReply = serviceHandler.ticketDisplayTST(amadeusSessionWrapper);
                    createSegmentPricing(gdsPNRReply,pnrResponse,ticketDisplayTSTReply);
                }catch(NullPointerException e){
                    logger.error("error in Retrieve PNR"+ e.getMessage());
                    e.printStackTrace();
                }catch(Exception ex){
                    ex.printStackTrace();
                    if(ex != null && ex.getMessage() != null &&  ex.getMessage().toString().contains("IGNORE")) {
                        gdsPNRReply =   serviceHandler.ignoreAndRetrievePNR(amadeusSessionWrapper);
                    }
                }
                Date lastPNRAddMultiElements = new Date();

                gdsPNRReply = readAirlinePNR(gdsPNRReply,lastPNRAddMultiElements,pnrResponse, amadeusSessionWrapper);
                checkSegmentStatus(gdsPNRReply);
                List<String> segmentNumbers = new ArrayList<>();
                for(PNRReply.OriginDestinationDetails originDestinationDetails : gdsPNRReply.getOriginDestinationDetails()){
                    for(PNRReply.OriginDestinationDetails.ItineraryInfo itineraryInfo : originDestinationDetails.getItineraryInfo()){
                        segmentNumbers.add(""+itineraryInfo.getElementManagementItinerary().getReference().getNumber());
                    }
                }
                Map<String,String> travellerMap = new HashMap<>();
                for(PNRReply.TravellerInfo travellerInfo : gdsPNRReply.getTravellerInfo()){
                    String keyNo =  "" + travellerInfo.getElementManagementPassenger().getReference().getNumber();
                    String lastName = travellerInfo.getPassengerData().get(0).getTravellerInformation().getTraveller().getSurname();
                    String name = travellerInfo.getPassengerData().get(0).getTravellerInformation().getPassenger().get(0).getFirstName();
                    String travellerName = name + lastName;
                    travellerName = travellerName.replaceAll("\\s+", "");
                    travellerName = travellerName.toLowerCase();
                    travellerMap.put(travellerName,keyNo);
                }
                addSSRDetailsToPNR(travellerMasterInfo, 1, lastPNRAddMultiElements, segmentNumbers, travellerMap, amadeusSessionWrapper);
                Thread.sleep(5000);
                gdsPNRReply = serviceHandler.retrivePNR(tstRefNo,amadeusSessionWrapper);
                createPNRResponse(gdsPNRReply, pricePNRReply, pnrResponse,travellerMasterInfo);
                //logger.debug("todo generatePNR called 8........gdsPNRReply:"+ Json.stringify(Json.toJson(gdsPNRReply)) + "   ***** pricePNRReply:");
            } catch (Exception e) {
                logger.error("todo error in generating PNR"+ e.getMessage());
                e.printStackTrace();
                logger.error("error in generatePNR : ", e);
                if(BaseCompassitesException.ExceptionCode.NO_SEAT.toString().equalsIgnoreCase(e.getMessage().toString())){
                    ErrorMessage errorMessage = new ErrorMessage();
                    errorMessage.setErrorCode(StaticConstatnts.NO_SEAT);
                    errorMessage.setType(ErrorMessage.ErrorType.ERROR);
                    errorMessage.setProvider(PROVIDERS.AMADEUS.toString());
                    errorMessage.setMessage(e.getMessage());
                    errorMessage.setGdsPNR(tstRefNo);
                    pnrResponse.setErrorMessage(errorMessage);
                } else {
                    ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage(
                            "error", ErrorMessage.ErrorType.ERROR, PROVIDERS.AMADEUS.toString());
                    pnrResponse.setErrorMessage(errorMessage);
                }
            }finally {
                if(amadeusSessionWrapper != null){
                   // amadeusSessionManager.removeActiveSession(amadeusSessionWrapper.getmSession().value);
                   // serviceHandler.logOut(amadeusSessionWrapper);
                }
            }
            logger.info("generatePNR Split :"+ Json.stringify(Json.toJson(pnrResponse)));
            pnrResponseList.add(pnrResponse);
        }
        return pnrResponseList;
    }

    private void createSegmentPricing(PNRReply gdsPNRReply,PNRResponse pnrResponse,TicketDisplayTSTReply ticketDisplayTSTReply) {
        List<TicketDisplayTSTReply.FareList> fareList = ticketDisplayTSTReply.getFareList();
        List<SegmentPricing> segmentPricingList = new ArrayList<>();
        List<PassengerTax> passengerTaxList = new ArrayList<>();
        boolean segmentWisePricing = false;
        Map<String, TSTPrice> tstPriceMap = new HashMap<>();

        Map<String,Object> airSegmentRefMap = new HashMap<>();
        Map<String,Object> travellerMap = new HashMap<>();
        Map<String, String> passengerType = new HashMap<>();
        BigDecimal totalPriceOfBooking = new BigDecimal(0);
        BigDecimal basePriceOfBooking = new BigDecimal(0);
        BigDecimal adtBaseFare = new BigDecimal(0);
        BigDecimal chdBaseFare = new BigDecimal(0);
        BigDecimal infBaseFare = new BigDecimal(0);
        BigDecimal adtTotalFare = new BigDecimal(0);
        BigDecimal chdTotalFare = new BigDecimal(0);
        BigDecimal infTotalFare = new BigDecimal(0);
        Map<String, AirSegmentInformation> segmentMap = new HashMap<>();
        String currency = null;
        PricingInformation pricingInformation = new PricingInformation();
        for(PNRReply.OriginDestinationDetails originDestination : gdsPNRReply.getOriginDestinationDetails()){
            for(PNRReply.OriginDestinationDetails.ItineraryInfo itineraryInfo : originDestination.getItineraryInfo()){
                String segType = itineraryInfo.getElementManagementItinerary().getSegmentName();
                if(segType.equalsIgnoreCase("AIR")) {
                    String segmentRef = "S" + itineraryInfo.getElementManagementItinerary().getReference().getNumber();
                    String segments = itineraryInfo.getTravelProduct().getBoardpointDetail().getCityCode() + itineraryInfo.getTravelProduct().getOffpointDetail().getCityCode();
                    airSegmentRefMap.put(segmentRef, segments);
                }
            }
        }
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
            String paxType = "ADT";
            totalFarePerPaxType = totalFarePerPaxType.add(paxTotalFare.multiply(new BigDecimal(paxCount)));
            baseFareOfPerPaxType = baseFareOfPerPaxType.add(baseFare.multiply(new BigDecimal(paxCount)));
            PassengerTax passengerTax = getTaxDetailsFromTST(fare.getTaxInformation(), paxType, paxCount);
            passengerTaxList.add(passengerTax);

            if(airSegmentRefMap.size() != fare.getSegmentInformation().size()){
                segmentWisePricing = true;
            }
            List<String> segmentKeys = new ArrayList<>();
            //if(segmentWisePricing){
            for(TicketDisplayTSTReply.FareList.SegmentInformation segmentInformation : fare.getSegmentInformation()){
                if(segmentInformation.getSegmentReference() != null && segmentInformation.getSegmentReference().getRefDetails() != null){
                    ReferencingDetailsTypeI referencingDetailsTypeI = segmentInformation.getSegmentReference().getRefDetails().get(0);
                    String key = referencingDetailsTypeI.getRefQualifier() + referencingDetailsTypeI.getRefNumber();
                    segmentKeys.add(airSegmentRefMap.get(key).toString().toLowerCase());
                }

            }
            //}
            segmentPricing.setSegmentKeysList(segmentKeys);
            segmentPricing.setTotalPrice(totalFarePerPaxType);
            segmentPricing.setBasePrice(baseFareOfPerPaxType);
            segmentPricing.setTax(totalFarePerPaxType.subtract(baseFareOfPerPaxType));
            segmentPricing.setPassengerType("ADT");
            segmentPricing.setPassengerTax(passengerTax);
            segmentPricing.setPassengerCount(new Long(paxCount));
            segmentPricing.setTstSequenceNumber(fare.getFareReference().getIDDescription().getIDSequenceNumber());
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

            TSTPrice tstPrice = getTSTPrice(fare, paxTotalFare, baseFare,paxType, passengerTax);
            for (TicketDisplayTSTReply.FareList.SegmentInformation segmentInformation : fare.getSegmentInformation()) {
                for (String key : segmentMap.keySet()) {
                    for(Object airSegVal : airSegmentRefMap.values()) {
                        if (key.equals(airSegVal) && segmentInformation.getFareQualifier()!=null && segmentInformation.getFareQualifier().size()>0) {
                            String farebasis = segmentInformation.getFareQualifier().get(0).getFareBasisDetails().getPrimaryCode()
                                    + segmentInformation.getFareQualifier().get(0).getFareBasisDetails().getFareBasisCode();
                            segmentMap.get(key).setFareBasis(farebasis);
                        }
                    }
                }

                if(segmentInformation.getSegmentReference() != null && segmentInformation.getSegmentReference().getRefDetails() != null){
                    ReferencingDetailsTypeI referencingDetailsTypeI = segmentInformation.getSegmentReference().getRefDetails().get(0);
                    String key = referencingDetailsTypeI.getRefQualifier() + referencingDetailsTypeI.getRefNumber();

                    String tstKey = airSegmentRefMap.get(key).toString() + paxType;
                    tstPriceMap.put(tstKey.toLowerCase(), tstPrice);
                }
            }
        }
        pricingInformation.setSegmentWisePricing(segmentWisePricing);
        pricingInformation.setSegmentPricingList(segmentPricingList);
        pnrResponse.setPricingInfo(pricingInformation);
    }

    public static TSTPrice getTSTPrice(TicketDisplayTSTReply.FareList fare, BigDecimal paxTotalFare, BigDecimal paxBaseFare, String paxType, PassengerTax passengerTax){
        TSTPrice tstPrice = new TSTPrice();
        for(TicketDisplayTSTReply.FareList.SegmentInformation segmentInfo : fare.getSegmentInformation()) {
            com.amadeus.xml.ttstrr_13_1_1a.BaggageDetailsTypeI bagAllowance =  null;
            if(segmentInfo.getBagAllowanceInformation()!=null){
                bagAllowance = segmentInfo.getBagAllowanceInformation().getBagAllowanceDetails();
                if(bagAllowance.getBaggageType().equalsIgnoreCase("w")) {
                    if(tstPrice.getMaxBaggageWeight() == 0 || tstPrice.getMaxBaggageWeight() > bagAllowance.getBaggageWeight().intValue()) {
                        tstPrice.setMaxBaggageWeight(bagAllowance.getBaggageWeight().intValue());
                    }
                } else {
                    if(tstPrice.getBaggageCount() == 0 || tstPrice.getBaggageCount() > bagAllowance.getBaggageQuantity().intValue()) {
                        tstPrice.setBaggageCount(bagAllowance.getBaggageQuantity().intValue());
                    }
                }
            }

            //reading booking class(RBD)
            String bookingClass = null;
            if(segmentInfo.getSegDetails()!=null && segmentInfo.getSegDetails().getSegmentDetail()!=null){
                bookingClass = segmentInfo.getSegDetails().getSegmentDetail().getClassOfService();
            }
            tstPrice.setBookingClass(bookingClass);
        }

        tstPrice.setTotalPrice(paxTotalFare);
        tstPrice.setBasePrice(paxBaseFare);
        tstPrice.setPassengerType(paxType);
        tstPrice.setPassengerTax(passengerTax);
        return tstPrice;
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

    @Override
    public PNRResponse generateSplitTicketWithSinglePNR(TravellerMasterInfo travellerMasterInfo) {
        logger.debug("generatePNR called ........");
        PNRResponse pnrResponse = new PNRResponse();
        PNRReply gdsPNRReply = null;
        FarePricePNRWithBookingClassReply pricePNRReply = null;
        AmadeusSessionWrapper amadeusSessionWrapper = null;
        String tstRefNo = "";
        String officeId = null;
        try {
            officeId = getSpecificOfficeIdforAirline(travellerMasterInfo.getItinerary());
            boolean isDelIdAirline = isDelIdAirlines(travellerMasterInfo);
            boolean isDelIdSeamen = (isDelIdAirline && travellerMasterInfo.isSeamen()) ? true : false;
            if(officeId == null) {
                if(travellerMasterInfo.isSeamen()){
                    officeId = travellerMasterInfo.getItinerary().getSeamanPricingInformation().getPricingOfficeId();
                }else{
                    if(isDelIdAirline) {
                        officeId = amadeusSourceOfficeService.getDelhiSourceOffice().getOfficeId();
                    } else {
                        officeId = travellerMasterInfo.getItinerary().getPricingInformation().getPricingOfficeId();
                    }
                }
                System.out.println("Off "+officeId);
            }
            /**
             * check for non batk and set booking office to BOM
             */
            if (officeId.equals(amadeusSourceOfficeService.getBenzySourceOffice().getOfficeId()) && !isBATK(travellerMasterInfo)) {
                officeId = amadeusSourceOfficeService.getDelhiSourceOffice().getOfficeId();
            }
            amadeusSessionWrapper = serviceHandler.logIn(officeId);
            logger.debug("generatePNR called........"+ Json.stringify(Json.toJson(amadeusSessionWrapper)));

            tstRefNo = travellerMasterInfo.getGdsPNR();

            try {
                gdsPNRReply = serviceHandler.retrivePNR(tstRefNo, amadeusSessionWrapper);
                TicketDisplayTSTReply ticketDisplayTSTReply = serviceHandler.ticketDisplayTST(amadeusSessionWrapper);
                createSegmentPricing(gdsPNRReply,pnrResponse,ticketDisplayTSTReply);
            }catch(NullPointerException e){
                logger.error("error in Retrieve PNR"+ e.getMessage());
                e.printStackTrace();
            }catch(Exception ex){
                ex.printStackTrace();
                if(ex != null && ex.getMessage() != null &&  ex.getMessage().toString().contains("IGNORE")) {
                    gdsPNRReply =   serviceHandler.ignoreAndRetrievePNR(amadeusSessionWrapper);
                }
            }
            Date lastPNRAddMultiElements = new Date();

            gdsPNRReply = readAirlinePNR(gdsPNRReply,lastPNRAddMultiElements,pnrResponse, amadeusSessionWrapper);
            checkSegmentStatus(gdsPNRReply);
            List<String> segmentNumbers = new ArrayList<>();
            for(PNRReply.OriginDestinationDetails originDestinationDetails : gdsPNRReply.getOriginDestinationDetails()){
                for(PNRReply.OriginDestinationDetails.ItineraryInfo itineraryInfo : originDestinationDetails.getItineraryInfo()){
                    segmentNumbers.add(""+itineraryInfo.getElementManagementItinerary().getReference().getNumber());
                }
            }
            Map<String,String> travellerMap = new HashMap<>();
            for(PNRReply.TravellerInfo travellerInfo : gdsPNRReply.getTravellerInfo()){
                String keyNo =  "" + travellerInfo.getElementManagementPassenger().getReference().getNumber();
                String lastName = travellerInfo.getPassengerData().get(0).getTravellerInformation().getTraveller().getSurname();
                String name = travellerInfo.getPassengerData().get(0).getTravellerInformation().getPassenger().get(0).getFirstName();
                String travellerName = name + lastName;
                travellerName = travellerName.replaceAll("\\s+", "");
                travellerName = travellerName.toLowerCase();
                travellerMap.put(travellerName,keyNo);
            }
            //addSSRDetailsToPNR(travellerMasterInfo, 1, lastPNRAddMultiElements, segmentNumbers, travellerMap, amadeusSessionWrapper);
            //Thread.sleep(5000);
            gdsPNRReply = serviceHandler.retrivePNR(tstRefNo,amadeusSessionWrapper);
            createPNRResponse(gdsPNRReply, pricePNRReply, pnrResponse,travellerMasterInfo);
            //logger.debug("todo generatePNR called 8........gdsPNRReply:"+ Json.stringify(Json.toJson(gdsPNRReply)) + "   ***** pricePNRReply:");
        } catch (Exception e) {
            logger.error("todo error in generating PNR"+ e.getMessage());
            e.printStackTrace();
            logger.error("error in generatePNR : ", e);
            if(BaseCompassitesException.ExceptionCode.NO_SEAT.toString().equalsIgnoreCase(e.getMessage().toString())){
                ErrorMessage errorMessage = new ErrorMessage();
                errorMessage.setErrorCode(StaticConstatnts.NO_SEAT);
                errorMessage.setType(ErrorMessage.ErrorType.ERROR);
                errorMessage.setProvider(PROVIDERS.AMADEUS.toString());
                errorMessage.setMessage(e.getMessage());
                errorMessage.setGdsPNR(tstRefNo);
                pnrResponse.setErrorMessage(errorMessage);
            } else {
                ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage(
                        "error", ErrorMessage.ErrorType.ERROR, PROVIDERS.AMADEUS.toString());
                pnrResponse.setErrorMessage(errorMessage);
            }
        }finally {
            if(amadeusSessionWrapper != null){
                amadeusSessionManager.removeActiveSession(amadeusSessionWrapper.getmSession().value);
                serviceHandler.logOut(amadeusSessionWrapper);
            }
        }
        logger.info("generatePNR :"+ Json.stringify(Json.toJson(pnrResponse)));
        return pnrResponse;
    }

    public void checkSegmentStatus(PNRReply pnrReply) throws BaseCompassitesException {
        for(PNRReply.OriginDestinationDetails originDestinationDetails : pnrReply.getOriginDestinationDetails()){
            for(PNRReply.OriginDestinationDetails.ItineraryInfo itineraryInfo : originDestinationDetails.getItineraryInfo()){
                for(String status : itineraryInfo.getRelatedProduct().getStatus()){
                    if(!AmadeusConstants.SEGMENT_HOLDING_CONFIRMED.equalsIgnoreCase(status)){
                        logger.debug("No Seats Available as segment status is : "+status);
                        throw new BaseCompassitesException(BaseCompassitesException.ExceptionCode.NO_SEAT.getExceptionCode());
                    }

                }
            }
        }

        return ;
    }

    public PNRResponse createPNRResponse(PNRReply gdsPNRReply,
                                         FarePricePNRWithBookingClassReply pricePNRReply, PNRResponse pnrResponse, TravellerMasterInfo travellerMasterInfo) {
        pnrResponse.setPnrNumber(gdsPNRReply.getPnrHeader().get(0).getReservationInfo().getReservation().getControlNumber());
        pnrResponse.setAmadeusPaxReference(createAmadeusPaxRefInfo(gdsPNRReply));

        if(pricePNRReply != null){
            setLastTicketingDate(pricePNRReply, pnrResponse, travellerMasterInfo);
        }
        pnrResponse.setFlightAvailable(true);
        if(gdsPNRReply.getSecurityInformation() != null && gdsPNRReply.getSecurityInformation().getSecondRpInformation() != null)
            pnrResponse.setCreationOfficeId(gdsPNRReply.getSecurityInformation().getSecondRpInformation().getCreationOfficeId());
        logger.debug("todo createPNRResponse: "+ Json.stringify(Json.toJson(pnrResponse)) );
        return pnrResponse;
    }

    public static List<AmadeusPaxInformation> createAmadeusPaxRefInfo(PNRReply gdsPNRReply) {

        List<AmadeusPaxInformation> amadeusPaxInformationList = new ArrayList<>();
        List<PNRReply.TravellerInfo> travellerInfoList = gdsPNRReply.getTravellerInfo();

        for (PNRReply.TravellerInfo travellerInfo : travellerInfoList) {
            amadeusPaxInformationList.add(AmadeusBookingHelper.extractPassengerData(travellerInfo));
        }

        return amadeusPaxInformationList;
    }

    private void addSSRDetailsToPNR(TravellerMasterInfo travellerMasterInfo, int iteration, Date lastPNRAddMultiElements,
                                    List<String> segmentNumbers, Map<String,String> travellerMap, AmadeusSessionWrapper amadeusSessionWrapper) throws BaseCompassitesException, InterruptedException {
        if(iteration <= 3){
            PNRReply addSSRResponse = serviceHandler.addSSRDetailsToPNR(travellerMasterInfo, segmentNumbers, travellerMap, amadeusSessionWrapper);
            simultaneousChangeAction(addSSRResponse, serviceHandler, lastPNRAddMultiElements, travellerMasterInfo, iteration, segmentNumbers, travellerMap, amadeusSessionWrapper);
            PNRReply savePNRReply = serviceHandler.savePNR(amadeusSessionWrapper);
            simultaneousChangeAction(savePNRReply, serviceHandler, lastPNRAddMultiElements, travellerMasterInfo, iteration, segmentNumbers, travellerMap, amadeusSessionWrapper);
        }else {
            serviceHandler.ignorePNRAddMultiElement(amadeusSessionWrapper);
            throw new BaseCompassitesException("Simultaneous changes Error");
        }
    }

    private void simultaneousChangeAction(PNRReply addSSRResponse, ServiceHandler serviceHandler,
                                          Date lastPNRAddMultiElements, TravellerMasterInfo travellerMasterInfo, int iteration,
                                          List<String> segmentNumbers, Map<String, String> travellerMap, AmadeusSessionWrapper amadeusSessionWrapper) throws InterruptedException, BaseCompassitesException {

        boolean simultaneousChangeToPNR = AmadeusBookingHelper.checkForSimultaneousChange(addSSRResponse);
        if (simultaneousChangeToPNR) {
            Period p = new Period(new DateTime(lastPNRAddMultiElements), new DateTime(), PeriodType.seconds());
            if (p.getSeconds() >= 12) {
                serviceHandler.ignorePNRAddMultiElement(amadeusSessionWrapper);
                throw new BaseCompassitesException("Simultaneous changes Error");
            } else {
                Thread.sleep(3000);
                PNRReply pnrReply = serviceHandler.ignoreAndRetrievePNR(amadeusSessionWrapper);
                lastPNRAddMultiElements = new Date();
                iteration = iteration + 1;
                addSSRDetailsToPNR(travellerMasterInfo, iteration, lastPNRAddMultiElements, segmentNumbers, travellerMap, amadeusSessionWrapper);
            }
        }

    }

    private PNRReply readAirlinePNR(PNRReply  pnrReply, Date lastPNRAddMultiElements, PNRResponse pnrResponse, AmadeusSessionWrapper amadeusSessionWrapper) throws BaseCompassitesException, InterruptedException {
        List<PNRReply.OriginDestinationDetails.ItineraryInfo> itineraryInfos = pnrReply.getOriginDestinationDetails().get(0).getItineraryInfo();
        String airlinePnr = null;
        if(itineraryInfos != null && itineraryInfos.size() > 0) {
            for(PNRReply.OriginDestinationDetails.ItineraryInfo itineraryInfo : itineraryInfos){
                if(itineraryInfo.getItineraryReservationInfo() != null && itineraryInfo.getItineraryReservationInfo().getReservation() != null){
                    airlinePnr =  itineraryInfo.getItineraryReservationInfo().getReservation().getControlNumber();
                }
            }
            pnrResponse.setAirlinePNR(airlinePnr);
            pnrResponse.setAirlinePNRMap(AmadeusHelper.readMultipleAirlinePNR(pnrReply));
        }
        if(airlinePnr == null){
            Period p = new Period(new DateTime(lastPNRAddMultiElements), new DateTime(), PeriodType.seconds());
            if (p.getSeconds() >= 12) {
                pnrResponse.setAirlinePNRError(true);
                for (PNRReply.PnrHeader pnrHeader : pnrReply.getPnrHeader()) {
                    pnrResponse.setPnrNumber(pnrHeader.getReservationInfo()
                            .getReservation().getControlNumber());
                }
                throw new BaseCompassitesException("Simultaneeous changes Error");
            }else {
                Thread.sleep(3000);
                pnrReply = serviceHandler.ignoreAndRetrievePNR(amadeusSessionWrapper);
                //lastPNRAddMultiElements = new Date();
                readAirlinePNR(pnrReply, lastPNRAddMultiElements, pnrResponse, amadeusSessionWrapper);
            }
        }

        return pnrReply;
    }

    @Override
    public PNRResponse checkFareChangeAndAvailabilityForSplitTicket(List<TravellerMasterInfo> travellerMasterInfos) {
        FarePricePNRWithBookingClassReply pricePNRReply = null;
        AmadeusSessionWrapper amadeusSessionWrapper = null;
        AmadeusSessionWrapper benzyAmadeusSessionWrapper = null;
        String officeId = null;
        boolean isFirstSegmentSell = false;
        PNRResponse pnrResponse = null;
        int i=1;
        List<PNRResponse> pnrResponses = new ArrayList<>();
        String pnr = "";
        for (TravellerMasterInfo travellerMasterInfo : travellerMasterInfos) {
            pnrResponse = new PNRResponse();
            try {
                officeId = getSpecificOfficeIdforAirline(travellerMasterInfo.getItinerary());
                boolean isDelIdAirline = isDelIdAirlines(travellerMasterInfo);
                boolean isDelIdSeamen = (isDelIdAirline && travellerMasterInfo.isSeamen()) ? true : false;
                if (officeId == null) {
                    if (travellerMasterInfo.isSeamen()) {
                        officeId = travellerMasterInfo.getItinerary().getSeamanPricingInformation().getPricingOfficeId();
                    } else {
                        if (isDelIdAirline) {
                            officeId = amadeusSourceOfficeService.getDelhiSourceOffice().getOfficeId();
                        } else {
                            officeId = travellerMasterInfo.getItinerary().getPricingInformation().getPricingOfficeId();
                        }
                    }
                }
                if (officeId.equals(amadeusSourceOfficeService.getBenzySourceOffice().getOfficeId()) && !isBATK(travellerMasterInfo)) {
                    officeId = amadeusSourceOfficeService.getDelhiSourceOffice().getOfficeId();
                }
                amadeusSessionWrapper = serviceHandler.logIn(officeId);
                PNRReply gdsPNRReply = null;
                if (isFirstSegmentSell) {
                    gdsPNRReply = serviceHandler.retrivePNR(pnr, amadeusSessionWrapper);
                }
                checkFlightAvailibility(travellerMasterInfo, pnrResponse, amadeusSessionWrapper);
                if (pnrResponse.isFlightAvailable()) {
                    gdsPNRReply = serviceHandler.addTravellerInfoToPNR(travellerMasterInfo, amadeusSessionWrapper);
                    /* Benzy changes */
                    PNRReply gdsPNRReplyBenzy = null;
                    FarePricePNRWithBookingClassReply pricePNRReplyBenzy = null;
                    travellerMasterInfo.getItinerary().getPricingInformation().setSegmentWisePricing(true);
                    pricePNRReply = checkPNRPricing(travellerMasterInfo, gdsPNRReply, pricePNRReply, pnrResponse, amadeusSessionWrapper);
                    int numberOfTst = (travellerMasterInfo.isSeamen()) ? 1 : getNumberOfTST(travellerMasterInfo.getTravellersList());

                    logger.debug(" gdsPNRReply " + Json.toJson(gdsPNRReply));
                    if (pnrResponse.isOfficeIdPricingError() || isDelIdSeamen) {
                        gdsPNRReply = serviceHandler.savePNR(amadeusSessionWrapper);
                        String tstRefNo = getPNRNoFromResponse(gdsPNRReply);
                        System.out.println(tstRefNo);
                        logger.debug("checkFareChangeAndAvailability called..........." + pnrResponse);
                        gdsPNRReplyBenzy = serviceHandler.retrivePNR(tstRefNo, amadeusSessionWrapper);
                        //pnrResponse.setPnrNumber(tstRefNo);
                        Boolean error = Boolean.FALSE;
                        gdsPNRReply = serviceHandler.savePNRES(amadeusSessionWrapper, amadeusSourceOfficeService.getBenzySourceOffice().getOfficeId());
                        if (gdsPNRReply.getGeneralErrorInfo().size() > 0) {
                            Thread.sleep(20000);
                            error = Boolean.TRUE;
                        }
                        if (!error) {
                            benzyAmadeusSessionWrapper = serviceHandler.logIn(amadeusSourceOfficeService.getBenzySourceOffice().getOfficeId());
                            PNRReply pnrReply = serviceHandler.retrivePNR(tstRefNo, benzyAmadeusSessionWrapper);
                            pricePNRReplyBenzy = checkPNRPricing(travellerMasterInfo, gdsPNRReplyBenzy, pricePNRReplyBenzy, pnrResponse, benzyAmadeusSessionWrapper);
                            createTST(pnrResponse, benzyAmadeusSessionWrapper, numberOfTst);
                            setLastTicketingDate(pricePNRReplyBenzy, pnrResponse, travellerMasterInfo);
                            gdsPNRReplyBenzy = serviceHandler.savePNR(benzyAmadeusSessionWrapper);

                            if (gdsPNRReplyBenzy.getGeneralErrorInfo().size() > 0) {
                                List<PNRReply.GeneralErrorInfo> generalErrorInfos = gdsPNRReplyBenzy.getGeneralErrorInfo();
                                for (PNRReply.GeneralErrorInfo generalErrorInfo : generalErrorInfos) {
                                    String textMsg = generalErrorInfo.getMessageErrorText().getText().get(0).trim();
                                    if (textMsg.equals("SIMULTANEOUS CHANGES TO PNR - USE WRA/RT TO PRINT OR IGNORE")) {
                                        error = Boolean.TRUE;
                                    }
                                }
                            }
                        }
                        if (error) {
                            PNRCancel pnrCancel = new PNRAddMultiElementsh().exitEsx(tstRefNo);
                            serviceHandler.exitESPnr(pnrCancel, amadeusSessionWrapper);
                            serviceHandler.logOut(benzyAmadeusSessionWrapper);
                            gdsPNRReply = serviceHandler.savePNRES(amadeusSessionWrapper, amadeusSourceOfficeService.getBenzySourceOffice().getOfficeId());
                            benzyAmadeusSessionWrapper = serviceHandler.logIn(amadeusSourceOfficeService.getBenzySourceOffice().getOfficeId());
                            try {
                                serviceHandler.retrivePNR(tstRefNo, benzyAmadeusSessionWrapper);
                            } catch (Exception ex) {
                                ex.printStackTrace();
                                if (ex != null && ex.getMessage() != null && ex.getMessage().toString().contains("IGNORE")) {
                                    gdsPNRReply = serviceHandler.ignoreAndRetrievePNR(benzyAmadeusSessionWrapper);
                                }
                            }
                            //serviceHandler.retrivePNR(tstRefNo,benzyAmadeusSessionWrapper);
                            pricePNRReplyBenzy = checkPNRPricing(travellerMasterInfo, gdsPNRReplyBenzy, pricePNRReplyBenzy, pnrResponse, benzyAmadeusSessionWrapper);
                            createTST(pnrResponse, benzyAmadeusSessionWrapper, numberOfTst);
                            setLastTicketingDate(pricePNRReplyBenzy, pnrResponse, travellerMasterInfo);
                            gdsPNRReplyBenzy = serviceHandler.savePNR(benzyAmadeusSessionWrapper);
                        }
                        if (pnrResponse.getPricingInfo() != null)
                            pnrResponse.getPricingInfo().setPricingOfficeId(amadeusSourceOfficeService.getBenzySourceOffice().getOfficeId().toString());
                        FareCheckRulesReply fareCheckRulesReply = serviceHandler.getFareRules(benzyAmadeusSessionWrapper);
                        try {
                            Map<String, Map> benzyFareRulesMap = null;
                            if (fareCheckRulesReply.getErrorInfo() == null)
                                benzyFareRulesMap = AmadeusHelper.getFareCheckRules(fareCheckRulesReply);

                            pnrResponse.setBenzyFareRuleMap(benzyFareRulesMap);
                            PNRCancel pnrCancel = new PNRAddMultiElementsh().exitEsx(tstRefNo);
                            serviceHandler.exitESPnr(pnrCancel, amadeusSessionWrapper);
                            serviceHandler.savePNR(amadeusSessionWrapper);
                            if (pnrResponse.getErrorMessage() == null)
                                pnrResponse.setPnrNumber(tstRefNo);
                            Thread.sleep(10000);
                        } catch (Exception e) {
                            throw new Exception();
                        }
                    } else {
                        setLastTicketingDate(pricePNRReply, pnrResponse, travellerMasterInfo);
                        String benzyOfficeId = amadeusSourceOfficeService.getBenzySourceOffice().getOfficeId().toString();
                        if (travellerMasterInfo.getSearchSelectOfficeId().equalsIgnoreCase(benzyOfficeId)) {
                            boolean seamen = travellerMasterInfo.isSeamen();
                            List<HashMap> miniRule = new ArrayList<>();
                            FlightItinerary flightItinerary = travellerMasterInfo.getItinerary();
                            try {
                                AmadeusSessionWrapper benzyamadeusSessionWrapper = serviceHandler.logIn(benzyOfficeId);
                                List<Journey> journeyList = seamen ? flightItinerary.getJourneyList() : flightItinerary.getNonSeamenJourneyList();
                                List<PAXFareDetails> paxFareDetailsList = flightItinerary.getPricingInformation(seamen).getPaxFareDetailsList();

                                List<PAXFareDetails> paxFareDetails = new ArrayList<>();
                                PAXFareDetails paxFareDetails1 = new PAXFareDetails();
                                List<FareJourney> fareJourneyList = new ArrayList<>();
                                paxFareDetails1.setPassengerTypeCode(paxFareDetailsList.get(0).getPassengerTypeCode());
                                FareJourney fareJourney = SerializationUtils.clone(paxFareDetailsList.get(0).getFareJourneyList().get(i));
                                fareJourneyList.add(fareJourney);
                                paxFareDetails1.setFareJourneyList(fareJourneyList);
                                paxFareDetails.add(paxFareDetails1);

                                FareInformativePricingWithoutPNRReply reply = serviceHandler.getFareInfo(journeyList, seamen, 1, 0, 0, paxFareDetails, amadeusSessionWrapper);
                                if (reply.getErrorGroup() != null) {
                                    amadeusLogger.debug("Not able to fetch FareInformativePricingWithoutPNRReply: " + reply.getErrorGroup().getErrorWarningDescription().getFreeText());
                                } else {
                                    String fare = reply.getMainGroup().getPricingGroupLevelGroup().get(0).getFareInfoGroup().getFareAmount().getOtherMonetaryDetails().get(0).getAmount();
                                    BigDecimal totalFare = new BigDecimal(fare);
                                    String currency = reply.getMainGroup().getPricingGroupLevelGroup().get(0).getFareInfoGroup().getFareAmount().getOtherMonetaryDetails().get(0).getCurrency();
                                    FareCheckRulesReply fareCheckRulesReply = serviceHandler.getFareRules(amadeusSessionWrapper);
                                    try {
                                        Map<String, Map> benzyFareRulesMap = AmadeusHelper.getFareCheckRules(fareCheckRulesReply);
                                        pnrResponse.setBenzyFareRuleMap(benzyFareRulesMap);
                                        pnrResponse.getPricingInfo().setPricingOfficeId(benzyOfficeId);
                                    } catch (Exception e) {
                                        amadeusLogger.debug("An exception while fetching the fareCheckRules:" + e.getMessage());
                                    }
                                }

                            } catch (Exception e) {
                                amadeusLogger.debug("An exception while fetching the genericfareRule:" + e.getMessage());
                            }
                        } else {

                            Map<String, String> pnrMap = new HashMap<>();
                            createSplitTST(pnrResponse, amadeusSessionWrapper, 1);
                            if (!pnrResponse.isFlightAvailable()) {
                                return pnrResponse;
                            }

                            gdsPNRReply = serviceHandler.savePNR(amadeusSessionWrapper);

                            String tstRefNo = getPNRNoFromResponse(gdsPNRReply);
                            System.out.println("PNR "+tstRefNo);
                            pnr = tstRefNo;
                            System.out.println(tstRefNo);
                            if(gdsPNRReply.getGeneralErrorInfo().size() !=0) {
                                boolean isWarning = isWarningPNR(gdsPNRReply);
                                if (isWarning) {
                                    System.out.println("ERROR AT END OF TRANSACTION TIME");
                                    Thread.sleep(4000);
                                    gdsPNRReply = serviceHandler.savePNR(amadeusSessionWrapper);
                                    if (isWarningPNR(gdsPNRReply)) {
                                        pnrResponse.setFlightAvailable(false);
                                        ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage(
                                                "connectionTime", ErrorMessage.ErrorType.ERROR, "Amadeus");
                                        pnrResponse.setErrorMessage(errorMessage);
                                        return pnrResponse;
                                    }
                                }

                                if (isSimultaneousPNR(gdsPNRReply)) {
                                    System.out.println("SIMULTANEOUS CHANGES TO PNR - USE WRA/RT TO PRINT OR IGNORE");
                                    logger.debug("SIMULTANEOUS CHANGES TO PNR - USE WRA/RT TO PRINT OR IGNORE");
                                    createSimultaneousPNR(amadeusSessionWrapper,gdsPNRReply, pnr, travellerMasterInfo, pnrResponse, officeId,
                                             pricePNRReply, benzyAmadeusSessionWrapper, i);
                                }
                                System.out.println("general infor error "+gdsPNRReply.getGeneralErrorInfo().size());
                                logger.debug("general infor error "+gdsPNRReply.getGeneralErrorInfo().size());
                            }
                            String segmentKey = createSegmentKey(travellerMasterInfo);
                            pnrMap.put(segmentKey,tstRefNo);
                            pnrResponse.setPnrMap(pnrMap);
                            pnrResponse.setPnrNumber(tstRefNo);
                            isFirstSegmentSell = true;
                            logger.debug("checkFareChangeAndAvailability called for split ..........." + pnrResponse);

                        }

                    }
                } else {
                    pnrResponse.setFlightAvailable(false);
                    return pnrResponse;
                }
                pnrResponses.add(pnrResponse);
            } catch (Exception e) {
                e.printStackTrace();
                isFirstSegmentSell = false;
                pnrResponse.setAddBooking(false);
                pnrResponse.setOriginalPNR("");
                logger.error("Error in checkFareChangeAndAvailability", e);
                ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage(
                        "error", ErrorMessage.ErrorType.ERROR, "Amadeus");
                pnrResponse.setErrorMessage(errorMessage);
                pnrResponses.add(pnrResponse);
            }
            i++;
        }
        return pnrResponse;
    }

    public void createSimultaneousPNR(AmadeusSessionWrapper amadeusSessionWrapper,PNRReply gdsPNRReply, String pnr, TravellerMasterInfo travellerMasterInfo, PNRResponse pnrResponse, String officeId,
                                      FarePricePNRWithBookingClassReply pricePNRReply, AmadeusSessionWrapper benzyAmadeusSessionWrapper, int count) {
        try {
            amadeusSessionWrapper = serviceHandler.logIn(officeId);
            gdsPNRReply = serviceHandler.retrivePNR(pnr, amadeusSessionWrapper);

            checkFlightAvailibility(travellerMasterInfo, pnrResponse, amadeusSessionWrapper);
            System.out.println("check available");
            if (pnrResponse.isFlightAvailable()) {
                System.out.println("Flight available");
                gdsPNRReply = serviceHandler.addTravellerInfoToPNR(travellerMasterInfo, amadeusSessionWrapper);
                PNRReply gdsPNRReplyBenzy = null;
                FarePricePNRWithBookingClassReply pricePNRReplyBenzy = null;
                travellerMasterInfo.getItinerary().getPricingInformation().setSegmentWisePricing(true);
                pricePNRReply = checkPNRPricing(travellerMasterInfo, gdsPNRReply, pricePNRReply, pnrResponse, amadeusSessionWrapper);
                logger.debug(" createSimultaneousPNR gdsPNRReply " + Json.toJson(gdsPNRReply));
                Map<String, String> pnrMap = new HashMap<>();
                createSplitTST(pnrResponse, amadeusSessionWrapper, 1);

                gdsPNRReply = serviceHandler.savePNR(amadeusSessionWrapper);
                if(gdsPNRReply.getGeneralErrorInfo().size() != 0) {
                    pnrResponse.setFlightAvailable(false);
                    ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage(
                            "simultaneousError", ErrorMessage.ErrorType.ERROR, "Amadeus");
                    pnrResponse.setErrorMessage(errorMessage);
                }
                System.out.println("save pnr");
                String tstRefNo = getPNRNoFromResponse(gdsPNRReply);
                String segmentKey = createSegmentKey(travellerMasterInfo);
                pnrMap.put(segmentKey,tstRefNo);
                pnrResponse.setPnrMap(pnrMap);
                pnrResponse.setPnrNumber(tstRefNo);
            }  else {
                setLastTicketingDate(pricePNRReply, pnrResponse, travellerMasterInfo);
                String benzyOfficeId = amadeusSourceOfficeService.getBenzySourceOffice().getOfficeId().toString();
                if (travellerMasterInfo.getSearchSelectOfficeId().equalsIgnoreCase(benzyOfficeId)) {
                    boolean seamen = travellerMasterInfo.isSeamen();
                    List<HashMap> miniRule = new ArrayList<>();
                    FlightItinerary flightItinerary = travellerMasterInfo.getItinerary();
                    try {
                        AmadeusSessionWrapper benzyamadeusSessionWrapper = serviceHandler.logIn(benzyOfficeId);
                        List<Journey> journeyList = seamen ? flightItinerary.getJourneyList() : flightItinerary.getNonSeamenJourneyList();
                        List<PAXFareDetails> paxFareDetailsList = flightItinerary.getPricingInformation(seamen).getPaxFareDetailsList();

                        List<PAXFareDetails> paxFareDetails = new ArrayList<>();
                        PAXFareDetails paxFareDetails1 = new PAXFareDetails();
                        List<FareJourney> fareJourneyList = new ArrayList<>();
                        paxFareDetails1.setPassengerTypeCode(paxFareDetailsList.get(0).getPassengerTypeCode());
                        FareJourney fareJourney = SerializationUtils.clone(paxFareDetailsList.get(0).getFareJourneyList().get(count));
                        fareJourneyList.add(fareJourney);
                        paxFareDetails1.setFareJourneyList(fareJourneyList);
                        paxFareDetails.add(paxFareDetails1);

                        FareInformativePricingWithoutPNRReply reply = serviceHandler.getFareInfo(journeyList, seamen, 1, 0, 0, paxFareDetails, amadeusSessionWrapper);
                        if (reply.getErrorGroup() != null) {
                            amadeusLogger.debug("Not able to fetch FareInformativePricingWithoutPNRReply: " + reply.getErrorGroup().getErrorWarningDescription().getFreeText());
                        } else {
                            String fare = reply.getMainGroup().getPricingGroupLevelGroup().get(0).getFareInfoGroup().getFareAmount().getOtherMonetaryDetails().get(0).getAmount();
                            BigDecimal totalFare = new BigDecimal(fare);
                            String currency = reply.getMainGroup().getPricingGroupLevelGroup().get(0).getFareInfoGroup().getFareAmount().getOtherMonetaryDetails().get(0).getCurrency();
                            FareCheckRulesReply fareCheckRulesReply = serviceHandler.getFareRules(amadeusSessionWrapper);
                            try {
                                Map<String, Map> benzyFareRulesMap = AmadeusHelper.getFareCheckRules(fareCheckRulesReply);
                                pnrResponse.setBenzyFareRuleMap(benzyFareRulesMap);
                                pnrResponse.getPricingInfo().setPricingOfficeId(benzyOfficeId);
                            } catch (Exception e) {
                                amadeusLogger.debug("An exception while fetching the fareCheckRules:" + e.getMessage());
                            }
                        }

                    } catch (Exception e) {
                        amadeusLogger.debug("An exception while fetching the genericfareRule:" + e.getMessage());
                    }
                } else {
                    Map<String, String> pnrMap = new HashMap<>();
                    createSplitTST(pnrResponse, amadeusSessionWrapper, 1);
                    
                    gdsPNRReply = serviceHandler.savePNR(amadeusSessionWrapper);

                    String tstRefNo = getPNRNoFromResponse(gdsPNRReply);
                    String segmentKey = createSegmentKey(travellerMasterInfo);
                    pnrMap.put(segmentKey,tstRefNo);
                    pnrResponse.setPnrMap(pnrMap);
                    pnrResponse.setPnrNumber(tstRefNo);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private boolean isWarningPNR(PNRReply gdsPNRReply) {
        boolean isWarning = false;
        if (gdsPNRReply.getGeneralErrorInfo().size() != 0) {
            for (PNRReply.GeneralErrorInfo generalErrorInfo: gdsPNRReply.getGeneralErrorInfo()) {
                if(generalErrorInfo.getMessageErrorText().getFreetextDetail().getSubjectQualifier().equalsIgnoreCase("3")) {
                    List<String> warningTexts = generalErrorInfo.getMessageErrorText().getText();
                    if(warningTexts.contains("ERROR AT END OF TRANSACTION TIME")) {
                        isWarning = true;
                        return isWarning;
                    }
                }
            }
        }
        return isWarning;
    }

    private boolean isSimultaneousPNR(PNRReply gdsPNRReply) {
        boolean isSimultaneousPNR = false;
        if (gdsPNRReply.getGeneralErrorInfo().size() != 0) {
            for (PNRReply.GeneralErrorInfo generalErrorInfo: gdsPNRReply.getGeneralErrorInfo()) {
                if(generalErrorInfo.getMessageErrorText().getFreetextDetail().getSubjectQualifier().equalsIgnoreCase("3")) {
                    List<String> warningTexts = generalErrorInfo.getMessageErrorText().getText();
                    for (String warningText: warningTexts) {
                        if (warningText.trim().equalsIgnoreCase("SIMULTANEOUS CHANGES TO PNR - USE WRA/RT TO PRINT OR IGNORE")) {
                            isSimultaneousPNR = true;
                            return isSimultaneousPNR;
                        }
                    }
                }
            }
        }
        return isSimultaneousPNR;
    }

    public List<PNRResponse> checkFareChangeAndAvailability(List<TravellerMasterInfo> travellerMasterInfos) {
        List<PNRResponse> pnrResponses = new ArrayList<>();
        FarePricePNRWithBookingClassReply pricePNRReply = null;
        AmadeusSessionWrapper amadeusSessionWrapper = null;
        AmadeusSessionWrapper benzyAmadeusSessionWrapper = null;
        String officeId = null;
        int i=0;
        for (TravellerMasterInfo travellerMasterInfo : travellerMasterInfos) {
            PNRResponse pnrResponse = new PNRResponse();
            try {
                officeId = getSpecificOfficeIdforAirline(travellerMasterInfo.getItinerary());
                boolean isDelIdAirline = isDelIdAirlines(travellerMasterInfo);
                boolean isDelIdSeamen = (isDelIdAirline && travellerMasterInfo.isSeamen()) ? true : false;
                if (officeId == null) {
                    if (travellerMasterInfo.isSeamen()) {
                        officeId = travellerMasterInfo.getItinerary().getSeamanPricingInformation().getPricingOfficeId();
                    } else {
                        if (isDelIdAirline) {
                            officeId = amadeusSourceOfficeService.getDelhiSourceOffice().getOfficeId();
                        } else {
                            officeId = travellerMasterInfo.getItinerary().getPricingInformation().getPricingOfficeId();
                        }
                    }
                }
                if (officeId.equals(amadeusSourceOfficeService.getBenzySourceOffice().getOfficeId()) && !isBATK(travellerMasterInfo)) {
                    officeId = amadeusSourceOfficeService.getDelhiSourceOffice().getOfficeId();
                }
                amadeusSessionWrapper = serviceHandler.logIn(officeId);
                PNRReply gdsPNRReply = null;
                if (travellerMasterInfo.getAdditionalInfo() != null && travellerMasterInfo.getAdditionalInfo().getAddBooking() != null && travellerMasterInfo.getAdditionalInfo().getAddBooking()) {
                    gdsPNRReply = serviceHandler.retrivePNR(travellerMasterInfo.getAdditionalInfo().getOriginalPNR(), amadeusSessionWrapper);
                }
                checkFlightAvailibility(travellerMasterInfo, pnrResponse, amadeusSessionWrapper);
                if (pnrResponse.isFlightAvailable()) {
                    gdsPNRReply = serviceHandler.addTravellerInfoToPNR(travellerMasterInfo, amadeusSessionWrapper);
                    /* Benzy changes */
                    PNRReply gdsPNRReplyBenzy = null;
                    FarePricePNRWithBookingClassReply pricePNRReplyBenzy = null;
                    pricePNRReply = checkPNRPricing(travellerMasterInfo, gdsPNRReply, pricePNRReply, pnrResponse, amadeusSessionWrapper);
                    int numberOfTst = (travellerMasterInfo.isSeamen()) ? 1 : getNumberOfTST(travellerMasterInfo.getTravellersList());

                    if (travellerMasterInfo.getAdditionalInfo() != null && travellerMasterInfo.getAdditionalInfo().getAddBooking() != null && travellerMasterInfo.getAdditionalInfo().getAddBooking()) {
                        String tstRefNo = getPNRNoFromResponse(gdsPNRReply);
                        pnrResponse.setAddBooking(true);
                        pnrResponse.setOriginalPNR(tstRefNo);
                    }
                    logger.debug(" gdsPNRReply " + Json.toJson(gdsPNRReply));
                    if (pnrResponse.isOfficeIdPricingError() || isDelIdSeamen) {
                        gdsPNRReply = serviceHandler.savePNR(amadeusSessionWrapper);
                        String tstRefNo = getPNRNoFromResponse(gdsPNRReply);
                        System.out.println(tstRefNo);
                        logger.debug("checkFareChangeAndAvailability called..........." + pnrResponse);
                        gdsPNRReplyBenzy = serviceHandler.retrivePNR(tstRefNo, amadeusSessionWrapper);
                        //pnrResponse.setPnrNumber(tstRefNo);
                        Boolean error = Boolean.FALSE;
                        gdsPNRReply = serviceHandler.savePNRES(amadeusSessionWrapper, amadeusSourceOfficeService.getBenzySourceOffice().getOfficeId());
                        if (gdsPNRReply.getGeneralErrorInfo().size() > 0) {
                            Thread.sleep(20000);
                            error = Boolean.TRUE;
                        }
                        if (!error) {
                            benzyAmadeusSessionWrapper = serviceHandler.logIn(amadeusSourceOfficeService.getBenzySourceOffice().getOfficeId());
                            PNRReply pnrReply = serviceHandler.retrivePNR(tstRefNo, benzyAmadeusSessionWrapper);
                            pricePNRReplyBenzy = checkPNRPricing(travellerMasterInfo, gdsPNRReplyBenzy, pricePNRReplyBenzy, pnrResponse, benzyAmadeusSessionWrapper);
                            createTST(pnrResponse, benzyAmadeusSessionWrapper, numberOfTst);
                            setLastTicketingDate(pricePNRReplyBenzy, pnrResponse, travellerMasterInfo);
                            gdsPNRReplyBenzy = serviceHandler.savePNR(benzyAmadeusSessionWrapper);

                            if (gdsPNRReplyBenzy.getGeneralErrorInfo().size() > 0) {
                                List<PNRReply.GeneralErrorInfo> generalErrorInfos = gdsPNRReplyBenzy.getGeneralErrorInfo();
                                for (PNRReply.GeneralErrorInfo generalErrorInfo : generalErrorInfos) {
                                    String textMsg = generalErrorInfo.getMessageErrorText().getText().get(0).trim();
                                    if (textMsg.equals("SIMULTANEOUS CHANGES TO PNR - USE WRA/RT TO PRINT OR IGNORE")) {
                                        error = Boolean.TRUE;
                                    }
                                }
                            }
                        }
                        if (error) {
                            PNRCancel pnrCancel = new PNRAddMultiElementsh().exitEsx(tstRefNo);
                            serviceHandler.exitESPnr(pnrCancel, amadeusSessionWrapper);
                            serviceHandler.logOut(benzyAmadeusSessionWrapper);
                            gdsPNRReply = serviceHandler.savePNRES(amadeusSessionWrapper, amadeusSourceOfficeService.getBenzySourceOffice().getOfficeId());
                            benzyAmadeusSessionWrapper = serviceHandler.logIn(amadeusSourceOfficeService.getBenzySourceOffice().getOfficeId());
                            try {
                                serviceHandler.retrivePNR(tstRefNo, benzyAmadeusSessionWrapper);
                            } catch (Exception ex) {
                                ex.printStackTrace();
                                if (ex != null && ex.getMessage() != null && ex.getMessage().toString().contains("IGNORE")) {
                                    gdsPNRReply = serviceHandler.ignoreAndRetrievePNR(benzyAmadeusSessionWrapper);
                                }
                            }
                            //serviceHandler.retrivePNR(tstRefNo,benzyAmadeusSessionWrapper);
                            pricePNRReplyBenzy = checkPNRPricing(travellerMasterInfo, gdsPNRReplyBenzy, pricePNRReplyBenzy, pnrResponse, benzyAmadeusSessionWrapper);
                            createTST(pnrResponse, benzyAmadeusSessionWrapper, numberOfTst);
                            setLastTicketingDate(pricePNRReplyBenzy, pnrResponse, travellerMasterInfo);
                            gdsPNRReplyBenzy = serviceHandler.savePNR(benzyAmadeusSessionWrapper);
                        }
                        if (pnrResponse.getPricingInfo() != null)
                            pnrResponse.getPricingInfo().setPricingOfficeId(amadeusSourceOfficeService.getBenzySourceOffice().getOfficeId().toString());
                        FareCheckRulesReply fareCheckRulesReply = serviceHandler.getFareRules(benzyAmadeusSessionWrapper);
                        try {
                            Map<String, Map> benzyFareRulesMap = null;
                            if (fareCheckRulesReply.getErrorInfo() == null)
                                benzyFareRulesMap = AmadeusHelper.getFareCheckRules(fareCheckRulesReply);

                            pnrResponse.setBenzyFareRuleMap(benzyFareRulesMap);
                            PNRCancel pnrCancel = new PNRAddMultiElementsh().exitEsx(tstRefNo);
                            serviceHandler.exitESPnr(pnrCancel, amadeusSessionWrapper);
                            serviceHandler.savePNR(amadeusSessionWrapper);
                            if (pnrResponse.getErrorMessage() == null)
                                pnrResponse.setPnrNumber(tstRefNo);
                            Thread.sleep(10000);
                        } catch (Exception e) {
                            throw new Exception();
                        }
                    } else {
                        setLastTicketingDate(pricePNRReply, pnrResponse, travellerMasterInfo);
                        String benzyOfficeId = amadeusSourceOfficeService.getBenzySourceOffice().getOfficeId().toString();
                        if (travellerMasterInfo.getSearchSelectOfficeId().equalsIgnoreCase(benzyOfficeId)) {
                            boolean seamen = travellerMasterInfo.isSeamen();
                            List<HashMap> miniRule = new ArrayList<>();
                            FlightItinerary flightItinerary = travellerMasterInfo.getItinerary();
                            try {
                                AmadeusSessionWrapper benzyamadeusSessionWrapper = serviceHandler.logIn(benzyOfficeId);
                                List<Journey> journeyList = seamen ? flightItinerary.getJourneyList() : flightItinerary.getNonSeamenJourneyList();
                                List<PAXFareDetails> paxFareDetailsList = flightItinerary.getPricingInformation(seamen).getPaxFareDetailsList();

                                List<PAXFareDetails> paxFareDetails = new ArrayList<>();
                                PAXFareDetails paxFareDetails1 = new PAXFareDetails();
                                List<FareJourney> fareJourneyList = new ArrayList<>();
                                paxFareDetails1.setPassengerTypeCode(paxFareDetailsList.get(0).getPassengerTypeCode());
                                FareJourney fareJourney = SerializationUtils.clone(paxFareDetailsList.get(0).getFareJourneyList().get(i));
                                fareJourneyList.add(fareJourney);
                                paxFareDetails1.setFareJourneyList(fareJourneyList);
                                paxFareDetails.add(paxFareDetails1);

                                FareInformativePricingWithoutPNRReply reply = serviceHandler.getFareInfo(journeyList, seamen, 1, 0, 0, paxFareDetails, amadeusSessionWrapper);
                                if (reply.getErrorGroup() != null) {
                                    amadeusLogger.debug("Not able to fetch FareInformativePricingWithoutPNRReply: " + reply.getErrorGroup().getErrorWarningDescription().getFreeText());
                                } else {
                                    String fare = reply.getMainGroup().getPricingGroupLevelGroup().get(0).getFareInfoGroup().getFareAmount().getOtherMonetaryDetails().get(0).getAmount();
                                    BigDecimal totalFare = new BigDecimal(fare);
                                    String currency = reply.getMainGroup().getPricingGroupLevelGroup().get(0).getFareInfoGroup().getFareAmount().getOtherMonetaryDetails().get(0).getCurrency();
                                    FareCheckRulesReply fareCheckRulesReply = serviceHandler.getFareRules(amadeusSessionWrapper);
                                    try {
                                        Map<String, Map> benzyFareRulesMap = AmadeusHelper.getFareCheckRules(fareCheckRulesReply);
                                        pnrResponse.setBenzyFareRuleMap(benzyFareRulesMap);
                                        pnrResponse.getPricingInfo().setPricingOfficeId(benzyOfficeId);
                                    } catch (Exception e) {
                                        amadeusLogger.debug("An exception while fetching the fareCheckRules:" + e.getMessage());
                                    }
                                }

                            } catch (Exception e) {
                                amadeusLogger.debug("An exception while fetching the genericfareRule:" + e.getMessage());
                            }
                        } else {
                            Map<String, String> pnrMap = new HashMap<>();
                            createTST(pnrResponse, amadeusSessionWrapper, numberOfTst);
                            gdsPNRReply = serviceHandler.savePNR(amadeusSessionWrapper);
                            String tstRefNo = getPNRNoFromResponse(gdsPNRReply);
                            System.out.println(tstRefNo);
                            String segmentKey = createSegmentKey(travellerMasterInfo);
                            pnrMap.put(segmentKey,tstRefNo);
                            pnrResponse.setPnrMap(pnrMap);
                            logger.debug("checkFareChangeAndAvailability called for split ..........." + pnrResponse);
                        }

                    }
                } else {
                    pnrResponse.setFlightAvailable(false);
                }
                pnrResponses.add(pnrResponse);
            } catch (Exception e) {
                e.printStackTrace();
                pnrResponse.setAddBooking(false);
                pnrResponse.setOriginalPNR("");
                logger.error("Error in checkFareChangeAndAvailability", e);
                ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage(
                        "error", ErrorMessage.ErrorType.ERROR, "Amadeus");
                pnrResponse.setErrorMessage(errorMessage);
                pnrResponses.add(pnrResponse);
            }
            i++;
        }
        return pnrResponses;
    }

    private String createSegmentKey(TravellerMasterInfo travellerMasterInfo) {
        StringBuilder stringBuilder = new StringBuilder();
        int segmentSize = travellerMasterInfo.getItinerary().getJourneyList().get(0).getAirSegmentList().size();
        String fromLocation = travellerMasterInfo.getItinerary().getJourneyList().get(0).getAirSegmentList().get(0).getFromLocation();
        String toLocation = travellerMasterInfo.getItinerary().getJourneyList().get(0).getAirSegmentList().get(segmentSize-1).getToLocation();
        stringBuilder.append(fromLocation);
        stringBuilder.append(toLocation);
        return stringBuilder.toString();
    }
    public void checkFlightAvailibility(TravellerMasterInfo travellerMasterInfo, PNRResponse pnrResponse, AmadeusSessionWrapper amadeusSessionWrapper) {
        logger.debug("checkFlightAvailibility called ........");
        System.out.println("4");
        AirSellFromRecommendationReply sellFromRecommendation = serviceHandler
                .checkFlightAvailability(travellerMasterInfo, amadeusSessionWrapper);
        System.out.println("5");
        if (sellFromRecommendation.getErrorAtMessageLevel() != null
                && sellFromRecommendation.getErrorAtMessageLevel().size() > 0
                && (sellFromRecommendation.getItineraryDetails() == null)) {
            ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage(
                    "error", ErrorMessage.ErrorType.ERROR, "Amadeus");
            pnrResponse.setErrorMessage(errorMessage);
        }
        boolean flightAvailable = AmadeusBookingHelper
                .validateFlightAvailability(sellFromRecommendation,
                        AmadeusConstants.AMADEUS_FLIGHT_AVAILIBILITY_CODE);
        pnrResponse.setSessionIdRef(amadeusSessionManager.storeActiveSession(amadeusSessionWrapper, null));
        pnrResponse.setFlightAvailable(flightAvailable);
    }

    private boolean isBATK(TravellerMasterInfo travellerMasterInfo) {
        String airlineStr = play.Play.application().configuration().getString("joc.special.airlines");
        List<String> specialAirline = new ArrayList<String>(Arrays.asList(airlineStr.split(",")));
        for (Journey journey : travellerMasterInfo.getItinerary().getJourneyList()) {
            for (AirSegmentInformation airSegmentInformation : journey.getAirSegmentList()) {
                if (specialAirline.contains(airSegmentInformation.getCarrierCode())) {
                    return true;
                }
            }
        }
        return false;
    }

    private String getSpecificOfficeIdforAirline(FlightItinerary itinerary) {
        Configuration config = Play.application().configuration();
        Configuration airlineBookingOfficeConfig = config.getConfig("amadeus.AIRLINE_BOOKING_OFFICE");
        for (Journey journey : itinerary.getJourneyList()) {
            for (AirSegmentInformation segmentInfo : journey.getAirSegmentList()) {
                //String officeId = config.getString("amadeus.AIRLINE_BOOKING_OFFICE."+carcode);
                String officeId = airlineBookingOfficeConfig.getString(segmentInfo.getCarrierCode());
                if (officeId != null) {
                    return officeId;
                }
            }
        }
        return null;
    }

    private static boolean isDelIdAirlines(TravellerMasterInfo travellerMasterInfo) {
        String airlineStr = play.Play.application().configuration().getString("joc.DELHI_ID.airlines");
        List<String> specialAirline = new ArrayList<String>(Arrays.asList(airlineStr.split(",")));
        List<Journey> journeys = travellerMasterInfo.getItinerary().getJourneyList();
        if (journeys.size() > 0) {
            for (Journey journey : journeys) {
                for (AirSegmentInformation airSegmentInformation : journey.getAirSegmentList()) {
                    if (specialAirline.contains(airSegmentInformation.getCarrierCode())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public FarePricePNRWithBookingClassReply checkPNRPricing(TravellerMasterInfo travellerMasterInfo,
                                                             PNRReply gdsPNRReply, FarePricePNRWithBookingClassReply pricePNRReply, PNRResponse pnrResponse, AmadeusSessionWrapper amadeusSessionWrapper) {
        String carrierCode = "";
        List<Journey> journeys;
        List<AirSegmentInformation> airSegmentList = new ArrayList<>();

        if (travellerMasterInfo.isSeamen()) {
            int size = travellerMasterInfo.getItinerary().getJourneyList().get(0).getAirSegmentList().size();
            carrierCode = travellerMasterInfo.getItinerary().getJourneyList()
                    .get(0).getAirSegmentList().get(size - 1).getValidatingCarrierCode();
            journeys = travellerMasterInfo.getItinerary().getJourneyList();
        } else {
            int size = travellerMasterInfo.getItinerary().getNonSeamenJourneyList().get(0).getAirSegmentList().size();
            carrierCode = travellerMasterInfo.getItinerary()
                    .getNonSeamenJourneyList().get(0).getAirSegmentList()
                    .get(size - 1).getValidatingCarrierCode();
            journeys = travellerMasterInfo.getItinerary().getNonSeamenJourneyList();
        }

        for (Journey journey : journeys) {
            for (AirSegmentInformation airSegmentInformation : journey.getAirSegmentList()) {
                airSegmentList.add(airSegmentInformation);
            }
        }
        boolean isDomestic = AmadeusHelper.checkAirportCountry("India", journeys);
        boolean isSegmentWisePricing = false;
        if (travellerMasterInfo.getItinerary().getPricingInformation() != null) {
            isSegmentWisePricing = travellerMasterInfo.getItinerary().getPricingInformation().isSegmentWisePricing();
        }
        boolean isAddBooking = false;
        if(travellerMasterInfo.getAdditionalInfo()!=null && travellerMasterInfo.getAdditionalInfo().getAddBooking()!=null && travellerMasterInfo.getAdditionalInfo().getAddBooking()) {
            isAddBooking = true;
        }
        pricePNRReply = serviceHandler.pricePNR(carrierCode, gdsPNRReply, travellerMasterInfo.isSeamen(), isDomestic, travellerMasterInfo.getItinerary(), airSegmentList, isSegmentWisePricing, amadeusSessionWrapper,isAddBooking);
        if (pricePNRReply.getApplicationError() != null) {
            if (pricePNRReply.getApplicationError().getErrorOrWarningCodeDetails().getErrorDetails().getErrorCode().equalsIgnoreCase("0")
                    && pricePNRReply.getApplicationError().getErrorOrWarningCodeDetails().getErrorDetails().getErrorCategory().equalsIgnoreCase("EC")) {
                pnrResponse.setOfficeIdPricingError(true);
            } else {
                pnrResponse.setFlightAvailable(false);
            }
            return pricePNRReply;
        }
        AmadeusBookingHelper.checkFare(pricePNRReply, pnrResponse, travellerMasterInfo);
        readBaggageInfoFromPnrReply(gdsPNRReply, pricePNRReply, pnrResponse);
//        AmadeusBookingHelper.setTaxBreakup(pnrResponse, travellerMasterInfo, pricePNRReply);
        return pricePNRReply;
    }

    private void readBaggageInfoFromPnrReply(PNRReply gdsPNRReply, FarePricePNRWithBookingClassReply pnrReply, PNRResponse pnrResponse) {
        amadeusLogger.debug("Read Baggage Info........");

        Map<String, Object> airSegmentRefMap = airSegmentRefMap(gdsPNRReply);
        Map<String, String> passengerType = passengerTypeMap(gdsPNRReply);

        HashMap<String, String> map = new HashMap<>();
        List<FarePricePNRWithBookingClassReply.FareList> fareList = pnrReply.getFareList();
        try {
            for (FarePricePNRWithBookingClassReply.FareList fare : fareList) {
                if (!fare.getPaxSegReference().getRefDetails().get(0).getRefQualifier().equals("PI")
                        && passengerType.get("P" + fare.getPaxSegReference().getRefDetails().get(0).getRefNumber()).equals(PassengerTypeCode.ADT.toString())) {
                    for (FarePricePNRWithBookingClassReply.FareList.SegmentInformation segmentInformation : fare.getSegmentInformation()) {
                        String temp = (segmentInformation.getSegmentReference() != null) ?
                                ("S" + segmentInformation.getSegmentReference().getRefDetails().get(0).getRefNumber()) : null;
                        if (temp != null && airSegmentRefMap.get(temp) != null && !map.containsKey(temp)) {
                            String key = airSegmentRefMap.get(temp).toString();
                            String baggage = null;
                            if (segmentInformation.getBagAllowanceInformation().getBagAllowanceDetails().getBaggageQuantity() == null) {
                                baggage = segmentInformation.getBagAllowanceInformation().getBagAllowanceDetails().getBaggageWeight()
                                        + " " + baggageCodes.get(segmentInformation.getBagAllowanceInformation().getBagAllowanceDetails().getMeasureUnit());
                            } else {
                                baggage = segmentInformation.getBagAllowanceInformation().getBagAllowanceDetails().getBaggageQuantity()
                                        + " " + baggageCodes.get(segmentInformation.getBagAllowanceInformation().getBagAllowanceDetails().getBaggageType());
                            }
                            map.put(key, baggage);
                        }
                    }
                }
            }
        } catch (Exception e) {
            amadeusLogger.error("Error in readBaggageInfo ", e);
            e.printStackTrace();
        }
        pnrResponse.setSegmentBaggageMap(map);
    }

    private Map<String, Object> airSegmentRefMap(PNRReply gdsPNRReply) {
        logger.debug(" AirSegment Reference Map creation. ");
        Map<String, Object> airSegmentRefMap = new HashMap<>();
        for (PNRReply.OriginDestinationDetails originDestination : gdsPNRReply.getOriginDestinationDetails()) {
            for (PNRReply.OriginDestinationDetails.ItineraryInfo itineraryInfo : originDestination.getItineraryInfo()) {
                String segType = itineraryInfo.getElementManagementItinerary().getSegmentName();
                if (segType.equalsIgnoreCase("AIR")) {
                    String segmentRef = "S" + itineraryInfo.getElementManagementItinerary().getReference().getNumber();
                    String segments = itineraryInfo.getTravelProduct().getBoardpointDetail().getCityCode() + itineraryInfo.getTravelProduct().getOffpointDetail().getCityCode();
                    airSegmentRefMap.put(segmentRef, segments);
                }
            }
        }
        return airSegmentRefMap;
    }

    private Map<String, String> passengerTypeMap(PNRReply gdsPNRReply) {
        logger.debug(" Passenger Type Map creation. ");
        Map<String, String> passengerType = new HashMap<>();
        for (PNRReply.TravellerInfo travellerInfo : gdsPNRReply.getTravellerInfo()) {
            String key = "P" + travellerInfo.getElementManagementPassenger().getReference().getNumber();
            PNRReply.TravellerInfo.PassengerData paxData = travellerInfo.getPassengerData().get(0);
            String paxType = paxData.getTravellerInformation().getPassenger().get(0).getType();
            String infantIndicator = paxData.getTravellerInformation().getPassenger().get(0).getInfantIndicator();
            if ("chd".equalsIgnoreCase(paxType) || "ch".equalsIgnoreCase(paxType)) {
                passengerType.put(key, "CHD");
            } else if ("inf".equalsIgnoreCase(paxType) || "in".equalsIgnoreCase(paxType)) {
                passengerType.put(key, "INF");
            } else {
                passengerType.put(key, "ADT");
            }

            if (infantIndicator != null && "1".equalsIgnoreCase(infantIndicator)) {
                passengerType.put("PI" + travellerInfo.getElementManagementPassenger().getReference().getNumber(), "INF");
            }
        }
        return passengerType;
    }

    public static int getNumberOfTST(List<Traveller> travellerList) {

        int adultCount = 0, childCount = 0, infantCount = 0;
        int totalCount = 0;
        for (Traveller traveller : travellerList) {
            PassengerTypeCode passengerTypeCode = DateUtility.getPassengerTypeFromDOB(traveller.getPassportDetails().getDateOfBirth());
            if (passengerTypeCode.name().equals(PassengerTypeCode.ADT.name())) {
                adultCount = 1;
            } else if (passengerTypeCode.name().equals(PassengerTypeCode.CHD.name())) {
                childCount = 1;
            } else {
                infantCount = 1;
            }

            totalCount = adultCount + childCount + infantCount;

        }
        return totalCount;
    }

    public String getPNRNoFromResponse(PNRReply gdsPNRReply) {
        String pnrNumber = null;
        for (PNRReply.PnrHeader pnrHeader : gdsPNRReply.getPnrHeader()) {
            pnrNumber = pnrHeader.getReservationInfo().getReservation()
                    .getControlNumber();
            if (Objects.nonNull(pnrHeader))
                break;
        }

        return pnrNumber;
    }

    private void createTST(PNRResponse pnrResponse, AmadeusSessionWrapper amadeusSessionWrapper, int numberOfTst) {
        TicketCreateTSTFromPricingReply ticketCreateTSTFromPricingReply = serviceHandler.createTST(numberOfTst, amadeusSessionWrapper);
        logger.info("createTST Called : {}", ticketCreateTSTFromPricingReply.getTstList().size());
        if (ticketCreateTSTFromPricingReply.getApplicationError() != null) {
            logger.error("Error in createTST: {}", ticketCreateTSTFromPricingReply.getApplicationError().toString());
            String errorCode = ticketCreateTSTFromPricingReply
                    .getApplicationError()
                    .getApplicationErrorInfo()
                    .getApplicationErrorDetail()
                    .getApplicationErrorCode();
            ErrorMessage errorMessage = new ErrorMessage();
            //ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage("error",ErrorMessage.ErrorType.ERROR, "Amadeus");
            String errorMsg = ticketCreateTSTFromPricingReply.getApplicationError().getErrorText().getErrorFreeText();
            errorMessage.setMessage(errorMsg);
            pnrResponse.setErrorMessage(errorMessage);
            pnrResponse.setFlightAvailable(false);
        }
    }

    private void createSplitTST(PNRResponse pnrResponse, AmadeusSessionWrapper amadeusSessionWrapper, int numberOfTst) {
        TicketCreateTSTFromPricingReply ticketCreateTSTFromPricingReply = serviceHandler.createSplitTST(numberOfTst, amadeusSessionWrapper);
        logger.info("createTST Called : {}", ticketCreateTSTFromPricingReply.getTstList().size());
        if (ticketCreateTSTFromPricingReply.getApplicationError() != null) {
            logger.error("Error in createTST: {}", ticketCreateTSTFromPricingReply.getApplicationError().toString());
            String errorCode = ticketCreateTSTFromPricingReply
                    .getApplicationError()
                    .getApplicationErrorInfo()
                    .getApplicationErrorDetail()
                    .getApplicationErrorCode();
            ErrorMessage errorMessage = new ErrorMessage();
            //ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage("error",ErrorMessage.ErrorType.ERROR, "Amadeus");
            String errorMsg = ticketCreateTSTFromPricingReply.getApplicationError().getErrorText().getErrorFreeText();
            errorMessage.setMessage(errorMsg);
            pnrResponse.setErrorMessage(errorMessage);
            pnrResponse.setFlightAvailable(false);
        }
    }

    public void setLastTicketingDate(FarePricePNRWithBookingClassReply pricePNRReply, PNRResponse pnrResponse, TravellerMasterInfo travellerMasterInfo) {
        Date lastTicketingDate = null;
        if (pricePNRReply.getFareList() != null && pricePNRReply.getFareList().size() > 0 && pricePNRReply.getFareList().get(0) != null && pricePNRReply.getFareList().get(0).getLastTktDate() != null) {
            StructuredDateTimeType dateTime = pricePNRReply
                    .getFareList().get(0).getLastTktDate().getDateTime();
            String day = ((dateTime.getDay().toString().length() == 1) ? "0"
                    + dateTime.getDay() : dateTime.getDay().toString());
            String month = ((dateTime.getMonth().toString().length() == 1) ? "0"
                    + dateTime.getMonth() : dateTime.getMonth().toString());
            String year = dateTime.getYear().toString();
            SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy");

            try {
                lastTicketingDate = sdf.parse(day + month + year);
            } catch (ParseException e) {
                logger.debug("error in setLastTicketingDate", e);
                e.printStackTrace();
            }
        }
    }
}
