package cauliflower.generator;

import cauliflower.application.CauliflowerException;
import cauliflower.representation.Label;
import cauliflower.representation.ProblemAnalysis;
import cauliflower.representation.Rule;
import cauliflower.util.FileSystem;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;

/**
 * CauliSpecification
 * <p>
 * Author: nic
 * Date: 19/07/16
 */
public class CauliSpecification extends GeneratorForProblem {

    public CauliSpecification(Path out, Verbosity verbosity) throws IOException{
        this(FileSystem.getOutputStream(out), verbosity);
    }

    public CauliSpecification(PrintStream out, Verbosity verbosity) {
        super(out, verbosity);
    }

    @Override
    protected void performInternal() throws CauliflowerException {
        prob().labels.stream().map(Label::toStringDesc).map(CauliSpecification::makeDeclaration).forEach(outputStream::println);
        outputStream.println();
        ProblemAnalysis.getRuleStream(prob()).map(Rule::toSpecString).map(CauliSpecification::makeDeclaration).forEach(outputStream::println);
    }

    public static String makeDeclaration(String s){
        return s + ";";
    }
}
