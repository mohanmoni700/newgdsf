package utils;

import com.compassites.model.travelomatrix.ResponseModels.Rule;
import com.compassites.model.travelomatrix.ResponseModels.TmxSpecialFareRule;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TravelomatrixFareRuleDeserializer extends JsonDeserializer<List<Rule>> {
    @Override
    public List<Rule> deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException {

        JsonNode node = jp.getCodec().readTree(jp);
        List<Rule> rules = new ArrayList<>();

        Iterator<JsonNode> elements = node.elements();
        while (elements.hasNext()) {
            JsonNode element = elements.next();
            String regex = "([A-Za-z]+)\\s(\\d+)";
            Pattern pattern = Pattern.compile(regex);
            if (element.isObject()) {
                Rule rule = new Rule();
                rule.setStartTime(element.get("start_time").asText());
                rule.setEndTime(element.get("end_time").asText());
                if(element.get("amount") != null) {
                    String amountStr = element.get("amount").asText();
                    Matcher matcher = pattern.matcher(amountStr);
                    Integer amount = null;
                    if (matcher.matches()) {
                        String currency = matcher.group(1); // This should be "INR"
                        String numberString = matcher.group(2); // This should be "2799"
                        try {
                            amount = Integer.parseInt(numberString);
                        } catch (NumberFormatException e) {
                            System.out.println("Invalid number format");
                        }
                    } else {
                        if(!element.get("amount").isNull()){
                            amount = element.get("amount").asInt();
                        }

                    }

                    rule.setAmount(amount);
                }
//                rule.setTax(element.has("tax") ? element.get("tax").asInt() : 0);
                if (element.has("tax") && !element.get("tax").isNull()) {
                    rule.setTax(element.get("tax").asInt());
                }
                rule.setPolicyInfo(element.has("policyInfo") ? element.get("policyInfo").asText() : "");
                rules.add(rule);
            } else if (element.isTextual()) {
                // Handling "DEFAULT" and "BEFORE_DEPARTURE"
                String key = element.asText();
                TmxSpecialFareRule specialRule = new TmxSpecialFareRule();
                specialRule.setRuleType(key);
                JsonNode specialNode = node.get(key);
                specialRule.setStartTime(specialNode.get("start_time").asText());
                specialRule.setEndTime(specialNode.get("end_time").asText());
                specialRule.setAmount(specialNode.get("amount").asInt());
                specialRule.setPolicyInfo(specialNode.get("policyInfo").asText());
                rules.add(specialRule);
            }
        }

        return rules;
    }
}
