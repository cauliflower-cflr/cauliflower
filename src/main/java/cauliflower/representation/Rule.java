package cauliflower.representation;

import java.util.List;

public class Rule {

    public final List<DomainProjection> allFieldReferences;
    public final LabelUse ruleHead;
    /* local */ Rule(LabelUse head, List<DomainProjection> projectedFields){
        this.ruleHead = head;
        this.allFieldReferences = projectedFields;
    }
}
