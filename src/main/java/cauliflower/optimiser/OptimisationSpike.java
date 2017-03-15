package cauliflower.optimiser;

import cauliflower.application.CauliflowerException;
import cauliflower.generator.CauliflowerSpecification;
import cauliflower.generator.Verbosity;
import cauliflower.parser.OmniParser;
import cauliflower.representation.Label;
import cauliflower.representation.Problem;
import cauliflower.representation.ProblemAnalysis;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class OptimisationSpike {

    private static Profile getProf(String... paths){
        return Profile.sumOfProfiles(Arrays.stream(paths)
                .map(s -> Paths.get(s))
                .map(Profile::nonThrowingNew)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList()));
    }

    public static void main(String[] args) throws IOException, CauliflowerException {
        //Transform tfm = new EvaluationOrderTransformation(true, true);
        Transform tfm = new RelationFilterTransformation();
        Profile prof = getProf(
                "/home/nic/uni/cauliflower/example/experiment_eachopt/TMP_OPTI/r0_out_0.log",
                "/home/nic/uni/cauliflower/example/experiment_eachopt/TMP_OPTI/r0_out_1.log"
        );
        Problem spec = OmniParser.get(Paths.get("/home/nic/uni/cauliflower/example/experiment_eachopt/VIRT.cflr"));
        System.out.println(prof);
        System.out.println(spec);
        Optional<Problem> res = tfm.apply(spec, prof);
        if(res.isPresent()){
            Problem newProb = res.get();
            System.out.println(newProb);
            Map<Label, Set<Label>> ldg = ProblemAnalysis.getLabelDependencyGraph(newProb);
            List<List<Label>> groups = ProblemAnalysis.getStronglyConnectedLabels(ldg);
            for(List<Label> g : groups){
                System.out.println(g);
            }
            new CauliflowerSpecification(System.out, new Verbosity()).perform(newProb);
        } else {
            System.err.println("NO OPTIMISATION MADE");
        }
    }

}
