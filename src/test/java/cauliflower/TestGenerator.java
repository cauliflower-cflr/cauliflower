package cauliflower;

import cauliflower.representation.*;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * TestGenerator
 * <p>
 * Author: nic
 * Date: 12/07/16
 */
public class TestGenerator {

    @Test
    public void testBindingEndpointsAtEndOfList(){
        Rule r = Utilities.parseOrFail("a<-x.x;b<-x.x;c<-x.x;b->a,b,c;").getRule(0);
        List<ProblemAnalysis.Bound> bindings = ProblemAnalysis.getBindings(r);
        List<LabelUse> lus = Clause.getUsedLabelsInOrder(r.ruleBody);
        assertTrue(bindings.get(bindings.size()-2).has(lus.get(0), true));
        assertTrue(bindings.get(bindings.size()-1).has(lus.get(2), false));
    }

}
