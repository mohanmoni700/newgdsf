package services;

import com.amadeus.xml.farqnr_07_1_1a.FareCheckRulesReply;
import com.amadeus.xml.flires_07_1_1a.AirFlightInfoReply;
import com.amadeus.xml.flires_07_1_1a.AirFlightInfoReply.FlightScheduleDetails.InteractiveFreeText;
import com.amadeus.xml.tipnrr_12_4_1a.FareInformativePricingWithoutPNRReply;
import com.amadeus.xml.tipnrr_12_4_1a.FareInformativePricingWithoutPNRReply.MainGroup.PricingGroupLevelGroup;
import com.amadeus.xml.tipnrr_12_4_1a.FareInformativePricingWithoutPNRReply.MainGroup.PricingGroupLevelGroup.FareInfoGroup.SegmentLevelGroup;
import com.amadeus.xml.tipnrr_12_4_1a.FareInformativePricingWithoutPNRReply.MainGroup.PricingGroupLevelGroup.FareInfoGroup.SegmentLevelGroup.BaggageAllowance.BaggageDetails;
import com.amadeus.xml.tmrxrr_18_1_1a.*;
import com.amadeus.xml.tmrxrr_18_1_1a.MiniRuleGetFromRecReply.MnrByPricingRecord;
import com.compassites.GDSWrapper.amadeus.ServiceHandler;
import com.compassites.model.*;
import com.sun.xml.ws.fault.ServerSOAPFaultException;
import com.thoughtworks.xstream.XStream;
import models.AmadeusSessionWrapper;
import models.FlightSearchOffice;
import models.MiniRule;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import play.libs.Json;
import utils.AmadeusHelper;
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
	private AmadeusSourceOfficeService amadeusSourceOfficeService;

	@Autowired
	private ServiceHandler serviceHandler;

	@Autowired
	public void setAmadeusSessionManager(AmadeusSessionManager amadeusSessionManager){
		this.amadeusSessionManager = amadeusSessionManager;
	}

	static {
		baggageCodes.put("700", "KG");
		baggageCodes.put("K", "KG");
		baggageCodes.put("701", "Lb");
		baggageCodes.put("L", "Lb");
		baggageCodes.put("C", "Special Charge");
		baggageCodes.put("N", "PC");
		baggageCodes.put("S", "Size");
		baggageCodes.put("V", "Value");
		baggageCodes.put("W", "Weight");
	}

	@Override
	public FlightItinerary getBaggageInfo(FlightItinerary flightItinerary, SearchParameters searchParams, boolean seamen) {
		AmadeusSessionWrapper amadeusSessionWrapper = null;
		try {
			String officeId = flightItinerary.getPricingInformation().getPricingOfficeId();
			FlightSearchOffice flightSearchOffice = new FlightSearchOffice(officeId);
			amadeusSessionWrapper = amadeusSessionManager.getSession(flightSearchOffice);
			List<Journey> journeyList = seamen ? flightItinerary.getJourneyList() : flightItinerary.getNonSeamenJourneyList();
			List<PAXFareDetails> paxFareDetailsList = flightItinerary.getPricingInformation(seamen).getPaxFareDetailsList();
			FareInformativePricingWithoutPNRReply reply = serviceHandler.getFareInfo(journeyList, seamen, searchParams.getAdultCount(), searchParams.getChildCount(), searchParams.getInfantCount(), paxFareDetailsList, amadeusSessionWrapper);
			FareCheckRulesReply fareCheckRulesReply = serviceHandler.getFareRules(amadeusSessionWrapper);
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

	public List<HashMap> addMiniFareRulesForFlightItenary(MiniRuleGetFromRecReply miniRuleGetFromPricingReply){

		HashMap<String,MiniRule> AdultMap = new HashMap<>();
		HashMap<String,MiniRule> ChildMap = new HashMap<>();
		HashMap<String,MiniRule> InfantMap = new HashMap<>();
		List<HashMap> paxTypeMap = new ArrayList<>();

		for(MnrByPricingRecord mnrByFareRecommendation: miniRuleGetFromPricingReply.getMnrByPricingRecord()){
			for(ReferencingDetailsType paxRef:mnrByFareRecommendation.getPaxRef().getPassengerReference()){
				BigDecimal cancellationFeeBeforeDept,cancellationFeeAfterDept,cancellationFeeNoShowAfterDept,cancellationFeeNoShowBeforeDept = new BigDecimal(0);
				BigDecimal changeFeeBeforeDept,changeFeeAfterDept,changeFeeNoShowAfterDept,changeFeeNoShowBeforeDept = new BigDecimal(0);
				String cancellationNoShowAfterDeptCurrency,cancellationFeeNoShowBeforeDeptCurrency,changeFeeNoShowAfterDeptCurrency,changeFeeNoShowBeforeDeptCurrency;

				String paxType = paxRef.getType();
				MiniRule miniRule = new MiniRule();
				//int size = mnrByFareRecommendation.getMnrRulesInfoGrp().size() -1;
				List<Integer> size = getMnrInfo(mnrByFareRecommendation);
				List<MiniRulesRegulPropertiesType.MnrMonInfoGrp> mnrMonInfoGrp = mnrByFareRecommendation.getMnrRulesInfoGrp().get(size.get(0)).getMnrMonInfoGrp();
				List<MiniRulesRegulPropertiesType.MnrMonInfoGrp> changeMnrMonInfoGrp = mnrByFareRecommendation.getMnrRulesInfoGrp().get(size.get(1)).getMnrMonInfoGrp();
				List<MiniRulesRegulPropertiesType.MnrRestriAppInfoGrp> restriAppInfoGrp = mnrByFareRecommendation.getMnrRulesInfoGrp().get(size.get(0)).getMnrRestriAppInfoGrp();
				List<MiniRulesRegulPropertiesType.MnrRestriAppInfoGrp> changeRestriAppInfoGrp = mnrByFareRecommendation.getMnrRulesInfoGrp().get(size.get(1)).getMnrRestriAppInfoGrp();
				if(mnrMonInfoGrp.size()>1) {
                    cancellationFeeAfterDept=((mnrMonInfoGrp.get(1).getMonetaryInfo().getMonetaryDetails().get(4).getAmount()));
                    miniRule.setCancellationFeeAfterDeptCurrency(mnrMonInfoGrp.get(1).getMonetaryInfo().getMonetaryDetails().get(4).getCurrency());
                    cancellationFeeBeforeDept=((mnrMonInfoGrp.get(0).getMonetaryInfo().getMonetaryDetails().get(4).getAmount()));
                    miniRule.setCancellationFeeBeforeDeptCurrency(mnrMonInfoGrp.get(0).getMonetaryInfo().getMonetaryDetails().get(4).getCurrency());
					cancellationFeeNoShowAfterDept=((mnrMonInfoGrp.get(1).getMonetaryInfo().getMonetaryDetails().get(9).getAmount()));
					cancellationNoShowAfterDeptCurrency =(mnrMonInfoGrp.get(1).getMonetaryInfo().getMonetaryDetails().get(9).getCurrency());
					cancellationFeeNoShowBeforeDept=((mnrMonInfoGrp.get(0).getMonetaryInfo().getMonetaryDetails().get(9).getAmount()));
					cancellationFeeNoShowBeforeDeptCurrency =(mnrMonInfoGrp.get(0).getMonetaryInfo().getMonetaryDetails().get(9).getCurrency());
				}else {
                    cancellationFeeAfterDept=((mnrMonInfoGrp.get(0).getMonetaryInfo().getMonetaryDetails().get(14).getAmount()));
                    miniRule.setCancellationFeeAfterDeptCurrency(mnrMonInfoGrp.get(0).getMonetaryInfo().getMonetaryDetails().get(14).getCurrency());
                    cancellationFeeBeforeDept=((mnrMonInfoGrp.get(0).getMonetaryInfo().getMonetaryDetails().get(4).getAmount()));
                    miniRule.setCancellationFeeBeforeDeptCurrency(mnrMonInfoGrp.get(0).getMonetaryInfo().getMonetaryDetails().get(4).getCurrency());
					cancellationFeeNoShowAfterDept=((mnrMonInfoGrp.get(0).getMonetaryInfo().getMonetaryDetails().get(19).getAmount()));
					cancellationNoShowAfterDeptCurrency =(mnrMonInfoGrp.get(0).getMonetaryInfo().getMonetaryDetails().get(19).getCurrency());
					cancellationFeeNoShowBeforeDept=((mnrMonInfoGrp.get(0).getMonetaryInfo().getMonetaryDetails().get(9).getAmount()));
					cancellationFeeNoShowBeforeDeptCurrency =(mnrMonInfoGrp.get(0).getMonetaryInfo().getMonetaryDetails().get(9).getCurrency());

                }
                if(changeMnrMonInfoGrp.size() > 1){
					changeFeeNoShowAfterDept=((changeMnrMonInfoGrp.get(1).getMonetaryInfo().getMonetaryDetails().get(14).getAmount()));
					changeFeeNoShowAfterDeptCurrency = (changeMnrMonInfoGrp.get(1).getMonetaryInfo().getMonetaryDetails().get(14).getCurrency());
					changeFeeAfterDept=((changeMnrMonInfoGrp.get(1).getMonetaryInfo().getMonetaryDetails().get(4).getAmount()));
					miniRule.setChangeFeeFeeAfterDeptCurrency(changeMnrMonInfoGrp.get(1).getMonetaryInfo().getMonetaryDetails().get(4).getCurrency());
					changeFeeBeforeDept=((changeMnrMonInfoGrp.get(0).getMonetaryInfo().getMonetaryDetails().get(4).getAmount()));
					miniRule.setChangeFeeBeforeDeptCurrency(changeMnrMonInfoGrp.get(0).getMonetaryInfo().getMonetaryDetails().get(4).getCurrency());
					changeFeeNoShowBeforeDept=((changeMnrMonInfoGrp.get(0).getMonetaryInfo().getMonetaryDetails().get(14).getAmount()));
					changeFeeNoShowBeforeDeptCurrency = (changeMnrMonInfoGrp.get(0).getMonetaryInfo().getMonetaryDetails().get(14).getCurrency());
				}else{
					changeFeeNoShowAfterDept=((changeMnrMonInfoGrp.get(0).getMonetaryInfo().getMonetaryDetails().get(19).getAmount()));
					changeFeeNoShowAfterDeptCurrency = (changeMnrMonInfoGrp.get(0).getMonetaryInfo().getMonetaryDetails().get(19).getCurrency());
					changeFeeAfterDept=((changeMnrMonInfoGrp.get(0).getMonetaryInfo().getMonetaryDetails().get(14).getAmount()));
					miniRule.setChangeFeeFeeAfterDeptCurrency(changeMnrMonInfoGrp.get(0).getMonetaryInfo().getMonetaryDetails().get(14).getCurrency());
					changeFeeBeforeDept=((changeMnrMonInfoGrp.get(0).getMonetaryInfo().getMonetaryDetails().get(4).getAmount()));
					miniRule.setChangeFeeBeforeDeptCurrency(changeMnrMonInfoGrp.get(0).getMonetaryInfo().getMonetaryDetails().get(4).getCurrency());
					changeFeeNoShowBeforeDept=((changeMnrMonInfoGrp.get(0).getMonetaryInfo().getMonetaryDetails().get(9).getAmount()));
					changeFeeNoShowBeforeDeptCurrency = (changeMnrMonInfoGrp.get(0).getMonetaryInfo().getMonetaryDetails().get(9).getCurrency());
				}

                BigDecimal markUp =new BigDecimal(play.Play.application().configuration().getDouble("markup"));
                cancellationFeeBeforeDept= cancellationFeeBeforeDept.add(cancellationFeeBeforeDept.multiply(markUp)).setScale(2, BigDecimal.ROUND_HALF_UP);
                cancellationFeeAfterDept= cancellationFeeAfterDept.add(cancellationFeeAfterDept.multiply(markUp)).setScale(2, BigDecimal.ROUND_HALF_UP);
				cancellationFeeNoShowAfterDept= cancellationFeeNoShowAfterDept.add(cancellationFeeNoShowAfterDept.multiply(markUp)).setScale(2, BigDecimal.ROUND_HALF_UP);
				cancellationFeeNoShowBeforeDept= cancellationFeeNoShowBeforeDept.add(cancellationFeeNoShowBeforeDept.multiply(markUp)).setScale(2, BigDecimal.ROUND_HALF_UP);
				changeFeeNoShowAfterDept = changeFeeNoShowAfterDept.add(changeFeeNoShowAfterDept.multiply(markUp)).setScale(2, BigDecimal.ROUND_HALF_UP);
				changeFeeNoShowBeforeDept = changeFeeNoShowBeforeDept.add(changeFeeNoShowBeforeDept.multiply(markUp)).setScale(2, BigDecimal.ROUND_HALF_UP);
				changeFeeAfterDept = changeFeeAfterDept.add(changeFeeAfterDept.multiply(markUp)).setScale(2, BigDecimal.ROUND_HALF_UP);
                changeFeeBeforeDept = changeFeeBeforeDept.add(changeFeeBeforeDept.multiply(markUp)).setScale(2, BigDecimal.ROUND_HALF_UP);

                miniRule.setCancellationFeeAfterDept(cancellationFeeAfterDept);
                miniRule.setCancellationFeeBeforeDept(cancellationFeeBeforeDept);
                miniRule.setChangeFeeAfterDept(changeFeeAfterDept);
                miniRule.setChangeFeeBeforeDept(changeFeeBeforeDept);

				List<StatusDetailsType299275C> cancelStatuslist =  restriAppInfoGrp.get(0).getMnrRestriAppInfo().getStatusInformation();
				HashMap<String,String> cancelKeys = mapFlags(cancelStatuslist);

				miniRule.setCancellationRefundableBeforeDept(Boolean.valueOf(cancelKeys.get("BDA").equalsIgnoreCase("0") ? false : true));
				miniRule.setCancellationRefundableAfterDept(Boolean.valueOf(cancelKeys.get("ADA").equalsIgnoreCase("0") ? false : true));
				miniRule.setCancellationNoShowBeforeDept(Boolean.valueOf(cancelKeys.get("BNA").equalsIgnoreCase("0") ? false : true));
				miniRule.setCancellationNoShowAfterDept(Boolean.valueOf(cancelKeys.get("ANA").equalsIgnoreCase("0") ? false : true));

				List<StatusDetailsType299275C> changeStatuslist =  changeRestriAppInfoGrp.get(0).getMnrRestriAppInfo().getStatusInformation();
				HashMap<String,String> changeKeys = mapFlags(changeStatuslist);

				miniRule.setChangeRefundableBeforeDept(Boolean.valueOf(changeKeys.get("BDA").equalsIgnoreCase("0") ? false : true));
				miniRule.setChangeRefundableAfterDept(Boolean.valueOf(changeKeys.get("ADA").equalsIgnoreCase("0") ? false : true));
				miniRule.setChangeNoShowBeforeDept(Boolean.valueOf(changeKeys.get("BNA").equalsIgnoreCase("0") ? false : true));
				miniRule.setChangeNoShowAfterDept(Boolean.valueOf(changeKeys.get("ANA").equalsIgnoreCase("0") ? false : true));

				int res = cancellationFeeNoShowAfterDept.compareTo(cancellationFeeNoShowBeforeDept);
				if(res == 1){
					miniRule.setCancellationFeeNoShow(cancellationFeeNoShowAfterDept);
					miniRule.setCancellationNoShowCurrency(cancellationNoShowAfterDeptCurrency);
					miniRule.setCancellationNoShowAfterDept(miniRule.getCancellationNoShowAfterDept());
				} else if(res == -1){
					miniRule.setCancellationFeeNoShow(cancellationFeeNoShowBeforeDept);
					miniRule.setCancellationNoShowCurrency(cancellationFeeNoShowBeforeDeptCurrency);
					miniRule.setCancellationNoShowAfterDept(miniRule.getCancellationNoShowAfterDept());
				}else{
					miniRule.setCancellationFeeNoShow(cancellationFeeNoShowAfterDept);
					miniRule.setCancellationNoShowCurrency(cancellationNoShowAfterDeptCurrency);
					miniRule.setCancellationNoShowAfterDept(miniRule.getCancellationNoShowAfterDept());
				}

				int res1 = changeFeeNoShowAfterDept.compareTo(changeFeeNoShowBeforeDept);
				if(res1 == 1){
					miniRule.setChangeFeeNoShow(changeFeeNoShowAfterDept);
					miniRule.setChangeFeeNoShowFeeCurrency(changeFeeNoShowAfterDeptCurrency);
					miniRule.setChangeNoShowAfterDept(miniRule.getChangeNoShowAfterDept());
				} else if(res1 == -1){
					miniRule.setChangeFeeNoShow(changeFeeNoShowBeforeDept);
					miniRule.setChangeFeeNoShowFeeCurrency(changeFeeNoShowBeforeDeptCurrency);
					miniRule.setChangeNoShowAfterDept(miniRule.getChangeNoShowBeforeDept());
				}else{
					miniRule.setChangeFeeNoShow(changeFeeNoShowAfterDept);
					miniRule.setChangeFeeNoShowFeeCurrency(changeFeeNoShowAfterDeptCurrency);
					miniRule.setChangeNoShowAfterDept(miniRule.getChangeNoShowAfterDept());
				}

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

	public HashMap mapFlags(List<StatusDetailsType299275C> statusList){
		HashMap<String,String> keyMap = new HashMap<>();
		for(int i=0;i<statusList.size();i++)
		{
			keyMap.put(statusList.get(i).getIndicator(),statusList.get(i).getAction());
		}
		return keyMap;
	}

	public List<Integer> getMnrInfo(MnrByPricingRecord mnrByFareRecommendation){
		List<Integer> returnList= new ArrayList<Integer>();
		int size = mnrByFareRecommendation.getMnrRulesInfoGrp().size();
		List< List<MiniRulesRegulPropertiesType.MnrMonInfoGrp> > cancelMonInfo = new ArrayList<>();
		List< List<MiniRulesRegulPropertiesType.MnrMonInfoGrp> > changeMonInfo = new ArrayList<>();
		HashMap<List<MiniRulesRegulPropertiesType.MnrMonInfoGrp>,Integer> hash = new HashMap<>();
		for(int i = 0 ; i<size ;i++){
			if(mnrByFareRecommendation.getMnrRulesInfoGrp().get(i).getMnrCatInfo().getDescriptionInfo().getNumber().equals(new BigInteger("33"))){
				cancelMonInfo.add(mnrByFareRecommendation.getMnrRulesInfoGrp().get(i).getMnrMonInfoGrp());
				hash.put(mnrByFareRecommendation.getMnrRulesInfoGrp().get(i).getMnrMonInfoGrp(),i);

			}
			if(mnrByFareRecommendation.getMnrRulesInfoGrp().get(i).getMnrCatInfo().getDescriptionInfo().getNumber().equals(new BigInteger("31"))){
				changeMonInfo.add(mnrByFareRecommendation.getMnrRulesInfoGrp().get(i).getMnrMonInfoGrp());
				hash.put(mnrByFareRecommendation.getMnrRulesInfoGrp().get(i).getMnrMonInfoGrp(),i);
			}
		}
		List<MiniRulesRegulPropertiesType.MnrMonInfoGrp> res = null;
		if(cancelMonInfo.size()>1){
			res = comparePrice(cancelMonInfo);
		}
		else {
			res = cancelMonInfo.get(0);
		}
		returnList.add(hash.get(res));
		if(changeMonInfo.size()>1){
			res = comparePrice(changeMonInfo);
		}
		else {
			res = changeMonInfo.get(0);
		}
		returnList.add(hash.get(res));
		return returnList;
	}


	public List<MiniRulesRegulPropertiesType.MnrMonInfoGrp> comparePrice(List<List<MiniRulesRegulPropertiesType.MnrMonInfoGrp>> list) {
		int res = 0;
		List<MiniRulesRegulPropertiesType.MnrMonInfoGrp> high = new ArrayList<>();
		if (list.size() < 3) {
			BigDecimal b1 = list.get(0).get(0).getMonetaryInfo().getMonetaryDetails().get(4).getAmount();
			BigDecimal b2 = list.get(1).get(0).getMonetaryInfo().getMonetaryDetails().get(4).getAmount();
			res = b1.compareTo(b2);
			if (res == 1) {
				return list.get(0);
			} else
				return list.get(1);
		} else {
			BigDecimal max = (list.get(0).get(0).getMonetaryInfo().getMonetaryDetails().get(4).getAmount());
			int index= 0;
			for (int i = 1; i < list.size(); i++) {
				BigDecimal b2 = (list.get(i).get(0).getMonetaryInfo().getMonetaryDetails().get(4).getAmount());
				res = max.compareTo(b2);
				if (res == 1) {
					high = list.get(index);
					max = (list.get(index).get(0).getMonetaryInfo().getMonetaryDetails().get(4).getAmount());
				} else if (res == -1) {
					index = i;
					high = list.get(index);
					max = (list.get(index).get(0).getMonetaryInfo().getMonetaryDetails().get(4).getAmount());
				} else
					high = list.get(i);
			}

		}
		return high;
	}


	
	public FlightItinerary getInFlightDetails(FlightItinerary flightItinerary, boolean seamen) {
		AmadeusSessionWrapper amadeusSessionWrapper = null;
		try {
			amadeusSessionWrapper = amadeusSessionManager.getSession();
			//ServiceHandler serviceHandler = new ServiceHandler();
//			serviceHandler.logIn();
			//serviceHandler.setSession(amadeusSessionWrapper.getmSession().value);
			List<Journey> journeyList = seamen ? flightItinerary.getJourneyList() : flightItinerary.getNonSeamenJourneyList();
			for(Journey journey : journeyList) {
				for(AirSegmentInformation segment : journey.getAirSegmentList()) {
					AirFlightInfoReply flightInfoReply = serviceHandler.getFlightInfo(segment, amadeusSessionWrapper);
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
        	//ServiceHandler serviceHandler = new ServiceHandler();
			//serviceHandler.setSession(amadeusSessionWrapper.getmSession().value);
//            serviceHandler.logIn();
			List<Journey> journeyList = seamen ? flightItinerary.getJourneyList() : flightItinerary.getNonSeamenJourneyList();
			List<PAXFareDetails> paxFareDetailsList = flightItinerary.getPricingInformation(seamen).getPaxFareDetailsList();
			FareInformativePricingWithoutPNRReply pricingReply = serviceHandler.getFareInfo(journeyList, seamen, searchParams.getAdultCount(), searchParams.getChildCount(),
							searchParams.getInfantCount(), paxFareDetailsList, amadeusSessionWrapper);
            amadeusLogger.debug("FareInformativePricingWithoutPNRReply "+ new Date()+" ------->>"+ new XStream().toXML(pricingReply));
			FareCheckRulesReply fareCheckRulesReply = serviceHandler.getFareRules(amadeusSessionWrapper);
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
								FareCheckRulesReply fcFareRulesReply = serviceHandler.getFareRulesForFCType(referenceDetails.getValue(), amadeusSessionWrapper);
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
			FlightSearchOffice flightSearchOffice = new FlightSearchOffice(flightItinerary.getPricingInformation(seamen).getPricingOfficeId().toString());
			//amadeusSessionWrapper = amadeusSessionManager.getSession();
			amadeusSessionWrapper = amadeusSessionManager.getSession(flightSearchOffice);
			amadeusSessionManager.removeActiveSession(amadeusSessionWrapper.getmSession().value);
			amadeusSessionWrapper = amadeusSessionManager.getSession(flightSearchOffice);
			//ServiceHandler serviceHandler = new ServiceHandler();

			//serviceHandler.setSession(amadeusSessionWrapper.getmSession().value);
//            serviceHandler.logIn();
			List<Journey> journeyList = seamen ? flightItinerary.getJourneyList() : flightItinerary.getNonSeamenJourneyList();
			List<PAXFareDetails> paxFareDetailsList = flightItinerary.getPricingInformation(seamen).getPaxFareDetailsList();
			FareInformativePricingWithoutPNRReply pricingReply = serviceHandler.getFareInfo(journeyList, seamen, searchParams.getAdultCount(), searchParams.getChildCount(),
					searchParams.getInfantCount(), paxFareDetailsList, amadeusSessionWrapper);
			MiniRuleGetFromRecReply miniRuleGetFromPricingReply = serviceHandler.retriveMiniRuleFromPricing(amadeusSessionWrapper);
			if(miniRuleGetFromPricingReply.getErrorWarningGroup() != null &&
					miniRuleGetFromPricingReply.getResponseDetails() != null &&
					"0".equalsIgnoreCase(miniRuleGetFromPricingReply.getResponseDetails().getStatusCode())){
					amadeusLogger.debug("MiniRuleGetFromPricingReply Error"+miniRuleGetFromPricingReply.getErrorWarningGroup().get(0).getErrorWarningDescription());
				return null;
			}

			miniRule = addMiniFareRulesForFlightItenary(miniRuleGetFromPricingReply);
		} catch (Exception e) {
			//System.out.println("getCancellationFee fare rule exception..........");
			e.printStackTrace();
		}finally {
//			if(amadeusSessionWrapper != null) {
//				amadeusSessionWrapper.setQueryInProgress(false);
//				amadeusSessionManager.updateAmadeusSession(amadeusSessionWrapper);
//				//serviceHandler.logOut(amadeusSessionWrapper);
//			}

			if(amadeusSessionWrapper != null){
				amadeusSessionManager.removeActiveSession(amadeusSessionWrapper.getmSession().value);
				serviceHandler.logOut(amadeusSessionWrapper);
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

	public List<HashMap> getGenericFareRuleFlightItenary(FlightItinerary flightItinerary,
														 SearchParameters searchParams,boolean seamen){
		List<HashMap> miniRule = new ArrayList<>();
		try {
			AmadeusSessionWrapper amadeusSessionWrapper = serviceHandler.logIn(amadeusSourceOfficeService.getBenzySourceOffice().getOfficeId());
			List<Journey> journeyList = seamen ? flightItinerary.getJourneyList() : flightItinerary.getNonSeamenJourneyList();
			List<PAXFareDetails> paxFareDetailsList = flightItinerary.getPricingInformation(seamen).getPaxFareDetailsList();
			FareInformativePricingWithoutPNRReply reply = serviceHandler.getFareInfo(journeyList, seamen, searchParams.getAdultCount(), searchParams.getChildCount(), searchParams.getInfantCount(), paxFareDetailsList, amadeusSessionWrapper);
			if(reply.getErrorGroup() != null){
				amadeusLogger.debug("Not able to fetch FareInformativePricingWithoutPNRReply: "+ reply.getErrorGroup().getErrorWarningDescription().getFreeText() );
			}else {
				String fare = reply.getMainGroup().getPricingGroupLevelGroup().get(0).getFareInfoGroup().getFareAmount().getOtherMonetaryDetails().get(0).getAmount();
				BigDecimal totalFare = new BigDecimal(fare);
				String currency = reply.getMainGroup().getPricingGroupLevelGroup().get(0).getFareInfoGroup().getFareAmount().getOtherMonetaryDetails().get(0).getCurrency();
				FareCheckRulesReply fareCheckRulesReply = serviceHandler.getFareRules(amadeusSessionWrapper);
				Map<String, Map> fareRules = AmadeusHelper.getFareCheckRules(fareCheckRulesReply);
				miniRule = AmadeusHelper.getMiniRulesFromGenericRules(fareRules, totalFare, currency);
			}
		}catch(Exception e){
			amadeusLogger.debug("An exception while fetching the genericfareRule:"+ e.getMessage());
		}
		return miniRule;
	}
}
