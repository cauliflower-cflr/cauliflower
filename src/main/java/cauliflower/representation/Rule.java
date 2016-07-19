package cauliflower.representation;

import cauliflower.util.CFLRException;
import cauliflower.util.Pair;
import cauliflower.util.Streamer;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Rule {

    public final int index;
    public final Piece.Pieces<DomainProjection> allFieldReferences;
    public final LabelUse ruleHead;
    public final Clause ruleBody;

    // private constructor, forcing use of the RuleBuilder
    private Rule(int idx, LabelUse head, Piece.Pieces<DomainProjection> projectedFields, Clause body){
        this.index = idx;
        this.ruleHead = head;
        this.allFieldReferences = projectedFields;
        this.ruleBody = body;
    }

    public static class RuleBuilder{
        private final Problem p;
        private Piece.Pieces<DomainProjection> projections = new Piece.Pieces<>();
        private LabelUse head = null;
        private Clause body = null;

        public RuleBuilder(Problem prob){
            this.p = prob;
        }

        public RuleBuilder setHead(LabelUse lu){
            head = lu;
            return this;
        }

        public RuleBuilder setBody(Clause cl){
            body = cl;
            return this;
        }

        public Rule finish() throws CFLRException {
            Rule ret = new Rule(p.getNumRules(), head, projections, body);
            if(head == null || body == null) throw new CFLRException("Incomplete parsing for rule with Head " + head + " and body " + body);
            p.addRule(ret);
            Clause.Visitor<Void> ruleAdder = new Clause.InOrderVisitor<>(new Clause.VisitorBase<Void>() {
                @Override
                public Void visitLabelUse(LabelUse cl) {
                    cl.usedInRule = ret;
                    return null;
                }
            });
            ruleAdder.visit(ret.ruleHead);
            ruleAdder.visit(ret.ruleBody);
            return ret;
        }

        public LabelUse useLabel(String name, int priority, List<String> fns) throws CFLRException {
            Label base = p.labels.get(name);
            if(base.fieldDomainCount != fns.size()){
                throw new CFLRException(String.format("Cannot use Label %s, declared with %d fields, used with %d", name, base.fieldDomainCount, fns.size()));
            }
            for(Pair<String, Domain> pair : Streamer.zip(fns.stream(), base.fieldDomains.stream(), Pair::new ).collect(Collectors.toList())){
                if(!projections.has(pair.first)) new DomainProjection(projections, pair.first, pair.second);
                else if(projections.get(pair.first).referencedField != pair.second){
                    throw new CFLRException(String.format("Cannot use the same name \"%s\" for different field domains: %s and %s",
                            pair.first, pair.second, projections.get(pair.first).referencedField));
                }
            }
            List<DomainProjection> lblProjs = new ArrayList<>();
            for(String fieldProj : fns) lblProjs.add(projections.get(fieldProj));
            return new LabelUse(p.labels.get(name), priority, lblProjs);
        }
    }

    @Override
    public String toString() {
        return ruleHead.toString() + "->" + ruleBody.toString();
    }

    public String toSpecString(){
        return new Clause.SpecString().visit(ruleHead) + "->" + new Clause.SpecString().visit(ruleBody);
    }
}
