package cauliflower.optimiser;

import cauliflower.application.Compiler;
import cauliflower.application.Configuration;
import cauliflower.parser.OmniParser;
import cauliflower.representation.*;
import cauliflower.util.Logs;
import cauliflower.util.Pair;
import cauliflower.util.Streamer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Pass
 * <p>
 * Author: nic
 * Date: 8/07/16
 */
public class Pass {

    private final int round;
    private final Controller parent;
    private final Path spec;
    private final Path executable;
    private List<Profile> profiles;

    /*local*/ Pass(Controller context, int roundNumber) throws IOException {
        round = roundNumber;
        parent = context;
        spec = parent.getSpecFileForRound(round);
        executable = parent.getExeFileForRound(round);
        profiles = null;
    }

    /*local*/ void compileExe() throws IOException, InterruptedException {
        Configuration curConf;
        try {
            curConf = new Configuration.Builder(spec)
                    .compiling().reporting().timing().optimising().parallelising()
                    .output(executable.getParent())
                    .named(executable.getFileName().toString())
                    .sampleProblem(parent.trainingSetStream().findAny().get())
                    .finalise();
        } catch (Configuration.ConfigurationException e) {
            //unreachable
            throw new IOException(e.msg);
        }
        Compiler comp = new Compiler(executable.toAbsolutePath(), curConf);
        comp.compile();
    }

    /*local*/ void generateLogs() throws IOException{
        profiles = Streamer.enumerate(parent.trainingSetStream(),
                (tset, idx) -> new Pair<>(tset, parent.getLogFileForRound(round, idx)))
                .map(p -> {
                    try {
                        return generateLog(p.first, p.second);
                    } catch (Exception e) {
                        Logs.forClass(Pass.class).error("Creating log " + p.second + " failed", e);
                        return null;
                    }
                })
                .collect(Collectors.toList());
        if(profiles.stream().anyMatch(p -> p == null)) throw new IOException("Failed to generate logs.");
    }

    /*local*/ Profile generateLog(Path trainingDir, Path logFile) throws IOException, InterruptedException {
        Logs.forClass(Pass.class).debug("Logging {} from {}", logFile, trainingDir);
        ProcessBuilder pb = new ProcessBuilder(executable.toString(), trainingDir.toString())
                .redirectErrorStream(true).redirectOutput(logFile.toFile());
        pb.environment().put("OMP_NUM_THREADS", "1"); // force profiling for a single core
        int code = -1;
        int count = 0;
        while (code != 0 && count < 5) {
            Process proc = pb.start();
            code = proc.waitFor();
            count++;
            Logs.forClass(Pass.class).trace("attempt {} has exit code {}", count, code);
        }
        if (code != 0){
            throw new IOException("Failed to generate a log for " + trainingDir.toString());
        }
        return new Profile(logFile);
    }

    /*local*/ void annotateParse() throws IOException {
        Problem parse = OmniParser.get(spec);
        parse.labels.stream().forEach(l -> System.out.println(String.format("%s - %d", l.name, profiles.get(0).getRelationSize(l))));
        List<Rule> rulePriority = IntStream.range(0, parse.getNumRules())
                .mapToObj(parse::getRule)
                .map(r -> new Pair<>(r, ruleWeight(r)))
                .sorted((p1, p2) -> p2.second.compareTo(p1.second))
                .map(p -> p.first)
                .collect(Collectors.toList());
        rulePriority.forEach(r ->{
            System.out.println(r);
            ProblemAnalysis.getBindings(r).forEach(b -> {
                System.out.print("  - ");
                b.boundEndpoints.forEach(e -> {
                    System.out.print(e.bound.toString() + " s=" + e.bindsSource + " !=" + e.bindsNegation + "  -  ");
                });
                System.out.println();
            });
            RuleOrderer.enumerateOrders(r).forEach(c ->{
                System.out.println(" -> " + new Clause.ClauseString().visit(c));
            });
        });
    }

    private Integer ruleWeight(Rule r){
        Clause.InOrderVisitor<Integer> iov = new Clause.InOrderVisitor<>(new Clause.VisitorBase<Integer>(){
            @Override
            public Integer visitLabelUse(LabelUse lu){
                return profiles.stream().mapToInt(p -> p.getDeltaExpansionTime(lu)).sum();
            }
        });
        iov.visit(r.ruleBody);
        return iov.visits.stream().filter(i -> i != null).mapToInt(Integer::intValue).sum();
    }

}
