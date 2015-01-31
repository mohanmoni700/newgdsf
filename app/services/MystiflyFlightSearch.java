package services;

import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;

import models.Airline;
import models.Airport;

import org.datacontract.schemas._2004._07.mystifly_onepoint.AirItineraryPricingInfo;
import org.datacontract.schemas._2004._07.mystifly_onepoint.AirLowFareSearchRS;
import org.datacontract.schemas._2004._07.mystifly_onepoint.ArrayOfFlightSegment;
import org.datacontract.schemas._2004._07.mystifly_onepoint.ArrayOfOriginDestinationOption;
import org.datacontract.schemas._2004._07.mystifly_onepoint.ArrayOfPricedItinerary;
import org.datacontract.schemas._2004._07.mystifly_onepoint.FareType;
import org.datacontract.schemas._2004._07.mystifly_onepoint.FlightSegment;
import org.datacontract.schemas._2004._07.mystifly_onepoint.ItinTotalFare;
import org.datacontract.schemas._2004._07.mystifly_onepoint.OperatingAirline;
import org.datacontract.schemas._2004._07.mystifly_onepoint.OriginDestinationOption;
import org.datacontract.schemas._2004._07.mystifly_onepoint.PTCFareBreakdown;
import org.datacontract.schemas._2004._07.mystifly_onepoint.PassengerFare;
import org.datacontract.schemas._2004._07.mystifly_onepoint.PassengerTypeQuantity;
import org.datacontract.schemas._2004._07.mystifly_onepoint.PricedItinerary;
import org.datacontract.schemas._2004._07.mystifly_onepoint.Tax;
import org.springframework.stereotype.Service;

import play.Logger;
import utils.ErrorMessageHelper;

import com.compassites.GDSWrapper.mystifly.AirLowFareSearchClient;
import com.compassites.GDSWrapper.mystifly.Mystifly;
import com.compassites.exceptions.IncompleteDetailsMessage;
import com.compassites.exceptions.RetryException;
import com.compassites.model.AirSegmentInformation;
import com.compassites.model.AirSolution;
import com.compassites.model.ErrorMessage;
import com.compassites.model.FlightItinerary;
import com.compassites.model.Journey;
import com.compassites.model.JourneyType;
import com.compassites.model.PassengerTax;
import com.compassites.model.PricingInformation;
import com.compassites.model.SearchJourney;
import com.compassites.model.SearchParameters;
import com.compassites.model.SearchResponse;

/**
 * @author Santhosh
 */
@Service
public class MystiflyFlightSearch implements FlightSearch {

	private SearchParameters searchParams;

	@RetryOnFailure(attempts = 2, delay = 2000, exception = RetryException.class)
	public SearchResponse search(SearchParameters searchParameters)
			throws IncompleteDetailsMessage {
		Logger.info("[Mystifly] search called at " + new Date());
		searchParams = searchParameters;

		SearchResponse searchResponse = new SearchResponse();
		AirLowFareSearchClient lowFareRequestClient = new AirLowFareSearchClient();
		AirLowFareSearchRS searchRS = null;
		try {
			searchRS = lowFareRequestClient.search(searchParameters);
		} catch (RemoteException remoteException) {
			throw new IncompleteDetailsMessage(remoteException.getMessage(),
					remoteException.getCause());
		}
		if (searchRS.getSuccess()) {
			AirSolution airSolution = createAirSolution(searchRS);
			searchResponse.setAirSolution(airSolution);
		} else {
			ErrorMessage errorMsg = ErrorMessageHelper.createErrorMessage(
					"partialResults", ErrorMessage.ErrorType.ERROR, provider());
			searchResponse.getErrorMessageList().add(errorMsg);
		}
		return searchResponse;
	}

	public String provider() {
		return Mystifly.PROVIDER;
	}

	private AirSolution createAirSolution(AirLowFareSearchRS searchRS) {
		AirSolution airSolution = new AirSolution();
		ArrayOfPricedItinerary pricedItineraries = searchRS
				.getPricedItineraries();
		List<FlightItinerary> itineraryList = createItineraryList(pricedItineraries);
		HashMap<Integer, FlightItinerary> itineraryHashMap = new HashMap<>();
		for (FlightItinerary itinerary : itineraryList) {
			itineraryHashMap.put(itinerary.hashCode(), itinerary);
		}
		airSolution.setNonSeamenHashMap(itineraryHashMap);
		return airSolution;
	}

	private List<FlightItinerary> createItineraryList(
			ArrayOfPricedItinerary pricedItineraries) {
		List<FlightItinerary> flightItineraryList = new ArrayList<>();
		for (PricedItinerary pricedItinerary : pricedItineraries
				.getPricedItineraryArray()) {
			FlightItinerary flightItinerary = new FlightItinerary();
			AirItineraryPricingInfo airlinePricingInfo = pricedItinerary
					.getAirItineraryPricingInfo();
			PricingInformation pricingInfo = setPricingInformtions(airlinePricingInfo);
			flightItinerary.setPricingInformation(pricingInfo);
			ArrayOfOriginDestinationOption arrayOfOriginDestinationOptions = pricedItinerary
					.getOriginDestinationOptions();
			List<Journey> journeys = getJourneys(arrayOfOriginDestinationOptions);
			flightItinerary.setFareSourceCode(airlinePricingInfo
					.getFareSourceCode());
			flightItinerary.setNonSeamenJourneyList(journeys);
			flightItinerary.setJourneyList(journeys);
			flightItineraryList.add(flightItinerary);
		}
		return flightItineraryList;
	}

	private PricingInformation setPricingInformtions(
			AirItineraryPricingInfo airlinePricingInfo) {
		ItinTotalFare itinTotalFare = airlinePricingInfo.getItinTotalFare();
		PricingInformation pricingInfo = new PricingInformation();
		pricingInfo.setProvider(Mystifly.PROVIDER);
		pricingInfo
				.setLCC(airlinePricingInfo.getFareType() == FareType.WEB_FARE);
		pricingInfo.setCurrency(itinTotalFare.getBaseFare().getCurrencyCode());
		String baseFare = itinTotalFare.getEquivFare().getAmount();
		pricingInfo.setBasePrice(new BigDecimal(baseFare));
		String totalTax = itinTotalFare.getTotalTax().getAmount();
		pricingInfo.setTax(new BigDecimal(totalTax));
		String total = itinTotalFare.getTotalFare().getAmount();
		pricingInfo.setTotalPrice(new BigDecimal(total));
		pricingInfo.setGdsCurrency("INR");
		pricingInfo.setTotalPriceValue(pricingInfo.getTotalPrice());
		setFareBeakup(pricingInfo, airlinePricingInfo);
		return pricingInfo;
	}

	private void setFareBeakup(PricingInformation pricingInfo,
			AirItineraryPricingInfo airlinePricingInfo) {
		List<PassengerTax> passengerTaxes = new ArrayList<>();
		for (PTCFareBreakdown ptcFareBreakdown : airlinePricingInfo
				.getPTCFareBreakdowns().getPTCFareBreakdownArray()) {

			PassengerTax passengerTax = new PassengerTax();
			PassengerFare paxFare = ptcFareBreakdown.getPassengerFare();
//			passengerTax.setBaseFare(new BigDecimal(paxFare.getBaseFare()
//					.getAmount()));
			PassengerTypeQuantity passenger = ptcFareBreakdown
					.getPassengerTypeQuantity();
			passengerTax.setPassengerCount(passenger.getQuantity());
			passengerTax.setPassengerType(passenger.getCode().toString());

			Map<String, BigDecimal> taxes = new HashMap<>();
			for (Tax tax : ptcFareBreakdown.getPassengerFare().getTaxes()
					.getTaxArray()) {
				taxes.put(tax.getTaxCode(), new BigDecimal(tax.getAmount()));
			}
			passengerTax.setTaxes(taxes);
			passengerTaxes.add(passengerTax);
			String paxCode = ptcFareBreakdown.getPassengerTypeQuantity()
					.getCode().toString();
			BigDecimal amount = new BigDecimal(paxFare.getBaseFare()
					.getAmount());
			if (paxCode.equalsIgnoreCase("ADT")) {
				pricingInfo.setAdtBasePrice(amount);
			} else if (paxCode.equalsIgnoreCase("CHD")) {
				pricingInfo.setChdBasePrice(amount);
			} else if (paxCode.equalsIgnoreCase("INF")) {
				pricingInfo.setInfBasePrice(amount);
			}
		}
		pricingInfo.setPassengerTaxes(passengerTaxes);
	}

	private List<Journey> getJourneys(
			ArrayOfOriginDestinationOption originDestinationOptions) {
		List<Journey> journeys = new ArrayList<>();
		if (searchParams.getJourneyType().equals(JourneyType.MULTI_CITY)) {
			// Multi-city results are coming a list of flightsegments inside one
			// OriginDestinationOption. WTF!!
			journeys = getMultiCityjourneys(originDestinationOptions
					.getOriginDestinationOptionArray(0).getFlightSegments());
		} else {
			for (OriginDestinationOption originDestinationOption : originDestinationOptions
					.getOriginDestinationOptionArray()) {
				Journey journey = new Journey();
				ArrayOfFlightSegment arrayOfFlightSegment = originDestinationOption
						.getFlightSegments();
				List<AirSegmentInformation> airSegmentInformations = addAirSegmentInformations(arrayOfFlightSegment);
				journey.setAirSegmentList(airSegmentInformations);
				journey.setNoOfStops(airSegmentInformations.size() - 1);
				journey.setTravelTime(getTravelTime(airSegmentInformations));
				journeys.add(journey);
			}
		}
		return journeys;
	}

	private List<AirSegmentInformation> addAirSegmentInformations(
			ArrayOfFlightSegment flightSegments) {
		List<AirSegmentInformation> airSegmentInformations = new ArrayList<>();
		for (FlightSegment flightSegment : flightSegments
				.getFlightSegmentArray()) {
			AirSegmentInformation airSegmentInfo = createAirSegment(flightSegment);
			airSegmentInformations.add(airSegmentInfo);
		}
		return airSegmentInformations;
	}

	private List<Journey> getMultiCityjourneys(
			ArrayOfFlightSegment flightSegments) {
		List<SearchJourney> searchjourneys = searchParams.getJourneyList();
		FlightSegment[] segmentList = flightSegments.getFlightSegmentArray();
		List<Journey> journeys = new ArrayList<>();
		int count = 0;
		for (SearchJourney searchJourney : searchjourneys) {
			Journey journey = new Journey();
			List<AirSegmentInformation> airSegmentList = new ArrayList<>();
			for (int i = count; i < segmentList.length; i++) {
				FlightSegment segment = segmentList[i];
				airSegmentList.add(createAirSegment(segment));
				count++;
				if (searchJourney.getDestination().equalsIgnoreCase(
						segment.getArrivalAirportLocationCode()))
					break;
			}
			journey.setAirSegmentList(airSegmentList);
			journey.setTravelTime(getTravelTime(airSegmentList));
			journey.setNoOfStops(airSegmentList.size() - 1);
			journey.setProvider(Mystifly.PROVIDER);
			journeys.add(journey);
		}
		return journeys;
	}

	private AirSegmentInformation createAirSegment(FlightSegment flightSegment) {
		AirSegmentInformation airSegment = new AirSegmentInformation();
		airSegment
				.setArrivalTime(flightSegment.getArrivalDateTime().toString());
		airSegment.setBookingClass(flightSegment.getCabinClassCode());
		airSegment.setConnectionTime(flightSegment.getJourneyDuration());
		airSegment.setConnectionTimeStr();
		Calendar departureDate = flightSegment.getDepartureDateTime();
		airSegment.setDepartureDate(departureDate.getTime());
		airSegment.setDepartureTime(departureDate.toString());
		OperatingAirline airline = flightSegment.getOperatingAirline();
		airSegment.setFlightNumber(airline.getFlightNumber());
		airSegment.setEquipment(airline.getEquipment());
		airSegment.setCarrierCode(flightSegment.getMarketingAirlineCode());
		airSegment.setFromAirport(Airport.getAiport(flightSegment
				.getDepartureAirportLocationCode()));
		airSegment.setFromDate(flightSegment.getDepartureDateTime().getTime()
				.toString());
		airSegment.setFromLocation(flightSegment
				.getDepartureAirportLocationCode());
		airSegment.setToAirport(Airport.getAiport(flightSegment
				.getArrivalAirportLocationCode()));
		airSegment.setToDate(flightSegment.getArrivalDateTime().getTime()
				.toString());
		airSegment.setAirline(Airline.getAirlineByCode(flightSegment
				.getMarketingAirlineCode()));
		airSegment.setToLocation(flightSegment.getArrivalAirportLocationCode());
		airSegment.setTravelTime("" + flightSegment.getJourneyDuration());
		return airSegment;
	}

	private Duration getTravelTime(List<AirSegmentInformation> airSegments) {
		List<Long> durations = new ArrayList<>();
		Long duration = 0L;
		Duration travelTime = null;
		for (AirSegmentInformation airSegment : airSegments) {
			Long flightTime = Long.parseLong(airSegment.getTravelTime());
			durations.add(flightTime * 60 * 1000); // mins to milli secs.
		}
		try {
			if (airSegments.size() > 1) {
				for (int i = 1; i < airSegments.size(); i++) {
					Long arrivalTime = Mystifly.DATE_FORMAT.parse(
							airSegments.get(i - 1).getArrivalTime()).getTime();
					Long departureTime = Mystifly.DATE_FORMAT.parse(
							airSegments.get(i).getDepartureTime()).getTime();
					Long transit = departureTime - arrivalTime;
					airSegments.get(i - 1).setConnectionTime(
							Integer.valueOf((int) (transit / 60000)));
					durations.add(transit);
				}
			}
			for (Long dur : durations) {
				duration += dur;
			}
			travelTime = DatatypeFactory.newInstance().newDuration(duration);
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (DatatypeConfigurationException e) {
			e.printStackTrace();
		}
		return travelTime;
	}

}
