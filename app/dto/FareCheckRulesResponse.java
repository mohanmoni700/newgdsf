package dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.pojomatic.annotations.Property;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FareCheckRulesResponse implements Serializable {

    private List<HashMap> miniRule;
    private List<String> detailedRuleList;

    @JsonProperty
    @Property
    private String cabinClass;

    private List<FareType> fareTypes;

    public Map<String, Map<String, List<String>>> getRuleMap() {
        return ruleMap;
    }

    public void setRuleMap(Map<String, Map<String, List<String>>> ruleMap) {
        this.ruleMap = ruleMap;
    }

    private Map<String, Map<String,List<String>>> ruleMap;

    public List<HashMap> getMiniRule() {
        return miniRule;
    }

    public void setMiniRule(List<HashMap> miniRule) {
        this.miniRule = miniRule;
    }

    public List<String> getDetailedRuleList() {
        return detailedRuleList;
    }

    public void setDetailedRuleList(List<String> detailedRuleList) {
        this.detailedRuleList = detailedRuleList;
    }

    public List<FareType> getFareTypes() {
        return fareTypes;
    }

    public void setFareTypes(List<FareType> fareTypes) {
        this.fareTypes = fareTypes;
    }

    public String getCabinClass(String cabinClass) {
        return this.cabinClass;
    }

    public void setCabinClass(String cabinClass) {
        this.cabinClass = cabinClass;
    }

}
