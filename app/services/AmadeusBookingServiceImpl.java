package services;

import com.amadeus.xml.farqnr_07_1_1a.FareCheckRulesReply;
import com.amadeus.xml.itares_05_2_ia.AirSellFromRecommendationReply;
import com.amadeus.xml.pnracc_11_3_1a.PNRReply;
import com.amadeus.xml.pnracc_11_3_1a.PNRReply.DataElementsMaster.DataElementsIndiv;
import com.amadeus.xml.pnracc_11_3_1a.PNRReply.OriginDestinationDetails.ItineraryInfo;
import com.amadeus.xml.pnracc_11_3_1a.PNRReply.TravellerInfo;
import com.amadeus.xml.pnracc_11_3_1a.PNRReply.TravellerInfo.PassengerData;
import com.amadeus.xml.pnracc_11_3_1a.TravellerDetailsTypeI;
import com.amadeus.xml.tautcr_04_1_1a.TicketCreateTSTFromPricingReply;
import com.amadeus.xml.tipnrr_12_4_1a.FareInformativePricingWithoutPNRReply;
import com.amadeus.xml.tpcbrq_12_4_1a.*;
import com.amadeus.xml.tpcbrr_12_4_1a.FarePricePNRWithBookingClassReply;
import com.amadeus.xml.tpcbrr_12_4_1a.FarePricePNRWithBookingClassReply.FareList;
import com.amadeus.xml.tpcbrr_12_4_1a.StructuredDateTimeType;
import com.amadeus.xml.tplprq_12_4_1a.FarePricePNRWithLowestFare;
import com.amadeus.xml.tplprr_12_4_1a.BaggageDetailsTypeI;
import com.amadeus.xml.tplprr_12_4_1a.FarePricePNRWithLowestFareReply;
import com.amadeus.xml.tplprr_12_4_1a.MonetaryInformationDetailsType223844C;
import com.amadeus.xml.tplprr_12_4_1a.MonetaryInformationType157202S;
import com.amadeus.xml.ttstrr_13_1_1a.TicketDisplayTSTReply;
import com.amadeus.xml.ws._2009._01.wbs_session_2_0.Session;
import com.compassites.GDSWrapper.amadeus.ServiceHandler;
import com.compassites.constants.AmadeusConstants;
import com.compassites.exceptions.BaseCompassitesException;
import com.compassites.model.*;
import com.compassites.model.traveller.PersonalDetails;
import com.compassites.model.traveller.Traveller;
import com.compassites.model.traveller.TravellerMasterInfo;
import com.fasterxml.jackson.databind.JsonNode;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.PeriodType;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import play.libs.Json;
import utils.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by Yaseen
 */
@Service
public class AmadeusBookingServiceImpl implements BookingService {

    static org.slf4j.Logger amadeusLogger = LoggerFactory.getLogger("amadeus");

    static org.slf4j.Logger logger = LoggerFactory.getLogger("gds");

    private AmadeusSessionManager amadeusSessionManager;

	private static Map<String, String> baggageCodes = new HashMap<>();

	@Autowired
	private RedisTemplate redisTemplate;

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
		ServiceHandler serviceHandler = null;
		PNRResponse pnrResponse = new PNRResponse();
        PNRReply gdsPNRReply = null;
		FarePricePNRWithBookingClassReply pricePNRReply = null;
		Session session = null;
		try {
			serviceHandler = new ServiceHandler();
            session = amadeusSessionManager.getActiveSession(travellerMasterInfo.getSessionIdRef());
            serviceHandler.setSession(session);

            int numberOfTst = (travellerMasterInfo.isSeamen()) ? 1	: getNumberOfTST(travellerMasterInfo.getTravellersList());

            TicketCreateTSTFromPricingReply ticketCreateTSTFromPricingReply = serviceHandler.createTST(numberOfTst);
            if (ticketCreateTSTFromPricingReply.getApplicationError() != null) {
                String errorCode = ticketCreateTSTFromPricingReply
                        .getApplicationError()
                        .getApplicationErrorInfo()
                        .getApplicationErrorDetail()
                        .getApplicationErrorCode();

                ErrorMessage errorMessage = ErrorMessageHelper
                        .createErrorMessage("error",
                                ErrorMessage.ErrorType.ERROR, "Amadeus");
                pnrResponse.setErrorMessage(errorMessage);
                pnrResponse.setFlightAvailable(false);
                return pnrResponse;
            }
            gdsPNRReply = serviceHandler.savePNR();
            String tstRefNo = getPNRNoFromResponse(gdsPNRReply);
			Thread.sleep(10000);
			gdsPNRReply = serviceHandler.retrivePNR(tstRefNo);
            Date lastPNRAddMultiElements = new Date();

            gdsPNRReply = readAirlinePNR(serviceHandler,gdsPNRReply,lastPNRAddMultiElements,pnrResponse);

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

			addSSRDetailsToPNR(serviceHandler, travellerMasterInfo, 1, lastPNRAddMultiElements, segmentNumbers, travellerMap);
			Thread.sleep(5000);
            gdsPNRReply = serviceHandler.retrivePNR(tstRefNo);

            createPNRResponse(gdsPNRReply, pricePNRReply, pnrResponse,travellerMasterInfo);

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("error in generatePNR : ", e);
			ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage(
					"error", ErrorMessage.ErrorType.ERROR, PROVIDERS.AMADEUS.toString());
			pnrResponse.setErrorMessage(errorMessage);
		}finally {
			if(session != null){
				serviceHandler.logOut();
				amadeusSessionManager.removeActiveSession(session);
			}

		}
		return pnrResponse;
	}

	private PNRReply readAirlinePNR(ServiceHandler serviceHandler, PNRReply  pnrReply, Date lastPNRAddMultiElements, PNRResponse pnrResponse) throws BaseCompassitesException, InterruptedException {
		List<ItineraryInfo> itineraryInfos = pnrReply.getOriginDestinationDetails().get(0).getItineraryInfo();
		String airlinePnr = null;
		if(itineraryInfos != null && itineraryInfos.size() > 0) {
            for(ItineraryInfo itineraryInfo : itineraryInfos){
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
                throw new BaseCompassitesException("Simultaneous changes Error");
            }else {
                Thread.sleep(3000);
                pnrReply = serviceHandler.ignoreAndRetrievePNR();
                lastPNRAddMultiElements = new Date();
                readAirlinePNR(serviceHandler, pnrReply, lastPNRAddMultiElements, pnrResponse);
            }
        }

        return pnrReply;
	}

	private void addSSRDetailsToPNR(ServiceHandler serviceHandler, TravellerMasterInfo travellerMasterInfo, int iteration, Date lastPNRAddMultiElements,
									List<String> segmentNumbers, Map<String,String> travellerMap) throws BaseCompassitesException, InterruptedException {
		if(iteration <= 3){
			PNRReply addSSRResponse = serviceHandler.addSSRDetailsToPNR(travellerMasterInfo, segmentNumbers, travellerMap);
			simultaneousChangeAction(addSSRResponse, serviceHandler, lastPNRAddMultiElements, travellerMasterInfo, iteration, segmentNumbers, travellerMap);
			PNRReply savePNRReply = serviceHandler.savePNR();
			simultaneousChangeAction(savePNRReply, serviceHandler, lastPNRAddMultiElements, travellerMasterInfo, iteration, segmentNumbers, travellerMap);
		}else {
			serviceHandler.ignorePNRAddMultiElement();
			throw new BaseCompassitesException("Simultaneous changes Error");
		}
	}


	private void simultaneousChangeAction(PNRReply  addSSRResponse, ServiceHandler serviceHandler,
										  Date lastPNRAddMultiElements, TravellerMasterInfo travellerMasterInfo, int iteration,
										  List<String> segmentNumbers, Map<String,String> travellerMap) throws InterruptedException, BaseCompassitesException {

		boolean simultaneousChangeToPNR  = AmadeusBookingHelper.checkForSimultaneousChange(addSSRResponse);
		if(simultaneousChangeToPNR) {
			Period p = new Period(new DateTime(lastPNRAddMultiElements), new DateTime(), PeriodType.seconds());
			if (p.getSeconds() >= 12) {
				serviceHandler.ignorePNRAddMultiElement();
				throw new BaseCompassitesException("Simultaneous changes Error");
			}else {
				Thread.sleep(3000);
				PNRReply pnrReply = serviceHandler.ignoreAndRetrievePNR();
				lastPNRAddMultiElements = new Date();
				iteration = iteration + 1;
				addSSRDetailsToPNR(serviceHandler, travellerMasterInfo, iteration, lastPNRAddMultiElements, segmentNumbers, travellerMap);
			}
		}

	}

	public void checkSegmentStatus(PNRReply pnrReply) throws BaseCompassitesException {
		for(PNRReply.OriginDestinationDetails originDestinationDetails : pnrReply.getOriginDestinationDetails()){
			for(ItineraryInfo itineraryInfo : originDestinationDetails.getItineraryInfo()){
				for(String status : itineraryInfo.getRelatedProduct().getStatus()){
					if(!AmadeusConstants.SEGMENT_HOLDING_CONFIRMED.equalsIgnoreCase(status)){
						throw new BaseCompassitesException("No seats available please try again later.s");
					}

				}
			}
		}

		return ;
	}


	public void checkFlightAvailibility(TravellerMasterInfo travellerMasterInfo,ServiceHandler serviceHandler, PNRResponse pnrResponse) {
        logger.debug("checkFlightAvailibility called ........");
		AirSellFromRecommendationReply sellFromRecommendation = serviceHandler
				.checkFlightAvailability(travellerMasterInfo);

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
        /*if(!flightAvailable){
            serviceHandler.logOut();
        }*/
        pnrResponse.setSessionIdRef(amadeusSessionManager.storeActiveSession(serviceHandler.getSession(), null));
		pnrResponse.setFlightAvailable(flightAvailable);
	}

	public FarePricePNRWithBookingClassReply checkPNRPricing(TravellerMasterInfo travellerMasterInfo,ServiceHandler serviceHandler,
						PNRReply gdsPNRReply, FarePricePNRWithBookingClassReply pricePNRReply, PNRResponse pnrResponse) {
		String carrierCode = "";
		List<Journey> journeys;
		List<AirSegmentInformation> airSegmentList = new ArrayList<>();

		if (travellerMasterInfo.isSeamen()) {
			carrierCode = travellerMasterInfo.getItinerary().getJourneyList()
					.get(0).getAirSegmentList().get(0).getCarrierCode();
			journeys = travellerMasterInfo.getItinerary().getJourneyList();
		} else {
			carrierCode = travellerMasterInfo.getItinerary()
					.getNonSeamenJourneyList().get(0).getAirSegmentList()
					.get(0).getCarrierCode();
			journeys = travellerMasterInfo.getItinerary().getNonSeamenJourneyList();
		}

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
		pricePNRReply = serviceHandler.pricePNR(carrierCode, gdsPNRReply, travellerMasterInfo.isSeamen() , isDomestic, travellerMasterInfo.getItinerary(), airSegmentList, isSegmentWisePricing);
		if(pricePNRReply.getApplicationError() != null) {
			pnrResponse.setFlightAvailable(false);
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
		}

		return pnrNumber;
	}

	public PNRResponse createPNRResponse(PNRReply gdsPNRReply,
			FarePricePNRWithBookingClassReply pricePNRReply, PNRResponse pnrResponse, TravellerMasterInfo travellerMasterInfo) {
//		PNRResponse pnrResponse = new PNRResponse();
		for (PNRReply.PnrHeader pnrHeader : gdsPNRReply.getPnrHeader()) {
			pnrResponse.setPnrNumber(pnrHeader.getReservationInfo()
					.getReservation().getControlNumber());
		}
        if(pricePNRReply != null){
            setLastTicketingDate(pricePNRReply, pnrResponse, travellerMasterInfo);
        }
		pnrResponse.setFlightAvailable(true);

//		pnrResponse.setTaxDetailsList(AmadeusBookingHelper
//				.getTaxDetails(pricePNRReply));
		return pnrResponse;
	}


    public void setLastTicketingDate(FarePricePNRWithBookingClassReply pricePNRReply, PNRResponse pnrResponse, TravellerMasterInfo travellerMasterInfo){
		Date lastTicketingDate = null;
		if(pricePNRReply.getFareList().get(0).getLastTktDate() != null){
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
			IssuanceResponse issuanceResponse, ServiceHandler serviceHandler) {
		try {

			// TODO: get right journey list for non seamen

			FlightItinerary flightItinerary = issuanceRequest.getFlightItinerary();
			boolean seamen = issuanceRequest.isSeamen();
			List<Journey> journeyList = seamen ? flightItinerary.getJourneyList() : flightItinerary.getNonSeamenJourneyList();
			List<PAXFareDetails> paxFareDetailsList = flightItinerary.getPricingInformation(seamen).getPaxFareDetailsList();
			FareInformativePricingWithoutPNRReply fareInfoReply = serviceHandler.getFareInfo(journeyList, seamen, issuanceRequest.getAdultCount(),
							issuanceRequest.getChildCount(), issuanceRequest
									.getInfantCount(), paxFareDetailsList);

			FareCheckRulesReply fareCheckRulesReply = serviceHandler
					.getFareRules();

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

	public PNRResponse checkFareChangeAndAvailability(
			TravellerMasterInfo travellerMasterInfo) {
        logger.debug("checkFareChangeAndAvailability called...........");
		ServiceHandler serviceHandler = null;
		PNRResponse pnrResponse = new PNRResponse();
		FarePricePNRWithBookingClassReply pricePNRReply = null;
		try {
			serviceHandler = new ServiceHandler();
			serviceHandler.logIn();
			checkFlightAvailibility(travellerMasterInfo, serviceHandler,
					pnrResponse);
			if (pnrResponse.isFlightAvailable()) {
				PNRReply gdsPNRReply = serviceHandler.addTravellerInfoToPNR(travellerMasterInfo);
				pricePNRReply = checkPNRPricing(travellerMasterInfo,serviceHandler, gdsPNRReply, pricePNRReply, pnrResponse);

                setLastTicketingDate(pricePNRReply, pnrResponse, travellerMasterInfo);
				return pnrResponse;
			} else {
				pnrResponse.setFlightAvailable(false);
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Error in checkFareChangeAndAvailability", e);
			ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage(
					"error", ErrorMessage.ErrorType.ERROR, "Amadeus");
			pnrResponse.setErrorMessage(errorMessage);
		}

		return pnrResponse;
	}

	public TravellerMasterInfo allPNRDetails(IssuanceRequest issuanceRequest,
			String gdsPNR) {
		TravellerMasterInfo masterInfo = new TravellerMasterInfo();
		boolean isSeamen = issuanceRequest.isSeamen();
		IssuanceResponse issuanceResponse = new IssuanceResponse();
		masterInfo.setSeamen(isSeamen);
        ServiceHandler serviceHandler = null;

        try {
            serviceHandler = new ServiceHandler();
            serviceHandler.logIn();

			PNRReply gdsPNRReply = serviceHandler.retrivePNR(gdsPNR);
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
			/*logger.debug("SET>>>>>>>>>>>>>>>>>>>"+Json.toJson(isTicketContainSet));*/
			for (String isFA : isTicketContainSet) {
				if(isFA.equalsIgnoreCase("FA")){
					AmadeusBookingHelper.createTickets(issuanceResponse,
							issuanceRequest, gdsPNRReply);
					break;
				} else if(isFA.equalsIgnoreCase("FHM")){
					AmadeusBookingHelper.createOfflineTickets(issuanceResponse,
							issuanceRequest, gdsPNRReply);
					break;
				} /*else {
					return masterInfo; 
				}*/
			}
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

			TicketDisplayTSTReply ticketDisplayTSTReply = serviceHandler.ticketDisplayTST();
			PricingInformation pricingInformation = AmadeusBookingHelper.getPricingInfoFromTST(gdsPNRReply, ticketDisplayTSTReply, isSeamen, journeyList);

            if(isSeamen){
                flightItinerary.setSeamanPricingInformation(pricingInformation);
            }else {
                flightItinerary.setPricingInformation(pricingInformation);
            }

			masterInfo.setItinerary(flightItinerary);

			masterInfo.setTravellersList(issuanceResponse.getTravellerList());
//            getCancellationFee(issuanceRequest,issuanceResponse,serviceHandler);
            masterInfo.setCancellationFeeText(issuanceResponse.getCancellationFeeText());
			// logger.debug("=========================AMADEUS RESPONSE================================================\n"+Json.toJson(gdsPNRReply));
			// logger.debug("====== masterInfo details =========="+
			// Json.toJson(masterInfo));
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("Error in allPNRDetails", e);
		}finally {
            serviceHandler.logOut();
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
    
	public JsonNode getBookingDetails(String gdsPNR) {
		logger.debug("Amadeus getBookingDetails called .......");
		PNRReply gdsPNRReply = null;
		TravellerMasterInfo masterInfo = new TravellerMasterInfo();
		FarePricePNRWithBookingClassReply pricePNRReply = null;
		PricingInformation pricingInfo = null;
		List<Journey> journeyList = null;
        Map<String, Object> json = new HashMap<>();
		ServiceHandler serviceHandler = null;
        try {
			serviceHandler = new ServiceHandler();
			serviceHandler.logIn();
			gdsPNRReply = serviceHandler.retrivePNR(gdsPNR);
			List<Traveller> travellersList = new ArrayList<>();
			List<TravellerInfo> travellerinfoList = gdsPNRReply.getTravellerInfo();
			for (TravellerInfo travellerInfo : travellerinfoList) {
				PassengerData passengerData = travellerInfo.getPassengerData().get(0);
				String lastName = passengerData.getTravellerInformation().getTraveller().getSurname();
				String firstName = passengerData.getTravellerInformation().getPassenger().get(0).getFirstName();
				Traveller traveller = new Traveller();
				PersonalDetails personalDetails = new PersonalDetails();
				String[] names = firstName.split("\\s");
				personalDetails.setFirstName(names[0]);
				if(names.length > 1)
					personalDetails.setMiddleName(names[1]);
				personalDetails.setLastName(lastName);
				traveller.setPersonalDetails(personalDetails);
				travellersList.add(traveller);

				String infantIndicator = passengerData.getTravellerInformation().getPassenger().get(0).getInfantIndicator();
				if(infantIndicator != null && !"".equalsIgnoreCase(infantIndicator)){
                    Traveller infantTraveller = new Traveller();
                    PersonalDetails infantPersonalDetail = new PersonalDetails();
                    String infantFirstName = "";
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
                    infantPersonalDetail.setFirstName(names[0]);
                    if(names.length > 1)
                        infantPersonalDetail.setMiddleName(names[1]);
                    infantTraveller.setPersonalDetails(infantPersonalDetail);
                    travellersList.add(infantTraveller);
				}

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
			TicketDisplayTSTReply ticketDisplayTSTReply = serviceHandler.ticketDisplayTST();

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
			pnrResponse.setAirlinePNRMap(AmadeusHelper.readMultipleAirlinePNR(gdsPNRReply));
            createPNRResponse(gdsPNRReply, pricePNRReply, pnrResponse, masterInfo);
			readBaggageInfoFromTST(gdsPNRReply, ticketDisplayTSTReply.getFareList(), pnrResponse);
            json.put("travellerMasterInfo", masterInfo);
            json.put("pnrResponse", pnrResponse);

		} catch (Exception e) {
			e.printStackTrace();
            logger.error("Error in Amadeus getBookingDetails : ", e);
		}finally {
			serviceHandler.logOut();
		}
		return Json.toJson(json);
	}

	public void getDisplayTicketDetails(String pnr){
		ServiceHandler serviceHandler = null;
		try {
			serviceHandler = new ServiceHandler();
			serviceHandler.logIn();

			serviceHandler.retrivePNR(pnr);

			serviceHandler.ticketDisplayTST();
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
						String temp = "S" + segmentInformation.getSegmentReference().getRefDetails().get(0).getRefNumber();
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

	private Map<String,Object> airSegmentRefMap(PNRReply gdsPNRReply){
		logger.debug(" AirSegment Reference Map creation. ");
		Map<String,Object> airSegmentRefMap = new HashMap<>();
		for(PNRReply.OriginDestinationDetails originDestination : gdsPNRReply.getOriginDestinationDetails()){
			for(PNRReply.OriginDestinationDetails.ItineraryInfo itineraryInfo : originDestination.getItineraryInfo()){
				String segmentRef = "S"+itineraryInfo.getElementManagementItinerary().getReference().getNumber();
				String segments = itineraryInfo.getTravelProduct().getBoardpointDetail().getCityCode() + itineraryInfo.getTravelProduct().getOffpointDetail().getCityCode();
				airSegmentRefMap.put(segmentRef,segments);
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
}