package services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.compassites.GDSWrapper.travelport.FlightDetailsClient;
import com.compassites.model.AirSegmentInformation;
import com.compassites.model.FlightInfo;
import com.compassites.model.FlightItinerary;
import com.compassites.model.Journey;
import com.compassites.model.SearchParameters;
import com.travelport.schema.air_v26_0.FlightDetailsRsp;
import com.travelport.schema.air_v26_0.TypeBaseAirSegment;
import com.travelport.schema.air_v26_0.TypeInFlightService;

/**
 * @author Santhosh
 */
@Service
public class TravelportFlightInfoServiceImpl implements FlightInfoService {

	public FlightItinerary getBaggageInfo(FlightItinerary flightItinerary,
			SearchParameters searchParams, boolean seamen) {
		return flightItinerary;
	}

	public FlightItinerary getInFlightDetails(FlightItinerary flightItinerary, boolean seamen) {
		FlightDetailsClient flightDetailsClient = new FlightDetailsClient();
		List<Journey> journeyList = seamen ? flightItinerary.getJourneyList() : flightItinerary.getNonSeamenJourneyList();
		FlightDetailsRsp flightDetailsRsp = flightDetailsClient
				.getFlightDetails(journeyList);

		for (Journey journey : journeyList) {
			for (AirSegmentInformation segment : journey.getAirSegmentList()) {
				TypeBaseAirSegment airsegment = getAirsegmentByKey(
						flightDetailsRsp.getAirSegment(),
						segment.getAirSegmentKey());

				List<String> amenities = new ArrayList<>();
				for (TypeInFlightService inFlightService : airsegment
						.getFlightDetails().get(0).getInFlightServices()) {
					amenities.add(inFlightService.name());
				}
				if (segment.getFlightInfo() != null) {
					segment.getFlightInfo().setAmenities(amenities);
				} else {
					FlightInfo flightInfo = new FlightInfo();
					flightInfo.setAmenities(amenities);
					segment.setFlightInfo(flightInfo);
				}
			}
		}
		return flightItinerary;
	}

	private TypeBaseAirSegment getAirsegmentByKey(
			List<TypeBaseAirSegment> airsegments, String key) {
		for (TypeBaseAirSegment airsegment : airsegments) {
			if (airsegment.getKey().equals(key))
				return airsegment;
		}
		return null;
	}

}
