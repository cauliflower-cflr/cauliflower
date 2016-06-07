package cauliflower.generator;

import cauliflower.application.Info;
import cauliflower.representation.*;
import cauliflower.util.Logs;
import cauliflower.util.TarjanScc;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * GeneratorUtils
 * <p>
 * Author: nic
 * Date: 3/06/16
 */
public class GeneratorUtils {

    public static void generatePreBlock(String problemName, String desc, Class<?> generator, PrintStream out){
        out.println("// " + problemName);
        out.println("//");
        out.println("// " + desc);
        out.println("//");
        out.println("// Generated on: " + new SimpleDateFormat("dd/MM/yyyy").format(new Date()));
        out.println("//           by: " + generator.getSimpleName());
        out.println("//      version: " + Info.buildVersion);
    }

    public static List<Label> getLabelsInClause(Clause c){
        Clause.InOrderVisitor<Label> iov = new Clause.InOrderVisitor<>(new Clause.VisitorBase<Label>() {
            @Override
            public Label visitLabelUse(LabelUse cl) {
                return cl.usedLabel;
            }
        });
        iov.visit(c);
        return iov.visits.stream().filter(l -> l != null).distinct().collect(Collectors.toList());
    }

    public static List<List<Label>> getLabelDependencyOrder(Problem prob){
        Map<Label, Set<Label>> successors = prob.labels.stream().collect(Collectors.toMap(k -> k, v -> new HashSet<>()));
        for(int ri=0; ri<prob.getNumRules(); ri++){
            Rule r = prob.getRule(ri);
            successors.get(r.ruleHead.usedLabel).addAll(getLabelsInClause(r.ruleBody));
        }
        return TarjanScc.getSCC(successors);
    }

}
