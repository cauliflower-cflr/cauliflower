package cauliflower.parser;

import cauliflower.parser.grammar.SpecificationBaseListener;
import cauliflower.parser.grammar.SpecificationLexer;
import cauliflower.parser.grammar.SpecificationListener;
import cauliflower.parser.grammar.SpecificationParser;
import cauliflower.representation.Domain;
import cauliflower.representation.DomainProjection;
import cauliflower.representation.Problem;
import cauliflower.util.CFLRException;
import cauliflower.util.Logs;
import cauliflower.util.Registrar;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;

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

    private String parseError; // set this string to the error message if we encounter one

    @Override
    public ParserOutputs parse(String s) throws CFLRException {
        try {
            return parse(new ANTLRInputStream(s));
        } catch(IOException e) {
            throw new CFLRException(e.getMessage());
        }
    }

    @Override
    public ParserOutputs parse(InputStream is) throws CFLRException {
        try {
            return parse(new ANTLRInputStream(is));
        } catch (IOException e) {
            throw new CFLRException(e.getMessage());
        }
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
        } catch(Exception exc) {
            if(parseError != null) throw new CFLRException(parseError);
            else throw new CFLRException(exc.toString() + ", Unexpected end of input");
        }
        ProblemBuilder pb = new ProblemBuilder(spec);
        Logs.forClass(AntlrParser.class).debug("Encoded: {}", pb.parsedProblem.toString());
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

    private static class ProblemBuilder implements SpecificationListener {

        final List<String> errors;
        final Problem parsedProblem;
        final Registrar labelNames;
        final Registrar vertexDomains;
        final Registrar fieldDomains;
        final List<Registrar> ruleFieldProjections;

        ProblemBuilder(SpecificationParser.SpecContext ctx) throws CFLRException{
            errors = new ArrayList<>();
            parsedProblem = new Problem();
            labelNames = new Registrar();
            vertexDomains = new Registrar();
            fieldDomains = new Registrar();
            ruleFieldProjections = new ArrayList<>();
            ParseTreeWalker walk = new ParseTreeWalker();
            walk.walk(this, ctx);
            if(errors.size() > 0){
                throw new CFLRException("Specification error:\n - " + errors.stream().collect(Collectors.joining("\n - ")));
            }
        }

        @Override
        public void enterSpecification(SpecificationParser.SpecificationContext ctx) {

        }

        @Override
        public void exitSpecification(SpecificationParser.SpecificationContext ctx) {

        }

        @Override
        public void enterEmptySpecification(SpecificationParser.EmptySpecificationContext ctx) {

        }

        @Override
        public void exitEmptySpecification(SpecificationParser.EmptySpecificationContext ctx) {

        }

        //parsing a typedef
        private String labelFromDomain;
        private String labelToDomain;
        @Override
        public void enterTypeDef(SpecificationParser.TypeDefContext ctx) {
            labelFromDomain = null;
            labelToDomain = null;
        }

        @Override
        public void exitTypeDef(SpecificationParser.TypeDefContext ctx) {
            try {
                // TODO handle multiple defs with same name
                if(!parsedProblem.vertexDomains.has(labelFromDomain)) parsedProblem.addVertexDomain(labelFromDomain);
                if(!parsedProblem.vertexDomains.has(labelToDomain)) parsedProblem.addVertexDomain(labelToDomain);
                for(String lfp : labelFieldParts) if(!parsedProblem.fieldDomains.has(lfp)) parsedProblem.addFieldDomain(lfp);
                parsedProblem.addLabel(labelName, labelFromDomain, labelToDomain, labelFieldParts);
            } catch (CFLRException e) {
                errors.add(e.getMessage());
            }
        }

        @Override
        public void enterRuleDef(SpecificationParser.RuleDefContext ctx) {

        }

        @Override
        public void exitRuleDef(SpecificationParser.RuleDefContext ctx) {

        }

        @Override
        public void enterUnitExpr(SpecificationParser.UnitExprContext ctx) {

        }

        @Override
        public void exitUnitExpr(SpecificationParser.UnitExprContext ctx) {

        }

        @Override
        public void enterChainExpr(SpecificationParser.ChainExprContext ctx) {

        }

        @Override
        public void exitChainExpr(SpecificationParser.ChainExprContext ctx) {

        }

        @Override
        public void enterEpsilonTerm(SpecificationParser.EpsilonTermContext ctx) {

        }

        @Override
        public void exitEpsilonTerm(SpecificationParser.EpsilonTermContext ctx) {

        }

        @Override
        public void enterNegateTerm(SpecificationParser.NegateTermContext ctx) {

        }

        @Override
        public void exitNegateTerm(SpecificationParser.NegateTermContext ctx) {

        }

        @Override
        public void enterReverseTerm(SpecificationParser.ReverseTermContext ctx) {

        }

        @Override
        public void exitReverseTerm(SpecificationParser.ReverseTermContext ctx) {

        }

        @Override
        public void enterLabelTerm(SpecificationParser.LabelTermContext ctx) {

        }

        @Override
        public void exitLabelTerm(SpecificationParser.LabelTermContext ctx) {

        }

        @Override
        public void enterIntersectTerm(SpecificationParser.IntersectTermContext ctx) {

        }

        @Override
        public void exitIntersectTerm(SpecificationParser.IntersectTermContext ctx) {

        }

        @Override
        public void enterSubTerm(SpecificationParser.SubTermContext ctx) {

        }

        @Override
        public void exitSubTerm(SpecificationParser.SubTermContext ctx) {

        }

        @Override
        public void enterLabelDef(SpecificationParser.LabelDefContext ctx) {
        }

        @Override
        public void exitLabelDef(SpecificationParser.LabelDefContext ctx) {
        }

        @Override
        public void enterLabelUse(SpecificationParser.LabelUseContext ctx) {

        }

        @Override
        public void exitLabelUse(SpecificationParser.LabelUseContext ctx) {
        }

        private String labelName;
        private List<String> labelFieldParts;
        @Override
        public void enterLabel(SpecificationParser.LabelContext ctx) {
            labelName = ctx.ID().getText();
            labelFieldParts = new ArrayList<>();
        }

        @Override
        public void exitLabel(SpecificationParser.LabelContext ctx) {
        }

        @Override
        public void enterDomain(SpecificationParser.DomainContext ctx) {
            String txt = ctx.ID().getText();
            if(labelFromDomain == null) labelFromDomain = txt;
            else labelToDomain = txt;
        }

        @Override
        public void exitDomain(SpecificationParser.DomainContext ctx) {

        }

        @Override
        public void enterField(SpecificationParser.FieldContext ctx) {
            String txt = ctx.ID().getText();
            labelFieldParts.add(txt);
        }

        @Override
        public void exitField(SpecificationParser.FieldContext ctx) {

        }

        @Override
        public void visitTerminal(TerminalNode node) {

        }

        @Override
        public void visitErrorNode(ErrorNode node) {
            errors.add(node.toString());
        }

        @Override
        public void enterEveryRule(ParserRuleContext ctx) {

        }

        @Override
        public void exitEveryRule(ParserRuleContext ctx) {

        }
    }

    public static void main(String[] args){
        try {
            new AntlrParser().parse(Arrays.stream(args).collect(Collectors.joining(" ")));
        } catch (CFLRException e) {
            e.printStackTrace();
        }
    }
}
