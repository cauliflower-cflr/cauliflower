package cauliflower.application;

import cauliflower.generator.Verbosity;
import cauliflower.representation.Problem;
import cauliflower.util.Logs;
import cauliflower.util.Pair;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Interfaces with the system compiler to compile
 * generated code into executables.
 */
public class Compiler implements Task<Path>{

    private final String name;
    private final Path execFile;
    private final Path buildDir;
    private final Path logFile;
    private final boolean debugGenerated;
    private final Verbosity verb;

    public Compiler(Configuration conf){
        this(conf.problemName, conf.getOutputDir(), conf.debugGenerated, new Verbosity(conf));
    }

    public Compiler(String exeName, Path exeDir, boolean debug, Verbosity verbosity) {
        this.name = exeName;
        this.execFile = Paths.get(exeDir.toString(), name);
        this.buildDir = Paths.get(exeDir.toString(), name + "_build");
        this.logFile = Paths.get(buildDir.toString(), "build.log");
        this.debugGenerated = debug;
        this.verb = verbosity;
    }


    @Override
    public Path perform(Problem spec) throws CauliflowerException {
        // initialising
        Generator gen = new Generator(name, buildDir, true, verb);
        Pair<Path, Optional<Path>> generatedFiles = gen.perform(spec);
        Path frontEnd = generatedFiles.second.get(); // this is safe because the generator is forced to generate it above

        try {
            // Run the build
            runProcess(frontEnd, "env");
            runProcess(frontEnd, "cmake", Info.cauliDistributionDirectory);
            runProcess(frontEnd, "make", "VERBOSE=1", "-j4");

            // copy the executable
            Files.copy(Paths.get(buildDir.toString(), name), execFile);
            return execFile;
        } catch(IOException e){
            except(e);
        } catch(InterruptedException e) {
            except(e.getMessage());
        }
        return null; // for some reason intelliJ complains without this, even though control cannot reach it...
    }

    private void runProcess(Path frontEndFile, String... cmd) throws IOException, InterruptedException, CauliflowerException{
        // build the process
        ProcessBuilder proc = new ProcessBuilder(cmd)
                .directory(buildDir.toFile())
                .redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.appendTo(logFile.toFile()));
        proc.environment().put("CAULI_FRONT", frontEndFile.toAbsolutePath().toString());
        proc.environment().put("CAULI_NAME", name);
        if(debugGenerated) proc.environment().put("CAULI_DEBUG", "true");

        // execute the process
        Logs.forClass(Compiler.class).debug("Executing: {}", proc.command().stream().collect(Collectors.joining(" ")));
        Process p = proc.start();
        if(p.waitFor() != 0) except("Process exited with non-zero status: " + proc.command().toString());
    }
}
