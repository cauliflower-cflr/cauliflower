package cauliflower;

import cauliflower.representation.Clause;
import cauliflower.representation.LabelUse;
import cauliflower.representation.Problem;
import cauliflower.representation.ProblemAnalysis;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

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
}
