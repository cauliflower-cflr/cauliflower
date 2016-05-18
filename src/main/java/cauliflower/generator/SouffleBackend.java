package cauliflower.generator;

import cauliflower.cflr.Label;
import cauliflower.cflr.Problem;
import cauliflower.util.CFLRException;

import java.io.PrintStream;

public class SouffleBackend implements Backend{

    private final PrintStream out;

    public SouffleBackend(PrintStream out){
        this.out = out;
    }

    @Override
    public void generate(String problemName, Problem prob) throws CFLRException {
        out.println("// " + problemName);

        out.println("// types");
        for(int i=0; i<prob.numDomains; i++){
            out.println(".type t" + i);
        }

    }
}
