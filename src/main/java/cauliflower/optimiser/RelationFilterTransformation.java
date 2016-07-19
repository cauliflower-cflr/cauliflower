package cauliflower.optimiser;

import cauliflower.application.CauliflowerException;
import cauliflower.representation.Clause;
import cauliflower.representation.Problem;
import cauliflower.representation.ProblemAnalysis;
import cauliflower.util.Pair;

import java.util.Optional;
import java.util.stream.Collectors;

/**
 * RelationFilterTransformation
 *
 * Identifies when large relations are accessed multiple times in a cyclic component. In
 * which case a new nonterminal is made which is the restriction of the larger relation
 * to the endpoints that are necessary
 *
 *  - Must be joining a large nonterminal with something that is effectively a terminal
 *
 * Author: nic
 * Date: 14/07/16
 */
public class RelationFilterTransformation implements Transform {

    private final boolean allowsSingletonFilters = false;
    private final boolean filtersRetainFields = false;

    @Override
    public Optional<Problem> apply(Problem spec, Profile prof) throws CauliflowerException {
        spec.labels.stream()
                .map(l -> new Pair<>(l, l.usages.stream().mapToLong(prof::getDeltaExpansionTime).sum()))
                .sorted(Pair::InverseSecondaryOrder)
                .flatMap(p -> ProblemAnalysis.getRuleStream(spec)
                        .filter(r -> Clause.getUsedLabelsInOrder(r.ruleBody).stream()
                                .filter(lu -> lu.usedLabel == p.first).count() >= 2)
                        .sorted((r1,r2) -> prof.ruleWeight(r2).compareTo(prof.ruleWeight(r1)))
                        .map(r -> new Pair<>(p.first, ProblemAnalysis.getBindings(r).stream()
                                .filter(b -> b.boundEndpoints.size() > 1 && p.first.usages.stream().anyMatch(lu -> b.has(lu, true) || b.has(lu, false)))
                                .collect(Collectors.toList()))))
                .forEach(System.out::println);
        return Optional.empty();
    }




}
