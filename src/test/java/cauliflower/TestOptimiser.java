package cauliflower;

import cauliflower.application.CauliflowerException;
import cauliflower.optimiser.Profile;
import cauliflower.optimiser.SubexpressionTransformation;
import cauliflower.representation.Clause;
import cauliflower.representation.LabelUse;
import cauliflower.representation.Problem;
import cauliflower.representation.ProblemAnalysis;
import cauliflower.util.CFLRException;
import org.junit.Test;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.*;

/**
 * TestOptimiser
 * <p>
 * Author: nic
 * Date: 11/07/16
 */
public class TestOptimiser {

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
    public void testFilterGeneratedForLargeRelation() {

    }

    @Test
    public void testSubexpressionDoesNothing() {
        try {
            Optional<Problem> out = new SubexpressionTransformation().apply(Utilities.parseOrFail(""), Profile.emptyProfile());
            assertThat("empty spec", out, is(Optional.empty()));
            out = new SubexpressionTransformation().apply(Utilities.parseOrFail("a<-x.x;b<-x.x;c<-x.x;d<-x.x;d->a,b,c;"), Profile.emptyProfile());
            assertThat("non-cyclic", out, is(Optional.empty()));
            out = new SubexpressionTransformation().apply(Utilities.parseOrFail("a<-x.x;b<-x.x;c<-x.x;b->a,b,c;"), Profile.emptyProfile());
            assertThat("broken chain", out, is(Optional.empty()));
        } catch (CauliflowerException exc) {
            fail(exc.getMessage());
        }
    }

    @Test
    public void testTerminalHoist() {
        Problem p = Utilities.parseOrFail("a<-x.x;b<-x.x;c<-x.x;a->a,b,c;");
        try {
            Optional<Problem> out = new SubexpressionTransformation().apply(p, Profile.emptyProfile());
            assertThat("should hoist a subexpression", out, not(Optional.empty()));
            assertThat(out.get().getNumRules(), is(2));
            String ntn = SubexpressionTransformation.subexpressionName("b", false, "c", true);
            assertThat(Utilities.ruleForm(out.get().getRule(0)), is(ntn + "->(b,c)"));
            assertThat(Utilities.ruleForm(out.get().getRule(1)), is("a->(a," + ntn + ")"));
        } catch (CauliflowerException exc) {
            fail(exc.getMessage());
        }
    }

    @Test
    public void testSubexpressionDirection() {
        try {
            Problem p = Utilities.parseOrFail("a<-x.x;b<-x.x;c<-x.x;a->a,-b,c;");
            Optional<Problem> out = new SubexpressionTransformation().apply(p, Profile.emptyProfile());
            String ntn = SubexpressionTransformation.subexpressionName("b", true, "c", true);
            assertThat(Utilities.ruleForm(out.get().getRule(0)), is(ntn + "->(-b,c)"));
            assertThat(Utilities.ruleForm(out.get().getRule(1)), is("a->(a," + ntn + ")"));

            p = Utilities.parseOrFail("a<-x.x;b<-x.x;c<-x.x;a->a,b,-c;");
            out = new SubexpressionTransformation().apply(p, Profile.emptyProfile());
            ntn = SubexpressionTransformation.subexpressionName("b", false, "c", false);
            assertThat(Utilities.ruleForm(out.get().getRule(0)), is(ntn + "->(b,-c)"));
            assertThat(Utilities.ruleForm(out.get().getRule(1)), is("a->(a," + ntn + ")"));

            p = Utilities.parseOrFail("a<-x.x;b<-x.x;c<-x.x;a->a,-b,-c;");
            out = new SubexpressionTransformation().apply(p, Profile.emptyProfile());
            ntn = SubexpressionTransformation.subexpressionName("b", true, "c", false);
            assertThat(Utilities.ruleForm(out.get().getRule(0)), is(ntn + "->(-b,-c)"));
            assertThat(Utilities.ruleForm(out.get().getRule(1)), is("a->(a," + ntn + ")"));

            p = Utilities.parseOrFail("a<-x.x;b<-x.x;c<-x.x;a->a,-(b,c);");
            out = new SubexpressionTransformation().apply(p, Profile.emptyProfile());
            ntn = SubexpressionTransformation.subexpressionName("b", false, "c", true);
            assertThat(Utilities.ruleForm(out.get().getRule(0)), is(ntn + "->(b,c)"));
            assertThat(Utilities.ruleForm(out.get().getRule(1)), is("a->(a,-" + ntn + ")"));
        } catch (CauliflowerException exc) {
            fail(exc.getMessage());
        }
    }

    @Test
    public void testTerminalMultiHoist() {
        Problem p = Utilities.parseOrFail("a<-x.x;b<-x.x;c<-x.x;a->a,b,c,b,c;");
        try {
            Optional<Problem> out = new SubexpressionTransformation().apply(p, Profile.emptyProfile());
            assertThat("should hoist a subexpression", out, not(Optional.empty()));
            assertThat(out.get().getNumRules(), is(2));
            String ntn = SubexpressionTransformation.subexpressionName("b", false, "c", true);
            assertThat(Utilities.ruleForm(out.get().getRule(0)), is(ntn + "->(b,c)"));
            assertThat(Utilities.ruleForm(out.get().getRule(1)), is("a->((a," + ntn + ")," + ntn + ")"));
        } catch (CauliflowerException exc) {
            fail(exc.getMessage());
        }
    }

    @Test
    public void testRedundantHoist() {
        Problem p = Utilities.parseOrFail("a<-x.x;b<-x.x;a->a,b,a,b;");
        try {
            Optional<Problem> out = new SubexpressionTransformation().apply(p, Profile.emptyProfile());
            assertThat("should hoist a subexpression", out, not(Optional.empty()));
            assertThat(out.get().getNumRules(), is(2));
            String ntn = SubexpressionTransformation.subexpressionName("a", false, "b", true);
            assertThat(Utilities.ruleForm(out.get().getRule(0)), is(ntn + "->(a,b)"));
            assertThat(Utilities.ruleForm(out.get().getRule(1)), is("a->(" + ntn + "," + ntn + ")"));
        } catch (CauliflowerException exc) {
            fail(exc.getMessage());
        }
    }

    @Test
    public void testSummarisingHoist() throws CFLRException, CauliflowerException {
        Problem p = Utilities.parseOrFail("a<-x.x;b<-x.x;a->b,a,b;");
        SubexpressionTransformation set = new SubexpressionTransformation();
        Profile prof = Profile.emptyProfile();
        assertThat("dont hoist without the diamond", set.apply(p, prof), is(Optional.empty()));
        prof.setRelationSources(p.labels.get("a"), 10);
        prof.setRelationSinks(p.labels.get("a"), 11);
        prof.setRelationSources(p.labels.get("b"), 30);
        prof.setRelationSinks(p.labels.get("b"), 10);
        assertThat("dont hoist a small diamond", set.apply(p, prof), is(Optional.empty()));
        prof.setRelationSinks(p.labels.get("a"), 30);
        assertThat("should hoist a large diamond", set.apply(p, prof), not(Optional.empty()));
    }
}
