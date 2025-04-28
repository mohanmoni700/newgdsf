package com.compassites.GDSWrapper.amadeus;

import com.amadeus.xml.farqnq_07_1_1a.FareCheckRules;
import com.amadeus.xml.farqnr_07_1_1a.FareCheckRulesReply;
import models.AmadeusSessionWrapper;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Created by user on 06-11-2014.
 */

@Component
public class FareRules {

    static org.slf4j.Logger logger = LoggerFactory.getLogger("gds");

    @Autowired
    private ServiceHandler serviceHandler;

    public FareCheckRules createFareRules(){

        FareCheckRules fareCheckRules = new FareCheckRules();

        FareCheckRules.MsgType msgType = new FareCheckRules.MsgType();
        FareCheckRules.MsgType.MessageFunctionDetails messageFunctionDetails = new FareCheckRules.MsgType.MessageFunctionDetails();
        messageFunctionDetails.setMessageFunction("712");
        msgType.setMessageFunctionDetails(messageFunctionDetails);
        fareCheckRules.setMsgType(msgType);

        FareCheckRules.ItemNumber itemNumber = new FareCheckRules.ItemNumber();
        FareCheckRules.ItemNumber.ItemNumberDetails itemNumberDetails = new FareCheckRules.ItemNumber.ItemNumberDetails();
        itemNumberDetails.setNumber("1");
        itemNumber.getItemNumberDetails().add(itemNumberDetails);
        fareCheckRules.setItemNumber(itemNumber);

        FareCheckRules.FareRule fareRule = new FareCheckRules.FareRule();
        FareCheckRules.FareRule.TarifFareRule tarifFareRule = new FareCheckRules.FareRule.TarifFareRule();
        tarifFareRule.getRuleSectionId().add("PE");
        /*tarifFareRule.getRuleSectionId().add("RU");
        tarifFareRule.getRuleSectionId().add("HI");
        tarifFareRule.getRuleSectionId().add("CO");
        tarifFareRule.getRuleSectionId().add("SU");
        tarifFareRule.getRuleSectionId().add("TR");
        tarifFareRule.getRuleSectionId().add("OD");*/
        fareRule.setTarifFareRule(tarifFareRule);
        fareCheckRules.setFareRule(fareRule);

        return fareCheckRules;
    }

    public FareCheckRules getFareInfoForFCType(String fcNumber){

        FareCheckRules fareCheckRules = new FareCheckRules();

        FareCheckRules.MsgType msgType = new FareCheckRules.MsgType();
        FareCheckRules.MsgType.MessageFunctionDetails messageFunctionDetails = new FareCheckRules.MsgType.MessageFunctionDetails();
        messageFunctionDetails.setMessageFunction("712");
        msgType.setMessageFunctionDetails(messageFunctionDetails);
        fareCheckRules.setMsgType(msgType);

        FareCheckRules.ItemNumber itemNumber = new FareCheckRules.ItemNumber();
        FareCheckRules.ItemNumber.ItemNumberDetails itemNumberDetails = new FareCheckRules.ItemNumber.ItemNumberDetails();
        itemNumberDetails.setNumber("1");
        itemNumber.getItemNumberDetails().add(itemNumberDetails);

        FareCheckRules.ItemNumber.ItemNumberDetails fcItemNumberDetails = new FareCheckRules.ItemNumber.ItemNumberDetails();
        fcItemNumberDetails.setNumber(fcNumber);
        fcItemNumberDetails.setType("FC");
        itemNumber.getItemNumberDetails().add(fcItemNumberDetails);

        fareCheckRules.setItemNumber(itemNumber);

        return fareCheckRules;
    }

    public FareCheckRules getFareCheckRulesForFareComponents(Map<String, String> fareComponentAndJourneyMap) {

        FareCheckRules fareCheckRules = new FareCheckRules();

        FareCheckRules.MsgType msgType = new FareCheckRules.MsgType();
        FareCheckRules.MsgType.MessageFunctionDetails messageFunctionDetails = new FareCheckRules.MsgType.MessageFunctionDetails();
        messageFunctionDetails.setMessageFunction("712");
        msgType.setMessageFunctionDetails(messageFunctionDetails);
        fareCheckRules.setMsgType(msgType);

        FareCheckRules.ItemNumber itemNumber = new FareCheckRules.ItemNumber();
        FareCheckRules.ItemNumber.ItemNumberDetails itemNumberDetails = new FareCheckRules.ItemNumber.ItemNumberDetails();
        itemNumberDetails.setNumber("1");
        itemNumber.getItemNumberDetails().add(itemNumberDetails);

        List<FareCheckRules.ItemNumber.ItemNumberDetails> fcItemNumberDetailsList = new ArrayList<>();
        for (Map.Entry<String, String> fcNumber : fareComponentAndJourneyMap.entrySet()) {
            FareCheckRules.ItemNumber.ItemNumberDetails fcItemNumberDetails = new FareCheckRules.ItemNumber.ItemNumberDetails();
            fcItemNumberDetails.setNumber(fcNumber.getKey());
            fcItemNumberDetails.setType("FC");
            fcItemNumberDetailsList.add(fcItemNumberDetails);
        }
        itemNumber.getItemNumberDetails().addAll(fcItemNumberDetailsList);

        fareCheckRules.setItemNumber(itemNumber);

        FareCheckRules.FareRule fareRule = new FareCheckRules.FareRule();
        FareCheckRules.FareRule.TarifFareRule tarifFareRule = new FareCheckRules.FareRule.TarifFareRule();
        tarifFareRule.getRuleSectionId().add("16");
        fareRule.setTarifFareRule(tarifFareRule);
        fareCheckRules.setFareRule(fareRule);

        return fareCheckRules;
    }

    public Map<String, FareCheckRulesReply> processFareRulesJourneyWise(AmadeusSessionWrapper amadeusSessionWrapper, Map<String, String> fareComponentsMap) {

        Map<String, FareCheckRulesReply> resultMap = new LinkedHashMap<>();
        List<String> errorMessages = new ArrayList<>();

        for (Map.Entry<String, String> entry : fareComponentsMap.entrySet()) {
            String itemNumber = entry.getKey();
            String originDestination = entry.getValue();

            Map<String, String> singleFareComponentMap = new LinkedHashMap<>();
            singleFareComponentMap.put(itemNumber, originDestination);

            try {
                FareCheckRulesReply reply = serviceHandler.getFareRulesFromFareComponent(amadeusSessionWrapper, singleFareComponentMap);
                if (reply != null) {
                    String key = originDestination + ":" + itemNumber;
                    resultMap.put(key, reply);
                } else {
                    logger.warn("Null reply for {}", originDestination);
                }
            } catch (Exception e) {
                String errorMsg = "Error retrieving fare rules for " + originDestination + ": " + e.getMessage();
                errorMessages.add(errorMsg);
                logger.error(errorMsg, e);
            }
        }

        if (!errorMessages.isEmpty()) {
            logger.error("Encountered {} errors: {}", errorMessages.size(), String.join("; ", errorMessages));
        }

        return resultMap;
    }

}