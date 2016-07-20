package cauliflower.optimiser;

import cauliflower.representation.*;
import cauliflower.util.CFLRException;
import cauliflower.util.Logs;
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
        //spec.vertexDomains.stream().forEach(d -> System.out.printf("%s, %d\n", d.name, prof.getVertexDomainSize(d)));
        //spec.labels.stream().forEach(l -> System.out.println(String.format("%s - %d [%d  %d]", l.name, prof.getRelationSize(l), prof.getRelationSources(l), prof.getRelationSinks(l))));
        return IntStream.range(0, spec.getNumRules())
                .mapToObj(spec::getRule)
                .map(r -> new Pair<>(r, prof.ruleWeight(r)))
                .sorted((p1, p2) -> p2.second.compareTo(p1.second))
                .map(p -> p.first)
                .map(r -> {
                    ProblemAnalysis.Bounds bindings = ProblemAnalysis.getBindings(r);

                    List<LabelUse> bodyUses = Clause.getUsedLabelsInOrder(r.ruleBody);
                    int cur = 1;
                    for(int i=2; i<=bodyUses.size() && cur < MAX_PERMUTATIONS; i++) cur *= i;
                    cur = Math.min(cur, MAX_PERMUTATIONS);
                    return IntStream.range(0, cur)
                            .parallel()
                            .mapToObj(i -> Streamer.permuteIndices(i, bodyUses.size()))
                            .map(lst -> new RuleCostEstimation(prof, bodyUses, lst, bindings))
                            .min(RuleCostEstimation::compareTo)
                            .filter(rco -> !rco.hasSameEvalOrder(r.ruleBody))
                            .map(rco -> new Pair<Rule, RuleCostEstimation>(r, rco));

                    //EvaluationOrderTransformation.enumerateOrders(r).forEach(c ->{
                    //    System.out.println(" -> " + new Clause.ClauseString().visit(c));
                    //});
                }).filter(Optional::isPresent).map(Optional::get)
                .findFirst()
                .map(p -> rebuildWithRulePriority(spec, p.first, p.second));
    }

    public Problem rebuildWithRulePriority(Problem in, Rule target, RuleCostEstimation desired){
        try {
            ProblemBuilder rebuild = new ProblemBuilder().withAllLabels(in);
            for(int i=0; i<in.getNumRules(); i++){
                Rule r = in.getRule(i);
                if(r == target){
                    Rule.RuleBuilder newRule = rebuild.buildRule();
                    newRule.setHead(ProblemBuilder.copyLabelUsage(target.ruleHead, newRule));
                    newRule.setBody(new ProblemBuilder.ClauseCopier(newRule){
                        @Override
                        public Clause visitLabelUse(LabelUse cl) {
                            try {
                                return newRule.useLabel(cl.usedLabel.name, desired.getPriority(cl), cl.usedField.stream().map(dp -> dp.name).collect(Collectors.toList()));
                            } catch (CFLRException e) {
                                Logs.forClass(this.getClass()).error("Exception thrown: ", e);
                                return null;
                            }
                        }
                    }.copy(target.ruleBody));
                    newRule.finish();
                } else {
                    rebuild.withRule(r);
                }
            }
            return rebuild.finalise();
        } catch(CFLRException exc){
            Logs.forClass(this.getClass()).error("Exception thrown: ", exc);
            return null;
        }
    }
}
