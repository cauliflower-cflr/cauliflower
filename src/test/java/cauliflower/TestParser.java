package cauliflower;

import cauliflower.parser.CFLRParser;
import cauliflower.parser.SimpleParser;
import cauliflower.util.CFLRException;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.fail;

public class TestParser {

    private CFLRParser.ParserOutputs parseSpec(String spec) throws CFLRException{
        InputStream stream = new ByteArrayInputStream(spec.getBytes(StandardCharsets.UTF_8));
        return new SimpleParser().parse(stream);
    }

    @Test
    public void testEmptySpecIsValid(){
        try{
            parseSpec("");
        } catch (Exception exc) {
            fail("unexpected exception");
        }
    }

    @Test
    public void testValidTypedecl(){
        try{
            parseSpec("a<-b.c;");
        } catch (Exception exc) {
            fail("unexpected exception");
        }
    }

    @Test
    public void testValidRule(){
        try{
            parseSpec("a<-b.c;a->a,a;");
        } catch (Exception exc) {
            fail("unexpected exception");
        }
    }

}
