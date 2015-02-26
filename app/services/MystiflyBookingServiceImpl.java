package services;

import com.compassites.GDSWrapper.mystifly.*;
import com.compassites.model.ErrorMessage;
import com.compassites.model.IssuanceRequest;
import com.compassites.model.IssuanceResponse;
import com.compassites.model.PNRResponse;
import com.compassites.model.traveller.Traveller;
import com.compassites.model.traveller.TravellerMasterInfo;

import org.datacontract.schemas._2004._07.mystifly_onepoint.*;
import org.datacontract.schemas._2004._07.mystifly_onepoint.Error;
import org.springframework.stereotype.Service;

import utils.ErrorMessageHelper;

import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Santhosh
 */
@Service
public class MystiflyBookingServiceImpl implements BookingService {

	@Override
	public PNRResponse generatePNR(TravellerMasterInfo travellerMasterInfo) {
		String fareSourceCode = travellerMasterInfo.getItinerary()
				.getFareSourceCode();
		AirRevalidateClient revalidateClient = new AirRevalidateClient();
		PNRResponse pnrRS = new PNRResponse();
		AirRevalidateRS revalidateRS;
		try {
			revalidateRS = revalidateClient.revalidate(fareSourceCode);
			if (revalidateRS.getSuccess()) {
				if (revalidateRS.getIsValid()) {
					PricedItinerary itinerary = revalidateRS
							.getPricedItineraries().getPricedItineraryArray(0);
					String newFareSourceCode = itinerary
							.getAirItineraryPricingInfo().getFareSourceCode();
					travellerMasterInfo.getItinerary().setFareSourceCode(
							newFareSourceCode);

					BookFlightClient bookFlightClient = new BookFlightClient();
					AirBookRS airbookRS = bookFlightClient
							.bookFlight(travellerMasterInfo);
					if (airbookRS.getSuccess()) {
						pnrRS.setPnrNumber(airbookRS.getUniqueID());
						pnrRS.setFlightAvailable(airbookRS.getSuccess());
						pnrRS.setValidTillDate(airbookRS.getTktTimeLimit()
								.getTime());
						setAirlinePNR(pnrRS);
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
		}
		return pnrRS;
	}

	@Override
	public PNRResponse priceChangePNR(TravellerMasterInfo travellerMasterInfo) {
		return generatePNR(travellerMasterInfo);
	}

	public IssuanceResponse issueTicket(IssuanceRequest issuanceRequest) {
		IssuanceResponse issuanceResponse = new IssuanceResponse();
		AirOrderTicketClient orderTicketClient = new AirOrderTicketClient();
		AirOrderTicketRS orderTicketRS = null;
		try {
			orderTicketRS = orderTicketClient.issueTicket(issuanceRequest
					.getGdsPNR());
			issuanceResponse.setPnrNumber(orderTicketRS.getUniqueID());
			List<Traveller> travellerList = issuanceRequest.getTravellerList();
			setTravellerTickets(travellerList, orderTicketRS.getUniqueID());
			issuanceResponse.setTravellerList(travellerList);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return issuanceResponse;
	}

	private void setAirlinePNR(PNRResponse pnrResponse) throws RemoteException {
		AirTripDetailsClient tripDetailsClient = new AirTripDetailsClient();
		AirTripDetailsRS tripDetailsRS = tripDetailsClient
				.getAirTripDetails(pnrResponse.getPnrNumber());

		TravelItinerary itinerary = tripDetailsRS.getTravelItinerary();
		String airlinePNR = itinerary.getItineraryInfo().getReservationItems().getReservationItemArray(0).getAirlinePNR();
		pnrResponse.setAirlinePNR(airlinePNR);
//		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH:mm");
//		Map<String, String> airlinePNRMap = new HashMap<>();
//		for (ReservationItem resItem : itinerary.getItineraryInfo()
//				.getReservationItems().getReservationItemArray()) {
//			String key = resItem.getDepartureAirportLocationCode()
//					+ resItem.getArrivalAirportLocationCode()
//					+ sdf.format(resItem.getDepartureDateTime().getTime());
//			airlinePNRMap.put(key, resItem.getAirlinePNR());
//		}
//		pnrResponse.setAirlinePNRMap(airlinePNRMap);
	}

	private void setTravellerTickets(List<Traveller> travellerList, String pnr)
			throws RemoteException {
		AirTripDetailsClient tripDetailsClient = new AirTripDetailsClient();
		AirTripDetailsRS tripDetailsRS = tripDetailsClient
				.getAirTripDetails(pnr);

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
						+ traveller.getContactId();
				ticketMap.put(key.toLowerCase(), eTicket.getETicketNumber());
			}
			traveller.setTicketNumberMap(ticketMap);
		}
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
		String fareSourceCode = travellerMasterInfo.getItinerary()
				.getFareSourceCode();
		AirRevalidateClient revalidateClient = new AirRevalidateClient();
		PNRResponse pnrRS = new PNRResponse();
		AirRevalidateRS revalidateRS;
		try {
			revalidateRS = revalidateClient.revalidate(fareSourceCode);
			if (revalidateRS.getSuccess()) {
				if (revalidateRS.getIsValid()) {
					PricedItinerary itinerary = revalidateRS
							.getPricedItineraries().getPricedItineraryArray(0);
					String newFareSourceCode = itinerary
							.getAirItineraryPricingInfo().getFareSourceCode();
					travellerMasterInfo.getItinerary().setFareSourceCode(
							newFareSourceCode);
					pnrRS.setFlightAvailable(true);
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
					pnrRS.setFlightAvailable(true);
					pnrRS.setPriceChanged(true);
				}
			} else {
				pnrRS.setErrorMessage(ErrorMessageHelper.createErrorMessage(
						"error", ErrorMessage.ErrorType.ERROR,
						Mystifly.PROVIDER));
			}
		} catch (RemoteException e) {
			pnrRS.setErrorMessage(ErrorMessageHelper.createErrorMessage(
					"error", ErrorMessage.ErrorType.ERROR, Mystifly.PROVIDER));
		}
		return pnrRS;
	}

	public TravellerMasterInfo allPNRDetails(String gdsPNR) {
		IssuanceResponse issuanceResponse = new IssuanceResponse();
		TravellerMasterInfo masterInfo = new TravellerMasterInfo();
		return masterInfo;
	}

}
