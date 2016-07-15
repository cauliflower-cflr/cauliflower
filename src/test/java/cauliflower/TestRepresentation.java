package cauliflower;

import cauliflower.representation.*;
import org.junit.Test;

import java.util.Map;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.*;

/**
 * TestRepresentation
 * <p>
 * Author: nic
 * Date: 14/07/16
 */
public class TestRepresentation {

    @Test
    public void testStratifiedDependencies(){
        try {
            Problem prob = Utilities.parseOrFail("a<-x.x;b<-x.x;a->b;");
            Map<Label, Set<Label>> deps = ProblemAnalysis.getLabelDependencyGraph(prob);
            assertTrue(deps.containsKey(prob.labels.get("a")));
            assertTrue(deps.containsKey(prob.labels.get("b")));
            assertTrue(deps.get(prob.labels.get("a")).contains(prob.labels.get("b")));
            assertEquals(1, deps.get(prob.labels.get("a")).size());
            assertTrue(deps.get(prob.labels.get("b")).isEmpty());
        } catch(Exception exc){
            fail(exc.getMessage());
        }
    }

    @Test
    public void testCyclicDependenciesMulti(){
        try {
            Problem prob = Utilities.parseOrFail("a<-x.x;b<-x.x;a->b;b->a;");
            Map<Label, Set<Label>> deps = ProblemAnalysis.getLabelDependencyGraph(prob);
            assertTrue(deps.containsKey(prob.labels.get("a")));
            assertTrue(deps.containsKey(prob.labels.get("b")));
            assertTrue(deps.get(prob.labels.get("a")).contains(prob.labels.get("b")));
            assertTrue(deps.get(prob.labels.get("b")).contains(prob.labels.get("a")));
            assertEquals(1, deps.get(prob.labels.get("a")).size());
            assertEquals(1, deps.get(prob.labels.get("b")).size());
        } catch(Exception exc){
            fail(exc.getMessage());
        }
    }

    @Test
    public void testCyclicDependenciesSelf(){
        try {
            Problem prob = Utilities.parseOrFail("a<-x.x;a->a;");
            Map<Label, Set<Label>> deps = ProblemAnalysis.getLabelDependencyGraph(prob);
            assertTrue(deps.containsKey(prob.labels.get("a")));
            assertTrue(deps.get(prob.labels.get("a")).contains(prob.labels.get("a")));
            assertEquals(1, deps.get(prob.labels.get("a")).size());
        } catch(Exception exc){
            fail(exc.getMessage());
        }
    }

    @Test
    public void testStratifiedDependenciesInverse(){
        try {
            Problem prob = Utilities.parseOrFail("a<-x.x;b<-x.x;a->b;");
            Map<Label, Set<Label>> deps = ProblemAnalysis.getInverseLabelDependencyGraph(prob);
            assertTrue(deps.containsKey(prob.labels.get("a")));
            assertTrue(deps.containsKey(prob.labels.get("b")));
            assertTrue(deps.get(prob.labels.get("b")).contains(prob.labels.get("a")));
            assertEquals(1, deps.get(prob.labels.get("b")).size());
            assertTrue(deps.get(prob.labels.get("a")).isEmpty());
        } catch(Exception exc){
            fail(exc.getMessage());
        }
    }

    @Test
    public void testCyclicDependenciesMultiInverse(){
        try {
            Problem prob = Utilities.parseOrFail("a<-x.x;b<-x.x;a->b;b->a;");
            Map<Label, Set<Label>> deps = ProblemAnalysis.getInverseLabelDependencyGraph(prob);
            assertTrue(deps.containsKey(prob.labels.get("a")));
            assertTrue(deps.containsKey(prob.labels.get("b")));
            assertTrue(deps.get(prob.labels.get("a")).contains(prob.labels.get("b")));
            assertTrue(deps.get(prob.labels.get("b")).contains(prob.labels.get("a")));
            assertEquals(1, deps.get(prob.labels.get("a")).size());
            assertEquals(1, deps.get(prob.labels.get("b")).size());
        } catch(Exception exc){
            fail(exc.getMessage());
        }
    }

    @Test
    public void testCyclicDependenciesSelfInverse(){
        try {
            Problem prob = Utilities.parseOrFail("a<-x.x;a->a;");
            Map<Label, Set<Label>> deps = ProblemAnalysis.getInverseLabelDependencyGraph(prob);
            assertTrue(deps.containsKey(prob.labels.get("a")));
            assertTrue(deps.get(prob.labels.get("a")).contains(prob.labels.get("a")));
            assertEquals(1, deps.get(prob.labels.get("a")).size());
        } catch(Exception exc){
            fail(exc.getMessage());
        }
    }

    @Test
    public void testNormalFormLeansLeft(){
        Utilities.ClauseSimpleString css = new Utilities.ClauseSimpleString();
        Rule r;
        r = Utilities.parseOrFail("a<-x.x;a->a,a,a;").getRule(0);
        assertThat(css.visit(r.ruleBody), is(css.visit(Clause.toNormalForm(r.ruleBody))));

        r = Utilities.parseOrFail("a<-x.x;a->a,(a,a);").getRule(0);
        assertThat(css.visit(Clause.toNormalForm(r.ruleBody)), is("((a,a),a)"));

        r = Utilities.parseOrFail("a<-x.x;a->a,(a,a),a;").getRule(0);
        assertThat(css.visit(Clause.toNormalForm(r.ruleBody)), is("(((a,a),a),a)"));

        r = Utilities.parseOrFail("a<-x.x;a->a,(a,a,a);").getRule(0);
        assertThat(css.visit(Clause.toNormalForm(r.ruleBody)), is("(((a,a),a),a)"));
    }

    @Test
    public void testNormalFormFlipsReversedChains(){
        Utilities.ClauseSimpleString css = new Utilities.ClauseSimpleString();
        Rule r;
        r = Utilities.parseOrFail("a<-x.x;b<-x.x;a->-(a,b);").getRule(0);
        assertThat(css.visit(Clause.toNormalForm(r.ruleBody)), is("(-b,-a)"));
        r = Utilities.parseOrFail("a<-x.x;b<-x.x;c<-x.x;a->-(a,b,c);").getRule(0);
        assertThat(css.visit(Clause.toNormalForm(r.ruleBody)), is("((-c,-b),-a)"));
    }

}

