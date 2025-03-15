package services;

import com.amadeus.xml.farqnr_07_1_1a.FareCheckRulesReply;
import com.amadeus.xml.itares_05_2_ia.AirSellFromRecommendationReply;
import com.amadeus.xml.pnracc_11_3_1a.PNRReply;
import com.amadeus.xml.pnracc_11_3_1a.PNRReply.DataElementsMaster.DataElementsIndiv;
import com.amadeus.xml.pnracc_11_3_1a.PNRReply.OriginDestinationDetails.ItineraryInfo;
import com.amadeus.xml.pnracc_11_3_1a.PNRReply.TravellerInfo;
import com.amadeus.xml.pnracc_11_3_1a.PNRReply.TravellerInfo.PassengerData;
import com.amadeus.xml.pnracc_11_3_1a.ReferencingDetailsType127526C;
import com.amadeus.xml.pnracc_11_3_1a.TravellerDetailsTypeI;
import com.amadeus.xml.pnrspl_11_3_1a.*;
import com.amadeus.xml.pnrxcl_11_3_1a.PNRCancel;
import com.amadeus.xml.pnrxcl_14_1_1a.ElementIdentificationType;
import com.amadeus.xml.tatreq_20_1_1a.TicketProcessEDoc;
import com.amadeus.xml.tautcr_04_1_1a.TicketCreateTSTFromPricingReply;
import com.amadeus.xml.tipnrr_12_4_1a.FareInformativePricingWithoutPNRReply;
import com.amadeus.xml.tmrxrr_18_1_1a.*;
import com.amadeus.xml.tpcbrr_12_4_1a.FarePricePNRWithBookingClassReply;
import com.amadeus.xml.tpcbrr_12_4_1a.FarePricePNRWithBookingClassReply.FareList;
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
import com.compassites.model.traveller.PersonalDetails;
import com.compassites.model.traveller.Preferences;
import com.compassites.model.traveller.Traveller;
import com.compassites.model.traveller.TravellerMasterInfo;
import com.compassites.model.amadeus.AmadeusPaxInformation;
import com.fasterxml.jackson.databind.JsonNode;
//import com.sun.org.apache.xpath.internal.operations.Bool;
import dto.FareCheckRulesResponse;
import models.AmadeusSessionWrapper;
import models.CartAirSegmentDTO;
import models.MiniRule;
import org.apache.commons.lang3.text.WordUtils;
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
import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.compassites.constants.StaticConstatnts.*;

/**
 * Created by Yaseen
 */
@Service
public class AmadeusBookingServiceImpl implements BookingService {

	static org.slf4j.Logger amadeusLogger = LoggerFactory.getLogger("amadeus");

    static org.slf4j.Logger logger = LoggerFactory.getLogger("gds");

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

	@Autowired
	private AmadeusIssuanceServiceImpl amadeusIssuanceService;

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

    public AmadeusBookingServiceImpl() {
    }

    @Autowired
    public AmadeusBookingServiceImpl(AmadeusSessionManager amadeusSessionManager) {
        this.amadeusSessionManager = amadeusSessionManager;
    }


	public RedisTemplate getRedisTemplate() {
		return redisTemplate;
	}

	public void setRedisTemplate(RedisTemplate redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	/*
            Generate PNR follows the Amadeus booking flow please refer tht flow provided by Amadeus for flow of this method
        */
	@Override
	public PNRResponse generatePNR(TravellerMasterInfo travellerMasterInfo) {
		logger.debug("generatePNR called ........");
		PNRResponse pnrResponse = new PNRResponse();
		PNRReply gdsPNRReply = null;
		FarePricePNRWithBookingClassReply pricePNRReply = null;
		AmadeusSessionWrapper amadeusSessionWrapper = null;
		String tstRefNo = "";
		try {
			amadeusSessionWrapper = amadeusSessionManager.getActiveSessionByRef(travellerMasterInfo.getSessionIdRef());
			logger.debug("generatePNR called........"+ Json.stringify(Json.toJson(amadeusSessionWrapper)));
			int numberOfTst = (travellerMasterInfo.isSeamen()) ? 1	: getNumberOfTST(travellerMasterInfo.getTravellersList());
//			gdsPNRReply = serviceHandler.retrivePNR(tstRefNo,amadeusSessionWrapper);
//			tstRefNo = getPNRNoFromResponse(gdsPNRReply);
			if( travellerMasterInfo.getGdsPNR()== null && !travellerMasterInfo.isChangedPriceHigh()) {
				createTST(pnrResponse, amadeusSessionWrapper, numberOfTst);
				gdsPNRReply = serviceHandler.savePNR(amadeusSessionWrapper);
				tstRefNo = getPNRNoFromResponse(gdsPNRReply);
				System.out.println(tstRefNo);
				Thread.sleep(10000);
			}else{
				tstRefNo = travellerMasterInfo.getGdsPNR();
			}
			boolean isAddBooking = false;
			if(travellerMasterInfo.getAdditionalInfo()!=null && travellerMasterInfo.getAdditionalInfo().getAddBooking()!=null && travellerMasterInfo.getAdditionalInfo().getAddBooking()) {
				pnrResponse.setAddBooking(true);
				pnrResponse.setOriginalPNR(tstRefNo);
				isAddBooking = true;
			}
			try {
				gdsPNRReply = serviceHandler.retrivePNR(tstRefNo, amadeusSessionWrapper);
				if(isAddBooking) {
					TicketDisplayTSTReply ticketDisplayTSTReply = serviceHandler.ticketDisplayTST(amadeusSessionWrapper);
					createSegmentPricing(gdsPNRReply, pnrResponse, ticketDisplayTSTReply);
				}
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
				for(ItineraryInfo itineraryInfo : originDestinationDetails.getItineraryInfo()){
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
				amadeusSessionManager.removeActiveSession(amadeusSessionWrapper.getmSession().value);
				serviceHandler.logOut(amadeusSessionWrapper);
			}
		}
		logger.info("generatePNR :"+ Json.stringify(Json.toJson(pnrResponse)));
		return pnrResponse;
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
			PassengerTax passengerTax = AmadeusAddBookingHelper.getTaxDetailsFromTST(fare.getTaxInformation(), paxType, paxCount);
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

			TSTPrice tstPrice = AmadeusAddBookingHelper.getTSTPrice(fare, paxTotalFare, baseFare,paxType, passengerTax);
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

	@Override
	public SplitPNRResponse splitPNR(IssuanceRequest issuanceRequest, String type) {
		logger.debug("split PNR called " + Json.toJson(issuanceRequest));
		//ServiceHandler serviceHandler = null;
		PNRResponse pnrResponse = new PNRResponse();
		SplitPNRResponse splitPNRResponse = new SplitPNRResponse();
		JsonNode jsonNode = null;
		CancelPNRResponse cancelPNRResponse = new CancelPNRResponse();
		PricingInformation pricingInfo = null;
		List<Journey> journeyList = null;
		AmadeusCancelServiceImpl amadeusCancelService = new AmadeusCancelServiceImpl();
		com.amadeus.xml.pnrspl_11_3_1a.PNRSplit pnrSplit = new com.amadeus.xml.pnrspl_11_3_1a.PNRSplit();
		ReservationControlInformationType reservationInfo = new ReservationControlInformationType();
		ReservationControlInformationDetailsTypeI reservationControlInformationDetailsTypeI = new ReservationControlInformationDetailsTypeI();
		String gdsPNR = issuanceRequest.getGdsPNR();
		PNRReply gdsPNRReply = null;
		AmadeusSessionWrapper amadeusSessionWrapper = null;
		try {
			amadeusSessionWrapper = serviceHandler.logIn();
			gdsPNRReply = serviceHandler.retrivePNR(gdsPNR, amadeusSessionWrapper);

			String paxType = gdsPNRReply.getTravellerInfo().get(0).getPassengerData().get(0).getTravellerInformation().getPassenger().get(0).getType();
			boolean isSeamen = false;
			if ("sea".equalsIgnoreCase(paxType) || "sc".equalsIgnoreCase(paxType))
				isSeamen = true;

			Map<String, Object> travellerSegMap = createTravellerSegmentMap(gdsPNRReply);
			String tstRefNo = getPNRNoFromResponse(gdsPNRReply);
			reservationControlInformationDetailsTypeI.setControlNumber(tstRefNo);
			reservationInfo.setReservation(reservationControlInformationDetailsTypeI);
			pnrSplit.setReservationInfo(reservationInfo);
			SplitPNRType splitPNRType = createSplitPNRType(issuanceRequest, travellerSegMap);
			pnrSplit.setSplitDetails(splitPNRType);
			PNRReply pnrSplitReply = serviceHandler.splitPNR(pnrSplit, amadeusSessionWrapper);
			serviceHandler.saveChildPNR("14", amadeusSessionWrapper);
			PNRReply childPNRReply = serviceHandler.saveChildPNR("11", amadeusSessionWrapper);
			String childPNR = createChildPNR(childPNRReply);
			serviceHandler.saveChildPNR("20", amadeusSessionWrapper);
			Thread.sleep(4000);
			PNRReply childRetrive = serviceHandler.retrivePNR(childPNR, amadeusSessionWrapper);

			journeyList = AmadeusBookingHelper.getJourneyListFromPNRResponse(childRetrive, redisTemplate);

			TicketDisplayTSTReply ticketDisplayTSTReply = serviceHandler.ticketDisplayTST(amadeusSessionWrapper);

			if (ticketDisplayTSTReply.getFareList() == null || ticketDisplayTSTReply.getFareList().isEmpty()) {
				ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage("priceNotAvailable", ErrorMessage.ErrorType.ERROR, PROVIDERS.AMADEUS.toString());
				pnrResponse.setErrorMessage(errorMessage);
			}

			pricingInfo = AmadeusBookingHelper.getPricingInfoFromTST(childRetrive, ticketDisplayTSTReply, isSeamen, journeyList);
			pricingInfo.setSegmentWisePricing(false);
			pnrResponse.setPricingInfo(pricingInfo);
			pnrResponse.setPnrNumber(childPNR);
			pnrResponse.setAmadeusPaxReference(createAmadeusPaxRefInfo(gdsPNRReply));
			Date lastPNRAddMultiElements = new Date();
			PNRReply childGdsReply = readChildAirlinePNR(serviceHandler, childRetrive, lastPNRAddMultiElements, pnrResponse, amadeusSessionWrapper);
			if (pnrResponse.getAirlinePNR() != null) {
				try {
					if (issuanceRequest.getCtSegmentDtoList() != null && !issuanceRequest.getCtSegmentDtoList().isEmpty()) {


//						Map<BigInteger, String> segmentMap = new HashMap<>();
//						for (CartAirSegmentDTO cartAirSegment : issuanceRequest.getCtSegmentDtoList()) {
//							for (PNRReply.OriginDestinationDetails originDestination : gdsPNRReply.getOriginDestinationDetails()) {
//								for (PNRReply.OriginDestinationDetails.ItineraryInfo itineraryInfo : originDestination.getItineraryInfo()) {
//									ElementIdentificationType elementIdentificationType = new ElementIdentificationType();
//									String segType = itineraryInfo.getElementManagementItinerary().getSegmentName();
//									if (segType.equalsIgnoreCase("AIR")) {
//										BigInteger segmentRef = itineraryInfo.getElementManagementItinerary().getReference().getNumber();
//										String segQualifier = itineraryInfo.getElementManagementItinerary().getReference().getQualifier();
//										if (segmentRef.equals(BigInteger.valueOf(cartAirSegment.getSequence()))) {
//											String boardCityCode = itineraryInfo.getTravelProduct().getBoardpointDetail().getCityCode();
//											String offPointCityCode = itineraryInfo.getTravelProduct().getOffpointDetail().getCityCode();
//											if (boardCityCode.equalsIgnoreCase(cartAirSegment.getFromLocation()) && offPointCityCode.equalsIgnoreCase(cartAirSegment.getToLocation())) {
//												segmentMap.put(segmentRef, segQualifier);
//											}
//										}
//									}
//								}
//							}
//						}


						if(type.equalsIgnoreCase(VOID_TICKET)) {
							TicketCancelDocumentResponse  ticketCancelDocumentResponse = amadeusTicketCancelDocumentServiceImpl.ticketCancelDocument(issuanceRequest.getGdsPNR(),issuanceRequest.getTicketsList());
							if(ticketCancelDocumentResponse.isSuccess()){
								cancelPNRResponse.setSuccess(true);
							}else{
								cancelPNRResponse.setSuccess(false);
							}
						} else if (type.equalsIgnoreCase(SPLIT_PNR)) {
							cancelPNRResponse.setSuccess(true);
						} else{
							cancelPNRResponse = cancelPNR(childPNR, false, amadeusSessionWrapper);
						}
					} else {
						if(type.equalsIgnoreCase(VOID_TICKET)) {
							TicketCancelDocumentResponse  ticketCancelDocumentResponse = amadeusTicketCancelDocumentServiceImpl.ticketCancelDocument(issuanceRequest.getGdsPNR(),issuanceRequest.getTicketsList());
							if(ticketCancelDocumentResponse.isSuccess()){
								cancelPNRResponse.setSuccess(true);
							}else{
								cancelPNRResponse.setSuccess(false);
							}
						} else if (type.equalsIgnoreCase(SPLIT_PNR)) {
							cancelPNRResponse.setSuccess(true);
						} else {
							cancelPNRResponse = cancelPNR(childPNR, false, amadeusSessionWrapper);
						}
					}
					if(!type.equalsIgnoreCase(REFUND_TICKET))
					splitPNRResponse.setCancelPNRResponse(cancelPNRResponse);
					serviceHandler.saveChildPNR("10", amadeusSessionWrapper);
					/*if(!type.equalsIgnoreCase(REFUND_TICKET) && !type.equalsIgnoreCase(SPLIT_PNR)) {
						FarePricePNRWithBookingClassReply pricePNRReply = null;
						pricePNRReply = checkPNRPrice(issuanceRequest, tstRefNo, pricePNRReply, pnrResponse, amadeusSessionWrapper);
					}*/

				} catch (Exception ex) {
					logger.error("error in cancelPNR : ", ex);
					ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage(
							"childPNRCancel", ErrorMessage.ErrorType.ERROR, PROVIDERS.AMADEUS.toString());
					pnrResponse.setErrorMessage(errorMessage);
				}
			} else {
				ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage(
						"noAirlinePNR", ErrorMessage.ErrorType.ERROR, PROVIDERS.AMADEUS.toString());
				pnrResponse.setErrorMessage(errorMessage);
				logger.debug("No Airline PNR found in splitPNR");
			}
		} catch (Exception e) {
			logger.error("error in splitPNR : ", e);
			ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage(
					"error", ErrorMessage.ErrorType.ERROR, PROVIDERS.AMADEUS.toString());
			pnrResponse.setErrorMessage(errorMessage);
		} finally {
			if (amadeusSessionWrapper != null) {
				amadeusSessionManager.removeActiveSession(amadeusSessionWrapper.getmSession().value);
				serviceHandler.logOut(amadeusSessionWrapper);
			}

		}
		splitPNRResponse.setPnrResponse(pnrResponse);
		return splitPNRResponse;
	}


	private CancelPNRResponse cancelPNR(String pnr, boolean isFullPNR, Map<BigInteger, String> segmentMap, AmadeusSessionWrapper amadeusSessionWrapper) {
		logger.debug("cancelPNR called for PNR : " + pnr);
		CancelPNRResponse cancelPNRResponse = new CancelPNRResponse();
		try {

			PNRReply pnrReply = serviceHandler.retrivePNR(pnr, amadeusSessionWrapper);
			if (!isFullPNR) {
				//pnrReply = serviceHandler.partialCancelPNR(pnr, pnrReply, segmentMap, amadeusSessionWrapper);
				pnrReply = serviceHandler.cancelFullPNR(pnr, pnrReply, amadeusSessionWrapper, false);

			} else {
				logger.debug("Cancel full pnr called: " + pnr);
				pnrReply = serviceHandler.cancelFullPNR(pnr, pnrReply, amadeusSessionWrapper, false);
			}
			com.amadeus.xml.pnracc_11_3_1a.PNRReply savePNRReply = serviceHandler.savePNR(amadeusSessionWrapper);
			PNRReply retrievePNRReply = serviceHandler.retrivePNR(pnr, amadeusSessionWrapper);

			//todo check for origindestinationDetails in retrievePNRReply to confirm cancellation
			cancelPNRResponse.setSuccess(true);
			logger.debug("Successfully Cancelled PNR " + pnr);
			return cancelPNRResponse;

		} catch (Exception e) {
			e.printStackTrace();
			logger.error(pnr + " : Error in PNR cancellation ", e);
			cancelPNRResponse.setSuccess(false);
			ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage("error", ErrorMessage.ErrorType.ERROR, PROVIDERS.AMADEUS.toString());
			cancelPNRResponse.setErrorMessage(errorMessage);
			return cancelPNRResponse;
		}
	}

	private FarePricePNRWithBookingClassReply checkPNRPrice(IssuanceRequest issuanceRequest, String tstRefNo, FarePricePNRWithBookingClassReply pricePNRReply, PNRResponse pnrResponse, AmadeusSessionWrapper amadeusSessionWrapper) {
		TravellerMasterInfo travellerMasterInfo = new TravellerMasterInfo();
		travellerMasterInfo = allPNRDetails(issuanceRequest, tstRefNo);
		PNRReply gdsPNRReply = serviceHandler.retrivePNR(tstRefNo, amadeusSessionWrapper);
		logger.debug("gdsPNRReply after cancel..." + Json.toJson(gdsPNRReply));
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

		List<Traveller> travellerList = issuanceRequest.getTravellerList();
		travellerMasterInfo.setTravellersList(travellerList);
		boolean isAddBooking = false;
		if(travellerMasterInfo.getAdditionalInfo()!=null && travellerMasterInfo.getAdditionalInfo().getAddBooking()!=null && travellerMasterInfo.getAdditionalInfo().getAddBooking()) {
			isAddBooking = true;
		}
		pricePNRReply = serviceHandler.pricePNR(carrierCode, gdsPNRReply, travellerMasterInfo.isSeamen(), isDomestic, travellerMasterInfo.getItinerary(), airSegmentList, isSegmentWisePricing, amadeusSessionWrapper, isAddBooking);
		if (pricePNRReply.getApplicationError() != null) {
			if (pricePNRReply.getApplicationError().getErrorOrWarningCodeDetails().getErrorDetails().getErrorCode().equalsIgnoreCase("0")
					&& pricePNRReply.getApplicationError().getErrorOrWarningCodeDetails().getErrorDetails().getErrorCategory().equalsIgnoreCase("EC")) {
				pnrResponse.setOfficeIdPricingError(true);
			} else {
				pnrResponse.setFlightAvailable(false);
			}
		}
		Map<String, Object> travellerSegMap = createTravellerCountMap(gdsPNRReply, issuanceRequest.getBookingTravellerList());
		AmadeusBookingHelper.checkFarePrice(pricePNRReply, pnrResponse, travellerMasterInfo, travellerSegMap);
		readBaggageInfoFromPnrReply(gdsPNRReply, pricePNRReply, pnrResponse);
		return pricePNRReply;
	}

	private CancelPNRResponse cancelPNR(String pnr, boolean isFullPNR, AmadeusSessionWrapper amadeusSessionWrapper) {
		logger.debug("cancelPNR called for PNR : " + pnr);
		CancelPNRResponse cancelPNRResponse = new CancelPNRResponse();
		try {

			PNRReply pnrReply = serviceHandler.retrivePNR(pnr, amadeusSessionWrapper);
				for (PNRReply.DataElementsMaster.DataElementsIndiv dataElementsDiv : pnrReply.getDataElementsMaster().getDataElementsIndiv()) {
					logger.debug("dataElementsDiv.getElementManagementData().getSegmentName() called for PNR : " + dataElementsDiv.getElementManagementData().getSegmentName());
					/*if ("FA".equals(dataElementsDiv.getElementManagementData().getSegmentName())) {
						logger.debug("Tickets are already issued cannot cancel the pnr: " + pnr);
						cancelPNRResponse.setSuccess(false);
						ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage("ticketIssuedError", ErrorMessage.ErrorType.ERROR, PROVIDERS.AMADEUS.toString());
						cancelPNRResponse.setErrorMessage(errorMessage);
						return cancelPNRResponse;
					} else if ("FHM".equals(dataElementsDiv.getElementManagementData().getSegmentName())) {
						logger.debug("Tickets are already issued in FHM cannot cancel the pnr: " + pnr);
						cancelPNRResponse.setSuccess(false);
						ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage("ticketIssuedError", ErrorMessage.ErrorType.ERROR, PROVIDERS.AMADEUS.toString());
						cancelPNRResponse.setErrorMessage(errorMessage);
						return cancelPNRResponse;
					} else if ("FHE".equals(dataElementsDiv.getElementManagementData().getSegmentName())) {
						logger.debug("Tickets are already issued in FHE cannot cancel the pnr: " + pnr);
						cancelPNRResponse.setSuccess(false);
						ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage("ticketIssuedError", ErrorMessage.ErrorType.ERROR, PROVIDERS.AMADEUS.toString());
						cancelPNRResponse.setErrorMessage(errorMessage);
						return cancelPNRResponse;
					}*/
				}
				logger.debug("Cancel pnr called: " + pnr);
				pnrReply = serviceHandler.cancelFullPNR(pnr, pnrReply, amadeusSessionWrapper, false);
			com.amadeus.xml.pnracc_11_3_1a.PNRReply savePNRReply = serviceHandler.savePNR(amadeusSessionWrapper);
			PNRReply retrievePNRReply = serviceHandler.retrivePNR(pnr, amadeusSessionWrapper);

			//todo check for origindestinationDetails in retrievePNRReply to confirm cancellation
			cancelPNRResponse.setSuccess(true);
			logger.debug("Successfully Cancelled PNR " + pnr);
			return cancelPNRResponse;

		} catch (Exception e) {
			e.printStackTrace();
			logger.error(pnr + " : Error in PNR cancellation ", e);
			cancelPNRResponse.setSuccess(false);
			ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage("error", ErrorMessage.ErrorType.ERROR, PROVIDERS.AMADEUS.toString());
			cancelPNRResponse.setErrorMessage(errorMessage);
			return cancelPNRResponse;
		}
	}


	private Map<String, Object> createTravellerCountMap(PNRReply gdsPNRReply, List<Traveller> bookingTravellerList) {
		Map<String, Object> travellerMap = new HashMap<>();
		int adultCount = 0, childCount = 0, infantCount = 0;
		int totalCount = 0;
		for (PNRReply.TravellerInfo travellerInfo : gdsPNRReply.getTravellerInfo()) {
			String key = travellerInfo.getElementManagementPassenger().getReference().getQualifier() + '-' + travellerInfo.getElementManagementPassenger().getReference().getNumber();
			String paxName = travellerInfo.getPassengerData().get(0).getTravellerInformation().getPassenger().get(0).getFirstName() + travellerInfo.getPassengerData().get(0).getTravellerInformation().getTraveller().getSurname();
			String paxType = travellerInfo.getPassengerData().get(0).getTravellerInformation().getPassenger().get(0).getType();
			logger.info("paxType...." + paxType + "...." + String.valueOf(PassengerTypeCode.ADT));
			for (Traveller traveller : bookingTravellerList) {
				String firstName = traveller.getPersonalDetails().getFirstName();
				String salutation = traveller.getPersonalDetails().getSalutation();
				String lastName = traveller.getPersonalDetails().getLastName();
				String fullName = (firstName + " " + salutation + lastName).toUpperCase();
				logger.debug("fullName...paxName...." + fullName + "...." + paxName);
				PassengerTypeCode passengerTypeCode = DateUtility.getPassengerTypeFromDOB(traveller.getPassportDetails().getDateOfBirth());
				logger.debug("passengerType...paxName...." + passengerTypeCode.name() + "...." + paxName);
				if (fullName.equalsIgnoreCase(paxName) && passengerTypeCode.name().equalsIgnoreCase(String.valueOf(PassengerTypeCode.ADT))) {
					adultCount = adultCount + 1;
					totalCount = totalCount + 1;
					break;
				} else if (fullName.equalsIgnoreCase(paxName) && passengerTypeCode.name().equalsIgnoreCase(String.valueOf(PassengerTypeCode.CHD))) {
					childCount = childCount + 1;
					totalCount = totalCount + 1;
					break;
				} else if (fullName.equalsIgnoreCase(paxName) && passengerTypeCode.name().equalsIgnoreCase(String.valueOf(PassengerTypeCode.INF))) {
					infantCount = infantCount + 1;
					totalCount = totalCount + 1;
					break;
				}
			}
			logger.info("paxName..." + paxName + "..." + paxType);
			logger.info("key..." + key);
			travellerMap.put(paxName, key);
		}
		logger.debug("adultCount size..." + adultCount);
		travellerMap.put("adultCount", adultCount);
		travellerMap.put("childCount", childCount);
		travellerMap.put("infantCount", infantCount);
		travellerMap.put("totalCount", totalCount);
		return travellerMap;
	}
	public PNRResponse getPNRPRicing(int numberOfTst, AmadeusSessionWrapper amadeusSessionWrapper) {
		PNRResponse pnrResponse = new PNRResponse();
		TicketCreateTSTFromPricingReply ticketCreateTSTFromPricingReply = serviceHandler.createTST(numberOfTst, amadeusSessionWrapper);
		if (ticketCreateTSTFromPricingReply.getApplicationError() != null) {
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
			return pnrResponse;
		}
		return pnrResponse;
	}

	private boolean isAirlinesInJourney(FlightItinerary itinerary, String [] airlineCodes){
		for(Journey journey: itinerary.getJourneyList() ){
			for( AirSegmentInformation segmentInfo : journey.getAirSegmentList()){
				for(String aiCode: airlineCodes){
					if(segmentInfo.getCarrierCode().contains(aiCode)){
						return true;
					}
				}
			}
		}
		return false;
	}

	private boolean isAirlineInJourney(FlightItinerary itinerary, String airlineCode){
		return isAirlinesInJourney(itinerary,new String[]{airlineCode});
	}

	private String getSpecificOfficeIdforAirline(FlightItinerary itinerary){
		Configuration config = Play.application().configuration();
		Configuration airlineBookingOfficeConfig = config.getConfig("amadeus.AIRLINE_BOOKING_OFFICE");
		for(Journey journey: itinerary.getJourneyList() ){
			for( AirSegmentInformation segmentInfo : journey.getAirSegmentList()){
				//String officeId = config.getString("amadeus.AIRLINE_BOOKING_OFFICE."+carcode);
				String officeId = airlineBookingOfficeConfig.getString(segmentInfo.getCarrierCode());
				if(officeId != null){
					return officeId;
				}
			}
		}
		return null;
	}

	private SplitPNRType createSplitPNRType(IssuanceRequest issuanceRequest,Map<String,Object> travellerSegMap){
		SplitPNRType splitPNRType = new SplitPNRType();
		SplitPNRDetailsType splitPNRDetailsType = new SplitPNRDetailsType();

		for (Traveller traveller:issuanceRequest.getTravellerList()){
			String firstName = traveller.getPersonalDetails().getFirstName();
			String middleName = traveller.getPersonalDetails().getMiddleName();
			String salutation = traveller.getPersonalDetails().getSalutation();
			String lastName = traveller.getPersonalDetails().getLastName();
			String fullName = firstName;
			if(middleName != null){
				fullName = (fullName+" "+middleName+" " + salutation + lastName).toUpperCase();
			}else {
				fullName = (fullName+" " + salutation + lastName).toUpperCase();
			}
			logger.info("fullName..."+fullName);
			String paxRef = travellerSegMap.get(fullName).toString();
			String paxRefArray[] = paxRef.split("-");
			splitPNRDetailsType.setType(paxRefArray[0]);
			List<String> tatto = new ArrayList<>();
			String tat = paxRefArray[1];
			tatto.add(tat);
			splitPNRDetailsType.getTattoo().addAll(tatto);
			splitPNRType.setPassenger(splitPNRDetailsType);
		}
		return splitPNRType;
	}

	/**
	 * DTO to get SplitPNR request based on segment Information
	 * @param issuanceRequest
	 * @param travellerSegMap
	 * @return
	 */
	private SplitPNRType createSplitPNRTypeForSegment(IssuanceRequest issuanceRequest,Map<String,Object> travellerSegMap){
		SplitPNRType splitPNRType = createSplitPNRType(issuanceRequest, travellerSegMap);
		List<SplitPNRDetailsType6435C> splitPNRDetailsType6435CList = new ArrayList<>();
		for (CartAirSegmentDTO segmentDTO : issuanceRequest.getCtSegmentDtoList()){
			SplitPNRDetailsType6435C pnrDetailsType = new SplitPNRDetailsType6435C();
			pnrDetailsType.setType("ST");
			pnrDetailsType.setTattoo(String.valueOf(segmentDTO.getSequence()));
			splitPNRDetailsType6435CList.add(pnrDetailsType);
		}
		splitPNRType.getOtherElement().addAll(splitPNRDetailsType6435CList);
		return splitPNRType;
	}
	private Map<String,Object> createTravellerSegmentMap(PNRReply gdsPNRReply){
		Map<String,Object> travellerMap = new HashMap<>();
		for(PNRReply.TravellerInfo travellerInfo : gdsPNRReply.getTravellerInfo()){
			String key = travellerInfo.getElementManagementPassenger().getReference().getQualifier() +'-'+ travellerInfo.getElementManagementPassenger().getReference().getNumber();
			String paxName = travellerInfo.getPassengerData().get(0).getTravellerInformation().getPassenger().get(0).getFirstName()+travellerInfo.getPassengerData().get(0).getTravellerInformation().getTraveller().getSurname();
			logger.info("paxName..."+paxName);
			logger.info("key..."+key);
			travellerMap.put(paxName,key);
		}
		return  travellerMap;
	}
	private PNRReply readChildAirlinePNR(ServiceHandler serviceHandler, PNRReply pnrReply, Date lastPNRAddMultiElements,PNRResponse pnrResponse, AmadeusSessionWrapper amadeusSessionWrapper) throws BaseCompassitesException, InterruptedException {
		List<ItineraryInfo> itineraryInfos = pnrReply.getOriginDestinationDetails().get(0).getItineraryInfo();
		String airlinePnr = null;
		if (itineraryInfos != null && itineraryInfos.size() > 0) {
			for (ItineraryInfo itineraryInfo : itineraryInfos) {
				if (itineraryInfo.getItineraryReservationInfo() != null && itineraryInfo.getItineraryReservationInfo().getReservation() != null) {
					airlinePnr = itineraryInfo.getItineraryReservationInfo().getReservation().getControlNumber();
				}
			}
		}

		if(airlinePnr == null){
			Period p = new Period(new DateTime(lastPNRAddMultiElements), new DateTime(), PeriodType.seconds());
			if (p.getSeconds() >= 12) {
				pnrResponse.setAirlinePNRError(true);
				for (PNRReply.PnrHeader pnrHeader : pnrReply.getPnrHeader()) {
					pnrResponse.setPnrNumber(pnrHeader.getReservationInfo()
							.getReservation().getControlNumber());
				}
				throw new BaseCompassitesException("Simultaneous changes Error");
			}else {
				Thread.sleep(3000);
				pnrReply = serviceHandler.ignoreAndRetrievePNR(amadeusSessionWrapper);
				lastPNRAddMultiElements = new Date();
				readAirlinePNR(pnrReply, lastPNRAddMultiElements, pnrResponse, amadeusSessionWrapper);
			}
		} else {
			pnrResponse.setAirlinePNR(airlinePnr);
			pnrResponse.setAirlinePNRMap(AmadeusHelper.readMultipleAirlinePNR(pnrReply));

		}

		return pnrReply;
	}
	private String createChildPNR(PNRReply gdsPNRReply){
		String childPNR = "";
		for(PNRReply.DataElementsMaster.DataElementsIndiv dataElementsDiv :gdsPNRReply.getDataElementsMaster().getDataElementsIndiv()) {
			if ("SP".equals(dataElementsDiv.getElementManagementData().getSegmentName())) {
				if(dataElementsDiv.getReferencedRecord() != null){
					childPNR = dataElementsDiv.getReferencedRecord().getReferencedReservationInfo().getReservation().getControlNumber();
				}
			}
		}
		return childPNR;
	}
	private Set<String> createDataElementSegment(PNRReply gdsPNRReply){
		Set<String> isChildPNRContainSet = new HashSet<String>();
		for (DataElementsIndiv isticket : gdsPNRReply.getDataElementsMaster().getDataElementsIndiv()) {
			String segmentName = isticket.getElementManagementData().getSegmentName();
			isChildPNRContainSet.add(segmentName);
		}
		return isChildPNRContainSet;
	}

	private PNRReply readAirlinePNR(PNRReply  pnrReply, Date lastPNRAddMultiElements, PNRResponse pnrResponse, AmadeusSessionWrapper amadeusSessionWrapper) throws BaseCompassitesException, InterruptedException {
		List<ItineraryInfo> itineraryInfos = pnrReply.getOriginDestinationDetails().get(0).getItineraryInfo();
		String airlinePnr = null;
		if(itineraryInfos != null && itineraryInfos.size() > 0) {
            for(ItineraryInfo itineraryInfo : itineraryInfos){
                if(itineraryInfo.getItineraryReservationInfo() != null && itineraryInfo.getItineraryReservationInfo().getReservation() != null){
                    airlinePnr =  itineraryInfo.getItineraryReservationInfo().getReservation().getControlNumber();
                }
            }
			logger.info("airlinePnr...."+airlinePnr);
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

	public void checkSegmentStatus(PNRReply pnrReply) throws BaseCompassitesException {
		for(PNRReply.OriginDestinationDetails originDestinationDetails : pnrReply.getOriginDestinationDetails()){
			for(ItineraryInfo itineraryInfo : originDestinationDetails.getItineraryInfo()){
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


	public void checkFlightAvailibility(TravellerMasterInfo travellerMasterInfo, PNRResponse pnrResponse, AmadeusSessionWrapper amadeusSessionWrapper) {
        logger.debug("checkFlightAvailibility called ........");
		System.out.println("4");
		AirSellFromRecommendationReply sellFromRecommendation = serviceHandler
				.checkFlightAvailability(travellerMasterInfo, amadeusSessionWrapper);
		System.out.println("5");
		if (sellFromRecommendation.getErrorAtMessageLevel() != null
				&& !sellFromRecommendation.getErrorAtMessageLevel().isEmpty()
				&& (sellFromRecommendation.getItineraryDetails() == null)) {
			ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage(
					"error", ErrorMessage.ErrorType.ERROR, "Amadeus");
			pnrResponse.setErrorMessage(errorMessage);
		}
		boolean flightAvailable = AmadeusBookingHelper
				.validateFlightAvailability(sellFromRecommendation,
                        AmadeusConstants.AMADEUS_FLIGHT_AVAILIBILITY_CODE);
        /*if(!flightAvailable){
            serviceHandler.logOut();
        }*/
        pnrResponse.setSessionIdRef(amadeusSessionManager.storeActiveSession(amadeusSessionWrapper, null));
		pnrResponse.setFlightAvailable(flightAvailable);
	}

	public FarePricePNRWithBookingClassReply checkPNRPricing(TravellerMasterInfo travellerMasterInfo,
						PNRReply gdsPNRReply, FarePricePNRWithBookingClassReply pricePNRReply, PNRResponse pnrResponse, AmadeusSessionWrapper amadeusSessionWrapper) {
		String carrierCode = "";
		List<Journey> journeys;
		List<AirSegmentInformation> airSegmentList = new ArrayList<>();

		if (travellerMasterInfo.isSeamen()) {
			int size = travellerMasterInfo.getItinerary().getJourneyList().get(0).getAirSegmentList().size();
			carrierCode = travellerMasterInfo.getItinerary().getJourneyList()
					.get(0).getAirSegmentList().get(size-1).getValidatingCarrierCode();
			journeys = travellerMasterInfo.getItinerary().getJourneyList();
		} else {
			int size = travellerMasterInfo.getItinerary().getNonSeamenJourneyList().get(0).getAirSegmentList().size();
			carrierCode = travellerMasterInfo.getItinerary()
					.getNonSeamenJourneyList().get(0).getAirSegmentList()
					.get(size-1).getValidatingCarrierCode();
			journeys = travellerMasterInfo.getItinerary().getNonSeamenJourneyList();
		}

		//TODO: can be optimised
		for(Journey journey : journeys){
			for(AirSegmentInformation airSegmentInformation : journey.getAirSegmentList()){
				airSegmentList.add(airSegmentInformation);
			}
		}
		boolean isDomestic = AmadeusHelper.checkAirportCountry("India", journeys);
		boolean isSegmentWisePricing = false;
		if(travellerMasterInfo.getItinerary().getPricingInformation()!=null) {
			isSegmentWisePricing = travellerMasterInfo.getItinerary().getPricingInformation().isSegmentWisePricing();
		}
		boolean isAddBooking = false;
		if(travellerMasterInfo.getAdditionalInfo()!=null && travellerMasterInfo.getAdditionalInfo().getAddBooking()!=null && travellerMasterInfo.getAdditionalInfo().getAddBooking()) {
			isAddBooking = true;
		}
		pricePNRReply = serviceHandler.pricePNR(carrierCode, gdsPNRReply, travellerMasterInfo.isSeamen() , isDomestic, travellerMasterInfo.getItinerary(), airSegmentList, isSegmentWisePricing, amadeusSessionWrapper,isAddBooking);

		if(pricePNRReply.getApplicationError() != null) {
			if(pricePNRReply.getApplicationError().getErrorOrWarningCodeDetails().getErrorDetails().getErrorCode().equalsIgnoreCase("0")
					&& pricePNRReply.getApplicationError().getErrorOrWarningCodeDetails().getErrorDetails().getErrorCategory().equalsIgnoreCase("EC")) {
				pnrResponse.setOfficeIdPricingError(true);
			}else {
				pnrResponse.setFlightAvailable(false);
			}
			return pricePNRReply;
		}
		AmadeusBookingHelper.checkFare(pricePNRReply, pnrResponse,travellerMasterInfo);
		readBaggageInfoFromPnrReply(gdsPNRReply, pricePNRReply, pnrResponse);
//        AmadeusBookingHelper.setTaxBreakup(pnrResponse, travellerMasterInfo, pricePNRReply);
		return pricePNRReply;
	}

	@Override
	public PNRResponse priceChangePNR(TravellerMasterInfo travellerMasterInfo) {
		return generatePNR(travellerMasterInfo);
	}

	public String getPNRNoFromResponse(PNRReply gdsPNRReply) {
		String pnrNumber = null;
		for (PNRReply.PnrHeader pnrHeader : gdsPNRReply.getPnrHeader()) {
			pnrNumber = pnrHeader.getReservationInfo().getReservation()
					.getControlNumber();
			if(Objects.nonNull(pnrHeader))
				break;
		}

		return pnrNumber;
	}

	public PNRResponse createPNRResponse(PNRReply gdsPNRReply,
			FarePricePNRWithBookingClassReply pricePNRReply, PNRResponse pnrResponse, TravellerMasterInfo travellerMasterInfo) {
//		PNRResponse pnrResponse = new PNRResponse();
		//for (PNRReply.PnrHeader pnrHeader : gdsPNRReply.getPnrHeader()) {
			pnrResponse.setPnrNumber(gdsPNRReply.getPnrHeader().get(0).getReservationInfo().getReservation().getControlNumber());
		//}

		//Creating Amadeus Pax Reference and Line number here
		pnrResponse.setAmadeusPaxReference(createAmadeusPaxRefInfo(gdsPNRReply));

        if(pricePNRReply != null){
            setLastTicketingDate(pricePNRReply, pnrResponse, travellerMasterInfo);
        }
		pnrResponse.setFlightAvailable(true);
		if(gdsPNRReply.getSecurityInformation() != null && gdsPNRReply.getSecurityInformation().getSecondRpInformation() != null)
			pnrResponse.setCreationOfficeId(gdsPNRReply.getSecurityInformation().getSecondRpInformation().getCreationOfficeId());
//		pnrResponse.setTaxDetailsList(AmadeusBookingHelper
//				.getTaxDetails(pricePNRReply));
		logger.debug("todo createPNRResponse: "+ Json.stringify(Json.toJson(pnrResponse)) );
		return pnrResponse;
	}

	public static List<AmadeusPaxInformation> createAmadeusPaxRefInfo(PNRReply gdsPNRReply) {

		List<AmadeusPaxInformation> amadeusPaxInformationList = new ArrayList<>();
		List<TravellerInfo> travellerInfoList = gdsPNRReply.getTravellerInfo();

		for (PNRReply.TravellerInfo travellerInfo : travellerInfoList) {
			amadeusPaxInformationList.add(AmadeusBookingHelper.extractPassengerData(travellerInfo));
		}

		return amadeusPaxInformationList;
	}

    public void setLastTicketingDate(FarePricePNRWithBookingClassReply pricePNRReply, PNRResponse pnrResponse, TravellerMasterInfo travellerMasterInfo){
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

        if(lastTicketingDate == null){
            Calendar calendar = Calendar.getInstance();
            Date holdDate = HoldTimeUtility.getHoldTime(travellerMasterInfo);
            calendar.setTime(holdDate);
            lastTicketingDate = calendar.getTime();
            pnrResponse.setHoldTime(true);
        }
        pnrResponse.setHoldTime(false);
        pnrResponse.setValidTillDate(lastTicketingDate);

    }



	public void getCancellationFee(IssuanceRequest issuanceRequest,
			IssuanceResponse issuanceResponse, ServiceHandler serviceHandler, AmadeusSessionWrapper amadeusSessionWrapper) {
		try {

			// TODO: get right journey list for non seamen

			FlightItinerary flightItinerary = issuanceRequest.getFlightItinerary();
			boolean seamen = issuanceRequest.isSeamen();
			List<Journey> journeyList = seamen ? flightItinerary.getJourneyList() : flightItinerary.getNonSeamenJourneyList();
			List<PAXFareDetails> paxFareDetailsList = flightItinerary.getPricingInformation(seamen).getPaxFareDetailsList();
			FareInformativePricingWithoutPNRReply fareInfoReply = serviceHandler.getFareInfo(journeyList, seamen, issuanceRequest.getAdultCount(),
							issuanceRequest.getChildCount(), issuanceRequest
									.getInfantCount(), paxFareDetailsList, amadeusSessionWrapper);

			FareCheckRulesReply fareCheckRulesReply = serviceHandler
					.getFareRules(amadeusSessionWrapper);

			StringBuilder fareRule = new StringBuilder();
			for (FareCheckRulesReply.TariffInfo tariffInfo : fareCheckRulesReply
					.getTariffInfo()) {
				if ("(16)".equals(tariffInfo.getFareRuleInfo()
						.getRuleCategoryCode())) {
					for (FareCheckRulesReply.TariffInfo.FareRuleText text : tariffInfo
							.getFareRuleText()) {
						fareRule.append(text.getFreeText().get(0));
					}
				}
			}
			issuanceResponse.setCancellationFeeText(fareRule.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public PNRResponse createTempPNR(TravellerMasterInfo travellerMasterInfo) {
		logger.debug("createTempPNR called in service..");
		PNRResponse pnrResponse = new PNRResponse();
		FarePricePNRWithBookingClassReply pricePNRReply = null;
		String tstRefNo = "";
		AmadeusSessionWrapper amadeusSessionWrapper = null;
		PNRReply gdsPNRReply = null;
		try {

			amadeusSessionWrapper = serviceHandler.logIn();

			checkFlightAvailibility(travellerMasterInfo, pnrResponse, amadeusSessionWrapper);
				serviceHandler.addTravellerInfoToPNR(travellerMasterInfo, amadeusSessionWrapper);
				gdsPNRReply = serviceHandler.savePNR(amadeusSessionWrapper);
				tstRefNo = getPNRNoFromResponse(gdsPNRReply);
				System.out.println(tstRefNo);
				Thread.sleep(10000);
				gdsPNRReply = serviceHandler.retrivePNR(tstRefNo,amadeusSessionWrapper);
				createPNRResponse(gdsPNRReply, pricePNRReply, pnrResponse,travellerMasterInfo);
		} catch (Exception e) {
			logger.error("todo error in generating tmp PNR"+ e.getMessage());
			e.printStackTrace();
			logger.error("error in generateTmpPNR : ", e);
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
			if (amadeusSessionWrapper != null) {
				amadeusSessionManager.removeActiveSession(amadeusSessionWrapper.getmSession().value);
				serviceHandler.logOut(amadeusSessionWrapper);
			}
		}
		return pnrResponse;
	}
	public PNRResponse checkFareChangeAndAvailability(TravellerMasterInfo travellerMasterInfo) {

        logger.debug("checkFareChangeAndAvailability called...........");
		PNRResponse pnrResponse = new PNRResponse();
		FarePricePNRWithBookingClassReply pricePNRReply = null;
		AmadeusSessionWrapper amadeusSessionWrapper = null;
		AmadeusSessionWrapper benzyAmadeusSessionWrapper = null;
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
			PNRReply gdsPNRReply = null;
			if(travellerMasterInfo.getAdditionalInfo()!=null && travellerMasterInfo.getAdditionalInfo().getAddBooking()!=null && travellerMasterInfo.getAdditionalInfo().getAddBooking()) {
				gdsPNRReply = serviceHandler.retrivePNR(travellerMasterInfo.getAdditionalInfo().getOriginalPNR(), amadeusSessionWrapper);
			}
			checkFlightAvailibility(travellerMasterInfo, pnrResponse, amadeusSessionWrapper);

			if (pnrResponse.isFlightAvailable()) {

				//if(travellerMasterInfo.getAdditionalInfo()!=null && travellerMasterInfo.getAdditionalInfo().getAddBooking()==null) {
					gdsPNRReply = serviceHandler.addTravellerInfoToPNR(travellerMasterInfo, amadeusSessionWrapper);
				//}
				if(travellerMasterInfo.getAdditionalInfo()!=null && travellerMasterInfo.getAdditionalInfo().getAddBooking()!=null && travellerMasterInfo.getAdditionalInfo().getAddBooking()) {
					List<Journey> journeyList = travellerMasterInfo.isSeamen() ? travellerMasterInfo.getItinerary().getJourneyList() : travellerMasterInfo.getItinerary().getNonSeamenJourneyList();
					List<PAXFareDetails> paxFareDetailsList = travellerMasterInfo.getItinerary().getPricingInformation(travellerMasterInfo.isSeamen()).getPaxFareDetailsList();
					FareInformativePricingWithoutPNRReply reply = serviceHandler.getFareInfo(journeyList, travellerMasterInfo.isSeamen(), 1, 0, 0, paxFareDetailsList, amadeusSessionWrapper);
					if (reply.getErrorGroup() != null) {
						amadeusLogger.debug("Not able to fetch FareInformativePricingWithoutPNRReply: " + reply.getErrorGroup().getErrorWarningDescription().getFreeText());
						pnrResponse.setFlightAvailable(false);
						pnrResponse.setPriceChanged(false);
						return pnrResponse;
					}
				}

                /* Benzy changes */
                PNRReply gdsPNRReplyBenzy = null;
                FarePricePNRWithBookingClassReply pricePNRReplyBenzy = null;
                pricePNRReply = checkPNRPricing(travellerMasterInfo, gdsPNRReply, pricePNRReply, pnrResponse, amadeusSessionWrapper);
                if(pricePNRReply.getApplicationError() == null){
                    FareCheckRulesResponse fareCheckRulesResponse = new FareCheckRulesResponse();
                    FareCheckRulesReply fareCheckRulesReply = serviceHandler.getFareRules(amadeusSessionWrapper);
                    Map<String, Map<String,List<String>>> fareRules = new ConcurrentHashMap<>();
                    List<String> detailedFareRuleList = new ArrayList<>();
                    if (fareCheckRulesReply.getErrorInfo() == null) {
                        fareRules = AmadeusHelper.getFareCheckRulesBenzy(fareCheckRulesReply);
                        detailedFareRuleList = AmadeusHelper.getDetailedFareDetailsList(fareCheckRulesReply.getTariffInfo().get(0).getFareRuleText());
                    }
                    fareCheckRulesResponse.setRuleMap(fareRules);
                    fareCheckRulesResponse.setDetailedRuleList(detailedFareRuleList);
                    pnrResponse.setFareCheckRulesResponse(fareCheckRulesResponse);
                }
                int numberOfTst = (travellerMasterInfo.isSeamen()) ? 1 : getNumberOfTST(travellerMasterInfo.getTravellersList());
                /**
                 * isEkAndSeamen flag to check Emirates flight and Seamen booking
                 * Price the PNR in Benzy if its EK-Seamen
                 */
                //gdsPNRReply = serviceHandler.savePNR(amadeusSessionWrapper);
				//gdsPNRReply = serviceHandler.savePNR(amadeusSessionWrapper);
				//gdsPNRReply = serviceHandler.retrivePNR(travellerMasterInfo.getAdditionalInfo().getOriginalPNR(),amadeusSessionWrapper);
				if(travellerMasterInfo.getAdditionalInfo()!=null && travellerMasterInfo.getAdditionalInfo().getAddBooking()!=null && travellerMasterInfo.getAdditionalInfo().getAddBooking()) {
					String tstRefNo = getPNRNoFromResponse(gdsPNRReply);
					pnrResponse.setAddBooking(true);
					pnrResponse.setOriginalPNR(tstRefNo);
				}
				logger.debug(" gdsPNRReply "+Json.toJson(gdsPNRReply));
				if(pnrResponse.isOfficeIdPricingError() || isDelIdSeamen || pnrResponse.isChangedPriceHigh()) {
					if(travellerMasterInfo.getAdditionalInfo()!=null && travellerMasterInfo.getAdditionalInfo().getAddBooking()!=null && travellerMasterInfo.getAdditionalInfo().getAddBooking()) {
						if(pricePNRReply.getApplicationError()!=null) {
							pnrResponse.setFlightAvailable(false);
							pnrResponse.setPriceChanged(false);
							return pnrResponse;
						}
					}
					gdsPNRReply = serviceHandler.savePNR(amadeusSessionWrapper);
					String tstRefNo = getPNRNoFromResponse(gdsPNRReply);
					System.out.println(tstRefNo);
					logger.debug("checkFareChangeAndAvailability called..........."+pnrResponse);
					gdsPNRReplyBenzy = serviceHandler.retrivePNR(tstRefNo, amadeusSessionWrapper);
					//pnrResponse.setPnrNumber(tstRefNo);
					Boolean error = Boolean.FALSE;
					gdsPNRReply = serviceHandler.savePNRES(amadeusSessionWrapper, amadeusSourceOfficeService.getBenzySourceOffice().getOfficeId());
					if(!gdsPNRReply.getGeneralErrorInfo().isEmpty()){
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

                        if (!gdsPNRReplyBenzy.getGeneralErrorInfo().isEmpty()) {
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
                    FareCheckRulesReply fareCheckRulesReply = null;
                    if (pnrResponse.getPricingInfo() != null) {
                        pnrResponse.getPricingInfo().setPricingOfficeId(amadeusSourceOfficeService.getBenzySourceOffice().getOfficeId().toString());
                        fareCheckRulesReply = serviceHandler.getFareRules(benzyAmadeusSessionWrapper);
                    }
                    FareCheckRulesResponse fareCheckRulesResponse = new FareCheckRulesResponse();
                    try {
                        Map<String, Map<String,List<String>>> fareRules = new ConcurrentHashMap<>();
                        List<String> detailedFareRuleList = new ArrayList<>();
                        if (fareCheckRulesReply.getErrorInfo() == null) {
                            fareRules = AmadeusHelper.getFareCheckRulesBenzy(fareCheckRulesReply);
                            detailedFareRuleList = AmadeusHelper.getDetailedFareDetailsList(fareCheckRulesReply.getTariffInfo().get(0).getFareRuleText());
                        }
                        fareCheckRulesResponse.setRuleMap(fareRules);
                        fareCheckRulesResponse.setDetailedRuleList(detailedFareRuleList);
                        pnrResponse.setFareCheckRulesResponse(fareCheckRulesResponse);

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
                            FareInformativePricingWithoutPNRReply reply = serviceHandler.getFareInfo(journeyList, seamen, 1, 0, 0, paxFareDetailsList, amadeusSessionWrapper);
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
							amadeusLogger.debug("An exception while fetching the genericfareRule:"+ e.getMessage());
						}
					}
					if(travellerMasterInfo.getAdditionalInfo()!=null && travellerMasterInfo.getAdditionalInfo().getAddBooking()!=null && travellerMasterInfo.getAdditionalInfo().getAddBooking()) {
						if(pricePNRReply.getApplicationError()!=null) {
							pnrResponse.setFlightAvailable(false);
							pnrResponse.setPriceChanged(false);
							return pnrResponse;
						}
					}

				}
				return pnrResponse;
			} else {
				pnrResponse.setFlightAvailable(false);
			}
		} catch (Exception e) {
			e.printStackTrace();
			pnrResponse.setAddBooking(false);
			pnrResponse.setOriginalPNR("");
			logger.error("Error in checkFareChangeAndAvailability", e);
			ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage(
					"error", ErrorMessage.ErrorType.ERROR, "Amadeus");
			pnrResponse.setErrorMessage(errorMessage);
		}

		return pnrResponse;
	}


	/**
	 * Check for Airlines that has to be priced in DELHI Office ID
	 */
	private static boolean isDelIdAirlines(TravellerMasterInfo travellerMasterInfo) {
		String airlineStr = play.Play.application().configuration().getString("joc.DELHI_ID.airlines");
		List<String> specialAirline = new ArrayList<String>(Arrays.asList(airlineStr.split(",")));
		List<Journey> journeys = travellerMasterInfo.getItinerary().getJourneyList();
		if(journeys.size() >0) {
			for (Journey journey: journeys) {
				for (AirSegmentInformation airSegmentInformation: journey.getAirSegmentList()) {
					if (specialAirline.contains(airSegmentInformation.getCarrierCode())) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private boolean isBATK(TravellerMasterInfo travellerMasterInfo) {
		String airlineStr = play.Play.application().configuration().getString("joc.special.airlines");
		List<String> specialAirline = new ArrayList<String>(Arrays.asList(airlineStr.split(",")));
		for (Journey journey: travellerMasterInfo.getItinerary().getJourneyList()) {
			for (AirSegmentInformation airSegmentInformation: journey.getAirSegmentList()) {
				if(specialAirline.contains(airSegmentInformation.getCarrierCode())) {
					return true;
				}
			}
		}
		return false;
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

	public TravellerMasterInfo allPNRDetails(IssuanceRequest issuanceRequest, String gdsPNR) {

		TravellerMasterInfo masterInfo = new TravellerMasterInfo();
		boolean isSeamen = issuanceRequest.isSeamen();
		String officeId = isSeamen ? issuanceRequest.getFlightItinerary().getSeamanPricingInformation().getPricingOfficeId() : issuanceRequest.getFlightItinerary().getPricingInformation().getPricingOfficeId();
		IssuanceResponse issuanceResponse = new IssuanceResponse();
		masterInfo.setSeamen(isSeamen);
        //ServiceHandler serviceHandler = null;
		AmadeusSessionWrapper amadeusSessionWrapper = null;
        try {
            //serviceHandler = new ServiceHandler();
            //// serviceHandler.logIn();
			amadeusSessionWrapper = serviceHandler.logIn(officeId);
			PNRReply gdsPNRReply = serviceHandler.retrivePNR(gdsPNR, amadeusSessionWrapper);
			Set<String> isTicketContainSet = new HashSet<String>();
			for (DataElementsIndiv isticket : gdsPNRReply.getDataElementsMaster().getDataElementsIndiv()) {
				String isTicketIssued = isticket.getElementManagementData().getSegmentName();
				/*logger.debug("<<<<<<<<<<<<<<<<<<<<<<<<DataElementsIndiv>>>>>>>>>>>>>>>>>>>>>>>>"+Json.toJson(isticket.getElementManagementData().getSegmentName()));*/
				isTicketContainSet.add(isTicketIssued);
				/*if (isTicketIssued.equals("FA")) {
					AmadeusBookingHelper.createTickets(issuanceResponse,
							issuanceRequest, gdsPNRReply);
				}else {
					return masterInfo;
				}*/
				/*if(isticket.getElementManagementData().getSegmentName() == "FA"){
					AmadeusBookingHelper.createTickets(issuanceResponse,
							issuanceRequest, gdsPNRReply);
				} else {
					return masterInfo;
				}*/
			}
			if(isTicketContainSet.contains("FA") || isTicketContainSet.contains("FHM") || isTicketContainSet.contains("FHE") ){
				issuanceResponse.setIssued(true);
			} else {
				issuanceResponse.setIssued(false);
			}
			/*logger.debug("SET>>>>>>>>>>>>>>>>>>>"+Json.toJson(isTicketContainSet));*/
			for (String isFA : isTicketContainSet) {
				if(isFA.equalsIgnoreCase("FA")){
					AmadeusBookingHelper.createTickets(issuanceResponse,
							issuanceRequest, gdsPNRReply);
					//break;
				} if(isFA.equalsIgnoreCase("FHM")){
					AmadeusBookingHelper.createOfflineTickets(issuanceResponse,
							issuanceRequest, gdsPNRReply);
					//break;
				} if(isFA.equalsIgnoreCase("FHE")){
					AmadeusBookingHelper.createOfflineTicketsinFHE(issuanceResponse,
							issuanceRequest, gdsPNRReply);
					//break;
				}
			}
			masterInfo.setTravellersList(issuanceResponse.getTravellerList());
			/*System.out
					.println("retrivePNR ===================================>>>>>>>>>>>>>>>>>>>>>>>>>"
							+ "\n" + Json.toJson(gdsPNRReply));*/
            FlightItinerary flightItinerary = new FlightItinerary();
            List<Journey> journeyList = AmadeusBookingHelper.getJourneyListFromPNRResponse(gdsPNRReply, redisTemplate);

			if (isSeamen) {
				flightItinerary.setJourneyList(journeyList);
			} else {
				flightItinerary.setNonSeamenJourneyList(journeyList);
			}

			TicketDisplayTSTReply ticketDisplayTSTReply = serviceHandler.ticketDisplayTST(amadeusSessionWrapper);
			PricingInformation pricingInformation = AmadeusBookingHelper.getPricingInfoFromTST(gdsPNRReply, ticketDisplayTSTReply, isSeamen, journeyList);

			List<TicketDisplayTSTReply.FareList> fareList = ticketDisplayTSTReply.getFareList();
			validatingCarrierInSegment(fareList,flightItinerary,isSeamen,gdsPNRReply);

            if(isSeamen){
                flightItinerary.setSeamanPricingInformation(pricingInformation);
            }else {
                flightItinerary.setPricingInformation(pricingInformation);
            }

			masterInfo.setItinerary(flightItinerary);


//          getCancellationFee(issuanceRequest,issuanceResponse,serviceHandler);
            masterInfo.setCancellationFeeText(issuanceResponse.getCancellationFeeText());
			// logger.debug("=========================AMADEUS RESPONSE================================================\n"+Json.toJson(gdsPNRReply));
			// logger.debug("====== masterInfo details =========="+
			// Json.toJson(masterInfo));
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Error in allPNRDetails", e);
		}finally {
            serviceHandler.logOut(amadeusSessionWrapper);
        }

		return masterInfo;
	}
	



	public static int getNumberOfTST(List<Traveller> travellerList){

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

    public List<HashMap> getMiniRuleFeeFromPNR(String gdsPNR){
		//test code
//		getFareCheckRules("DELVS38LF");

		logger.info("Amadeus get mini rule from pnr called");
		PNRReply gdsPNRReply = null;
		MiniRuleGetFromRecReply miniRuleGetFromPricingRecReply = null;
//		ServiceHandler serviceHandler = null;
		AmadeusSessionWrapper amadeusSessionWrapper = null;
		List<HashMap> miniRules = new ArrayList<>();
		try {
			amadeusSessionWrapper = serviceHandler.logIn();
			gdsPNRReply = serviceHandler.retrivePNR(gdsPNR, amadeusSessionWrapper);
			miniRuleGetFromPricingRecReply = serviceHandler.retriveMiniRuleFromPNR(amadeusSessionWrapper, gdsPNR);
			Map<String, String> segmentRefMap = new HashMap<>();

			//origin Destination map
			List<PNRReply.OriginDestinationDetails> originDestinationDetails = gdsPNRReply.getOriginDestinationDetails();
			for(PNRReply.OriginDestinationDetails originDestinationDetails1 : originDestinationDetails){
				for(ItineraryInfo itineraryInfo : originDestinationDetails1.getItineraryInfo()){
					if("AIR".equalsIgnoreCase(itineraryInfo.getElementManagementItinerary().getSegmentName())) {
						String key = itineraryInfo.getElementManagementItinerary().getReference().getQualifier() + itineraryInfo.getElementManagementItinerary().getReference().getNumber();
						String value = itineraryInfo.getTravelProduct().getBoardpointDetail().getCityCode() + itineraryInfo.getTravelProduct().getOffpointDetail().getCityCode();
						segmentRefMap.put(key, value);
					}
				}
			}

			//PassengerType map
            HashMap<String, String> passengerType = new HashMap<>();
            for(PNRReply.TravellerInfo travellerInfo : gdsPNRReply.getTravellerInfo()){
                String key = "PA" + travellerInfo.getElementManagementPassenger().getReference().getNumber();
                PassengerData paxData = travellerInfo.getPassengerData().get(0);
                String paxType = paxData.getTravellerInformation().getPassenger().get(0).getType();
                String infantIndicator = paxData.getTravellerInformation().getPassenger().get(0).getInfantIndicator();
                if("chd".equalsIgnoreCase(paxType) || "ch".equalsIgnoreCase(paxType)) {
                    passengerType.put(key,"CHD");
                } else if("inf".equalsIgnoreCase(paxType) || "in".equalsIgnoreCase(paxType)) {
                    passengerType.put(key,"INF");
                }else {
                    passengerType.put(key,"ADT");
                }

                if(infantIndicator != null && "1".equalsIgnoreCase(infantIndicator)){
                    passengerType.put("PI"+ travellerInfo.getElementManagementPassenger().getReference().getNumber(), "INF");
                }
            }
			if(miniRuleGetFromPricingRecReply != null)
				miniRules=addMiniFareRules(miniRuleGetFromPricingRecReply, gdsPNRReply,passengerType,segmentRefMap);


		}catch (Exception e) {
			e.printStackTrace();
			logger.error("Error in Amadeus getMiniRuleFeeFromPNR : ", e);
		}finally {
			serviceHandler.logOut(amadeusSessionWrapper);
		}

    	return miniRules;
	}

    public MiniRule getMiniRuleFeeFromEticket(String gdsPNR,String Eticket,MiniRule miniRule){
		logger.info("Amadeus get mini rule from ticket called");
        PNRReply gdsPNRReply = null;
		MiniRuleGetFromRecReply miniRuleGetFromETicketReply = null;
        ServiceHandler serviceHandler = null;
		AmadeusSessionWrapper amadeusSessionWrapper = null;
        try {
			amadeusSessionWrapper = serviceHandler.logIn();
            gdsPNRReply = serviceHandler.retrivePNR(gdsPNR, amadeusSessionWrapper);
            miniRuleGetFromETicketReply = serviceHandler.retriveMiniRuleFromPricing(amadeusSessionWrapper);
            List<MonetaryInformationDetailsType> MonetaryInformationDetailsType = miniRuleGetFromETicketReply.getMnrByPricingRecord().get(0).getMnrRulesInfoGrp().get(3).getMnrMonInfoGrp().get(0).getMonetaryInfo().getMonetaryDetails();
            addMiniFareRulesFromEticket(MonetaryInformationDetailsType,miniRule);
        }catch (Exception e) {
            e.printStackTrace();
            logger.error("Error in Amadeus getMiniRuleFeeFromPNR : ", e);
        }finally {
            serviceHandler.logOut(amadeusSessionWrapper);
        }

        return miniRule;

    }

    public List<MiniRulesRegulPropertiesType.MnrMonInfoGrp> comparePrice(List<List<MiniRulesRegulPropertiesType.MnrMonInfoGrp>> list) {
		int res = 0;
		List<MiniRulesRegulPropertiesType.MnrMonInfoGrp> high = new ArrayList<>();
		if (list.size() < 3) {
			BigDecimal b1 = (list.get(0).get(0).getMonetaryInfo().getMonetaryDetails().get(4).getAmount());
			BigDecimal b2 = (list.get(1).get(0).getMonetaryInfo().getMonetaryDetails().get(4).getAmount());
			res = b1.compareTo(b2);
			if (res == 1) {
				return list.get(0);
			} else
				return list.get(1);
		} else {
			BigDecimal max = (list.get(0).get(0).getMonetaryInfo().getMonetaryDetails().get(4).getAmount());
			int index= 0;
			for (int i = 1; i < list.size(); i++) {
				BigDecimal b2 = (list.get(i).get(0).getMonetaryInfo().getMonetaryDetails().get(4).getAmount());
					res = max.compareTo(b2);
					if (res == 1) {
						high = list.get(index);
						max = (list.get(index).get(0).getMonetaryInfo().getMonetaryDetails().get(4).getAmount());
					} else if (res == -1) {
						index = i;
						high = list.get(index);
						max = (list.get(index).get(0).getMonetaryInfo().getMonetaryDetails().get(4).getAmount());
					} else
						high = list.get(i);
			}

		}
		return high;
	}

    public List<Integer> getMnrInfo(MiniRuleGetFromRecReply.MnrByPricingRecord monetaryInformationType1){
    	List<Integer> returnList= new ArrayList<Integer>();
		int size = monetaryInformationType1.getMnrRulesInfoGrp().size();
		List< List<MiniRulesRegulPropertiesType.MnrMonInfoGrp> > cancelMonInfo = new ArrayList<>();
		List< List<MiniRulesRegulPropertiesType.MnrMonInfoGrp> > changeMonInfo = new ArrayList<>();
		HashMap<List<MiniRulesRegulPropertiesType.MnrMonInfoGrp>,Integer> hash = new HashMap<>();
		for(int i = 0 ; i<size ;i++){
			if(monetaryInformationType1.getMnrRulesInfoGrp().get(i).getMnrCatInfo().getDescriptionInfo().getNumber().equals(new BigInteger("33"))){
				cancelMonInfo.add(monetaryInformationType1.getMnrRulesInfoGrp().get(i).getMnrMonInfoGrp());
				hash.put(monetaryInformationType1.getMnrRulesInfoGrp().get(i).getMnrMonInfoGrp(),i);

			}
			if(monetaryInformationType1.getMnrRulesInfoGrp().get(i).getMnrCatInfo().getDescriptionInfo().getNumber().equals(new BigInteger("31"))){
				changeMonInfo.add(monetaryInformationType1.getMnrRulesInfoGrp().get(i).getMnrMonInfoGrp());
				hash.put(monetaryInformationType1.getMnrRulesInfoGrp().get(i).getMnrMonInfoGrp(),i);
			}
		}
		List<MiniRulesRegulPropertiesType.MnrMonInfoGrp> res = null;
		if(cancelMonInfo.size()>1){
			 res = comparePrice(cancelMonInfo);
		}
		else {
			if(cancelMonInfo.size()>0) {
				res = cancelMonInfo.get(0);
			}
		}
		returnList.add(hash.get(res));
		if(changeMonInfo.size()>1){
			res = comparePrice(changeMonInfo);
		}
		else {
			if(changeMonInfo.size()>0) {
				res = changeMonInfo.get(0);
			}
		}
		returnList.add(hash.get(res));
		return returnList;
	}

	public List<HashMap> addMiniFareRules(MiniRuleGetFromRecReply miniRuleGetFromPricingRecReply, PNRReply gdsPNRReply,HashMap<String, String> passengerType,Map<String, String> segmentRefMap){
        logger.info("addMinirules form pnr reply and MiniRuleGetFromPricingRecReply start");
		HashMap<String,MiniRule> AdultMap = new HashMap<>();
		HashMap<String,MiniRule> ChildMap = new HashMap<>();
		HashMap<String,MiniRule> InfantMap = new HashMap<>();
		HashMap<String,String> isSeamenMap = new HashMap<>();

		List<MiniRulesRegulPropertiesType.MnrMonInfoGrp> monetaryInformationType = null;
        List<MiniRulesRegulPropertiesType.MnrRestriAppInfoGrp> restriAppInfoGrp = null;
		List<MiniRulesRegulPropertiesType.MnrRestriAppInfoGrp> changeRestriAppInfoGrp = null;
        List<MiniRulesRegulPropertiesType.MnrMonInfoGrp> changeMnrMonInfoGrp = null;
		List<HashMap> paxTypeMap = new ArrayList<>();

		for(MiniRuleGetFromRecReply.MnrByPricingRecord monetaryInformationType1 : miniRuleGetFromPricingRecReply.getMnrByPricingRecord()) {
			//int monInfoSize = monetaryInformationType1.getMnrRulesInfoGrp().size()-1;
			/*monetaryInformationType = monetaryInformationType1.getMnrRulesInfoGrp().get(monInfoSize).getMnrMonInfoGrp();
            restriAppInfoGrp = monetaryInformationType1.getMnrRulesInfoGrp().get(monInfoSize).getMnrRestriAppInfoGrp();
			changeRestriAppInfoGrp = monetaryInformationType1.getMnrRulesInfoGrp().get(monInfoSize-1).getMnrRestriAppInfoGrp();
            changeMnrMonInfoGrp = monetaryInformationType1.getMnrRulesInfoGrp().get(monInfoSize-1).getMnrMonInfoGrp();
			com.amadeus.xml.tmrqrr_11_1_1a.CategoryDescriptionType catType = monetaryInformationType1.getMnrRulesInfoGrp().get(monInfoSize-1).getMnrCatInfo().getDescriptionInfo();
			if(catType.getNumber().equals(BigInteger.valueOf(33))) {*/
				List<Integer> size = getMnrInfo(monetaryInformationType1);
				if(size.size() >0) {
					monetaryInformationType = monetaryInformationType1.getMnrRulesInfoGrp().get(size.get(0)).getMnrMonInfoGrp();
					restriAppInfoGrp = monetaryInformationType1.getMnrRulesInfoGrp().get(size.get(0)).getMnrRestriAppInfoGrp();
				}
				if(size.size()>1) {
					changeRestriAppInfoGrp = monetaryInformationType1.getMnrRulesInfoGrp().get(size.get(1)).getMnrRestriAppInfoGrp();
					changeMnrMonInfoGrp = monetaryInformationType1.getMnrRulesInfoGrp().get(size.get(1)).getMnrMonInfoGrp();
				}
			/*}*/
            for (ReferencingDetailsType mnrPaxRef : monetaryInformationType1.getPaxRef().getPassengerReference()) {

				BigDecimal cancellationFeeBeforeDept,cancellationFeeAfterDept,cancellationFeeNoShowAfterDept,cancellationFeeNoShowBeforeDept = new BigDecimal(0);
				BigDecimal changeFeeBeforeDept,changeFeeAfterDept,changeFeeNoShowAfterDept,changeFeeNoShowBeforeDept = new BigDecimal(0);
				String cancellationNoShowAfterDeptCurrency,cancellationFeeNoShowBeforeDeptCurrency,changeFeeNoShowAfterDeptCurrency,changeFeeNoShowBeforeDeptCurrency;
				String paxRef = mnrPaxRef.getType() + mnrPaxRef.getValue();
                /*int sizeOfFareComponentInfo = monetaryInformationType1.getFareComponentInfo().size() - 1;
				int sizeOfSegmentRef = monetaryInformationType1.getFareComponentInfo().get(sizeOfFareComponentInfo).getSegmentRefernce().size() - 1;
				String src = monetaryInformationType1.getFareComponentInfo().get(0).getSegmentRefernce().get(0).getReference().getType() + monetaryInformationType1.getFareComponentInfo().get(0).getSegmentRefernce().get(0).getReference().getValue();
				String dest = monetaryInformationType1.getFareComponentInfo().get(sizeOfFareComponentInfo).getSegmentRefernce().get(sizeOfSegmentRef).getReference().getType() + monetaryInformationType1.getFareComponentInfo().get(sizeOfFareComponentInfo).getSegmentRefernce().get(sizeOfSegmentRef).getReference().getValue();
				String key = (segmentRefMap.get(src)).substring(0, 3) + (segmentRefMap.get(dest)).substring(3, 6);*/
				List<String> keys = new ArrayList<>();
				for(MiniRuleGetFromRecReply.MnrByPricingRecord.FareComponentInfo fareComponentInfo : monetaryInformationType1.getFareComponentInfo()){
					for(ElementManagementSegmentType segmentType : fareComponentInfo.getSegmentRefernce()) {
						keys.add(segmentType.getReference().getType()+segmentType.getReference().getValue());
					}
				}
				Collections.sort(keys);
				String key = (segmentRefMap.get(keys.get(0))).substring(0, 3) + (segmentRefMap.get(keys.get(keys.size() - 1))).substring(3, 6);
				String paxType = passengerType.get(paxRef);
				MiniRule miniRule = new MiniRule();
				if(monetaryInformationType.size()>1) {
					cancellationFeeAfterDept=((monetaryInformationType.get(1).getMonetaryInfo().getMonetaryDetails().get(4).getAmount()));
                    miniRule.setCancellationFeeAfterDeptCurrency(monetaryInformationType.get(1).getMonetaryInfo().getMonetaryDetails().get(4).getCurrency());
					cancellationFeeBeforeDept=((monetaryInformationType.get(0).getMonetaryInfo().getMonetaryDetails().get(4).getAmount()));
                    miniRule.setCancellationFeeBeforeDeptCurrency(monetaryInformationType.get(0).getMonetaryInfo().getMonetaryDetails().get(4).getCurrency());
					cancellationFeeNoShowAfterDept=((monetaryInformationType.get(1).getMonetaryInfo().getMonetaryDetails().get(9).getAmount()));
					cancellationNoShowAfterDeptCurrency =(monetaryInformationType.get(1).getMonetaryInfo().getMonetaryDetails().get(9).getCurrency());
					cancellationFeeNoShowBeforeDept=((monetaryInformationType.get(0).getMonetaryInfo().getMonetaryDetails().get(9).getAmount()));
					cancellationFeeNoShowBeforeDeptCurrency =(monetaryInformationType.get(0).getMonetaryInfo().getMonetaryDetails().get(9).getCurrency());
				}else {
					cancellationFeeAfterDept=((monetaryInformationType.get(0).getMonetaryInfo().getMonetaryDetails().get(14).getAmount()));
                    miniRule.setCancellationFeeAfterDeptCurrency(monetaryInformationType.get(0).getMonetaryInfo().getMonetaryDetails().get(14).getCurrency());
					cancellationFeeBeforeDept=((monetaryInformationType.get(0).getMonetaryInfo().getMonetaryDetails().get(4).getAmount()));
                    miniRule.setCancellationFeeBeforeDeptCurrency(monetaryInformationType.get(0).getMonetaryInfo().getMonetaryDetails().get(4).getCurrency());
					cancellationFeeNoShowAfterDept=((monetaryInformationType.get(0).getMonetaryInfo().getMonetaryDetails().get(19).getAmount()));
                    cancellationNoShowAfterDeptCurrency = (monetaryInformationType.get(0).getMonetaryInfo().getMonetaryDetails().get(19).getCurrency());
					cancellationFeeNoShowBeforeDept=((monetaryInformationType.get(0).getMonetaryInfo().getMonetaryDetails().get(9).getAmount()));
					cancellationFeeNoShowBeforeDeptCurrency =(monetaryInformationType.get(0).getMonetaryInfo().getMonetaryDetails().get(9).getCurrency());
				}
                if(changeMnrMonInfoGrp.size() > 1){
					changeFeeNoShowAfterDept=((changeMnrMonInfoGrp.get(1).getMonetaryInfo().getMonetaryDetails().get(14).getAmount()));
					changeFeeNoShowAfterDeptCurrency = (changeMnrMonInfoGrp.get(1).getMonetaryInfo().getMonetaryDetails().get(14).getCurrency());
                    changeFeeAfterDept=((changeMnrMonInfoGrp.get(1).getMonetaryInfo().getMonetaryDetails().get(4).getAmount()));
                    miniRule.setChangeFeeFeeAfterDeptCurrency(changeMnrMonInfoGrp.get(1).getMonetaryInfo().getMonetaryDetails().get(4).getCurrency());
                    changeFeeBeforeDept=((changeMnrMonInfoGrp.get(0).getMonetaryInfo().getMonetaryDetails().get(4).getAmount()));
                    miniRule.setChangeFeeBeforeDeptCurrency(changeMnrMonInfoGrp.get(0).getMonetaryInfo().getMonetaryDetails().get(4).getCurrency());
                    changeFeeNoShowBeforeDept=((changeMnrMonInfoGrp.get(0).getMonetaryInfo().getMonetaryDetails().get(14).getAmount()));
					changeFeeNoShowBeforeDeptCurrency = (changeMnrMonInfoGrp.get(0).getMonetaryInfo().getMonetaryDetails().get(14).getCurrency());
                }else{
					changeFeeNoShowAfterDept=((changeMnrMonInfoGrp.get(0).getMonetaryInfo().getMonetaryDetails().get(19).getAmount()));
					changeFeeNoShowAfterDeptCurrency = (changeMnrMonInfoGrp.get(0).getMonetaryInfo().getMonetaryDetails().get(19).getCurrency());
                    changeFeeAfterDept=((changeMnrMonInfoGrp.get(0).getMonetaryInfo().getMonetaryDetails().get(14).getAmount()));
                    miniRule.setChangeFeeFeeAfterDeptCurrency(changeMnrMonInfoGrp.get(0).getMonetaryInfo().getMonetaryDetails().get(14).getCurrency());
                    changeFeeBeforeDept=((changeMnrMonInfoGrp.get(0).getMonetaryInfo().getMonetaryDetails().get(4).getAmount()));
                    miniRule.setChangeFeeBeforeDeptCurrency(changeMnrMonInfoGrp.get(0).getMonetaryInfo().getMonetaryDetails().get(4).getCurrency());
					changeFeeNoShowBeforeDept=((changeMnrMonInfoGrp.get(0).getMonetaryInfo().getMonetaryDetails().get(9).getAmount()));
					changeFeeNoShowBeforeDeptCurrency = (changeMnrMonInfoGrp.get(0).getMonetaryInfo().getMonetaryDetails().get(9).getCurrency());
                }

                BigDecimal markUp =new BigDecimal(play.Play.application().configuration().getDouble("markup"));
				cancellationFeeBeforeDept= cancellationFeeBeforeDept.add(cancellationFeeBeforeDept.multiply(markUp)).setScale(2, BigDecimal.ROUND_HALF_UP);
				cancellationFeeAfterDept= cancellationFeeAfterDept.add(cancellationFeeAfterDept.multiply(markUp)).setScale(2, BigDecimal.ROUND_HALF_UP);
				cancellationFeeNoShowAfterDept= cancellationFeeNoShowAfterDept.add(cancellationFeeNoShowAfterDept.multiply(markUp)).setScale(2, BigDecimal.ROUND_HALF_UP);
				cancellationFeeNoShowBeforeDept= cancellationFeeNoShowBeforeDept.add(cancellationFeeNoShowBeforeDept.multiply(markUp)).setScale(2, BigDecimal.ROUND_HALF_UP);
				changeFeeNoShowAfterDept = changeFeeNoShowAfterDept.add(changeFeeNoShowAfterDept.multiply(markUp)).setScale(2, BigDecimal.ROUND_HALF_UP);
                changeFeeNoShowBeforeDept = changeFeeNoShowBeforeDept.add(changeFeeNoShowBeforeDept.multiply(markUp)).setScale(2, BigDecimal.ROUND_HALF_UP);
				changeFeeAfterDept = changeFeeAfterDept.add(changeFeeAfterDept.multiply(markUp)).setScale(2, BigDecimal.ROUND_HALF_UP);
				changeFeeBeforeDept = changeFeeBeforeDept.add(changeFeeBeforeDept.multiply(markUp)).setScale(2, BigDecimal.ROUND_HALF_UP);

                miniRule.setCancellationFeeAfterDept(cancellationFeeAfterDept);
				miniRule.setCancellationFeeBeforeDept(cancellationFeeBeforeDept);
				miniRule.setChangeFeeAfterDept(changeFeeAfterDept);
				miniRule.setChangeFeeBeforeDept(changeFeeBeforeDept);

				List<StatusDetailsType299275C> cancelStatuslist =  restriAppInfoGrp.get(0).getMnrRestriAppInfo().getStatusInformation();
				HashMap<String,String> cancelKeys = mapFlags(cancelStatuslist);

				miniRule.setCancellationRefundableBeforeDept(Boolean.valueOf(cancelKeys.get("BDA").equalsIgnoreCase("0") ? false : true));
                miniRule.setCancellationRefundableAfterDept(Boolean.valueOf(cancelKeys.get("ADA").equalsIgnoreCase("0") ? false : true));
                miniRule.setCancellationNoShowBeforeDept(Boolean.valueOf(cancelKeys.get("BNA").equalsIgnoreCase("0") ? false : true));
                miniRule.setCancellationNoShowAfterDept(Boolean.valueOf(cancelKeys.get("ANA").equalsIgnoreCase("0") ? false : true));

                List<StatusDetailsType299275C> changeStatuslist =  changeRestriAppInfoGrp.get(0).getMnrRestriAppInfo().getStatusInformation();
				HashMap<String,String> changeKeys = mapFlags(changeStatuslist);

				miniRule.setChangeRefundableBeforeDept(Boolean.valueOf(changeKeys.get("BDA").equalsIgnoreCase("0") ? false : true));
				miniRule.setChangeRefundableAfterDept(Boolean.valueOf(changeKeys.get("ADA").equalsIgnoreCase("0") ? false : true));
				miniRule.setChangeNoShowBeforeDept(Boolean.valueOf(changeKeys.get("BNA").equalsIgnoreCase("0") ? false : true));
				miniRule.setChangeNoShowAfterDept(Boolean.valueOf(changeKeys.get("ANA").equalsIgnoreCase("0") ? false : true));

				int res = cancellationFeeNoShowAfterDept.compareTo(cancellationFeeNoShowBeforeDept);
				if(res == 1){
					miniRule.setCancellationFeeNoShow(cancellationFeeNoShowAfterDept);
					miniRule.setCancellationNoShowCurrency(cancellationNoShowAfterDeptCurrency);
					miniRule.setCancellationNoShowAfterDept(miniRule.getCancellationNoShowAfterDept());
				} else if(res == -1){
					miniRule.setCancellationFeeNoShow(cancellationFeeNoShowBeforeDept);
					miniRule.setCancellationNoShowCurrency(cancellationFeeNoShowBeforeDeptCurrency);
					miniRule.setCancellationNoShowAfterDept(miniRule.getCancellationNoShowBeforeDept());
				}else{
					miniRule.setCancellationFeeNoShow(cancellationFeeNoShowAfterDept);
					miniRule.setCancellationNoShowCurrency(cancellationNoShowAfterDeptCurrency);
					miniRule.setCancellationNoShowAfterDept(miniRule.getCancellationNoShowAfterDept());
				}

				int res1 = changeFeeNoShowAfterDept.compareTo(changeFeeNoShowBeforeDept);
				if(res1 == 1){
					miniRule.setChangeFeeNoShow(changeFeeNoShowAfterDept);
					miniRule.setChangeFeeNoShowFeeCurrency(changeFeeNoShowAfterDeptCurrency);
					miniRule.setChangeNoShowAfterDept(miniRule.getChangeNoShowAfterDept());
				} else if(res1 == -1){
					miniRule.setChangeFeeNoShow(changeFeeNoShowBeforeDept);
					miniRule.setChangeFeeNoShowFeeCurrency(changeFeeNoShowBeforeDeptCurrency);
					miniRule.setChangeNoShowAfterDept(miniRule.getChangeNoShowBeforeDept());
				}else{
					miniRule.setChangeFeeNoShow(changeFeeNoShowAfterDept);
					miniRule.setChangeFeeNoShowFeeCurrency(changeFeeNoShowAfterDeptCurrency);
					miniRule.setChangeNoShowAfterDept(miniRule.getChangeNoShowAfterDept());
				}

				if (paxType.equalsIgnoreCase("ADT")) {
					AdultMap.put(key, miniRule);
				} else if (paxType.equalsIgnoreCase("CHD")) {
					ChildMap.put(key, miniRule);
				} else if (paxType.equalsIgnoreCase("INF")) {
					InfantMap.put(key, miniRule);

			}
		}
    }

		String paxType = gdsPNRReply.getTravellerInfo().get(0).getPassengerData().get(0).getTravellerInformation().getPassenger().get(0).getType();
		String isSeamen = "false";
		if ("sea".equalsIgnoreCase(paxType)	|| "sc".equalsIgnoreCase(paxType))
			isSeamen = "true";
		isSeamenMap.put("isSeamen",isSeamen);

		paxTypeMap.add(AdultMap);
        paxTypeMap.add(ChildMap);
        paxTypeMap.add(InfantMap);
        paxTypeMap.add(passengerType);
        paxTypeMap.add(isSeamenMap);

        logger.info("addMinirules form pnr reply and MiniRuleGetFromPricingRecReply end");
		return paxTypeMap;
	}

	public HashMap mapFlags(List<StatusDetailsType299275C> statusList){
    	HashMap<String,String> keyMap = new HashMap<>();
    	for(int i=0;i<statusList.size();i++)
		{
			keyMap.put(statusList.get(i).getIndicator(),statusList.get(i).getAction());
		}
    	return keyMap;
	}

   /* public HashMap mapMonInfo(List<com.amadeus.xml.tmrqrr_11_1_1a.MiniRulesRegulPropertiesType.MnrMonInfoGrp> monInfo){
        HashMap<String,String> keyMap = new HashMap<>();
        for(int i=0;i<monInfo.size();i++){
            List<com.amadeus.xml.tmrqrr_11_1_1a.MonetaryInformationDetailsType> monInfoList = monInfo.get(i).getMonetaryInfo().getMonetaryDetails();
            for(int j=0 ; j< monInfoList.size();j++){
                keyMap.put(monInfoList.get(j).getTypeQualifier(),monInfoList.get(j).getAmount());
            }
        }
		List<com.amadeus.xml.tmrqrr_11_1_1a.MiniRulesRegulPropertiesType.MnrMonInfoGrp> monInfoList = monetaryInformationType;
		HashMap<String,String> cancelMonInfoMap = mapMonInfo(monInfoList);

        return keyMap;
    }*/

    public MiniRule addMiniFareRulesFromEticket(List<MonetaryInformationDetailsType> monetaryInformationType, MiniRule miniRule){

		miniRule.setCancellationFeeAfterDept((monetaryInformationType.get(0).getAmount()));
		miniRule.setCancellationFeeAfterDeptCurrency(monetaryInformationType.get(0).getCurrency());
		miniRule.setCancellationFeeBeforeDept(( monetaryInformationType.get(1).getAmount()));
		miniRule.setCancellationFeeBeforeDeptCurrency(monetaryInformationType.get(1).getCurrency());
	/*	miniRule.setCancellationFeeNoShowAfterDept(new BigDecimal(monetaryInformationType.get(2).getAmount()));
		miniRule.setCancellationNoShowAfterDeptCurrency(monetaryInformationType.get(2).getCurrency());
*/
		return miniRule;
    }

	public JsonNode getBookingDetails(String gdsPNR) {
		logger.info("Amadeus getBookingDetails called for PNR: " + gdsPNR);
		AmadeusSessionWrapper amadeusSessionWrapper = serviceHandler.logIn();
		return getBookingDetail(gdsPNR, amadeusSessionWrapper);
	}
	public JsonNode getBookingDetailsByOfficeId(String gdsPNR, String officeId) {
		logger.info("Amadeus getBookingDetailsByOfficeId called for PNR: " + gdsPNR + "--OfficeID: " + officeId);
		AmadeusSessionWrapper sessionWrapper = serviceHandler.logIn(officeId);
		return getBookingDetail(gdsPNR, sessionWrapper);
	}

	public JsonNode getBookingDetail(String gdsPNR, AmadeusSessionWrapper amadeusSessionWrapper) {
		logger.debug("Amadeus getBookingDetails called .......");
		PNRReply gdsPNRReply = null;
		MiniRuleGetFromRecReply miniRuleGetFromPricingRecReply = null;
		MiniRuleGetFromRecReply miniRuleGetFromETicketReply = null;
		TravellerMasterInfo masterInfo = new TravellerMasterInfo();
		FarePricePNRWithBookingClassReply pricePNRReply = null;
		PricingInformation pricingInfo = null;
		List<Journey> journeyList = null;
		Map<String, Object> json = new HashMap<>();
		try {
			gdsPNRReply = serviceHandler.retrivePNR(gdsPNR, amadeusSessionWrapper);
			List<Traveller> travellersList = new ArrayList<>();
            List<Traveller> childTravellersList = new ArrayList<>();
            List<Traveller> infantTravellersList = new ArrayList<>();
			List<TravellerInfo> travellerinfoList = gdsPNRReply.getTravellerInfo();
			Preferences preferences = getPreferenceFromPNR(gdsPNRReply);
			for (TravellerInfo travellerInfo : travellerinfoList) {
				Traveller traveller = new Traveller();

				com.amadeus.xml.pnracc_11_3_1a.ElementManagementSegmentType elementManagementPassenger = travellerInfo.getElementManagementPassenger();
				ReferencingDetailsType127526C reference = elementManagementPassenger.getReference();
				traveller.setAmadeusPaxRefQualifier(reference.getQualifier());
				traveller.setAmadeusPaxRefNumber(reference.getNumber());

				String segmentName = elementManagementPassenger.getSegmentName();
				BigInteger lineNumber = elementManagementPassenger.getLineNumber();
				String amadeusPaxSegLineRef = segmentName + lineNumber;
				traveller.setAmadeusPaxSegLineRef(amadeusPaxSegLineRef);

				PassengerData passengerData = travellerInfo.getPassengerData().get(0);
				String lastName = passengerData.getTravellerInformation().getTraveller().getSurname();
				String firstNameResponse = passengerData.getTravellerInformation().getPassenger().get(0).getFirstName();
				String passengerType = passengerData.getTravellerInformation().getPassenger().get(0).getType();
				String firstName = "";

				PersonalDetails personalDetails = new PersonalDetails();
				String[] names = firstNameResponse.split("\\s");
				//personalDetails.setFirstName(names[0]);

				if(names.length > 1){
					//personalDetails.setSalutation(names[names.length-1]);
					for (String name : names){
						if(name.equalsIgnoreCase("Mr") || name.equalsIgnoreCase("Mrs") || name.equalsIgnoreCase("Ms")
								|| name.equalsIgnoreCase("Miss") || name.equalsIgnoreCase("Master") || name.equalsIgnoreCase("Mstr") || name.equalsIgnoreCase("Capt")){
							personalDetails.setSalutation(WordUtils.capitalizeFully(name));
							if(personalDetails.getSalutation().equalsIgnoreCase("Mstr"))
								personalDetails.setSalutation("Mstr");
						}else{
							firstName = firstName+" "+name;
						}

					}
					personalDetails.setFirstName(firstName.trim());
				}



				/*if(names.length > 1)
					personalDetails.setMiddleName(names[1]);*/
				personalDetails.setLastName(lastName);
				personalDetails.setMiddleName("");
				traveller.setPersonalDetails(personalDetails);
				String infantIndicator = passengerData.getTravellerInformation().getPassenger().get(0).getInfantIndicator();
				if(passengerType != null && passengerType != ""){
					if(passengerType.equals("CHD")){
						personalDetails.setPaxType("CHD");
						traveller.setPersonalDetails(personalDetails);
						childTravellersList.add(traveller);
					} else {
						personalDetails.setPaxType("ADT");
						traveller.setPersonalDetails(personalDetails);
						travellersList.add(traveller);
					}
				} else {
					personalDetails.setPaxType("ADT");
					traveller.setPersonalDetails(personalDetails);
					travellersList.add(traveller);
				}
				if(infantIndicator != null && !"".equalsIgnoreCase(infantIndicator)){
					Traveller infantTraveller = new Traveller();
					PersonalDetails infantPersonalDetail = new PersonalDetails();
					String infantFirstName = "";
					String infFirstName = "";
					String infantLastName = "";
					TravellerDetailsTypeI infantPassenger = (passengerData.getTravellerInformation().getPassenger().size() > 1) ? passengerData.getTravellerInformation().getPassenger().get(1) : null;
					if(infantPassenger != null && ("inf".equalsIgnoreCase(infantPassenger.getType()) || "in".equalsIgnoreCase(infantPassenger.getType()))){
						infantFirstName = passengerData.getTravellerInformation().getPassenger().get(1).getFirstName();
						infantLastName = lastName;
					}else {
						infantLastName = travellerInfo.getPassengerData().get(1).getTravellerInformation().getTraveller().getSurname();
						infantFirstName = travellerInfo.getPassengerData().get(1).getTravellerInformation().getPassenger().get(0).getFirstName();
					}
					infantPersonalDetail.setLastName(infantLastName);
					names = infantFirstName.split("\\s");
					// infantPersonalDetail.setFirstName(names[0]);

					if(names.length >= 1){
						//personalDetails.setSalutation(names[names.length-1]);
						for (String name : names){
							if(name.equalsIgnoreCase("Mr") || name.equalsIgnoreCase("Mrs") || name.equalsIgnoreCase("Ms")
									|| name.equalsIgnoreCase("Miss") || name.equalsIgnoreCase("Mstr")|| name.equalsIgnoreCase("Master") || name.equalsIgnoreCase("Capt")){
								infantPersonalDetail.setSalutation(WordUtils.capitalizeFully(name));
								if(infantPersonalDetail.getSalutation().equalsIgnoreCase("Mstr"))
									infantPersonalDetail.setSalutation("Mstr");
							}else{
								infFirstName = infFirstName+" "+name;
							}

						}
						infantPersonalDetail.setFirstName(infFirstName.trim());
						infantPersonalDetail.setMiddleName("");
						infantPersonalDetail.setPaxType("INF");
					}

                    /*if(names.length > 1)
                        infantPersonalDetail.setMiddleName(names[1]);*/
					infantTraveller.setPersonalDetails(infantPersonalDetail);
					infantTravellersList.add(infantTraveller);
				}

			}
			if(childTravellersList.size() > 0){
				travellersList.addAll(childTravellersList);
			}
			if(infantTravellersList.size() > 0){
				travellersList.addAll(infantTravellersList);
			}
			masterInfo.setTravellersList(travellersList);
			Map<String, Integer> paxTypeCount = AmadeusBookingHelper.getPaxTypeCount(travellerinfoList);
			String paxType = travellerinfoList.get(0).getPassengerData().get(0).getTravellerInformation().getPassenger().get(0).getType();
			boolean isSeamen = false;
			if ("sea".equalsIgnoreCase(paxType)	|| "sc".equalsIgnoreCase(paxType))
				isSeamen = true;

			FlightItinerary flightItinerary = new FlightItinerary();
			journeyList = AmadeusBookingHelper.getJourneyListFromPNRResponse(gdsPNRReply, redisTemplate);
			//String carrierCode = "";
			if (isSeamen) {
				flightItinerary.setJourneyList(journeyList);
				//carrierCode = flightItinerary.getJourneyList().get(0).getAirSegmentList().get(0).getCarrierCode();
			} else {
				flightItinerary.setNonSeamenJourneyList(journeyList);
				//carrierCode = flightItinerary.getNonSeamenJourneyList().get(0).getAirSegmentList().get(0).getCarrierCode();
			}
//			pricePNRReply = serviceHandler.pricePNR(carrierCode, gdsPNRReply);

			//todo -- added for segment wise pricing
			TicketDisplayTSTReply ticketDisplayTSTReply = serviceHandler.ticketDisplayTST(amadeusSessionWrapper);

//			pricingInfo = AmadeusBookingHelper.getPricingInfo(pricePNRReply, totalFareIdentifier,
//							paxTypeCount.get("adultCount"),	paxTypeCount.get("childCount"),	paxTypeCount.get("infantCount"));
			PNRResponse pnrResponse = new PNRResponse();
			if(ticketDisplayTSTReply.getFareList() == null &&  ticketDisplayTSTReply.getFareList().size() == 0){
//			if(true){
				ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage("priceNotAvailable", ErrorMessage.ErrorType.ERROR, PROVIDERS.AMADEUS.toString());
				pnrResponse.setErrorMessage(errorMessage);
				json.put("pnrResponse", pnrResponse);
				return Json.toJson(json);
			}
			List<TicketDisplayTSTReply.FareList> fareList = ticketDisplayTSTReply.getFareList();
			validatingCarrierInSegment(fareList,flightItinerary,isSeamen,gdsPNRReply);
			pricingInfo = AmadeusBookingHelper.getPricingInfoFromTST(gdsPNRReply, ticketDisplayTSTReply, isSeamen, journeyList);
			if (isSeamen) {
				flightItinerary.setSeamanPricingInformation(pricingInfo);
			} else {
				flightItinerary.setPricingInformation(pricingInfo);
			}
			masterInfo.setSeamen(isSeamen);
			masterInfo.setItinerary(flightItinerary);

			// TODO: change hardcoded value
			masterInfo.setCabinClass(CabinClass.ECONOMY);

			pnrResponse.setPnrNumber(gdsPNR);
			pricingInfo.setProvider("Amadeus");
			pnrResponse.setPricingInfo(pricingInfo);

			List<ItineraryInfo> itineraryInfos = null;
			if(gdsPNRReply.getOriginDestinationDetails()!=null && gdsPNRReply.getOriginDestinationDetails().size()>0){
				itineraryInfos = gdsPNRReply.getOriginDestinationDetails().get(0).getItineraryInfo();
				if(itineraryInfos != null && itineraryInfos.size() > 0 && itineraryInfos.get(0).getItineraryReservationInfo() != null) {
					String airlinePnr = itineraryInfos.get(0).getItineraryReservationInfo().getReservation().getControlNumber();
					pnrResponse.setAirlinePNR(airlinePnr);
				}
			}

			// fetch generic fare rule
			FarePricePNRWithBookingClassReply  pricePNRReplyBenzy = null;
			pricePNRReply = checkPNRPricing(masterInfo, gdsPNRReply, pricePNRReply, pnrResponse, amadeusSessionWrapper);
			FareCheckRulesResponse fareInformativePricing = amadeusIssuanceService.getFareInformativePricing(pricePNRReply, amadeusSessionWrapper);
			pnrResponse.setFareCheckRulesResponse(fareInformativePricing);

			List<HashMap> miniRules = getMiniRuleFeeFromPNR(gdsPNR);
			logger.debug("mini rules in getbooking details is "+Json.toJson(miniRules));
			pnrResponse.setAirlinePNRMap(AmadeusHelper.readMultipleAirlinePNR(gdsPNRReply));
			createPNRResponse(gdsPNRReply, pricePNRReply, pnrResponse, masterInfo);
			readBaggageInfoFromTST(gdsPNRReply, ticketDisplayTSTReply.getFareList(), pnrResponse);
			json.put("travellerMasterInfo", masterInfo);
			json.put("pnrResponse", pnrResponse);
			json.put("miniRuleResponse", miniRules);

		} catch (Exception e) {
			logger.error("Error in Amadeus getBookingDetails : ", e);
		}finally {
			serviceHandler.logOut(amadeusSessionWrapper);
		}
		return Json.toJson(json);
	}

	private void validatingCarrierInSegment(List<TicketDisplayTSTReply.FareList> fareList, FlightItinerary flightItinerary, Boolean isSeamen, PNRReply gdsPNRReply){
		logger.debug("validatingCarrierInSegment");
		try {
			Map<String, String> validatingCarrierMap = new HashMap<>();
			Map<String, String> segmentRefMap = new HashMap<>();
			for (TicketDisplayTSTReply.FareList fare : fareList) {
				List<TicketDisplayTSTReply.FareList.SegmentInformation> segmentInformationList = fare.getSegmentInformation();
				for(TicketDisplayTSTReply.FareList.SegmentInformation segmentInformation : segmentInformationList) {
					if(segmentInformation.getSegDetails() != null && "AIR".equalsIgnoreCase(segmentInformation.getSegDetails().getSegmentDetail().getIdentification())) {
						String value = fare.getValidatingCarrier().getCarrierInformation().getCarrierCode();
						String segmentRefKey = segmentInformation.getSegmentReference().getRefDetails().get(0).getRefQualifier()+"T"+ segmentInformation.getSegmentReference().getRefDetails().get(0).getRefNumber();
						validatingCarrierMap.put(segmentRefKey, value);
					}
				}
			}

			List<PNRReply.OriginDestinationDetails> originDestinationDetails = gdsPNRReply.getOriginDestinationDetails();
			for(PNRReply.OriginDestinationDetails originDestinationDetails1 : originDestinationDetails){
				for(ItineraryInfo itineraryInfo : originDestinationDetails1.getItineraryInfo()){
					if("AIR".equalsIgnoreCase(itineraryInfo.getElementManagementItinerary().getSegmentName())) {
						String value = itineraryInfo.getElementManagementItinerary().getReference().getQualifier() + itineraryInfo.getElementManagementItinerary().getReference().getNumber();
						String key = itineraryInfo.getTravelProduct().getBoardpointDetail().getCityCode() + itineraryInfo.getTravelProduct().getOffpointDetail().getCityCode();
						segmentRefMap.put(key, value);
					}
				}
			}
			//gdsPNRReply.getOriginDestinationDetails().get(0).getItineraryInfo().get(0).getElementManagementItinerary().getReference().getQualifier()
			List<Journey> journeyList1 = null;
			if (isSeamen) {
				journeyList1 = flightItinerary.getJourneyList();
			} else {
				journeyList1 = flightItinerary.getNonSeamenJourneyList();
			}
			for (Journey journey : journeyList1) {
				for (AirSegmentInformation airSegmentInformation : journey.getAirSegmentList()) {
					String segmentKey = airSegmentInformation.getFromLocation()+airSegmentInformation.getToLocation();
					String tstKey = segmentRefMap.get(segmentKey);
					String validateCarrier = validatingCarrierMap.get(tstKey);
					airSegmentInformation.setValidatingCarrierCode(validateCarrier);
				}
			}
		} catch (Exception e){
			logger.debug("Exception validatingCarrierInSegment ",e);
		}
	}
	public Preferences getPreferenceFromPNR(PNRReply pnrReply){
		Preferences preferences = null;
		return preferences;
	}
	public void getDisplayTicketDetails(String pnr){
		//ServiceHandler serviceHandler = null;
		AmadeusSessionWrapper amadeusSessionWrapper = null;
		try {
			//serviceHandler = new ServiceHandler();
			amadeusSessionWrapper = serviceHandler.logIn();
			serviceHandler.retrivePNR(pnr, amadeusSessionWrapper);

			serviceHandler.ticketDisplayTST(amadeusSessionWrapper);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Error in Amadeus getDisplayTicketDetails", e);
		}

	}

	private void readBaggageInfoFromPnrReply(PNRReply gdsPNRReply, FarePricePNRWithBookingClassReply pnrReply, PNRResponse pnrResponse) {
		amadeusLogger.debug("Read Baggage Info........");

		Map<String,Object> airSegmentRefMap = airSegmentRefMap(gdsPNRReply);
		Map<String, String> passengerType = passengerTypeMap(gdsPNRReply);

		HashMap<String, String> map = new HashMap<>();
		List<FarePricePNRWithBookingClassReply.FareList> fareList = pnrReply.getFareList();
		try {
			for (FareList fare : fareList) {
				if (!fare.getPaxSegReference().getRefDetails().get(0).getRefQualifier().equals("PI")
					&& passengerType.get("P" + fare.getPaxSegReference().getRefDetails().get(0).getRefNumber()).equals(PassengerTypeCode.ADT.toString())) {
					for (FareList.SegmentInformation segmentInformation : fare.getSegmentInformation()) {
						String temp = (segmentInformation.getSegmentReference() != null) ?
								("S" + segmentInformation.getSegmentReference().getRefDetails().get(0).getRefNumber()) : null;
						if (temp != null && airSegmentRefMap.get(temp) != null && !map.containsKey(temp)) {
							String key = airSegmentRefMap.get(temp).toString();
							String baggage = null;
							if(segmentInformation.getBagAllowanceInformation().getBagAllowanceDetails().getBaggageQuantity()==null) {
								baggage = segmentInformation.getBagAllowanceInformation().getBagAllowanceDetails().getBaggageWeight()
										+ " " +baggageCodes.get(segmentInformation.getBagAllowanceInformation().getBagAllowanceDetails().getMeasureUnit());
							} else {
								baggage = segmentInformation.getBagAllowanceInformation().getBagAllowanceDetails().getBaggageQuantity()
										+ " " +baggageCodes.get(segmentInformation.getBagAllowanceInformation().getBagAllowanceDetails().getBaggageType());
							}
							map.put(key, baggage);
						}
					}
				}
			}
		} catch (Exception e) {
			amadeusLogger.error("Error in readBaggageInfo " , e);
			e.printStackTrace();
		}
		pnrResponse.setSegmentBaggageMap(map);
	}

	private Map<String,Object> airSegmentRefMap(PNRReply gdsPNRReply){
		logger.debug(" AirSegment Reference Map creation. ");
		Map<String,Object> airSegmentRefMap = new HashMap<>();
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
		return  airSegmentRefMap;
	}

	private Map<String, String> passengerTypeMap(PNRReply gdsPNRReply){
		logger.debug(" Passenger Type Map creation. ");
		Map<String, String> passengerType = new HashMap<>();
		for(PNRReply.TravellerInfo travellerInfo : gdsPNRReply.getTravellerInfo()){
			String key = "P" + travellerInfo.getElementManagementPassenger().getReference().getNumber();
			PassengerData paxData = travellerInfo.getPassengerData().get(0);
			String paxType = paxData.getTravellerInformation().getPassenger().get(0).getType();
			String infantIndicator = paxData.getTravellerInformation().getPassenger().get(0).getInfantIndicator();
			if("chd".equalsIgnoreCase(paxType) || "ch".equalsIgnoreCase(paxType)) {
				passengerType.put(key,"CHD");
			} else if("inf".equalsIgnoreCase(paxType) || "in".equalsIgnoreCase(paxType)) {
				passengerType.put(key,"INF");
			}else {
				passengerType.put(key,"ADT");
			}

			if(infantIndicator != null && "1".equalsIgnoreCase(infantIndicator)){
				passengerType.put("PI"+ travellerInfo.getElementManagementPassenger().getReference().getNumber(), "INF");
			}
		}
		return passengerType;
	}

	private void readBaggageInfoFromTST(PNRReply gdsPNRReply, List<TicketDisplayTSTReply.FareList> fareList, PNRResponse pnrResponse) {
		amadeusLogger.debug("Read Baggage Info........");

		Map<String,Object> airSegmentRefMap = airSegmentRefMap(gdsPNRReply);
		Map<String, String> passengerType = passengerTypeMap(gdsPNRReply);

		HashMap<String, String> map = new HashMap<>();
		try {
			for (TicketDisplayTSTReply.FareList fare : fareList) {
				if (!fare.getPaxSegReference().getRefDetails().get(0).getRefQualifier().equals("PI")
						&& passengerType.get("P" + fare.getPaxSegReference().getRefDetails().get(0).getRefNumber()).equals(PassengerTypeCode.ADT.toString())) {
					for (TicketDisplayTSTReply.FareList.SegmentInformation segmentInformation : fare.getSegmentInformation()) {
						String temp = null;
						if(segmentInformation.getSegmentReference()!=null && segmentInformation.getSegmentReference().getRefDetails()!=null && segmentInformation.getSegmentReference().getRefDetails().size()>0 && segmentInformation.getSegmentReference().getRefDetails().get(0).getRefNumber()!=null){
							temp = "S" + segmentInformation.getSegmentReference().getRefDetails().get(0).getRefNumber();
						}						
						if (airSegmentRefMap.get(temp) != null && !map.containsKey(temp)) {
							String key = airSegmentRefMap.get(temp).toString();
							String baggage = null;
							if(segmentInformation.getBagAllowanceInformation().getBagAllowanceDetails().getBaggageQuantity()==null) {
								baggage = segmentInformation.getBagAllowanceInformation().getBagAllowanceDetails().getBaggageWeight()
										+ " " +baggageCodes.get(segmentInformation.getBagAllowanceInformation().getBagAllowanceDetails().getMeasureUnit());
							} else {
								baggage = segmentInformation.getBagAllowanceInformation().getBagAllowanceDetails().getBaggageQuantity()
										+ " " +baggageCodes.get(segmentInformation.getBagAllowanceInformation().getBagAllowanceDetails().getBaggageType());
							}
							map.put(key, baggage);
						}
					}
				}
			}
		} catch (Exception e) {
			amadeusLogger.error("Error in readBaggageInfo " , e);
			e.printStackTrace();
		}
		pnrResponse.setSegmentBaggageMap(map);
	}

	public Boolean isBenzyFare(FlightItinerary flightItinerary, boolean seamen){
		if((seamen && flightItinerary.getSeamanPricingInformation().getPricingOfficeId().equalsIgnoreCase(amadeusSourceOfficeService.getBenzySourceOffice().getOfficeId())) ||
		    !seamen && flightItinerary.getPricingInformation().getPricingOfficeId().equalsIgnoreCase(amadeusSourceOfficeService.getBenzySourceOffice().getOfficeId())){
	      return Boolean.TRUE;
		}
		return Boolean.FALSE;
	}
}