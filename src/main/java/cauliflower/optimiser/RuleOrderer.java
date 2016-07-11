package cauliflower.optimiser;

import cauliflower.representation.Clause;
import cauliflower.representation.LabelUse;
import cauliflower.representation.Rule;
import cauliflower.util.Pair;
import cauliflower.util.Streamer;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * RuleOrderer: reorders the highest-level compositions/reversals for a rule
 * <p>
 * Author: nic
 * Date: 8/07/16
 */
public class RuleOrderer {

    public static final int MAX_PERMUTATIONS=2*3*4*5*6*7*8; // i.e. 8 factorial

    public static Stream<Clause> enumerateOrders(Rule r){
        Clause base = new CanonicalClauseMaker(false).visit(r.ruleBody);
        ChainFlattener ctb = new ChainFlattener();
        ctb.visit(base);
        return ctb.permuteRecover();
    }

    public static class ChainFlattener implements Clause.Visitor<Void> {

        List<Pair<Clause, ChainFlattener>> subs =  new ArrayList<>();

        public Stream<Clause> permuteRecover(){
            int cur=1;
            for(int i=1; i<=subs.size() && cur < MAX_PERMUTATIONS; i++) cur *= i;
            return IntStream.range(0, Math.min(cur, MAX_PERMUTATIONS)).parallel().mapToObj(i -> Streamer.permuteIndices(i, subs.size())).map(this::recoverOrdered);
        }

        public Clause recoverOrdered(List<Integer> ordering){
            return recover();
        }

        public Clause recover(){
            if(subs.size() == 1){
                return recover(subs.get(0));
            } else if(subs.size() == 2 && subs.get(0).first == null && subs.get(1).first == null){
                return new Clause.Intersect(subs.get(0).second.recover(), subs.get(1).second.recover());
            } else {
                Clause base = null;
                for(Pair<Clause, ChainFlattener> itm : subs){
                    Clause nxt = recover(itm);
                    base = base == null ? nxt : new Clause.Compose(base, nxt);
                }
                return base;
            }
        }

        private Clause recover(Pair<Clause, ChainFlattener> itm){
            if(itm.second == null) return itm.first;
            else{
                switch(itm.first.type){
                    case NEGATE: return new Clause.Negate(itm.second.recover());
                    case REVERSE: return new Clause.Reverse(itm.second.recover());
                    default: return itm.second.recover(); // intersection, this case is handled by recover()
                }
            }
        }

        @Override
        public Void visitCompose(Clause.Compose cl) {
            visit(cl.left);
            visit(cl.right);
            return null;
        }

        @Override
        public Void visitIntersect(Clause.Intersect cl) {
            ChainFlattener ctb = new ChainFlattener();
            subs.add(new Pair<>(cl, ctb));
            ChainFlattener lhs = new ChainFlattener();
            ctb.subs.add(new Pair<>(null, lhs));
            lhs.visit(cl.left);
            ChainFlattener rhs = new ChainFlattener();
            rhs.visit(cl.right);
            ctb.subs.add(new Pair<>(null, rhs));
            return null;
        }

        @Override
        public Void visitReverse(Clause.Reverse cl) {
            ChainFlattener ctb = new ChainFlattener();
            subs.add(new Pair<>(cl, ctb));
            ctb.visit(cl.sub);
            return null;
        }

        @Override
        public Void visitNegate(Clause.Negate cl) {
            ChainFlattener ctb = new ChainFlattener();
            subs.add(new Pair<>(cl, ctb));
            ctb.visit(cl.sub);
            return null;
        }

        @Override
        public Void visitLabelUse(LabelUse cl) {
            subs.add(new Pair<>(cl, null));
            return null;
        }

        @Override
        public Void visitEpsilon(Clause.Epsilon cl) {
            subs.add(new Pair<>(cl, null));
            return null;
        }
    }

    public static class CanonicalClauseMaker implements Clause.Visitor<Clause> {

        final boolean reversing;

        public CanonicalClauseMaker(Boolean rev){
            this.reversing = rev;
        }

        @Override
        public Clause visitCompose(Clause.Compose cl) {
            CanonicalClauseMaker ccm = new CanonicalClauseMaker(reversing);
            return reversing ? new Clause.Compose(ccm.visit(cl.right), ccm.visit(cl.left)) : new Clause.Compose(ccm.visit(cl.left), ccm.visit(cl.right));
        }

        @Override
        public Clause visitReverse(Clause.Reverse cl) {
            return new CanonicalClauseMaker(!reversing).visit(cl.sub);
        }

        @Override
        public Clause visitIntersect(Clause.Intersect cl) {
            CanonicalClauseMaker ccm = new CanonicalClauseMaker(reversing);
            return new Clause.Intersect(ccm.visit(cl.left), ccm.visit(cl.right));
        }

        @Override
        public Clause visitNegate(Clause.Negate cl) {
            Clause ret = new Clause.Negate(new CanonicalClauseMaker(false).visit(cl.sub));
            return reversing ? new Clause.Reverse(ret) : ret;
        }

        @Override
        public Clause visitLabelUse(LabelUse cl) {
            return reversing ? new Clause.Reverse(cl) : cl;
        }

        @Override
        public Clause visitEpsilon(Clause.Epsilon cl) {
            return reversing ? new Clause.Reverse(cl) : cl;
        }
    }
}
