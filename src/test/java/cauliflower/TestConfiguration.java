package cauliflower;

import cauliflower.application.Configuration;
import cauliflower.generator.Adt;
import org.junit.Test;
import org.omg.IOP.ExceptionDetailMessage;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TestConfiguration {

    @Test
    public void testHelpOption(){
        try {
            Configuration.fromArgs("-h");
            fail();
        } catch(Configuration.HelpException he){
            // do nothing
        }catch(Exception e){
            fail();
        }
        try {
            Configuration.fromArgs("--help");
            fail();
        } catch(Configuration.HelpException he){
            // do nothing
        }catch(Exception e){
            fail();
        }
    }

    @Test
    public void testSpecFile(){
        // zero files bad
        try {
            Configuration.fromArgs();
            fail();
        } catch (Exception e) {
            // do nothing
        }
        // one file good
        try {
            Configuration cfg = Configuration.fromArgs("foo");
            assertTrue(cfg.specFile.size() == 1);
            assertTrue(cfg.specFile.get(0).equals("foo"));
        } catch (Exception e) {
            fail();
        }
        // two files bad
        try {
            Configuration.fromArgs("a", "b");
            fail();
        } catch (Exception e) {
            // do nothing
        }
    }

    @Test
    public void testNonExistantArgThrowsError(){
        try{
            Configuration.fromArgs("-foo bar");
            fail();
        } catch(Exception exc){
            // do nothing
        }
    }

    @Test
    public void testAdtArguments(){
        //all valid ADTs should succeed
        try{
            for(Adt a : Adt.values()){
                Configuration.fromArgs("-a", a.name(), "foo");
            }
        } catch(Exception exc){
            fail();
        }
        // non-adt should cause an error
        try {
            Configuration.fromArgs("-a", "ThisIsNotAnAdt", "foo");
            fail();
        } catch(Configuration.ConfigurationException exc){
            // do nothing
        } catch(Exception exc){
            fail();
        }
    }

    @Test
    public void testCompileOverridesGenerators(){
        try{
            Configuration.fromArgs("-cs", "bar", "-c", "baz", "foo");
            fail();
        } catch(Exception exc){
            //good
        }

        try{
            Configuration.fromArgs("--compile", "baz", "-sn", "bar", "foo");
            fail();
        } catch(Exception exc){
            //good
        }

        try{
            Configuration.fromArgs("-c", "baz", "foo");
        } catch(Exception exc){
            fail();
        }

        try{
            Configuration.fromArgs("--compile", "baz", "foo");
        } catch(Exception exc){
            fail();
        }
    }

}
