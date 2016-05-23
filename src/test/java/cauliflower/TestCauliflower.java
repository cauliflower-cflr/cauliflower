package cauliflower;

import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import static org.junit.Assert.fail;

@RunWith(Parameterized.class)
public class TestCauliflower {

    @Parameterized.Parameters
    public static Collection<Object[]> cases() throws IOException{
        return Files.list(new File("src/test/examples/").toPath()) // for each example problem
                .map(Path::toFile) // as a file
                .filter(File::isDirectory) // find the directories
                .flatMap(d -> Arrays.stream(d.listFiles())) // and for each of their files
                .filter(f -> f.getName().endsWith(".cflr")) // choose the Cauliflower problems
                .flatMap(p -> Arrays.stream(p.getParentFile().listFiles())
                        .filter(File::isDirectory)
                        .map(d -> new File[]{p, d}))
                .limit(1)
                .collect(Collectors.toList());
    }

    @BeforeClass
    public static void initTestCauliflower() throws Exception{
        scratchpad = Files.createTempDirectory("cauliflower_test_");
    }

    @AfterClass
    public static void destroyTestCauliflower() throws Exception{
        // relying on the (probably bad) delete-on-exit
    }

    private static Path scratchpad;

    @Parameterized.Parameter
    public File specFile;
    @Parameterized.Parameter(value = 1)
    public File testDir;

    @Test
    public void testCompilation(){
        // build the exe if it does not exist
        File exeFile = new File(scratchpad.toFile(), specFile.getName().substring(0, specFile.getName().lastIndexOf(".")));
        if(! exeFile.exists()){
            try{
                Main.main(new String[]{"--compile", exeFile.getAbsolutePath(), specFile.getAbsolutePath()});
            } catch(Exception exc){
                fail();
            }
        }

        //run the exe in a process TODO
        new ProcessBuilder(exeFile.getAbsolutePath());
        System.out.println(specFile.getPath() + " - " + testDir.getPath());
    }

}
