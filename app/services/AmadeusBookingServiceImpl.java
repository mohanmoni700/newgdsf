package services;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import play.libs.Json;

import utils.AmadeusBookingHelper;
import utils.DateUtility;
import utils.ErrorMessageHelper;

import com.amadeus.xml.farqnr_07_1_1a.FareCheckRulesReply;
import com.amadeus.xml.itares_05_2_ia.AirSellFromRecommendationReply;
import com.amadeus.xml.pnracc_10_1_1a.PNRReply;
import com.amadeus.xml.pnracc_10_1_1a.PNRReply.DataElementsMaster.DataElementsIndiv;
import com.amadeus.xml.pnracc_10_1_1a.PNRReply.OriginDestinationDetails.ItineraryInfo;
import com.amadeus.xml.pnracc_10_1_1a.PNRReply.TravellerInfo;
import com.amadeus.xml.pnracc_10_1_1a.PNRReply.TravellerInfo.PassengerData;
import com.amadeus.xml.tautcr_04_1_1a.TicketCreateTSTFromPricingReply;
import com.amadeus.xml.tipnrr_12_4_1a.FareInformativePricingWithoutPNRReply;
import com.amadeus.xml.tpcbrr_07_3_1a.FarePricePNRWithBookingClassReply;
import com.amadeus.xml.tplprr_12_4_1a.BaggageDetailsTypeI;
import com.amadeus.xml.tplprr_12_4_1a.FarePricePNRWithLowestFareReply;
import com.amadeus.xml.tplprr_12_4_1a.MonetaryInformationDetailsType223844C;
import com.amadeus.xml.tplprr_12_4_1a.MonetaryInformationType157202S;
import com.amadeus.xml.ttktir_09_1_1a.DocIssuanceIssueTicketReply;
import com.compassites.GDSWrapper.amadeus.ServiceHandler;
import com.compassites.model.CabinClass;
import com.compassites.model.ErrorMessage;
import com.compassites.model.FlightItinerary;
import com.compassites.model.IssuanceRequest;
import com.compassites.model.IssuanceResponse;
import com.compassites.model.Journey;
import com.compassites.model.LowFareResponse;
import com.compassites.model.PNRResponse;
import com.compassites.model.PassengerTypeCode;
import com.compassites.model.PricingInformation;
import com.compassites.model.traveller.PersonalDetails;
import com.compassites.model.traveller.Traveller;
import com.compassites.model.traveller.TravellerMasterInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.thoughtworks.xstream.XStream;
import utils.HoldTimeUtility;

@Service
public class AmadeusBookingServiceImpl implements BookingService {

	private final String amadeusFlightAvailibilityCode = "OK";

	private final String totalFareIdentifier = "712";

	private final String issuenceOkStatus = "O";

	private final String cappingLimitString = "CT RJT";

    static org.slf4j.Logger amadeusLogger = LoggerFactory.getLogger("amadeus");

    static org.slf4j.Logger logger = LoggerFactory.getLogger("gds");

	@Override
	public PNRResponse generatePNR(TravellerMasterInfo travellerMasterInfo) {
		ServiceHandler serviceHandler = null;
		PNRResponse pnrResponse = new PNRResponse();
		FarePricePNRWithBookingClassReply pricePNRReply = null;
		try {
			serviceHandler = new ServiceHandler();
			serviceHandler.logIn();
			checkFlightAvailibility(travellerMasterInfo, serviceHandler,
					pnrResponse);
			if (pnrResponse.isFlightAvailable()) {

				PNRReply gdsPNRReply = serviceHandler
						.addTravellerInfoToPNR(travellerMasterInfo);
				pricePNRReply = checkPNRPricing(travellerMasterInfo,
						serviceHandler, gdsPNRReply, pricePNRReply, pnrResponse);

				if (!pnrResponse.isPriceChanged()) {
					int numberOfTst = (travellerMasterInfo.isSeamen()) ? 1	: getPassengerTypeCount(travellerMasterInfo.getTravellersList());

					TicketCreateTSTFromPricingReply ticketCreateTSTFromPricingReply = serviceHandler
							.createTST(numberOfTst);
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
					gdsPNRReply = serviceHandler.retrivePNR(tstRefNo);

					// pnrResponse.setPnrNumber(gdsPNRReply.getPnrHeader().get(0).getReservationInfo().getReservation().getControlNumber());
					createPNRResponse(gdsPNRReply, pricePNRReply, pnrResponse,travellerMasterInfo);
					
					// getCancellationFee(travellerMasterInfo, serviceHandler);
				}
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

	public void checkFlightAvailibility(
			TravellerMasterInfo travellerMasterInfo,
			ServiceHandler serviceHandler, PNRResponse pnrResponse) {

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
		pnrResponse.setFlightAvailable(flightAvailable);
	}

	public FarePricePNRWithBookingClassReply checkPNRPricing(
			TravellerMasterInfo travellerMasterInfo,
			ServiceHandler serviceHandler, PNRReply gdsPNRReply,
			FarePricePNRWithBookingClassReply pricePNRReply,
			PNRResponse pnrResponse) {
		String carrierCode = "";
		if (travellerMasterInfo.isSeamen()) {
			carrierCode = travellerMasterInfo.getItinerary().getJourneyList()
					.get(0).getAirSegmentList().get(0).getCarrierCode();
		} else {
			carrierCode = travellerMasterInfo.getItinerary()
					.getNonSeamenJourneyList().get(0).getAirSegmentList()
					.get(0).getCarrierCode();
		}
		pricePNRReply = serviceHandler.pricePNR(carrierCode, gdsPNRReply);
		if(pricePNRReply.getApplicationError() != null) {
			pnrResponse.setFlightAvailable(false);
			return pricePNRReply;
		}
		AmadeusBookingHelper.checkFare(pricePNRReply, pnrResponse,
				travellerMasterInfo, totalFareIdentifier);
        AmadeusBookingHelper.setTaxBreakup(pnrResponse, travellerMasterInfo, pricePNRReply);
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
		FarePricePNRWithBookingClassReply.FareList.LastTktDate.DateTime dateTime = pricePNRReply
				.getFareList().get(0).getLastTktDate().getDateTime();
		String day = ((dateTime.getDay().toString().length() == 1) ? "0"
				+ dateTime.getDay() : dateTime.getDay().toString());
		String month = ((dateTime.getMonth().toString().length() == 1) ? "0"
				+ dateTime.getMonth() : dateTime.getMonth().toString());
		String year = dateTime.getYear().toString();
		// pnrResponse.setValidTillDate(""+dateTime.getDay()+dateTime.getMonth()+dateTime.getYear());
		SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy");
		Date lastTicketingDate = null;
		try {
			lastTicketingDate = sdf.parse(day + month + year);
		} catch (ParseException e) {
			e.printStackTrace();
		}
        if(lastTicketingDate == null){
            Calendar calendar = Calendar.getInstance();
            Date holdDate = HoldTimeUtility.getHoldTime(travellerMasterInfo);
            calendar.setTime(holdDate);
            lastTicketingDate = calendar.getTime();
        }
		pnrResponse.setValidTillDate(lastTicketingDate);
		pnrResponse.setFlightAvailable(true);
		
		// TODO: there is a delay in airline PNR generation.
		pnrResponse.setAirlinePNR("");
		
//		pnrResponse.setTaxDetailsList(AmadeusBookingHelper
//				.getTaxDetails(pricePNRReply));
		return pnrResponse;
	}

	public IssuanceResponse issueTicket(IssuanceRequest issuanceRequest) {
		ServiceHandler serviceHandler = null;
		IssuanceResponse issuanceResponse = new IssuanceResponse();
		issuanceResponse.setPnrNumber(issuanceRequest.getGdsPNR());
		try {
			serviceHandler = new ServiceHandler();
			serviceHandler.logIn();
			PNRReply gdsPNRReply = serviceHandler.retrivePNR(issuanceRequest
					.getGdsPNR());
			String carrierCode = "";
			if (issuanceRequest.isSeamen()) {
				carrierCode = issuanceRequest.getFlightItinerary()
						.getJourneyList().get(0).getAirSegmentList().get(0)
						.getCarrierCode();
			} else {
				carrierCode = issuanceRequest.getFlightItinerary()
						.getNonSeamenJourneyList().get(0).getAirSegmentList()
						.get(0).getCarrierCode();
			}
			FarePricePNRWithBookingClassReply pricePNRReply = serviceHandler
					.pricePNR(carrierCode, gdsPNRReply);

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
			gdsPNRReply = serviceHandler
					.retrivePNR(issuanceRequest.getGdsPNR());
//			XMLFileUtility.createXMLFile(gdsPNRReply, "retrievePNRRes1.xml");
            amadeusLogger.debug("retrievePNRRes1 "+ new Date()+" ------->>"+ new XStream().toXML(gdsPNRReply));
			DocIssuanceIssueTicketReply issuanceIssueTicketReply = serviceHandler
					.issueTicket();
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
			getCancellationFee(issuanceRequest, issuanceResponse,
					serviceHandler);
			logger.debug("");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return issuanceResponse;
	}

	public void getCancellationFee(IssuanceRequest issuanceRequest,
			IssuanceResponse issuanceResponse, ServiceHandler serviceHandler) {
		try {

			// TODO: get right journey list for non seamen
			FareInformativePricingWithoutPNRReply fareInfoReply = serviceHandler
					.getFareInfo(issuanceRequest.getFlightItinerary()
							.getJourneyList(), issuanceRequest.getAdultCount(),
							issuanceRequest.getChildCount(), issuanceRequest
									.getInfantCount());

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
		ServiceHandler serviceHandler = null;
		PNRResponse pnrResponse = new PNRResponse();
		FarePricePNRWithBookingClassReply pricePNRReply = null;
		try {
			serviceHandler = new ServiceHandler();
			serviceHandler.logIn();
			checkFlightAvailibility(travellerMasterInfo, serviceHandler,
					pnrResponse);
			if (pnrResponse.isFlightAvailable()) {
				PNRReply gdsPNRReply = serviceHandler
						.addTravellerInfoToPNR(travellerMasterInfo);
				pricePNRReply = checkPNRPricing(travellerMasterInfo,
						serviceHandler, gdsPNRReply, pricePNRReply, pnrResponse);
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
            String carrierCode = "";
            if (issuanceRequest.isSeamen()) {
                carrierCode = flightItinerary.getJourneyList()
                        .get(0).getAirSegmentList().get(0).getCarrierCode();
            } else {
                carrierCode = flightItinerary.getNonSeamenJourneyList().get(0).getAirSegmentList()
                        .get(0).getCarrierCode();
            }
            FarePricePNRWithBookingClassReply pricePNRReply = serviceHandler.pricePNR(carrierCode, gdsPNRReply);
            PricingInformation pricingInformation = AmadeusBookingHelper.getPricingForDownsellAndConversion(pricePNRReply,totalFareIdentifier, issuanceRequest.getAdultCount());
            if(isSeamen){
                flightItinerary.setSeamanPricingInformation(pricingInformation);
            }else {
                flightItinerary.setPricingInformation(pricingInformation);
            }

			masterInfo.setItinerary(flightItinerary);

			masterInfo.setTravellersList(issuanceResponse.getTravellerList());
            getCancellationFee(issuanceRequest,issuanceResponse,serviceHandler);
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
		} catch (Exception e) {
			// TODO Auto-generated catch block
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
		try {
			ServiceHandler serviceHandler = new ServiceHandler();
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
				personalDetails.setFirstName(firstName);
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
			pricePNRReply = serviceHandler.pricePNR(carrierCode, gdsPNRReply);
			pricingInfo = AmadeusBookingHelper.getPricingInfo(pricePNRReply, totalFareIdentifier,
							paxTypeCount.get("adultCount"),	paxTypeCount.get("childCount"),	paxTypeCount.get("infantCount"));
			if (isSeamen) {
				flightItinerary.setSeamanPricingInformation(pricingInfo);
			} else {
				flightItinerary.setPricingInformation(pricingInfo);
			}
			masterInfo.setSeamen(isSeamen);
			masterInfo.setItinerary(flightItinerary);
		} catch (Exception e) {
			e.printStackTrace();
		}
		// TODO: change hardcoded value
		masterInfo.setCabinClass(CabinClass.ECONOMY);

		PNRResponse pnrResponse = new PNRResponse();
		pnrResponse.setPnrNumber(gdsPNR);
		pnrResponse.setPricingInfo(pricingInfo);
		
		List<ItineraryInfo> itineraryInfos = gdsPNRReply.getOriginDestinationDetails().get(0).getItineraryInfo();
		if(itineraryInfos != null && itineraryInfos.size() > 0) {
			String airlinePnr = itineraryInfos.get(0).getItineraryReservationInfo().getReservation().getControlNumber();
			pnrResponse.setAirlinePNR(airlinePnr);
		}
		gdsPNRReply.getOriginDestinationDetails().get(0).getItineraryInfo().get(0).getItineraryReservationInfo().getReservation().getControlNumber();
		createPNRResponse(gdsPNRReply, pricePNRReply, pnrResponse, masterInfo);

		Map<String, Object> json = new HashMap<>();
		json.put("travellerMasterInfo", masterInfo);
		json.put("pnrResponse", pnrResponse);
		
		return Json.toJson(json);
	}

}