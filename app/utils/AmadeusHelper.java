package utils;

import com.amadeus.xml.farqnr_07_1_1a.FareCheckRulesReply;
import com.amadeus.xml.pnracc_11_3_1a.PNRReply;
import com.amadeus.xml.pnracc_11_3_1a.PNRReply.OriginDestinationDetails;
import com.amadeus.xml.pnracc_11_3_1a.ReservationControlInformationTypeI115879S;
import com.compassites.model.AirSegmentInformation;
import com.compassites.model.Journey;
import models.Airport;
import models.AmadeusSessionWrapper;
import models.MiniRule;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by yaseen on 19-10-2015.
 */
public class AmadeusHelper {

    public static boolean checkAirportCountry(String country, List<Journey> journeys) {
        Set<String> airportCodes = new HashSet<>();
        for (Journey journey : journeys) {
            for (AirSegmentInformation airSegmentInformation : journey.getAirSegmentList()) {
                airportCodes.add(airSegmentInformation.getFromLocation());
                airportCodes.add(airSegmentInformation.getToLocation());
            }
        }
        List<String> list = new ArrayList<>();
        list.addAll(airportCodes);
        boolean result = Airport.checkCountry(country, list);
        return result;
    }

    public static Map<String, String> readMultipleAirlinePNR(PNRReply gdsPNRReply) {
        Map<String, String> airlinePNRMap = new HashMap<>();
        int segmentSequence = 1;
        for (OriginDestinationDetails originDestinationDetails : gdsPNRReply.getOriginDestinationDetails()) {

            for (OriginDestinationDetails.ItineraryInfo itineraryInfo : originDestinationDetails.getItineraryInfo()) {
                ReservationControlInformationTypeI115879S itineraryReservationInfo = itineraryInfo.getItineraryReservationInfo();
                if (itineraryReservationInfo != null && itineraryReservationInfo.getReservation() != null) {
                    String airlinePNR = itineraryInfo.getItineraryReservationInfo().getReservation().getControlNumber();
                    String segments = itineraryInfo.getTravelProduct().getBoardpointDetail().getCityCode() + itineraryInfo.getTravelProduct().getOffpointDetail().getCityCode() + segmentSequence;
                    airlinePNRMap.put(segments.toLowerCase(), airlinePNR);
                    segmentSequence++;
                }

            }
        }
        return airlinePNRMap;
    }

    public static Map<String, Map> getFareCheckRules(FareCheckRulesReply fareCheckRulesReply) {

        List<FareCheckRulesReply.TariffInfo.FareRuleText> fareRuleTextList = fareCheckRulesReply.getTariffInfo().get(0).getFareRuleText();
        Map<String, String> changeMap = new ConcurrentHashMap<>();
        Map<String, String> cancelMap = new ConcurrentHashMap<>();
        int index = 0;

        for (; index < fareRuleTextList.size(); index++) {

            String trimmedValue = fareRuleTextList.get(index).getFreeText().toString().replaceAll("\\[|\\]", "").trim();
            if (trimmedValue.equals("CHANGES")) {

                int counter = index;
                for (; counter < fareRuleTextList.size(); counter++) {

                    String text = fareRuleTextList.get(counter).getFreeText().toString().replaceAll("\\[|\\]", "").trim();

                    if (text.equals("CANCELLATIONS")) {
                        break;
                    }
                    if(counter == fareRuleTextList.size()-1){
                        if(text.equalsIgnoreCase("ANY TIME") || text.equalsIgnoreCase("BEFORE DEPARTURE") || text.equalsIgnoreCase("AFTER DEPARTURE")){
                            break;
                        }
                    }

                    if (text.equals("ANY TIME")) {
                        changeMap.put(text, fareRuleTextList.get(++counter).getFreeText().toString());
                    } else if (text.equals("BEFORE DEPARTURE")) {
                        changeMap.put(text, fareRuleTextList.get(++counter).getFreeText().toString());
                    } else if (text.equals("AFTER DEPARTURE")) {
                        changeMap.put(text, fareRuleTextList.get(++counter).getFreeText().toString());
                    } else if ((text.endsWith("NO-SHOW.") && text.startsWith("CHARGE")) || text.equals("CHANGES PERMITTED FOR NO-SHOW.") || text.equals("CHANGES AGAINST NO SHOW - FREE")) {
                        changeMap.put("NO-SHOW", fareRuleTextList.get(counter).getFreeText().toString());
                    }

                }
                index = counter - 1;
            } else if (trimmedValue.equals("CANCELLATIONS")) {

                int counter = index;
                for (; counter < fareRuleTextList.size(); counter++) {
                    String cancelText = fareRuleTextList.get(counter).getFreeText().toString().replaceAll("\\[|\\]", "").trim();

                    if (cancelText.equals("CHANGES")) {
                        break;
                    }
                    if(counter == fareRuleTextList.size()-1){
                        if(cancelText.equalsIgnoreCase("ANY TIME") || cancelText.equalsIgnoreCase("BEFORE DEPARTURE") || cancelText.equalsIgnoreCase("AFTER DEPARTURE")){
                            break;
                        }
                    }


                    if (cancelText.equals("ANY TIME")) {
                        cancelMap.put(cancelText, fareRuleTextList.get(++counter).getFreeText().toString());
                    } else if (cancelText.equals("BEFORE DEPARTURE")) {
                        cancelMap.put(cancelText, fareRuleTextList.get(++counter).getFreeText().toString());
                    } else if (cancelText.equals("AFTER DEPARTURE")) {
                        cancelMap.put(cancelText, fareRuleTextList.get(++counter).getFreeText().toString());
                    } else if ((cancelText.endsWith("NO-SHOW.") && cancelText.startsWith("CHARGE")) || cancelText.equals("CANCELLATIONS PERMITTED FOR NO-SHOW.")) {
                        cancelMap.put("NO-SHOW", fareRuleTextList.get(counter).getFreeText().toString());
                    }

                }
                index = counter - 1;
            }
        }

        Map<String, Map> finalMap = new ConcurrentHashMap<>();
        finalMap.put("ChangeRules", changeMap);
        finalMap.put("CancellationRules", cancelMap);

        return finalMap;
    }



    public static Map<String, Map<String, List<String>>> getFareCheckRulesBenzy(FareCheckRulesReply fareCheckRulesReply) {
        if (fareCheckRulesReply == null || fareCheckRulesReply.getTariffInfo() == null || fareCheckRulesReply.getTariffInfo().isEmpty()) {
            return new ConcurrentHashMap<>();
        }

        List<FareCheckRulesReply.TariffInfo.FareRuleText> fareRuleTextList = fareCheckRulesReply.getTariffInfo().get(0).getFareRuleText();
        Map<String, Map<String, List<String>>> finalMap = new ConcurrentHashMap<>();
        Map<String, List<String>> changeMap = new ConcurrentHashMap<>();
        Map<String, List<String>> cancelMap = new ConcurrentHashMap<>();
        Map<String, List<String>> noteMap = new ConcurrentHashMap<>();
        int index = 0;
        boolean changesProcessed = false;
        boolean cancellationsProcessed = false;

        while (index < fareRuleTextList.size()) {
            String trimmedValue = fareRuleTextList.get(index).getFreeText().toString().replaceAll("\\[|\\]", "").trim();
            if (trimmedValue.equals("CHANGES") && !changesProcessed) {
                changesProcessed = true;
                index++;
                index = processRules(fareRuleTextList, index, changeMap, "CANCELLATIONS");
            } else if (trimmedValue.equals("CANCELLATIONS") && !cancellationsProcessed) {
                cancellationsProcessed = true;
                index++;
                index = processRules(fareRuleTextList, index, cancelMap, "CHANGES");
            } else {
                index++;
            }
        }

        if (changeMap.isEmpty() && cancelMap.isEmpty()) {
            index = 0;
            while (index < fareRuleTextList.size()) {
                String trimmedValue = fareRuleTextList.get(index).getFreeText().toString().replaceAll("\\[|\\]", "").trim();
                if (isNoteText(trimmedValue)) {
                    index++;
                    List<String> noteRules = new ArrayList<>();
                    while (index < fareRuleTextList.size()) {
                        String text = fareRuleTextList.get(index).getFreeText().toString().replaceAll("\\[|\\]", "").trim();
                        if (isSeparatorLine(text)) {
                            break;
                        }
                        if (!text.isEmpty() && !text.equals(" ")) {
                            noteRules.add(text);
                        }
                        index++;
                    }
                    noteMap.put(trimmedValue, noteRules);
                    break;
                }
                index++;
            }
        }

        if (!changeMap.isEmpty()) finalMap.put("Change", changeMap);
        if (!cancelMap.isEmpty()) finalMap.put("Cancellation", cancelMap);
        if (changeMap.isEmpty() && cancelMap.isEmpty() && !noteMap.isEmpty()) finalMap.put("Note", noteMap);

        return finalMap;
    }

    private static int processRules(List<FareCheckRulesReply.TariffInfo.FareRuleText> fareRuleTextList, int startIndex, Map<String, List<String>> rulesMap, String breakSection) {
        int index = startIndex;
        String currentCategory = null;
        List<String> rules = new ArrayList<>();
        boolean noteEncounteredInCategory = false;

        while (index < fareRuleTextList.size()) {
            String text = fareRuleTextList.get(index).getFreeText().toString().replaceAll("\\[|\\]", "").trim();

            if (text.equals(breakSection)) {
                if (currentCategory != null && !rules.isEmpty() && !rulesMap.containsKey(currentCategory)) {
                    rulesMap.put(currentCategory, new ArrayList<>(rules));
                }
                break;
            }

            if (isSpecificCategory(text) && !rulesMap.containsKey(text)) {
                if (currentCategory != null && !rules.isEmpty() && !rulesMap.containsKey(currentCategory)) {
                    rulesMap.put(currentCategory, new ArrayList<>(rules));
                }
                currentCategory = text;
                rules.clear();
                noteEncounteredInCategory = false;
                index++;
                continue;
            }

            if (isNoteText(text) || isSeparatorLine(text)) {
                if (currentCategory != null && !rules.isEmpty() && !rulesMap.containsKey(currentCategory)) {
                    rulesMap.put(currentCategory, new ArrayList<>(rules));
                }
                noteEncounteredInCategory = true;
                index++;
                while (index < fareRuleTextList.size()) {
                    String nextText = fareRuleTextList.get(index).getFreeText().toString().replaceAll("\\[|\\]", "").trim();
                    if (isSpecificCategory(nextText) || nextText.equals(breakSection)) {
                        break;
                    }
                    index++;
                }
                continue;
            }

            if (!text.isEmpty() && !text.equals(" ") && !noteEncounteredInCategory) {
                if (currentCategory == null) {
                    currentCategory = "Generic";
                }
                if (!isSpecificCategory(text)) {
                    rules.add(text);
                }
            }
            index++;
        }

        if (currentCategory != null && !rules.isEmpty() && !noteEncounteredInCategory && !rulesMap.containsKey(currentCategory)) {
            rulesMap.put(currentCategory, new ArrayList<>(rules));
        }

        return index;
    }

    private static boolean isSeparatorLine(String line) {
        return line.matches("^[\\-\\=]+$");
    }

    private static boolean isNoteText(String line) {
        return line.matches("^NOTE -.*");
    }

    private static boolean isSpecificCategory(String line) {
        return line.equalsIgnoreCase("ANY TIME") || line.equalsIgnoreCase("BEFORE DEPARTURE") || line.equalsIgnoreCase("AFTER DEPARTURE");
    }


    public static List<HashMap> getMiniRulesFromGenericRules(Map<String, Map> benzyFareRules,BigDecimal totalFare,String currency){

        Map<String,String> changeRulesMap = benzyFareRules.get("ChangeRules");
        MiniRule miniRule = new MiniRule();
        HashMap adultMap = new HashMap();
        List<HashMap> miniRules = new LinkedList<>();
        if(changeRulesMap.size() == 0) {
            miniRule.setChangeFeeBeforeDept(null);
            miniRule.setChangeFeeAfterDept(null);
            miniRule.setChangeFeeNoShow(null);
            miniRule.setChangeFeeBeforeDeptCurrency(currency);
            miniRule.setChangeRefundableBeforeDept(true);
            miniRule.setChangeFeeFeeAfterDeptCurrency(currency);
            miniRule.setChangeRefundableAfterDept(true);
            miniRule.setChangeFeeNoShowFeeCurrency(currency);
            miniRule.setChangeNoShowBeforeDept(true);
            miniRule.setChangeNoShowAfterDept(true);
        }else {
            if (changeRulesMap.get("BEFORE DEPARTURE") != null) {
                miniRule.setChangeFeeBeforeDeptCurrency(currency);
                if (changeRulesMap.get("BEFORE DEPARTURE").contains("CHANGES PERMITTED")) {
                    miniRule.setChangeRefundableBeforeDept(true);
                    miniRule.setChangeFeeBeforeDept(new BigDecimal("0"));
                }
                if (changeRulesMap.get("BEFORE DEPARTURE").contains("NON-REFUNDABLE")) {
                    miniRule.setChangeFeeBeforeDept(totalFare);
                    miniRule.setChangeRefundableBeforeDept(false);
                }
                if (changeRulesMap.get("BEFORE DEPARTURE").contains("CHARGE")) {
                    String data = changeRulesMap.get("BEFORE DEPARTURE");
                    BigDecimal charge = getCharges(data);
                    miniRule.setChangeFeeBeforeDept(charge);
                    miniRule.setChangeRefundableBeforeDept(true);
                }

        }
        if(changeRulesMap.get("AFTER DEPARTURE") != null) {
            miniRule.setChangeFeeFeeAfterDeptCurrency(currency);
            if(changeRulesMap.get("AFTER DEPARTURE").contains("CHANGES PERMITTED")){
                miniRule.setChangeRefundableAfterDept(true);
                miniRule.setChangeFeeAfterDept(new BigDecimal("0"));
            }
            if(changeRulesMap.get("AFTER DEPARTURE").contains("NON-REFUNDABLE")){
                miniRule.setChangeFeeAfterDept(totalFare);
                miniRule.setChangeRefundableAfterDept(false);
            }
            if(changeRulesMap.get("AFTER DEPARTURE").contains("CHARGE")){
                String data = changeRulesMap.get("AFTER DEPARTURE");
                BigDecimal charge = getCharges(data);
                miniRule.setChangeFeeAfterDept(charge);
                miniRule.setChangeRefundableAfterDept(true);
            }
        }
        if(changeRulesMap.get("NO-SHOW") != null) {
            miniRule.setChangeFeeNoShowFeeCurrency(currency);
            if(changeRulesMap.get("NO-SHOW").contains("CHANGES PERMITTED")){
                miniRule.setChangeNoShowBeforeDept(true);
                miniRule.setChangeNoShowAfterDept(true);
                miniRule.setChangeFeeNoShow(new BigDecimal("0"));
            }
            if(changeRulesMap.get("NO-SHOW").contains("NON-REFUNDABLE")){
                miniRule.setChangeFeeNoShow(totalFare);
                miniRule.setChangeNoShowBeforeDept(false);
                miniRule.setChangeNoShowAfterDept(false);
            }
            if(changeRulesMap.get("NO-SHOW").contains("CHARGE")){
                String data = changeRulesMap.get("NO-SHOW");
                BigDecimal charge = getCharges(data);
                miniRule.setChangeFeeNoShow(charge);
                miniRule.setChangeNoShowBeforeDept(true);
                miniRule.setChangeNoShowAfterDept(true);
            }
        }

        if(changeRulesMap.get("ANY TIME") != null){
            miniRule.setChangeFeeNoShowFeeCurrency(currency);
            miniRule.setChangeFeeFeeAfterDeptCurrency(currency);
            miniRule.setChangeFeeBeforeDeptCurrency(currency);
            if(changeRulesMap.get("ANY TIME").contains("CHANGES PERMITTED")){
                miniRule.setChangeFeeBeforeDept(new BigDecimal("0"));
                miniRule.setChangeFeeAfterDept(new BigDecimal("0"));
                miniRule.setChangeFeeNoShow(new BigDecimal("0"));
                miniRule.setChangeRefundableAfterDept(true);
                miniRule.setChangeRefundableAfterDept(true);
                miniRule.setChangeNoShowBeforeDept(true);
                miniRule.setChangeRefundableBeforeDept(true);
                miniRule.setChangeNoShowAfterDept(true);
            }
            if(changeRulesMap.get("ANY TIME").contains("NON-REFUNDABLE")){
                miniRule.setChangeFeeBeforeDept(totalFare);
                miniRule.setChangeFeeAfterDept(totalFare);
                miniRule.setChangeFeeNoShow(totalFare);
                miniRule.setChangeRefundableAfterDept(false);
                miniRule.setChangeRefundableAfterDept(false);
                miniRule.setChangeNoShowBeforeDept(false);
                miniRule.setChangeRefundableBeforeDept(false);
                miniRule.setChangeNoShowAfterDept(false);
            }
            if(changeRulesMap.get("ANY TIME").contains("CHARGE")){
                String data = changeRulesMap.get("ANY TIME");
                BigDecimal charge = getCharges(data);
                if(miniRule.getChangeFeeBeforeDept() == null)
                miniRule.setChangeFeeBeforeDept(charge);
                if(miniRule.getChangeFeeAfterDept() == null)
                miniRule.setChangeFeeAfterDept(charge);
                if(miniRule.getChangeFeeNoShow() == null)
                miniRule.setChangeFeeNoShow(charge);

                miniRule.setChangeRefundableAfterDept(true);
                miniRule.setChangeRefundableAfterDept(true);
                miniRule.setChangeNoShowBeforeDept(true);
                miniRule.setChangeRefundableBeforeDept(true);
                miniRule.setChangeNoShowAfterDept(true);
            }

            }
        }
        Map<String,String> cancellationRulesMap = benzyFareRules.get("CancellationRules");
        if(cancellationRulesMap.size() == 0){
            miniRule.setCancellationFeeBeforeDept(null);
            miniRule.setCancellationFeeAfterDept(null);
            miniRule.setCancellationFeeNoShow(null);
            miniRule.setCancellationFeeBeforeDeptCurrency(currency);
            miniRule.setCancellationRefundableBeforeDept(true);
            miniRule.setCancellationFeeAfterDeptCurrency(currency);
            miniRule.setCancellationRefundableAfterDept(true);
            miniRule.setCancellationNoShowCurrency(currency);
                miniRule.setCancellationNoShowAfterDept(true);
                miniRule.setCancellationNoShowBeforeDept(true);
        }else {
            if (cancellationRulesMap.get("BEFORE DEPARTURE") != null) {
                miniRule.setCancellationFeeBeforeDeptCurrency(currency);
                if (cancellationRulesMap.get("BEFORE DEPARTURE").contains("CANCELLATIONS PERMITTED")) {
                    miniRule.setCancellationRefundableBeforeDept(true);
                    miniRule.setCancellationFeeBeforeDept(new BigDecimal("0"));
                }
                if (cancellationRulesMap.get("BEFORE DEPARTURE").contains("NON-REFUNDABLE")) {
                    miniRule.setCancellationFeeBeforeDept(totalFare);
                    miniRule.setCancellationRefundableBeforeDept(false);
                }
                if (cancellationRulesMap.get("BEFORE DEPARTURE").contains("CHARGE")) {
                    String data = cancellationRulesMap.get("BEFORE DEPARTURE");
                    BigDecimal charge = getCharges(data);
                    miniRule.setCancellationFeeBeforeDept(charge);
                    miniRule.setCancellationRefundableBeforeDept(true);
                }
            }
            if (cancellationRulesMap.get("AFTER DEPARTURE") != null) {
                miniRule.setCancellationFeeAfterDeptCurrency(currency);
                if (cancellationRulesMap.get("AFTER DEPARTURE").contains("CANCELLATIONS PERMITTED")) {
                    miniRule.setCancellationRefundableAfterDept(true);
                    miniRule.setCancellationFeeAfterDept(new BigDecimal("0"));
                }
                if (cancellationRulesMap.get("AFTER DEPARTURE").contains("NON-REFUNDABLE")) {
                    miniRule.setCancellationFeeAfterDept(totalFare);
                    miniRule.setCancellationRefundableAfterDept(false);
                }
                if (cancellationRulesMap.get("AFTER DEPARTURE").contains("CHARGE")) {
                    String data = cancellationRulesMap.get("AFTER DEPARTURE");
                    BigDecimal charge = getCharges(data);
                    miniRule.setCancellationFeeAfterDept(charge);
                    miniRule.setCancellationRefundableAfterDept(true);
                }
            }

        if(cancellationRulesMap.get("NO-SHOW") != null) {
            miniRule.setCancellationNoShowCurrency(currency);
            if(cancellationRulesMap.get("NO-SHOW").contains("CANCELLATIONS PERMITTED")){
                miniRule.setCancellationNoShowAfterDept(true);
                miniRule.setCancellationNoShowBeforeDept(true);
                miniRule.setCancellationFeeNoShow(new BigDecimal("0"));
            }
            if(cancellationRulesMap.get("NO-SHOW").contains("NON-REFUNDABLE")){
                miniRule.setCancellationFeeNoShow(totalFare);
                miniRule.setCancellationNoShowAfterDept(false);
                miniRule.setCancellationNoShowBeforeDept(false);
            }
            if(cancellationRulesMap.get("NO-SHOW").contains("CHARGE")){
                String data = cancellationRulesMap.get("NO-SHOW");
                BigDecimal charge = getCharges(data);
                miniRule.setCancellationFeeNoShow(charge);
                miniRule.setCancellationNoShowAfterDept(true);
                miniRule.setCancellationNoShowBeforeDept(true);
            }
        }
        if(cancellationRulesMap.get("ANY TIME") != null){
            miniRule.setCancellationNoShowCurrency(currency);
            miniRule.setCancellationFeeAfterDeptCurrency(currency);
            miniRule.setCancellationFeeBeforeDeptCurrency(currency);
            if(cancellationRulesMap.get("ANY TIME").contains("CANCELLATIONS PERMITTED")){
                miniRule.setCancellationRefundableBeforeDept(true);
                miniRule.setCancellationRefundableAfterDept(true);
                miniRule.setCancellationNoShowAfterDept(true);
                miniRule.setCancellationNoShowBeforeDept(true);
                miniRule.setCancellationFeeBeforeDept(new BigDecimal("0"));
                miniRule.setCancellationFeeAfterDept(new BigDecimal("0"));
                miniRule.setCancellationFeeNoShow(new BigDecimal("0"));
            }
            if(cancellationRulesMap.get("ANY TIME").contains("NON-REFUNDABLE")){
                miniRule.setCancellationFeeBeforeDept(totalFare);
                miniRule.setCancellationFeeAfterDept(totalFare);
                miniRule.setCancellationFeeNoShow(totalFare);
                miniRule.setCancellationRefundableBeforeDept(false);
                miniRule.setCancellationRefundableAfterDept(false);
                miniRule.setCancellationNoShowAfterDept(false);
                miniRule.setCancellationNoShowBeforeDept(false);
            }
            if(cancellationRulesMap.get("ANY TIME").contains("CHARGE")){
                String data = cancellationRulesMap.get("ANY TIME");
                BigDecimal charge = getCharges(data);
                if(miniRule.getCancellationFeeBeforeDept() == null)
                miniRule.setCancellationFeeBeforeDept(charge);
                if(miniRule.getCancellationFeeAfterDept() == null)
                miniRule.setCancellationFeeAfterDept(charge);
                if(miniRule.getCancellationFeeNoShow() ==  null)
                miniRule.setCancellationFeeNoShow(charge);

                    miniRule.setCancellationRefundableBeforeDept(true);
                    miniRule.setCancellationRefundableAfterDept(true);
                    miniRule.setCancellationNoShowAfterDept(true);
                    miniRule.setCancellationNoShowBeforeDept(true);
                }
            }
        }
        if(miniRule.getChangeFeeNoShow() == null && miniRule.getCancellationFeeAfterDept() == null &&
           miniRule.getCancellationFeeBeforeDept() == null &&  miniRule.getCancellationFeeNoShow() == null &&
           miniRule.getChangeFeeAfterDept() == null && miniRule.getChangeFeeBeforeDept() == null){
            miniRule.setChangeNoShowBeforeDept(true);
            miniRule.setChangeRefundableBeforeDept(true);
            miniRule.setChangeRefundableAfterDept(true);
            miniRule.setCancellationRefundableAfterDept(true);
            miniRule.setCancellationRefundableBeforeDept(true);
            miniRule.setCancellationNoShowBeforeDept(true);
            miniRule.setCancellationNoShowAfterDept(true);
        }

        adultMap.put("ADT", miniRule);
        miniRules.add(adultMap);


        return  miniRules;
    }

    public static List<String> getDetailedFareDetailsList(List<FareCheckRulesReply.TariffInfo.FareRuleText> fareRuleTextList){

        List<String> detailedFareRulesList = new ArrayList<>();

        for (FareCheckRulesReply.TariffInfo.FareRuleText fareRuleText : fareRuleTextList){
            detailedFareRulesList.addAll(fareRuleText.getFreeText());
        }

        return  detailedFareRulesList;
    }

    public static BigDecimal getCharges(String data){
        BigDecimal charge = new BigDecimal(0);
        if(data != null) {
            String regex = "\\d+";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(data);
            StringBuilder sb = new StringBuilder();
            while (matcher.find()) {
                sb.append(matcher.group());
            }
            charge = new BigDecimal(sb.toString());
        }
        return charge;
    }

}