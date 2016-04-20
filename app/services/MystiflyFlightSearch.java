package services;

import com.compassites.GDSWrapper.mystifly.AirLowFareSearchClient;
import com.compassites.GDSWrapper.mystifly.Mystifly;
import com.compassites.exceptions.IncompleteDetailsMessage;
import com.compassites.exceptions.RetryException;
import com.compassites.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import models.Airline;
import models.Airport;
import org.datacontract.schemas._2004._07.mystifly_onepoint.*;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import play.libs.Json;
import utils.ErrorMessageHelper;
import utils.MystiflyHelper;
import utils.XMLFileUtility;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import java.math.BigDecimal;
import java.rmi.RemoteException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Santhosh
 */
@Service
public class MystiflyFlightSearch implements FlightSearch {

	private SearchParameters searchParams;

	static Logger logger = LoggerFactory.getLogger("gds");

	@RetryOnFailure(attempts = 2, delay = 2000, exception = RetryException.class)
	public SearchResponse search(SearchParameters searchParameters)
			throws IncompleteDetailsMessage {
		logger.debug("[Mystifly] search started at " + new Date());
		searchParams = searchParameters;

        /*
            Return null if search Date type is arrival
         */
        JsonNode jsonNode = Json.toJson(searchParameters);
        if(jsonNode.findValue("dateType").asText().equals(DateType.ARRIVAL.name().toString())){
            return null;
        }

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
		XMLFileUtility.createXMLFile(searchResponse, "MystifySearchRes.xml");
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
		ConcurrentHashMap<Integer, FlightItinerary> itineraryHashMap = new ConcurrentHashMap<>();
		for (FlightItinerary itinerary : itineraryList) {
			itineraryHashMap.put(itinerary.hashCode(), itinerary);
		}
		airSolution.setNonSeamenHashMap(itineraryHashMap);
		//System.out.println("itineraryHashMap Mystify:\n"+itineraryHashMap);
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
			PricingInformation pricingInfo = MystiflyHelper.setPricingInformtions(airlinePricingInfo);
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
		airSegment.setArrivalTime(flightSegment.getArrivalDateTime().toString());
		airSegment.setArrivalDate(flightSegment.getArrivalDateTime().getTime());
		airSegment.setBookingClass(flightSegment.getCabinClassCode());
		airSegment.setConnectionTime(flightSegment.getJourneyDuration());
		airSegment.setConnectionTimeStr();
		Calendar departureDate = flightSegment.getDepartureDateTime();
		airSegment.setDepartureDate(departureDate.getTime());
		airSegment.setDepartureTime(departureDate.toString());

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
		airSegment.setDepartureTime(sdf.format(departureDate.getTime()));
		airSegment.setArrivalTime(sdf.format(flightSegment.getArrivalDateTime().getTime()));


		Airport fromAirport = new Airport();
		Airport toAirport = new Airport();
		fromAirport = Airport.getAiport(flightSegment.getDepartureAirportLocationCode());
		toAirport = Airport.getAiport(flightSegment.getArrivalAirportLocationCode());

		OperatingAirline airline = flightSegment.getOperatingAirline();
		airSegment.setFlightNumber(airline.getFlightNumber());
		airSegment.setEquipment(airline.getEquipment());
		airSegment.setOperatingCarrierCode(airline.getCode());
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
		if(airline.getCode() != null)
			airSegment.setOperatingAirline(Airline.getAirlineByCode(airline.getCode()));
		airSegment.setToLocation(flightSegment.getArrivalAirportLocationCode());
		airSegment.setTravelTime("" + flightSegment.getJourneyDuration());

		//added to fix timezone bug for departureDate
		String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
		DateTimeFormatter DATETIME_FORMATTER = DateTimeFormat.forPattern(DATE_FORMAT);
		DateTimeZone dateTimeZone = DateTimeZone.forID(fromAirport.getTime_zone());
		DateTime departureDateTime = DATETIME_FORMATTER.withZone(dateTimeZone).parseDateTime(flightSegment.getDepartureDateTime().toString());

		dateTimeZone = DateTimeZone.forID(toAirport.getTime_zone());
		DateTime arrivalDate = DATETIME_FORMATTER.withZone(dateTimeZone).parseDateTime(flightSegment.getArrivalDateTime().toString());
		airSegment.setDepartureDate(departureDateTime.toDate());
		airSegment.setDepartureTime(departureDateTime.toString());
		airSegment.setArrivalTime(arrivalDate.toString());
		airSegment.setArrivalDate(arrivalDate.toDate());

		//hopping
		List<HoppingFlightInformation> hoppingFlightInformations = null;
		if(flightSegment.getStopQuantity()!=0 ){
	        if(flightSegment.getStopQuantityInfo()!=null){
	        		HoppingFlightInformation hop =new HoppingFlightInformation();
		        	hop.setLocation(flightSegment.getStopQuantityInfo().getLocationCode());
		        	
					Date aDate = flightSegment.getStopQuantityInfo().getArrivalDateTime().getTime();
					Date dDate = flightSegment.getStopQuantityInfo().getDepartureDateTime().getTime();
					
					SimpleDateFormat dateFormater = new SimpleDateFormat("dd-MMM-yyyy");
					SimpleDateFormat timeFormater = new SimpleDateFormat("HH:mm");
					
					//Arrival
		        	hop.setStartTime(timeFormater.format(aDate));
		    		hop.setStartDate(dateFormater.format(aDate));
		        	//Departure
		        	hop.setEndTime(timeFormater.format(dDate));
		        	hop.setEndDate(dateFormater.format(dDate));
		        	
		        	if(hoppingFlightInformations==null){ 
		        		hoppingFlightInformations = new ArrayList<HoppingFlightInformation>();
		        	}
		        	hoppingFlightInformations.add(hop);
		        }
		}
        airSegment.setHoppingFlightInformations(hoppingFlightInformations);
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
