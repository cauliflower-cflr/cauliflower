package cauliflower.optimiser;

import cauliflower.application.CauliflowerException;
import cauliflower.representation.Problem;

import java.util.Optional;

/**
 * SubexpressionTransformation
 * <p>
 *     Performs two kinds of subexpression optimisations:
 *     <ul>
 *         <li>Chains of relations which are fixed in this cyclic SCC are moved to a previous SCC</li>
 *         <li>Chains of relations which occur multiple times in this SCC are make into their own nonterminal</li>
 *     </ul>
 *     Realistically this optimisation does not need a profile, and should be run immediately after reading the spec
 *
 * Author: nic
 * Date: 14/07/16
 */
public class SubexpressionTransformation implements Transform {

    @Override
    public Optional<Problem> apply(Problem spec, Profile prof) throws CauliflowerException {
        return Optional.empty();
    }
}
