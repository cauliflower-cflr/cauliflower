package cauliflower.application;


import cauliflower.Main;
import cauliflower.generator.Adt;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Configuration
 * <p>
 * Author: nic
 * Date: 18/05/16
 */
public class Configuration {

    //Files
    @Parameter(description = "<cflr specification file>")
    public List<String> specFile;

    @Parameter(names = "-cs", description = "Write a CSV-based front-end to this file.")
    public String csvOutFile = null;

    @Parameter(names = "-sn", description = "Write a semi-naive back-end to this file.")
    public String snOutFile = null;

    //Configurations
    @Parameter(names = {"-a", "--adt"}, description = "The abstract-data-type used by the solver.")
    public Adt adt = Adt.Std;

    //Flags
    @Parameter(names = {"-h", "--help"}, description = "Display this help message.", help = true)
    public boolean help;

    @Parameter(names = {"-p", "--parallel"}, description = "Generate parallel evaluation code.")
    public boolean parallel;

    @Parameter(names = {"-r", "--reports"}, description = "Emit reports on runtime statistics.")
    public boolean reports;

    @Parameter(names = {"-t", "--timers"}, description = "Emit code to log executions times.")
    public boolean timers;

    // Override default public constructor
    private Configuration () {}

    /**
     * Throw exceptions for invalid configuration
     */
    private void validate() throws ConfigurationException{
        if (specFile == null || specFile.size() == 0) throw new ConfigurationException("No specification provided.");
        if (specFile.size() != 1) throw new ConfigurationException("At most one specification per execution, found: "
                + specFile.stream().collect(Collectors.joining(", ")) + ".");
    }

    /**
     * Static constructor for configurations from command line arguments
     * @param args the arguments
     * @return a valid configuration object
     * @throws ConfigurationException when the configuration is invalid
     */
    public static Configuration fromArgs(String... args) throws ConfigurationException, HelpException{
        Configuration ret = new Configuration();
        JCommander com = new JCommander(ret);
        com.setProgramName(Main.class.getName());
        try {
            com.parse(args);
        } catch (ParameterException exc){
            throw new ConfigurationException(exc.getLocalizedMessage());
        }
        if (ret.help)  throw new HelpException(com);
        ret.validate();
        return ret;
    }

    public static void main(String[] args) throws Exception{
        try{
            fromArgs("-h");
        } catch (HelpException he){
            System.out.println(he.usage);
        }
    }

    /**
     * A simple class to record the exceptions thrown for configuration
     */
    public static class ConfigurationException extends Exception{
        public final String msg;
        public ConfigurationException(String m){
            msg = m;
        }
    }

    /**
     * When help is requested, this exception is thrown
     */
    public static class HelpException extends Exception{
        public final String usage;
        public HelpException(JCommander com){
            StringBuilder sb = new StringBuilder();
            com.usage(sb);
            usage = sb.toString();
        }
    }

}
