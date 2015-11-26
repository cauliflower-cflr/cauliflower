package cauliflower.cflr;

import cauliflower.Util;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Rule.java
 *
 * Specifies the production of rules.
 *
 * Also contains subordinate classes for defining the rules
 *
 * Created by nic on 25/11/15.
 */
public class Rule {

    public static abstract class Clause {
        @Override
        public abstract String toString();
    }

    public static class Lbl extends Clause {
        public final int label;
        public final List<Integer> fields;
        public Lbl(int label, int...fields){
            this.label = label;
            this.fields = Arrays.stream(fields).boxed().collect(Collectors.toList());
        }
        public String toString(){
            StringBuilder ret = new StringBuilder();
            ret.append(label);
            for(int f : fields) ret.append("[" + f + "]");
            return ret.toString();
        }
    }

    public static class Rev extends Clause {
        public final Clause clause;
        public Rev(Clause clause) {
            this.clause = clause;
        }
        public String toString() {
            return "-" + clause.toString();
        }
    }

    public static class Neg extends Clause {
        public final Clause clause;
        public Neg(Clause clause) {
            this.clause = clause;
        }
        public String toString() {
            return "!" + clause.toString();
        }
    }

    public static class And extends Clause {
        public final Clause left;
        public final Clause right;
        public And(Clause left, Clause right) {
            this.left = left;
            this.right = right;
        }
        public String toString() {
            return "(" + left.toString() + "&" + right.toString() + ")";
        }
    }

    public final Lbl head;
    public final List<Clause> body;
    public final List<Lbl> dependencies;

    public Rule(Lbl head, Clause...body){
        this.head = head;
        this.body = Arrays.asList(body);
        this.dependencies = this.body.stream().flatMap((Rule.Clause c) -> Util.clauseLabels(c).stream()).collect(Collectors.toList());
    }

    @Override
    public String toString(){
        StringBuilder ret = new StringBuilder();
        ret.append(head.toString());
        ret.append(" ->");
        for(Clause c : body){
            ret.append(" ");
            ret.append(c.toString());
        }
        return ret.toString();
    }

    public interface ClauseVisitor {
        default void visit(Clause c) {
            if(c instanceof Lbl) visitLbl((Lbl) c);
            else if(c instanceof Rev) visitRev((Rev)c);
            else if(c instanceof Neg) visitNeg((Neg)c);
            else if(c instanceof And) visitAnd((And)c);
        }
        void visitLbl(Lbl l);
        void visitRev(Rev r);
        void visitNeg(Neg n);
        void visitAnd(And a);
    }
}
