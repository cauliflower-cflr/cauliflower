package cauliflower;

import cauliflower.cflr.Label;
import cauliflower.cflr.Problem;
import cauliflower.cflr.Rule;
import cauliflower.generator.Backend;
import cauliflower.generator.CppSemiNaiveBackend;
import cauliflower.generator.DebugBackend;
import cauliflower.generator.NameMap;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Arrays;

public class Main {

    // temporary hack for version information
    public static int MAJOR = 0;
    public static int MINOR = 0;
    public static int REVISION = 1;

    public static void main(String[] args) {

        Label aLbl = new Label();
        Label bLbl = new Label();
        Label sLbl = new Label();

        Rule r1 = new Rule(new Rule.Lbl(2));
        Rule r2 = new Rule(new Rule.Lbl(2), new Rule.Lbl(0), new Rule.Lbl(2), new Rule.Lbl(1));

        NameMap m = new NameMap();
        Problem p = new Problem(1, Arrays.asList(aLbl, bLbl, sLbl), Arrays.asList(r1, r2));
        Backend b1 = new DebugBackend(System.out);
        Backend b2 = new CppSemiNaiveBackend(CppSemiNaiveBackend.Adt.StdTree, System.out);
        try {
            b1.generate("running", p);
            System.out.println("------------------------------");
            b2.generate("running", p);
            PrintStream ps = new PrintStream(new FileOutputStream("include/OUT.h"));
            new CppSemiNaiveBackend(CppSemiNaiveBackend.Adt.StdTree, ps).generate("running", p);
            ps.close();
        } catch (Exception exc){
            exc.printStackTrace();
        }
    }

}
