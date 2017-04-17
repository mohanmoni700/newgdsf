package services;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

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
			for (Journey journey : seamen ? flightItinerary.getJourneyList() : flightItinerary.getNonSeamenJourneyList()) {
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
								// TODO: set airline based Standard Baggage
							} else if(baggageInfo.getBaggage().endsWith("P")){
								bagInfo.setBaggageAllowance(new BigInteger(baggageInfo.getBaggage().replaceAll("\\D+", "")));
								bagInfo.setBaggageUnit("PC");
								airSegment.setFlightInfo(bagInfo);
							}
							break;
						}
					}
				}
			}

		} catch (Exception e){
			mystiflyLogger.error("Error in Mystifly getBaggageInfo", e);
			e.printStackTrace();
		}
		return flightItinerary;
	}

	public String getMystiflyFareRules(FlightItinerary flightItinerary,SearchParameters searchParam, boolean seamen) {
		AirRulesClient airRulesClient = new AirRulesClient();
		StringBuilder fareRule = new StringBuilder();
		AirRulesRS airRulesRS = airRulesClient.getAirRules(flightItinerary.getPricingInformation().getFareSourceCode());
		FareRule[] fareRules = airRulesRS.getFareRules().getFareRuleArray();
		try {
			for(FareRule fareRule1:fareRules){
				RuleDetail[] ruleDetail = fareRule1.getRuleDetails().getRuleDetailArray();
				for(RuleDetail ruleDetail1 : ruleDetail){
					fareRule.append(ruleDetail1.getRules());
				}
			}
			logger.debug("Fare Rules "+fareRule.toString());
		} catch (Exception e){
			mystiflyLogger.error("Error in Mystifly getFareRules", e);
			e.printStackTrace();
		}
		return fareRule.toString().replace("\n", "").replace("\r", "");
	}

	public FlightItinerary getInFlightDetails(FlightItinerary flightItinerary, boolean seamen) {
		return flightItinerary;
	}

}
