package cauliflower.representation;

import java.util.ArrayList;
import java.util.List;

/**
 * ProblemAnalysis
 * <p>
 * Author: nic
 * Date: 8/07/16
 */
public class ProblemAnalysis {

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

    /**
     * Sorts the usages so that highest priority comes FIRST, and preserves left-to-right order.
     * This is bubble sort for the time being because i'm lazy and collections.sort does not say
     * that it preserves original order amongst ties.
     */
    public static List<LabelUse> getEvaluationOrder(List<LabelUse> leftToRightOrder){
        ArrayList<LabelUse> ret = new ArrayList<>(leftToRightOrder);
        for(int end=1; end<ret.size(); end++){
            for(int swp=0; swp<ret.size()-end-1; swp++){
                if(ret.get(swp).priority < ret.get(swp+1).priority){
                    LabelUse tmp = ret.get(swp+1);
                    ret.set(swp+1, ret.get(swp));
                    ret.set(swp, tmp);
                }
            }
        }
        return ret;
    }
}