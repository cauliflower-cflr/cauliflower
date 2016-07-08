package cauliflower.application;

import cauliflower.parser.OmniParser;
import cauliflower.representation.*;
import cauliflower.util.FileSystem;
import cauliflower.util.Logs;
import cauliflower.util.Pair;
import cauliflower.util.Streamer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Optimiser
 * <p>
 * Given a Cauliflower specification, continually runs and
 * analyses the generated code, using feedback to refine the
 * specification and improve performance.
 * <p>
 * Author: nic
 * Date: 24/05/16
 */
public class Optimiser {

    // these patterns are used to match lines of profiler output
    public static final Pattern TIME_UPDATE = Pattern.compile("TIME [0-9]* upd .* [0-9]*");
    public static final Pattern TIME_EXPAND = Pattern.compile("TIME [0-9]* exp .* [0-9]*");
    public static final Pattern SIZE_PARTS = Pattern.compile("SIZE final .* [0-9]* [0-9]* [0-9]*");
    public static final Pattern DOM_FIELD = Pattern.compile("f:.*=[0-9]*");
    public static final Pattern DOM_VERTEX = Pattern.compile("v:.*=[0-9]*");

    public final Path inputSpec;
    public final Path optimisedSpec;
    public final List<Path> trainingSet;

    private final Path workingDir;

    public Optimiser(Path srcSpec, Path targetSpec, List<Path> trainingSet) throws IOException{
        this.inputSpec = srcSpec;
        this.optimisedSpec = targetSpec;
        this.trainingSet = trainingSet;
        this.workingDir = Files.createTempDirectory("cauli_opt_" + inputSpec.getFileName().toString());
    }

    public void optimise() throws IOException, Configuration.HelpException, Configuration.ConfigurationException, InterruptedException {
        Files.copy(inputSpec, getSpecFileForRound(0));
        boolean going = true;
        int optimisationRound = 0;
        while (going) {
            Logs.forClass(this.getClass()).trace("Round {}", optimisationRound);
            OptimisationPass pass = new OptimisationPass(optimisationRound);
            pass.compileExe();
            pass.generateLogs();
            pass.annotateParse();
            going = false;
        }
    }

    private Path getSpecFileForRound(int round){
        return Paths.get(workingDir.toString(), "r" + round + "_spec.cfg");
    }

    private Path getExeFileForRound(int round){
        return Paths.get(workingDir.toString(), "r" + round + "_exe");
    }

    private Path getLogFileForRound(int round, int ts){
        return Paths.get(workingDir.toString(), "r" + round + "_out_" + ts + ".log");
    }

    /**
     * Contains the necessary state information for a single optimisation pass
     */
    private class OptimisationPass {

        private final int round;
        private final Path spec;
        private final Path executable;
        private List<Profile> profiles;

        private OptimisationPass(int roundNumber) throws IOException {
            round = roundNumber;
            spec = getSpecFileForRound(round);
            executable = getExeFileForRound(round);
            profiles = null;
        }

        private void compileExe() throws Configuration.HelpException, Configuration.ConfigurationException, IOException, InterruptedException {
            Configuration curConf = new Configuration(
                    "-c", "-p", "-r", "-t", "-O",
                    "-o", executable.getParent().toString(),
                    "-n", executable.getFileName().toString(),
                    spec.toString(),
                    trainingSet.stream().limit(1).map(Path::toString).collect(Collectors.joining()));
            Compiler comp = new Compiler(executable.toAbsolutePath(), curConf);
            comp.compile();
        }

        private void generateLogs() throws IOException{
            profiles = Streamer.enumerate(trainingSet.stream(),
                    (tset, idx) -> new Pair<>(tset, getLogFileForRound(round, idx)))
                    .map(p -> {
                        try {
                            return generateLog(p.first, p.second);
                        } catch (Exception e) {
                            Logs.forClass(Optimiser.class).error("Creating log " + p.second + " failed", e);
                            return null;
                        }
                    })
                    .collect(Collectors.toList());
            if(profiles.stream().anyMatch(p -> p == null)) throw new IOException("Failed to generate logs.");
        }

        private Profile generateLog(Path trainingDir, Path logFile) throws IOException, InterruptedException {
            Logs.forClass(Optimiser.class).debug("Logging {} from {}", logFile, trainingDir);
            ProcessBuilder pb = new ProcessBuilder(executable.toString(), trainingDir.toString())
                    .redirectErrorStream(true).redirectOutput(logFile.toFile());
            pb.environment().put("OMP_NUM_THREADS", "1"); // force profiling for a single core
            int code = -1;
            int count = 0;
            while (code != 0 && count < 5) {
                Process proc = pb.start();
                code = proc.waitFor();
                count++;
                Logs.forClass(Optimiser.class).trace("attempt {} has exit code {}", count, code);
            }
            if (code != 0){
                throw new IOException("Failed to generate a log for " + trainingDir.toString());
            }
            return new Profile(logFile);
        }

        private void annotateParse() throws IOException {
            Problem parse = OmniParser.get(spec);
            parse.labels.stream().forEach(l -> System.out.println(String.format("%s - %d", l.name, profiles.get(0).getRelationSize(l))));
            List<Rule> rulePriority = IntStream.range(0, parse.getNumRules())
                    .mapToObj(parse::getRule)
                    .map(r -> new Pair<>(r, ruleWeight(r)))
                    .sorted((p1, p2) -> p2.second.compareTo(p1.second))
                    .peek(p -> System.out.println(p.first.toString() + " - " + p.second))
                    .map(p -> p.first)
                    .collect(Collectors.toList());
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

    /**
     * Reads the log for an execution and creates a table (string-integer) for profile data
     */
    private class Profile {
        Map<String, Integer> data = new HashMap<>();
        private Profile(Path logPath) throws IOException {
            FileSystem.getLineStream(logPath).forEach(l -> {
                if(TIME_UPDATE.matcher(l).matches()){
                    int time = Integer.parseInt(l.substring(l.lastIndexOf(" ") + 1));
                    String var = "u:" + l.substring(l.indexOf(" upd ") + 5, l.lastIndexOf(" "));
                    if(!data.containsKey(var)) data.put(var, 0);
                    data.put(var, data.get(var) + time);
                } else if (TIME_EXPAND.matcher(l).matches()){
                    int time = Integer.parseInt(l.substring(l.lastIndexOf(" ") + 1));
                    String var = "x:" + l.substring(l.indexOf(" exp ") + 5, l.lastIndexOf(" "));
                    if(!data.containsKey(var)) data.put(var, 0);
                    data.put(var, data.get(var) + time);
                } else if (SIZE_PARTS.matcher(l).matches()){
                    String[] ps = l.split(" ");
                    data.put("st:" + ps[2], Integer.parseInt(ps[3]));
                    data.put("s:" + ps[2], Integer.parseInt(ps[4]));
                    data.put("t:" + ps[2], Integer.parseInt(ps[5]));
                } else if (DOM_VERTEX.matcher(l).matches() || DOM_FIELD.matcher(l).matches()){
                    data.put("d" + l.substring(0, l.indexOf("=")), Integer.parseInt(l.substring(l.lastIndexOf("=") + 1)));
                }
            });
        }
        public int getDeltaExpansionTime(LabelUse lu){
            String s = "x:" + lu.toString();
            return data.containsKey(s) ? data.get(s) : 0;
        }
        public int getRelationSize(Label l){
            String s = "st:" + l.name;
            return data.containsKey(s) ? data.get(s) : 0;
        }
        public int getRelationSources(Label l){
            String s = "s:" + l.name;
            return data.containsKey(s) ? data.get(s) : 0;
        }
        public int getRelationSinks(Label l){
            String s = "t:" + l.name;
            return data.containsKey(s) ? data.get(s) : 0;
        }
        public int getFieldDomainSize(Domain d){
            String s = "df:" + d.name;
            return data.containsKey(s) ? data.get(s) : 0;
        }
        public int getVertexDomainSize(Domain d){
            String s = "dv:" + d.name;
            return data.containsKey(s) ? data.get(s) : 0;
        }
    }

}
