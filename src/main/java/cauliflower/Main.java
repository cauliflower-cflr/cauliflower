package cauliflower;

import cauliflower.application.Cauliflower;
import cauliflower.generator.GeneratorUtils;
import cauliflower.parser.AntlrParser;
import cauliflower.parser.ParseFile;
import cauliflower.representation.Label;
import cauliflower.representation.Problem;
import cauliflower.util.CFLRException;
import cauliflower.util.Logs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

public class Main {

    public static void main(String[] args) throws IOException {
        //Cauliflower.main(args);
        AntlrParser parser = new AntlrParser();
        new ParseFile(parser).read(new File(args[0]));
        Problem prob = parser.problem;
        for(int i=0; i<prob.getNumRules(); i++){
            System.out.println(GeneratorUtils.getLabelsInClause(prob.getRule(i).ruleBody));
        }
        GeneratorUtils.getLabelDependencyOrder(prob).forEach(System.out::println);
    }

}
