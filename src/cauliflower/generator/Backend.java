package cauliflower.generator;

import cauliflower.cflr.Problem;
import cauliflower.util.CFLRException;

public interface Backend {

    void generate(String problemName, Problem prob) throws CFLRException;

}
