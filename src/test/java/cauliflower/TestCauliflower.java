package cauliflower;

import cauliflower.application.Cauliflower;
import cauliflower.util.FileSystem;
import cauliflower.util.Logs;
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
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(Parameterized.class)
public class TestCauliflower {

    @Parameterized.Parameters(name = "{index}: {1} - {2}")
    public static Collection<Object[]> cases() throws IOException{
        return Files.list(Paths.get("src", "test", "examples")) // for each example problem
                .flatMap(p -> {
                    try {
                        return Files.list(p);
                    } catch (IOException e) {
                        e.printStackTrace();
                        return null;
                    }
                })
                .filter(p -> p.getFileName().toString().endsWith(".cflr"))
                .filter(p -> p.getFileName().toString().equals("epsilonic.cflr"))
                .flatMap(f -> {
                    try {
                        return Files.list(f.getParent())
                                .filter(p -> Files.isDirectory(p))
                                .map(p -> new Object[]{f.getParent(), f.getFileName().toString(), p.getFileName().toString()});
                    } catch (IOException e) {
                        e.printStackTrace();
                        return null;
                    }
                })
                .collect(Collectors.toList());
    }

    @BeforeClass
    public static void initTestCauliflower() throws Exception{
        scratchpad = Files.createTempDirectory("cauliflower_test_");
    }

    @AfterClass
    public static void destroyTestCauliflower() throws Exception{
        FileSystem.recursiveRemove(scratchpad);
    }

    private static Path scratchpad;


    public final Path problemFile;
    public final Path sampleDir;

    public TestCauliflower(Path dir, String probName, String sampleName){
        problemFile = Paths.get(dir.toString(), probName);
        sampleDir = Paths.get(dir.toString(), sampleName);
    }


    @Test
    public void testCompilation(){
        // build the exe if it does not exist
        File exeFile = new File(scratchpad.toFile(), problemFile.getFileName().toString().substring(0, problemFile.getFileName().toString().lastIndexOf(".")));
        if(! exeFile.exists()){
            try{
                Cauliflower.main(new String[]{"-p", "--compile", "--output-dir", exeFile.getParentFile().getAbsolutePath(), "--name", exeFile.getName(), problemFile.toAbsolutePath().toString()});
            } catch(Exception exc){
                fail();
            }
        }

        try{
            // for each known correct answer
            Files.list(sampleDir).filter(p -> p.toString().endsWith(".ans")).forEach(p -> {
                Logs.forClass(TestCauliflower.class).trace("Checking {}", p);
                // determine the target relation
                String rel = p.toFile().getName().substring(0, p.toFile().getName().length()-4);
                try {
                    //run the exe in a process
                    ProcessBuilder pb = new ProcessBuilder(exeFile.getAbsolutePath(), sampleDir.toAbsolutePath().toString(), rel)
                            .redirectErrorStream(true);
                    Process proc = pb.start();
                    //get the calculated answer
                    List<String> outp = captureOutput(proc.getInputStream(), true);
                    assertTrue(proc.waitFor() == 0);
                    //get the correct answer
                    List<String> answ = captureOutput(new FileInputStream(p.toFile()), false);
                    //compare the answers
                    assertThat(outp, is(answ));
                    assertThat("Incorrect size of output set " + p.getFileName(), outp.size(), is(answ.size()));
                    for(int i=0; i<answ.size(); i++){
                        assertThat("Outputs should be identical in " + p.getFileName(), outp.get(i), is(answ.get(i)));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
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
