package cauliflower.application;

import cauliflower.parser.CFLRParser;
import cauliflower.util.Logs;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.stream.Collectors;

/**
 * Interfaces with the system compiler to compile
 * generated code into executables.
 */
public class Compiler {

    public final File execFile;
    public final File buildDir;
    public final File frontEnd;
    public final File backEnd;
    public final File logFile;

    private final Configuration configuration;

    public Compiler(String execPath, Configuration conf) throws IOException {
        this.execFile = new File(execPath);
        this.buildDir = new File(execFile.getParentFile(), execFile.getName() + "_build");
        if(execFile.exists()) throw new IOException("Executable " + execFile.getPath() + " already exists.");
        if(buildDir.exists()) throw new IOException("Build directory " + buildDir.getPath() + " already exists.");
        if (!buildDir.mkdirs()) throw new IOException("Failed to create a build directory at: " + buildDir.getAbsolutePath());
        this.logFile = new File(buildDir, "build.log");
        this.backEnd = new File(buildDir, execFile.getName() + ".h");
        this.frontEnd = new File(buildDir, execFile.getName() + ".cpp");
        this.configuration = conf;
    }

    public void compile(CFLRParser.ParserOutputs parse) throws IOException, InterruptedException {
        // initialising
        Generator gen = new Generator(backEnd, frontEnd, configuration);
        gen.generate(execFile.getName(), parse);

        // Run the build
        runProcess(processBuilderInit().command("env"));
        runProcess(processBuilderInit().command("cmake", Info.cauliDistributionDirectory));
        runProcess(processBuilderInit().command("make", "VERBOSE=1", "-j4"));

        // copy the executable
        Files.copy(new File(buildDir, execFile.getName()).toPath(), execFile.toPath());
    }

    private void runProcess(ProcessBuilder proc) throws IOException, InterruptedException {
        Logs.forClass(Compiler.class).debug("Executing: {}", proc.command().stream().collect(Collectors.joining(" ")));
        Process p = proc.start();
        p.waitFor();
    }

    private ProcessBuilder processBuilderInit(){
        ProcessBuilder ret = new ProcessBuilder();
        ret.environment().put("CAULI_FRONT", frontEnd.getAbsolutePath());
        ret.environment().put("CAULI_NAME", execFile.getName());
        return ret.directory(buildDir)
                .redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.appendTo(logFile));
    }

}
