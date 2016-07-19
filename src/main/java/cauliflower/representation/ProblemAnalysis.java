package cauliflower.representation;

import cauliflower.util.Pair;
import cauliflower.util.Streamer;
import cauliflower.util.TarjanScc;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * ProblemAnalysis
 * <p>
 * Author: nic
 * Date: 8/07/16
 */
public class ProblemAnalysis {

    private Map<Label, Set<Label>> deps;
    private Map<Label, Set<Label>> ideps;
    private List<List<Label>> groups;
    private Map<Label, List<Label>> memberships;

    private static Map<Problem, ProblemAnalysis> analysed = new HashMap<>();

    private ProblemAnalysis(Problem problem){
        deps = ProblemAnalysis.getLabelDependencyGraph(problem);
        ideps = ProblemAnalysis.getInverseLabelDependencyGraph(problem);
        groups = ProblemAnalysis.getStronglyConnectedLabels(deps);
        memberships = groups.stream().flatMap(ls -> ls.stream().map(l -> new Pair<Label, List<Label>>(l, ls))).collect(Collectors.toMap(p -> p.first, p->p.second));
        analysed.put(problem, this);
    }

    private boolean isEffectivelyTerminal(LabelUse lu){
        return !memberships.get(lu.usedInRule.ruleHead.usedLabel).contains(lu.usedLabel);
    }

    private boolean ruleIsCyclic(Rule r){
        return Clause.getUsedLabelsInOrder(r.ruleBody).stream().anyMatch(lu -> memberships.get(r.ruleHead.usedLabel).contains(lu.usedLabel));
    }

    public static boolean isEffectivelyTerminal(Problem prob, LabelUse lu){
        analysed.computeIfAbsent(prob, ProblemAnalysis::new);
        return analysed.get(prob).isEffectivelyTerminal(lu);
    }

    public static boolean ruleIsCyclic(Problem prob, Rule r){
        analysed.computeIfAbsent(prob, ProblemAnalysis::new);
        return analysed.get(prob).ruleIsCyclic(r);
    }

    /**
     * Get the dependency mapping where A maps to B if "A -> ...B..." is a rule
     */
    public static Map<Label, Set<Label>> getLabelDependencyGraph(Problem prob){
        return toCompletedMap(prob.labels.stream(),
                IntStream.range(0, prob.getNumRules())
                        .mapToObj(i -> prob.getRule(i))
                        .flatMap(r -> Clause.getUsedLabelsInOrder(r.ruleBody).stream().distinct().map(lu -> new Pair<>(r.ruleHead.usedLabel, lu.usedLabel))));
    }

    /**
     * Get the inverse dependency mapping where A maps to B if "B -> ...A..." is a rule
     */
    public static Map<Label, Set<Label>> getInverseLabelDependencyGraph(Problem prob){
        return toCompletedMap(prob.labels.stream(),
                IntStream.range(0, prob.getNumRules())
                        .mapToObj(i -> prob.getRule(i))
                        .flatMap(r -> Clause.getUsedLabelsInOrder(r.ruleBody).stream().distinct().map(lu -> new Pair<>(lu.usedLabel, r.ruleHead.usedLabel))));
    }

    private static Map<Label, Set<Label>> toCompletedMap(Stream<Label> allLabels, Stream<Pair<Label, Label>> pairs){
        return Stream.concat(
                allLabels.map(l -> new Pair<Label, Set<Label>>(l, Collections.emptySet())), // DO NOT BELIEVE INTELLIJ, THAT GENERIC NOTATION IS VERY NECESSARY
                pairs.map(p -> new Pair<>(p.first, Collections.singleton(p.second)))
                )
                .collect(Collectors.toMap(
                        p -> p.first,
                        p -> p.second,
                        (a,b) -> Stream.concat(a.stream(), b.stream()).collect(Collectors.toSet()))); // probably very inefficient
    }

    /**
     * Get the strongly connected components for this problem ordered by earliest declared label first
     */
    public static List<List<Label>> getStronglyConnectedLabels(Problem prob) {
        return getStronglyConnectedLabels(getLabelDependencyGraph(prob));
    }

    /**
     * Get the strongly connected components for this label dependency ordered by earliest declared label first
     */
    public static List<List<Label>> getStronglyConnectedLabels(Map<Label, Set<Label>> deps) {
        return TarjanScc.getSCC(deps).stream()
                .map(l -> l.stream()
                        .sorted((l1, l2)->l1.index - l2.index)
                        .collect(Collectors.toList()))
                // it is an error to provide an empty group, so getAsInt is safe to use here
                .sorted((li1, li2) -> li1.stream().mapToInt(l -> l.index).min().getAsInt() - li2.stream().mapToInt(l -> l.index).min().getAsInt())
                .collect(Collectors.toList());
    }

    /**
     * The label's endpoint for a bound variable
     */
    public static class Binding {
        public final LabelUse bound;
        public final boolean bindsSource;
        public final boolean bindsNegation;
        public Binding(LabelUse usage, boolean sourceBinding, boolean antiBinding){
            bound = usage;
            bindsSource = sourceBinding;
            bindsNegation = antiBinding;
        }
    }

    /**
     * The list of all endpoints bound by a specific binding
     */
    public static class Bound {
        public final List<Binding> boundEndpoints = new ArrayList<>();
        public Binding getOrNull(LabelUse lu, boolean source){
            return boundEndpoints.stream().filter(bnd -> bnd.bindsSource == source && bnd.bound == lu).findAny().orElse(null);
        }
        public boolean has(LabelUse lu, boolean source){
            return getOrNull(lu, source) != null;
        }
    }

    /**
     * determine the variable bindings for a given rule
     * @param r the rule to get the bindings for
     * @return a list of bindings, such that the output's source is at size()-2 and the output's sink is at size()-1
     */
    public static List<Bound> getBindings(Rule r){
        BindingFinder find = new BindingFinder(new HashSet<>(), null, null, false);
        Pair<Binding, Binding> outers = find.visit(r.ruleBody);
        return find.bindings.stream()
                //this is a trick to ensure the source and sink nodes are second last and last resp.
                .map(b -> new Pair<>(b, b.boundEndpoints.contains(outers.first) ? 1 : b.boundEndpoints.contains(outers.second) ? 2 : 0))
                .sorted(Pair::secondaryOrder)
                .map(p -> p.first)
                .collect(Collectors.toList());
    }

    private static class BindingFinder implements Clause.Visitor<Pair<Binding, Binding>>{

        final Set<Bound> bindings;
        final Binding source;
        final Binding sink;
        boolean anti;

        public BindingFinder(Set<Bound> bindings, Binding src, Binding snk, boolean negative){
            this.bindings = bindings;
            this.source = src;
            this.sink = snk;
            this.anti = negative;
        }

        @Override
        public Pair<Binding, Binding> visitCompose(Clause.Compose cl) {
            if(anti) throw new RuntimeException("Negation currently only supports singleton clauses");
            Pair<Binding, Binding> l = new BindingFinder(bindings, source, null, false).visit(cl.left);
            Pair<Binding, Binding> r = new BindingFinder(bindings, l.second, sink, false).visit(cl.right);
            return new Pair<>(l.first, r.second);
        }

        @Override
        public Pair<Binding, Binding> visitIntersect(Clause.Intersect cl) {
            if(anti) throw new RuntimeException("Negation currently only supports singleton clauses");
            Pair<Binding, Binding> l = new BindingFinder(bindings, source, sink, false).visit(cl.left);
            return new BindingFinder(bindings, l.first, l.second, false).visit(cl.right);
        }

        @Override
        public Pair<Binding, Binding> visitReverse(Clause.Reverse cl) {
            Pair<Binding, Binding> sub = new BindingFinder(bindings, sink, source, anti).visit(cl.sub);
            return new Pair<>(sub.second, sub.first);
        }

        @Override
        public Pair<Binding, Binding> visitNegate(Clause.Negate cl) {
            return new BindingFinder(bindings, source, sink, !anti).visit(cl.sub);
        }

        @Override
        public Pair<Binding, Binding> visitLabelUse(LabelUse cl) {
            Bound src;
            if(source != null){
                src = bindings.stream().filter(b -> b.has(source.bound, source.bindsSource)).findAny().get();
            } else {
                src = new Bound();
                bindings.add(src);
            }
            Bound snk;
            if(sink != null){
                snk = bindings.stream().filter(b -> b.has(sink.bound, sink.bindsSource)).findAny().get();
            } else {
                snk = new Bound();
                bindings.add(snk);
            }
            Pair<Binding, Binding> ret = new Pair<>(new Binding(cl, true, anti), new Binding(cl, false, anti));
            src.boundEndpoints.add(ret.first);
            snk.boundEndpoints.add(ret.second);
            return ret;
        }

        @Override
        public Pair<Binding, Binding> visitEpsilon(Clause.Epsilon cl) {
            if(source != null && sink != null){
                Bound src = bindings.stream().filter(b -> b.has(source.bound, source.bindsSource)).findAny().get();
                Bound snk = bindings.stream().filter(b -> b.has(sink.bound, sink.bindsSource)).findAny().get();
                src.boundEndpoints.addAll(snk.boundEndpoints);
                bindings.remove(snk);
            }
            if(source == null) {
                return new Pair<>(sink, sink);
            } else {
                return new Pair<>(source, source);
            }
        }
    }

    public static Stream<Rule> getRuleStream(Problem p){
        return IntStream.range(0, p.getNumRules()).mapToObj(p::getRule);
    }

    /**
     * Returns a list of labeluses ordered according to their evaluation priority
     */
    public static List<LabelUse> getEvaluationOrder(List<LabelUse> leftToRightOrder){
        return getEvaluationOrder(leftToRightOrder, leftToRightOrder.stream().map(lu -> lu.priority).collect(Collectors.toList()));
    }
    public static List<LabelUse> getEvaluationOrder(List<LabelUse> leftToRightOrder, List<Integer> priorityOverride){
        return getEvaluationOrderOverride(Streamer.zip(leftToRightOrder.stream(), priorityOverride.stream(), Pair::new).collect(Collectors.toList()));
    }
    private static List<LabelUse> getEvaluationOrderOverride(List<Pair<LabelUse, Integer>> order){
        List<Pair<LabelUse, Integer>> prio = Streamer.enumerate(order.stream(), (p, i) -> new Pair<>(p.first, p.second*order.size() - i)).collect(Collectors.toList());
        Collections.sort(prio, (p1,p2) -> p2.second - p1.second); // had to factor this out of the stream for REASONS UNKNOWN TODO
        return prio.stream().map(p ->p.first).collect(Collectors.toList());
    }
}
