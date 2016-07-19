package cauliflower;

import cauliflower.representation.*;
import org.junit.Test;
import org.slf4j.helpers.Util;

import java.util.List;
import java.util.stream.Collectors;

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

    @Test
    public void testEpsilonChain(){
        Problem prob = Utilities.parseOrFail("a<-x.x; b<-x.x; c<-x.x; a->b,~,c;");
        List<ProblemAnalysis.Bound> bindings = ProblemAnalysis.getBindings(prob.getRule(0));
        bindings.stream().forEach(b -> System.out.println(b.boundEndpoints.stream().map(bn -> bn.bound + " - " + bn.bindsSource).collect(Collectors.joining(" , "))));
    }

    @Test
    public void testEpsilonSimpleIntersection(){

    }

    @Test
    public void testEpsilonMultiIntersection(){

    }

}
