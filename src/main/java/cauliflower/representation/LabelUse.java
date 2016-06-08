package cauliflower.representation;

import java.util.List;

/**
 * Records the occurrence of a label in a rule
 */
public class LabelUse extends Clause{

    public final Label usedLabel;
    public final List<DomainProjection> usedField;
    public Rule usedInRule;

    /* local */ LabelUse(Label label, List<DomainProjection> fields){
        super(ClauseType.LABEL);
        this.usedLabel = label;
        this.usedField = fields;
        this.usedInRule = null;
        usedLabel.usages.add(this);
    }

    public String toString(){
        return String.format("Lu(%s : %s)", usedLabel, usedField);
    }
}
