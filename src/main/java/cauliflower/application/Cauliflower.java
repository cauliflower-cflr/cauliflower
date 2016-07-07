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

    public static void main(String[] args) {
        try {
            Configuration conf = new Configuration(args);
            if (conf.optimise != null) {
                Optimiser opt = new Optimiser(conf.specFile, conf.outputBase, conf.sampleDirs);
                opt.optimise();
            } else
            if (conf.compile) {
                Compiler comp = new Compiler(conf.outputBase.toString(), conf);
                comp.compile();
            } else {
                Generator gen = new Generator(conf.outputBase.getFileName().toString(), conf);
                Path b = Paths.get(conf.outputBase.toString() + ".h");
                Path f = Paths.get(conf.outputBase.toString() + ".cpp");
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
