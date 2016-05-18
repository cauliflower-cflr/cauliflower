package cauliflower.parser;

import cauliflower.cflr.Problem;
import cauliflower.util.CFLRException;
import cauliflower.util.Registrar;

import java.io.InputStream;
import java.util.List;

/**
 * Parses input cflr files to a problem definition
 * Created by nic on 1/12/15.
 */
public interface CFLRParser {
    ParserOutputs parse(InputStream is) throws CFLRException;

    class ParserOutputs{
        public final Problem problem;
        public final Registrar labelNames;
        public final Registrar fieldDomains;
        public final Registrar vertexDomains;
        public final List<Registrar> ruleFields;

        public ParserOutputs(Problem prob, Registrar ln, Registrar fd, Registrar vd, List<Registrar> rfs){
            problem = prob;
            labelNames = ln;
            fieldDomains = fd;
            vertexDomains = vd;
            ruleFields = rfs;
        }
    }
}
