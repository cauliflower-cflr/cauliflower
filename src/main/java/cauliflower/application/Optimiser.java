package cauliflower.application;

import cauliflower.parser.CFLRParser;

import java.io.File;

/**
 * Optimiser
 *
 * Given a Cauliflower specification, continually runs and
 * analyses the analysis, using feedback to refine the
 * specification and improve performance.
 *
 * Author: nic
 * Date: 24/05/16
 */
public class Optimiser {

    public final File inputSpec;
    public final File optimisedSpec;

    public Optimiser(String srcSpec, String targetSpec){
        this.inputSpec = new File(srcSpec);
        this.optimisedSpec = new File(targetSpec);
    }

    public void optimise(CFLRParser.ParserOutputs po){

    }

}
