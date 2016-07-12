package cauliflower.optimiser;

import cauliflower.representation.LabelUse;
import cauliflower.representation.ProblemAnalysis;
import cauliflower.util.Pair;

import java.util.List;
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
    private final List<LabelUse> evalOrder;
    private final List<Pair<ProblemAnalysis.Bound, Boolean>> currentBindings;

    public RuleCostEstimation(Profile prof, List<LabelUse> leftToRight, List<Integer> priorities, List<ProblemAnalysis.Bound> bindings) {
        this.profile = prof;
        this.evalOrder = ProblemAnalysis.getEvaluationOrder(leftToRight, priorities);
        this.currentBindings = bindings.stream().map(b -> new Pair<>(b, false)).collect(Collectors.toList());
        this.timeCost = 1;
        evalOrder.forEach(lu -> {
            boolean sourceBound = currentBindings.stream().anyMatch(b -> b.second && b.first.has(lu, true));
            boolean sinkBound = currentBindings.stream().anyMatch(b -> b.second && b.first.has(lu, false));
            this.timeCost *= getIterationCount(lu, sourceBound, sinkBound);
            currentBindings.stream().filter(bind -> bind.first.has(lu, true) || bind.first.has(lu, false)).forEach(bind -> bind.second = true);
        });
    }

    private double getIterationCount(LabelUse lu, boolean sourceBound, boolean sinkBound){
        int logFactor = profile.getRelationSize(lu.usedLabel);
        double rsize;
        if(sourceBound){
            if(sinkBound) {
                rsize = 1;
            } else {
                rsize = logFactor*(profile.getRelationSinks(lu.usedLabel)/(double)profile.getVertexDomainSize(lu.usedLabel.dstDomain));
            }
        } else {
            if(sinkBound) {
                rsize = logFactor*(profile.getRelationSources(lu.usedLabel)/(double)profile.getVertexDomainSize(lu.usedLabel.srcDomain));
            } else {
                rsize = logFactor;
            }
        }
        return rsize*Math.log(logFactor);
    }

    @Override
    public int compareTo(RuleCostEstimation other) {
        return (int)(timeCost - other.timeCost);
    }

    @Override
    public String toString(){
        return timeCost + evalOrder.toString();
    }
}
