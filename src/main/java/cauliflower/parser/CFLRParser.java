package cauliflower.parser;

import cauliflower.cflr.Problem;
import cauliflower.cflr.Rule;
import cauliflower.util.CFLRException;
import cauliflower.util.Registrar;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Parses input cflr files to a problem definition
 * Created by nic on 1/12/15.
 */
public interface CFLRParser {

    default ParserOutputs parse(String s) throws CFLRException {
        return parse(new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8)));
    }

    ParserOutputs parse(InputStream is) throws CFLRException;

    class ParserOutputs{
        public final Problem problem;
        public final Registrar labelNames;
        public final Registrar fieldDomains;
        public final Registrar vertexDomains;
        public final List<Registrar> ruleFields;

        public ParserOutputs(Problem prob, Registrar ln, Registrar fd, Registrar vd, List<Registrar> rfs){
            problem = prob;
            labelNames = ln;
            fieldDomains = fd;
            vertexDomains = vd;
            ruleFields = rfs;
        }

        public StringBuilder appendTerm(StringBuilder sb, int labelIdx, List<Integer> fieldIdxs, Registrar fieldReg){
            return sb.append(labelNames.fromIndex(labelIdx))
                    .append(fieldIdxs.stream().map(f -> "[" + fieldReg.fromIndex(f) + "]").collect(Collectors.joining()));
        }

        public StringBuilder appendLbl(StringBuilder sb, int r, Rule.Lbl l){
            return appendTerm(sb, l.label, l.fields, ruleFields.get(r));
        }

        @Override
        public String toString(){
            StringBuilder sb = new StringBuilder();
            for(int i=0; i<problem.labels.size(); i++){
                appendTerm(sb, i, problem.labels.get(i).fDomains, fieldDomains)
                        .append("<-")
                        .append(vertexDomains.fromIndex(problem.labels.get(i).fromDomain))
                        .append(".")
                        .append(vertexDomains.fromIndex(problem.labels.get(i).toDomain))
                        .append(";");
            }
            for(int r=0; r<problem.rules.size(); r++){
                appendLbl(sb, r, problem.rules.get(r).head);
                sb.append("->");
                final int curR = r;
                sb.append(problem.rules.get(r).body.stream().map(c ->{
                    ClausePrinter cp = new ClausePrinter(new StringBuilder(), curR);
                    cp.visit(c);
                    return cp.sb.toString();
                }).collect(Collectors.joining(",")));
                sb.append(";");
            }
            return sb.toString();
        }

        private class ClausePrinter implements Rule.ClauseVisitor{

            private final StringBuilder sb;
            private final int ruleIdx;

            public ClausePrinter(StringBuilder sb, int ruleIdx){
                this.sb = sb;
                this.ruleIdx = ruleIdx;
            }

            @Override
            public void visitLbl(Rule.Lbl l) {
                appendLbl(sb, ruleIdx, l);
            }

            @Override
            public void visitRev(Rule.Rev r) {
                sb.append("-");
                visit(r.clause);
            }

            @Override
            public void visitNeg(Rule.Neg n) {
                sb.append("!");
                visit(n.clause);
            }

            @Override
            public void visitAnd(Rule.And a) {
                sb.append("(");
                visit(a.left);
                sb.append("&");
                visit(a.right);
                sb.append(")");
            }
        }
    }
}
