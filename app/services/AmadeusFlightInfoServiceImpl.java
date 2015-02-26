package services;

import com.amadeus.xml.farqnr_07_1_1a.FareCheckRulesReply;
import com.amadeus.xml.flires_07_1_1a.AirFlightInfoReply;
import com.amadeus.xml.flires_07_1_1a.AirFlightInfoReply.FlightScheduleDetails.InteractiveFreeText;
import com.amadeus.xml.tipnrr_12_4_1a.FareInformativePricingWithoutPNRReply;
import com.amadeus.xml.tipnrr_12_4_1a.FareInformativePricingWithoutPNRReply.MainGroup.PricingGroupLevelGroup;
import com.amadeus.xml.tipnrr_12_4_1a.FareInformativePricingWithoutPNRReply.MainGroup.PricingGroupLevelGroup.FareInfoGroup.SegmentLevelGroup;
import com.amadeus.xml.tipnrr_12_4_1a.FareInformativePricingWithoutPNRReply.MainGroup.PricingGroupLevelGroup.FareInfoGroup.SegmentLevelGroup.BaggageAllowance.BaggageDetails;
import com.compassites.GDSWrapper.amadeus.ServiceHandler;
import com.compassites.model.*;
import com.sun.xml.ws.fault.ServerSOAPFaultException;
import com.thoughtworks.xstream.XStream;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * @author Santhosh
 */

@Service
public class AmadeusFlightInfoServiceImpl implements FlightInfoService {

    static org.slf4j.Logger amadeusLogger = LoggerFactory.getLogger("amadeus");

	private static Map<String, String> baggageCodes = new HashMap<>();

    static {
		baggageCodes.put("700", "Kilos");
		baggageCodes.put("701", "Pounds");
		baggageCodes.put("C", "Special Charge");
		baggageCodes.put("N", "Number of pieces");
		baggageCodes.put("S", "Size");
		baggageCodes.put("V", "Value");
		baggageCodes.put("W", "Weight");
	}

	@Override
	public FlightItinerary getBaggageInfo(FlightItinerary flightItinerary, SearchParameters searchParams, boolean seamen) {
		try {
			ServiceHandler serviceHandler = new ServiceHandler();
			serviceHandler.logIn();
			List<Journey> journeyList = seamen ? flightItinerary.getJourneyList() : flightItinerary.getNonSeamenJourneyList();
			FareInformativePricingWithoutPNRReply reply = serviceHandler.getFareInfo(journeyList, searchParams.getAdultCount(), searchParams.getChildCount(), searchParams.getInfantCount());
			addBaggageInfo(flightItinerary, reply.getMainGroup().getPricingGroupLevelGroup(), seamen);
		} catch (ServerSOAPFaultException ssf) {
			ssf.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return flightItinerary;
	}
	
	public FlightItinerary getInFlightDetails(FlightItinerary flightItinerary, boolean seamen) {
		try {
			ServiceHandler serviceHandler = new ServiceHandler();
			serviceHandler.logIn();
			List<Journey> journeyList = seamen ? flightItinerary.getJourneyList() : flightItinerary.getNonSeamenJourneyList();
			for(Journey journey : journeyList) {
				for(AirSegmentInformation segment : journey.getAirSegmentList()) {
					AirFlightInfoReply flightInfoReply = serviceHandler.getFlightInfo(segment);
					List<String> amneties = new ArrayList<>();
					for(InteractiveFreeText freeText : flightInfoReply.getFlightScheduleDetails().getInteractiveFreeText()) {
						amneties.add(freeText.getFreeText());
					}
					if (segment.getFlightInfo() != null) {
						segment.getFlightInfo().setAmneties(amneties);
					} else {
						FlightInfo flightInfo = new FlightInfo();
						flightInfo.setAmneties(amneties);
						segment.setFlightInfo(flightInfo);
					}
				}
			}
		} catch (ServerSOAPFaultException ssf) {
			ssf.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return flightItinerary;
	}
	
	public String getCancellationFee(FlightItinerary flightItinerary, SearchParameters searchParams, boolean seamen) {
        String fareRules = "";
        try {
        	ServiceHandler serviceHandler = new ServiceHandler();
            serviceHandler.logIn();
			List<Journey> journeyList = seamen ? flightItinerary.getJourneyList() : flightItinerary.getNonSeamenJourneyList();
			FareInformativePricingWithoutPNRReply pricingReply = serviceHandler.getFareInfo(journeyList, searchParams.getAdultCount(), searchParams.getChildCount(), searchParams.getInfantCount());
//            XMLFileUtility.createXMLFile(pricingReply, "FareInformativePricingWithoutPNRReply.xml");
            amadeusLogger.debug("FareInformativePricingWithoutPNRReply "+ new Date()+" ------->>"+ new XStream().toXML(pricingReply));
			FareCheckRulesReply fareCheckRulesReply = serviceHandler.getFareRules();
            StringBuilder fareRule = new StringBuilder();
            for(FareCheckRulesReply.TariffInfo tariffInfo : fareCheckRulesReply.getTariffInfo()){
                if("(16)".equals(tariffInfo.getFareRuleInfo().getRuleCategoryCode())){
                    for(FareCheckRulesReply.TariffInfo.FareRuleText text : tariffInfo.getFareRuleText() ) {
                        fareRule.append(text.getFreeText().get(0));
                    }
                }
            }
            fareRules = fareRule.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return fareRules;
    }
	
	private void addBaggageInfo(FlightItinerary itinerary, List<PricingGroupLevelGroup> pricingLevelGroup, boolean seamen) {
		List<SegmentLevelGroup> segmentGrpList = new ArrayList<>();
		for(PricingGroupLevelGroup pricingLevelGrp : pricingLevelGroup) {
			for(SegmentLevelGroup segment : pricingLevelGrp.getFareInfoGroup().getSegmentLevelGroup()) {
				segmentGrpList.add(segment);
			}
		}
		for (Journey journey : seamen ? itinerary.getJourneyList() : itinerary.getNonSeamenJourneyList()) {
			for (AirSegmentInformation airSegment : journey.getAirSegmentList()) {
				for (SegmentLevelGroup segmentGrp : segmentGrpList) {
					String from  = segmentGrp.getSegmentInformation().getBoardPointDetails().getTrueLocationId();
					String to = segmentGrp.getSegmentInformation().getOffpointDetails().getTrueLocationId();
					if(airSegment.getFromLocation().equalsIgnoreCase(from) && airSegment.getToLocation().equalsIgnoreCase(to)) {
						FlightInfo baggageInfo = new FlightInfo();
						BaggageDetails baggageDetails = segmentGrp.getBaggageAllowance().getBaggageDetails();
						baggageInfo.setBaggageAllowance(baggageDetails.getFreeAllowance().toBigInteger());
						baggageInfo.setBaggageUnit(baggageCodes.get(baggageDetails.getQuantityCode()));
						airSegment.setFlightInfo(baggageInfo);
					}
				}
			}
		}
	}

}
