package cauliflower;

import cauliflower.cflr.Label;
import cauliflower.cflr.Problem;
import cauliflower.cflr.Rule;
import cauliflower.generator.CppCSVBackend;
import cauliflower.generator.CppSemiNaiveBackend;
import cauliflower.generator.DebugBackend;
import cauliflower.parser.CFLRParser;
import cauliflower.parser.SimpleParser;
import cauliflower.util.CFLRException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Main {

    // temporary hack for version information
    public static int MAJOR = 0;
    public static int MINOR = 0;
    public static int REVISION = 1;

    public static String running(){
        return "a <- v . v;" +
                "b <- v . v;" +
                "S <- v . v;" +
                "S -> ;" +
                "S -> a, S, b;";
    }

    public static String rev(){
        return "a <- v . v;" +
                "b <- v . v;" +
                "S <- v . v;" +
                "S -> a, -b;";
    }

    public static String pointsTo(){
        return "alloc <- vert . heap;" +
                "assign <- vert . vert;" +
                "load[field] <- vert . vert;" +
                "store[field] <- vert . vert;" +
                "bridge <- vert . vert;" +
                "pt <- vert . heap;" +
                "pt -> alloc;" +
                "pt -> assign, pt;" +
                "pt -> bridge, pt;" +
                "bridge -> load[f], pt, -pt, store[f];";
    }

    public static String jptx() {
        return "Alloc <- vert . heap;" +
                "Assign <- vert . vert;" +
                "Load[field] <- vert . vert;" +
                "Store[field] <- vert . vert;" +
                "VarPointsTo <- vert . heap;" +
                "VarPointsTo -> Alloc;" +
                "VarPointsTo -> -Assign, VarPointsTo;" +
                "VarPointsTo -> -Load[f], VarPointsTo, -VarPointsTo, -Store[f], VarPointsTo;";
    }
    public static String jpt() {
        return "Alloc <- vert . heap;" +
                "Assign <- vert . vert;" +
                "Load[field] <- vert . vert;" +
                "Store[field] <- vert . vert;" +
                "Bridge <- vert . vert;" +
                "VarPointsTo <- vert . heap;" +
                "LVPT[field] <- vert . heap;" +
                "SVPT[field] <- vert . heap;" +
                "VarPointsTo -> Alloc;" +
                "VarPointsTo -> -Assign, VarPointsTo;" +
                "VarPointsTo -> Bridge, VarPointsTo;" +
                "LVPT[f] -> -Load[f], VarPointsTo;" +
                "SVPT[f] -> Store[f], VarPointsTo;" +
                "Bridge -> LVPT[f], -SVPT[f];";
    }

    public static String flds(){
        return "a[f] <- v . v;" +
                "b[f] <- v . v;" +
                "c <- v . v;" +
                "c -> a[f], b[f];";
    }

    public static String inter(){
        return "a <- v . v;" +
                "b <- v . v;" +
                "c <- v . v;" +
                "d <- v . v;" +
                "e <- v . v;" +
                "f <- v . v;" +
                "g <- v . v;" +
                "c -> (a & b);" +
                "d -> (b & -a);" +
                "e -> (-b & a);" +
                "f -> (-a & -b);" +
                "g -> (-(a & b) & (-a & -b))";
    }

    public static void out(String gram, String name, String src) throws IOException {
        InputStream gs = new ByteArrayInputStream(gram.getBytes(StandardCharsets.UTF_8));
        CFLRParser.ParserOutputs po =  new SimpleParser().parse(gs);
        new DebugBackend(System.out).generate(name, po.problem);
        PrintStream ps = new PrintStream(new FileOutputStream(src));
        PrintStream ps2 = new PrintStream(new FileOutputStream(src.replace("include/", "spikes/").replace(".h", ".cpp")));
        //new CppSemiNaiveBackend(CppSemiNaiveBackend.Adt.StdTree, System.out).generate(name, p);
        new CppSemiNaiveBackend(CppSemiNaiveBackend.Adt.Btree, ps).generate(name, po.problem);
        //new CppCSVBackend(System.out, src.substring(src.indexOf("include/") + 8), po.labelNames, po.fieldDomains, true).generate(name, po.problem);
        new CppCSVBackend(ps2, src.substring(src.indexOf("include/") + 8), po.labelNames, po.fieldDomains, true).generate(name, po.problem);
        ps.close();
        ps2.close();
        System.out.println("---------------------------------------");
    }

    public static void main(String[] args) {
        try {
            //out(running(), "running", "include/running_OUT.h");
            //out(rev(), "rev", "include/rev_OUT.h");
            //out(pointsTo(), "pt", "include/pt_OUT.h");
            //out(jpt(), "jpt", "include/jpt_OUT.h");
            //out(jptx(), "jptx", "include/jptx_OUT.h");
            //out(flds(), "fld", "include/fld_OUT.h");
            out(inter(), "inter", "include/inter_OUT.h");
        } catch (Exception exc){
            exc.printStackTrace();
        }
    }

}
