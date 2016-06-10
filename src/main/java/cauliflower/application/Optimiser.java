package cauliflower.application;

import cauliflower.util.Logs;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
            OptimisationPass pass = new OptimisationPass(optimisationRound, curSpec);
            pass.compileExe();
            pass.generateLogs();
            pass.annotateParse();
            going = false;
        }
    }

    /**
     * Contains the necessary state information for a single optimisation pass
     */
    private class OptimisationPass {

        private final int round;
        private final File spec;
        private final File workingDir;
        private final File executable;
        private List<File> logs;

        private OptimisationPass(int roundNumber, File currentSpecification) throws IOException {
            round = roundNumber;
            spec = currentSpecification;
            workingDir = Files.createTempDirectory("cauli_opt_" + round + "_").toFile();
            executable = new File(workingDir, "opt_" + round + "_exe");
            logs = IntStream.rangeClosed(0, trainingSet.size()).mapToObj(i -> new File(workingDir, round + "_" + i + ".log")).collect(Collectors.toList());
            Logs.forClass(Optimiser.class).debug("Round {}, Dir: {}", round, workingDir.getAbsolutePath());
        }

        private void compileExe() throws Configuration.HelpException, Configuration.ConfigurationException, IOException, InterruptedException {
            Configuration curConf = Configuration.fromArgs("-p", "-r", "-t", "--compile", executable.getAbsolutePath(), spec.getAbsolutePath());
            Compiler comp = new Compiler(executable.getAbsolutePath(), curConf);
            comp.compile();
        }

        private void generateLogs() throws IOException, InterruptedException {
            logs = new ArrayList<>();
            int i=0;
            for(File trainingDir : trainingSet){
                File curLog = new File(workingDir, round + "_" + i++ + "_" + trainingDir.getName() + ".log");
                Logs.forClass(Optimiser.class).debug("Logging {} from {}", curLog, trainingDir);
                ProcessBuilder pb = new ProcessBuilder(executable.getAbsolutePath(), trainingDir.getAbsolutePath())
                        .redirectErrorStream(true).redirectOutput(curLog);
                pb.environment().put("OMP_NUM_THREADS", "1");
                int code = -1;
                int count = 0;
                while (code != 0 && count < 5) {
                    Process proc = pb.start();
                    code = proc.waitFor();
                    count++;
                    Logs.forClass(Optimiser.class).trace("attempt {} has exit code {}", count, code);
                }
                if (code != 0) throw new IOException("Failed to generate a log for " + trainingDir);
                logs.add(curLog);
            }
        }

        private void annotateParse() {

        }

    }

}
