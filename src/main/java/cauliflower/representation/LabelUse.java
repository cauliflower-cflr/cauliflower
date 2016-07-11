package cauliflower.representation;

import java.util.List;

/**
 * Records the occurrence of a label in a rule
 */
public class LabelUse extends Clause{

    public final Label usedLabel;
    public final int usageIndex;
    public final int priority;
    public final List<DomainProjection> usedField;
    public Rule usedInRule;

    /* local */ LabelUse(Label label, int pri, List<DomainProjection> fields){
        super(ClauseType.LABEL);
        this.usedLabel = label;
        this.priority = pri;
        this.usageIndex = usedLabel.usages.size();
        this.usedField = fields;
        this.usedInRule = null;
        usedLabel.usages.add(this);
    }

    public String toString(){
        return String.format("%s%s(%d)", usedLabel.name, usedField, usageIndex);
    }
}
