package cauliflower.application;

import cauliflower.util.Logs;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Cauliflower
 * <p>
 * Author: nic
 * Date: 1/06/16
 */
public class Cauliflower {

    public static final String EXT_OPTIMISE = "cflr";
    public static final String EXT_EXE = null;
    public static final String EXT_HEADER = "h";
    public static final String EXT_SOURCE = "cpp";

    public static void main(String[] args) {
        try {
            Configuration conf = new Configuration(args);
            if (conf.optimise) {
                Optimiser opt = new Optimiser(conf.specFile, conf.getOutput(EXT_OPTIMISE), conf.sampleDirs);
                opt.optimise();
            } else if (conf.compile) {
                Compiler comp = new Compiler(conf.getOutput(EXT_EXE), conf);
                comp.compile();
            } else {
                Generator gen = new Generator(conf.problemName, conf);
                Path b = conf.getOutput(EXT_HEADER);
                Path f = conf.getOutput(EXT_SOURCE);
                gen.generateBackend(b);
                gen.generateFrontend(f, b);
            }
        } catch (Configuration.ConfigurationException e) {
            Logs.forClass(Cauliflower.class).error(e.msg);
            System.exit(1);
        } catch (Configuration.HelpException e) {
            System.out.println(e.usage);
        } catch (Exception e) {
            Logs.forClass(Cauliflower.class).error(e.getLocalizedMessage(), e);
            System.exit(1);
        }
    }
}
