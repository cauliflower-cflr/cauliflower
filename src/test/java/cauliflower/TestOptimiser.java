package cauliflower;

import cauliflower.optimiser.RuleOrderer;
import cauliflower.parser.AntlrParser;
import cauliflower.representation.Clause;
import cauliflower.representation.LabelUse;
import cauliflower.representation.Problem;
import cauliflower.util.CFLRException;
import org.junit.Test;

import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * TestOptimiser
 * <p>
 * Author: nic
 * Date: 11/07/16
 */
public class TestOptimiser {

    private Problem parse(String spec){
        AntlrParser ap = new AntlrParser();
        try {
            ap.parse(spec);
        } catch (CFLRException e) {
            fail(e.getMessage());
        }
        return ap.problem;
    }

    private void checkClauseForm(String form, Clause recieved){
        assertEquals(form, new ClauseSimpleString().visit(recieved));
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

    @Test
    public void canonicalClausePreservesLabelUse(){
        Problem p = parse("a<-x.x;a->a;");
        checkClauseForm("a", new RuleOrderer.CanonicalClauseMaker(false).visit(p.getRule(0).ruleBody));
    }

    @Test
    public void canonicalClauseFlipsComposes(){
        Problem p = parse("a<-x.x;b<-x.x;a->a,b;");
        checkClauseForm("(a,b)", new RuleOrderer.CanonicalClauseMaker(false).visit(p.getRule(0).ruleBody));
        p = parse("a<-x.x;b<-x.x;a->-a,b;");
        checkClauseForm("(-a,b)", new RuleOrderer.CanonicalClauseMaker(false).visit(p.getRule(0).ruleBody));
        p = parse("a<-x.x;b<-x.x;a->a,-b;");
        checkClauseForm("(a,-b)", new RuleOrderer.CanonicalClauseMaker(false).visit(p.getRule(0).ruleBody));
        p = parse("a<-x.x;b<-x.x;a->-(a,b);");
        checkClauseForm("(-b,-a)", new RuleOrderer.CanonicalClauseMaker(false).visit(p.getRule(0).ruleBody));
    }
}
