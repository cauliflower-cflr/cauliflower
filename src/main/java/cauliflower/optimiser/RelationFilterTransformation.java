package cauliflower.optimiser;

import cauliflower.application.CauliflowerException;
import cauliflower.representation.LabelUse;
import cauliflower.representation.Problem;
import cauliflower.representation.ProblemAnalysis;
import cauliflower.util.Pair;
import cauliflower.util.Streamer;

import java.util.List;
import java.util.Map;
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
 *  A large relation is updated in a cycle, consider all of its usages within that cycle (it is effectively a terminal in latter cycles)
 *  for some subset of its uses, where all those endpoints join effective terminals (i.e. the source of A always joins a certain thing, or the sink)
 *  Create a self-loop for the terminoid, then join the large relation with it to create the filter, uses of the large relation should subsequently
 *  go via the filter first.
 *
 * Author: nic
 * Date: 14/07/16
 */
public class RelationFilterTransformation implements Transform {

    private final boolean allowsSingletonFilters = false; // needs some more advanced profiling to decide if this is smart
    private final boolean filtersRetainFields = false;

    private Problem spec;

    @Override
    public Optional<Problem> apply(Problem spc, Profile prof) throws CauliflowerException {
        spec = spc;
        spec.labels.stream()
                .map(l -> new Pair<>(l, l.usages.stream().mapToLong(prof::getDeltaExpansionTime).sum()))
                .sorted(Pair::InverseSecondaryOrder)
                .map(l -> l.first)
                .map(l -> l.usages.stream()
                        .filter(lu -> !ProblemAnalysis.isEffectivelyTerminal(spec, lu)) // must be a nonterminal
                        .filter(lu -> ProblemAnalysis.ruleIsCyclic(spec, lu.usedInRule)) // and in a cyclic rule
                        .filter(lu -> lu.usedInRule.ruleHead != lu) // and not the head
                        .filter(lu -> bindsWithTerminal(lu, true) || bindsWithTerminal(lu, false)) // and binds to at least one terminal
                        .collect(Collectors.toList()))
                .flatMap(Streamer::choices)
                .filter(l -> !l.isEmpty())
                .filter(l -> l.stream().allMatch(lu -> bindsWithTerminal(lu, true)) || l.stream().allMatch(lu -> bindsWithTerminal(lu, false)))
                .map(l -> new Pair<>(l, benefitOfFilter(l)))
                .max(Pair::secondaryOrder)
                .map(p -> p.first)
                .ifPresent(System.out::println);
        return Optional.empty();
    }

    /**
     * higher is better
     */
    private double benefitOfFilter(List<LabelUse> filt){
        return filt.size();
    }

    public boolean bindsWithTerminal(LabelUse lu, boolean sourceBinding){
        return ProblemAnalysis.getBindings(lu.usedInRule)
                .find(lu, sourceBinding)
                .map(b -> b.boundEndpoints.stream()
                        .anyMatch(bnd -> ProblemAnalysis.isEffectivelyTerminal(spec, bnd.bound)))
                .orElse(false);
    }
}
