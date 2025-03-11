package dto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FareCheckRulesResponse {

    private List<HashMap> miniRule;
    private List<String> detailedRuleList;

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


}
