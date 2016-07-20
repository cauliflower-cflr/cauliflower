package cauliflower.optimiser;

import cauliflower.application.CauliflowerException;
import cauliflower.application.Task;
import cauliflower.generator.CauliflowerSpecification;
import cauliflower.generator.Verbosity;
import cauliflower.representation.Problem;
import cauliflower.util.Logs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
        Problem curSpec = inputSpec;
        try {
            this.workingDir = null;
            this.workingDir = Files.createTempDirectory("cauli_opt_");// + inputSpec.getFileName().toString());
            int optimisationRound = 0;
            while (optimisationRound < maxRounds) {
                Logs.forClass(this.getClass()).trace("Round {}", optimisationRound);
                new CauliflowerSpecification(getSpecFileForRound(optimisationRound), new Verbosity()).perform(curSpec);
                Pass pass = new Pass(this, optimisationRound, new Transform.Group(false,
                        new SubexpressionTransformation.TerminalChain(),
                        new SubexpressionTransformation.RedundantChain(),
                        new SubexpressionTransformation.SummarisingChain(),
                        new RelationFilterTransformation(),
                        new EvaluationOrderTransformation(true, true)
                ));
                Optional<Problem> nextSpec = pass.perform(curSpec);
                if (!nextSpec.isPresent()) break;
                curSpec = nextSpec.get();
                optimisationRound++;
            }
        } catch(IOException exc){
            except(exc);
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
