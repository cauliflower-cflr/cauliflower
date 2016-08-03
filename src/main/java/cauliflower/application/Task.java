package cauliflower.application;


import cauliflower.representation.Problem;

import java.io.IOException;

/**
 * Task
 * <p>
 * Author: nic
 * Date: 14/07/16
 */
public interface Task<T> {

    T perform(Problem spec) throws CauliflowerException;

    /**
     * Utility for throwing CauliflowerExceptions with a custom message
     */
    default void except(String msg) throws CauliflowerException{
        throw new CauliflowerException(this.getClass(), msg);
    }

    /**
     * Utility for throwing CauliflowerExceptions when another exception occurs
     */
    default void except(IOException exc) throws CauliflowerException{
        throw new CauliflowerException(this.getClass(), exc.toString());
    }
}
