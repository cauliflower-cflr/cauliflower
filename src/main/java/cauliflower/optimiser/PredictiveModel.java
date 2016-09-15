package cauliflower.optimiser;

import cauliflower.representation.LabelUse;
import cauliflower.representation.ProblemAnalysis;

import java.util.*;
import java.util.function.DoubleBinaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Used to predict the cost of a rule evaluation
 */
public class PredictiveModel {

    interface ApproximationFactory <A> {
        A forRelation(LabelUse l);
        A forBackwards(A a);
        A forJoined(A l, A r);
        A forAnnealed(A in, A target, double weight);
        double disconnectionsOnTheLeft(A me, A myLeft);
        double disconnectionsOnTheRight(A me, A myRight);
    }

    private class Approxim {

        private final double srcProp, dstProp;
        private final double srcVol, dstVol;
        private final double size;
        private final Set<LabelUse> members;

        /**
         * get an approximation of a single relation
         */
        public Approxim(Profile prof, LabelUse l){
            size = prof.getRelationSize(l.usedLabel);
            srcVol = prof.getRelationSources(l.usedLabel);
            dstVol = prof.getRelationSinks(l.usedLabel);
            srcProp = srcVol/((double)prof.getVertexDomainSize(l.usedLabel.srcDomain));
            dstProp = dstVol/((double)prof.getVertexDomainSize(l.usedLabel.dstDomain));
            members = Collections.singleton(l);
        }

        /**
         * get an approximation by flipping a known approximation
         */
        public Approxim(Approxim flip){
            size = flip.size;
            srcVol = flip.dstVol;
            dstVol = flip.srcVol;
            srcProp = flip.dstProp;
            dstProp = flip.srcProp;
            members = flip.members;
        }

        /**
         * get an approximation by joining two approximations together
         */
        public Approxim(Approxim l, Approxim r){
            System.out.println(l + " X " + r);
            members = Stream.concat(l.members.stream(), r.members.stream()).collect(Collectors.toSet());
            //size = l.size*r.size*l.dstProp*r.srcProp; // every pair with every pair is only true for the hourglass
            //size = l.size*r.srcProp*(r.size/r.srcVol); // this one's not symmetric
            size = (l.size*r.size)/((l.dstVol/l.dstProp));//*(r.srcVol/r.srcProp)); // this one had better work
            double joinShrinkage = l.dstProp*r.srcProp;
            srcVol = l.srcVol*joinShrinkage;
            dstVol = r.dstVol*joinShrinkage;
            srcProp = l.srcProp*joinShrinkage;
            dstProp = r.dstProp*joinShrinkage;
        }

        /**
         * get an approximation by annealing one approximation towards the target relation
         */
        //public Approxim(Approxim a, Profile prof, LabelUse target, double weight){

        //}

        public double disconnectionsOnMyLeft(Approxim toMyLeft){
            return toMyLeft.size * (1-srcProp);
        }

        public double disconnectionsOnMyRight(Approxim toMyRight){
            return toMyRight.size * (1-dstProp);
        }

        @Override
        public String toString() {
            return String.format("%.1f[%.3f{%.1f}-%.3f{%.1f}]{%s}", size,
                    srcProp,
                    srcVol,
                    dstProp,
                    dstVol,
                    members.stream().map(lu -> lu.usedLabel.name + lu.usageIndex).collect(Collectors.joining()));
        }
    }

    private class CharacteristicApproximator implements ApproximationFactory<Approxim>{

        private final Profile prof;
        private final List<LabelUse> evalOrder;

        public CharacteristicApproximator(Profile p, List<LabelUse> eo){
            this.prof = p;
            this.evalOrder = eo;
        }

        @Override
        public Approxim forRelation(LabelUse l) {
            return null;
        }

        @Override
        public Approxim forBackwards(Approxim approxim) {
            return null;
        }

        @Override
        public Approxim forJoined(Approxim l, Approxim r) {
            return null;
        }

        @Override
        public Approxim forAnnealed(Approxim in, Approxim target, double weight) {
            return null;
        }

        @Override
        public double disconnectionsOnTheLeft(Approxim me, Approxim myLeft) {
            return 0;
        }

        @Override
        public double disconnectionsOnTheRight(Approxim me, Approxim myRight) {
            return 0;
        }
    }

    private class Multispace {

        private List<Approxim> spaces = new ArrayList<>();

//        private Optional<Approxim> removeFromBoundOrEmpty(Optional<ProblemAnalysis.Bound> group, LabelUse otherBound){
//            System.out.println(group);
//            LabelUse target = group.map(bnd -> bnd.boundEndpoints.stream()
//                            .filter(bou -> bou.bound != otherBound)
//                            .findAny())
//                    .orElse(Optional.empty())
//                    .map(bnd -> bnd.bound)
//                    .orElse(null); // "then why are you using optionals!?" <- bad programmer
//            if (target == null) return Optional.empty();
//            Optional<Approxim> ret = spaces.stream().filter(ap -> ap.members.contains(target)).findAny();
//            spaces = spaces.stream().filter(ap -> ret.map(rap -> ap != rap).orElse(true)).collect(Collectors.toList());
//            return ret;
//        }
//
//        public Optional<Approxim> before(LabelUse lu, ProblemAnalysis.Bounds bindings){
//            return removeFromBoundOrEmpty(bindings.find(lu, true), lu);
//        }
//
//        public Optional<Approxim> after(LabelUse lu, ProblemAnalysis.Bounds bindings){
//            return removeFromBoundOrEmpty(bindings.find(lu, false), lu);
//        }

        public Optional<Approxim> removeIfPresent(LabelUse target){
            if(target == null) return Optional.empty();
            Optional<Approxim> ret = spaces.stream().filter(ap -> ap.members.contains(target)).findAny();
            spaces = spaces.stream().filter(ap -> ret.map(rap -> ap != rap).orElse(true)).collect(Collectors.toList());
            return ret;
        }

        public void add(Approxim newApp){
            spaces = Stream.concat(spaces.stream(), Stream.of(newApp)).collect(Collectors.toList());
        }

        public double accumulateSpace(){
            double ret = 1;
            for(Approxim a : spaces) ret *= a.size;
            return ret;
        }

    }

    public double getCost(Profile prof, List<LabelUse> evalOrder, ProblemAnalysis.Bounds bindings){
        //TODO this only works for linear chains, think about how to make it intersection/negation safe
        if (bindings.all.stream().anyMatch(bnd -> bnd.boundEndpoints.size() > 2)) return -1;

        // determine which label-uses in the chain are backwards
        Map<LabelUse, Boolean> appearsBackwards = new HashMap<>();
        Map<LabelUse, LabelUse> lefts = new HashMap<>();
        Map<LabelUse, LabelUse> rights = new HashMap<>();
        ProblemAnalysis.Binding cur = bindings.entry.boundEndpoints.get(0);
        do {
            appearsBackwards.put(cur.bound, !cur.bindsSource);
            // get the next part
            final ProblemAnalysis.Binding in = cur;
            cur = bindings.find(in.bound, !in.bindsSource)
                    .map(bou -> bou.boundEndpoints.stream()
                            .filter(bnd -> bnd.bound != in.bound)
                            .findAny())
                    .orElse(Optional.empty())
                    .orElse(null);
            if(cur != null){
                rights.put(in.bound, cur.bound);
                lefts.put(cur.bound, in.bound);
            }
        } while(cur != null);

        // We need the eval order as a list of left-to-right approximations
        List<Approxim> disjoint = new ArrayList<>(evalOrder.size());
        for(LabelUse lu : evalOrder){
            Approxim alu = new Approxim(prof, lu);
            if(appearsBackwards.get(lu)) alu = new Approxim(alu);
            disjoint.add(alu);
        }
        System.out.println(disjoint);

        double totalCost = 0;
        Multispace msp = new Multispace();
        for(Approxim apx : disjoint){
            LabelUse lu = apx.members.stream().findFirst().get();
            Approxim left = msp.removeIfPresent(lefts.get(lu)).orElse(null);
            Approxim right = msp.removeIfPresent(rights.get(lu)).orElse(null);

            double cost = 0;
            double extraneousSpace = msp.accumulateSpace();
            if(left == null){
                if(right == null){
                    //accumulate no costs, just grow the space
                    msp.add(apx);
                } else {
                    //right only, count the disconnections to the right
                    cost = apx.disconnectionsOnMyRight(right);
                    msp.add(new Approxim(apx, right));
                }
            } else {
                if(right == null){
                    // left only, flip the right case
                    cost = apx.disconnectionsOnMyLeft(left);
                    msp.add(new Approxim(left, apx));
                } else {
                    // both sides, need to check the mathematics because this is simpler than what i though it ought to be
                    cost = apx.disconnectionsOnMyLeft(left) + apx.disconnectionsOnMyRight(right);
                    msp.add(new Approxim(new Approxim(left, apx), right));
                }
            }
            totalCost += extraneousSpace*cost;
        }
        return totalCost;
    }

}
