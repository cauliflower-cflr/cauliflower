package cauliflower;

import cauliflower.cflr.Label;
import cauliflower.cflr.Problem;
import cauliflower.cflr.Rule;
import cauliflower.generator.CppSemiNaiveBackend;
import cauliflower.generator.DebugBackend;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Arrays;

public class Main {

    // temporary hack for version information
    public static int MAJOR = 0;
    public static int MINOR = 0;
    public static int REVISION = 1;

    public static Problem running(){
        Label aLbl = new Label();
        Label bLbl = new Label();
        Label sLbl = new Label();
        Rule r1 = new Rule(new Rule.Lbl(2));
        Rule r2 = new Rule(new Rule.Lbl(2), new Rule.Lbl(0), new Rule.Lbl(2), new Rule.Lbl(1));
        return new Problem(1, Arrays.asList(aLbl, bLbl, sLbl), Arrays.asList(r1, r2));
    }

    public static Problem rev(){
        Label aLbl = new Label();
        Label bLbl = new Label();
        Label sLbl = new Label();
        Rule r = new Rule(new Rule.Lbl(2), new Rule.Lbl(0), new Rule.Rev(new Rule.Lbl(1)));
        return new Problem(1, Arrays.asList(aLbl, bLbl, sLbl), Arrays.asList(r));
    }

    public static void out(Problem p, String name, String src) throws Exception{
        new DebugBackend(System.out).generate(name, p);
        PrintStream ps = new PrintStream(new FileOutputStream(src));
        new CppSemiNaiveBackend(CppSemiNaiveBackend.Adt.StdTree, ps).generate(name, p);
        ps.close();
        System.out.println("---------------------------------------");
    }

    public static void main(String[] args) {
        try {
            out(running(), "running", "include/running_OUT.h");
            out(rev(), "rev", "include/rev_OUT.h");
        } catch (Exception exc){
            exc.printStackTrace();
        }
    }

}
