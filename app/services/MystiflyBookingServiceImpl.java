package services;

import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import com.amadeus.xml.pnracc_11_3_1a.PNRReply;
import com.compassites.constants.CacheConstants;
import com.compassites.model.traveller.PersonalDetails;
import com.fasterxml.jackson.databind.JsonNode;
import org.datacontract.schemas._2004._07.mystifly_onepoint.*;
import org.datacontract.schemas._2004._07.mystifly_onepoint.Error;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import utils.DateUtility;
import utils.ErrorMessageHelper;
import utils.HoldTimeUtility;

import com.compassites.GDSWrapper.mystifly.AirOrderTicketClient;
import com.compassites.GDSWrapper.mystifly.AirRevalidateClient;
import com.compassites.GDSWrapper.mystifly.*;
import com.compassites.GDSWrapper.mystifly.BookFlightClient;
import com.compassites.GDSWrapper.mystifly.Mystifly;
import com.compassites.model.ErrorMessage;
import com.compassites.model.IssuanceRequest;
import com.compassites.model.IssuanceResponse;
import com.compassites.model.PNRResponse;
import com.compassites.model.PricingInformation;
import com.compassites.model.traveller.Traveller;
import com.compassites.model.traveller.TravellerMasterInfo;
import utils.MystiflyHelper;
import com.compassites.model.*;
import play.libs.Json;

/**
 * @author Santhosh
 */
@Service
public class MystiflyBookingServiceImpl implements BookingService {

	@Autowired
	MystiflyFlightInfoServiceImpl mystiflyFlightInfoService;

	@Autowired
	private RedisTemplate redisTemplate;

	static Logger mystiflyLogger = LoggerFactory.getLogger("mystifly");

	static Logger logger = LoggerFactory.getLogger("gds");


	@Override
	public PNRResponse generatePNR(TravellerMasterInfo travellerMasterInfo) {
		String fareSourceCode = travellerMasterInfo.getItinerary().getPricingInformation().getFareSourceCode();

		AirRevalidateClient revalidateClient = new AirRevalidateClient();
		PNRResponse pnrRS = new PNRResponse();
		AirRevalidateRS revalidateRS;
		//mystiflyLogger.debug("Issuance PNR "+issuanceRequest.getJocPNR());
		try {
			revalidateRS = revalidateClient.revalidate(fareSourceCode);
			if (revalidateRS.getSuccess()) {
				PricedItinerary itinerary = revalidateRS.getPricedItineraries().getPricedItineraryArray(0);
				if (revalidateRS.getIsValid() || !itinerary.isNil()) {

					String newFareSourceCode = itinerary
							.getAirItineraryPricingInfo().getFareSourceCode();
					travellerMasterInfo.getItinerary().setFareSourceCode(
							newFareSourceCode);

					BookFlightClient bookFlightClient = new BookFlightClient();
					AirBookRS airbookRS = bookFlightClient.bookFlight(
							travellerMasterInfo.getItinerary(),
							travellerMasterInfo.getTravellersList(), travellerMasterInfo.getUserTimezone());
					if (airbookRS.getSuccess()) {
						pnrRS.setPnrNumber(airbookRS.getUniqueID());
						pnrRS.setFlightAvailable(airbookRS.getSuccess());
						pnrRS.setBookedStatus(airbookRS.getStatus());
						if (airbookRS.getTktTimeLimit().getTime() == null) {
							Calendar calendar = Calendar.getInstance();
							Date holdDate = HoldTimeUtility
									.getHoldTime(travellerMasterInfo);
							calendar.setTime(holdDate);
							pnrRS.setValidTillDate(calendar.getTime());
							pnrRS.setHoldTime(true);
						} else {
							pnrRS.setValidTillDate(airbookRS.getTktTimeLimit()
									.getTime());
							pnrRS.setHoldTime(false);
						}
						setAirlinePNR(pnrRS);
						readBaggageInfo(pnrRS, travellerMasterInfo);
					} else {
						ErrorMessage error = new ErrorMessage();
						Error[] errors = airbookRS.getErrors().getErrorArray();
						error.setErrorCode(errors[0].getCode());
						error.setMessage(errors[0].getMessage());
						error.setProvider(Mystifly.PROVIDER);
						pnrRS.setErrorMessage(error);
					}
				} else {
					pnrRS.setFlightAvailable(false);
				}
			} else {
				pnrRS.setErrorMessage(ErrorMessageHelper.createErrorMessage(
						"error", ErrorMessage.ErrorType.ERROR,
						Mystifly.PROVIDER));
			}
		} catch (RemoteException e) {
			pnrRS.setErrorMessage(ErrorMessageHelper.createErrorMessage(
					"error", ErrorMessage.ErrorType.ERROR, Mystifly.PROVIDER));
			logger.error("Error in Mystifly generatePNR : ", e);
		}
		return pnrRS;
	}

	@Override
	public SplitPNRResponse splitPNR(IssuanceRequest issuanceRequest){
		SplitPNRResponse splitPNRResponse = null;
		return splitPNRResponse;
	}
	/*@Override
	public void cancelMystiflyBooking(String pnr) {
		try {
			AirCancelClient airCancelClient = new AirCancelClient();
			airCancelClient.cancelBooking(pnr);
		} catch (Exception ex){
			logger.debug("Mystilfy AirBook  cancel pnr",ex);
		}
	}*/
	@Override
	public PNRResponse priceChangePNR(TravellerMasterInfo travellerMasterInfo) {
		return generatePNR(travellerMasterInfo);
	}

	public IssuanceResponse issueTicket(IssuanceRequest issuanceRequest) {
		mystiflyLogger.debug(" issuanceRequest "+issuanceRequest);
		IssuanceResponse issuanceResponse = new IssuanceResponse();
		PricingInformation pricingInfo = issuanceRequest.getFlightItinerary().getPricingInformation();
		try {
			if (pricingInfo.isLCC()) {
				PNRResponse pnrRS = new PNRResponse();
				TravellerMasterInfo travellerMasterInfo = new TravellerMasterInfo();
				travellerMasterInfo.setItinerary(issuanceRequest.getFlightItinerary());
				travellerMasterInfo.setTravellersList(issuanceRequest.getTravellerList());
				pnrRS = generatePNR(travellerMasterInfo);
				if(pnrRS.isFlightAvailable() && !pnrRS.isPriceChanged()) {
					issuanceResponse.setSuccess(true);
					issuanceResponse.setPnrNumber(pnrRS.getPnrNumber());
					issuanceResponse.setAirlinePnr(pnrRS.getAirlinePNR());
					issuanceResponse.setBookingStatus(pnrRS.getBookedStatus());
					issuanceRequest.setGdsPNR(pnrRS.getPnrNumber());
					issuanceResponse.setValidTillDate(pnrRS.getValidTillDate());
				}
			} else {
				AirOrderTicketClient orderTicketClient = new AirOrderTicketClient();
				AirOrderTicketRS orderTicketRS = orderTicketClient
						.issueTicket(issuanceRequest.getGdsPNR());
				if(orderTicketRS.getSuccess()) {
					issuanceResponse.setPnrNumber(orderTicketRS.getUniqueID());
					issuanceResponse.setSuccess(true);
				} else {
					issuanceResponse.setSuccess(false);
				}
			}

			List<Traveller> travellerList = issuanceRequest.getTravellerList();
			//mystiflyLogger.debug("travellerList  "+Json.toJson(travellerList));
			setTravellerTickets(travellerList, issuanceRequest.getGdsPNR());
			issuanceResponse.setTravellerList(travellerList);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.error("Error in Mystifly issueTicket : ", e);

		}
		redisTemplate.opsForValue().set(issuanceRequest.getJocPNR()+":booking_status", Json.stringify(Json.toJson(issuanceResponse)));
		redisTemplate.expire(issuanceRequest.getJocPNR()+":booking_status", CacheConstants.CACHE_TIMEOUT_IN_SECS, TimeUnit.SECONDS);
		return issuanceResponse;
	}

	public IssuanceResponse readTripDetails(IssuanceRequest issuanceRequest) {
		mystiflyLogger.debug(" issuanceRequest "+issuanceRequest);
		IssuanceResponse issuanceResponse = new IssuanceResponse();
		PricingInformation pricingInfo = issuanceRequest.getFlightItinerary().getPricingInformation();
		try {
			//if (pricingInfo.isLCC()) {
				PNRResponse pnrRS = new PNRResponse();
				TravellerMasterInfo travellerMasterInfo = new TravellerMasterInfo();
				travellerMasterInfo.setItinerary(issuanceRequest.getFlightItinerary());
				travellerMasterInfo.setTravellersList(issuanceRequest.getTravellerList());
				//pnrRS = generatePNR(travellerMasterInfo);
				issuanceResponse.setSuccess(true);
				if(pnrRS.isFlightAvailable() && !pnrRS.isPriceChanged()) {
				issuanceResponse.setPnrNumber(pnrRS.getPnrNumber());
				issuanceResponse.setAirlinePnr(pnrRS.getAirlinePNR());
				issuanceRequest.setGdsPNR(pnrRS.getPnrNumber());
				issuanceResponse.setValidTillDate(pnrRS.getValidTillDate());
				}
			/*} else {
				AirOrderTicketClient orderTicketClient = new AirOrderTicketClient();
				AirOrderTicketRS orderTicketRS = orderTicketClient
						.issueTicket(issuanceRequest.getGdsPNR());
				issuanceResponse.setPnrNumber(orderTicketRS.getUniqueID());
			}*/

				List<Traveller> travellerList = issuanceRequest.getTravellerList();
				setTravellerTickets(travellerList, issuanceRequest.getGdsPNR());
				issuanceResponse.setTravellerList(travellerList);
			//}
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.error("Error in Mystifly issueTicket : ", e);

		}
		return issuanceResponse;
	}
	private void setAirlinePNR(PNRResponse pnrResponse) throws RemoteException {
		AirTripDetailsClient tripDetailsClient = new AirTripDetailsClient();
		AirTripDetailsRS tripDetailsRS = tripDetailsClient
				.getAirTripDetails(pnrResponse.getPnrNumber());
		mystiflyLogger.debug("Traveller Tickets "+tripDetailsRS);
		TravelItinerary itinerary = tripDetailsRS.getTravelItinerary();
		String airlinePNR = itinerary.getItineraryInfo().getReservationItems()
				.getReservationItemArray(0).getAirlinePNR();
		pnrResponse.setAirlinePNR(airlinePNR);

		ArrayOfReservationItem arrayOfReservationItems = itinerary.getItineraryInfo().getReservationItems();

		Map<String, String> airlinePNRMap = new HashMap<>();
		int segmentSequence = 1;
		for(int i =0; i < arrayOfReservationItems.sizeOfReservationItemArray(); i++){
			ReservationItem reservationItem = arrayOfReservationItems.getReservationItemArray(i);
			String segments = reservationItem.getDepartureAirportLocationCode() + reservationItem.getArrivalAirportLocationCode() + segmentSequence;
			airlinePNR = reservationItem.getAirlinePNR();
			airlinePNRMap.put(segments.toLowerCase(), airlinePNR);
			segmentSequence++;
		}
		pnrResponse.setAirlinePNRMap(airlinePNRMap);
	}

	private void setTravellerTickets(List<Traveller> travellerList, String pnr)
			throws RemoteException {
		AirTripDetailsClient tripDetailsClient = new AirTripDetailsClient();
		AirTripDetailsRS tripDetailsRS = tripDetailsClient
				.getAirTripDetails(pnr);
		mystiflyLogger.debug("Traveller Tickets "+tripDetailsRS);
		ItineraryInfo itinerary = tripDetailsRS.getTravelItinerary()
				.getItineraryInfo();
		for (CustomerInfo customerInfo : itinerary.getCustomerInfos()
				.getCustomerInfoArray()) {
			Traveller traveller = findTravellerWithPassportNum(travellerList,
					customerInfo.getCustomer().getPassportNumber());
			Map<String, String> ticketMap = new HashMap<>();
			for (ETicket eTicket : customerInfo.getETickets().getETicketArray()) {
				ReservationItem[] reservationItems = itinerary
						.getReservationItems().getReservationItemArray();
				ReservationItem reservationItem = findReservationItemFromRPH(
						reservationItems, eTicket.getItemRPH());
				String key = reservationItem.getDepartureAirportLocationCode()
						+ reservationItem.getArrivalAirportLocationCode()
						+ traveller.getContactId() + eTicket.getItemRPH();
				ticketMap.put(key.toLowerCase(), eTicket.getETicketNumber());
			}
			traveller.setTicketNumberMap(ticketMap);
		}
	}

	public JsonNode getBookingDetails(String gdsPNR) throws RemoteException{
		Map<String, Object> json = new HashMap<>();
		TravellerMasterInfo masterInfo = new TravellerMasterInfo();
		AirTripDetailsClient tripDetailsClient = new AirTripDetailsClient();
		AirTripDetailsRS tripDetailsRS = tripDetailsClient.getAirTripDetails(gdsPNR);
		if(tripDetailsRS.getSuccess()) {
			List<Traveller> travellersList = new ArrayList<>();
			CustomerInfo arrayOfCustomerInfo[] = tripDetailsRS.getTravelItinerary().getItineraryInfo().getCustomerInfos().getCustomerInfoArray();
			for(int len =0; len < arrayOfCustomerInfo.length; len++){
				Traveller traveller = new Traveller();
				PersonalDetails personalDetails = new PersonalDetails();
				Customer customers = arrayOfCustomerInfo[len].getCustomer();
				String firName = customers.getPaxName().getPassengerFirstName();
				String lastName = customers.getPaxName().getPassengerLastName();
				String title = customers.getPaxName().getPassengerTitle();
				personalDetails.setSalutation(title);
				personalDetails.setFirstName(firName);
				personalDetails.setMiddleName("");
				personalDetails.setLastName(lastName);
				traveller.setPersonalDetails(personalDetails);
				travellersList.add(traveller);
			}
			masterInfo.setTravellersList(travellersList);
			FlightItinerary flightItinerary = new FlightItinerary();
			json.put("travellerMasterInfo", masterInfo);
		}
		return Json.toJson(json);
	}

	private void setAirlinePNRMap(PNRResponse pnrResponse) throws RemoteException {
		AirTripDetailsClient tripDetailsClient = new AirTripDetailsClient();
		AirTripDetailsRS tripDetailsRS = tripDetailsClient
				.getAirTripDetails(pnrResponse.getPnrNumber());
		mystiflyLogger.debug("Traveller Tickets "+tripDetailsRS);
		TravelItinerary itinerary = tripDetailsRS.getTravelItinerary();
		String airlinePNR = itinerary.getItineraryInfo().getReservationItems()
				.getReservationItemArray(0).getAirlinePNR();
		pnrResponse.setAirlinePNR(airlinePNR);
		ArrayOfReservationItem arrayOfReservationItems = itinerary.getItineraryInfo().getReservationItems();

		Map<String, String> airlinePNRMap = new HashMap<>();
		int segmentSequence = 1;
		for(int i =0; i < arrayOfReservationItems.sizeOfReservationItemArray(); i++){
			ReservationItem reservationItem = arrayOfReservationItems.getReservationItemArray(i);
			String segments = reservationItem.getDepartureAirportLocationCode() + reservationItem.getArrivalAirportLocationCode() + segmentSequence;
			airlinePNR = reservationItem.getAirlinePNR();
			airlinePNRMap.put(segments.toLowerCase(), airlinePNR);
			segmentSequence++;
		}
		pnrResponse.setAirlinePNRMap(airlinePNRMap);
	}

	private ReservationItem findReservationItemFromRPH(
			ReservationItem[] reservationItems, int rph) {
		for (ReservationItem resItem : reservationItems) {
			if (resItem.getItemRPH() == rph)
				return resItem;
		}
		return null;
	}

	private Traveller findTravellerWithPassportNum(
			List<Traveller> travellerList, String passportNum) {
		for (Traveller traveller : travellerList) {
			if (traveller.getPassportDetails().getPassportNumber()
					.equals(passportNum))
				return traveller;
		}
		return null;
	}

	public PNRResponse checkFareChangeAndAvailability(
			TravellerMasterInfo travellerMasterInfo) {
		String fareSourceCode = travellerMasterInfo.getItinerary().getPricingInformation().getFareSourceCode();

		AirRevalidateClient revalidateClient = new AirRevalidateClient();
		PNRResponse pnrRS = new PNRResponse();
		AirRevalidateRS revalidateRS;
		try {
			revalidateRS = revalidateClient.revalidate(fareSourceCode);
			if (revalidateRS.getSuccess()) {
				if (revalidateRS.getIsValid()) {
					PricedItinerary itinerary = revalidateRS
							.getPricedItineraries().getPricedItineraryArray(0);
					AirItineraryPricingInfo airItineraryPricingInfo = revalidateRS.getPricedItineraries().getPricedItineraryArray(0).getAirItineraryPricingInfo();
					PricingInformation newPriceInfo = MystiflyHelper.setPricingInformtions(airItineraryPricingInfo);
					String newFareSourceCode = itinerary
							.getAirItineraryPricingInfo().getFareSourceCode();
					travellerMasterInfo.getItinerary().setFareSourceCode(
							newFareSourceCode);
					//travellerMasterInfo.getItinerary().getPricingInformation().setAdtBasePrice();
					pnrRS.setFlightAvailable(true);
					pnrRS.setPricingInfo(newPriceInfo);
					//pnrRS.setPricingInfo(travellerMasterInfo.getItinerary().getPricingInformation(travellerMasterInfo.isSeamen()));
				} else if (revalidateRS.getPricedItineraries() == null
						|| revalidateRS.getPricedItineraries()
						.getPricedItineraryArray() == null
						|| revalidateRS.getPricedItineraries()
						.getPricedItineraryArray().length == 0) {
					pnrRS.setPriceChanged(false);
					pnrRS.setFlightAvailable(false);

				} else {

					AirItineraryPricingInfo pricingInfo = revalidateRS
							.getPricedItineraries().getPricedItineraryArray(0)
							.getAirItineraryPricingInfo();
					pnrRS.setChangedPrice(new BigDecimal(pricingInfo
							.getItinTotalFare().getTotalFare().getAmount()));
					pnrRS.setChangedBasePrice(new BigDecimal(pricingInfo
							.getItinTotalFare().getBaseFare().getAmount()));
					pnrRS.setOriginalPrice(travellerMasterInfo.getItinerary()
							.getPricingInformation().getTotalPrice());
					pnrRS.setPricingInfo(travellerMasterInfo.getItinerary()
							.getPricingInformation(travellerMasterInfo.isSeamen()));

					pnrRS.setFlightAvailable(true);
					pnrRS.setPriceChanged(true);

					PricingInformation newPriceInfo = MystiflyHelper.setPricingInformtions(pricingInfo);
					pnrRS.setPricingInfo(newPriceInfo);
				}
			} else {
				pnrRS.setErrorMessage(ErrorMessageHelper.createErrorMessage(
						"error", ErrorMessage.ErrorType.ERROR,
						Mystifly.PROVIDER));
			}
		} catch (RemoteException e) {
			pnrRS.setErrorMessage(ErrorMessageHelper.createErrorMessage(
					"error", ErrorMessage.ErrorType.ERROR, Mystifly.PROVIDER));
			logger.error("Error in Mystifly checkFareChangeAndAvailability : ", e);

		}
		return pnrRS;
	}

	public TravellerMasterInfo allPNRDetails(String gdsPNR) {
		IssuanceResponse issuanceResponse = new IssuanceResponse();
		TravellerMasterInfo masterInfo = new TravellerMasterInfo();
		return masterInfo;
	}

	private PNRResponse readBaggageInfo(PNRResponse  pnrResponse, TravellerMasterInfo travellerMasterInfo) {
		int adultCount = 0, childCount = 0, infantCount = 0;
		for (Traveller traveller : travellerMasterInfo.getTravellersList()) {
			PassengerTypeCode passengerType = DateUtility.getPassengerTypeFromDOB(traveller.getPassportDetails().getDateOfBirth());
			if (passengerType.equals(PassengerTypeCode.ADT) || passengerType.equals(PassengerTypeCode.SEA)) {
				adultCount++;
			} else if (passengerType.equals(PassengerTypeCode.CHD)) {
				childCount++;
			} else {
				infantCount++;
			}
		}
		SearchParameters parameters = new SearchParameters();
		parameters.setAdultCount(adultCount);
		parameters.setChildCount(childCount);
		parameters.setInfantCount(infantCount);
		HashMap<String, String> map = new HashMap<>();
		FlightItinerary flightItinerary = mystiflyFlightInfoService.getBaggageInfo(travellerMasterInfo.getItinerary(), parameters, travellerMasterInfo.isSeamen());
		mystiflyLogger.debug("Read Baggage Info........");
		try {
			for (Journey journey : travellerMasterInfo.isSeamen() ? flightItinerary.getJourneyList() : flightItinerary.getNonSeamenJourneyList()){
				for (AirSegmentInformation segmentInformation : journey.getAirSegmentList()) {
					String key = segmentInformation.getFromLocation().concat(segmentInformation.getToLocation());
					String baggage = segmentInformation.getFlightInfo().getBaggageAllowance() + "	"
							+ segmentInformation.getFlightInfo().getBaggageUnit();
					map.put(key, baggage);
					segmentInformation.getAirSegmentKey();
				}
			}
		} catch (Exception e){
			logger.error("Error in readBaggageInfo " , e);
			e.printStackTrace();
		}
		pnrResponse.setSegmentBaggageMap(map);
		return pnrResponse;
	}

}
