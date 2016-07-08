package cauliflower.representation;

import java.util.List;

/**
 * Records the occurrence of a label in a rule
 */
public class LabelUse extends Clause{

    public final Label usedLabel;
    public final int usageIndex;
    public final List<DomainProjection> usedField;
    public Rule usedInRule;

    /* local */ LabelUse(Label label, List<DomainProjection> fields){
        super(ClauseType.LABEL);
        this.usedLabel = label;
        this.usageIndex = usedLabel.usages.size();
        this.usedField = fields;
        this.usedInRule = null;
        usedLabel.usages.add(this);
    }

    public String toString(){
        return String.format("%s%s(%d)", usedLabel.name, usedField, usageIndex);
    }
}
