package cauliflower.representation;

import cauliflower.util.CFLRException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ProblemBuilder
 * <p>
 * Author: nic
 * Date: 15/07/16
 */
public class ProblemBuilder {

    private Problem internal = new Problem();

    public ProblemBuilder withType(String name, String sourceDomain, String sinkDomain, List<String> fieldDomains) throws CFLRException{
        if(!internal.vertexDomains.has(sourceDomain)) internal.addVertexDomain(sourceDomain);
        if(!internal.vertexDomains.has(sinkDomain)) internal.addVertexDomain(sinkDomain);
        for(String s : fieldDomains) if(!internal.fieldDomains.has(s)) internal.addFieldDomain(s);
        internal.addLabel(name, sourceDomain, sinkDomain, fieldDomains);
        return this;
    }

    public ProblemBuilder withLabel(Label lbl) throws CFLRException{
        return withType(lbl.name, lbl.srcDomain.name, lbl.dstDomain.name, lbl.fieldDomains.stream().map(d -> d.name).collect(Collectors.toList()));
    }

    public ProblemBuilder withAllLabels(Problem other) throws CFLRException {
        ProblemBuilder ret = this;
        for(int i=0; i<other.labels.size(); i++){ // so old fashioned
            ret = ret.withLabel(other.labels.get(i));
        }
        return ret;
    }

    public ProblemBuilder withRule(Rule other) throws CFLRException {
        Rule.RuleBuilder r = buildRule();
        r.setHead(copyLabelUsage(other.ruleHead, r)).setBody(new ClauseCopier(r).copy(other.ruleBody)).finish();
        return this;
    }

    public static LabelUse copyLabelUsage(LabelUse other, Rule.RuleBuilder forThisRule) throws CFLRException{
        return forThisRule.useLabel(other.usedLabel.name, other.priority, other.usedField.stream().map(dp -> dp.name).collect(Collectors.toList()));
    }

    public static class ClauseCopier implements Clause.Visitor<Clause>{
        private CFLRException failure;
        private final Rule.RuleBuilder forThisRule;
        public ClauseCopier(Rule.RuleBuilder forRule) {
            this.forThisRule = forRule;
        }
        public Clause copy(Clause copy) throws CFLRException{
            failure = null;
            Clause ret = this.visit(copy);
            if(failure != null) throw failure;
            return ret;
        }
        @Override
        public Clause visitCompose(Clause.Compose cl) {
            return new Clause.Compose(visit(cl.left), visit(cl.right));
        }
        @Override
        public Clause visitIntersect(Clause.Intersect cl) {
            return new Clause.Intersect(visit(cl.left), visit(cl.right));
        }
        @Override
        public Clause visitReverse(Clause.Reverse cl) {
            return new Clause.Reverse(visit(cl.sub));
        }
        @Override
        public Clause visitNegate(Clause.Negate cl) {
            return new Clause.Negate(visit(cl.sub));
        }
        @Override
        public Clause visitLabelUse(LabelUse cl) {
            try {
                return copyLabelUsage(cl, forThisRule);
            } catch (CFLRException e) {
                failure = e;
                return null;
            }
        }
        @Override
        public Clause visitEpsilon(Clause.Epsilon cl) {
            return new Clause.Epsilon();
        }
    }

    public Rule.RuleBuilder buildRule(){
        return new Rule.RuleBuilder(internal);
    }

    public Problem finalise(){
        Problem ret = internal;
        internal = null;
        return ret;
    }
}
