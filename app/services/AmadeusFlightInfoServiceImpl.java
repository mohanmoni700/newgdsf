package services;

import com.amadeus.xml.farqnr_07_1_1a.FareCheckRulesReply;
import com.amadeus.xml.flires_07_1_1a.AirFlightInfoReply;
import com.amadeus.xml.flires_07_1_1a.AirFlightInfoReply.FlightScheduleDetails.InteractiveFreeText;
import com.amadeus.xml.tipnrr_12_4_1a.FareInformativePricingWithoutPNRReply;
import com.amadeus.xml.tipnrr_12_4_1a.FareInformativePricingWithoutPNRReply.MainGroup.PricingGroupLevelGroup;
import com.amadeus.xml.tipnrr_12_4_1a.FareInformativePricingWithoutPNRReply.MainGroup.PricingGroupLevelGroup.FareInfoGroup.SegmentLevelGroup;
import com.amadeus.xml.tipnrr_12_4_1a.FareInformativePricingWithoutPNRReply.MainGroup.PricingGroupLevelGroup.FareInfoGroup.SegmentLevelGroup.BaggageAllowance.BaggageDetails;
import com.amadeus.xml.tmrcrr_11_1_1a.*;
import com.compassites.GDSWrapper.amadeus.ServiceHandler;
import com.compassites.model.*;
import com.sun.xml.ws.fault.ServerSOAPFaultException;
import com.thoughtworks.xstream.XStream;
import models.AmadeusSessionWrapper;
import models.MiniRule;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import play.libs.Json;
import utils.AmadeusSessionManager;
import utils.XMLFileUtility;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

/**
 * @author Santhosh
 */

@Service
public class AmadeusFlightInfoServiceImpl implements FlightInfoService {

    static org.slf4j.Logger amadeusLogger = LoggerFactory.getLogger("amadeus");

	private static Map<String, String> baggageCodes = new HashMap<>();

	private AmadeusSessionManager amadeusSessionManager;

	@Autowired
	public void setAmadeusSessionManager(AmadeusSessionManager amadeusSessionManager){
		this.amadeusSessionManager = amadeusSessionManager;
	}

    static {
		baggageCodes.put("700", "Kilos");
		baggageCodes.put("K", "Kilos");
		baggageCodes.put("701", "Pounds");
		baggageCodes.put("L", "Pounds");
		baggageCodes.put("C", "Special Charge");
		baggageCodes.put("N", "Number of pieces");
		baggageCodes.put("S", "Size");
		baggageCodes.put("V", "Value");
		baggageCodes.put("W", "Weight");
	}

	@Override
	public FlightItinerary getBaggageInfo(FlightItinerary flightItinerary, SearchParameters searchParams, boolean seamen) {
		AmadeusSessionWrapper amadeusSessionWrapper = null;
		try {
			amadeusSessionWrapper = amadeusSessionManager.getSession();
			ServiceHandler serviceHandler = new ServiceHandler();
//			serviceHandler.logIn();
			serviceHandler.setSession(amadeusSessionWrapper.getmSession().value);
			List<Journey> journeyList = seamen ? flightItinerary.getJourneyList() : flightItinerary.getNonSeamenJourneyList();
			List<PAXFareDetails> paxFareDetailsList = flightItinerary.getPricingInformation(seamen).getPaxFareDetailsList();
			FareInformativePricingWithoutPNRReply reply = serviceHandler.getFareInfo(journeyList, seamen, searchParams.getAdultCount(), searchParams.getChildCount(), searchParams.getInfantCount(), paxFareDetailsList);
			addBaggageInfo(flightItinerary, reply.getMainGroup().getPricingGroupLevelGroup(), seamen);
			
		} catch (ServerSOAPFaultException ssf) {
			ssf.printStackTrace();
			amadeusLogger.error("Error in getBaggageInfo SOAP ", ssf);
		} catch (Exception e) {
			amadeusLogger.error("Error in getBaggageInfo", e);
			e.printStackTrace();
		}finally {
			if(amadeusSessionWrapper != null) {
				amadeusSessionWrapper.setQueryInProgress(false);
				amadeusSessionManager.updateAmadeusSession(amadeusSessionWrapper);
			}
		}
		return flightItinerary;
	}

	public List<HashMap> addMiniFareRulesForFlightItenary(MiniRuleGetFromPricingReply miniRuleGetFromPricingReply){

		HashMap<String,MiniRule> AdultMap = new HashMap<>();
		HashMap<String,MiniRule> ChildMap = new HashMap<>();
		HashMap<String,MiniRule> InfantMap = new HashMap<>();
		List<HashMap> paxTypeMap = new ArrayList<>();



		for(MiniRuleGetFromPricingReply.MnrByFareRecommendation mnrByFareRecommendation: miniRuleGetFromPricingReply.getMnrByFareRecommendation()){
			for(ReferencingDetailsType152449C paxRef:mnrByFareRecommendation.getPaxRef().getPassengerReference()){
				BigDecimal cancellationFeeBeforeDept,cancellationFeeAfterDept,cancellationFeeNoShow = new BigDecimal(0);
				BigDecimal changeFeeBeforeDept,changeFeeAfterDept,changeFeeNoShow = new BigDecimal(0);

				String paxType = paxRef.getType();
				MiniRule miniRule = new MiniRule();
				int size = mnrByFareRecommendation.getMnrRulesInfoGrp().size() -1;
				List<MiniRulesRegulPropertiesType.MnrMonInfoGrp> mnrMonInfoGrp = mnrByFareRecommendation.getMnrRulesInfoGrp().get(size-1).getMnrMonInfoGrp();
				if(mnrMonInfoGrp.size() > 1) {
					cancellationFeeAfterDept=(new BigDecimal(mnrMonInfoGrp.get(1).getMonetaryInfo().getMonetaryDetails().get(3).getAmount()));
					miniRule.setCancellationFeeAfterDeptCurrency(mnrMonInfoGrp.get(1).getMonetaryInfo().getMonetaryDetails().get(3).getCurrency());
					cancellationFeeBeforeDept=(new BigDecimal(mnrMonInfoGrp.get(0).getMonetaryInfo().getMonetaryDetails().get(3).getAmount()));
					miniRule.setCancellationFeeBeforeDeptCurrency(mnrMonInfoGrp.get(0).getMonetaryInfo().getMonetaryDetails().get(3).getCurrency());
					cancellationFeeNoShow=(new BigDecimal(mnrMonInfoGrp.get(0).getMonetaryInfo().getMonetaryDetails().get(13).getAmount()));
					miniRule.setCancellationNoShowCurrency(mnrMonInfoGrp.get(0).getMonetaryInfo().getMonetaryDetails().get(13).getCurrency());
					changeFeeNoShow=(new BigDecimal(mnrMonInfoGrp.get(1).getMonetaryInfo().getMonetaryDetails().get(13).getAmount()));
					miniRule.setChangeFeeNoShowFeeCurrency(mnrMonInfoGrp.get(1).getMonetaryInfo().getMonetaryDetails().get(13).getCurrency());
					changeFeeAfterDept=(new BigDecimal(mnrMonInfoGrp.get(1).getMonetaryInfo().getMonetaryDetails().get(3).getAmount()));
					miniRule.setChangeFeeFeeAfterDeptCurrency(mnrMonInfoGrp.get(1).getMonetaryInfo().getMonetaryDetails().get(3).getCurrency());
					changeFeeBeforeDept=(new BigDecimal(mnrMonInfoGrp.get(0).getMonetaryInfo().getMonetaryDetails().get(3).getAmount()));
					miniRule.setChangeFeeBeforeDeptCurrency(mnrMonInfoGrp.get(0).getMonetaryInfo().getMonetaryDetails().get(3).getCurrency());
				}
				else {
                    cancellationFeeAfterDept = (new BigDecimal(mnrMonInfoGrp.get(0).getMonetaryInfo().getMonetaryDetails().get(13).getAmount()));
                    miniRule.setCancellationFeeAfterDeptCurrency(mnrMonInfoGrp.get(0).getMonetaryInfo().getMonetaryDetails().get(13).getCurrency());
                    cancellationFeeBeforeDept = (new BigDecimal(mnrMonInfoGrp.get(0).getMonetaryInfo().getMonetaryDetails().get(3).getAmount()));
                    miniRule.setCancellationFeeBeforeDeptCurrency(mnrMonInfoGrp.get(0).getMonetaryInfo().getMonetaryDetails().get(3).getCurrency());
                    cancellationFeeNoShow = (new BigDecimal(mnrMonInfoGrp.get(0).getMonetaryInfo().getMonetaryDetails().get(8).getAmount()));
                    miniRule.setCancellationNoShowCurrency(mnrMonInfoGrp.get(0).getMonetaryInfo().getMonetaryDetails().get(8).getCurrency());
                    changeFeeNoShow = (new BigDecimal(mnrMonInfoGrp.get(0).getMonetaryInfo().getMonetaryDetails().get(18).getAmount()));
                    miniRule.setChangeFeeNoShowFeeCurrency(mnrMonInfoGrp.get(0).getMonetaryInfo().getMonetaryDetails().get(18).getCurrency());
                    changeFeeAfterDept = (new BigDecimal(mnrMonInfoGrp.get(0).getMonetaryInfo().getMonetaryDetails().get(13).getAmount()));
                    miniRule.setChangeFeeFeeAfterDeptCurrency(mnrMonInfoGrp.get(0).getMonetaryInfo().getMonetaryDetails().get(13).getCurrency());
                    changeFeeBeforeDept = (new BigDecimal(mnrMonInfoGrp.get(0).getMonetaryInfo().getMonetaryDetails().get(3).getAmount()));
                    miniRule.setChangeFeeFeeAfterDeptCurrency(mnrMonInfoGrp.get(0).getMonetaryInfo().getMonetaryDetails().get(3).getCurrency());
                }
				BigDecimal markUp = new BigDecimal(0.10);
				cancellationFeeBeforeDept= cancellationFeeBeforeDept.add(cancellationFeeBeforeDept.multiply(markUp)).setScale(2, BigDecimal.ROUND_HALF_UP);
				cancellationFeeAfterDept= cancellationFeeAfterDept.add(cancellationFeeAfterDept.multiply(markUp)).setScale(2, BigDecimal.ROUND_HALF_UP);
				cancellationFeeNoShow= cancellationFeeNoShow.add(cancellationFeeNoShow.multiply(markUp)).setScale(2, BigDecimal.ROUND_HALF_UP);
				changeFeeNoShow = changeFeeNoShow.add(changeFeeNoShow.multiply(markUp)).setScale(2, BigDecimal.ROUND_HALF_UP);
				changeFeeAfterDept = changeFeeAfterDept.add(changeFeeAfterDept.multiply(markUp)).setScale(2, BigDecimal.ROUND_HALF_UP);
				changeFeeBeforeDept = changeFeeBeforeDept.add(changeFeeBeforeDept.multiply(markUp)).setScale(2, BigDecimal.ROUND_HALF_UP);

				miniRule.setCancellationFeeAfterDept(cancellationFeeAfterDept);
				miniRule.setCancellationFeeBeforeDept(cancellationFeeBeforeDept);
				miniRule.setCancellationFeeNoShow(cancellationFeeNoShow);
				miniRule.setChangeFeeNoShow(changeFeeNoShow);
				miniRule.setChangeFeeAfterDept(changeFeeAfterDept);
				miniRule.setChangeFeeBeforeDept(changeFeeBeforeDept);

				if (paxType.equalsIgnoreCase("PA") && AdultMap.size() == 0) {
					AdultMap.put("ADT", miniRule);
				} else if (paxType.equalsIgnoreCase("PA") && AdultMap.size() > 0) {
					ChildMap.put("CHD", miniRule);
				} else if (paxType.equalsIgnoreCase("PI")) {
					InfantMap.put("INF", miniRule);

				}
			}
		}

		paxTypeMap.add(AdultMap);
		paxTypeMap.add(ChildMap);
		paxTypeMap.add(InfantMap);

		return paxTypeMap;
	}
	
	public FlightItinerary getInFlightDetails(FlightItinerary flightItinerary, boolean seamen) {
		AmadeusSessionWrapper amadeusSessionWrapper = null;
		try {
			amadeusSessionWrapper = amadeusSessionManager.getSession();
			ServiceHandler serviceHandler = new ServiceHandler();
//			serviceHandler.logIn();
			serviceHandler.setSession(amadeusSessionWrapper.getmSession().value);
			List<Journey> journeyList = seamen ? flightItinerary.getJourneyList() : flightItinerary.getNonSeamenJourneyList();
			for(Journey journey : journeyList) {
				for(AirSegmentInformation segment : journey.getAirSegmentList()) {
					AirFlightInfoReply flightInfoReply = serviceHandler.getFlightInfo(segment);
					List<String> amenities = new ArrayList<>();
					for(InteractiveFreeText freeText : flightInfoReply.getFlightScheduleDetails().getInteractiveFreeText()) {
						amenities.add(freeText.getFreeText());
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
		} catch (ServerSOAPFaultException ssf) {
			ssf.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}finally {
			if(amadeusSessionWrapper != null) {
				amadeusSessionWrapper.setQueryInProgress(false);
				amadeusSessionManager.updateAmadeusSession(amadeusSessionWrapper);
			}
		}
		return flightItinerary;
	}
	
	public String getCancellationFee(FlightItinerary flightItinerary, SearchParameters searchParams, boolean seamen) {
		String fareRules = "";
		AmadeusSessionWrapper amadeusSessionWrapper = null;
        try {
			amadeusSessionWrapper = amadeusSessionManager.getSession();
        	ServiceHandler serviceHandler = new ServiceHandler();
			serviceHandler.setSession(amadeusSessionWrapper.getmSession().value);
//            serviceHandler.logIn();
			List<Journey> journeyList = seamen ? flightItinerary.getJourneyList() : flightItinerary.getNonSeamenJourneyList();
			List<PAXFareDetails> paxFareDetailsList = flightItinerary.getPricingInformation(seamen).getPaxFareDetailsList();
			FareInformativePricingWithoutPNRReply pricingReply = serviceHandler.getFareInfo(journeyList, seamen, searchParams.getAdultCount(), searchParams.getChildCount(),
							searchParams.getInfantCount(), paxFareDetailsList);
            amadeusLogger.debug("FareInformativePricingWithoutPNRReply "+ new Date()+" ------->>"+ new XStream().toXML(pricingReply));
			FareCheckRulesReply fareCheckRulesReply = serviceHandler.getFareRules();
			//System.out.println("fareCheckRulesReply ***"+fareCheckRulesReply);
            StringBuilder fareRule = new StringBuilder();
			if(fareCheckRulesReply.getErrorInfo() != null){
				if(fareCheckRulesReply.getErrorInfo().getErrorFreeText()!=null){
					//System.out.println("No fare rules:\n"+fareCheckRulesReply.getErrorInfo().getErrorFreeText().getFreeText());
				}
				return "No Fare Rules";
			}
			if(fareCheckRulesReply.getTariffInfo() == null || fareCheckRulesReply.getTariffInfo().size() == 0){
				for(FareCheckRulesReply.FlightDetails flightDetails :fareCheckRulesReply.getFlightDetails()){
					for(FareCheckRulesReply.FlightDetails.TravellerGrp travellerGrp : flightDetails.getTravellerGrp()){
						for(FareCheckRulesReply.FlightDetails.TravellerGrp.TravellerIdentRef.ReferenceDetails referenceDetails : travellerGrp.getTravellerIdentRef().getReferenceDetails()){
							if("FC".equalsIgnoreCase(referenceDetails.getType())){
								FareCheckRulesReply fcFareRulesReply = serviceHandler.getFareRulesForFCType(referenceDetails.getValue());
								fareRule.append(getFareRuleFromTariffInfo(fcFareRulesReply));
							}
						}
					}
				}
			}else{
				fareRule.append(getFareRuleFromTariffInfo(fareCheckRulesReply));
			}

            fareRules = fareRule.toString();
            //System.out.println("fareRule.toString()!!!"+fareRule.toString());
        } catch (Exception e) {
        	//System.out.println("getCancellationFee fare rule exception..........");        	
            e.printStackTrace();
        }finally {
			if(amadeusSessionWrapper != null) {
				amadeusSessionWrapper.setQueryInProgress(false);
				amadeusSessionManager.updateAmadeusSession(amadeusSessionWrapper);
			}
		}
		return fareRules;
    }

    public List<HashMap> getMiniRulesFromFlightItenary(FlightItinerary flightItinerary, SearchParameters searchParams, boolean seamen){
	    AmadeusSessionWrapper amadeusSessionWrapper = null;
		List<HashMap> miniRule = new ArrayList<>();
		try {
			amadeusSessionWrapper = amadeusSessionManager.getSession();
			ServiceHandler serviceHandler = new ServiceHandler();
			serviceHandler.setSession(amadeusSessionWrapper.getmSession().value);
//            serviceHandler.logIn();
			List<Journey> journeyList = seamen ? flightItinerary.getJourneyList() : flightItinerary.getNonSeamenJourneyList();
			List<PAXFareDetails> paxFareDetailsList = flightItinerary.getPricingInformation(seamen).getPaxFareDetailsList();
			FareInformativePricingWithoutPNRReply pricingReply = serviceHandler.getFareInfo(journeyList, seamen, searchParams.getAdultCount(), searchParams.getChildCount(),
					searchParams.getInfantCount(), paxFareDetailsList);
			MiniRuleGetFromPricingReply miniRuleGetFromPricingReply = serviceHandler.retriveMiniRuleFromPricing();
			if(miniRuleGetFromPricingReply.getErrorWarningGroup() != null){
				if(miniRuleGetFromPricingReply.getErrorWarningGroup().getErrorWarningDescription()!=null){
					amadeusLogger.debug("MiniRuleGetFromPricingReply Error"+miniRuleGetFromPricingReply.getErrorWarningGroup().getErrorWarningDescription());
				}
				return null;
			}

			miniRule = addMiniFareRulesForFlightItenary(miniRuleGetFromPricingReply);
		} catch (Exception e) {
			//System.out.println("getCancellationFee fare rule exception..........");
			e.printStackTrace();
		}finally {
			if(amadeusSessionWrapper != null) {
				amadeusSessionWrapper.setQueryInProgress(false);
				amadeusSessionManager.updateAmadeusSession(amadeusSessionWrapper);
			}
		}
		amadeusLogger.info("miniRule: " + Json.toJson(miniRule));
		return miniRule;
	}

	public String getFareRuleFromTariffInfo(FareCheckRulesReply fareCheckRulesReply){
		StringBuilder fareRuleText = new StringBuilder();
		for(FareCheckRulesReply.TariffInfo tariffInfo : fareCheckRulesReply.getTariffInfo()){
			if("(16)".equals(tariffInfo.getFareRuleInfo().getRuleCategoryCode())){
				for(FareCheckRulesReply.TariffInfo.FareRuleText text : tariffInfo.getFareRuleText() ) {
					fareRuleText.append(text.getFreeText().get(0));
				}
			}
		}

		return fareRuleText.toString();
	}

	private void addBaggageInfo(FlightItinerary itinerary, List<PricingGroupLevelGroup> pricingLevelGroup, boolean seamen) {
		try {
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
		
		} catch (Exception e) {
			amadeusLogger.error("Error in addBaggageInfo" , e);
			e.printStackTrace();
		}
		
	}
}
