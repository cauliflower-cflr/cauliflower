package cauliflower.parser;

import cauliflower.parser.grammar.SpecificationBaseVisitor;
import cauliflower.parser.grammar.SpecificationLexer;
import cauliflower.parser.grammar.SpecificationParser;
import cauliflower.representation.Problem;
import cauliflower.util.CFLRException;
import cauliflower.util.Logs;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.BitSet;
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
        parseError = null;
        SpecificationLexer lex = new SpecificationLexer(ais);
        SpecificationParser par = new SpecificationParser(new CommonTokenStream(lex));
        par.setErrorHandler(new BailErrorStrategy());
        par.removeErrorListeners();
        par.addErrorListener(this);
        try {
            SpecificationParser.SpecContext spec = par.spec();
            Logs.forClass(AntlrParser.class).debug("Parsed: {}", new SpecificationUtils.PrettyPrinter(" ", "   ").visit(spec).toString());
            ProblemBuilder pb = new ProblemBuilder(spec);
            return pb.po;
        } catch(Exception exc) {
            // TODO error handling that is actually helpful
            if(parseError != null) throw new CFLRException(parseError);
            else throw new CFLRException(exc.toString() + ", Unexpected end of input");
        }
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

    private static class ProblemBuilder extends SpecificationBaseVisitor<Object> {
        public final Problem prob;
        public final ParserOutputs po;
        public ProblemBuilder(SpecificationParser.SpecContext ctx){
            this.prob = new Problem();
            this.po = SpecificationUtils.Auditor.getRegistrars(ctx);
            //init the problem with registered domains
            for(int vd=0; vd<po.vertexDomains.size(); vd++) prob.vertexDomains.add(prob.new Domain(vd, po.vertexDomains.fromIndex(vd)));
            for(int fd=0; fd<po.fieldDomains.size(); fd++) prob.fieldDomains.add(prob.new Domain(fd, po.fieldDomains.fromIndex(fd)));
            this.visit(ctx);
        }
        @Override
        public Object visitTypeDef(SpecificationParser.TypeDefContext ctx) {
            new LabelBuilder().visit(ctx);
            return null;
        }
        @Override
        public Object visitRuleDef(SpecificationParser.RuleDefContext ctx) {
            return null;
        }
        private class LabelBuilder extends SpecificationBaseVisitor<Object>{

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
