package cauliflower.optimiser;

import cauliflower.application.CauliflowerException;
import cauliflower.representation.Problem;

import java.util.Optional;

/**
 * RelationFilterTransformation
 * <p>
 * Author: nic
 * Date: 14/07/16
 */
public class RelationFilterTransformation implements Transform {
    @Override
    public Optional<Problem> apply(Problem spec, Profile prof) throws CauliflowerException {
        return Optional.empty();
    }
}
