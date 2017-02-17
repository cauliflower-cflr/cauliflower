package cauliflower.optimiser;

import cauliflower.application.CauliflowerException;
import cauliflower.application.Task;
import cauliflower.generator.CauliflowerSpecification;
import cauliflower.generator.Verbosity;
import cauliflower.parser.OmniParser;
import cauliflower.representation.Problem;
import cauliflower.util.Logs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.Stack;
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
        Stack<Problem> specStack = new Stack<>();
        Stack<Profile> profStack = new Stack<>();
        try {
            this.workingDir = null;
            this.workingDir = Files.createTempDirectory("cauli_opt_");// + inputSpec.getFileName().toString());
            // initial options for transformation
            Transform.Group allTransforms = new Transform.Group(false,
                    /*new SubexpressionTransformation.TerminalChain(),
                    new SubexpressionTransformation.RedundantChain(),
                    new SubexpressionTransformation.SummarisingChain(),*/
                    /*new SubexpressionTransformation.ChomskyChain()*/
                    /*new RelationFilterTransformation()*/
                    new EvaluationOrderTransformation(true, true)
            );

            specStack.push(inputSpec);
            long timeToBeat = Long.MAX_VALUE;
            while (specStack.size() <= maxRounds) {
                int optimisationRound = profStack.size();
                Logs.forClass(this.getClass()).trace("Round {}", optimisationRound);

                // generate the profile of this round
                if(specStack.size() > optimisationRound){
                    profStack.push(new Pass(this, optimisationRound).perform(specStack.peek()));
                } else {
                    Logs.forClass(this.getClass()).trace("profile already present, skipping runs");
                }

                Profile lastProf = profStack.peek();
                if(lastProf.getTotalTime() <= timeToBeat){
                    timeToBeat = lastProf.getTotalTime();
                    Optional<Problem> nextSpec = allTransforms.apply(specStack.peek(), lastProf);
                    if(!nextSpec.isPresent()) break;
                    // re-read the specification from file, this should not be necessary, but i do it because
                    // the parser creates nicer looking internal representations than my optimisers do using
                    // the ProblemBuilder
                    new CauliflowerSpecification(getSpecFileForRound(optimisationRound), new Verbosity()).perform(nextSpec.get()); // this writes the spec to file
                    specStack.push(OmniParser.get(getSpecFileForRound(optimisationRound)));
                } else {
                    Logs.forClass(this.getClass()).debug("Time is worse ({} vs {}), blacklisting the last transform", lastProf.getTotalTime(), timeToBeat);
                    specStack.pop();
                    profStack.pop();
                    allTransforms.blacklistLast();
                }
            }
        } catch(IOException exc){
            except(exc);
        }
        return specStack.pop();
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
