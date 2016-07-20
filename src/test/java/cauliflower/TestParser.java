package cauliflower;

import cauliflower.parser.AntlrParser;
import cauliflower.util.CFLRException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class TestParser {

    @Parameterized.Parameters(name = "{index}: {1}")
    public static Collection<Object[]> cases() throws IOException {
        return Arrays.asList(
                // general grammar
                new Object[]{true,"empty specification",""},
                new Object[]{true,"type declaration","a<-b.c;"},
                new Object[]{true,"rule declaration","a<-a.a;a->a;"},
                // different clauses
                new Object[]{true,"chain","a<-a.a;a->a,a;"},
                new Object[]{true,"negation","a<-a.a;a->!a;"},
                new Object[]{true,"reversal","a<-a.a;a->-a;"},
                new Object[]{true,"subexpression","a<-a.a;a->(a);"},
                new Object[]{true,"epsilon","a<-a.a;a->~;"},
                new Object[]{true,"complex expression","x<-a.a;y[f]<-a.a;z<-a.a;x->(-z & !y[b]),(x,y[a]);"},
                // syntax errors
                new Object[]{false,"missing semicolon","a<-b.c"},
                new Object[]{false,"incorrect type-def","a->b.c;"},
                new Object[]{false,"incorrect rule-def","a<-b,c;"},
                // type errors
                new Object[]{false,"missing type-def","a->a;"},
                new Object[]{false,"write to non-existing field","a<-x.x;a[f]->a;"},
                new Object[]{false,"read from non-existing field","a<-x.x;a->a[f];"},
                new Object[]{false,"read without necessary field","a[b]<-x.x;a[f]->a;"},
                new Object[]{false,"write without necessary field","a[b]<-x.x;a->a[f];"}
        );
    }

    @Parameterized.Parameter(value = 0)
    public boolean shouldSucceed;
    @Parameterized.Parameter(value = 1)
    public String reason;
    @Parameterized.Parameter(value = 2)
    public String spec;

    @Test
    public void testAntlrParse(){
        try {
            new AntlrParser().parse(spec);
            assertTrue("\"" + spec + "\" should succeed because the" + reason + " is valid", shouldSucceed);
        } catch (CFLRException e) {
            assertFalse("\"" + spec + "\" should fail because the" + reason + " is invalid", shouldSucceed);
        }
    }

}
