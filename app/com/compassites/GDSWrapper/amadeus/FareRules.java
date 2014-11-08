package com.compassites.GDSWrapper.amadeus;

import com.amadeus.xml.farqnq_07_1_1a.FareCheckRules;

/**
 * Created by user on 06-11-2014.
 */
public class FareRules {


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
        tarifFareRule.getRuleSectionId().add("RU");
        tarifFareRule.getRuleSectionId().add("HI");
        tarifFareRule.getRuleSectionId().add("PE");
        tarifFareRule.getRuleSectionId().add("CO");
        tarifFareRule.getRuleSectionId().add("SU");
        tarifFareRule.getRuleSectionId().add("TR");
        tarifFareRule.getRuleSectionId().add("OD");
        fareRule.setTarifFareRule(tarifFareRule);
        fareCheckRules.setFareRule(fareRule);

        return fareCheckRules;
    }
}
