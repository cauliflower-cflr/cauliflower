package cauliflower.optimiser;

import cauliflower.application.CauliflowerException;
import cauliflower.parser.OmniParser;
import cauliflower.representation.Problem;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;
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
        Transform tfm = new EvaluationOrderTransformation(true, true);
        Profile prof = getProf(
                "/home/nic/uni/cauliflower/example/experiment_eachopt/TMP_OPTI/r0_out_0.log",
                "/home/nic/uni/cauliflower/example/experiment_eachopt/TMP_OPTI/r0_out_1.log"
        );
        Problem spec = OmniParser.get(Paths.get("/home/nic/uni/cauliflower/example/experiment_eachopt/VIRT.cflr"));
        System.out.println(prof);
        System.out.println(spec);
        tfm.apply(spec, prof);
    }

}
