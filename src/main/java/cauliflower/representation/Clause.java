package cauliflower.representation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Clause
 * <p>
 * Author: nic
 * Date: 3/06/16
 */
public abstract class Clause {

    public final ClauseType type;

    public Clause(ClauseType ty) {
        this.type = ty;
    }

    protected abstract <T> T visitMe(Visitor<T> vis);

    @Override
    public String toString() {
        return new ClauseString().visit(this);
    }

    /**
     * Different clause types
     */

    public enum ClauseType {
        COMPOSE, INTERSECT, REVERSE, NEGATE, LABEL, EPSILON
    }

    public static class Compose extends Clause {
        public final Clause left;
        public final Clause right;

        public Compose(Clause l, Clause r) {
            super(ClauseType.COMPOSE);
            this.left = l;
            this.right = r;
        }

        @Override
        protected <T> T visitMe(Visitor<T> vis) {
            return vis.visitCompose(this);
        }
    }

    public static class Intersect extends Clause {
        public final Clause left;
        public final Clause right;

        public Intersect(Clause l, Clause r) {
            super(ClauseType.INTERSECT);
            this.left = l;
            this.right = r;
        }

        @Override
        protected <T> T visitMe(Visitor<T> vis) {
            return vis.visitIntersect(this);
        }
    }

    public static class Reverse extends Clause {
        public final Clause sub;

        public Reverse(Clause s) {
            super(ClauseType.REVERSE);
            this.sub = s;
        }

        @Override
        protected <T> T visitMe(Visitor<T> vis) {
            return vis.visitReverse(this);
        }
    }

    public static class Negate extends Clause {
        public final Clause sub;

        public Negate(Clause s) {
            super(ClauseType.NEGATE);
            this.sub = s;
        }

        @Override
        protected <T> T visitMe(Visitor<T> vis) {
            return vis.visitNegate(this);
        }
    }

    public static class Epsilon extends Clause {
        public Epsilon() {
            super(ClauseType.EPSILON);
        }

        @Override
        protected <T> T visitMe(Visitor<T> vis) {
            return vis.visitEpsilon(this);
        }
    }

    /**
     * A clause visitor
     */
    public interface Visitor<T> {
        T visitCompose(Compose cl);

        T visitIntersect(Intersect cl);

        T visitReverse(Reverse cl);

        T visitNegate(Negate cl);

        T visitLabelUse(LabelUse cl);

        T visitEpsilon(Epsilon cl);

        default T visit(Clause cl) {
            return cl.visitMe(this);
        }
    }

    public static class VisitorBase<T> implements Visitor<T> {
        @Override
        public T visitCompose(Compose cl) {
            return null;
        }

        @Override
        public T visitIntersect(Intersect cl) {
            return null;
        }

        @Override
        public T visitReverse(Reverse cl) {
            return null;
        }

        @Override
        public T visitNegate(Negate cl) {
            return null;
        }

        @Override
        public T visitLabelUse(LabelUse cl) {
            return null;
        }

        @Override
        public T visitEpsilon(Epsilon cl) {
            return null;
        }
    }

    /**
     * Some utility
     */
    public static class ClauseString implements Visitor<String> {
        @Override
        public String visitCompose(Compose cl) {
            return "(" + visit(cl.left) + "," + visit(cl.right) + ")";
        }

        @Override
        public String visitIntersect(Intersect cl) {
            return "(" + visit(cl.left) + "&" + visit(cl.right) + ")";
        }

        @Override
        public String visitReverse(Reverse cl) {
            return "-" + visit(cl.sub);
        }

        @Override
        public String visitNegate(Negate cl) {
            return "!" + visit(cl.sub);
        }

        @Override
        public String visitLabelUse(LabelUse lu) {
            return lu.toString();
        }

        @Override
        public String visitEpsilon(Epsilon cl) {
            return "~EPSILON~";
        }
    }

    public static class SpecString extends ClauseString {
        @Override
        public String visitLabelUse(LabelUse lu) {
            return String.format("%s%s%s", lu.usedLabel.name,
                    lu.usedField.stream().map(dp -> "[" + dp.name + "]").collect(Collectors.joining()),
                    lu.priority == 0 ? "": "{" + lu.priority + "}");
        }

        @Override
        public String visitEpsilon(Epsilon cl) {
            return "~";
        }
    }

    public static class InOrderVisitor<T> implements Visitor<Void> {
        public final List<T> visits;
        public final Visitor<T> subVisitor;

        public InOrderVisitor(Visitor<T> trueVisitor) {
            visits = new ArrayList<>();
            subVisitor = trueVisitor;
        }

        public List<T> visitAllNonNull(Clause cl){
            visits.clear();
            this.visit(cl);
            return visits.stream().filter(t -> t!=null).collect(Collectors.toList());
        }

        @Override
        public Void visitCompose(Compose cl) {
            visit(cl.left);
            visits.add(subVisitor.visit(cl));
            visit(cl.right);
            return null;
        }

        @Override
        public Void visitIntersect(Intersect cl) {
            visit(cl.left);
            visits.add(subVisitor.visit(cl));
            visit(cl.right);
            return null;
        }

        @Override
        public Void visitReverse(Reverse cl) {
            visits.add(subVisitor.visit(cl));
            visit(cl.sub);
            return null;
        }

        @Override
        public Void visitNegate(Negate cl) {
            visits.add(subVisitor.visit(cl));
            visit(cl.sub);
            return null;
        }

        @Override
        public Void visitLabelUse(LabelUse cl) {
            visits.add(subVisitor.visit(cl));
            return null;
        }

        @Override
        public Void visitEpsilon(Epsilon cl) {
            visits.add(subVisitor.visit(cl));
            return null;
        }
    }

    public static List<LabelUse> getUsedLabelsInOrder(Clause c){
        return new Clause.InOrderVisitor<>(new Clause.VisitorBase<LabelUse>(){
            @Override
            public LabelUse visitLabelUse(LabelUse cl) {
                return cl;
            }
        }).visitAllNonNull(c);
    }

    public static Optional<ProblemAnalysis.Binding> getIfFilter(Clause c){
        if(c instanceof Intersect){
            if(((Intersect) c).left instanceof Epsilon || ((Intersect) c).right instanceof Epsilon){
                Clause other = ((Intersect) c).left instanceof Epsilon ? ((Intersect) c).right : ((Intersect) c).left;
                if(other instanceof Compose){
                    if(((Compose) other).left instanceof Reverse || ((Compose) other).right instanceof Reverse){
                        Clause l = ((Compose) other).left instanceof Reverse ? ((Reverse) ((Compose) other).left).sub : ((Compose) other).left;
                        Clause r = ((Compose) other).right instanceof Reverse ? ((Reverse) ((Compose) other).right).sub : ((Compose) other).right;
                        if(l instanceof LabelUse && r instanceof LabelUse && ((LabelUse) l).usedLabel == ((LabelUse) r).usedLabel){
                            return Optional.of(new ProblemAnalysis.Binding((LabelUse)l, ((Compose) other).right instanceof Reverse, false));
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Converts the visited clause into a normal-form clause, i.e. reversals as low as possible and left-leaning
     */
    public static Clause toNormalForm(Clause in){
        return new ComposeOrderer(true).visit(new ReverseSinker(false).visit(in));
    }
    private static class ComposeOrderer implements Visitor<Clause> {

        final boolean leftDeep;

        private ComposeOrderer(boolean leftDeep) {
            this.leftDeep = leftDeep;
        }

        @Override
        public Clause visitCompose(Compose cl) {
            Clause lft = new ComposeOrderer(true).visit(cl.left);
            Clause rgh = new ComposeOrderer(false).visit(cl.right);
            Clause from = leftDeep ? rgh : lft;
            Clause onto = leftDeep ? lft : rgh;
            while(from instanceof Compose){
                Clause extract = leftDeep ? ((Compose) from).left : ((Compose) from).right;
                from = leftDeep ? ((Compose) from).right : ((Compose) from).left;
                onto = leftDeep ? new Compose(onto, extract) : new Compose(extract, onto);
            }
            return leftDeep ? new Compose(onto, from) : new Compose(from, onto);
        }

        @Override
        public Clause visitIntersect(Intersect cl) {
            ComposeOrderer forwards = new ComposeOrderer(true);
            return new Intersect(forwards.visit(cl.left), forwards.visit(cl.right));
        }

        @Override
        public Clause visitReverse(Reverse cl) {
            return new Reverse(new ComposeOrderer(true).visit(cl.sub));
        }

        @Override
        public Clause visitNegate(Negate cl) {
            return new Negate(new ComposeOrderer(true).visit(cl.sub));
        }

        @Override
        public Clause visitLabelUse(LabelUse cl) {
            return cl;
        }

        @Override
        public Clause visitEpsilon(Epsilon cl) {
            return cl;
        }
    }
    private static class ReverseSinker implements Visitor<Clause>{

        final boolean reversed;

        private ReverseSinker(boolean reversing){
            reversed = reversing;
        }

        @Override
        public Clause visitCompose(Compose cl) {
            return reversed ? new Compose(visit(cl.right), visit(cl.left)) : new Compose(visit(cl.left), visit(cl.right));
        }

        @Override
        public Clause visitIntersect(Intersect cl) {
            return new Intersect(visit(cl.left), visit(cl.right));
        }

        @Override
        public Clause visitReverse(Reverse cl) {
            return new ReverseSinker(!reversed).visit(cl.sub);
        }

        @Override
        public Clause visitNegate(Negate cl) {
            return new Negate(visit(cl.sub));
        }

        @Override
        public Clause visitLabelUse(LabelUse cl) {
            return reversed ? new Reverse(cl) : cl;
        }

        @Override
        public Clause visitEpsilon(Epsilon cl) {
            return cl;
        }
    }

}
