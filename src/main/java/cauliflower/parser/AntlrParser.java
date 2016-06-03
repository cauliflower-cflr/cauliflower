package cauliflower.parser;

import cauliflower.parser.grammar.SpecificationBaseVisitor;
import cauliflower.parser.grammar.SpecificationLexer;
import cauliflower.parser.grammar.SpecificationParser;
import cauliflower.representation.Clause;
import cauliflower.representation.Problem;
import cauliflower.representation.Rule;
import cauliflower.util.CFLRException;
import cauliflower.util.Logs;
import cauliflower.util.Registrar;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.tree.ErrorNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AntlrParser
 * <p>
 * Author: nic
 * Date: 27/05/16
 */
public class AntlrParser implements CFLRParser, ANTLRErrorListener {

    public Problem problem;
    private String parseError; // set this string to the error message if we encounter one

    @Override
    public ParserOutputs parse(String s) throws CFLRException {
        return parse(new ANTLRInputStream(s));
    }

    @Override
    public ParserOutputs parse(InputStream is) throws CFLRException {
        ANTLRInputStream ais;
        try {
            ais = new ANTLRInputStream(is);
        } catch (IOException e) {
            throw new CFLRException(e.getMessage());
        }
        return parse(ais);
    }

    private ParserOutputs parse(ANTLRInputStream ais) throws CFLRException {
        SpecificationParser.SpecContext spec;
        try {
            SpecificationLexer lex = new SpecificationLexer(ais);
            SpecificationParser par = new SpecificationParser(new CommonTokenStream(lex));
            par.setErrorHandler(new BailErrorStrategy());
            par.removeErrorListeners();
            par.addErrorListener(this);
            spec = par.spec();
            Logs.forClass(AntlrParser.class).debug("Parsed: {}", new SpecificationUtils.PrettyPrinter(" ", "   ").visit(spec).toString());
        } catch (Exception exc) {
            if (parseError != null) throw new CFLRException(parseError);
            else throw new CFLRException(exc.toString() + ", Unexpected end of input");
        }
        ProblemBuilder pb = new ProblemBuilder(spec);
        Logs.forClass(AntlrParser.class).debug("Encoded: {}", pb.parsedProblem.toString());
        problem = pb.parsedProblem;
        return new ParserOutputs(null, pb.labelNames, pb.fieldDomains, pb.vertexDomains, pb.ruleFieldProjections);
    }


    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
        parseError = String.format("Syntax Error: %s, Line %d, Char %d", msg, line, charPositionInLine);
    }

    @Override
    public void reportAmbiguity(Parser recognizer, DFA dfa, int startIndex, int stopIndex, boolean exact, BitSet ambigAlts, ATNConfigSet configs) {
        parseError = String.format("Ambiguity: %s", dfa.toString());

    }

    @Override
    public void reportAttemptingFullContext(Parser recognizer, DFA dfa, int startIndex, int stopIndex, BitSet conflictingAlts, ATNConfigSet configs) {
        parseError = String.format("Full Context: %s", dfa.toString());
    }

    @Override
    public void reportContextSensitivity(Parser recognizer, DFA dfa, int startIndex, int stopIndex, int prediction, ATNConfigSet configs) {
        parseError = String.format("Context Sensitive: %s", dfa.toString());
    }

    private static class FieldListParse extends SpecificationBaseVisitor<List<String>> {
        @Override
        public List<String> visitLabel(SpecificationParser.LabelContext ctx) {
            final IdentifierParse idp = new IdentifierParse();
            return ctx.fld().stream().map(idp::visit).collect(Collectors.toList());
        }
    }

    private static class IdentifierParse extends SpecificationBaseVisitor<String> {
        @Override
        public String visitLabel(SpecificationParser.LabelContext ctx) {
            return ctx.ID().getText();
        }

        @Override
        public String visitDomain(SpecificationParser.DomainContext ctx) {
            return ctx.ID().getText();
        }

        @Override
        public String visitField(SpecificationParser.FieldContext ctx) {
            return ctx.ID().getText();
        }
    }

    private static class ProblemBuilder extends SpecificationBaseVisitor<Void> {

        final List<Throwable> errors;
        final Problem parsedProblem;
        final Registrar labelNames;
        final Registrar vertexDomains;
        final Registrar fieldDomains;
        final List<Registrar> ruleFieldProjections;

        ProblemBuilder(SpecificationParser.SpecContext ctx) throws CFLRException {
            errors = new ArrayList<>();
            parsedProblem = new Problem();
            labelNames = new Registrar();
            vertexDomains = new Registrar();
            fieldDomains = new Registrar();
            ruleFieldProjections = new ArrayList<>();
            this.visit(ctx);
            if (errors.size() > 0) {
                errors.stream().map(Throwable::toString).forEach(s -> Logs.forClass(AntlrParser.class).error("Error: {}", s));
                throw (CFLRException) errors.get(0);
            }
        }

        @Override
        public Void visitTypeDef(SpecificationParser.TypeDefContext ctx) {
            String lblName = new IdentifierParse().visit(ctx.lbl());
            String fromD = new IdentifierParse().visit(ctx.from);
            String toD = new IdentifierParse().visit(ctx.to);
            List<String> flds = new FieldListParse().visit(ctx.lbl());
            try {
                if (!parsedProblem.vertexDomains.has(fromD)) parsedProblem.addVertexDomain(fromD);
                if (!parsedProblem.vertexDomains.has(toD)) parsedProblem.addVertexDomain(toD);
                for (String f : flds) if (!parsedProblem.fieldDomains.has(f)) parsedProblem.addFieldDomain(f);
                parsedProblem.addLabel(lblName, fromD, toD, flds);
            } catch (CFLRException exc) {
                errors.add(exc);
            }
            return null;
        }

        @Override
        public Void visitRuleDef(SpecificationParser.RuleDefContext ctx){
            try {
                Rule.RuleBuilder rb = new Rule.RuleBuilder(parsedProblem);
                rb.setHead(rb.useLabel(new IdentifierParse().visit(ctx.lbl()), new FieldListParse().visit(ctx.lbl())));
                rb.setBody(new ClauseBuilder(rb).visit(ctx.expr()));
                rb.finish();
            } catch (CFLRException e) {
                errors.add(e);
            }
            return null;
        }

        @Override
        public Void visitErrorNode(ErrorNode node) {
            errors.add(new CFLRException(node.toString()));
            return null;
        }

        private class ClauseBuilder extends SpecificationBaseVisitor<Clause>{
            public final Rule.RuleBuilder rb;
            public ClauseBuilder(Rule.RuleBuilder rBuild){
                this.rb = rBuild;
            }

            @Override
            public Clause visitChainExpr(SpecificationParser.ChainExprContext ctx){
                return new Clause.Compose(visit(ctx.lhs), visit(ctx.rhs));
            }

            @Override
            public Clause visitSubTerm(SpecificationParser.SubTermContext ctx){
                return visit(ctx.expr());
            }

            @Override
            public Clause visitNegateTerm(SpecificationParser.NegateTermContext ctx) {
                return new Clause.Negate(visit(ctx.term()));
            }

            @Override
            public Clause visitReverseTerm(SpecificationParser.ReverseTermContext ctx) {
                return new Clause.Reverse(visit(ctx.term()));
            }

            @Override
            public Clause visitIntersectTerm(SpecificationParser.IntersectTermContext ctx) {
                return new Clause.Intersect(visit(ctx.lhs), visit(ctx.rhs));
            }

            @Override
            public Clause visitLabelTerm(SpecificationParser.LabelTermContext ctx){
                try {
                    return rb.useLabel(new IdentifierParse().visit(ctx.lbl()), new FieldListParse().visit(ctx.lbl()));
                } catch (CFLRException e) {
                    errors.add(e);
                    return null;
                }
            }

            @Override
            public Clause visitEpsilonTerm(SpecificationParser.EpsilonTermContext ctx) {
                return new Clause.Epsilon();
            }
        }

    }

    public static void main(String[] args) {
        try {
            new AntlrParser().parse(Arrays.stream(args).collect(Collectors.joining(" ")));
        } catch (CFLRException e) {
            e.printStackTrace();
        }
    }
}
