package cauliflower;

import cauliflower.application.Configuration;
import cauliflower.generator.CppSemiNaiveBackend;
import cauliflower.parser.AntlrParser;
import cauliflower.parser.ParseFile;

import java.io.File;
import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException, Configuration.HelpException, Configuration.ConfigurationException {
        // Cauliflower.main(args);
        Configuration conf = new Configuration(args);
        AntlrParser ap = new AntlrParser();
        new ParseFile(ap).read(conf.specFile.toFile());
        CppSemiNaiveBackend.generate("fast", ap.problem, conf, System.out);
    }

}
