package cauliflower;

import cauliflower.representation.*;
import cauliflower.util.CFLRException;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * TestBinding
 * <p>
 * Author: nic
 * Date: 12/07/16
 */
public class TestBinding {

    @Test
    public void testBindingEndpointsAtEndOfList(){
        Rule r = Utilities.parseOrFail("a<-x.x;b<-x.x;c<-x.x;b->a,b,c;").getRule(0);
        ProblemAnalysis.Bounds bindings = ProblemAnalysis.getBindings(r);
        List<LabelUse> lus = Clause.getUsedLabelsInOrder(r.ruleBody);
        assertTrue(bindings.entry.has(lus.get(0), true));
        assertTrue(bindings.exit.has(lus.get(2), false));
    }

    @Test
    public void testEpsilonElidedInChain() throws CFLRException {
        Problem prob = Utilities.parseOrFail("a<-x.x; b<-x.x; c<-x.x; a->b,~,c;");
        ProblemAnalysis.Bounds bindings = ProblemAnalysis.getBindings(prob.getRule(0));
        Label blbl = prob.labels.get("b");
        Label clbl = prob.labels.get("c");
        ProblemAnalysis.Bound bnd = bindings.find(blbl.usages.get(0), false).get();
        assertThat(bnd.has(clbl.usages.get(0), true), is(true));
    }

    @Test
    public void testEpsilonSimpleIntersection() throws CFLRException {
        Problem prob = Utilities.parseOrFail("a<-x.x; b<-x.x; c<-x.x; a->(b&~),c;");
        ProblemAnalysis.Bounds bindings = ProblemAnalysis.getBindings(prob.getRule(0));
        LabelUse blbl = prob.labels.get("b").usages.get(0);
        LabelUse clbl = prob.labels.get("c").usages.get(0);
        assertThat(bindings.find(blbl, true).get().has(blbl, false), is(true));
        assertThat(bindings.find(blbl, true).get().has(clbl, true), is(true));
    }

    @Test
    public void testEpsilonMultiIntersection() throws CFLRException {
        String[] rforms = {"a->b&c&~;", "a->c&~&b;", "a->~&b&c;"};
        for(String s : rforms){
            Problem prob = Utilities.parseOrFail("a<-x.x; b<-x.x; c<-x.x;" + s);
            ProblemAnalysis.Bounds bindings = ProblemAnalysis.getBindings(prob.getRule(0));
            LabelUse b1 = prob.labels.get("b").usages.get(0);
            LabelUse c1 = prob.labels.get("c").usages.get(0);
            assertThat(bindings.find(b1, true).get().has(b1, false), is(true));
            assertThat(bindings.find(b1, true).get().has(c1, true), is(true));
            assertThat(bindings.find(b1, true).get().has(c1, false), is(true));
        }
    }

    @Test
    public void testEpsilonSelfLoop() throws CFLRException {
        Problem prob = Utilities.parseOrFail("a<-x.x; b<-x.x; c<-x.x; a->~&(b,c);");
        ProblemAnalysis.Bounds b = ProblemAnalysis.getBindings(prob.getRule(0));
        LabelUse b1 = prob.labels.get("b").usages.get(0);
        LabelUse c1 = prob.labels.get("c").usages.get(0);
        assertThat(b.entry, is(b.exit));
        assertThat(b.find(b1, true).get(), is(b.find(c1, false).get()));
        assertThat(b.find(b1, false).get(), is(b.find(c1, true).get()));
    }

    @Test
    public void testEpsilonSourceFilterCreation() throws CFLRException {
        Problem prob = Utilities.parseOrFail("a<-x.x; b<-x.x; a->(b,-b)&~;");
        ProblemAnalysis.Bounds b = ProblemAnalysis.getBindings(prob.getRule(0));
        LabelUse b1 = prob.labels.get("b").usages.get(0);
        LabelUse b2 = prob.labels.get("b").usages.get(1);
        assertThat(b.all.size(), is(2));
        assertThat(b.entry, is(b.exit));
        assertThat(b.find(b1, true).get(), is(b.entry));
        assertThat(b.find(b1, true).get(), is(b.find(b2, true).get()));
        assertThat(b.find(b1, true).get(), is(not(b.find(b1, false).get())));
        assertThat(b.find(b1, false).get(), is(b.find(b2, false).get()));
    }

    @Test
    public void testEpsilonSinkFilterCreation() throws CFLRException {
        Problem prob = Utilities.parseOrFail("a<-x.x; b<-x.x; a->~&(-b,b);");
        ProblemAnalysis.Bounds b = ProblemAnalysis.getBindings(prob.getRule(0));
        LabelUse b1 = prob.labels.get("b").usages.get(0);
        LabelUse b2 = prob.labels.get("b").usages.get(1);
        assertThat(b.all.size(), is(2));
        assertThat(b.entry, is(b.exit));
        assertThat(b.find(b1, false).get(), is(b.entry));
        assertThat(b.find(b1, true).get(), is(b.find(b2, true).get()));
        assertThat(b.find(b1, true).get(), is(not(b.find(b1, false).get())));
        assertThat(b.find(b1, false).get(), is(b.find(b2, false).get()));
    }

}
