package cauliflower.parser;

import cauliflower.parser.grammar.SpecificationBaseVisitor;
import cauliflower.parser.grammar.SpecificationParser;
import cauliflower.util.Logs;
import cauliflower.util.Registrar;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * SpecificationUtils
 * <p>
 * Author: nic
 * Date: 27/05/16
 */
public class SpecificationUtils {

    /**
     * Generates the set of registrars from a parsed specification
     */
    public static class Auditor extends SpecificationBaseVisitor<Object>{
        private VertexDomainAuditor vertexDomains = new VertexDomainAuditor();
        private FieldDomainAuditor fieldDomains = new FieldDomainAuditor();
        private List<FieldDomainAuditor> ruleFieldDomains = new ArrayList<>();
        private LabelNameAuditor labelNames = new LabelNameAuditor();

        public static CFLRParser.ParserOutputs getRegistrars(SpecificationParser.SpecContext spec){
            Auditor a = new Auditor();
            a.visit(spec);
            return new CFLRParser.ParserOutputs(null,
                    a.labelNames.labelNames,
                    a.fieldDomains.fieldDomains,
                    a.vertexDomains.vertexDomains,
                    a.ruleFieldDomains.stream().map(fda -> fda.fieldDomains).collect(Collectors.toList()));
        }

        // static utility, prevent construction
        private Auditor() {}

        @Override
        public Object visitTypeDef(SpecificationParser.TypeDefContext ctx) {
            vertexDomains.visit(ctx);
            fieldDomains.visit(ctx);
            labelNames.visit(ctx);
            return null;
        }
        @Override
        public Object visitRuleDef(SpecificationParser.RuleDefContext ctx) {
            FieldDomainAuditor thisRuleFieldDomains = new FieldDomainAuditor();
            thisRuleFieldDomains.visit(ctx);
            ruleFieldDomains.add(thisRuleFieldDomains);
            labelNames.visit(ctx);
            return null;
        }
    }

    /**
     * Registers the vertex domains
     */
    private static class VertexDomainAuditor extends SpecificationBaseVisitor<Object>{
        public final Registrar vertexDomains = new Registrar();
        @Override
        public Object visitDomain(SpecificationParser.DomainContext ctx) {
            vertexDomains.toIndex(ctx.ID().getText());
            return null;
        }
    }

    /**
     * Registers the field domains
     */
    private static class FieldDomainAuditor extends SpecificationBaseVisitor<Object>{
        public final Registrar fieldDomains = new Registrar();
        @Override
        public Object visitField(SpecificationParser.FieldContext ctx) {
            fieldDomains.toIndex(ctx.ID().getText());
            return null;
        }
    }

    /**
     * registers the label names
     */
    private static class LabelNameAuditor extends SpecificationBaseVisitor<Object>{
        public final Registrar labelNames = new Registrar();
        @Override
        public Object visitLabel(SpecificationParser.LabelContext ctx){
            labelNames.toIndex(ctx.ID().getText());
            return null;
        }
    }

    /**
     * Converts a specification to a string representation using standardised formatting
     */
    public static class PrettyPrinter extends SpecificationBaseVisitor<StringBuffer> {

        private final String space;
        private final String line;

        public PrettyPrinter(String ws, String nl){
            space = ws;
            line = nl;
        }

        @Override
        public StringBuffer visitSpecification(SpecificationParser.SpecificationContext ctx){
            StringBuffer ret = new StringBuffer();
            for(ParseTree t : ctx.def()){
                ret.append(this.visit(t)).append(";").append(line);
            }
            return ret;
        }

        @Override
        public StringBuffer visitEmptySpecification(SpecificationParser.EmptySpecificationContext ctx){
            return new StringBuffer();
        }

        @Override
        public StringBuffer visitTypeDef(SpecificationParser.TypeDefContext ctx) {
            return visit(ctx.lbl()).append(space).append("<-").append(space)
                    .append(visit(ctx.from)).append(space).append(".").append(space).append(visit(ctx.to));
        }

        @Override
        public StringBuffer visitRuleDef(SpecificationParser.RuleDefContext ctx) {
            return visit(ctx.lbl()).append(space).append("->").append(space).append(visit(ctx.expr()));
        }

        @Override
        public StringBuffer visitUnitExpr(SpecificationParser.UnitExprContext ctx) {
            return visit(ctx.term());
        }

        @Override
        public StringBuffer visitChainExpr(SpecificationParser.ChainExprContext ctx) {
            return visit(ctx.expr()).append(",").append(space).append(visit(ctx.term()));
        }

        @Override
        public StringBuffer visitEpsilonTerm(SpecificationParser.EpsilonTermContext ctx) {
            return new StringBuffer(ctx.getText());
        }

        @Override
        public StringBuffer visitNegateTerm(SpecificationParser.NegateTermContext ctx) {
            return new StringBuffer("!").append(visit(ctx.term()));
        }

        @Override
        public StringBuffer visitReverseTerm(SpecificationParser.ReverseTermContext ctx) {
            return new StringBuffer("-").append(visit(ctx.term()));
        }

        @Override
        public StringBuffer visitLabelTerm(SpecificationParser.LabelTermContext ctx) {
            return visit(ctx.lbl());
        }

        @Override
        public StringBuffer visitIntersectTerm(SpecificationParser.IntersectTermContext ctx) {
            return visit(ctx.lhs).append(space).append("&").append(space).append(visit(ctx.rhs));
        }

        @Override
        public StringBuffer visitSubTerm(SpecificationParser.SubTermContext ctx) {
            return new StringBuffer("(").append(visit(ctx.expr())).append(")");
        }

        @Override
        public StringBuffer visitLabel(SpecificationParser.LabelContext ctx){
            StringBuffer ret = new StringBuffer(ctx.ID().getText());
            ctx.fld().stream().forEach(f -> ret.append(visit(f)));
            return ret;
        }

        @Override
        public StringBuffer visitDomain(SpecificationParser.DomainContext ctx) {
            return new StringBuffer(ctx.ID().getText());
        }

        @Override
        public StringBuffer visitField(SpecificationParser.FieldContext ctx) {
            return new StringBuffer("[").append(ctx.ID().getText()).append("]");
        }
    }

}
