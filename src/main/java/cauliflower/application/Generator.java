package cauliflower.application;

import cauliflower.generator.Adt;
import cauliflower.generator.CppCSVBackend;
import cauliflower.generator.CppParallelBackend;
import cauliflower.generator.CppSerialBackend;
import cauliflower.parser.CFLRParser;
import cauliflower.parser.OmniParser;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;

/**
 * Generator
 *
 * A wrapper for code generation
 *
 * Author: nic
 * Date: 24/05/16
 */
public class Generator {

    public final String name;
    public final Path spec;
    public final Adt adt;
    public final boolean parallel, timers, reports;

    public Generator(String name, Configuration conf){
        this(name, conf.specFile, conf.adt, conf.parallel, conf.timers, conf.reports);
    }

    public Generator(String n, Path s, Adt a, boolean par, boolean time, boolean rep){
        this.name = n;
        this.spec = s;
        this.adt = a;
        this.parallel = par;
        this.timers = time;
        this.reports = rep;
    }

    public void generateBackend(Path output) throws IOException{
        PrintStream ps = new PrintStream(new FileOutputStream(output.toFile()));
        if(parallel){
            new CppParallelBackend(ps, timers).generate(name, OmniParser.getLegacy(spec).problem);
        } else {
            new CppSerialBackend(adt, ps).generate(name, OmniParser.getLegacy(spec).problem);
        }
        ps.close();
    }

    public void generateFrontend(Path output, Path backend) throws IOException{
        PrintStream ps2 = new PrintStream(new FileOutputStream(output.toFile()));
        String relPath = output.getParent().relativize(backend).toString();
        CFLRParser.ParserOutputs parse = OmniParser.getLegacy(spec);
        new CppCSVBackend(ps2, relPath, parse, reports).generate(name, parse.problem);
        ps2.close();
    }
}
