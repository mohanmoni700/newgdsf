package services;

import java.math.BigInteger;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.compassites.GDSWrapper.mystifly.AirRevalidateClient;
import org.datacontract.schemas._2004._07.mystifly_onepoint.AirRevalidateRS;
import org.datacontract.schemas._2004._07.mystifly_onepoint_airrules1_1.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

	static org.slf4j.Logger logger = LoggerFactory.getLogger("gds");

	static Logger mystiflyLogger = LoggerFactory.getLogger("mystifly");

	@Override
	public FlightItinerary getBaggageInfo(FlightItinerary flightItinerary,SearchParameters searchParam, boolean seamen) {

		AirRulesClient airRulesClient = new AirRulesClient();
		AirRulesRS airRulesRS = airRulesClient.getAirRules(flightItinerary.getPricingInformation().getFareSourceCode());
		BaggageInfo[] baggageInfos = airRulesRS.getBaggageInfos().getBaggageInfoArray();
		try {
			List<Journey> journeyList = new ArrayList<>();
			for (Journey journey : seamen ? flightItinerary.getJourneyList() : flightItinerary.getNonSeamenJourneyList()) {
				List<AirSegmentInformation> airSegmentInformationList = new ArrayList<>();
				for (AirSegmentInformation airSegment : journey.getAirSegmentList()) {
					for (BaggageInfo baggageInfo : baggageInfos) {
						if (baggageInfo.getArrival().equalsIgnoreCase(airSegment.getToLocation())) {
							com.compassites.model.FlightInfo bagInfo = new com.compassites.model.FlightInfo();
							if (baggageInfo.getBaggage().toLowerCase().endsWith("k")) {
								bagInfo.setBaggageAllowance(new BigInteger(baggageInfo.getBaggage().replaceAll("\\D+", "")));
								bagInfo.setBaggageUnit("Kg");
								airSegment.setFlightInfo(bagInfo);
							} else if (baggageInfo.getBaggage().equals("SB")) {
								bagInfo.setBaggageUnit(baggageInfo.getBaggage());
								airSegment.setFlightInfo(bagInfo);
							} else if(baggageInfo.getBaggage().endsWith("P")){
								bagInfo.setBaggageAllowance(new BigInteger(baggageInfo.getBaggage().replaceAll("\\D+", "")));
								bagInfo.setBaggageUnit("PC");
								airSegment.setFlightInfo(bagInfo);
							}
							break;
						}
					}
					airSegmentInformationList.add(airSegment);
				}
				journey.setAirSegmentList(airSegmentInformationList);
				journeyList.add(journey);
			}
			flightItinerary.setJourneyList(journeyList);
		} catch (Exception e){
			mystiflyLogger.error("Error in Mystifly getBaggageInfo", e);
			e.printStackTrace();
		}
		return flightItinerary;
	}

	public String getMystiflyFareRules(FlightItinerary flightItinerary,SearchParameters searchParam, boolean seamen) {
		AirRulesClient airRulesClient = new AirRulesClient();
		StringBuilder fareRule = new StringBuilder();
		String fareSourceCode = flightItinerary.getPricingInformation().getFareSourceCode();
		String fareRuleString = "";
		AirRevalidateClient revalidateClient = new AirRevalidateClient();
		AirRevalidateRS revalidateRS;
		try {
			revalidateRS = revalidateClient.revalidate(fareSourceCode);
			if(!revalidateRS.isNilPricedItineraries()) {
				AirRulesRS airRulesRS = airRulesClient.getAirRules(revalidateRS.getPricedItineraries().getPricedItineraryArray(0).getAirItineraryPricingInfo().getFareSourceCode());
				FareRule[] fareRules = airRulesRS.getFareRules().getFareRuleArray();
				try {
					if (fareRules.length != 0) {
						for (FareRule fareRule1 : fareRules) {
							RuleDetail[] ruleDetail = fareRule1.getRuleDetails().getRuleDetailArray();
							for (RuleDetail ruleDetail1 : ruleDetail) {
								fareRule.append(ruleDetail1.getRules());
							}
						}
						//fareRuleString = fareRule.toString().replace("\n", "").replace("<br>", "").replace("\t", "").replace("\r", "").replace("\"", "");
						fareRuleString = fareRule.toString().replaceAll("\\<.*?\\>", "").replace("\n", "").replace("\t", "").replace("\r", "");
						logger.debug("Fare Rules " + fareRule.toString());
					} else {
						fareRuleString = "No Fare Rules";
					}
				} catch (Exception e) {
					mystiflyLogger.error("Error in Mystifly getFareRules", e);
					e.printStackTrace();
				}
			} else {
				fareRuleString = "No Fare";
			}
		} catch (RemoteException rm){

		}
		return fareRuleString;
	}

	public FlightItinerary getInFlightDetails(FlightItinerary flightItinerary, boolean seamen) {
		return flightItinerary;
	}

}
