package cauliflower.application;

import cauliflower.parser.OmniParser;
import cauliflower.representation.Problem;
import cauliflower.util.Logs;

import java.io.IOException;

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
            Problem prob = OmniParser.get(conf.specFile);
            Task mainTask = new Generator(conf);
            if (conf.optimise) {
                mainTask = new Optimiser(conf);
            } else if (conf.compile) {
                mainTask = new Compiler(conf);
            }
            mainTask.perform(prob);
        } catch (Configuration.ConfigurationException e) {
            Logs.forClass(Cauliflower.class).error(e.msg);
            System.exit(Info.FAILURE_ARG);
        } catch (Configuration.HelpException e) {
            System.out.println(e.usage);
        } catch (IOException exc) {
            exc.printStackTrace();
            Logs.forClass(Cauliflower.class).error(exc.getMessage());
            System.exit(Info.FAILURE_SPEC);
        } catch (CauliflowerException exc) {
            System.exit(exc.getExitCode());
        }
    }
}
