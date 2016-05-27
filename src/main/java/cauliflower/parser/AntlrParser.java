package cauliflower.parser;

import cauliflower.parser.grammar.SpecificationLexer;
import cauliflower.parser.grammar.SpecificationParser;
import cauliflower.util.CFLRException;
import cauliflower.util.Logs;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;

import java.io.IOException;
import java.io.InputStream;

/**
 * AntlrParser
 * <p>
 * Author: nic
 * Date: 27/05/16
 */
public class AntlrParser implements CFLRParser {

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
        SpecificationLexer lex = new SpecificationLexer(ais);
        SpecificationParser par = new SpecificationParser(new CommonTokenStream(lex));
        Logs.forClass(AntlrParser.class).debug("Parsed: {}", new SpecificationUtils.PrettyPrinter(" ", "   ").visit(par.spec()).toString());
        ParserOutputs po = SpecificationUtils.Auditor.getRegistrars(par.spec());
        Logs.forClass(AntlrParser.class).trace("Labels: {}", po.labelNames);
        Logs.forClass(AntlrParser.class).trace("Verts:  {}", po.vertexDomains);
        Logs.forClass(AntlrParser.class).trace("Fields: {}", po.fieldDomains);
        Logs.forClass(AntlrParser.class).trace("RFIs:   {}", po.ruleFields);
        return po;
    }

}
