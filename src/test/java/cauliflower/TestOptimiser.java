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

    private void checkClauseForm(String form, Clause recieved){
        assertEquals(form, new Utilities.ClauseSimpleString().visit(recieved));
    }

    @Test
    public void canonicalClausePreservesLabelUse(){
        Problem p = Utilities.parseOrFail("a<-x.x;a->a;");
        checkClauseForm("a", new RuleOrderer.CanonicalClauseMaker(false).visit(p.getRule(0).ruleBody));
    }

    @Test
    public void canonicalClauseFlipsComposes(){
        Problem p = Utilities.parseOrFail("a<-x.x;b<-x.x;a->a,b;");
        checkClauseForm("(a,b)", new RuleOrderer.CanonicalClauseMaker(false).visit(p.getRule(0).ruleBody));
        p = Utilities.parseOrFail("a<-x.x;b<-x.x;a->-a,b;");
        checkClauseForm("(-a,b)", new RuleOrderer.CanonicalClauseMaker(false).visit(p.getRule(0).ruleBody));
        p = Utilities.parseOrFail("a<-x.x;b<-x.x;a->a,-b;");
        checkClauseForm("(a,-b)", new RuleOrderer.CanonicalClauseMaker(false).visit(p.getRule(0).ruleBody));
        p = Utilities.parseOrFail("a<-x.x;b<-x.x;a->-(a,b);");
        checkClauseForm("(-b,-a)", new RuleOrderer.CanonicalClauseMaker(false).visit(p.getRule(0).ruleBody));
    }
}
