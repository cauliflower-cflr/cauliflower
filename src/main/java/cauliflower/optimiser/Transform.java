package cauliflower.optimiser;

import cauliflower.application.CauliflowerException;
import cauliflower.representation.Problem;

import java.util.Optional;

/**
 * Transform
 * <p>
 * Author: nic
 * Date: 14/07/16
 */
public interface Transform {
    Optional<Problem> apply(Problem spec, Profile prof) throws CauliflowerException;
}
