package services;

import java.math.BigInteger;

import org.datacontract.schemas._2004._07.mystifly_onepoint_airrules1_1.AirRulesRS;
import org.datacontract.schemas._2004._07.mystifly_onepoint_airrules1_1.BaggageInfo;
import org.springframework.stereotype.Service;

import com.compassites.GDSWrapper.mystifly.AirRulesClient;
import com.compassites.model.AirSegmentInformation;
import com.compassites.model.FlightItinerary;
import com.compassites.model.Journey;
import com.compassites.model.SearchParameters;

/**
 * @author Santhosh
 */
@Service
public class MystiflyFlightInfoServiceImpl implements FlightInfoService {

	@Override
	public FlightItinerary getFlightnfo(FlightItinerary flightItinerary,
			SearchParameters searchParams) {
		AirRulesClient airRulesClient = new AirRulesClient();
		AirRulesRS airRulesRS = airRulesClient.getAirRules(flightItinerary
				.getFareSourceCode());

		BaggageInfo[] baggageInfos = airRulesRS.getBaggageInfos()
				.getBaggageInfoArray();
		for (Journey journey : flightItinerary.getJourneyList()) {
			for (AirSegmentInformation airSegment : journey.getAirSegmentList()) {
				for (BaggageInfo baggageInfo : baggageInfos) {
					if (baggageInfo.getArrival().equalsIgnoreCase(
							airSegment.getToLocation())) {
						com.compassites.model.BaggageInfo bagInfo = new com.compassites.model.BaggageInfo();
						if (baggageInfo.getBaggage().toLowerCase()
								.endsWith("k")) {
							bagInfo.setValue(new BigInteger(baggageInfo
									.getBaggage().replaceAll("\\D+", "")));
							bagInfo.setUnit("Kg");
							airSegment.setBaggageInfo(bagInfo);
						} else if (baggageInfo.getBaggage().equals("SB")) {
							bagInfo.setUnit(baggageInfo.getBaggage());
							airSegment.setBaggageInfo(bagInfo);
							// TODO: set airline based Standard Baggage
						}
						break;
					}
				}
			}
		}
		return flightItinerary;
	}

}
