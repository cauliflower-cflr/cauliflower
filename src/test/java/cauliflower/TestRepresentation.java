package cauliflower;

import cauliflower.representation.*;
import cauliflower.util.CFLRException;
import org.junit.Test;

import java.util.List;
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
    public void testPriority(){
        List<LabelUse> uses = Clause.getUsedLabelsInOrder(Utilities.parseOrFail("a<-x.x;a->a,a,a;").getRule(0).ruleBody);
        assertEquals(0, uses.get(0).priority);
        assertEquals(0, uses.get(1).priority);
        assertEquals(0, uses.get(2).priority);
        uses = Clause.getUsedLabelsInOrder(Utilities.parseOrFail("a<-x.x;a->a{0},a{1},a{-1};").getRule(0).ruleBody);
        assertEquals(0, uses.get(0).priority);
        assertEquals(1, uses.get(1).priority);
        assertEquals(-1, uses.get(2).priority);
    }

    @Test
    public void testDefaultPriorityOrderings() {
        Problem p = Utilities.parseOrFail("a<-x.x;b<-x.x;c<-x.x;d<-x.x;a->a,b,c,d;b->a{1},b{2},c{3},d{4};c->a{0},b{1},c{0},d{1};");
        int[][] correctOrders = {{0, 1, 2, 3}, {3, 2, 1, 0}, {1, 3, 0, 2}};
        for (int i = 0; i < correctOrders.length; i++) {
            List<LabelUse> givens = Clause.getUsedLabelsInOrder(p.getRule(i).ruleBody);
            List<LabelUse> pri = ProblemAnalysis.getEvaluationOrder(givens);
            for (int j = 0; j < correctOrders[i].length; j++) {
                assertEquals(givens.get(correctOrders[i][j]), pri.get(j));
            }
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

