package cauliflower.generator;

import cauliflower.application.CauliflowerException;
import cauliflower.application.Task;
import cauliflower.representation.Problem;

import java.io.PrintStream;

/**
 * GeneratorForProblem
 * <p>
 * Author: nic
 * Date: 14/07/16
 */
public abstract class GeneratorForProblem implements Task<Void> {

    protected final Verbosity verb;
    protected final PrintStream outputStream;
    private Problem prob;

    public GeneratorForProblem(PrintStream out, Verbosity verbosity){
        this.verb = verbosity;
        this.outputStream = out;
    }

    protected Problem prob(){
        return prob;
    }

    public Void perform(Problem problem) throws CauliflowerException{
        this.prob = problem;
        this.performInternal();
        if(outputStream != System.out && outputStream != System.err) outputStream.close();
        return null;
    }

    protected abstract void performInternal() throws CauliflowerException;

}
