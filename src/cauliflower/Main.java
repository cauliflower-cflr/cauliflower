package cauliflower;

import cauliflower.cflr.Label;
import cauliflower.cflr.Problem;
import cauliflower.cflr.Rule;
import cauliflower.generator.CppSemiNaiveBackend;
import cauliflower.generator.DebugBackend;
import cauliflower.util.CFLRException;

import java.io.FileOutputStream;
import java.io.IOException;
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

    public static Problem pointsTo(){
        Label al = new Label();
        Label as = new Label();
        Label st = new Label(2);
        Label lo = new Label(2);
        Label br = new Label();
        Label pt = new Label();
        Rule r1 = new Rule(new Rule.Lbl(5), new Rule.Lbl(0));
        Rule r2 = new Rule(new Rule.Lbl(5), new Rule.Lbl(1), new Rule.Lbl(5));
        Rule r3 = new Rule(new Rule.Lbl(5), new Rule.Lbl(4), new Rule.Lbl(5));
        Rule r4 = new Rule(new Rule.Lbl(4), new Rule.Lbl(3, 0), new Rule.Lbl(5), new Rule.Rev(new Rule.Lbl(5)), new Rule.Lbl(2, 0), new Rule.Lbl(5));
        return new Problem(3, Arrays.asList(al, as, st, lo, br, pt), Arrays.asList(r1, r2, r3, r4));
    }

    public static Problem flds(){
        Label a = new Label(0);
        Label b = new Label(0, 0);
        Label c = new Label(0, 0, 0);
        Label d = new Label(0, 0, 0, 0);
        Rule r1 = new Rule(new Rule.Lbl(0, 3), new Rule.Lbl(2, 2, 0, 1), new Rule.Lbl(1, 1, 2));
        return new Problem(2, Arrays.asList(a, b, c, d), Arrays.asList(r1));
    }

    public static void out(Problem p, String name, String src) throws IOException {
        new DebugBackend(System.out).generate(name, p);
        PrintStream ps = new PrintStream(new FileOutputStream(src));
        //new CppSemiNaiveBackend(CppSemiNaiveBackend.Adt.StdTree, System.out).generate(name, p);
        new CppSemiNaiveBackend(CppSemiNaiveBackend.Adt.StdTree, ps).generate(name, p);
        ps.close();
        System.out.println("---------------------------------------");
    }

    public static void main(String[] args) {
        try {
            //out(running(), "running", "include/running_OUT.h");
            //out(rev(), "rev", "include/rev_OUT.h");
            out(pointsTo(), "pt", "include/pt_OUT.h");
            //out(flds(), "fld", "include/fld_OUT.h");
        } catch (Exception exc){
            exc.printStackTrace();
        }
    }

}
