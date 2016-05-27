package cauliflower;

import cauliflower.parser.AntlrParser;
import org.junit.Test;

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
            new AntlrParser().parse("a<-b.c;a->a,a;");
        } catch (Exception exc) {
            fail(exc.getMessage());
        }
    }

}
