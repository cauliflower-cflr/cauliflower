package cauliflower.application;

import cauliflower.optimiser.Controller;
import cauliflower.representation.Problem;

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

    private final Controller controller;

    public Optimiser(Configuration conf){
        this(conf.sampleDirs);
    }

    public Optimiser(List<Path> trainingSet){
        this.controller = new Controller(1, trainingSet);
    }

    public Void perform(Problem spec) throws CauliflowerException {
        controller.perform(spec);
        return null;
    }

}
