package cauliflower;

import cauliflower.parser.AntlrParser;
import cauliflower.parser.OmniParser;
import cauliflower.representation.Clause;
import cauliflower.representation.LabelUse;
import cauliflower.representation.Problem;
import cauliflower.representation.Rule;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.fail;

/**
 * Utilities
 * <p>
 * Author: nic
 * Date: 12/07/16
 */
public class Utilities {

    public static Problem parseOrFail(String s){
        try{
            AntlrParser ap = new AntlrParser();
            ap.parse(s);
            return ap.problem;
        } catch(Exception exc){
            fail("Failed to parse: " + s + " : " + exc.getMessage());
            return null;
        }
    }

    public static Problem parseOrFail(Path p) throws IOException {
        return OmniParser.get(p);
    }

    public static String ruleForm(Rule r){
        Clause.Visitor<String> cts = new ClauseSimpleString();
        return cts.visit(r.ruleHead) + "->" + cts.visit(r.ruleBody);
    }

    public static class ClauseSimpleString implements Clause.Visitor<String> {
        @Override
        public String visitCompose(Clause.Compose cl) {
            return "(" + visit(cl.left) + "," + visit(cl.right) + ")";
        }

        @Override
        public String visitIntersect(Clause.Intersect cl) {
            return "(" + visit(cl.left) + "&" + visit(cl.right) + ")";
        }

        @Override
        public String visitReverse(Clause.Reverse cl) {
            return "-" + visit(cl.sub);
        }

        @Override
        public String visitNegate(Clause.Negate cl) {
            return "!" + visit(cl.sub);
        }

        @Override
        public String visitLabelUse(LabelUse lu) {
            return lu.usedLabel.name + lu.usedField.stream().map(dp -> "[" + dp.name + "]").collect(Collectors.joining());
        }

        @Override
        public String visitEpsilon(Clause.Epsilon cl) {
            return "~";
        }
    }
}
