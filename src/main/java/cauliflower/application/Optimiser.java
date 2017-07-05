package cauliflower.application;

import cauliflower.generator.CauliflowerSpecification;
import cauliflower.generator.Verbosity;
import cauliflower.optimiser.Controller;
import cauliflower.optimiser.Passes;
import cauliflower.representation.Problem;
import cauliflower.util.FileSystem;
import cauliflower.util.Logs;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Optimiser
 * <p>
 * Given a Cauliflower specification, continually runs and
 * analyses the generated code, using feedback to refine the
 * specification and improve performance.
 * <p>
 * Author: nic
 * Date: 24/05/16
 */
public class Optimiser implements Task<Void>{

    private final Path optimisedSpec;
    private final Controller controller;

    public Optimiser(Configuration conf){
        this(FileSystem.constructPath(conf.getOutputDir(), conf.problemName, "cflr"), conf.sampleDirs, conf.optimise, conf.optimisationPasses);
    }

    public Optimiser(Path optimisedSpec, List<Path> trainingSet, int rounds, List<Passes> passes){
        this.optimisedSpec = optimisedSpec;
        this.controller = new Controller(rounds, trainingSet, passes);
    }

    public Void perform(Problem spec) throws CauliflowerException {
        Problem out = controller.perform(spec);
        if(out == spec){
            Logs.forClass(this.getClass()).warn("Could not find an optimisation for this spec.");
        } else {
            try {
                new CauliflowerSpecification(optimisedSpec, new Verbosity()).perform(out);
            } catch (IOException e) {
                except(e);
            }
        }
        return null;
    }
}
