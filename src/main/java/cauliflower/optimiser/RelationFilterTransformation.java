package cauliflower.optimiser;

import cauliflower.application.CauliflowerException;
import cauliflower.application.Info;
import cauliflower.representation.*;
import cauliflower.util.CFLRException;
import cauliflower.util.Logs;
import cauliflower.util.Pair;
import cauliflower.util.Streamer;

import java.util.*;
import java.util.function.Function;
import java.util.function.ToDoubleBiFunction;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * RelationFilterTransformation
 *
 * Identifies when large relations are accessed multiple times in a cyclic component. In
 * which case a new nonterminal is made which is the restriction of the larger relation
 * to the endpoints that are necessary
 *
 *  - Must be joining a large nonterminal with something that is effectively a terminal
 *
 *  A large relation is updated in a cycle, consider all of its usages within that cycle (it is effectively a terminal in latter cycles)
 *  for some subset of its uses, where all those endpoints join effective terminals (i.e. the source of A always joins a certain thing, or the sink)
 *  Create a self-loop for the terminoid, then join the large relation with it to create the filter, uses of the large relation should subsequently
 *  go via the filter first.
 *
 * Author: nic
 * Date: 14/07/16
 */
public class RelationFilterTransformation implements Transform {

    // TODO decide if we are propagating fields from limiter to filter
    // TODO allow filtering relations which have fields
    private final boolean allowsSingletonFilters = Info.optAllowSingletonFilters; // needs some more advanced profiling to decide if this is smart

    public static final double MINIMUM_BENEFIT = 1.5;

    private Problem spec;
    private Map<Rule, ProblemAnalysis.Bounds> bindings;
    private Profile prof;

    @Override
    public Optional<Problem> apply(Problem spe, Profile pro) throws CauliflowerException {
        init(spe, pro);
        return spec.labels.stream()
                .map(l -> new Pair<>(l, l.usages.stream().mapToLong(prof::getDeltaExpansionTime).sum()))
                .sorted(Pair::InverseSecondaryOrder)
                .map(Pair::getFirst)
                .map(l -> l.usages.stream()
                        .filter(lu -> lu.usedField.size() == 0) // must have no fields
                        .filter(lu -> !ProblemAnalysis.isEffectivelyTerminal(spec, lu)) // and be a nonterminal
                        .filter(lu -> ProblemAnalysis.ruleIsCyclic(spec, lu.usedInRule)) // and in a cyclic rule
                        .filter(lu -> lu.usedInRule.ruleHead != lu) // and not the head
                        .filter(lu -> bindsWithTerminal(lu, true) || bindsWithTerminal(lu, false)) // and binds to at least one terminal
                        .collect(Collectors.toList()))
                .flatMap(Streamer::choices)
                .filter(l -> !l.isEmpty())
                .filter(l -> allowsSingletonFilters || l.size() > 1)
                .filter(l -> l.stream().allMatch(lu -> bindsWithTerminal(lu, true)) || l.stream().allMatch(lu -> bindsWithTerminal(lu, false)))
                .map(l -> new Pair<>(l, benefitOfFilter(l)))
                .max(Pair::secondaryOrder)
                .filter(p -> p.second > MINIMUM_BENEFIT)
                .map(Pair::getFirst)
                .map(this::filterThese)
                .filter(p -> p!=null);
    }

    private void init(Problem speci, Profile profil){
        this.spec = speci;
        this.prof = profil;
        this.bindings = ProblemAnalysis.getRuleStream(spec).collect(Collectors.toMap(r -> r, ProblemAnalysis::getBindings));
    }

    /**
     * higher is better
     */
    private double benefitOfFilter(List<LabelUse> filt){
        Label filteredLabel = filt.get(0).usedLabel;
        List<Pair<List<ProblemAnalysis.Binding>, List<ProblemAnalysis.Binding>>> limiters = disjunctionOfConjunctionsOfLimiters(filt).collect(Collectors.toList());
        ToDoubleBiFunction<ProblemAnalysis.Binding, Boolean> factorizer = (b, src) -> {
            // optimistically returns the expected fraction of the side that is covered by the bound filter
            long size = b.bindsSource ? prof.getRelationSources(b.bound.usedLabel) : prof.getRelationSinks(b.bound.usedLabel);
            return Math.min(1.0, size/(double)(src ? prof.getRelationSources(filteredLabel) : prof.getRelationSinks(filteredLabel)));
        };
        double[] srcSink = limiters.stream()
                // the running fraction, i.e. an optimistic best case assuming perfect distribution of the endpoints
                // smaller means a more effective filter
                .map(conj -> new Pair<>(
                        conj.first.stream().mapToDouble(b -> factorizer.applyAsDouble(b, true)).reduce(1.0, (d1, d2) -> d1*d2),
                        conj.second.stream().mapToDouble(b -> factorizer.applyAsDouble(b, false)).reduce(1.0, (d1, d2) -> d1*d2)))
                //also optimistically assumes that unioning the filters only brings the covered fraction a fraction-th closer to full
                .reduce(new double[]{0.0, 0.0},
                        (co, p) -> new double[]{learnTowardsUnit(co[0], p.first), learnTowardsUnit(co[0], p.first)},
                        (co1, co2) -> new double[]{learnTowardsUnit(co1[0], co2[0]), learnTowardsUnit(co1[1], co2[1])});
        // most optimistically of all, assume the src/sink binders shrink each other perfectly
        return 1.0/(srcSink[0]*srcSink[1]);
    }

    public static double learnTowardsUnit(double current, double next){
        return current + (1.0 - current)*next;
    }

    /**
     * true if AT LEAST ONE of the things that bind here is effectively a terminal
     */
    private boolean bindsWithTerminal(LabelUse lu, boolean sourceBinding){
        return bindings.get(lu.usedInRule)
                .find(lu, sourceBinding)
                .map(b -> b.boundEndpoints.stream()
                        .anyMatch(bnd -> ProblemAnalysis.isEffectivelyTerminal(spec, bnd.bound)))
                .orElse(false);
    }

    /**
     * @param filtered The label uses we want to filter for
     * @return pairs (src, sink) of lists of limiters, guaranteeing that all the src lists are nonempty or all are empty, same for the sink lists
     */
    private Stream<Pair<List<ProblemAnalysis.Binding>, List<ProblemAnalysis.Binding>>> disjunctionOfConjunctionsOfLimiters(List<LabelUse> filtered){
        boolean srcs = filtered.stream().allMatch(lu -> bindsWithTerminal(lu, true));
        boolean snks = filtered.stream().allMatch(lu -> bindsWithTerminal(lu, false));
        Function<Boolean, Stream<List<ProblemAnalysis.Binding>>> sideLimiters = side -> filtered.stream()
                .map(lu -> bindings.get(lu.usedInRule).find(lu, side).get())
                .map(bnd -> bnd.boundEndpoints.stream()
                        .filter(bep -> ProblemAnalysis.isEffectivelyTerminal(spec, bep.bound))
                        .collect(Collectors.toList()));
        return Streamer.zip(
                sideLimiters.apply(true).map(l -> srcs ? l : l.stream().limit(0).collect(Collectors.toList())),
                sideLimiters.apply(false).map(l -> snks ? l : l.stream().limit(0).collect(Collectors.toList())),
                Pair::new);

    }

    private Problem filterThese(List<LabelUse> filtered) {
        try {
            Logs.forClass(this.getClass()).debug("Filtering {}", filtered);
            List<Pair<List<ProblemAnalysis.Binding>, List<ProblemAnalysis.Binding>>> limiters = disjunctionOfConjunctionsOfLimiters(filtered).collect(Collectors.toList());
            Label usedL = filtered.get(0).usedLabel;
            String baseName = usedL.name + filtered.stream().map(lu -> "_" + lu.usageIndex).collect(Collectors.joining());
            String filterName = "filter_" + baseName;
            ProblemBuilder ret = new ProblemBuilder()
                    .withAllLabels(spec)
                    .withType(filterName, usedL.srcDomain.name, usedL.dstDomain.name, Collections.emptyList());
            Rule.RuleBuilder filterBuilder = ret.buildRule();
            filterBuilder.setHead(filterBuilder.useLabel(filterName, 0, Collections.emptyList())); // fields in filters not currently supported

            Clause lBody = getFilterSideBody(limiters.stream().map(Pair::getFirst).collect(Collectors.toList()), "limiter_S_" + baseName, usedL.srcDomain.name, filterBuilder, ret);
            Clause rBody = getFilterSideBody(limiters.stream().map(Pair::getSecond).collect(Collectors.toList()), "limiter_T_" + baseName, usedL.dstDomain.name, filterBuilder, ret);
            Clause body = filterBuilder.useLabel(usedL.name, -1, Collections.emptyList()); // give the large filtered relation a very low priority
            if(lBody != null) body = new Clause.Compose(lBody, body);
            if(rBody != null) body = new Clause.Compose(body, rBody);
            filterBuilder.setBody(body).finish();

            for(int ri=0; ri<spec.getNumRules(); ri++){
                Rule.RuleBuilder r = ret.buildRule();
                r.setHead(ProblemBuilder.copyLabelUsage(spec.getRule(ri).ruleHead, r));
                r.setBody(new ProblemBuilder.ClauseCopier(r){
                    @Override
                    public Clause visitLabelUse(LabelUse cl) {
                        for(LabelUse lu : filtered) if(lu == cl){
                            try {
                                return forThisRule.useLabel(filterName, cl.priority, cl.usedField.stream().map(d -> d.name).collect(Collectors.toList()));
                            } catch (CFLRException e) {
                                failure = e;
                                return null;
                            }
                        }
                        return super.visitLabelUse(cl);
                    }
                }.copy(spec.getRule(ri).ruleBody)).finish();
            }

            return ret.finalise();
        } catch(CFLRException exc){
            Logs.forClass(this.getClass()).error("Exception occurred: ", exc);
            return null;
        }
    }

    /**
     * @return either the single limiter (if there is only one) or adds all the disjunctions to a side-filter
     */
    private Clause getFilterSideBody(List<List<ProblemAnalysis.Binding>> side, String limiterName, String limiterDomain, Rule.RuleBuilder filterRule, ProblemBuilder mainProb) throws CFLRException{
        if(side.stream().anyMatch(List::isEmpty)) return null;
        Clause ret = null;
        if(side.size() > 1){
            mainProb.withType(limiterName, limiterDomain, limiterDomain, Collections.emptyList());
            ret = filterRule.useLabel(limiterName, 0, Collections.emptyList());
        }
        Clause tmp = null;
        for(List<ProblemAnalysis.Binding> limit : side){
            if(side.size() > 1){
                filterRule = mainProb.buildRule();
                filterRule.setHead(filterRule.useLabel(limiterName, 0, Collections.emptyList()));
            }
            tmp = omniLimiter(limit, filterRule);
            if(side.size() > 1){
                filterRule.setBody(tmp).finish();
            }
        }
        if(side.size() > 1){
            return ret;
        } else {
            return tmp;
        }
    }

    /**
     * Creates a large intersection over all the limiters it is given
     */
    private Clause.Intersect omniLimiter(List<ProblemAnalysis.Binding> limiters, Rule.RuleBuilder rb) throws CFLRException{
        // sort the limiters so that the smallest relation is visited first
        limiters.sort(costsLessComparator(b -> b.bindsSource ? prof.getRelationSources(b.bound.usedLabel) : prof.getRelationSinks(b.bound.usedLabel)));
        Clause.Intersect ret = null;
        for(ProblemAnalysis.Binding curBind : limiters){
            //reusing label names is a good idea here, because it ensures that the bindings have matching labels where they actually need to match
            Clause left = rb.useLabel(curBind.bound.usedLabel.name, 0, curBind.bound.usedField.stream().map(d -> d.name).collect(Collectors.toList()));
            Clause right = rb.useLabel(curBind.bound.usedLabel.name, 0, curBind.bound.usedField.stream().map(d -> d.name).collect(Collectors.toList()));
            if(curBind.bindsSource) right = new Clause.Reverse(right);
            else left = new Clause.Reverse(left);
            Clause.Intersect limitation = new Clause.Intersect(new Clause.Compose(left, right), new Clause.Epsilon());
            ret = ret == null ? limitation : new Clause.Intersect(ret, limitation);
        }
        return ret;
    }

    private static <T, V extends Comparable<? super V>> Comparator<T> costsLessComparator(Function<T, V> costFunc){
        return (T a, T b) -> costFunc.apply(a).compareTo(costFunc.apply(b));
    }
}

