package cauliflower.optimiser;

import cauliflower.representation.*;
import cauliflower.util.Pair;
import cauliflower.util.Streamer;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * EvaluationOrderTransformation: reorders the highest-level compositions/reversals for a rule
 * <p>
 * Author: nic
 * Date: 8/07/16
 */
public class EvaluationOrderTransformation implements Transform{

    public static final int MAX_PERMUTATIONS=2*3*4*5*6*7*8; // i.e. 8 factorial

    boolean forAllRules;
    boolean exhaustiveMode;

    public EvaluationOrderTransformation(boolean allRules, boolean exhaustive){
        this.forAllRules = allRules;
        this.exhaustiveMode = exhaustive;
    }

    @Override
    public Optional<Problem> apply(Problem spec, Profile prof) {
        spec.vertexDomains.stream().forEach(d -> System.out.printf("%s, %d\n", d.name, prof.getVertexDomainSize(d)));
        spec.labels.stream().forEach(l -> System.out.println(String.format("%s - %d [%d  %d]", l.name, prof.getRelationSize(l), prof.getRelationSources(l), prof.getRelationSinks(l))));
        List<Rule> rulePriority = IntStream.range(0, spec.getNumRules())
                .mapToObj(spec::getRule)
                .map(r -> new Pair<>(r, ruleWeight(r, prof)))
                .sorted((p1, p2) -> p2.second.compareTo(p1.second))
                .map(p -> p.first)
                .collect(Collectors.toList());
        rulePriority.forEach(r ->{
            System.out.println(r);
            List<ProblemAnalysis.Bound> bindings = ProblemAnalysis.getBindings(r);

            List<LabelUse> bodyUses = Clause.getUsedLabelsInOrder(r.ruleBody);
            int cur = 1;
            for(int i=2; i<=bodyUses.size() && cur < MAX_PERMUTATIONS; i++) cur *= i;
            cur = Math.min(cur, MAX_PERMUTATIONS);
            IntStream.range(0, cur)
                    .parallel()
                    .mapToObj(i -> Streamer.permuteIndices(i, bodyUses.size()))
                    .map(lst -> new RuleCostEstimation(prof, bodyUses, lst, bindings))
                    .sequential()
                    .sorted()
                    .forEach(System.out::println);

            //EvaluationOrderTransformation.enumerateOrders(r).forEach(c ->{
            //    System.out.println(" -> " + new Clause.ClauseString().visit(c));
            //});
        });
        return Optional.empty();
    }

    private Integer ruleWeight(Rule r, Profile prof){
        Clause.InOrderVisitor<Integer> iov = new Clause.InOrderVisitor<>(new Clause.VisitorBase<Integer>(){
            @Override
            public Integer visitLabelUse(LabelUse lu){
                return prof.getDeltaExpansionTime(lu);
            }
        });
        iov.visit(r.ruleBody);
        return iov.visits.stream().filter(i -> i != null).mapToInt(Integer::intValue).sum();
    }
//
//    public static Stream<Clause> enumerateOrders(Rule r){
//        Clause base = new CanonicalClauseMaker(false).visit(r.ruleBody);
//        ChainFlattener ctb = new ChainFlattener();
//        ctb.visit(base);
//        return ctb.permuteRecover();
//    }
//
//    public static class ChainFlattener implements Clause.Visitor<Void> {
//
//        List<Pair<Clause, ChainFlattener>> subs =  new ArrayList<>();
//
//        public Stream<Clause> permuteRecover(){
//            int cur=1;
//            for(int i=1; i<=subs.size() && cur < MAX_PERMUTATIONS; i++) cur *= i;
//            return IntStream.range(0, Math.min(cur, MAX_PERMUTATIONS)).parallel().mapToObj(i -> Streamer.permuteIndices(i, subs.size())).map(this::recoverOrdered);
//        }
//
//        public Clause recoverOrdered(List<Integer> ordering){
//            return recover();
//        }
//
//        public Clause recover(){
//            if(subs.size() == 1){
//                return recover(subs.get(0));
//            } else if(subs.size() == 2 && subs.get(0).first == null && subs.get(1).first == null){
//                return new Clause.Intersect(subs.get(0).second.recover(), subs.get(1).second.recover());
//            } else {
//                Clause base = null;
//                for(Pair<Clause, ChainFlattener> itm : subs){
//                    Clause nxt = recover(itm);
//                    base = base == null ? nxt : new Clause.Compose(base, nxt);
//                }
//                return base;
//            }
//        }
//
//        private Clause recover(Pair<Clause, ChainFlattener> itm){
//            if(itm.second == null) return itm.first;
//            else{
//                switch(itm.first.type){
//                    case NEGATE: return new Clause.Negate(itm.second.recover());
//                    case REVERSE: return new Clause.Reverse(itm.second.recover());
//                    default: return itm.second.recover(); // intersection, this case is handled by recover()
//                }
//            }
//        }
//
//        @Override
//        public Void visitCompose(Clause.Compose cl) {
//            visit(cl.left);
//            visit(cl.right);
//            return null;
//        }
//
//        @Override
//        public Void visitIntersect(Clause.Intersect cl) {
//            ChainFlattener ctb = new ChainFlattener();
//            subs.add(new Pair<>(cl, ctb));
//            ChainFlattener lhs = new ChainFlattener();
//            ctb.subs.add(new Pair<>(null, lhs));
//            lhs.visit(cl.left);
//            ChainFlattener rhs = new ChainFlattener();
//            rhs.visit(cl.right);
//            ctb.subs.add(new Pair<>(null, rhs));
//            return null;
//        }
//
//        @Override
//        public Void visitReverse(Clause.Reverse cl) {
//            ChainFlattener ctb = new ChainFlattener();
//            subs.add(new Pair<>(cl, ctb));
//            ctb.visit(cl.sub);
//            return null;
//        }
//
//        @Override
//        public Void visitNegate(Clause.Negate cl) {
//            ChainFlattener ctb = new ChainFlattener();
//            subs.add(new Pair<>(cl, ctb));
//            ctb.visit(cl.sub);
//            return null;
//        }
//
//        @Override
//        public Void visitLabelUse(LabelUse cl) {
//            subs.add(new Pair<>(cl, null));
//            return null;
//        }
//
//        @Override
//        public Void visitEpsilon(Clause.Epsilon cl) {
//            subs.add(new Pair<>(cl, null));
//            return null;
//        }
//    }
//
//    public static class CanonicalClauseMaker implements Clause.Visitor<Clause> {
//
//        final boolean reversing;
//
//        public CanonicalClauseMaker(Boolean rev){
//            this.reversing = rev;
//        }
//
//        @Override
//        public Clause visitCompose(Clause.Compose cl) {
//            CanonicalClauseMaker ccm = new CanonicalClauseMaker(reversing);
//            return reversing ? new Clause.Compose(ccm.visit(cl.right), ccm.visit(cl.left)) : new Clause.Compose(ccm.visit(cl.left), ccm.visit(cl.right));
//        }
//
//        @Override
//        public Clause visitReverse(Clause.Reverse cl) {
//            return new CanonicalClauseMaker(!reversing).visit(cl.sub);
//        }
//
//        @Override
//        public Clause visitIntersect(Clause.Intersect cl) {
//            CanonicalClauseMaker ccm = new CanonicalClauseMaker(reversing);
//            return new Clause.Intersect(ccm.visit(cl.left), ccm.visit(cl.right));
//        }
//
//        @Override
//        public Clause visitNegate(Clause.Negate cl) {
//            Clause ret = new Clause.Negate(new CanonicalClauseMaker(false).visit(cl.sub));
//            return reversing ? new Clause.Reverse(ret) : ret;
//        }
//
//        @Override
//        public Clause visitLabelUse(LabelUse cl) {
//            return reversing ? new Clause.Reverse(cl) : cl;
//        }
//
//        @Override
//        public Clause visitEpsilon(Clause.Epsilon cl) {
//            return reversing ? new Clause.Reverse(cl) : cl;
//        }
//    }
}
