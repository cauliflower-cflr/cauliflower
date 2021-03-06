package cauliflower.application;

import cauliflower.generator.CppCSVBackend;
import cauliflower.generator.CppSemiNaiveBackend;
import cauliflower.generator.GeneratorForProblem;
import cauliflower.generator.Verbosity;
import cauliflower.representation.Problem;
import cauliflower.util.FileSystem;
import cauliflower.util.Pair;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Generator
 *
 * A wrapper for code generation
 *
 * Author: nic
 * Date: 24/05/16
 */
public class Generator implements Task<Pair<Path, Optional<Path>>>{

    private String name;
    private Path outputBack;
    private Path outputFront;
    private boolean frontEnd;
    private Verbosity verb;

    public Generator(Configuration conf){
        this(conf.problemName, conf.getOutputDir(), true, new Verbosity(conf));
    }

    public Generator(String name, Path directory, boolean generateFrontEnd, Verbosity verbosity){
        this.name = name;
        this.outputBack = FileSystem.constructPath(directory, name, "h");
        this.outputFront = FileSystem.constructPath(directory, name, "cpp");
        this.frontEnd = generateFrontEnd;
        this.verb = verbosity;
    }

    public void overrideBackendOutput(Path p){
        this.outputBack = p;
    }

    public void overrideFrontendOutput(Path p){
        this.outputFront = p;
    }

    @Override
    public Pair<Path, Optional<Path>> perform(Problem spec) throws CauliflowerException {
        try {
            Pair<Path, Optional<Path>> ret = new Pair<>(outputBack, Optional.empty());
            FileSystem.mkdirFor(outputBack);
            List<GeneratorForProblem> tasks = new ArrayList<>();
            tasks.add(new CppSemiNaiveBackend(name, FileSystem.getOutputStream(outputBack), verb));
            if(frontEnd){
                ret.second = Optional.of(outputFront);
                FileSystem.mkdirFor(outputFront);
                String relPath = outputFront.getParent().toAbsolutePath().relativize(outputBack.toAbsolutePath()).toString();
                tasks.add(new CppCSVBackend(name, relPath, FileSystem.getOutputStream(outputFront), verb));
            }
            for(GeneratorForProblem t : tasks) t.perform(spec);
            return ret;
        } catch (IOException e) {
            except(e);
            return null;
        }
    }
}
