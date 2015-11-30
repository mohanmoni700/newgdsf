package services;

import com.amadeus.xml.farqnr_07_1_1a.FareCheckRulesReply;
import com.amadeus.xml.itares_05_2_ia.AirSellFromRecommendationReply;
import com.amadeus.xml.pnracc_11_3_1a.PNRReply;
import com.amadeus.xml.pnracc_11_3_1a.PNRReply.DataElementsMaster.DataElementsIndiv;
import com.amadeus.xml.pnracc_11_3_1a.PNRReply.OriginDestinationDetails.ItineraryInfo;
import com.amadeus.xml.pnracc_11_3_1a.PNRReply.TravellerInfo;
import com.amadeus.xml.pnracc_11_3_1a.PNRReply.TravellerInfo.PassengerData;
import com.amadeus.xml.tautcr_04_1_1a.TicketCreateTSTFromPricingReply;
import com.amadeus.xml.tipnrr_12_4_1a.FareInformativePricingWithoutPNRReply;
import com.amadeus.xml.tpcbrr_12_4_1a.FarePricePNRWithBookingClassReply;
import com.amadeus.xml.tpcbrr_12_4_1a.StructuredDateTimeType;
import com.amadeus.xml.tplprr_12_4_1a.BaggageDetailsTypeI;
import com.amadeus.xml.tplprr_12_4_1a.FarePricePNRWithLowestFareReply;
import com.amadeus.xml.tplprr_12_4_1a.MonetaryInformationDetailsType223844C;
import com.amadeus.xml.tplprr_12_4_1a.MonetaryInformationType157202S;
import com.amadeus.xml.ttktir_09_1_1a.DocIssuanceIssueTicketReply;
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
import com.thoughtworks.xstream.XStream;
import models.AmadeusSessionWrapper;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.PeriodType;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import play.libs.Json;
import utils.*;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by Yaseen
 */
@Service
public class AmadeusBookingServiceImpl implements BookingService {

	private final String amadeusFlightAvailibilityCode = "OK";

	private final String totalFareIdentifier = "712";

	private final String issuenceOkStatus = "O";

	private final String cappingLimitString = "CT RJT";

    static org.slf4j.Logger amadeusLogger = LoggerFactory.getLogger("amadeus");

    static org.slf4j.Logger logger = LoggerFactory.getLogger("gds");

    private AmadeusSessionManager amadeusSessionManager;

    public AmadeusBookingServiceImpl() {
    }

    @Autowired
    public AmadeusBookingServiceImpl(AmadeusSessionManager amadeusSessionManager) {
        this.amadeusSessionManager = amadeusSessionManager;
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

            int numberOfTst = (travellerMasterInfo.isSeamen()) ? 1	: getPassengerTypeCount(travellerMasterInfo.getTravellersList());

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

			addSSRDetailsToPNR(serviceHandler, travellerMasterInfo, 1, lastPNRAddMultiElements);
			Thread.sleep(5000);
            gdsPNRReply = serviceHandler.retrivePNR(tstRefNo);

            createPNRResponse(gdsPNRReply, pricePNRReply, pnrResponse,travellerMasterInfo);

		} catch (Exception e) {
			e.printStackTrace();
			ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage(
					"error", ErrorMessage.ErrorType.ERROR, PROVIDERS.AMADEUS.toString());
			pnrResponse.setErrorMessage(errorMessage);
			XMLFileUtility.createXMLFile(e, "AmadeusException.xml");
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

		}
        if(airlinePnr == null){
            Period p = new Period(new DateTime(lastPNRAddMultiElements), new DateTime(), PeriodType.seconds());
            if (p.getSeconds() >= 12) {
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

	private void addSSRDetailsToPNR(ServiceHandler serviceHandler, TravellerMasterInfo travellerMasterInfo, int iteration, Date lastPNRAddMultiElements) throws BaseCompassitesException, InterruptedException {
		if(iteration <= 3){
			PNRReply addSSRResponse = serviceHandler.addSSRDetailsToPNR(travellerMasterInfo);
			simultaneousChangeAction(addSSRResponse, serviceHandler, lastPNRAddMultiElements, travellerMasterInfo, iteration);
			PNRReply savePNRReply = serviceHandler.savePNR();
			simultaneousChangeAction(savePNRReply, serviceHandler, lastPNRAddMultiElements, travellerMasterInfo, iteration);
		}else {
			serviceHandler.ignorePNRAddMultiElement();
			throw new BaseCompassitesException("Simultaneous changes Error");
		}
	}


	private void simultaneousChangeAction(PNRReply  addSSRResponse, ServiceHandler serviceHandler,
										  Date lastPNRAddMultiElements, TravellerMasterInfo travellerMasterInfo, int iteration) throws InterruptedException, BaseCompassitesException {

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
				addSSRDetailsToPNR(serviceHandler, travellerMasterInfo, iteration, lastPNRAddMultiElements);
			}
		}

	}

	public void checkSegmentStatus(PNRReply pnrReply) throws BaseCompassitesException {
		for(PNRReply.OriginDestinationDetails originDestinationDetails : pnrReply.getOriginDestinationDetails()){
			for(ItineraryInfo itineraryInfo : originDestinationDetails.getItineraryInfo()){
				for(String status : itineraryInfo.getRelatedProduct().getStatus()){
					if(!AmadeusConstants.SEGMENT_HOLDING_CONFIRMED.equalsIgnoreCase(status)){
						throw new BaseCompassitesException("Status of the segment is not cofirmed");
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
						amadeusFlightAvailibilityCode);
        /*if(!flightAvailable){
            serviceHandler.logOut();
        }*/
        pnrResponse.setSessionIdRef(amadeusSessionManager.storeActiveSession(serviceHandler.getSession()));
		pnrResponse.setFlightAvailable(flightAvailable);
	}

	public FarePricePNRWithBookingClassReply checkPNRPricing(TravellerMasterInfo travellerMasterInfo,ServiceHandler serviceHandler,
						PNRReply gdsPNRReply, FarePricePNRWithBookingClassReply pricePNRReply, PNRResponse pnrResponse) {
		String carrierCode = "";
		List<Journey> journeys;
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
		//todo -- isDomesticFlight variable is hard coded
		boolean isDomestic = AmadeusHelper.checkAirportCountry("India", journeys);
		pricePNRReply = serviceHandler.pricePNR(carrierCode, gdsPNRReply, travellerMasterInfo.isSeamen() , isDomestic, travellerMasterInfo.getItinerary());
		if(pricePNRReply.getApplicationError() != null) {
			pnrResponse.setFlightAvailable(false);
			return pricePNRReply;
		}
		AmadeusBookingHelper.checkFare(pricePNRReply, pnrResponse,
				travellerMasterInfo, totalFareIdentifier);
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

    public IssuanceResponse issueTicket(IssuanceRequest issuanceRequest) {
		ServiceHandler serviceHandler = null;
		IssuanceResponse issuanceResponse = new IssuanceResponse();
		issuanceResponse.setPnrNumber(issuanceRequest.getGdsPNR());
		AmadeusSessionWrapper amadeusSessionWrapper = null;
		try {
			serviceHandler = new ServiceHandler();
			serviceHandler.logIn();
			PNRReply gdsPNRReply = serviceHandler.retrivePNR(issuanceRequest
					.getGdsPNR());
			String carrierCode = "";
			List<Journey> journeys = null;
			if (issuanceRequest.isSeamen()) {
				carrierCode = issuanceRequest.getFlightItinerary()
						.getJourneyList().get(0).getAirSegmentList().get(0)
						.getCarrierCode();
				journeys = issuanceRequest.getFlightItinerary().getJourneyList();
			} else {
				carrierCode = issuanceRequest.getFlightItinerary()
						.getNonSeamenJourneyList().get(0).getAirSegmentList()
						.get(0).getCarrierCode();
				journeys = issuanceRequest.getFlightItinerary().getNonSeamenJourneyList();
			}
			//todo isDomesticFlight variable in call to priciPNR is hard coded
			com.amadeus.xml.tpcbrr_12_4_1a.FarePricePNRWithBookingClassReply pricePNRReply = serviceHandler
					.pricePNR(carrierCode, gdsPNRReply, issuanceRequest.isSeamen(), false, issuanceRequest.getFlightItinerary());

			int numberOfTst = (issuanceRequest.isSeamen()) ? 1
					: getPassengerTypeCount(issuanceRequest.getTravellerList());

			TicketCreateTSTFromPricingReply ticketCreateTSTFromPricingReply = serviceHandler
					.createTST(numberOfTst);

			if (ticketCreateTSTFromPricingReply.getApplicationError() != null) {
				String errorCode = ticketCreateTSTFromPricingReply
						.getApplicationError().getApplicationErrorInfo()
						.getApplicationErrorDetail().getApplicationErrorCode();

				ErrorMessage errorMessage = ErrorMessageHelper
						.createErrorMessage("error",
								ErrorMessage.ErrorType.ERROR, "Amadeus");
				issuanceResponse.setErrorMessage(errorMessage);
				issuanceResponse.setSuccess(false);
				return issuanceResponse;
			}
			gdsPNRReply = serviceHandler.savePNR();

			gdsPNRReply = serviceHandler.retrivePNR(issuanceRequest.getGdsPNR());

//			XMLFileUtility.createXMLFile(gdsPNRReply, "retrievePNRRes1.xml");
            amadeusLogger.debug("retrievePNRRes1 "+ new Date()+" ------->>"+ new XStream().toXML(gdsPNRReply));
			/*DocIssuanceIssueTicketReply issuanceIssueTicketReply = serviceHandler.issueTicket();
			if (issuenceOkStatus.equals(issuanceIssueTicketReply
					.getProcessingStatus().getStatusCode())) {
				gdsPNRReply = serviceHandler.retrivePNR(issuanceRequest
						.getGdsPNR());
				// getCancellationFee(issuanceRequest, issuanceResponse,
				// serviceHandler);
				AmadeusBookingHelper.createTickets(issuanceResponse,
						issuanceRequest, gdsPNRReply);
			} else {
				String errorDescription = issuanceIssueTicketReply
						.getErrorGroup().getErrorWarningDescription()
						.getFreeText();
				if (errorDescription.contains(cappingLimitString)) {
					logger.debug("Send Email to operator saying capping limit is reached");
					issuanceResponse.setCappingLimitReached(true);
				}
			}
*/
			issuanceResponse = docIssuance(serviceHandler, issuanceRequest, issuanceResponse);

            logger.debug("=======================  Issuance end =========================");
		} catch (Exception e) {
			XMLFileUtility.createXMLFile(e, "PNRRetrieveException.xml");
			e.printStackTrace();
		}finally {
			serviceHandler.logOut();
//			amadeusSessionManager.updateAmadeusSession(amadeusSessionWrapper);
		}
		return issuanceResponse;
	}


	public IssuanceResponse docIssuance(ServiceHandler serviceHandler, IssuanceRequest issuanceRequest, IssuanceResponse issuanceResponse) throws InterruptedException {
		Date pnrResponseReceivedAt = new Date();
		DocIssuanceIssueTicketReply issuanceIssueTicketReply = serviceHandler.issueTicket();
		if (issuenceOkStatus.equals(issuanceIssueTicketReply.getProcessingStatus().getStatusCode())) {
			Thread.sleep(3000L);
			PNRReply gdsPNRReply = serviceHandler.retrivePNR(issuanceRequest.getGdsPNR());
			boolean allTicketsReceived = AmadeusBookingHelper.createTickets(issuanceResponse,issuanceRequest, gdsPNRReply);
            if(allTicketsReceived){
				issuanceResponse.setSuccess(true);
				return issuanceResponse;
			}else {
				issuanceResponse = ignoreAndRetrievePNR(serviceHandler, issuanceRequest, issuanceResponse , pnrResponseReceivedAt);
			}
		} else {

			String errorDescription = issuanceIssueTicketReply
					.getErrorGroup().getErrorWarningDescription()
					.getFreeText();
			if (errorDescription.contains(cappingLimitString)) {
				logger.debug("Send Email to operator saying capping limit is reached");
				issuanceResponse.setCappingLimitReached(true);
			}
		}

		return issuanceResponse;
	}

	public IssuanceResponse ignoreAndRetrievePNR(ServiceHandler serviceHandler, IssuanceRequest issuanceRequest, IssuanceResponse issuanceResponse,  Date pnrResponseReceivedAt) throws InterruptedException {
		Thread.sleep(3000L);
		PNRReply gdsPNRReply = serviceHandler.ignoreAndRetrievePNR();
		boolean allTicketsReceived = AmadeusBookingHelper.createTickets(issuanceResponse, issuanceRequest, gdsPNRReply);

		if(allTicketsReceived){
			issuanceResponse.setSuccess(true);
			return issuanceResponse;
		}else{
			Period p = new Period(new DateTime(pnrResponseReceivedAt), new DateTime(), PeriodType.minutes());
			if(p.getMinutes() >= 2){
				issuanceResponse.setSuccess(false);
				return issuanceResponse;
			}
			ignoreAndRetrievePNR(serviceHandler, issuanceRequest, issuanceResponse, pnrResponseReceivedAt);
		}
		return issuanceResponse;
	}

	public void getCancellationFee(IssuanceRequest issuanceRequest,
			IssuanceResponse issuanceResponse, ServiceHandler serviceHandler) {
		try {

			// TODO: get right journey list for non seamen

			FlightItinerary flightItinerary = issuanceRequest.getFlightItinerary();
			boolean seamen = issuanceRequest.isSeamen();
			List<Journey> journeyList = seamen ? flightItinerary.getJourneyList() : flightItinerary.getNonSeamenJourneyList();
			List<PAXFareDetails> paxFareDetailsList = flightItinerary.getPricingInformation(seamen).getPaxFareDetailsList();
			FareInformativePricingWithoutPNRReply fareInfoReply = serviceHandler.getFareInfo(journeyList, issuanceRequest.getAdultCount(),
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
		try {
			ServiceHandler serviceHandler = new ServiceHandler();
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
				} /*else {
					return masterInfo; 
				}*/
			}
			/*System.out
					.println("retrivePNR ===================================>>>>>>>>>>>>>>>>>>>>>>>>>"
							+ "\n" + Json.toJson(gdsPNRReply));*/
            FlightItinerary flightItinerary = new FlightItinerary();
            List<Journey> journeyList = AmadeusBookingHelper.getJourneyListFromPNRResponse(gdsPNRReply);

			if (isSeamen) {
				flightItinerary.setJourneyList(journeyList);
			} else {
				flightItinerary.setNonSeamenJourneyList(journeyList);
			}

			TicketDisplayTSTReply ticketDisplayTSTReply = serviceHandler.ticketDisplayTST();
			PricingInformation pricingInformation = AmadeusBookingHelper.getPricingInfoFromTST(gdsPNRReply, ticketDisplayTSTReply, isSeamen);

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
		}

		return masterInfo;
	}
	
	public LowFareResponse getLowestFare(String pnr, boolean isSeamen) {
		LowFareResponse lowestFare = new LowFareResponse();
		ServiceHandler serviceHandler = null;
		try {
			serviceHandler = new ServiceHandler();
			serviceHandler.logIn();
			serviceHandler.retrivePNR(pnr);
			FarePricePNRWithLowestFareReply farePricePNRWithLowestFareReply = serviceHandler.getLowestFare(isSeamen);
			com.amadeus.xml.tplprr_12_4_1a.FarePricePNRWithLowestFareReply.FareList fareList = farePricePNRWithLowestFareReply.getFareList().get(0);
			MonetaryInformationType157202S monetaryinfo = fareList.getFareDataInformation();
			BigDecimal totalFare = null;
			for(MonetaryInformationDetailsType223844C monetaryDetails : monetaryinfo.getFareDataSupInformation()) {
				if(totalFareIdentifier.equals(monetaryDetails.getFareDataQualifier())) {
					totalFare = new BigDecimal(monetaryDetails.getFareAmount());
					break;
				}
			}
			for(com.amadeus.xml.tplprr_12_4_1a.FarePricePNRWithLowestFareReply.FareList.SegmentInformation segmentInfo : fareList.getSegmentInformation()) {
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
			}
			lowestFare.setGdsPnr(pnr);
			lowestFare.setAmount(totalFare);

			serviceHandler.logOut();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return lowestFare;
	}

    public static int getPassengerTypeCount(List<Traveller> travellerList){

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
				PassengerData paxData = travellerInfo.getPassengerData().get(0);
				String lastName = paxData.getTravellerInformation().getTraveller().getSurname();
				String firstName = paxData.getTravellerInformation().getPassenger().get(0).getFirstName();
				Traveller traveller = new Traveller();
				PersonalDetails personalDetails = new PersonalDetails();
				String[] names = firstName.split("\\s");
				personalDetails.setFirstName(names[0]);
				if(names.length > 1)
					personalDetails.setMiddleName(names[1]);
				personalDetails.setLastName(lastName);
				traveller.setPersonalDetails(personalDetails);
				travellersList.add(traveller);
			}
			masterInfo.setTravellersList(travellersList);
			Map<String, Integer> paxTypeCount = AmadeusBookingHelper.getPaxTypeCount(travellerinfoList);
			String paxType = travellerinfoList.get(0).getPassengerData().get(0).getTravellerInformation().getPassenger().get(0).getType();
			boolean isSeamen = false;
			if ("sea".equalsIgnoreCase(paxType)	|| "sc".equalsIgnoreCase(paxType))
				isSeamen = true;

			FlightItinerary flightItinerary = new FlightItinerary();
			journeyList = AmadeusBookingHelper.getJourneyListFromPNRResponse(gdsPNRReply);
			String carrierCode = "";
			if (isSeamen) {
				flightItinerary.setJourneyList(journeyList);
				carrierCode = flightItinerary.getJourneyList().get(0).getAirSegmentList().get(0).getCarrierCode();
			} else {
				flightItinerary.setNonSeamenJourneyList(journeyList);
				carrierCode = flightItinerary.getNonSeamenJourneyList().get(0).getAirSegmentList().get(0).getCarrierCode();
			}
//			pricePNRReply = serviceHandler.pricePNR(carrierCode, gdsPNRReply);

            //todo -- added for segment wise pricing
			TicketDisplayTSTReply ticketDisplayTSTReply = serviceHandler.ticketDisplayTST();

//			pricingInfo = AmadeusBookingHelper.getPricingInfo(pricePNRReply, totalFareIdentifier,
//							paxTypeCount.get("adultCount"),	paxTypeCount.get("childCount"),	paxTypeCount.get("infantCount"));

			pricingInfo = AmadeusBookingHelper.getPricingInfoFromTST(gdsPNRReply, ticketDisplayTSTReply, isSeamen);
			if (isSeamen) {
				flightItinerary.setSeamanPricingInformation(pricingInfo);
			} else {
				flightItinerary.setPricingInformation(pricingInfo);
			}
			masterInfo.setSeamen(isSeamen);
			masterInfo.setItinerary(flightItinerary);

            // TODO: change hardcoded value
            masterInfo.setCabinClass(CabinClass.ECONOMY);

            PNRResponse pnrResponse = new PNRResponse();
            pnrResponse.setPnrNumber(gdsPNR);
            pricingInfo.setProvider("Amadeus");
            pnrResponse.setPricingInfo(pricingInfo);

            List<ItineraryInfo> itineraryInfos = gdsPNRReply.getOriginDestinationDetails().get(0).getItineraryInfo();
            if(itineraryInfos != null && itineraryInfos.size() > 0) {
                String airlinePnr = itineraryInfos.get(0).getItineraryReservationInfo().getReservation().getControlNumber();
                pnrResponse.setAirlinePNR(airlinePnr);
            }
            gdsPNRReply.getOriginDestinationDetails().get(0).getItineraryInfo().get(0).getItineraryReservationInfo().getReservation().getControlNumber();
            createPNRResponse(gdsPNRReply, pricePNRReply, pnrResponse, masterInfo);

            json.put("travellerMasterInfo", masterInfo);
            json.put("pnrResponse", pnrResponse);

		} catch (Exception e) {
			e.printStackTrace();
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
		}

	}

}