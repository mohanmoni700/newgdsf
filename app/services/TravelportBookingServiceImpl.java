package services;

import com.compassites.GDSWrapper.travelport.*;
import com.compassites.model.*;
import com.compassites.model.FlightInfo;
import com.compassites.model.Journey;
import com.compassites.model.traveller.Traveller;
import com.compassites.model.traveller.TravellerMasterInfo;
import com.travelport.schema.air_v26_0.*;
import com.travelport.schema.common_v26_0.*;
import com.travelport.schema.universal_v26_0.AirCreateReservationRsp;
import com.travelport.schema.universal_v26_0.ProviderReservationInfo;
import com.travelport.schema.universal_v26_0.UniversalRecordRetrieveRsp;
import com.travelport.service.air_v26_0.AirFaultMessage;
import models.Airline;
import models.Airport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import play.libs.Json;
import utils.ErrorMessageHelper;
import utils.StringUtility;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

/*
 *
 * Created by user on 02-09-2014.
 */

@Service
public class TravelportBookingServiceImpl implements BookingService {

    static Logger logger = LoggerFactory.getLogger("gds");

    @Override
	public PNRResponse generatePNR(TravellerMasterInfo travellerMasterInfo) {
		PNRResponse pnrResponse = new PNRResponse();
		try {
			// AirItinerary airItinerary =
			// AirRequestClient.getItinerary(responseTwo,
			// responseTwo.getAirPricingSolution().get(0));
			AirItinerary airItinerary = AirRequestClient
					.buildAirItinerary(travellerMasterInfo);
			TypeCabinClass typeCabinClass = TypeCabinClass
					.valueOf(travellerMasterInfo.getCabinClass().upperValue());

			AirPriceRsp priceRsp = AirRequestClient.priceItinerary(
					airItinerary, "INR", typeCabinClass, travellerMasterInfo);
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
		
		pnrResponse.setChangedPrice(totalPrice);
		pnrResponse.setChangedBasePrice(changedBasePrice);
		pnrResponse.setOriginalPrice(searchPrice);
		pnrResponse.setFlightAvailable(true);
		if(totalPrice.toBigIntegerExact().equals(searchPrice.toBigIntegerExact())) {
			pnrResponse.setPriceChanged(false);
		} else {
			pnrResponse.setPriceChanged(true);
		}
		return pnrResponse;
	}

	public PNRResponse retrievePNR(
			UniversalRecordRetrieveRsp universalRecordRetrieveRsp,
			PNRResponse pnrResponse) {
		
		Helper.ReservationInfoMap reservationInfoMap = Helper
				.createReservationInfoMap(universalRecordRetrieveRsp
						.getUniversalRecord().getProviderReservationInfo());
		Date lastDate = null;
		Calendar calendar = Calendar.getInstance();
		for (AirReservation airReservation : universalRecordRetrieveRsp
				.getUniversalRecord().getAirReservation()) {
			for (ProviderReservationInfoRef reservationInfoRef : airReservation
					.getProviderReservationInfoRef()) {
				ProviderReservationInfo reservationInfo = reservationInfoMap
						.getByRef(reservationInfoRef);
				if(!StringUtils.hasText(reservationInfo.getLocatorCode())) {
					ErrorMessage error = ErrorMessageHelper.createErrorMessage(
							"Booking failed", ErrorMessage.ErrorType.ERROR, "Travelport");
					pnrResponse.setErrorMessage(error);
					return pnrResponse;
				}
				pnrResponse.setPnrNumber(reservationInfo.getLocatorCode());
			}
		}
		List<AirReservation> airResList = universalRecordRetrieveRsp.getUniversalRecord().getAirReservation();
		if(airResList.size() > 0) {
			AirReservation airResInfo = airResList.get(0);
			List<SupplierLocator> supplierLocators = airResInfo.getSupplierLocator();
			if(supplierLocators.size() > 0) {		
				String airlinePNR = supplierLocators.get(0).getSupplierLocatorCode();
				pnrResponse.setAirlinePNR(airlinePNR);
			}
		}
		try {
			List<GeneralRemark> generalRemarks = universalRecordRetrieveRsp.getUniversalRecord().getGeneralRemark();
			if(generalRemarks != null && generalRemarks.size() > 0) {
				String remarkData = generalRemarks.get(0).getRemarkData();
				int i = remarkData.lastIndexOf("BY");
				String subString = remarkData.substring(i + 2);

				subString = subString.trim();
				String[] args1 = subString.split("/");
				String dateString = args1[0] + "/" + args1[1];
				dateString = dateString + calendar.get(Calendar.YEAR);
				SimpleDateFormat sdf = new SimpleDateFormat("HHmm/ddMMMyyyy");

				lastDate = sdf.parse(dateString);
			} else {
				calendar.setTime(new Date());
				calendar.add(Calendar.HOUR_OF_DAY, 6);
				lastDate = calendar.getTime();
			}
		} catch (Exception e) {
			e.printStackTrace();
			calendar.setTime(new Date());
			calendar.add(Calendar.HOUR_OF_DAY, 6);
			lastDate = calendar.getTime();
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
			TypeCabinClass typeCabinClass = TypeCabinClass
					.valueOf(travellerMasterInfo.getCabinClass().upperValue());

			AirPriceRsp priceRsp = AirRequestClient.priceItinerary(
					airItinerary, "INR",
					typeCabinClass, travellerMasterInfo);
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
			//logger.debug("Response======>>>>>\n"+ Json.toJson(universalRecordRetrieveRsp));
			// traveller deatials
			List<Traveller> travellerList = issuanceRequest.getTravellerList();
			
			
			for (Traveller traveller : travellerList) {
				Map<String, String> ticketMap = new HashMap<>();
				for (AirReservation airReservation : universalRecordRetrieveRsp.getUniversalRecord().getAirReservation()) {
					for (TypeBaseAirSegment airSegment : airReservation.getAirSegment()) {
						String key = airSegment.getOrigin()+airSegment.getDestination()+traveller.getContactId();
						ticketMap.put(key.toLowerCase(), airReservation.getDocumentInfo().getTicketInfo().get(0).getNumber());
					}
				}
				traveller.setTicketNumberMap(ticketMap);
			}
			

			List<Journey> journeyList = new ArrayList<>();
			List<AirSegmentInformation> airSegmentList = new ArrayList<>();
			FlightItinerary flightItinerary = new FlightItinerary();

			Journey journey = new Journey();
			for (AirReservation airReservation : universalRecordRetrieveRsp.getUniversalRecord().getAirReservation()) {
				

				for (TypeBaseAirSegment airSegment : airReservation.getAirSegment()) {
					AirSegmentInformation airSegmentInformation = new AirSegmentInformation();
					String carrierCode = airSegment.getCarrier();
					airSegmentInformation.setCarrierCode(carrierCode);
					airSegmentInformation.setFlightNumber(airSegment.getFlightNumber());
					SimpleDateFormat format= new SimpleDateFormat("yyyy-MM-dd"); 
					/*Date arrvalDate = format.parse(airSegment.getArrivalTime());
					airSegmentInformation.setArrivalDate(arrvalDate);

					Date depDate = format.parse(airSegment.getDepartureTime());
					airSegmentInformation.setDepartureDate(depDate);*/
					String arrivalDateTime = airSegment.getArrivalTime();
					String departureDateTime = airSegment.getDepartureTime();
					
					String fromLoc = airSegment.getOrigin();
					String toLoc = airSegment.getDestination();
					airSegmentInformation.setFromLocation(fromLoc);
					airSegmentInformation.setToLocation(airSegment.getDestination());
					airSegmentInformation.setTravelTime(airSegment.getTravelTime().toString());
					airSegmentInformation.setDistanceTravelled(airSegment.getDistance().toString());
					airSegmentInformation.setFromDate(arrivalDateTime);
					airSegmentInformation.setToDate(departureDateTime);
					airSegmentInformation.setArrivalDate(format.parse(arrivalDateTime));
					airSegmentInformation.setDepartureDate(format.parse(departureDateTime));
					
					airSegmentInformation.setAirline(Airline.getAirlineByCode(carrierCode));
					
					airSegmentInformation.setFromAirport(Airport.getAiport(fromLoc));
					airSegmentInformation.setToAirport(Airport.getAiport(toLoc));
					airSegmentInformation.setBookingClass(airSegment.getCabinClass().toString());
					
					for (FlightDetails flightDetails : airSegment.getFlightDetails()) {
						if (flightDetails.getOriginTerminal() != null) {
							airSegmentInformation.setFromTerminal(flightDetails.getOriginTerminal());
						}
						if (flightDetails.getDestinationTerminal() != null) {
							airSegmentInformation.setToTerminal(flightDetails.getDestinationTerminal());
						}
						airSegmentInformation.setEquipment(flightDetails.getEquipment());
					}
					//airSegmentList.add(airSegmentInformation);
					for (AirPricingInfo airPricingInfo : airReservation.getAirPricingInfo()) {
						FlightInfo flightInfo = new FlightInfo();
						for (FareInfo fareInfo : airPricingInfo.getFareInfo()) {
							flightInfo.setBaggageUnit(fareInfo.getBaggageAllowance().getMaxWeight().getUnit().toString());
							flightInfo.setBaggageAllowance(fareInfo.getBaggageAllowance().getMaxWeight().getValue());
						}
						airSegmentInformation.setFlightInfo(flightInfo);
					}
					airSegmentList.add(airSegmentInformation);
				}
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
			logger.debug("\n<<<<<<<<===================masterInfo======>>>>>\n" + Json.toJson(masterInfo));

		} catch (Exception e) {
			e.printStackTrace();
		}

		return masterInfo;
	}

    public LowFareResponse getLowestFare(String pnr, String provider, boolean isSeamen) {
        TerminalRequestClient terminalRequestClient = new TerminalRequestClient();
        LowFareResponse lowFareRS = new LowFareResponse();
        String token = terminalRequestClient.createTerminalSession();
        List<String> lowestFareTextList= terminalRequestClient.getLowestFare(token, isSeamen);
        System.out.println("==========>> Lowest Fare "+ lowestFareTextList);
        String lowestPriceText = lowestFareTextList.get(5);
        BigDecimal lowestFarePrice = StringUtility.getLowestFareFromString(lowestPriceText);
        lowFareRS.setAmount(lowestFarePrice);
        lowFareRS.setGdsPnr(pnr);
        return lowFareRS;
    }
}