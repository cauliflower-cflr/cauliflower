package cauliflower.application;

import cauliflower.parser.CFLRParser;
import cauliflower.parser.ParseFile;
import cauliflower.parser.SimpleParser;
import cauliflower.util.Logs;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Optimiser
 * <p>
 * Given a Cauliflower specification, continually runs and
 * analyses the generated code, using feedback to refine the
 * specification and improve performance.
 * <p>
 * Author: nic
 * Date: 24/05/16
 */
public class Optimiser {

    public final File inputSpec;
    public final File optimisedSpec;
    public final List<File> trainingSet;

    public Optimiser(String srcSpec, String targetSpec, List<String> trainingSet) {
        this.inputSpec = new File(srcSpec);
        this.optimisedSpec = new File(targetSpec);
        this.trainingSet = trainingSet.stream().map(File::new).collect(Collectors.toList());
    }

    public void optimise() throws IOException, Configuration.HelpException, Configuration.ConfigurationException, InterruptedException {
        File curSpec = inputSpec;
        boolean going = true;
        int optimisationRound = 0;
        while (going) {
            Path workingDir = Files.createTempDirectory("cauli_opt_" + optimisationRound);
            Logs.forClass(Optimiser.class).debug("Iteration {}, working dir: {}", optimisationRound, workingDir);
            File exef = new File(workingDir.toFile(), "opt_exe");
            Configuration curConf = Configuration.fromArgs("-p", "-r", "-t", "--compile", exef.getAbsolutePath(), curSpec.getAbsolutePath());
            CFLRParser.ParserOutputs curParse = new ParseFile(new SimpleParser()).read(curSpec);
            Compiler comp = new Compiler(exef.getAbsolutePath(), curConf);
            comp.compile(curParse);
            for (int i = 0; i < trainingSet.size(); i++) {
                File logf = new File(workingDir.toFile(), optimisationRound + "_" + i + ".log");
                Logs.forClass(Optimiser.class).debug("Logging {} from {}", logf, trainingSet.get(i));

                // continually try to run until we succeed, arbitrarily stop after 5
                ProcessBuilder pb = new ProcessBuilder(exef.getAbsolutePath(), trainingSet.get(i).getAbsolutePath())
                        .redirectErrorStream(true).redirectOutput(logf);
                pb.environment().put("OMP_NUM_THREADS", "1");
                int code = -1;
                int count = 0;
                while (code != 0 && count < 5) {
                    Process proc = pb.start();
                    code = proc.waitFor();
                    count++;
                    Logs.forClass(Optimiser.class).trace("attempt {} has exit code {}", count, code);
                }
                if (code != 0) throw new IOException("Failed to successfully execute test-case " + trainingSet.get(i));
            }
            going = false;
        }

    }

}
