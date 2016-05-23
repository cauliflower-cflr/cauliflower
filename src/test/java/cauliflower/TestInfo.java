package cauliflower;

import cauliflower.application.Info;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertTrue;

public class TestInfo {

    @Test
    public void testLibsDirContainsCmake(){
        File tst = new File(Info.cauliDistributionDirectory, "CMakeLists.txt");
        assertTrue(tst.exists());
        assertTrue(tst.isFile());
    }

    @Test
    public void testLibsDirContainsInclude(){
        File tst = new File(Info.cauliDistributionDirectory, "include");
        assertTrue(tst.exists());
        assertTrue(tst.isDirectory());
    }

}
