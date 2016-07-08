package cauliflower.application;

import cauliflower.optimiser.Controller;

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
public class Optimiser {

    private final Controller controller;

    public Optimiser(Path srcSpec, Path targetSpec, List<Path> trainingSet) throws IOException{
        this.controller = new Controller(srcSpec, targetSpec, trainingSet);
    }

    public void optimise() throws IOException, InterruptedException {
        controller.optimise(1);
    }

}
