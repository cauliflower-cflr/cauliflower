package cauliflower.optimiser;

import cauliflower.application.CauliflowerException;
import cauliflower.application.Task;
import cauliflower.representation.Problem;
import cauliflower.util.Logs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Controller
 * <p>
 * Author: nic
 * Date: 8/07/16
 */
public class Controller implements Task<Problem> {

    private final int maxRounds;
    private final List<Path> trainingSet;
    private Path workingDir;

    public Controller(int maxRounds, List<Path> trainingSet) {
        this.maxRounds = maxRounds;
        this.trainingSet = trainingSet;
    }

    @Override
    public Problem perform(Problem inputSpec) throws CauliflowerException {
        this.workingDir = null;
        try {
            this.workingDir = Files.createTempDirectory("cauli_opt_");// + inputSpec.getFileName().toString());
        } catch (IOException e) {
            except(e);
        }
        int optimisationRound = 0;
        Problem curSpec = inputSpec;
        while (optimisationRound < maxRounds) {
            Logs.forClass(this.getClass()).trace("Round {}", optimisationRound);
            try {
                Pass pass = new Pass(this, optimisationRound, Arrays.asList(
                        new CommonSubexpressionTransformation(),
                        new RelationFilterTransformation(),
                        new EvaluationOrderTransformation(true, true)
                ));
                Optional<Problem> nextSpec = pass.perform(curSpec);
                if(nextSpec.isPresent()) curSpec = nextSpec.get();
                else break;
            } catch (IOException e) {
                except(e);
            }
            optimisationRound++;
        }
        //TODO return the best spec, not the most recent one
        return curSpec;
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
