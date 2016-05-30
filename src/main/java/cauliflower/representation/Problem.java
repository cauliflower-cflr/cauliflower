package cauliflower.representation;

import java.util.ArrayList;
import java.util.List;

/**
 * Problem
 * <p>
 * Author: nic
 * Date: 30/05/16
 */
public class Problem {

    public final List<Domain> vertexDomains;
    public final List<Domain> fieldDomains;

    public final List<Label> labels;

    public Problem(){
        this.vertexDomains = new ArrayList<>();
        this.fieldDomains = new ArrayList<>();
        this.labels = new ArrayList<>();
    }

    /**
     * Rules for generating new relations
     */
    public class Rule{
        public final List<DomainProjection> allFieldReferences;
        public final LabelUse ruleHead;
        public Rule(LabelUse head, List<DomainProjection> projectedFields){
            this.ruleHead = head;
            this.allFieldReferences = projectedFields;
        }
        public class LabelUse {
            public final Label usedLabel;
            public final List<DomainProjection> usedField;
            public LabelUse(Label label, List<DomainProjection> fields){
                this.usedLabel = label;
                this.usedField = fields;
            }
            public String toString(){
                return String.format("Lu(%s : %s)", usedLabel, usedField);
            }
        }
        public class DomainProjection {
            public final int index;
            public final String name;
            public final Domain referencedField;
            public DomainProjection(int idx, String nm, Domain field) {
                this.index = idx;
                this.name = nm;
                this.referencedField = field;
            }
            public String toString(){
                return String.format("Dp(%d=%s : %s)", index, name, referencedField.toString());
            }
        }
    }

    /**
     * A single relation which is stored as a terminal/nonterminal in memory
     */
    public class Label {
        public final int index;
        public final String name;
        public final Domain srcDomain;
        public final Domain dstDomain;
        public final int fieldDomainCount;
        public final List<Domain> fieldDomains;
        public Label(int idx, String nm, Domain src, Domain dst, List<Domain> fld){
            this.index = idx;
            this.name = nm;
            this.srcDomain = src;
            this.dstDomain = dst;
            this.fieldDomains = fld;
            this.fieldDomainCount = fieldDomains.size();
        }
    }

    /**
     * Vertex and field domains
     */
    public class Domain {
        public final int index;
        public final String name;
        public Domain(int idx, String nm){
            this.index = idx;
            this.name = nm;
        }
        public String toString(){
            return String.format("D(%d=%s)", index, name);
        }
    }
}
