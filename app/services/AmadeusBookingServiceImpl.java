package services;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import models.Airline;
import models.Airport;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.stereotype.Service;

import play.libs.Json;
import utils.AmadeusBookingHelper;
import utils.DateUtility;
import utils.ErrorMessageHelper;
import utils.XMLFileUtility;

import com.amadeus.xml.farqnr_07_1_1a.FareCheckRulesReply;
import com.amadeus.xml.itares_05_2_ia.AirSellFromRecommendationReply;
import com.amadeus.xml.pnracc_10_1_1a.PNRReply;
import com.amadeus.xml.pnracc_10_1_1a.PNRReply.DataElementsMaster.DataElementsIndiv;
import com.amadeus.xml.pnracc_10_1_1a.PNRReply.OriginDestinationDetails;
import com.amadeus.xml.pnracc_10_1_1a.PNRReply.OriginDestinationDetails.ItineraryInfo;
import com.amadeus.xml.tautcr_04_1_1a.TicketCreateTSTFromPricingReply;
import com.amadeus.xml.tipnrr_12_4_1a.FareInformativePricingWithoutPNRReply;
import com.amadeus.xml.tpcbrr_07_3_1a.FarePricePNRWithBookingClassReply;
import com.amadeus.xml.tpcbrr_07_3_1a.FarePricePNRWithBookingClassReply.FareList;
import com.amadeus.xml.tpcbrr_07_3_1a.FarePricePNRWithBookingClassReply.FareList.SegmentInformation;
import com.amadeus.xml.tpcbrr_07_3_1a.FarePricePNRWithBookingClassReply.FareList.TaxInformation;
import com.amadeus.xml.tplprr_12_4_1a.FarePricePNRWithLowestFareReply;
import com.amadeus.xml.tplprr_12_4_1a.MonetaryInformationDetailsType223844C;
import com.amadeus.xml.ttktir_09_1_1a.DocIssuanceIssueTicketReply;
import com.compassites.GDSWrapper.amadeus.ServiceHandler;
import com.compassites.model.AirSegmentInformation;
import com.compassites.model.ErrorMessage;
import com.compassites.model.FlightItinerary;
import com.compassites.model.IssuanceRequest;
import com.compassites.model.IssuanceResponse;
import com.compassites.model.Journey;
import com.compassites.model.LowestFare;
import com.compassites.model.PNRResponse;
import com.compassites.model.PassengerTax;
import com.compassites.model.PassengerTypeCode;
import com.compassites.model.PricingInformation;
import com.compassites.model.traveller.Traveller;
import com.compassites.model.traveller.TravellerMasterInfo;

@Service
public class AmadeusBookingServiceImpl implements BookingService {

	private final String amadeusFlightAvailibilityCode = "OK";

	private final String totalFareIdentifier = "712";

	private final String issuenceOkStatus = "O";

	private final String cappingLimitString = "CT RJT";

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
					int numberOfTst = (travellerMasterInfo.isSeamen()) ? 1
							: AmadeusBookingHelper
									.getPassengerTypeCount(travellerMasterInfo
											.getTravellersList());

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
					createPNRResponse(gdsPNRReply, pricePNRReply, pnrResponse);
					
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
		setTaxBreakup(pnrResponse, travellerMasterInfo, pricePNRReply);
		return pricePNRReply;
	}
	
	private void setTaxBreakup(PNRResponse pnrResponse, TravellerMasterInfo travellerMasterInfo,
			FarePricePNRWithBookingClassReply pricePNRReply) {
		Map<String, Integer> passengerTypeMap = getPassengerTypeCount(travellerMasterInfo.getTravellersList());
		PricingInformation pricingInfo = travellerMasterInfo.isSeamen() ? travellerMasterInfo
				.getItinerary().getSeamanPricingInformation() : travellerMasterInfo
				.getItinerary().getPricingInformation();
		List<PassengerTax> passengerTaxes = new ArrayList<>();
		for (FareList fare : pricePNRReply.getFareList()) {
			SegmentInformation segmentInfo = fare.getSegmentInformation().get(0);
			List<TaxInformation> taxInfos = fare.getTaxInformation();
			if (segmentInfo != null && taxInfos.size() > 0) {
				PassengerTax passengerTax = new PassengerTax();
				String paxType = segmentInfo.getFareQualifier().getFareBasisDetails().getDiscTktDesignator();
				Map<String, BigDecimal> taxes = new HashMap<>();
				if(paxType.equalsIgnoreCase("ADT") || paxType.equalsIgnoreCase("SEA") || paxType.equalsIgnoreCase("SC")) {
					passengerTax.setPassengerType("ADT");
					passengerTax.setPassengerCount(passengerTypeMap.get("adultCount"));
					for(TaxInformation taxInfo : taxInfos) {
						String amount = taxInfo.getAmountDetails().getFareDataMainInformation().getFareAmount();
						taxes.put(taxInfo.getTaxDetails().getTaxType().getIsoCountry(), new BigDecimal(amount));
					}
				} else if(paxType.equalsIgnoreCase("CH") || paxType.equalsIgnoreCase("CHD")) {
					passengerTax.setPassengerType("CHD");
					passengerTax.setPassengerCount(passengerTypeMap.get("childCount"));
					for(TaxInformation taxInfo : taxInfos) {
						String amount = taxInfo.getAmountDetails().getFareDataMainInformation().getFareAmount();
						taxes.put(taxInfo.getTaxDetails().getTaxType().getIsoCountry(), new BigDecimal(amount));
					}
				} else if(paxType.equalsIgnoreCase("IN") || paxType.equalsIgnoreCase("INF")) {
					passengerTax.setPassengerType("INF");
					passengerTax.setPassengerCount(passengerTypeMap.get("infantCount"));
					for(TaxInformation taxInfo : taxInfos) {
						String amount = taxInfo.getAmountDetails().getFareDataMainInformation().getFareAmount();
						taxes.put(taxInfo.getTaxDetails().getTaxType().getIsoCountry(), new BigDecimal(amount));
					}
				}
				passengerTax.setTaxes(taxes);
				passengerTaxes.add(passengerTax);
			}
		}
		pricingInfo.setPassengerTaxes(passengerTaxes);
		pnrResponse.setPricingInfo(pricingInfo);
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
			FarePricePNRWithBookingClassReply pricePNRReply, PNRResponse pnrResponse) {
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
					: AmadeusBookingHelper
							.getPassengerTypeCount(issuanceRequest
									.getTravellerList());

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
			XMLFileUtility.createXMLFile(gdsPNRReply, "retrievePNRRes1.xml");
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
					System.out.println("Send Email to operator saying capping limit is reached");
					issuanceResponse.setCappingLimitReached(true);
				}
			}
			getCancellationFee(issuanceRequest, issuanceResponse,
					serviceHandler);
			System.out.println("");
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
			System.out
			.println("retrivePNR ===================================>>>>>>>>>>>>>>>>>>>>>>>>>"
					+ "\n" + Json.toJson(gdsPNRReply));
			Set<String> isTicketContainSet = new HashSet<String>();
			for (DataElementsIndiv isticket : gdsPNRReply.getDataElementsMaster().getDataElementsIndiv()) {
				String isTicketIssued = isticket.getElementManagementData().getSegmentName();
				/*System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<DataElementsIndiv>>>>>>>>>>>>>>>>>>>>>>>>"+Json.toJson(isticket.getElementManagementData().getSegmentName()));*/
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
			/*System.out.println("SET>>>>>>>>>>>>>>>>>>>"+Json.toJson(isTicketContainSet));*/
			for (String isFA : isTicketContainSet) {
				if(isFA.equalsIgnoreCase("FA")){
					AmadeusBookingHelper.createTickets(issuanceResponse,
							issuanceRequest, gdsPNRReply);
					break;
				} else {
					return masterInfo; 
				}
			}
			/*System.out
					.println("retrivePNR ===================================>>>>>>>>>>>>>>>>>>>>>>>>>"
							+ "\n" + Json.toJson(gdsPNRReply));*/
			List<Journey> journeyList = new ArrayList<>();
			List<AirSegmentInformation> airSegmentList = new ArrayList<>();
			FlightItinerary flightItinerary = new FlightItinerary();

			Journey journey = new Journey();
			for (OriginDestinationDetails originDestinationDetails : gdsPNRReply
					.getOriginDestinationDetails()) {
				for (ItineraryInfo itineraryInfo : originDestinationDetails
						.getItineraryInfo()) {
					AirSegmentInformation airSegmentInformation = new AirSegmentInformation();

					String fromLoc = itineraryInfo.getTravelProduct()
							.getBoardpointDetail().getCityCode();
					String toLoc = itineraryInfo.getTravelProduct()
							.getOffpointDetail().getCityCode();
					airSegmentInformation.setFromLocation(fromLoc);
					airSegmentInformation.setToLocation(toLoc);
					airSegmentInformation.setBookingClass(itineraryInfo.getTravelProduct().getProductDetails().getClassOfService());
					Airport fromAirport = Airport
							.getAiport(airSegmentInformation.getFromLocation());
					Airport toAirport = Airport.getAiport(airSegmentInformation
							.getToLocation());

					SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyHHmm");
					String DATE_FORMAT = "ddMMyyHHmm";
					DateTimeFormatter DATETIME_FORMATTER = DateTimeFormat
							.forPattern(DATE_FORMAT);
					DateTimeZone dateTimeZone = DateTimeZone.forID(fromAirport
							.getTime_zone());
					String fromDateTime = itineraryInfo.getTravelProduct()
							.getProduct().getArrDate()
							+ itineraryInfo.getTravelProduct().getProduct()
									.getArrTime();
					String toDateTime = itineraryInfo.getTravelProduct()
							.getProduct().getDepDate()
							+ itineraryInfo.getTravelProduct().getProduct()
									.getDepTime();
					DateTime departureDate = DATETIME_FORMATTER.withZone(
							dateTimeZone).parseDateTime(toDateTime);
					dateTimeZone = DateTimeZone.forID(toAirport.getTime_zone());
					DateTime arrivalDate = DATETIME_FORMATTER.withZone(
							dateTimeZone).parseDateTime(fromDateTime);

					/*
					 * Date fromDate =
					 * format.parse(itineraryInfo.getTravelProduct
					 * ().getProduct().getArrDate()); Date toDate =
					 * format.parse(
					 * itineraryInfo.getTravelProduct().getProduct()
					 * .getDepDate());
					 */

					airSegmentInformation.setFlightNumber(itineraryInfo
							.getTravelProduct().getProductDetails()
							.getIdentification());

					// airSegmentInformation.setDepartureDate(toDate);
					airSegmentInformation.setDepartureDate(departureDate
							.toDate());
					airSegmentInformation.setDepartureTime(departureDate
							.toString());
					airSegmentInformation
							.setArrivalTime(arrivalDate.toString());
					airSegmentInformation.setArrivalDate(arrivalDate.toDate());

					// System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>\n"+fromDateTime);
					// System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>\n"+toDateTime);
					airSegmentInformation.setFromDate(fromDateTime);
					airSegmentInformation.setToDate(toDateTime);
					/*
					 * airSegmentInformation.setFromDate(format.format(fromDateTime
					 * ));
					 * airSegmentInformation.setToDate(format.format(toDateTime
					 * ));
					 */

					// String arrivalTer =
					// itineraryInfo.getFlightDetail().getArrivalStationInfo().getTerminal();
					if (itineraryInfo.getFlightDetail() != null
							&& itineraryInfo.getFlightDetail()
									.getArrivalStationInfo() != null) {
						airSegmentInformation.setFromTerminal(itineraryInfo
								.getFlightDetail().getArrivalStationInfo()
								.getTerminal());
					}
					if (itineraryInfo.getFlightDetail() != null
							&& itineraryInfo.getFlightDetail()
									.getDepartureInformation() != null) {
						airSegmentInformation.setToTerminal(itineraryInfo
								.getFlightDetail().getDepartureInformation()
								.getDepartTerminal());
					}

					airSegmentInformation.setAirline(Airline
							.getAirlineByCode(itineraryInfo.getTravelProduct()
									.getCompanyDetail().getIdentification()));
					airSegmentInformation.setFromAirport(Airport
							.getAiport(fromLoc));
					airSegmentInformation
							.setToAirport(Airport.getAiport(toLoc));
					String responseEquipment = itineraryInfo.getFlightDetail()
							.getProductDetails().getEquipment();
					if (responseEquipment != null) {
						itineraryInfo.getFlightDetail().getProductDetails()
								.getEquipment();
					}
					airSegmentInformation.setEquipment(responseEquipment);

					airSegmentList.add(airSegmentInformation);
				}
				journey.setAirSegmentList(airSegmentList);
				journeyList.add(journey);
			}
			if (isSeamen) {
				flightItinerary.setJourneyList(journeyList);
			} else {
				flightItinerary.setNonSeamenJourneyList(journeyList);
			}
			masterInfo.setItinerary(flightItinerary);

			masterInfo.setTravellersList(issuanceResponse.getTravellerList());

			// System.out.println("=========================AMADEUS RESPONSE================================================\n"+Json.toJson(gdsPNRReply));
			// System.out.println("====== masterInfo details =========="+
			// Json.toJson(masterInfo));
		} catch (Exception e) {
			e.printStackTrace();
		}

		return masterInfo;
	}
	
	public LowestFare getLowestFare(TravellerMasterInfo travellerMasterInfo) {
		LowestFare lowestFare = new LowestFare();
		ServiceHandler serviceHandler = null;
		try {
			serviceHandler = new ServiceHandler();
			serviceHandler.logIn();
//			serviceHandler.checkFlightAvailability(travellerMasterInfo);
//			PNRReply gdsPNRReply = serviceHandler.addTravellerInfoToPNR(travellerMasterInfo);
			String carrierCode = "";
			if (travellerMasterInfo.isSeamen()) {
				carrierCode = travellerMasterInfo.getItinerary().getJourneyList()
						.get(0).getAirSegmentList().get(0).getCarrierCode();
			} else {
				carrierCode = travellerMasterInfo.getItinerary()
						.getNonSeamenJourneyList().get(0).getAirSegmentList()
						.get(0).getCarrierCode();
			}
//			serviceHandler.pricePNR(carrierCode, gdsPNRReply);
			
			serviceHandler.retrivePNR("3KX8H4");
			FarePricePNRWithLowestFareReply farePricePNRWithLowestFareReply = serviceHandler.getLowestFare(carrierCode);
			Map<String, Integer> passengerTypeCountMap = getPassengerTypeCount(travellerMasterInfo.getTravellersList());
			
			BigDecimal totalFare = new BigDecimal(0);
	        for(com.amadeus.xml.tplprr_12_4_1a.FarePricePNRWithLowestFareReply.FareList fare : farePricePNRWithLowestFareReply.getFareList()) {
	        	int paxCount = 0;
	        	String paxType = fare.getSegmentInformation().get(0).getFareQualifier().getFareBasisDetails().getDiscTktDesignator();
	        	if(paxType.equalsIgnoreCase("ADT") || paxType.equalsIgnoreCase("SEA") || paxType.equalsIgnoreCase("SC")) {
	        		paxCount = passengerTypeCountMap.get("adultCount");
	        	} else if(paxType.equalsIgnoreCase("CHD") || paxType.equalsIgnoreCase("CH")) {
	        		paxCount = passengerTypeCountMap.get("childCount");
	        	} else if(paxType.equalsIgnoreCase("INF") || paxType.equalsIgnoreCase("IN")) {
	        		paxCount = passengerTypeCountMap.get("infantCount");
	        	}
	        	for(MonetaryInformationDetailsType223844C fareData : fare.getFareDataInformation().getFareDataSupInformation()) {
	        		BigDecimal amount = new BigDecimal(fareData.getFareAmount());
	        		if(totalFareIdentifier.equals(fareData.getFareDataQualifier())) {
	        			totalFare = totalFare.add(amount.multiply(new BigDecimal(paxCount)));
	        		}
	        	}
	        }
			lowestFare.setAmount(totalFare);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return lowestFare;
	}
	
	private Map<String, Integer> getPassengerTypeCount(List<Traveller> travellerList) {
		int adultCount = 0, childCount = 0, infantCount = 0;
		Map<String, Integer> passengerTypeMap = new HashMap<>();
		for (Traveller traveller : travellerList) {
			PassengerTypeCode passengerType = DateUtility.getPassengerTypeFromDOB(traveller.getPassportDetails().getDateOfBirth());
			if (passengerType.equals(PassengerTypeCode.ADT)) {
				adultCount++;
			} else if (passengerType.equals(PassengerTypeCode.CHD)) {
				childCount++;
			} else {
				infantCount++;
			}
		}
		passengerTypeMap.put("adultCount", adultCount);
		passengerTypeMap.put("childCount", childCount);
		passengerTypeMap.put("infantCount", infantCount);
		return passengerTypeMap;
	}
	
}