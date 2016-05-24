package cauliflower;

import cauliflower.application.Compiler;
import cauliflower.application.Configuration;
import cauliflower.application.Generator;
import cauliflower.application.Optimiser;
import cauliflower.parser.CFLRParser;
import cauliflower.parser.ParseFile;
import cauliflower.parser.SimpleParser;
import cauliflower.util.Logs;

import java.io.File;
import java.io.IOException;

public class Main {

    public static void main(String[] args) {
        try {
            Configuration conf = Configuration.fromArgs(args);
            File in = new File(conf.specFile.get(0));
            if (!in.exists() || !in.isFile())
                throw new IOException("Unable to locate Grammar input file " + conf.specFile.get(0));
            CFLRParser.ParserOutputs po = new ParseFile(new SimpleParser()).read(in);
            String name = in.getName();
            if (name.contains(".")) name = name.substring(0, name.lastIndexOf('.'));
            if (conf.optimiseOutFile != null) {
                Optimiser opt = new Optimiser(conf.specFile.get(0), conf.optimiseOutFile, conf.optimiseTests);
                opt.optimise();
            } else if (conf.compOutFile != null) {
                Compiler comp = new Compiler(conf.compOutFile, conf);
                comp.compile(po);
            } else if (conf.snOutFile != null) {
                Generator gen = new Generator(conf);
                gen.generate(name, po);
            }
        } catch (Configuration.ConfigurationException e) {
            Logs.forClass(Main.class).error(e.msg);
            System.exit(1);
        } catch (Configuration.HelpException e) {
            System.out.println(e.usage);
        } catch (Exception e) {
            Logs.forClass(Main.class).error(e.getLocalizedMessage(), e);
            System.exit(1);
        }
    }
}
