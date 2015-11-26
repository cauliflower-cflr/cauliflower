package cauliflower;

import cauliflower.cflr.Rule;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Util.java
 *
 * Various utilities
 *
 * Created by nic on 25/11/15.
 */
public class Util {

    public static List<Rule.Lbl> clauseLabels(Rule.Clause c){
        List<Rule.Lbl> ret = new ArrayList<>();
        new Rule.ClauseVisitor() {
            @Override
            public void visitLbl(Rule.Lbl l) {
                ret.add(l);
            }
            @Override
            public void visitRev(Rule.Rev r) {
                this.visit(r.clause);
            }
            @Override
            public void visitNeg(Rule.Neg n) {
                this.visit(n.clause);
            }
            @Override
            public void visitAnd(Rule.And a) {
                this.visit(a.left);
                this.visit(a.right);
            }
        }.visit(c);
        return ret;
    }

}
