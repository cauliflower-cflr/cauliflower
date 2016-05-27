package cauliflower.application;


import cauliflower.Main;
import cauliflower.generator.Adt;
import cauliflower.util.Logs;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Configuration
 * <p>
 * Author: nic
 * Date: 18/05/16
 */
public class Configuration {

    @Parameter(description = "<cflr specification file>")
    public List<String> specFile;

    @Parameter(names = {"-a", "--adt"}, description = "The abstract-data-type used by the solver.")
    public Adt adt = Adt.Std;

    @Parameter(names = {"-c", "--compile"}, description = "Compile a csv-based semi-naive executable to this file.")
    public String compOutFile = null;

    @Parameter(names = {"-cs", "--generate-csv"}, description = "Write a CSV-based front-end to this file.")
    public String csvOutFile = null;

    @Parameter(names = {"-h", "--help"}, description = "Display this help message.", help = true)
    public boolean help = false;

    @Parameter(names = {"-o", "--optimise"}, description = "Generate an optimised specification to this file.")
    public String optimiseOutFile = null;

    @Parameter(names = {"-ot", "--optimise-test"}, description = "When optimising, use this test case as a training example.")
    public List<String> optimiseTests = new ArrayList<>();

    @Parameter(names = {"-p", "--parallel"}, description = "Generate parallel evaluation code.")
    public boolean parallel = false;

    @Parameter(names = {"-r", "--reports"}, description = "Emit reports on runtime statistics.")
    public boolean reports = false;

    @Parameter(names = {"-sn", "--generate-semi-naive"}, description = "Write a semi-naive back-end to this file.")
    public String snOutFile = null;

    @Parameter(names = {"-t", "--timers"}, description = "Emit code to log executions times.")
    public boolean timers = false;

    // Override default public constructor
    private Configuration() {
    }

    /**
     * Throw exceptions for invalid configuration
     */
    private void validate() throws ConfigurationException {
        if (specFile == null || specFile.size() == 0)
            throw new ConfigurationException("No specification provided.");

        if (specFile.size() != 1)
            throw new ConfigurationException("At most one specification per execution, found: "
                    + specFile.stream().collect(Collectors.joining(", ")) + ".");

        if (compOutFile != null && (csvOutFile != null || snOutFile != null))
            throw new ConfigurationException("The '--compile' option overrides '-cs' and '-sn'.");

        if (optimiseOutFile != null && optimiseTests.size() == 0)
            throw new ConfigurationException("Cannot optimise, please provide at least 1 test-case for training.");

        String fil = optimiseTests.stream().filter(s -> !new File(s).isDirectory()).findAny().orElse(null);
        if (fil != null) throw new ConfigurationException("Training set \"" + fil + "\" is not a directory.");
    }

    /**
     * Static constructor for configurations from command line arguments
     *
     * @param args the arguments
     * @return a valid configuration object
     * @throws ConfigurationException when the configuration is invalid
     */
    public static Configuration fromArgs(String... args) throws ConfigurationException, HelpException {
        Logs.forClass(Configuration.class).debug("Args=\"{}\"", Arrays.stream(args).collect(Collectors.joining(" ")));
        Configuration ret = new Configuration();
        JCommander com = new JCommander(ret);
        com.setProgramName(Main.class.getName());
        try {
            com.parse(args);
        } catch (ParameterException exc) {
            throw new ConfigurationException(exc.getLocalizedMessage());
        }
        if (ret.help) throw new HelpException(com);
        ret.validate();
        return ret;
    }

    public static void main(String[] args) throws Exception {
        try {
            fromArgs("-h");
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

}
