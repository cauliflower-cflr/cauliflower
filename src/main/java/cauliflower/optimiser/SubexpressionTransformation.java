package cauliflower.optimiser;

import cauliflower.application.CauliflowerException;
import cauliflower.representation.*;
import cauliflower.util.CFLRException;
import cauliflower.util.Logs;
import cauliflower.util.Pair;
import cauliflower.util.Streamer;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * SubexpressionTransformation
 * <p>
 *     Performs three kinds of subexpression optimisations:
 *     <ul>
 *         <li>Chains which are fixed in this cyclic SCC are calculated in an earlier SCC</li>
 *         <li>Chains which occur multiple times in this SCC are put into their own nonterminal</li>
 *         <li>Chains with a high probability of coincidence are moved to their own nonterminal</li>
 *     </ul>
 *     Realistically the first two optimisations do not need a profile, and should be run immediately after reading the spec
 *     The last one uses the profile to work out if a subchain has a good chance of coincidence.
 *
 *     TODO actually we should use the profile to prioritise which subexpressions to hoist
 *
 * Author: nic
 * Date: 14/07/16
 */
public class SubexpressionTransformation implements Transform {

    public static final Comparator<ProblemAnalysis.Binding> bindingOrder = (b1, b2) -> {
        int nc = b1.bound.usedLabel.name.compareTo(b2.bound.usedLabel.name);
        return nc == 0 ? ((Boolean) b1.bindsSource).compareTo(b2.bindsSource) : nc;
    };
    private List<BoundPair> allBoundPairs;

    @Override
    public Optional<Problem> apply(Problem spec, Profile prof) throws CauliflowerException {
        // we are only interested in pairs that are not negating
        allBoundPairs = ProblemAnalysis.getRuleStream(spec)
                .map(ProblemAnalysis::getBindings)
                .flatMap(List::stream)
                .filter(b -> b.boundEndpoints.size() == 2) // we are only interested in pairs
                .filter(b -> b.boundEndpoints.stream().noneMatch(e -> e.bindsNegation)) // that are not negated
                // TODO Conservatively only one of the labels can be fielded, practically their fields just cant bind each other
                .map(BoundPair::new)
                .collect(Collectors.toList());
        Logs.forClass(this.getClass()).trace("Bound pairs:{}", allBoundPairs);
        return new Transform.Group(false, // only do at most one optimisation
                new TerminalChain(), // termi-chains first because they reduce the number of cases in redundant
                new RedundantChain(), // redundancies second because we don't want to accidentally break some options for terminal-chains
                new SummarisingChain()) // this occurs last because this optimisation is temperamental, so only do it if there is nothing better
                .apply(spec, prof);
    }

    private class BoundPair implements Comparable<BoundPair>{
        final LabelUse loLabel, hiLabel;
        final boolean loSource, hiSource;
        public BoundPair(ProblemAnalysis.Bound bound){
            this(bound.boundEndpoints.stream().min(bindingOrder).get(), bound.boundEndpoints.stream().max(bindingOrder).get());
        }
        public BoundPair(ProblemAnalysis.Binding lower, ProblemAnalysis.Binding higher){
            loLabel = lower.bound;
            loSource = lower.bindsSource;
            hiLabel = higher.bound;
            hiSource = higher.bindsSource;
        }

        public boolean involvesDirectly(Clause cl){
            Boolean ret = new Clause.VisitorBase<Boolean>(){
                @Override
                public Boolean visitReverse(Clause.Reverse cl) {
                    return visit(cl.sub);
                }
                @Override
                public Boolean visitLabelUse(LabelUse cl) {
                    return cl == loLabel || cl == hiLabel;
                }
            }.visit(cl);
            return (ret == null) ? false : ret;
        }

        @Override
        public String toString() {
            return String.format("%s(%b)<->%s(%b)", loLabel, loSource, hiLabel, hiSource);
        }

        public String getNonterminalName() {
            return subexpressionName(loLabel.usedLabel.name, loSource, hiLabel.usedLabel.name, hiSource);
        }

        public String getNonterminalSourceDomain(){
            return (loSource ? loLabel.usedLabel.dstDomain : loLabel.usedLabel.srcDomain).name;
        }

        public String getNonterminalSinkDomain(){
            return (hiSource ? hiLabel.usedLabel.dstDomain : hiLabel.usedLabel.srcDomain).name;
        }

        public List<String> getNonterminalFieldDomains(){
            return Stream.concat(loLabel.usedLabel.fieldDomains.stream(), hiLabel.usedLabel.fieldDomains.stream()).map(d -> d.name).collect(Collectors.toList());
        }

        public Rule toRule(Rule.RuleBuilder r) throws CFLRException{
            BiFunction<Integer, Stream<?>, List<String>> conv = (offset, strm) -> Streamer.enumerate(strm, (f, i) -> "f" + (i+offset)).collect(Collectors.toList());
            BiFunction<LabelUse, Boolean, Clause> toClause = (lu, src) -> src ? new Clause.Reverse(lu) : lu;
            LabelUse head = r.useLabel(getNonterminalName(), 0, conv.apply(0, getNonterminalFieldDomains().stream()));
            LabelUse lo = r.useLabel(loLabel.usedLabel.name, 0, conv.apply(0, loLabel.usedField.stream()));
            LabelUse hi = r.useLabel(hiLabel.usedLabel.name, 0, conv.apply(lo.usedField.size(), hiLabel.usedField.stream()));
            return r.setHead(head)
                    .setBody(new Clause.Compose(toClause.apply(lo, loSource), toClause.apply(hi, !hiSource)))
                    .finish();
        }

        public double estimateJoinRedundancy(Profile prof){
            long pre = loSource ? prof.getRelationSinks(loLabel.usedLabel) : prof.getRelationSources(loLabel.usedLabel);
            long lMid = loSource ? prof.getRelationSources(loLabel.usedLabel) : prof.getRelationSinks(loLabel.usedLabel);
            long hMid = hiSource ? prof.getRelationSources(hiLabel.usedLabel) : prof.getRelationSinks(hiLabel.usedLabel);
            long post = hiSource ? prof.getRelationSinks(hiLabel.usedLabel) : prof.getRelationSources(hiLabel.usedLabel);
            double inner = Math.min(lMid, hMid);
            double outer = Math.max(pre, post);
            return outer == 0 ? 0 : inner/outer;
        }

        @Override
        public int compareTo(BoundPair other) {
            int ret = bindingOrder.compare(new ProblemAnalysis.Binding(this.loLabel, this.loSource, false), new ProblemAnalysis.Binding(other.loLabel, other.loSource, false));
            if (ret == 0) {
                return bindingOrder.compare(new ProblemAnalysis.Binding(this.hiLabel, this.hiSource, false), new ProblemAnalysis.Binding(other.hiLabel, other.hiSource, false));
            }
            return ret;
        }
    }

    private Problem rebuildWithNonterminalInsteadOf(Problem spec, BoundPair chain) {
        /* find all the bound pairs with the same labels
         * this is only safe to do because of the ORDER of optimisations:
         *  - if ab appears in many sccs, and a or b are nonterminals in the lowest one,
         *    they must be terminals in the higher ones, so the terminalChain
         *    optimisation will automatically optimise the redundant (cyclic) part.
         *  - if ab is a redundant op, we can guarantee there is not already
         *    a nonterminal calculating ab, since that would have been caught by
         *    terminalChain
         */
        List<BoundPair> relevantPairs = allBoundPairs.stream().filter(bp ->
                bp.loSource == chain.loSource
                        && bp.hiSource == chain.hiSource
                        && bp.loLabel.usedLabel == chain.loLabel.usedLabel
                        && bp.hiLabel.usedLabel == chain.hiLabel.usedLabel)
                .collect(Collectors.toList());
        Logs.forClass(this.getClass()).trace("Relevant pairs: {}", relevantPairs);
        try {
            String name = chain.getNonterminalName();
            ProblemBuilder bp = new ProblemBuilder().withAllLabels(spec)
                    .withType(name, chain.getNonterminalSourceDomain(), chain.getNonterminalSinkDomain(), chain.getNonterminalFieldDomains());
            chain.toRule(bp.buildRule());
            for(int i=0; i<spec.getNumRules(); i++){
                Rule r = spec.getRule(i);
                Rule.RuleBuilder rbuild = bp.buildRule();
                rbuild.setHead(ProblemBuilder.copyLabelUsage(r.ruleHead, rbuild));
                List<BoundPair> pairsInRule = relevantPairs.stream().filter(rel -> Clause.getUsedLabelsInOrder(r.ruleBody).contains(rel.loLabel)).collect(Collectors.toList());
                Clause start = Clause.toNormalForm(r.ruleBody);
                for(BoundPair pair : pairsInRule) start = removeChain(start, pair, name, rbuild);
                rbuild.setBody(adoptClause(start, rbuild, name)).finish();
            }
            return bp.finalise();
        } catch(CFLRException exc){
            Logs.forClass(this.getClass()).error("UNREACHABLE - {}", exc);
            return null; // this should be unreachable, unless the construction of the problem is broken somehow
        }
    }

    private Clause removeChain(Clause base, BoundPair chain, String stubName, Rule.RuleBuilder forRule){
        /*
         * in normal form, a chain AB can have the forms:
         *  - ((_,A),B)   -> (_,X)
         *  - ((_,-B),-A) -> (_,-X)
         *  - (A,B)       -> X
         *  - (-B,-A)     -> -X
         * All these transformations have the benefit of retaining normal form, so we dont need to
         * call it repeatedly in this method
         */
        return new Clause.Visitor<Clause>(){
            @Override
            public Clause visitCompose(Clause.Compose cl) {
                if(chain.involvesDirectly(cl.right)){
                    Clause rc = cl.right;
                    Clause lc = cl.left;
                    Clause other = null;
                    if(lc instanceof Clause.Compose){
                        other = ((Clause.Compose) cl.left).left;
                        lc = ((Clause.Compose) cl.left).right;
                    }
                    assert(chain.involvesDirectly(lc));
                    //Clause fin = (rc instanceof Clause.Reverse) //TODO
                    try {
                        Clause ret = forRule.useLabel(stubName, Math.max(chain.loLabel.priority, chain.hiLabel.priority), Stream.concat(chain.loLabel.usedField.stream(), chain.hiLabel.usedField.stream()).map(dp -> dp.name).collect(Collectors.toList()));
                        if(Clause.getUsedLabelsInOrder(lc).get(0) == chain.hiLabel){
                            ret = new Clause.Reverse(ret);
                        }
                        if(other != null){
                            ret = new Clause.Compose(other, ret);
                        }
                        return ret;
                    } catch (CFLRException e) {
                        Logs.forClass(this.getClass()).error("Failed to remove chain");
                        return null; // unreachable
                    }
                } else return new Clause.Compose(visit(cl.left), visit(cl.right));
            }
            @Override
            public Clause visitIntersect(Clause.Intersect cl) {
                return new Clause.Intersect(visit(cl.left), visit(cl.right));
            }
            @Override
            public Clause visitReverse(Clause.Reverse cl) {
                return cl; // don't bother recursion since we aren't involved
            }
            @Override
            public Clause visitNegate(Clause.Negate cl) {
                return new Clause.Negate(visit(cl.sub)); // i mean this works...but we dont really handle negation
            }
            @Override
            public Clause visitLabelUse(LabelUse cl) {
                return cl; // we know we are not involved
            }
            @Override
            public Clause visitEpsilon(Clause.Epsilon cl) {
                return cl; // this is not involved
            }
        }.visit(base);
    }

    private Clause adoptClause(Clause base, Rule.RuleBuilder forRule, String stubName) throws CFLRException {
        return new ProblemBuilder.ClauseCopier(forRule){
            @Override
            public Clause visitLabelUse(LabelUse cl) {
                if(cl.usedLabel.name.equals(stubName)) return cl;
                return super.visitLabelUse(cl);
            }
        }.copy(base);
    }

    /**
     * if a chain is fixed in this SCC, move it to an earlier one
     */
    private class TerminalChain implements Transform {
        @Override
        public Optional<Problem> apply(Problem spec, Profile prof) throws CauliflowerException {
            // TODO pick the most useful one, not just any one
            for(BoundPair bp : allBoundPairs)
                if(ProblemAnalysis.isEffectivelyTerminal(spec, bp.loLabel)
                    && ProblemAnalysis.isEffectivelyTerminal(spec, bp.hiLabel)
                    && ProblemAnalysis.ruleIsCyclic(spec, bp.hiLabel.usedInRule)){
                Logs.forClass(this.getClass()).trace("Hoisting: {}", bp);
                return Optional.of(rebuildWithNonterminalInsteadOf(spec, bp));
            }
            return Optional.empty();
        }
    }

    /**
     * if a chain appears multiple times in this scc, make a nonterminal for it
     */
    private class RedundantChain implements Transform {
        @Override
        public Optional<Problem> apply(Problem spec, Profile prof) throws CauliflowerException {
            return allBoundPairs.stream()
                    .map(BoundPair::getNonterminalName)
                    .collect(Collectors.groupingBy(s->s, Collectors.counting()))
                    .entrySet().stream()
                    .filter(e -> e.getValue() > 1)
                    // TODO pick the most useful one, not just any one
                    .findAny()
                    .map(s -> allBoundPairs.stream().filter(abp -> abp.getNonterminalName().equals(s.getKey())).findAny())
                    .orElse(Optional.empty())
                    .map(bp -> rebuildWithNonterminalInsteadOf(spec, bp));
        }
    }

    /**
     * If a chain has a high degree of redundancy, make a nonterminal for it
     */
    private class SummarisingChain implements Transform {
        @Override
        public Optional<Problem> apply(Problem spec, Profile prof) throws CauliflowerException {
            return allBoundPairs.stream()
                    .filter(bp -> Clause.getUsedLabelsInOrder(bp.loLabel.usedInRule.ruleBody).size() > 2)
                    .map(bp -> new Pair<>(bp, bp.estimateJoinRedundancy(prof)))
                    .filter(p -> p.second > 1.5) // arbitrary cutoff
                    .max((p1, p2) -> p2.second.compareTo(p1.second)) // sort descending (i.e. highest redundancy factor first
                    .map(p -> rebuildWithNonterminalInsteadOf(spec, p.first));
        }
    }

    public static String subexpressionName(String ln, boolean ls, String hn, boolean hs){
        return String.format("%s%s_%s%s", ln, ls?"S":"T", hn, hs?"S":"T");
    }

}
