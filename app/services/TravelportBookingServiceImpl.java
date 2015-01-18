package services;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import play.libs.Json;
import utils.ErrorMessageHelper;
import utils.StringUtility;

import com.compassites.GDSWrapper.travelport.AirRequestClient;
import com.compassites.GDSWrapper.travelport.AirReservationClient;
import com.compassites.GDSWrapper.travelport.AirTicketClient;
import com.compassites.GDSWrapper.travelport.Helper;
import com.compassites.GDSWrapper.travelport.UniversalRecordClient;
import com.compassites.model.AirSegmentInformation;
import com.compassites.model.ErrorMessage;
import com.compassites.model.FlightItinerary;
import com.compassites.model.IssuanceRequest;
import com.compassites.model.IssuanceResponse;
import com.compassites.model.Journey;
import com.compassites.model.PNRResponse;
import com.compassites.model.Passenger;
import com.compassites.model.PassengerTypeCode;
import com.compassites.model.traveller.PersonalDetails;
import com.compassites.model.traveller.Traveller;
import com.compassites.model.traveller.TravellerMasterInfo;
import com.travelport.schema.air_v26_0.AirItinerary;
import com.travelport.schema.air_v26_0.AirPriceRsp;
import com.travelport.schema.air_v26_0.AirReservation;
import com.travelport.schema.air_v26_0.AirSegmentDetails;
import com.travelport.schema.air_v26_0.AirSegmentList;
import com.travelport.schema.air_v26_0.AirTicketingRsp;
import com.travelport.schema.air_v26_0.Coupon;
import com.travelport.schema.air_v26_0.DocumentInfo;
import com.travelport.schema.air_v26_0.ETR;
import com.travelport.schema.air_v26_0.FlightDetails;
import com.travelport.schema.air_v26_0.Ticket;
import com.travelport.schema.air_v26_0.TicketInfo;
import com.travelport.schema.air_v26_0.TypeBaseAirSegment;
import com.travelport.schema.common_v26_0.BookingTraveler;
import com.travelport.schema.common_v26_0.BookingTravelerName;
import com.travelport.schema.common_v26_0.ProviderReservationInfoRef;
import com.travelport.schema.common_v26_0.TypeCabinClass;
import com.travelport.schema.universal_v26_0.AirCreateReservationRsp;
import com.travelport.schema.universal_v26_0.ProviderReservationInfo;
import com.travelport.schema.universal_v26_0.UniversalRecord;
import com.travelport.schema.universal_v26_0.UniversalRecordRetrieveRsp;
import com.travelport.service.air_v26_0.AirFaultMessage;

/*
 *
 * Created by user on 02-09-2014.
 */

@Service
public class TravelportBookingServiceImpl implements BookingService {

	@Override
	public PNRResponse generatePNR(TravellerMasterInfo travellerMasterInfo) {
		PNRResponse pnrResponse = new PNRResponse();
		try {
			// AirItinerary airItinerary =
			// AirRequestClient.getItinerary(responseTwo,
			// responseTwo.getAirPricingSolution().get(0));
			AirItinerary airItinerary = AirRequestClient
					.buildAirItinerary(travellerMasterInfo);
			PassengerTypeCode passengerType = null;
			if (travellerMasterInfo.isSeamen()) {
				passengerType = PassengerTypeCode.SEA;
			} else {
				passengerType = PassengerTypeCode.ADT;
			}
			TypeCabinClass typeCabinClass = TypeCabinClass
					.valueOf(travellerMasterInfo.getCabinClass().upperValue());
			List<Passenger> passengerList = AirRequestClient.createPassengers(
					travellerMasterInfo.getTravellersList(), passengerType);

			AirPriceRsp priceRsp = AirRequestClient.priceItinerary(
					airItinerary, passengerType.toString(), "INR",
					typeCabinClass, passengerList);
			pnrResponse = checkFare(priceRsp, travellerMasterInfo);
			if (!pnrResponse.isPriceChanged()) {
				// AirPricingSolution airPriceSolution =
				// AirReservationClient.stripNonXmitSections(AirRequestClient.getPriceSolution(priceRsp));
				AirCreateReservationRsp reservationRsp = AirReservationClient
						.reserve(AirRequestClient.getPriceSolution(priceRsp),
								travellerMasterInfo);
				UniversalRecordRetrieveRsp universalRecordRetrieveRsp = UniversalRecordClient
						.retrievePNR(reservationRsp);
				pnrResponse = retrievePNR(universalRecordRetrieveRsp,
						pnrResponse);
			}

		} catch (AirFaultMessage airFaultMessage) {
			airFaultMessage.printStackTrace(); // To change body of catch
												// statement use File | Settings
												// | File Templates.
			ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage(
					"error", ErrorMessage.ErrorType.ERROR, "TravelPort");
			pnrResponse.setErrorMessage(errorMessage);
		} catch (Exception e) {
			e.printStackTrace();
			ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage(
					"error", ErrorMessage.ErrorType.ERROR, "TravelPort");
			pnrResponse.setErrorMessage(errorMessage);
		}

		return pnrResponse;
	}

	@Override
	public PNRResponse priceChangePNR(TravellerMasterInfo travellerMasterInfo) {
		return generatePNR(travellerMasterInfo);
	}

	public IssuanceResponse issueTicket(IssuanceRequest issuanceRequest) {
		IssuanceResponse issuanceResponse = new IssuanceResponse();
		AirTicketClient airTicketClient = new AirTicketClient();
		AirTicketingRsp airTicketingRsp = airTicketClient
				.issueTicket(issuanceRequest.getGdsPNR());
		if (airTicketingRsp.getETR().size() > 0) {
			issuanceResponse.setSuccess(true);
			issuanceResponse.setPnrNumber(issuanceRequest.getGdsPNR());
			List<Traveller> travellerList = issuanceRequest.getTravellerList();
			for (Traveller traveller : travellerList) {
				List<Ticket> tickets = null;
				for (ETR etr : airTicketingRsp.getETR()) {
					BookingTravelerName name = etr.getBookingTraveler()
							.getBookingTravelerName();
					if (traveller.getPersonalDetails().getFirstName()
							.equalsIgnoreCase(name.getFirst())
							&& traveller.getPersonalDetails().getLastName()
									.equalsIgnoreCase(name.getLast())) {
						tickets = etr.getTicket();
						break;
					}
				}
				Map<String, String> ticketMap = new HashMap<>();
				for (Ticket ticket : tickets) {
					for (Coupon coupon : ticket.getCoupon()) {
						String key = coupon.getOrigin()
								+ coupon.getDestination()
								+ traveller.getContactId();
						ticketMap.put(key.toLowerCase(),
								ticket.getTicketNumber());
					}
				}
				traveller.setTicketNumberMap(ticketMap);
			}
			issuanceResponse.setTravellerList(travellerList);
		} else {
			issuanceResponse.setSuccess(false);
		}
		return issuanceResponse;
	}

	public PNRResponse checkFare(AirPriceRsp priceRsp,
			TravellerMasterInfo travellerMasterInfo) {
		PNRResponse pnrResponse = new PNRResponse();
		BigDecimal searchPrice = new BigDecimal(0);
		if (travellerMasterInfo.isSeamen()) {
			searchPrice = travellerMasterInfo.getItinerary()
					.getSeamanPricingInformation().getTotalPriceValue();
		} else {
			searchPrice = travellerMasterInfo.getItinerary()
					.getPricingInformation().getTotalPriceValue();
		}
		BigDecimal totalPrice = new BigDecimal(
				StringUtility.getPriceFromString(priceRsp.getAirPriceResult()
						.get(0).getAirPricingSolution().get(0).getTotalPrice()));
		BigDecimal changedBasePrice = new BigDecimal(
				StringUtility.getPriceFromString(priceRsp.getAirPriceResult()
						.get(0).getAirPricingSolution().get(0).getBasePrice()));
		if (totalPrice.toBigIntegerExact().equals(
				searchPrice.toBigIntegerExact())) {

			pnrResponse.setPriceChanged(false);
			pnrResponse.setFlightAvailable(true);
			return pnrResponse;
		}
		pnrResponse.setChangedPrice(totalPrice);
		pnrResponse.setChangedBasePrice(changedBasePrice);
		pnrResponse.setOriginalPrice(searchPrice);
		pnrResponse.setFlightAvailable(true);
		pnrResponse.setPriceChanged(true);
		return pnrResponse;
	}

	public PNRResponse retrievePNR(
			UniversalRecordRetrieveRsp universalRecordRetrieveRsp,
			PNRResponse pnrResponse) {
		// PNRResponse pnrResponse = new PNRResponse();
		Helper.ReservationInfoMap reservationInfoMap = Helper
				.createReservationInfoMap(universalRecordRetrieveRsp
						.getUniversalRecord().getProviderReservationInfo());
		Date lastDate = null;
		for (AirReservation airReservation : universalRecordRetrieveRsp
				.getUniversalRecord().getAirReservation()) {
			for (ProviderReservationInfoRef reservationInfoRef : airReservation
					.getProviderReservationInfoRef()) {
				ProviderReservationInfo reservationInfo = reservationInfoMap
						.getByRef(reservationInfoRef);
				pnrResponse.setPnrNumber(reservationInfo.getLocatorCode());
			}
		}
		try {
			String remarkData = universalRecordRetrieveRsp.getUniversalRecord()
					.getGeneralRemark().get(0).getRemarkData();
			int i = remarkData.lastIndexOf("BY");
			String subString = remarkData.substring(i + 2);

			subString = subString.trim();
			String[] args1 = subString.split("/");
			String dateString = args1[0] + "/" + args1[1];
			dateString = dateString + Calendar.getInstance().get(Calendar.YEAR);
			SimpleDateFormat sdf = new SimpleDateFormat("HHmm/ddMMMyyyy");

			lastDate = sdf.parse(dateString);
		} catch (NullPointerException e) {
			e.printStackTrace();
			pnrResponse.setFlightAvailable(false);

		} catch (ParseException e) {
			e.printStackTrace();
		}

		pnrResponse.setValidTillDate(lastDate);
		return pnrResponse;
	}

	public PNRResponse checkFareChangeAndAvailability(
			TravellerMasterInfo travellerMasterInfo) {
		PNRResponse pnrResponse = new PNRResponse();
		try {
			// AirItinerary airItinerary =
			// AirRequestClient.getItinerary(responseTwo,
			// responseTwo.getAirPricingSolution().get(0));
			AirItinerary airItinerary = AirRequestClient
					.buildAirItinerary(travellerMasterInfo);
			PassengerTypeCode passengerType = null;
			if (travellerMasterInfo.isSeamen()) {
				passengerType = PassengerTypeCode.SEA;
			} else {
				passengerType = PassengerTypeCode.ADT;
			}
			TypeCabinClass typeCabinClass = TypeCabinClass
					.valueOf(travellerMasterInfo.getCabinClass().upperValue());
			List<Passenger> passengerList = AirRequestClient.createPassengers(
					travellerMasterInfo.getTravellersList(), passengerType);

			AirPriceRsp priceRsp = AirRequestClient.priceItinerary(
					airItinerary, passengerType.toString(), "INR",
					typeCabinClass, passengerList);
			pnrResponse = checkFare(priceRsp, travellerMasterInfo);

		} catch (AirFaultMessage airFaultMessage) {
			airFaultMessage.printStackTrace(); // To change body of catch
												// statement use File | Settings
												// | File Templates.
			ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage(
					"error", ErrorMessage.ErrorType.ERROR, "TravelPort");
			pnrResponse.setErrorMessage(errorMessage);
		} catch (Exception e) {
			e.printStackTrace();
			ErrorMessage errorMessage = ErrorMessageHelper.createErrorMessage(
					"error", ErrorMessage.ErrorType.ERROR, "TravelPort");

			pnrResponse.setErrorMessage(errorMessage);
		}

		return pnrResponse;
	}

	public TravellerMasterInfo allPNRDetails(IssuanceRequest issuanceRequest,
			String gdsPNR) {
		TravellerMasterInfo masterInfo = new TravellerMasterInfo();
		boolean isSeamen = issuanceRequest.isSeamen();
		masterInfo.setSeamen(isSeamen);

		try {
			UniversalRecordRetrieveRsp universalRecordRetrieveRsp = UniversalRecordClient
					.retrievePNR(gdsPNR);

			// traveller deatials
			List<Traveller> travellerList = new ArrayList<>();
			for (Traveller traveller1 : issuanceRequest.getTravellerList()) {
				Traveller traveller = new Traveller();
				traveller.setCdcDetails(traveller1.getCdcDetails());
				traveller.setContactId(traveller1.getContactId());
				traveller.setVisaDetails(traveller1.getVisaDetails());
				traveller.setPreferences(traveller1.getPreferences());
				traveller.setPassportDetails(traveller1.getPassportDetails());
				for (BookingTraveler travelerName : universalRecordRetrieveRsp
						.getUniversalRecord().getBookingTraveler()) {
					PersonalDetails personalDetails = new PersonalDetails();
					personalDetails.setFirstName(travelerName
							.getBookingTravelerName().getFirst());
					personalDetails.setMiddleName(travelerName
							.getBookingTravelerName().getMiddle());
					personalDetails.setLastName(travelerName
							.getBookingTravelerName().getLast());
					personalDetails.setGender(travelerName
							.getBookingTravelerName().getPrefix());

					traveller.setPersonalDetails(personalDetails);
				}

				for (AirReservation airReservation : universalRecordRetrieveRsp
						.getUniversalRecord().getAirReservation()) {
					for (TicketInfo ticketInfo : airReservation
							.getDocumentInfo().getTicketInfo()) {
						Map<String, String> ticketNumberMap = new HashMap<String, String>();
						ticketNumberMap.put("TicketNumber",
								ticketInfo.getNumber());
						ticketNumberMap.put("airPricingInfoRef",
								ticketInfo.getAirPricingInfoRef());
						ticketNumberMap.put("bookingTravelerRef",
								ticketInfo.getBookingTravelerRef());
						traveller.setTicketNumberMap(ticketNumberMap);
					}
				}

				// flight Ititinrary Deatails
				List<Journey> journeyList = new ArrayList<>();
				List<AirSegmentInformation> airSegmentList = new ArrayList<>();
				FlightItinerary flightItinerary = new FlightItinerary();
				Journey journey = new Journey();
				for (AirReservation airReservation : universalRecordRetrieveRsp
						.getUniversalRecord().getAirReservation()) {
					SimpleDateFormat format = new SimpleDateFormat("yyMMddHHmm");
					for (TypeBaseAirSegment airSegment : airReservation
							.getAirSegment()) {
						AirSegmentInformation airSegmentInformation = new AirSegmentInformation();
						airSegmentInformation.setCarrierCode(airSegment
								.getCarrier());
						airSegmentInformation.setFlightNumber(airSegment
								.getFlightNumber());
						Date arrvalDate = format.parse(airSegment
								.getArrivalTime());
						airSegmentInformation.setArrivalDate(arrvalDate);

						Date depDate = format.parse(airSegment
								.getDepartureTime());
						airSegmentInformation.setDepartureDate(depDate);

						airSegmentInformation.setFromLocation(airSegment
								.getOrigin());
						airSegmentInformation.setToLocation(airSegment
								.getDestination());
						airSegmentInformation.setTravelTime(airSegment
								.getTravelTime().toString());
						airSegmentInformation.setDistanceTravelled(airSegment
								.getDistance().toString());
						for (FlightDetails flightDetails : airSegment
								.getFlightDetails()) {

						}

						airSegmentList.add(airSegmentInformation);
					}
					travellerList.add(traveller);
					masterInfo.setTravellersList(travellerList); // traveller is
					journey.setAirSegmentList(airSegmentList);
					journeyList.add(journey);
				}
				if (isSeamen) {
					flightItinerary.setJourneyList(journeyList);
				} else {
					flightItinerary.setNonSeamenJourneyList(journeyList);
				}
				masterInfo.setItinerary(flightItinerary);
				// System.out.println("==========>>>>>>\n"+Json.toJson(masterInfo));
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return masterInfo;
	}

}