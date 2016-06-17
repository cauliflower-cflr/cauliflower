package cauliflower.application;

import cauliflower.generator.*;
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
    public final Configuration cfg;

    public Generator(String name, Configuration conf){
        this.name = name;
        this.cfg = conf;
    }

    public void generateBackend(Path output) throws IOException{
        PrintStream ps = new PrintStream(new FileOutputStream(output.toFile()));
        if(cfg.parallel){
            CppSemiNaiveBackend.generate(name, OmniParser.get(cfg.specFile), cfg, ps);
        } else {
            new CppSerialBackend(cfg.adt, ps).generate(name, OmniParser.getLegacy(cfg.specFile).problem);
        }
        ps.close();
    }

    public void generateFrontend(Path output, Path backend) throws IOException{
        PrintStream ps2 = new PrintStream(new FileOutputStream(output.toFile()));
        String relPath = output.getParent().toAbsolutePath().relativize(backend.toAbsolutePath()).toString();
        new CppCSVBackend(name, relPath, OmniParser.get(cfg.specFile), cfg.reports, ps2).generate();
        ps2.close();
    }
}
