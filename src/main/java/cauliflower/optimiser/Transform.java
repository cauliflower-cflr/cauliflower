package cauliflower.optimiser;

import cauliflower.application.CauliflowerException;
import cauliflower.representation.Problem;
import cauliflower.util.Logs;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

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

        protected boolean doAll;
        protected List<Transform> subTransforms;
        protected Transform lastTransform;

        public Group(boolean doAll, List<Transform> ts){
            this.doAll = doAll;
            this.subTransforms = ts;
            this.lastTransform = null;
        }

        public Group(boolean doAll, Transform ... ts){
            this(doAll, Arrays.asList(ts));
        }

        public static Group makeGroupFromPasses(boolean doAll, List<Passes> ts) {
            return new Group(doAll, ts.stream().map(Passes::getTransform).collect(Collectors.toList()));
        }

        public void blacklistLast(){
            subTransforms = subTransforms.stream().filter(c -> !c.getClass().equals(lastTransform.getClass())).collect(Collectors.toList());
            Logs.forClass(this.getClass()).trace("Blacklisting {} -> {}",
                    lastTransform.getClass().getSimpleName(),
                    subTransforms.stream()
                            .map(Transform::getClass)
                            .map(Class::getSimpleName)
                            .collect(Collectors.joining(",")));
            lastTransform = null;
        }

        @Override
        public String toString() {
            return String.format("<%s>[%s]", doAll ? "all" : "some", subTransforms.stream()
                    .map(Transform::getClass)
                    .map(Class::getSimpleName)
                    .collect(Collectors.joining(",")));
        }

        @Override
        public Optional<Problem> apply(Problem spec, Profile prof) throws CauliflowerException {
            Problem cur = spec;
            for(Transform tfm : subTransforms){
                Logs.forClass(this.getClass()).trace("Attempting {}", tfm.getClass().getSimpleName());
                Optional<Problem> next = tfm.apply(cur, prof);
                if(next.isPresent()){
                    Logs.forClass(tfm.getClass()).trace("Optimised: {}", next.get());
                    lastTransform = tfm;
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
