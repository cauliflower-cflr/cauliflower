package cauliflower.optimiser;

import cauliflower.representation.Clause;
import cauliflower.representation.LabelUse;
import cauliflower.representation.Rule;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * RuleOrderer: reorders the highest-level compositions/reversals for a rule
 * <p>
 * Author: nic
 * Date: 8/07/16
 */
public class RuleOrderer {

    public static Stream<Clause> enumerateOrders(Rule r){
        //CanonicalClause base = new CanonicalClause(false).visit(r.ruleBody);
        return new ClauseOrderEnumerator(false).visit(r.ruleBody);
    }

    private static Stream<Clause.Compose> crossCompose(Stream<Clause> al, Stream<Clause> bl){
        List<Clause> tmp = bl.collect(Collectors.toList());
        return al.flatMap(a -> tmp.stream().map(b -> new Clause.Compose(a, b)));
    }

    public interface CanonicalClause{
        Clause get();
    }
    public static class UnitClause implements CanonicalClause {
        public Clause unit;
        @Override
        public Clause get() {
            return unit;
        }
    }
    public static class ChainClause implements  CanonicalClause {
        public List<CanonicalClause> list;
        @Override
        public Clause get() {
            return null; // TODO
        }
    }

    private static class ClauseOrderEnumerator implements Clause.Visitor<Stream<Clause>> {

        final boolean shouldSwap;

        public ClauseOrderEnumerator(boolean shouldReverse){
            this.shouldSwap = shouldReverse;
        }

        @Override
        public Stream<Clause> visitCompose(Clause.Compose cl) {
            ClauseOrderEnumerator forwards = new ClauseOrderEnumerator(shouldSwap);
            ClauseOrderEnumerator backwards = new ClauseOrderEnumerator(!shouldSwap);
            return Stream.concat(
                    crossCompose(forwards.visit(cl.left), forwards.visit(cl.right)),
                    crossCompose(backwards.visit(cl.right), backwards.visit(cl.left)).map(Clause.Reverse::new));
        }

        @Override
        public Stream<Clause> visitReverse(Clause.Reverse cl) {
            return new ClauseOrderEnumerator(!shouldSwap).visit(cl.sub);
        }

        @Override
        public Stream<Clause> visitIntersect(Clause.Intersect cl) {
            return Stream.of(cl).map(c -> shouldSwap ? new Clause.Reverse(c) : c);
        }

        @Override
        public Stream<Clause> visitNegate(Clause.Negate cl) {
            return Stream.of(cl).map(c -> shouldSwap ? new Clause.Reverse(c) : c);
        }

        @Override
        public Stream<Clause> visitLabelUse(LabelUse cl) {
            return Stream.of(cl).map(c -> shouldSwap ? new Clause.Reverse(c) : c);
        }

        @Override
        public Stream<Clause> visitEpsilon(Clause.Epsilon cl) {
            return Stream.of(cl).map(c -> shouldSwap ? new Clause.Reverse(c) : c);
        }
    }
}
