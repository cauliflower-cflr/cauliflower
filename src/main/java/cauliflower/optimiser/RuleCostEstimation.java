package cauliflower.optimiser;

import cauliflower.representation.Clause;
import cauliflower.representation.Label;
import cauliflower.representation.LabelUse;
import cauliflower.representation.ProblemAnalysis;
import cauliflower.util.Pair;
import cauliflower.util.Streamer;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * RuleCostEstimation
 * <p>
 * Author: nic
 * Date: 12/07/16
 */
public class RuleCostEstimation implements Comparable<RuleCostEstimation>{

    public double timeCost;

    private final Profile profile;
    public final List<LabelUse> evalOrder;
    private Map<LabelUse, Integer> evalPriorities;
    private final List<Pair<ProblemAnalysis.Bound, Binder>> currentBindings;
    private final Map<LabelUse, Pair<Binder, Binder>> bindingsAtEval;

    public RuleCostEstimation(Profile prof, List<LabelUse> leftToRight, List<Integer> priorities, ProblemAnalysis.Bounds bindings) {
        this.profile = prof;
        this.evalOrder = ProblemAnalysis.getEvaluationOrder(leftToRight, priorities);
        evalPriorities = Streamer.zip(leftToRight.stream(), priorities.stream(), Pair::new).collect(Collectors.toMap(p -> p.first, p -> p.second));
        currentBindings = bindings.all.stream().map(b -> new Pair<>(b, (Binder)null)).collect(Collectors.toList());
        bindingsAtEval = evalOrder.stream()
                .sequential()
                .collect(Collectors.toMap(lu -> lu, lu -> {
                    Binder sourceBound = currentBindings.stream().filter(b -> b.second != null && b.first.has(lu, true)).findAny().map(p -> p.second).orElse(null);
                    Binder sinkBound = currentBindings.stream().filter(b -> b.second != null && b.first.has(lu, false)).findAny().map(p -> p.second).orElse(null);
                    if(sourceBound == null) currentBindings.stream().filter(bind -> bind.first.has(lu, true )).forEach(bind -> bind.second = new Binder(lu.usedLabel, true));
                    if(sinkBound == null)   currentBindings.stream().filter(bind -> bind.first.has(lu, false)).forEach(bind -> bind.second = new Binder(lu.usedLabel, false));
                    return new Pair<>(sourceBound, sinkBound);
                }));
        double iterCount = 1;
        for(LabelUse lu : evalOrder){
            timeCost += iterCount*workForIteration(lu, bindingsAtEval.get(lu).first, bindingsAtEval.get(lu).second);
            iterCount *= outputsForIteration(lu, bindingsAtEval.get(lu).first, bindingsAtEval.get(lu).second);
        }
    }

    public int getPriority(LabelUse lu){
        return evalPriorities.get(lu);
    }

    public boolean hasSameEvalOrder(Clause body){
        return ProblemAnalysis.getEvaluationOrder(Clause.getUsedLabelsInOrder(body)).equals(evalOrder);
    }

    private double outputsForIteration(LabelUse lu, Binder source, Binder sink) {
        double ret = profile.getRelationSize(lu.usedLabel);
        if(source != null){
            // apply a factor for the branchingness. i.e. if a elation has R edges emanating from D points, then
            // R/D new points will result from the join
            ret = ret/(double)(profile.getVertexDomainSize(lu.usedLabel.srcDomain));
            ret *= getDomainSaturationFactor(lu.usedLabel, true, source.lbl, source.source);
        }
        if(sink != null){
            ret = ret/(double)(profile.getVertexDomainSize(lu.usedLabel.dstDomain));
            ret *= getDomainSaturationFactor(lu.usedLabel, false, sink.lbl, sink.source);
        }
        return ret;
//        if(source == null && sink == null){
//            return profile.getRelationSize(lu.usedLabel);
//        } else if(source == null){
//            return profile.getVertexDomainSize(lu.usedLabel.srcDomain)/(double)profile.getVertexDomainSize(lu.usedLabel.dstDomain);
//        } else if(sink == null){
//            return profile.getVertexDomainSize(lu.usedLabel.dstDomain)/(double)profile.getVertexDomainSize(lu.usedLabel.srcDomain);
//        } else {
//            return profile.getRelationSize(lu.usedLabel)/(double)(profile.getVertexDomainSize(lu.usedLabel.srcDomain)*profile.getVertexDomainSize(lu.usedLabel.dstDomain));
//        }
    }

    /**
     * apply a factor of the probability that a match will be made, according to the saturation of this
     * source's domain-sat and the binder's relevant domain-sat.  if the binder covers a much smaller fraction
     * than this, the probability is small (i.e. many iterations fail), if the binder covers a much larger
     * fraction, the probability is high.  IMPORTANTLY, if the binder is the same relation, ALL binds succeed
     * (i.e. the factor is 1.0)
     */
    private double getDomainSaturationFactor(Label myLbl, boolean mySrc, Label otherLbl, boolean otherSrc){
        double ret = 1.0;
        if (myLbl != otherLbl || mySrc != otherSrc) {
            if (otherSrc) {
                ret = profile.getRelationSources(otherLbl) / (double) profile.getVertexDomainSize(otherLbl.srcDomain);
            } else {
                ret = profile.getRelationSinks(otherLbl) / (double) profile.getVertexDomainSize(otherLbl.dstDomain);
            }
        }
        return (ret+1.0)/2.0;
    }

    private double workForIteration(LabelUse lu, Binder source, Binder sink) {
        return Math.log(profile.getRelationSize(lu.usedLabel));
//        int logFactor =;
//        double rsize;
//        if(source != null){
//            if(sink != null) {
//                rsize = 1;
//            } else {
//                rsize = logFactor*(profile.getRelationSinks(lu.usedLabel)/(double)profile.getVertexDomainSize(lu.usedLabel.dstDomain));
//            }
//        } else {
//            if(sink != null) {
//                rsize = logFactor*(profile.getRelationSources(lu.usedLabel)/(double)profile.getVertexDomainSize(lu.usedLabel.srcDomain));
//            } else {
//                return profile.getRelationSize(lu.usedLabel);
//            }
//        }
//        return rsize*Math.log(logFactor);
    }

    private static class Binder{
        final Label lbl;
        final boolean source;
        public Binder(Label l, boolean src){
            lbl = l;
            source = src;
        }

        @Override
        public String toString() {
            return lbl.toString() + "(" + source + ")";
        }
    }

    @Override
    public int compareTo(RuleCostEstimation other) {
        return (int)(timeCost - other.timeCost);
    }

    @Override
    public String toString(){
        return timeCost + evalOrder.stream().map(lu -> lu.toString() + bindingsAtEval.get(lu).toString()).collect(Collectors.joining("   "));
    }
}

