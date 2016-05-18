package cauliflower;

import cauliflower.application.Configuration;
import cauliflower.generator.Backend;
import cauliflower.generator.CppCSVBackend;
import cauliflower.generator.CppParallelBackend;
import cauliflower.generator.CppSerialBackend;
import cauliflower.parser.CFLRParser;
import cauliflower.parser.ParseFile;
import cauliflower.parser.SimpleParser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

public class Main {

    public static void main(String[] args) {
        try {
            Configuration conf = Configuration.fromArgs(args);
            File in = new File(conf.specFile.get(0));
            if(!in.exists() || !in.isFile()) throw new IOException("Unable to locate Grammar input file " + conf.specFile.get(0));
            CFLRParser.ParserOutputs po = new ParseFile(new SimpleParser()).read(in);
            String name = in.getName();
            if(name.contains(".")) name = name.substring(0, name.lastIndexOf('.'));
            if(conf.snOutFile != null) {
                File snf = new File(conf.snOutFile);
                PrintStream ps = new PrintStream(new FileOutputStream(snf));
                Backend backend = conf.parallel ? new CppParallelBackend(ps, conf.timers) : new CppSerialBackend(conf.adt, ps);
                backend.generate(name, po.problem);
                ps.close();
                if(conf.csvOutFile != null) {
                    PrintStream ps2 = new PrintStream(new FileOutputStream(conf.csvOutFile));
                    String relPath = new File(conf.csvOutFile).getParentFile().toPath().relativize(snf.toPath()).toString();
                    new CppCSVBackend(ps2, relPath, po, conf.reports).generate(name, po.problem);
                    ps2.close();
                }
            }
        } catch (Configuration.ConfigurationException e) {
            System.err.println(e.msg);
            System.exit(1);
        } catch (Configuration.HelpException e) {
            System.out.println(e.usage);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
