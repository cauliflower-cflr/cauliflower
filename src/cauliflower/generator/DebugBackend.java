package cauliflower.generator;

import cauliflower.cflr.Label;
import cauliflower.cflr.Problem;
import cauliflower.cflr.Rule;

import java.io.PrintStream;

/**
 * DebugBackend.java
 *
 * Prints the input problem in some readable format
 *
 * Created by nic on 25/11/15.
 */
public class DebugBackend implements Backend{

    private final PrintStream out;

    public DebugBackend(PrintStream out){
        this.out = out;
    }

    public void generate(String problemName, Problem prob){
        int i=0;
        out.println(problemName + ":");
        for(Label l : prob.labels){
            out.print(i + "(");
            boolean pr = false;
            for(int d : l.fDomains){
                if(pr) out.print(",");
                else pr = true;
                out.print(d);
            }
            out.println(");");
            i++;
        }

        for(Rule r : prob.rules){
            out.println(r.toString());
        }
    }
}
