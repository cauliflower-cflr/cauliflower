package cauliflower.optimiser;

import cauliflower.application.Configuration;
import cauliflower.util.Logs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

/**
 * Controller
 * <p>
 * Author: nic
 * Date: 8/07/16
 */
public class Controller {

    private final Path inputSpec;
    private final Path optimisedSpec;
    private final Path workingDir;
    private final List<Path> trainingSet;

    public Controller(Path srcSpec, Path targetSpec, List<Path> trainingSet) throws IOException {
        this.inputSpec = srcSpec;
        this.optimisedSpec = targetSpec;
        this.trainingSet = trainingSet;
        workingDir = Files.createTempDirectory("cauli_opt_" + inputSpec.getFileName().toString());
    }

    public void optimise(int maxRounds) throws IOException, InterruptedException {
        Files.copy(inputSpec, getSpecFileForRound(0));
        boolean going = true;
        int optimisationRound = 0;
        while (going && optimisationRound < maxRounds) {
            Logs.forClass(this.getClass()).trace("Round {}", optimisationRound);
            Pass pass = new Pass(this, optimisationRound);
            pass.compileExe();
            pass.generateLogs();
            pass.annotateParse();
            optimisationRound++;
        }
    }

    /*local*/ Stream<Path> trainingSetStream(){
        return trainingSet.stream();
    }

    /*local*/ Path getSpecFileForRound(int round){
        return Paths.get(workingDir.toString(), "r" + round + "_spec.cfg");
    }

    /*local*/ Path getExeFileForRound(int round){
        return Paths.get(workingDir.toString(), "r" + round + "_exe");
    }

    /*local*/ Path getLogFileForRound(int round, int ts){
        return Paths.get(workingDir.toString(), "r" + round + "_out_" + ts + ".log");
    }
}
