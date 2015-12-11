package cauliflower;

import cauliflower.cflr.Label;
import cauliflower.cflr.Problem;
import cauliflower.cflr.Rule;
import cauliflower.generator.Adt;
import cauliflower.generator.CppCSVBackend;
import cauliflower.generator.CppSemiNaiveBackend;
import cauliflower.generator.DebugBackend;
import cauliflower.parser.CFLRParser;
import cauliflower.parser.ParseFile;
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

    public static void strOut(String s, String name, String src) throws IOException{
        out(fromString(s), name, src);
    }

    public static CFLRParser.ParserOutputs fromString(String s) throws IOException{
        InputStream gs = new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
        return new SimpleParser().parse(gs);
    }

    public static void out(CFLRParser.ParserOutputs po, String name, String src) throws IOException {
        new DebugBackend(System.out).generate(name, po.problem);
        PrintStream ps = new PrintStream(new FileOutputStream(src));
        PrintStream ps2 = new PrintStream(new FileOutputStream(src.replace("include/", "spikes/").replace(".h", ".cpp")));
        //new CppSemiNaiveBackend(CppSemiNaiveBackend.Adt.StdTree, System.out).generate(name, p);
        new CppSemiNaiveBackend(Adt.Btree, ps).generate(name, po.problem);
        //new CppCSVBackend(System.out, src.substring(src.indexOf("include/") + 8), po.labelNames, po.fieldDomains, true).generate(name, po.problem);
        new CppCSVBackend(ps2, src.substring(src.indexOf("include/") + 8), po.labelNames, po.fieldDomains, true).generate(name, po.problem);
        ps.close();
        ps2.close();
        System.out.println("---------------------------------------");
    }

    public static String usage(){
        return new StringBuilder("Usage:\n")
                .append("    java ").append(Main.class.getName()).append("[OPTIONS] {[-sn file [-cs file]] <cflr-file>}\n\n")
                .append("Reads the grammar from file <cflr-file> as a CFL-R problem, and\n")
                .append("writes outputs for that file depending on what was queued up \n")
                .append("before the input grammar:\n")
                .append("  -sn <file>  Write a semi-naive solver to <file>\n")
                .append("  -cs <file>  Write a frontend which reads CSV files to <file>\n")
                .append("Options:\n")
                .append("  -a <adt>    Uses <adt> as the abstract data-type\n")
                .append("  -v          Verbose mode\n")
                .toString();
    }

    public static void main(String[] args) {
        try {
            if(args.length > 0) {
                int i = 0;
                boolean verbose = false;
                String curSN = null;
                String curCS = null;
                Adt curAdt = Adt.Btree;
                while (i < args.length) {
                    if (args[i].equals("-h") || args[i].equals("--help")) {
                        System.out.println(usage());
                        System.exit(0);
                    } else if(args[i].equals("-v")) {
                        verbose = true;
                    } else if(args[i].equals("-a")) {
                        curAdt = Adt.valueOf(args[++i]);
                    } else if(args[i].equals("-sn")){
                        curSN = args[++i];
                    } else if(args[i].equals("-cs")){
                        curCS = args[++i];
                    } else {
                        File in = new File(args[i]);
                        if(!in.exists() || !in.isFile()) throw new IOException("Unable to locate Granmmar input file " + args[i]);
                        CFLRParser.ParserOutputs po = new ParseFile(new SimpleParser()).read(in);
                        String name = in.getName();
                        if(name.contains(".")) name = name.substring(0, name.lastIndexOf('.'));
                        if(verbose) new DebugBackend(System.out).generate(name, po.problem);
                        if(curSN != null) {
                            File snf = new File(curSN);
                            PrintStream ps = new PrintStream(new FileOutputStream(snf));
                            new CppSemiNaiveBackend(curAdt, ps).generate(name, po.problem);
                            ps.close();
                            if(curCS != null) {
                                PrintStream ps2 = new PrintStream(new FileOutputStream(curCS));
                                new CppCSVBackend(ps2, snf.getAbsolutePath(), po.labelNames, po.fieldDomains, false).generate(name, po.problem);
                                ps2.close();
                            }
                        }
                        curSN = null;
                        curCS = null;
                    }
                    i++;
                }
                //strOut(running(), "running", "include/running_OUT.h");
                //strOut(rev(), "rev", "include/rev_OUT.h");
                //strOut(pointsTo(), "pt", "include/pt_OUT.h");
                //strOut(jpt(), "jpt", "include/jpt_OUT.h");
                //strOut(jptx(), "jptx", "include/jptx_OUT.h");
                //strOut(flds(), "fld", "include/fld_OUT.h");
                //strOut(inter(), "inter", "include/inter_OUT.h");
            } else {
                System.err.println(usage());
                System.exit(1);
            }
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }

}
