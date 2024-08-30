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

public class TravelomatrixFareRuleDeserializer extends JsonDeserializer<List<Rule>> {
    @Override
    public List<Rule> deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException {

        JsonNode node = jp.getCodec().readTree(jp);
        List<Rule> rules = new ArrayList<>();

        Iterator<JsonNode> elements = node.elements();
        while (elements.hasNext()) {
            JsonNode element = elements.next();
            if (element.isObject()) {
                Rule rule = new Rule();
                rule.setStartTime(element.get("start_time").asText());
                rule.setEndTime(element.get("end_time").asText());
                if(element.get("amount") != null)
                rule.setAmount(element.get("amount").asInt());
                rule.setTax(element.has("tax") ? element.get("tax").asInt() : 0);
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
