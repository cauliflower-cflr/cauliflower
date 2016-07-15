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
        BindingFinder find = new BindingFinder(null, null, false);
        List<Bound> grp = find.visit(r.ruleBody);
        grp.add(find.source);
        grp.add(find.sink);
        return grp;
    }

    private static class BindingFinder implements Clause.Visitor<List<Bound>>{

        Bound source;
        Bound sink;
        boolean anti;

        public BindingFinder(Bound src, Bound snk, boolean negative){
            this.source = src;
            this.sink = snk;
            this.anti = negative;
        }

        @Override
        public List<Bound> visitCompose(Clause.Compose cl) {
            BindingFinder left = new BindingFinder(source, null, anti);
            List<Bound> ret = left.visit(cl.left);
            BindingFinder right = new BindingFinder(left.sink, sink, anti);
            ret.addAll(right.visit(cl.right));
            source = left.source;
            sink = right.sink;
            ret.add(left.sink);
            return ret;
        }

        @Override
        public List<Bound> visitIntersect(Clause.Intersect cl) {
            BindingFinder left = new BindingFinder(source, sink, anti);
            List<Bound> ret = left.visit(cl.left);
            source = left.source;
            sink = left.sink;
            BindingFinder right = new BindingFinder(source, sink, anti);
            ret.addAll(right.visit(cl.right));
            return ret;
        }

        @Override
        public List<Bound> visitReverse(Clause.Reverse cl) {
            BindingFinder sub = new BindingFinder(sink, source, anti);
            List<Bound> ret = sub.visit(cl.sub);
            source = sub.sink;
            sink = sub.source;
            return ret;
        }

        @Override
        public List<Bound> visitNegate(Clause.Negate cl) {
            BindingFinder sub = new BindingFinder(source, sink, !anti);
            List<Bound> ret = sub.visit(cl.sub);
            source = sub.source;
            sink = sub.sink;
            return ret;
        }

        @Override
        public List<Bound> visitLabelUse(LabelUse cl) {
            if(source == null) source = new Bound();
            if(sink == null) sink = new Bound();
            source.boundEndpoints.add(new Binding(cl, true, anti));
            sink.boundEndpoints.add(new Binding(cl, false, anti));
            return new ArrayList<>();
        }

        @Override
        public List<Bound> visitEpsilon(Clause.Epsilon cl) {
            throw new RuntimeException("Epsilon is not handled"); // TODO merge the source and sink sets (somehow?)
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
