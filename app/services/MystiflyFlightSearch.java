package services;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import models.Airport;

import org.datacontract.schemas._2004._07.mystifly_onepoint.AirItineraryPricingInfo;
import org.datacontract.schemas._2004._07.mystifly_onepoint.AirLowFareSearchRS;
import org.datacontract.schemas._2004._07.mystifly_onepoint.ArrayOfFlightSegment;
import org.datacontract.schemas._2004._07.mystifly_onepoint.ArrayOfOriginDestinationOption;
import org.datacontract.schemas._2004._07.mystifly_onepoint.ArrayOfPricedItinerary;
import org.datacontract.schemas._2004._07.mystifly_onepoint.FlightSegment;
import org.datacontract.schemas._2004._07.mystifly_onepoint.ItinTotalFare;
import org.datacontract.schemas._2004._07.mystifly_onepoint.OperatingAirline;
import org.datacontract.schemas._2004._07.mystifly_onepoint.OriginDestinationOption;
import org.datacontract.schemas._2004._07.mystifly_onepoint.PricedItinerary;
import org.springframework.stereotype.Service;

import play.Logger;

import com.compassites.GDSWrapper.mystifly.LowFareRequestClient;
import com.compassites.GDSWrapper.mystifly.Mystifly;
import com.compassites.exceptions.IncompleteDetailsMessage;
import com.compassites.exceptions.RetryException;
import com.compassites.model.AirSegmentInformation;
import com.compassites.model.AirSolution;
import com.compassites.model.FlightItinerary;
import com.compassites.model.Journey;
import com.compassites.model.JourneyType;
import com.compassites.model.PricingInformation;
import com.compassites.model.SearchJourney;
import com.compassites.model.SearchParameters;
import com.compassites.model.SearchResponse;

/**
 * 
 * @author Santhosh
 */
@Service
public class MystiflyFlightSearch implements FlightSearch {

	private SearchParameters searchParams;

	@RetryOnFailure(attempts = 2, delay = 2000, exception = RetryException.class)
	public SearchResponse search(SearchParameters searchParameters)
			throws IncompleteDetailsMessage, Exception {
		Logger.info("[Mystifly] search called at " + new Date());
		searchParams = searchParameters;

		SearchResponse searchResponse = new SearchResponse();
		LowFareRequestClient lowFareRequestClient = new LowFareRequestClient();
		AirLowFareSearchRS searchRS = lowFareRequestClient
				.search(searchParameters);
		AirSolution airSolution = createAirSolution(searchRS);
		searchResponse.setAirSolution(airSolution);
		return searchResponse;
	}

	public String provider() {
		return Mystifly.PROVIDER;
	}

	private AirSolution createAirSolution(AirLowFareSearchRS searchRS) {
		AirSolution airSolution = new AirSolution();
		ArrayOfPricedItinerary pricedItineraries = searchRS
				.getPricedItineraries();

		List<FlightItinerary> flightItineraryList = addFlightItineraryList(pricedItineraries);
		airSolution.setFlightItineraryList(flightItineraryList);
		return airSolution;
	}

	private List<FlightItinerary> addFlightItineraryList(
			ArrayOfPricedItinerary pricedItineraries) {
		List<FlightItinerary> flightItineraryList = new ArrayList<>();
		for (PricedItinerary pricedItinerary : pricedItineraries
				.getPricedItineraryArray()) {
			FlightItinerary flightItinerary = new FlightItinerary();
			flightItinerary.setProvider(Mystifly.PROVIDER);

			AirItineraryPricingInfo airlinePricingInfo = pricedItinerary
					.getAirItineraryPricingInfo();

			PricingInformation pricingInfo = setPricingInformtions(airlinePricingInfo);
			flightItinerary.setPricingInformation(pricingInfo);

			ArrayOfOriginDestinationOption arrayOfOriginDestinationOptions = pricedItinerary
					.getOriginDestinationOptions();
			List<Journey> journies = getJournies(arrayOfOriginDestinationOptions);
			flightItinerary.setFareSourceCode(airlinePricingInfo
					.getFareSourceCode());
			flightItinerary.setJourneyList(journies);
			flightItineraryList.add(flightItinerary);
		}
		return flightItineraryList;
	}

	private PricingInformation setPricingInformtions(
			AirItineraryPricingInfo airlinePricingInfo) {
		ItinTotalFare itinTotalFare = airlinePricingInfo.getItinTotalFare();
		PricingInformation pricingInfo = new PricingInformation();
		pricingInfo.setCurrency(itinTotalFare.getBaseFare().getCurrencyCode());

		// TODO: Fix decimals.
		String baseFare = itinTotalFare.getBaseFare().getAmount();
		pricingInfo.setBasePrice(baseFare.substring(0, baseFare.length() - 3));
		String tax = itinTotalFare.getTotalTax().getAmount();
		pricingInfo.setTax(tax.substring(0, tax.length() - 3));
		String total = itinTotalFare.getTotalFare().getAmount();
		pricingInfo.setTotalPrice(total.substring(0, total.length() - 3));
		pricingInfo.setTotalPriceValue(Long.parseLong(pricingInfo
				.getTotalPrice()));
		return pricingInfo;
	}

	private List<Journey> getJournies(
			ArrayOfOriginDestinationOption originDestinationOptions) {
		List<Journey> journies = new ArrayList<>();
		if (searchParams.getJourneyType().equals(JourneyType.MULTI_CITY)) {
			// Multi-city results are coming a list of flightsegments inside one
			// OriginDestinationOption. WTF!!
			journies = getMultiCityJournies(originDestinationOptions
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
				journies.add(journey);
			}
		}
		return journies;
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

	private List<Journey> getMultiCityJournies(
			ArrayOfFlightSegment flightSegments) {
		List<SearchJourney> searchJournies = searchParams.getJourneyList();
		FlightSegment[] segmentList = flightSegments.getFlightSegmentArray();
		List<Journey> journies = new ArrayList<>();
		int count = 0;

		// Assuming flightSegments are ordered
		for (SearchJourney searchJourney : searchJournies) {
			Journey journey = new Journey();
			List<AirSegmentInformation> airSegmentList = new ArrayList<>();
			for (int i = count; i < segmentList.length; i++) {
				FlightSegment segment = segmentList[i];
				airSegmentList.add(createAirSegment(segment));
				count++;
				if (searchJourney.getDestination().equalsIgnoreCase(
						segment.getArrivalAirportLocationCode())) {
					break;
				}
			}
			journey.setAirSegmentList(airSegmentList);
			journey.setNoOfStops(airSegmentList.size() - 1);
			journies.add(journey);
		}
		return journies;
	}

	private AirSegmentInformation createAirSegment(FlightSegment flightSegment) {
		AirSegmentInformation airSegmentInfo = new AirSegmentInformation();
		airSegmentInfo.setArrivalTime(flightSegment.getArrivalDateTime()
				.toString());
		airSegmentInfo.setBookingClass(flightSegment.getCabinClassCode());

		// TODO: verify
		airSegmentInfo.setConnectionTime(flightSegment.getJourneyDuration());
		airSegmentInfo.setConnectionTimeStr();
		Calendar departureDate = flightSegment.getDepartureDateTime();
		airSegmentInfo.setDepartureDate(departureDate.getTime());
		airSegmentInfo.setDepartureTime(departureDate.toString());
		OperatingAirline airline = flightSegment.getOperatingAirline();
		airSegmentInfo.setFlightNumber(airline.getFlightNumber());
		airSegmentInfo.setCarrierCode(flightSegment.getMarketingAirlineCode());
		airSegmentInfo.setFromAirport(Airport.getAiport(flightSegment
				.getDepartureAirportLocationCode()));

		// TODO: Check timezone
		airSegmentInfo.setFromDate(flightSegment.getDepartureDateTime()
				.getTime().toString());
		airSegmentInfo.setFromLocation(flightSegment
				.getDepartureAirportLocationCode());
		airSegmentInfo.setToAirport(Airport.getAiport(flightSegment
				.getArrivalAirportLocationCode()));
		airSegmentInfo.setToDate(flightSegment.getArrivalDateTime().getTime()
				.toString());
		airSegmentInfo.setToLocation(flightSegment
				.getArrivalAirportLocationCode());
		airSegmentInfo.setTravelTime("" + flightSegment.getJourneyDuration());
		return airSegmentInfo;
	}

}
