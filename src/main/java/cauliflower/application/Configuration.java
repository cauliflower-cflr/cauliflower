package cauliflower.application;


import cauliflower.Main;
import cauliflower.generator.Adt;
import cauliflower.util.FileSystem;
import cauliflower.util.Logs;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Configuration
 * <p>
 * Author: nic
 * Date: 18/05/16
 */
public class Configuration {

    public final Path specFile;
    public final String problemName;
    public final List<Path> sampleDirs;
    public final Adt adt;
    public final boolean compile;
    public final boolean debugGenerated;
    public final boolean optimise;
    public final boolean parallel;
    public final boolean reports;
    public final boolean timers;

    private final Path outputDir;

    public Configuration(String... args) throws ConfigurationException, HelpException {
        this(new ConfigurationInternal(args));
    }

    private Configuration(ConfigurationInternal ci) throws ConfigurationException {
        if (ci._specAndSamples == null || ci._specAndSamples.size() == 0) {
            throw new ConfigurationException("No specification provided.");
        }

        this.specFile = Paths.get(ci._specAndSamples.get(0));
        this.sampleDirs = ci._specAndSamples.stream()
                .skip(1)
                .map(s -> Paths.get(s))
                .collect(Collectors.toList());
        this.adt = ci._adt;
        this.compile = ci._compile;
        this.debugGenerated = ci._debugGenerated;
        this.optimise = ci._optimise;
        this.parallel = ci._parallel;
        this.reports = ci._reports;
        this.timers = ci._timers;

        this.outputDir = Paths.get(ci._output == null ? "." : ci._output);
        this.problemName = ci._name == null ? FileSystem.stripExtension(specFile) : ci._name;

        Path fil = sampleDirs.stream().filter(p -> !Files.isDirectory(p)).findAny().orElse(null);
        if (fil != null) throw new ConfigurationException("Sample \"" + fil + "\" is not a directory.");
        if (optimise && sampleDirs.isEmpty()) throw new ConfigurationException("When optimising, you must provide at least one sample directory for training.");
    }

    public Path getOutput(String ext){
        String n = problemName;
        if(ext != null) n = n + "." + ext;
        return Paths.get(outputDir.toString(), n);
    }

    public Path getOutput(){
        return getOutput(null);
    }

    public static void main(String[] args) throws Exception {
        try {
            new Configuration("-h");
        } catch (HelpException he) {
            System.out.println(he.usage);
        }
    }

    /**
     * A simple class to record the exceptions thrown for configuration
     */
    public static class ConfigurationException extends Exception {
        public final String msg;

        public ConfigurationException(String m) {
            msg = m;
        }
    }

    /**
     * When help is requested, this exception is thrown
     */
    public static class HelpException extends Exception {
        public final String usage;

        public HelpException(JCommander com) {
            StringBuilder sb = new StringBuilder("Cauliflower\n")
                    .append("\n")
                    .append("Version: ").append(Info.buildVersion).append("\n")
                    .append("Built:   ").append(Info.buildDate).append("\n")
                    .append("Home:    ").append(Info.cauliDistributionDirectory).append("\n")
                    .append("\n");
            com.usage(sb);
            usage = sb.toString();
        }
    }

    private static class ConfigurationInternal {

        @Parameter(description = "<cflr specification file> {sample directories} ")
        private List<String> _specAndSamples = null;

        @Parameter(names = {"-a", "--adt"}, description = "The abstract-data-type used by the solver.")
        private Adt _adt = Adt.Std;

        @Parameter(names = {"-c", "--compile"}, description = "Compile a csv-based semi-naive executable to this file.")
        private boolean _compile = false;

        @Parameter(names = {"-g", "--debug-generated"}, description = "Compile the generated code in debug mode (requires -c).")
        private boolean _debugGenerated = false;

        @Parameter(names = {"-h", "--help"}, description = "Display this help message.", help = true)
        private boolean _help = false;

        @Parameter(names = {"-n", "--name"}, description = "Rename the problem (the default name for a problem in file 'foo/bar.cflr' is 'bar').")
        private String _name = null;

        @Parameter(names = {"-o", "--output-dir"}, description = "Set the output directory (default is \"./\").")
        private String _output = null;

        @Parameter(names = {"-O", "--optimise"}, description = "Optimise the input specification.")
        private boolean _optimise = false;

        @Parameter(names = {"-p", "--parallel"}, description = "Generate parallel evaluation code.")
        private boolean _parallel = false;

        @Parameter(names = {"-r", "--reports"}, description = "Emit reports on runtime statistics.")
        private boolean _reports = false;

        @Parameter(names = {"-t", "--timers"}, description = "Emit code to log executions times.")
        private boolean _timers = false;

        private ConfigurationInternal(String...args) throws ConfigurationException, HelpException{
            Logs.forClass(Configuration.class).debug("Args=\"{}\"", Arrays.stream(args).collect(Collectors.joining(" ")));
            JCommander com = new JCommander(this);
            com.setProgramName(Main.class.getName());
            try {
                com.parse(args);
            } catch (ParameterException exc) {
                throw new ConfigurationException(exc.getLocalizedMessage());
            }
            if (_help) throw new HelpException(com);
        }
    }

    public static class Builder{
        private final Path specification;
        private final List<Path> samples;
        private final ConfigurationInternal internal;
        public Builder(Path spec){
            this.specification = spec;
            this.samples = new ArrayList<>();
            ConfigurationInternal tmp = null;
            try {
                 tmp = new ConfigurationInternal();
            } catch (Exception exc) {
                //unreachable
            }
            this.internal = tmp;
        }
        public Configuration finalise() throws ConfigurationException {
            internal._specAndSamples = Stream.concat(Stream.of(specification), samples.stream())
                    .map(Path::toString)
                    .collect(Collectors.toList());
            return new Configuration(internal);
        }
        public Builder compiling(){
            this.internal._compile = true;
            return this;
        }
        public Builder optimising(){
            this.internal._optimise = true;
            return this;
        }
        public Builder parallelising(){
            this.internal._parallel = true;
            return this;
        }
        public Builder reporting(){
            this.internal._reports = true;
            return this;
        }
        public Builder timing(){
            this.internal._timers = true;
            return this;
        }
        public Builder named(String n){
            this.internal._name = n;
            return this;
        }
        public Builder output(Path dir){
            this.internal._output = dir.toString();
            return this;
        }
        public Builder sampleProblem(Path sampl){
            this.samples.add(sampl);
            return this;
        }
    }
}
