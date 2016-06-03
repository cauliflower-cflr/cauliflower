package cauliflower.representation;

import java.util.stream.Collectors;

/**
 * Clause
 * <p>
 * Author: nic
 * Date: 3/06/16
 */
public abstract class Clause {

    public final ClauseType type;

    public Clause(ClauseType ty){
        this.type = ty;
    }

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
        public Compose(Clause l, Clause r){
            super(ClauseType.COMPOSE);
            this.left = l;
            this.right = r;
        }
    }

    public static class Intersect extends Clause {
        public final Clause left;
        public final Clause right;
        public Intersect(Clause l, Clause r){
            super(ClauseType.INTERSECT);
            this.left = l;
            this.right = r;
        }
    }

    public static class Reverse extends Clause{
        public final Clause sub;
        public Reverse(Clause s){
            super(ClauseType.REVERSE);
            this.sub = s;
        }
    }

    public static class Negate extends Clause{
        public final Clause sub;
        public Negate(Clause s){
            super(ClauseType.NEGATE);
            this.sub = s;
        }
    }

    public static class Epsilon extends Clause{
        public Epsilon(){
            super(ClauseType.EPSILON);
        }
    }

    /**
     * A clause visitor
     */
    public interface Visitor<T>{
        T visitCompose(Compose cl);
        T visitIntersect(Intersect cl);
        T visitReverse(Reverse cl);
        T visitNegate(Negate cl);
        T visitLabelUse(LabelUse cl);
        T visitEpsilon(Epsilon cl);
        default T visit(Clause cl){
            switch(cl.type){
                case COMPOSE: return visitCompose((Compose) cl);
                case INTERSECT: return visitIntersect((Intersect) cl);
                case REVERSE: return visitReverse((Reverse) cl);
                case NEGATE: return visitNegate((Negate) cl);
                case LABEL: return visitLabelUse((LabelUse) cl);
                case EPSILON: return visitEpsilon((Epsilon) cl);
            }
            throw new RuntimeException("Unreachable, failed to find the visitor");
        }
    }

    /**
     * Some utility
     */
    public class ClauseString implements Visitor<String>{
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

}