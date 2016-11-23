package cauliflower.optimiser;

import cauliflower.application.CauliflowerException;
import cauliflower.application.Compiler;
import cauliflower.application.Task;
import cauliflower.generator.Verbosity;
import cauliflower.representation.Problem;
import cauliflower.util.Logs;
import cauliflower.util.Pair;
import cauliflower.util.Streamer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Pass
 * Used to create a profile of the current specification
 * <p>
 * Author: nic
 * Date: 8/07/16
 */
public class Pass implements Task<Profile> {

    private final int round;
    private final Controller parent;
    private final Path executable;
    private List<Profile> profiles;

    /*local*/ Pass(Controller context, int roundNumber) throws IOException {
        round = roundNumber;
        parent = context;
        executable = parent.getExeFileForRound(round);
        profiles = null;
    }

    @Override
    public Profile perform(Problem spec) throws CauliflowerException {
        compileExe(spec);
        generateLogs();
        Profile prof = profileLogs();
        return prof;
    }

    private void compileExe(Problem spec) throws CauliflowerException{
        Compiler comp = new Compiler(executable.getFileName().toString(), executable.getParent(), false, new Verbosity(true, true, true, false));
        comp.perform(spec);
    }

    private void generateLogs() throws CauliflowerException{
        profiles = Streamer.enumerate(parent.trainingSetStream(),
                (tset, idx) -> new Pair<>(tset, parent.getLogFileForRound(round, idx)))
                .map(p -> {
                    try {
                        return generateLog(p.first, p.second);
                    } catch (Exception e) {
                        Logs.forClass(Pass.class).error("Creating log " + p.second + " failed", e);
                        return null;
                    }
                })
                .collect(Collectors.toList());
        if(profiles.stream().anyMatch(p -> p == null)) except("Failed to generate logs.");
    }

    private Profile generateLog(Path trainingDir, Path logFile) throws IOException, InterruptedException {
        Logs.forClass(Pass.class).debug("Logging {} from {}", logFile, trainingDir);
        ProcessBuilder pb = new ProcessBuilder(executable.toString(), trainingDir.toString())
                .redirectErrorStream(true).redirectOutput(logFile.toFile());
        pb.environment().put("OMP_NUM_THREADS", "1"); // force profiling for a single core
        int code = -1;
        int count = 0;
        while (code != 0 && count < 5) {
            Process proc = pb.start();
            code = proc.waitFor();
            count++;
            Logs.forClass(Pass.class).trace("attempt {} has exit code {}", count, code);
        }
        if (code != 0){
            throw new IOException("Failed to generate a log for " + trainingDir.toString());
        }
        return new Profile(logFile);
    }

    private Profile profileLogs(){
        return Profile.sumOfProfiles(profiles);
    }
}
