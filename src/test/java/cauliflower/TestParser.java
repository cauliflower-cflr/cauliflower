package cauliflower;

import cauliflower.parser.AntlrParser;
import cauliflower.representation.Clause;
import cauliflower.representation.LabelUse;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TestParser {

    @Test
    public void testEmptySpecIsValid(){
        try{
            new AntlrParser().parse("");
        } catch (Exception exc) {
            fail(exc.getMessage());
        }
    }

    @Test
    public void testValidTypedecl(){
        try{
            new AntlrParser().parse("a<-b.c;");
        } catch (Exception exc) {
            fail(exc.getMessage());
        }
    }

    @Test
    public void testValidRule(){
        try{
            new AntlrParser().parse("a<-a.a;a->a;");
        } catch (Exception exc) {
            fail(exc.getMessage());
        }
    }

    @Test
    public void testChainIsValid(){
        try{
            new AntlrParser().parse("a<-a.a;a->a,a;");
        } catch (Exception exc) {
            fail(exc.getMessage());
        }
    }

    @Test
    public void testNegateIsValid(){
        try{
            new AntlrParser().parse("a<-a.a;a->!a;");
        } catch (Exception exc) {
            fail(exc.getMessage());
        }
    }

    @Test
    public void testReverseIsValid(){
        try{
            new AntlrParser().parse("a<-a.a;a->-a;");
        } catch (Exception exc) {
            fail(exc.getMessage());
        }
    }

    @Test
    public void testSubIsValid(){
        try{
            new AntlrParser().parse("a<-a.a;a->(a);");
        } catch (Exception exc) {
            fail(exc.getMessage());
        }
    }

    @Test
    public void testEpsilonIsValid(){
        try{
            new AntlrParser().parse("a<-a.a;a->~;");
        } catch (Exception exc) {
            fail(exc.getMessage());
        }
    }

    @Test
    public void testComplex(){
        try{
            new AntlrParser().parse("x<-a.a;y[f]<-a.a;z<-a.a;x->(-z & !y[b]),(x,y[a]);");
        } catch (Exception exc) {
            exc.printStackTrace();
            fail(exc.getMessage());
        }
    }

    /**
     * SYNTAX ERRORS
     */

    @Test
    public void testTypeSyntaxError(){
        try{
            new AntlrParser().parse("a->b.c;");
            fail();
        } catch (Exception exc) {
            // good
        }
    }

    @Test
    public void testRuleSyntaxError(){
        try{
            new AntlrParser().parse("a<-b,c;");
            fail();
        } catch (Exception exc) {
            // good
        }
    }

    /**
     * SEMANTIC ERRORS
     */

    @Test
    public void testUndeclaredLabelError(){
        try{
            new AntlrParser().parse("a->a;");
            fail();
        } catch (Exception exc) {
            // good
        }
    }

    @Test
    public void testFieldSizeMismatches(){
        try{
            new AntlrParser().parse("a<-x.x;a[f]->a;");
            fail();
        } catch (Exception exc) {
            // good
        }
        try{
            new AntlrParser().parse("a<-x.x;a->a[f];");
            fail();
        } catch (Exception exc) {
            // good
        }
        try{
            new AntlrParser().parse("a[b]<-x.x;a[f]->a;");
            fail();
        } catch (Exception exc) {
            // good
        }
        try{
            new AntlrParser().parse("a[b]<-x.x;a->a[f];");
            fail();
        } catch (Exception exc) {
            // good
        }
    }

    Clause.InOrderVisitor<LabelUse> labelGatherer = new Clause.InOrderVisitor<>(new Clause.VisitorBase<LabelUse>(){
        @Override
        public LabelUse visitLabelUse(LabelUse lu){
            return lu;
        }
    });

    @Test
    public void testPriority(){
        List<LabelUse> uses = labelGatherer.visitAllNonNull(Utilities.parseOrFail("a<-x.x;a->a,a,a;").getRule(0).ruleBody);
        assertEquals(0, uses.get(0).priority);
        assertEquals(0, uses.get(1).priority);
        assertEquals(0, uses.get(2).priority);
        uses = labelGatherer.visitAllNonNull(Utilities.parseOrFail("a<-x.x;a->a{0},a{1},a{-1};").getRule(0).ruleBody);
        assertEquals(0, uses.get(0).priority);
        assertEquals(1, uses.get(1).priority);
        assertEquals(-1, uses.get(2).priority);
    }

}
