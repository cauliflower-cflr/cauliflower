package cauliflower;

import cauliflower.application.Cauliflower;
import cauliflower.util.FileSystem;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.assertTrue;
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
                .collect(Collectors.toList());
    }

    @BeforeClass
    public static void initTestCauliflower() throws Exception{
        scratchpad = Files.createTempDirectory("cauliflower_test_");
    }

    @AfterClass
    public static void destroyTestCauliflower() throws Exception{
        //FileSystem.recursiveRemove(scratchpad); TODO uncomment me
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
                Cauliflower.main(new String[]{"-p", "--compile", exeFile.getAbsolutePath(), specFile.getAbsolutePath()});
            } catch(Exception exc){
                fail();
            }
        }

        try{
            // for each known correct answer
            Files.list(testDir.toPath()).filter(p -> p.toString().endsWith(".ans")).forEach(p -> {
                // determine the target relation
                String rel = p.toFile().getName().substring(0, p.toFile().getName().length()-4);
                try {
                    //run the exe in a process
                    ProcessBuilder pb = new ProcessBuilder(exeFile.getAbsolutePath(), testDir.getAbsolutePath(), rel)
                            .redirectErrorStream(true);
                    Process proc = pb.start();
                    //get the calculated answer
                    List<String> outp = captureOutput(proc.getInputStream(), true);
                    assertTrue(proc.waitFor() == 0);
                    //get the correct answer
                    List<String> answ = captureOutput(new FileInputStream(p.toFile()), false);
                    //compare the answers
                    assertTrue(answ.size() == outp.size());
                    for(int i=0; i<answ.size(); i++){
                        assertTrue(outp.get(i).equals(answ.get(i)));
                    }
                } catch (Exception e) {
                    fail();
                }
            });
        } catch(Exception exc){
            fail();
        }
    }

    // helper method to turn an input stream into a list of lines
    // skip the first line (caulflower outputs this) if necessary
    private List<String> captureOutput(InputStream in, boolean skip) throws IOException{
        Scanner sca = new Scanner(in);
        List<String> ret = new ArrayList<>();
        while(sca.hasNextLine()){
            String ln = sca.nextLine();
            if(!skip) ret.add(ln);
            skip = false;
        }
        sca.close();
        in.close();
        return ret.stream().sorted().distinct().collect(Collectors.toList());
    }

}
