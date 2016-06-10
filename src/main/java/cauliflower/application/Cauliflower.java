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
            Configuration conf = Configuration.fromArgs(args);
            File in = new File(conf.specFile.get(0));
            if (!in.exists() || !in.isFile())
                throw new IOException("Unable to locate Grammar input file " + conf.specFile.get(0));
            String name = in.getName();
            if (name.contains(".")) name = name.substring(0, name.lastIndexOf('.'));
            if (conf.optimiseOutFile != null) {
                Optimiser opt = new Optimiser(conf.specFile.get(0), conf.optimiseOutFile, conf.optimiseTests);
                opt.optimise();
            } else if (conf.compOutFile != null) {
                Compiler comp = new Compiler(conf.compOutFile, conf);
                comp.compile();
            } else if (conf.snOutFile != null) {
                Generator gen = new Generator(name, conf);
                Path back = Paths.get(conf.snOutFile);
                gen.generateBackend(back);
                if (conf.csvOutFile != null) gen.generateFrontend(Paths.get(conf.csvOutFile), back);
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
