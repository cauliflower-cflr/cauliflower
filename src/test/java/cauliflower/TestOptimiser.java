package cauliflower;

import cauliflower.application.CauliflowerException;
import cauliflower.optimiser.Profile;
import cauliflower.optimiser.SubexpressionTransformation;
import cauliflower.representation.Clause;
import cauliflower.representation.LabelUse;
import cauliflower.representation.Problem;
import cauliflower.representation.ProblemAnalysis;
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
    public void testDefaultPriorityOrderings(){
        Problem p = Utilities.parseOrFail("a<-x.x;b<-x.x;c<-x.x;d<-x.x;a->a,b,c,d;b->a{1},b{2},c{3},d{4};c->a{0},b{1},c{0},d{1};");
        int[][] correctOrders = {{0,1,2,3}, {3,2,1,0}, {1,3,0,2}};
        for(int i=0; i<correctOrders.length; i++){
            List<LabelUse> givens = Clause.getUsedLabelsInOrder(p.getRule(i).ruleBody);
            List<LabelUse> pri = ProblemAnalysis.getEvaluationOrder(givens);
            for(int j=0; j<correctOrders[i].length; j++){
                assertEquals(givens.get(correctOrders[i][j]), pri.get(j));
            }
        }
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
        } catch(CauliflowerException exc){
            fail(exc.getMessage());
        }
    }

    @Test
    public void testSubexpressionHoist() {
        Problem p = Utilities.parseOrFail("a<-x.x;b<-x.x;c<-x.x;a->a,b,c;");
        try {
            Optional<Problem> out = new SubexpressionTransformation().apply(p, Profile.emptyProfile());
            assertThat("should hoist a subexpression", out, not(Optional.empty()));
        } catch(CauliflowerException exc){
            fail(exc.getMessage());
        }
    }

    @Test
    public void testSubexpressionMultiHoist() {
        Problem p = Utilities.parseOrFail("a<-x.x;b<-x.x;c<-x.x;a->a,b,c,b,c;");
        try {
            Optional<Problem> out = new SubexpressionTransformation().apply(p, Profile.emptyProfile());
            assertThat("should hoist a subexpression", out, not(Optional.empty()));
        } catch(CauliflowerException exc){
            fail(exc.getMessage());
        }
    }
}
