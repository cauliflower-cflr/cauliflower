package cauliflower.representation;

import java.util.List;

/**
 * Records the occurrence of a label in a rule
 */
public class LabelUse {

    public final Label usedLabel;
    public final List<DomainProjection> usedField;

    /* local */ LabelUse(Label label, List<DomainProjection> fields){
        this.usedLabel = label;
        this.usedField = fields;
    }

    public String toString(){
        return String.format("Lu(%s : %s)", usedLabel, usedField);
    }
}
