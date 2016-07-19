package cauliflower;

import cauliflower.representation.*;
import cauliflower.util.CFLRException;
import org.junit.Test;

import java.util.Map;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

/**
 * TestRepresentation
 * <p>
 * Author: nic
 * Date: 14/07/16
 */
public class TestRepresentation {

    @Test
    public void ensureLabelSpecStringIsIdempotentNoField() throws CFLRException {
        String lbl = "a<-x.y";
        Problem prob = Utilities.parseOrFail(lbl + ";");
        assertThat(prob.labels.get("a").toStringDesc(), is(lbl));
        Problem prob2 = Utilities.parseOrFail(prob.labels.get("a").toStringDesc() + ";");
        assertThat(prob2.labels.get("a").toStringDesc(), is(lbl));
    }

    @Test
    public void ensureLabelSpecStringIsIdempotentWithField() throws CFLRException {
        String lbl = "a[f][g]<-x.y";
        Problem prob = Utilities.parseOrFail(lbl + ";");
        assertThat(prob.labels.get("a").toStringDesc(), is(lbl));
        Problem prob2 = Utilities.parseOrFail(prob.labels.get("a").toStringDesc() + ";");
        assertThat(prob2.labels.get("a").toStringDesc(), is(lbl));
    }

    @Test
    public void ensureRuleSpecStringIsIdempotent() throws CFLRException {
        String lbls = "a[o]<-x.y;b<-x.z;c<-x.z;d[o]<-y.z;";
        String r = "a[f]->((b&!c),-d[f])";
        Problem prob = Utilities.parseOrFail(lbls + r + ";");
        assertThat(prob.getRule(0).toSpecString(), is(r));
        Problem prob2 = Utilities.parseOrFail(lbls + prob.getRule(0).toSpecString() + ";");
        assertThat(prob2.getRule(0).toSpecString(), is(r));
    }

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

