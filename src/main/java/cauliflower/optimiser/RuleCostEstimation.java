package cauliflower.optimiser;

import cauliflower.representation.Clause;
import cauliflower.representation.Label;
import cauliflower.representation.LabelUse;
import cauliflower.representation.ProblemAnalysis;
import cauliflower.util.Pair;
import cauliflower.util.Streamer;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
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

    public RuleCostEstimation(Profile prof, List<LabelUse> leftToRight, List<Integer> priorities, List<ProblemAnalysis.Bound> bindings) {
        this.profile = prof;
        this.evalOrder = ProblemAnalysis.getEvaluationOrder(leftToRight, priorities);
        evalPriorities = Streamer.zip(leftToRight.stream(), priorities.stream(), Pair::new).collect(Collectors.toMap(p -> p.first, p -> p.second));
        currentBindings = bindings.stream().map(b -> new Pair<>(b, (Binder)null)).collect(Collectors.toList());
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
        if(source != null) ret = ret/(double)(profile.getVertexDomainSize(lu.usedLabel.srcDomain));
        if(sink != null) ret = ret/(double)(profile.getVertexDomainSize(lu.usedLabel.dstDomain));
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

