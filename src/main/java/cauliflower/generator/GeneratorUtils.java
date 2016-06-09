package cauliflower.generator;

import cauliflower.application.Info;
import cauliflower.representation.*;
import cauliflower.util.Logs;
import cauliflower.util.TarjanScc;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    public static Map<Label, Set<Label>> getLabelDependencyGraph(Problem prob){
        Map<Label, Set<Label>> successors = prob.labels.stream().collect(Collectors.toMap(k -> k, v -> new HashSet<>()));
        for(int ri=0; ri<prob.getNumRules(); ri++){
            Rule r = prob.getRule(ri);
            successors.get(r.ruleHead.usedLabel).addAll(getLabelsInClause(r.ruleBody));
        }
        return successors;
    }

    public static Map<Label, Set<Label>> inverse(Map<Label, Set<Label>> graph){
        Map<Label, Set<Label>> ret = Stream.concat(
                graph.values().stream().flatMap(Set::stream),
                graph.keySet().stream())
                .distinct()
                .collect(Collectors.toMap(k -> k, v -> new HashSet<>()));
        graph.keySet().forEach(k -> graph.get(k).forEach(v -> ret.get(v).add(k)));
        return ret;
    }

}
