package services;

import com.amadeus.xml.farqnr_07_1_1a.FareCheckRulesReply;
import com.amadeus.xml.itares_05_2_ia.AirSellFromRecommendationReply;
import com.amadeus.xml.pnracc_10_1_1a.PNRReply;
import com.amadeus.xml.pnracc_10_1_1a.PNRReply.OriginDestinationDetails.ItineraryInfo;
import com.amadeus.xml.pnracc_10_1_1a.PNRReply.TravellerInfo;
import com.amadeus.xml.pnracc_10_1_1a.PNRReply.TravellerInfo.PassengerData;
import com.amadeus.xml.pnracc_10_1_1a.PNRReply.TravellerInfo.PassengerData.TravellerInformation;
import com.amadeus.xml.pnradd_10_1_1a.PNRAddMultiElements.TravellerInfo.PassengerData.TravellerInformation.Passenger;
import com.amadeus.xml.tautcr_04_1_1a.TicketCreateTSTFromPricingReply;
import com.amadeus.xml.tipnrr_12_4_1a.FareInformativePricingWithoutPNRReply;
import com.amadeus.xml.tpcbrr_07_3_1a.FarePricePNRWithBookingClassReply;
import com.amadeus.xml.ttktir_09_1_1a.DocIssuanceIssueTicketReply;
import com.compassites.GDSWrapper.amadeus.ServiceHandler;
import com.compassites.model.AirSegmentInformation;
import com.compassites.model.ErrorMessage;
import com.compassites.model.FlightItinerary;
import com.compassites.model.IssuanceRequest;
import com.compassites.model.IssuanceResponse;
import com.compassites.model.Journey;
import com.compassites.model.PNRResponse;
import com.compassites.model.traveller.PersonalDetails;
import com.compassites.model.traveller.Traveller;
import com.compassites.model.traveller.TravellerMasterInfo;

import org.springframework.stereotype.Service;

import play.libs.Json;
import utils.AmadeusBookingHelper;
import utils.ErrorMessageHelper;
import utils.XMLFileUtility;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

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
					pnrResponse = createPNRResponse(gdsPNRReply, pricePNRReply);

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

		pnrResponse = AmadeusBookingHelper.checkFare(pricePNRReply,
				travellerMasterInfo, totalFareIdentifier);

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
			FarePricePNRWithBookingClassReply pricePNRReply) {
		PNRResponse pnrResponse = new PNRResponse();

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
		pnrResponse.setTaxDetailsList(AmadeusBookingHelper
				.getTaxDetails(pricePNRReply));
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
			System.out
					.println("---------------------------------------Fare Rules------------------------------------\n"
							+ fareRule.toString());
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

	public IssuanceResponse allPNRDetails(IssuanceRequest issuanceRequest,
			String gdsPNR) {
		IssuanceResponse issuanceResponse = new IssuanceResponse();

		List<FlightItinerary> flightItineraryList = new ArrayList<>();
		List<Journey> journeyList = new ArrayList<>();
		List<AirSegmentInformation> airsegmentationList = new ArrayList<>();
		List<Traveller> travellersList = new ArrayList<>();

		boolean isSeamen = issuanceRequest.isSeamen();
		///System.out.println(isSeamen + "my issuance obj is =========>>>>\n"+ Json.toJson(issuanceRequest));

		try {
			TravellerMasterInfo masterInfo = new TravellerMasterInfo();
			FlightItinerary flightItinerary = new FlightItinerary();

			ServiceHandler serviceHandler = new ServiceHandler();
			serviceHandler.logIn();

			PNRReply gdsPNRReply = serviceHandler.retrivePNR(gdsPNR);
			/* for PNR number */

			for (TravellerInfo travellerInfo : gdsPNRReply.getTravellerInfo()) {
				Traveller traveller = new Traveller();
				for (PassengerData passengerData : travellerInfo.getPassengerData()) {
					PersonalDetails personalDetails = new PersonalDetails();

					personalDetails.setLastName(passengerData
							.getTravellerInformation().getTraveller()
							.getSurname());
					for (TravellerInformation.Passenger passenger : passengerData
							.getTravellerInformation().getPassenger()) {
						String fullNamegds = passenger.getFirstName();
						String[] fullNames = fullNamegds.split("\\s+");
						// System.out.println(Arrays.toString(fullNames)+"<<<<<<==========");
						personalDetails.setFirstName(fullNames[0]);
						personalDetails.setMiddleName(fullNames[1]);

					}
					//System.out.println("====== Personal details =========="+ Json.toJson(personalDetails));
					traveller.setPersonalDetails(personalDetails);
					//System.out.println("====== traveller details =========="+ Json.toJson(traveller));
				}
				travellersList.add(traveller);
				masterInfo.setTravellersList(travellersList);
			}

			AmadeusBookingHelper.createTickets(issuanceResponse,
					issuanceRequest, gdsPNRReply);
			System.out.println(Json.toJson(issuanceResponse)+"<<<<<<<========== Issuence response\n");
			for (PNRReply.PnrHeader pnrHeader : gdsPNRReply.getPnrHeader()) {
				issuanceResponse.setPnrNumber(pnrHeader.getReservationInfo()
						.getReservation().getControlNumber());
			}
			// System.out.println(issuanceRequest.getProvider()+"=============GDS-PNR-REPLY================>>>>>>>>\n"+Json.toJson(gdsPNRReply));

			for (PNRReply.OriginDestinationDetails details : gdsPNRReply.getOriginDestinationDetails()) {
				// List<AirSegmentInformation> airsegmentList = new ArrayList<>();
				int size = 0;
				Journey journey = new Journey();

				for (ItineraryInfo itineraryInfo : details.getItineraryInfo()) {
					size++;
					// String toTerminal =
					// itineraryInfo.getFlightDetail().getArrivalStationInfo().getTerminal();
					AirSegmentInformation airSegmentInformation = new AirSegmentInformation();
					// /(details.getItineraryInfo().size() == size) ?
					// airSegmentInformation.setToTerminal(itineraryInfo.getFlightDetail().getArrivalStationInfo().getTerminal())
					// :
					// airSegmentInformation.setFromTerminal(itineraryInfo.getFlightDetail().getArrivalStationInfo().getTerminal());
					//String checkValue = itineraryInfo.getFlightDetail().getArrivalStationInfo().getTerminal();
					if (details.getItineraryInfo().size() == size) {
						if (!isSeamen) {
							airSegmentInformation.setToTerminal(itineraryInfo.getFlightDetail().getArrivalStationInfo().getTerminal());
						} else {
							continue;
						}
					} else {
						airSegmentInformation.setFromTerminal(itineraryInfo.getFlightDetail().getArrivalStationInfo().getTerminal());
					}
					/* From & To Dates */
					StringBuffer depdateStr = new StringBuffer(itineraryInfo.getTravelProduct().getProduct().getDepDate()
							.concat(itineraryInfo.getTravelProduct().getProduct().getDepTime()));
					SimpleDateFormat format = new SimpleDateFormat("ddMMyyHHmm");
					Date depdate = format.parse(depdateStr.toString());
					airSegmentInformation.setDepartureDate(depdate);

					StringBuffer arrdateStr = new StringBuffer(itineraryInfo.getTravelProduct().getProduct().getArrDate()
							.concat(itineraryInfo.getTravelProduct().getProduct().getArrTime()));
					SimpleDateFormat format1 = new SimpleDateFormat("ddMMyyHHmm");
					Date arrdate = format1.parse(arrdateStr.toString());
					airSegmentInformation.setArrivalDate(arrdate);

					/* From & To Location */
					airSegmentInformation.setFromLocation(itineraryInfo.getTravelProduct().getBoardpointDetail().getCityCode());
					airSegmentInformation.setToLocation(itineraryInfo.getTravelProduct().getOffpointDetail().getCityCode());
					/*For  flight Number*/
					airSegmentInformation.setFlightNumber(itineraryInfo.getTravelProduct().getProductDetails().getIdentification());
					
					airsegmentationList.add(airSegmentInformation);

				}
				// System.out.println("=============journey.getAirSegmentList==========\n"+Json.toJson(airsegmentationList));
				journey.setAirSegmentList(airsegmentationList);
				journeyList.add(journey);
			}
			if (isSeamen) {
				flightItinerary.setJourneyList(journeyList);
			} else {
				flightItinerary.setNonSeamenJourneyList(journeyList);
			}

			// flightItinerary.setJourneyList(journeyList);
			/*
			 * airSegmentInformation.setArrivalTime(gdsPNRReply);
			 * airSegmentInformation.setBookingClass(bookingClass);
			 * 
			 * }
			 */
			/*
			 * System.out.println("=========================AMADEUS RESPONSE" +
			 * "================================================\n"
			 * +Json.toJson(gdsPNRReply));
			 */
			masterInfo.setItinerary(flightItinerary);
			System.out.println("====== masterInfo details =========="+ Json.toJson(masterInfo));
		} catch (Exception e) {
			e.printStackTrace();
		}
	
		return issuanceResponse;
	}
}
