package dto;

import java.util.HashMap;
import java.util.List;

public class FareCheckRulesResponse {

    private List<HashMap> miniRule;
    private List<String> detailedRuleList;

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
