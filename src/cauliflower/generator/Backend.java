package cauliflower.generator;

import cauliflower.cflr.Problem;

/**
 * Created by nic on 25/11/15.
 */
public interface Backend {

    void generate(String problemName, Problem prob) throws Exception;

}
