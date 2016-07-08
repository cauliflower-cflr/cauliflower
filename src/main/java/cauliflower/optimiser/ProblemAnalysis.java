package cauliflower.optimiser;

import cauliflower.representation.Clause;
import cauliflower.representation.LabelUse;
import cauliflower.representation.Rule;

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
    }

    /**
     * determine the variable bindings for a given rule
     * @param r the rule to get the bindings for
     * @return a list of bindings
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
            throw new RuntimeException("Epsilon is not handled"); // TODO
        }
    }
}
