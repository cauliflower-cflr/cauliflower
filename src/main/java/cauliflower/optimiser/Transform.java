package cauliflower.optimiser;

import cauliflower.application.CauliflowerException;
import cauliflower.representation.Problem;
import cauliflower.util.Logs;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Transform
 * <p>
 * Author: nic
 * Date: 14/07/16
 */
public interface Transform {

    Optional<Problem> apply(Problem spec, Profile prof) throws CauliflowerException;

    default void except(String msg) throws CauliflowerException{
        throw new CauliflowerException(this.getClass(), msg);
    }

    default void except(IOException exc) throws CauliflowerException{
        throw new CauliflowerException(this.getClass(), exc.getMessage());
    }

    class Group implements Transform {

        protected final boolean doAll;
        protected final List<Transform> subTransforms;

        public Group(boolean doAll, List<Transform> ts){
            this.doAll = doAll;
            this.subTransforms = ts;
        }

        public Group(boolean doAll, Transform ... ts){
            this(doAll, Arrays.asList(ts));
        }

        @Override
        public Optional<Problem> apply(Problem spec, Profile prof) throws CauliflowerException {
            Problem cur = spec;
            for(Transform tfm : subTransforms){
                Optional<Problem> next = tfm.apply(cur, prof);
                if(next.isPresent()){
                    Logs.forClass(tfm.getClass()).trace("Optimised: {}", next.get());
                    if(doAll) cur = next.get();
                    else return next;
                } else {
                    Logs.forClass(tfm.getClass()).trace("No optimisation made");
                }
            }
            return cur == spec ? Optional.empty() : Optional.of(cur);
        }
    }
}
