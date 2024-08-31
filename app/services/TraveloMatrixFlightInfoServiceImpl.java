package services;

import com.compassites.GDSWrapper.travelomatrix.FareRulesTMX;
import com.compassites.exceptions.RetryException;
import com.compassites.model.*;
import com.compassites.model.travelomatrix.ResponseModels.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import models.MiniRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;


@Service
public class TraveloMatrixFlightInfoServiceImpl implements TraveloMatrixFlightInfoService{

    static Logger logger = LoggerFactory.getLogger("gds");

    static Logger travelomatrixLogger = LoggerFactory.getLogger("travelomatrix");

   public FareRulesTMX fareRulesTMX = new FareRulesTMX();

   @Override
   @RetryOnFailure(attempts = 2, delay =30000, exception = RetryException.class)
    public List<HashMap> flightFareRules(String resultToken,String returnResultToken) {
       List<HashMap> minirule = null;
       JsonNode returnJsonResponse = null;
       JsonNode jsonResponse = fareRulesTMX.getFareRules(resultToken);
       if(!returnResultToken.equalsIgnoreCase("null"))
       returnJsonResponse = fareRulesTMX.getFareRules(returnResultToken);
       try {
           travelomatrixLogger.debug("Response for FareRules: ResultToken:"+ resultToken +" ----  Response: \n"+ jsonResponse);
           TraveloMatrixFaruleReply response = new ObjectMapper().treeToValue(jsonResponse, TraveloMatrixFaruleReply.class);
           TraveloMatrixFaruleReply returnResponse = null;
           if(!returnResultToken.equalsIgnoreCase("null")){
           returnResponse = new ObjectMapper().treeToValue(returnJsonResponse, TraveloMatrixFaruleReply.class);
               //Roundtrip
               if (response.getStatus() == 0 && returnResponse.getStatus() == 0) {
                   travelomatrixLogger.debug("FareRule Respose is not Reeceived for ResultToken :" + resultToken);
               } else if (response != null && returnResponse != null) {
                   minirule = getMergedMiniRuleFromFareRule(response,returnResponse);
               } else if (response != null) {
                   minirule = getMiniRuleFromFareRule(response);
               } else if (returnResponse != null) {
                   minirule = getMiniRuleFromFareRule(returnResponse);
               }
           }else{
               if (response.getStatus() == 0) {
                   travelomatrixLogger.debug("FareRule Respose is not Reeceived for ResultToken :" + resultToken);
               } else{
                   minirule = getMiniRuleFromFareRule(response);
               }
           }
       } catch (JsonProcessingException e) {
           throw new RuntimeException(e);
       }
       return minirule;
   }

   public List<HashMap> getMiniRuleFromFareRule(TraveloMatrixFaruleReply response){
       MiniRule miniRule = new MiniRule();
       HashMap AdultMap = new HashMap();
       List<HashMap> miniRules = new LinkedList<>();
       BigDecimal zeroDecimal = new BigDecimal(0);
       String currency = "INR";
       if(response.getFareRule().getFareRuleDetail() != null){
           List<Rule> cancellationChargeList = null;
           List<Rule> dateChangesList = null;
           List<Rule> noShowChargesList = null;
           if(response.getFareRule().getFareRuleDetail().get(0) != null) {
               // Coding to be done
               cancellationChargeList = response.getFareRule().getFareRuleDetail().get(0).getCancellationCharge();
               dateChangesList = response.getFareRule().getFareRuleDetail().get(0).getDateChange();
               noShowChargesList = response.getFareRule().getFareRuleDetail().get(0).getNoShowCharge();
           }
           BigDecimal cancellationChargeBeforeDept = null;
           BigDecimal dateChangeBeforeDept = null;
           BigDecimal cancellationChargeAfterDept = null;
           BigDecimal dateChangeAfterDept = null;
           BigDecimal noShowBeforeDept = null;
           BigDecimal noShowAfterDept = null;

           for(Rule cancellationCharge : cancellationChargeList){
               if(cancellationCharge.getAmount() != 0) {
                   BigDecimal charge = new BigDecimal(cancellationCharge.getAmount());
                   if (cancellationChargeBeforeDept == null) {
                       cancellationChargeBeforeDept = charge;
                   } else if (cancellationChargeBeforeDept.compareTo(charge) == -1) {
                       cancellationChargeBeforeDept = charge;
                   }
               }else if(cancellationCharge.getAmount() == 0){
                   cancellationChargeBeforeDept = new BigDecimal(0);
               }
           }

           for(Rule dateCharge : dateChangesList){
               if(dateCharge.getAmount() != 0) {
                   BigDecimal charge = new BigDecimal(dateCharge.getAmount());
                   if (dateChangeBeforeDept == null) {
                       dateChangeBeforeDept = charge;
                   } else if (dateChangeBeforeDept.compareTo(charge) == -1) {
                       dateChangeBeforeDept = charge;
                   }
               }else  if(dateCharge.getAmount() == 0) {
                   dateChangeBeforeDept = new BigDecimal(0);
               }
           }

           BigDecimal markUp =new BigDecimal(play.Play.application().configuration().getDouble("markup"));
           if(cancellationChargeBeforeDept != null)
               cancellationChargeBeforeDept= cancellationChargeBeforeDept.add(cancellationChargeBeforeDept.multiply(markUp)).setScale(2, BigDecimal.ROUND_HALF_UP);
           if(dateChangeBeforeDept != null)
               dateChangeBeforeDept= dateChangeBeforeDept.add(dateChangeBeforeDept.multiply(markUp)).setScale(2, BigDecimal.ROUND_HALF_UP);

           miniRule.setCancellationFeeBeforeDept(cancellationChargeBeforeDept);
           miniRule.setChangeFeeBeforeDept(dateChangeBeforeDept);
           if(cancellationChargeBeforeDept!= null && cancellationChargeBeforeDept.compareTo(zeroDecimal) == 1) {
               miniRule.setCancellationRefundableBeforeDept(true);
           }

           miniRule.setCancellationFeeAfterDept(zeroDecimal);
           miniRule.setChangeFeeNoShow(zeroDecimal);
           miniRule.setCancellationFeeNoShow(zeroDecimal);
           miniRule.setCancellationRefundableAfterDept(false);
           miniRule.setCancellationNoShowBeforeDept(false);
           miniRule.setCancellationNoShowAfterDept(false);
           miniRule.setChangeFeeAfterDept(zeroDecimal);
           miniRule.setChangeFeeNoShow(zeroDecimal);
           miniRule.setCancellationFeeAfterDeptCurrency(currency);
           miniRule.setCancellationNoShowCurrency(currency);
           miniRule.setCancellationFeeBeforeDeptCurrency(currency);
           miniRule.setChangeFeeFeeAfterDeptCurrency(currency);
           if(dateChangeBeforeDept != null && dateChangeBeforeDept.compareTo(zeroDecimal) == 1) {
               miniRule.setChangeRefundableBeforeDept(true);
           }
       miniRule.setChangeRefundableAfterDept(false);
       miniRule.setChangeNoShowBeforeDept(false);
       miniRule.setChangeNoShowAfterDept(false);
       miniRule.setChangeFeeBeforeDeptCurrency(currency);
       miniRule.setChangeFeeFeeAfterDeptCurrency(currency);
       miniRule.setChangeFeeNoShowFeeCurrency(currency);

           AdultMap.put("ADT", miniRule);
           miniRules.add(AdultMap);
       }else{
           logger.debug("minirules are null");
       }
       return miniRules;
   }
   //this is retrieve flight info
   public FlightItinerary getFlightInfo(FlightItinerary flightItinerary){
       FlightItinerary flightItinerary1 = new FlightItinerary();
       flightItinerary1.setIsLCC(flightItinerary.getLCC());
       flightItinerary1.setResultToken(flightItinerary.getResultToken());
       flightItinerary1.setId(flightItinerary.getId());
       flightItinerary1.setPricingInformation(flightItinerary.getPricingInformation());
       List<Journey> journeyList = flightItinerary.getNonSeamenJourneyList();
       for(Journey journey:journeyList){
           List<AirSegmentInformation> airSegmentInformationList = journey.getAirSegmentList();
           for(AirSegmentInformation airSegmentInformation:airSegmentInformationList){
               FlightInfo flightInfo = new FlightInfo();
               String baggage = airSegmentInformation.getBaggage();
               String numericPart = baggage.replaceAll("[^0-9]", "");
               String nonNumericPart = baggage.replaceAll("[0-9]", "").trim();
               BigInteger baga = new BigInteger(numericPart);
               flightInfo.setBaggageAllowance(baga);
               flightInfo.setBaggageUnit(nonNumericPart);
               flightInfo.setAmenities(null);
               airSegmentInformation.setFlightInfo(flightInfo);
           }
       }
       flightItinerary1.setNonSeamenJourneyList(journeyList);
       flightItinerary1.setJourneyList(journeyList);
       flightItinerary1.setSeamanPricingInformation(flightItinerary.getSeamanPricingInformation());
       flightItinerary1.setFareSourceCode(flightItinerary.getFareSourceCode());
       flightItinerary1.setFareType(flightItinerary.getFareType());
       flightItinerary1.setPriceOnlyPTC(flightItinerary.isPriceOnlyPTC());
       flightItinerary1.setPassportMandatory(flightItinerary.isPassportMandatory());
       flightItinerary1.setRefundable(flightItinerary.getRefundable());
       flightItinerary1.setTotalTravelTime(flightItinerary.getTotalTravelTime());
       flightItinerary1.setTotalTravelTimeStr(flightItinerary.getTotalTravelTimeStr());
       return flightItinerary1;
   }

 public List<HashMap>  getMergedMiniRuleFromFareRule(TraveloMatrixFaruleReply response,TraveloMatrixFaruleReply returnResponse){
     MiniRule miniRule = new MiniRule();
     HashMap AdultMap = new HashMap();
     List<HashMap> miniRules = new LinkedList<>();
     BigDecimal zeroDecimal = new BigDecimal(0);
     String currency = "INR";
     if(response.getFareRule().getFareRuleDetail() != null){
         List<Rule> cancellationChargeList = null;
         List<Rule> dateChangesList = null;
         List<Rule> noShowChargesList = null;

         List<Rule> recancellationChargeList = null;
         List<Rule> redateChangesList = null;
         List<Rule> renoShowChargesList = null;

         if(response.getFareRule().getFareRuleDetail().get(0) != null) {
             // Coding to be done
             cancellationChargeList = response.getFareRule().getFareRuleDetail().get(0).getCancellationCharge();
             dateChangesList = response.getFareRule().getFareRuleDetail().get(0).getDateChange();
             noShowChargesList = response.getFareRule().getFareRuleDetail().get(0).getNoShowCharge();
         }
         if(returnResponse.getFareRule().getFareRuleDetail().get(0) != null) {
             // Coding to be done
             recancellationChargeList = returnResponse.getFareRule().getFareRuleDetail().get(0).getCancellationCharge();
             redateChangesList = returnResponse.getFareRule().getFareRuleDetail().get(0).getDateChange();
             renoShowChargesList = returnResponse.getFareRule().getFareRuleDetail().get(0).getNoShowCharge();
         }
         BigDecimal cancellationChargeBeforeDept = null;
         BigDecimal dateChangeBeforeDept = null;
         BigDecimal cancellationChargeAfterDept = null;
         BigDecimal dateChangeAfterDept = null;
         BigDecimal noShowBeforeDept = null;
         BigDecimal noShowAfterDept = null;

         for(Rule cancellationCharge : cancellationChargeList){
             if(cancellationCharge.getAmount() != 0) {
                 BigDecimal charge = new BigDecimal(cancellationCharge.getAmount());
                 if (cancellationChargeBeforeDept == null) {
                     cancellationChargeBeforeDept = charge;
                 } else if (cancellationChargeBeforeDept.compareTo(charge) == -1) {
                     cancellationChargeBeforeDept = charge;
                 }
             }else if(cancellationCharge.getAmount() == 0){
                 cancellationChargeBeforeDept = new BigDecimal(0);
             }
         }

         for(Rule cancellationCharge : recancellationChargeList){
             if(cancellationCharge.getAmount() != 0) {
                 BigDecimal charge = new BigDecimal(cancellationCharge.getAmount());
                 if (cancellationChargeBeforeDept == null) {
                     cancellationChargeBeforeDept = charge;
                 } else if (cancellationChargeBeforeDept.compareTo(charge) == -1) {
                     cancellationChargeBeforeDept.add(charge);
                 }
             }else if(cancellationCharge.getAmount() == 0){
                 cancellationChargeBeforeDept = new BigDecimal(0);
             }
         }

         for(Rule dateCharge : dateChangesList){
             if(dateCharge.getAmount() != 0) {
                 BigDecimal charge = new BigDecimal(dateCharge.getAmount());
                 if (dateChangeBeforeDept == null) {
                     dateChangeBeforeDept = charge;
                 } else if (dateChangeBeforeDept.compareTo(charge) == -1) {
                     dateChangeBeforeDept = charge;
                 }
             }else  if(dateCharge.getAmount() == 0) {
                 dateChangeBeforeDept = new BigDecimal(0);
             }
         }

         for(Rule dateCharge : redateChangesList){
             if(dateCharge.getAmount() != 0) {
                 BigDecimal charge = new BigDecimal(dateCharge.getAmount());
                 if (dateChangeBeforeDept == null) {
                     dateChangeBeforeDept = charge;
                 } else if (dateChangeBeforeDept.compareTo(charge) == -1) {
                     dateChangeBeforeDept.add(charge);
                 }
             }else  if(dateCharge.getAmount() == 0) {
                 dateChangeBeforeDept = new BigDecimal(0);
             }
         }

         BigDecimal markUp =new BigDecimal(play.Play.application().configuration().getDouble("markup"));
         if(cancellationChargeBeforeDept != null)
             cancellationChargeBeforeDept= cancellationChargeBeforeDept.add(cancellationChargeBeforeDept.multiply(markUp)).setScale(2, BigDecimal.ROUND_HALF_UP);
         if(dateChangeBeforeDept != null)
             dateChangeBeforeDept= dateChangeBeforeDept.add(dateChangeBeforeDept.multiply(markUp)).setScale(2, BigDecimal.ROUND_HALF_UP);

         miniRule.setCancellationFeeBeforeDept(cancellationChargeBeforeDept);
         miniRule.setChangeFeeBeforeDept(dateChangeBeforeDept);
         if(cancellationChargeBeforeDept!= null && cancellationChargeBeforeDept.compareTo(zeroDecimal) == 1) {
             miniRule.setCancellationRefundableBeforeDept(true);
         }

         miniRule.setCancellationFeeAfterDept(zeroDecimal);
         miniRule.setChangeFeeNoShow(zeroDecimal);
         miniRule.setCancellationFeeNoShow(zeroDecimal);
         miniRule.setCancellationRefundableAfterDept(false);
         miniRule.setCancellationNoShowBeforeDept(false);
         miniRule.setCancellationNoShowAfterDept(false);
         miniRule.setChangeFeeAfterDept(zeroDecimal);
         miniRule.setChangeFeeNoShow(zeroDecimal);
         miniRule.setCancellationFeeAfterDeptCurrency(currency);
         miniRule.setCancellationNoShowCurrency(currency);
         miniRule.setCancellationFeeBeforeDeptCurrency(currency);
         miniRule.setChangeFeeFeeAfterDeptCurrency(currency);
         if(dateChangeBeforeDept != null && dateChangeBeforeDept.compareTo(zeroDecimal) == 1) {
             miniRule.setChangeRefundableBeforeDept(true);
         }
         miniRule.setChangeRefundableAfterDept(false);
         miniRule.setChangeNoShowBeforeDept(false);
         miniRule.setChangeNoShowAfterDept(false);
         miniRule.setChangeFeeBeforeDeptCurrency(currency);
         miniRule.setChangeFeeFeeAfterDeptCurrency(currency);
         miniRule.setChangeFeeNoShowFeeCurrency(currency);

         AdultMap.put("ADT", miniRule);
         miniRules.add(AdultMap);
     }else{
         logger.debug("minirules are null");
     }
     return miniRules;
 }

}
