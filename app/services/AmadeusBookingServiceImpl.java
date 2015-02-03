package services;

import com.amadeus.xml.farqnr_07_1_1a.FareCheckRulesReply;
import com.amadeus.xml.itares_05_2_ia.AirSellFromRecommendationReply;
import com.amadeus.xml.pnracc_10_1_1a.PNRReply;
import com.amadeus.xml.pnracc_10_1_1a.PNRReply.OriginDestinationDetails;
import com.amadeus.xml.pnracc_10_1_1a.PNRReply.OriginDestinationDetails.ItineraryInfo;
import com.amadeus.xml.pnracc_10_1_1a.PNRReply.TravellerInfo;
import com.amadeus.xml.pnracc_10_1_1a.PNRReply.TravellerInfo.PassengerData;
import com.amadeus.xml.pnracc_10_1_1a.PNRReply.TravellerInfo.PassengerData.TravellerInformation;
import com.amadeus.xml.pnradd_10_1_1a.PNRAddMultiElements.TravellerInfo.PassengerData.TravellerInformation.Passenger;
import com.amadeus.xml.tautcr_04_1_1a.TicketCreateTSTFromPricingReply;
import com.amadeus.xml.tipnrr_12_4_1a.FareInformativePricingWithoutPNRReply;
import com.amadeus.xml.tpcbrr_07_3_1a.FarePricePNRWithBookingClassReply;
import com.amadeus.xml.tpcbrr_07_3_1a.FarePricePNRWithBookingClassReply.FareList;
import com.amadeus.xml.tpcbrr_07_3_1a.FarePricePNRWithBookingClassReply.FareList.SegmentInformation;
import com.amadeus.xml.tpcbrr_07_3_1a.FarePricePNRWithBookingClassReply.FareList.TaxInformation;
import com.amadeus.xml.ttktir_09_1_1a.DocIssuanceIssueTicketReply;
import com.compassites.GDSWrapper.amadeus.ServiceHandler;
import com.compassites.model.AirSegmentInformation;
import com.compassites.model.ErrorMessage;
import com.compassites.model.FlightInfo;
import com.compassites.model.FlightItinerary;
import com.compassites.model.IssuanceRequest;
import com.compassites.model.IssuanceResponse;
import com.compassites.model.Journey;
import com.compassites.model.PNRResponse;
import com.compassites.model.PassengerTax;
import com.compassites.model.PassengerTypeCode;
import com.compassites.model.PricingInformation;
import com.compassites.model.traveller.PersonalDetails;
import com.compassites.model.traveller.Traveller;
import com.compassites.model.traveller.TravellerMasterInfo;

import models.Airline;
import models.Airport;

import org.springframework.stereotype.Service;

import play.libs.Json;
import utils.AmadeusBookingHelper;
import utils.ErrorMessageHelper;
import utils.XMLFileUtility;
import utils.DateUtility;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

			/*
			 * AirSellFromRecommendationReply sellFromRecommendation =
			 * serviceHandler.checkFlightAvailability(travellerMasterInfo);
			 * 
			 * if(sellFromRecommendation.getErrorAtMessageLevel() != null &&
			 * sellFromRecommendation.getErrorAtMessageLevel().size() > 0 &&
			 * (sellFromRecommendation.getItineraryDetails() == null)){
			 * 
			 * ErrorMessage errorMessage =
			 * ErrorMessageHelper.createErrorMessage("error",
			 * ErrorMessage.ErrorType.ERROR, "Amadeus");
			 * pnrResponse.setErrorMessage(errorMessage); }
			 * 
			 * boolean flightAvailable =
			 * validateFlightAvailability(sellFromRecommendation);
			 */

			checkFlightAvailibility(travellerMasterInfo, serviceHandler,
					pnrResponse);

			if (pnrResponse.isFlightAvailable()) {

				PNRReply gdsPNRReply = serviceHandler
						.addTravellerInfoToPNR(travellerMasterInfo);

				/*
				 * String carrierCode = ""; if(travellerMasterInfo.isSeamen()) {
				 * carrierCode =
				 * travellerMasterInfo.getItinerary().getJourneyList
				 * ().get(0).getAirSegmentList().get(0).getCarrierCode(); } else
				 * { carrierCode =
				 * travellerMasterInfo.getItinerary().getNonSeamenJourneyList
				 * ().get(0).getAirSegmentList().get(0).getCarrierCode(); }
				 * pricePNRReply = serviceHandler.pricePNR(carrierCode,
				 * gdsPNRReply);
				 * 
				 * pnrResponse = checkFare(pricePNRReply, travellerMasterInfo);
				 */

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
		AmadeusBookingHelper.checkFare(pricePNRReply, pnrResponse,
				travellerMasterInfo, totalFareIdentifier);
		PricingInformation pi = setTaxBreakup(pnrResponse, travellerMasterInfo, pricePNRReply);
		pnrResponse.setPricingInfo(pi);
		return pricePNRReply;
	}
	
	private PricingInformation setTaxBreakup(PNRResponse pnrResponse, TravellerMasterInfo travellerMasterInfo,
			FarePricePNRWithBookingClassReply pricePNRReply) {
		int adultCount = 0, childCount = 0, infantCount = 0;
		for (Traveller traveller : travellerMasterInfo.getTravellersList()) {
			PassengerTypeCode passengerType = DateUtility.getPassengerTypeFromDOB(traveller.getPassportDetails().getDateOfBirth());
			if (passengerType.equals(PassengerTypeCode.ADT)) {
				adultCount++;
			} else if (passengerType.equals(PassengerTypeCode.CHD)) {
				childCount++;
			} else {
				infantCount++;
			}
		}
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
				if(paxType.equalsIgnoreCase("ADT") || paxType.equalsIgnoreCase("SEA")) {
					passengerTax.setPassengerType("ADT");
					passengerTax.setPassengerCount(adultCount);
					for(TaxInformation taxInfo : taxInfos) {
						String amount = taxInfo.getAmountDetails().getFareDataMainInformation().getFareAmount();
						taxes.put(taxInfo.getTaxDetails().getTaxType().getIsoCountry(), new BigDecimal(amount));
					}
				} else if(paxType.equalsIgnoreCase("CH") || paxType.equalsIgnoreCase("CHD")) {
					passengerTax.setPassengerType("CHD");
					passengerTax.setPassengerCount(childCount);
					for(TaxInformation taxInfo : taxInfos) {
						String amount = taxInfo.getAmountDetails().getFareDataMainInformation().getFareAmount();
						taxes.put(taxInfo.getTaxDetails().getTaxType().getIsoCountry(), new BigDecimal(amount));
					}
				} else if(paxType.equalsIgnoreCase("IN") || paxType.equalsIgnoreCase("INF")) {
					passengerTax.setPassengerType("INF");
					passengerTax.setPassengerCount(infantCount);
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
		return pricingInfo;
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
					System.out
							.println("Send Email to operator saying capping limit is reached");
					issuanceResponse.setCappingLimitReached(true);
				}
			}

			getCancellationFee(issuanceRequest, issuanceResponse,
					serviceHandler);
			System.out.println("");
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("Issuance Response ========>>"+Json.toJson(issuanceResponse));
		return issuanceResponse;
	}

	public void getCancellationFee(IssuanceRequest issuanceRequest,
			IssuanceResponse issuanceResponse, ServiceHandler serviceHandler) {
		// ServiceHandler serviceHandler = null;
		try {
			// serviceHandler = new ServiceHandler();
			// serviceHandler.logIn();
			/*
			 * int adultCount = 0,childCount = 0,infantCount = 0; for(Traveller
			 * traveller : travellerMasterInfo.getTravellersList()){
			 * PassengerTypeCode passengerType =
			 * DateUtility.getPassengerTypeFromDOB
			 * (traveller.getPersonalDetails().getDateOfBirth());
			 * if(passengerType.equals(PassengerTypeCode.ADT)){ adultCount++; }
			 * else if(passengerType.equals(PassengerTypeCode.CHD)){
			 * childCount++; } else { infantCount++; } }
			 */

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
			/*System.out
					.println("---------------------------------------Fare Rules------------------------------------\n"
							+ fareRule.toString());*/
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
		
		///System.out.println(isSeamen + "my issuance obj is =========>>>>\n"+ Json.toJson(issuanceRequest));

		try {
			ServiceHandler serviceHandler = new ServiceHandler();
			serviceHandler.logIn();

			PNRReply gdsPNRReply = serviceHandler.retrivePNR(gdsPNR);
			AmadeusBookingHelper.createTickets(issuanceResponse, issuanceRequest, gdsPNRReply);
			
			System.out.println("retrivePNR ===================================>>>>>>>>>>>>>>>>>>>>>>>>>"
					+ "\n"+Json.toJson(gdsPNRReply));
			List<Journey> journeyList = new ArrayList<>();
			List<AirSegmentInformation> airSegmentList = new ArrayList<>();
			FlightItinerary flightItinerary = new FlightItinerary();
			
			Journey journey = new Journey();
			for (OriginDestinationDetails originDestinationDetails : gdsPNRReply.getOriginDestinationDetails()) {
				for (ItineraryInfo itineraryInfo : originDestinationDetails.getItineraryInfo()) {
					SimpleDateFormat format = new SimpleDateFormat("ddMMyy");
					AirSegmentInformation airSegmentInformation = new AirSegmentInformation();
					
					Date fromDate = format.parse(itineraryInfo.getTravelProduct().getProduct().getArrDate());
					Date toDate = format.parse(itineraryInfo.getTravelProduct().getProduct().getDepDate());
					
					airSegmentInformation.setFlightNumber(itineraryInfo.getTravelProduct().getProductDetails().getIdentification());
					
					airSegmentInformation.setDepartureDate(toDate);
					airSegmentInformation.setDepartureTime(itineraryInfo.getTravelProduct().getProduct().getDepTime());
					
					
					airSegmentInformation.setArrivalDate(fromDate);
					airSegmentInformation.setArrivalTime(itineraryInfo.getTravelProduct().getProduct().getArrTime());
					
					airSegmentInformation.setFromDate(itineraryInfo.getTravelProduct().getProduct().getArrDate());
					airSegmentInformation.setToDate(itineraryInfo.getTravelProduct().getProduct().getDepDate());
					
					String fromLoc = itineraryInfo.getTravelProduct().getBoardpointDetail().getCityCode();
					String toLoc = itineraryInfo.getTravelProduct().getOffpointDetail().getCityCode();
					airSegmentInformation.setFromLocation(fromLoc);
					airSegmentInformation.setToLocation(toLoc);
					
					//String arrivalTer = itineraryInfo.getFlightDetail().getArrivalStationInfo().getTerminal();
					if(itineraryInfo.getFlightDetail().getDepartureInformation() != null ){
						airSegmentInformation.setFromTerminal(itineraryInfo.getFlightDetail().getArrivalStationInfo().getTerminal());
					}
					if(itineraryInfo.getFlightDetail().getArrivalStationInfo() != null){
						airSegmentInformation.setToTerminal(itineraryInfo.getFlightDetail().getArrivalStationInfo().getTerminal());
					}
					
					
					airSegmentInformation.setAirline(Airline.getAirlineByCode(itineraryInfo.getTravelProduct().getCompanyDetail().getIdentification()));
					airSegmentInformation.setFromAirport(Airport.getAiport(fromLoc));
					airSegmentInformation.setToAirport(Airport.getAiport(toLoc));
					airSegmentInformation.setEquipment(itineraryInfo.getFlightDetail().getProductDetails().getEquipment());

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
			
			
			//System.out.println("=========================AMADEUS RESPONSE================================================\n"+Json.toJson(gdsPNRReply));
			System.out.println("====== masterInfo details =========="+ Json.toJson(masterInfo));
		} catch (Exception e) {
			e.printStackTrace();
		}
	
		return masterInfo;
	}
}