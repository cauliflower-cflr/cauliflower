package cauliflower;

import cauliflower.generator.Adt;
import cauliflower.generator.CppCSVBackend;
import cauliflower.generator.CppSemiNaiveBackend;
import cauliflower.generator.DebugBackend;
import cauliflower.parser.CFLRParser;
import cauliflower.parser.ParseFile;
import cauliflower.parser.SimpleParser;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class Main {

    // temporary hack for version information
    public static int MAJOR = 0;
    public static int MINOR = 0;
    public static int REVISION = 1;

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
            } else {
                System.err.println(usage());
                System.exit(1);
            }
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }

}
