package cauliflower;

import cauliflower.application.Configuration;
import cauliflower.generator.Adt;
import org.junit.Test;

import java.nio.file.Paths;

import static org.junit.Assert.*;

public class TestConfiguration {

    @Test
    public void testHelpOption(){
        try {
            new Configuration("-h");
            fail();
        } catch(Configuration.HelpException he){
            // do nothing
        }catch(Exception e){
            fail();
        }
        try {
            new Configuration("--help");
            fail();
        } catch(Configuration.HelpException he){
            // do nothing
        }catch(Exception e){
            fail();
        }
    }

    @Test
    public void testNoSpecErrors(){
        // zero files bad
        try {
            new Configuration();
            fail();
        } catch (Exception e) {
            // do nothing
        }
    }

    @Test
    public void testNonExistantArgThrowsError(){
        try{
            new Configuration("-foo bar");
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
                new Configuration("-a", a.name(), "foo");
            }
        } catch(Exception exc){
            fail();
        }
        // non-adt should cause an error
        try {
            new Configuration("-a", "ThisIsNotAnAdt", "foo");
            fail();
        } catch(Configuration.ConfigurationException exc){
            // do nothing
        } catch(Exception exc){
            fail();
        }
    }

    @Test
    public void testOutputName() throws Configuration.HelpException, Configuration.ConfigurationException {
        assertEquals(Paths.get("foo"), new Configuration("foo.cflr").outputBase);
        assertEquals(Paths.get("foo"), new Configuration("foo.").outputBase);
        assertEquals(Paths.get("foo"), new Configuration("foo").outputBase);
        assertEquals(Paths.get("x/y/foo"), new Configuration("x/y/foo.cflr").outputBase);
        assertEquals(Paths.get("x/y/foo"), new Configuration("x/y/foo.").outputBase);
        assertEquals(Paths.get("x/y/foo"), new Configuration("x/y/foo").outputBase);
    }

    @Test
    public void testRenameOutput() throws Configuration.HelpException, Configuration.ConfigurationException {
        assertEquals(Paths.get("bar"), new Configuration("-n", "bar", "foo.cflr").outputBase);
        assertEquals(Paths.get("bar"), new Configuration("--name", "bar", "foo.cflr").outputBase);
        assertEquals(Paths.get("bar.cflr"), new Configuration("--name", "bar.cflr", "foo.cflr").outputBase);
        assertEquals(Paths.get("x/y/z/bar"), new Configuration("-n", "x/y/z/bar", "foo.cflr").outputBase);
        assertEquals(Paths.get("x/y/z/bar.cflr"), new Configuration("-n", "x/y/z/bar.cflr", "foo.cflr").outputBase);
    }

}
