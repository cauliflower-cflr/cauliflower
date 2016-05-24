package cauliflower.application;

import cauliflower.generator.Backend;
import cauliflower.generator.CppCSVBackend;
import cauliflower.generator.CppParallelBackend;
import cauliflower.generator.CppSerialBackend;
import cauliflower.parser.CFLRParser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Generator
 *
 * A wrapper for code generation
 *
 * Author: nic
 * Date: 24/05/16
 */
public class Generator {

    public final File snFront;
    public final File csvBack;

    private final Configuration configuration;

    public Generator(Configuration conf){
        this(conf.snOutFile, conf.csvOutFile, conf);
    }

    public Generator(String semiNaive, String csv, Configuration conf){
        this(semiNaive == null ? null : new File(semiNaive), csv == null ? null : new File(csv), conf);
    }

    public Generator(File semiNaive, File csv, Configuration conf){
        this.snFront = semiNaive;
        this.csvBack = csv;
        this.configuration = conf;
    }

    public void generate(String name, CFLRParser.ParserOutputs parse) throws IOException{
        PrintStream ps = new PrintStream(new FileOutputStream(snFront));
        Backend backend = configuration.parallel ? new CppParallelBackend(ps, configuration.timers) : new CppSerialBackend(configuration.adt, ps);
        backend.generate(name, parse.problem);
        ps.close();
        if(csvBack != null) {
            PrintStream ps2 = new PrintStream(new FileOutputStream(csvBack));
            String relPath = csvBack.getParentFile().toPath().relativize(snFront.toPath()).toString();
            new CppCSVBackend(ps2, relPath, parse, configuration.reports).generate(name, parse.problem);
            ps2.close();
        }
    }
}
