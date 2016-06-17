package cauliflower.generator;

import cauliflower.application.Configuration;
import cauliflower.representation.*;
import cauliflower.util.CFLRException;
import cauliflower.util.TarjanScc;

import java.io.PrintStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * CppParallelBackend.java
 *
 * generates code for an execution of the semi-naive code
 *
 * Created by nic on 25/11/15.
 */
public class CppSemiNaiveBackend {

    public static final String INDENT = "    ";
    public static final String PARALLEL_SCOPE = "parallel";
    public static final int PARTITION_COUNT = 400; // arbitrary value i borrowed from souffle

    //static constructor
    public static void generate(String problemName, Problem prob, Configuration config, PrintStream out) throws CFLRException{
        new CppSemiNaiveBackend(problemName, prob, config, out).generate();
    }

    private final Problem p;
    private final PrintStream outputStream;
    private final String name;
    private final Configuration cfg;
    private final boolean useHints = false;

    private Stack<Scope> scopeStack;

    private CppSemiNaiveBackend(String problemName, Problem prob, Configuration config, PrintStream out){
        this.outputStream = out;
        this.p = prob;
        this.name = problemName;
        this.cfg = config;
        this.scopeStack = new Stack<>();
    }

    private String structName(){
        return toIdent(name + "_semi_naive");
    }

    public void generate() throws CFLRException{
        if (!name.matches("[a-zA-Z][a-zA-Z0-9_]*")) throw new CFLRException("Problem name must be a valid c identifier: \"" + name + "\"");
        GeneratorUtils.generatePreBlock(name, "Semi-naive method fore evaluating CFLR solutions", this.getClass(), outputStream);
        generateImports();
        Scope namespaceScope = new Scope("namespace", "namespace cflr");
        Scope structScope = new Scope("container", "struct " + structName());
        generateIdxes();
        generateDefs();
        new Scope("solve", "static void solve(vols_t& volume, rels_t& relations)");
        generateInitialisers();
        generateSemiNaive();
        structScope.popMe();
        line(";"); // inelegant solution to ending a struct def with ;
        namespaceScope.popMe();
    }

    private void generateImports(){
        line("#include <array>");
        line("#include <omp.h>");
        for(String imp : Adt.Souffle.imports){
            line("#include " + imp);
        }
        line("#include \"" + Adt.Souffle.importLoc + "\"");
        line("#include \"relation.h\"");
        line("#include \"Util.h\"");
    }

    private void generateIdxes(){
        p.labels.stream().forEach(l -> line("static const unsigned " + idxer(l) + " = " + l.index + ";"));
        //field indices precede vertex indices because of some limitation i once programmed into this thing
        p.fieldDomains.stream().forEach(d -> line("static const unsigned " + idxer(d) + " = " + d.index + ";"));
        p.vertexDomains.stream().forEach(d -> line("static const unsigned " + idxer(d) + " = " + (p.fieldDomains.size() + d.index + ";")));
    }

    private void generateDefs(){
        line("static const unsigned num_lbls = " + p.labels.size() + ";");
        line("static const unsigned num_domains = " + (p.vertexDomains.size() + p.fieldDomains.size()) + ";");
        line("typedef " + Adt.Souffle.typename + " adt_t;");
        line("typedef std::array<relation<adt_t>, num_lbls> rels_t;");
        line("typedef std::array<size_t, num_domains> vols_t;");
    }

    private void generateInitialisers(){
        // Epsilon initialisation
        line("size_t largest_vertex_domain = 0;");
        p.vertexDomains.stream().forEach(d -> line("largest_vertex_domain = std::max(largest_vertex_domain, volume[" + idxer(d) + "]);"));
        line("const adt_t epsilon = adt_t::identity(largest_vertex_domain);");
        for(int ri=0; ri<p.getNumRules(); ri++){
            Rule r = p.getRule(ri);
            if(r.ruleBody instanceof Clause.Epsilon) line("for(auto& a : " + idxRel(r.ruleHead.usedLabel) + ".adts) a.union_copy(epsilon);");
        }

        // Delta initialisation
        line("rels_t deltas{" + p.labels.stream().map(l -> relationDecl(l, "")).collect(Collectors.joining(",")) + "};");
        p.labels.stream()
                .forEach(l -> line("for(unsigned i=0; i<%s.adts.size(); ++i) %s.adts[i].deep_copy(%s.adts[i]);", idxRel(l), idxRel(l), idxDelta(l)));
    }

    private void generateSemiNaive(){
        Map<Label, Set<Label>> depGraph = GeneratorUtils.getLabelDependencyGraph(p), depGraphInverse = GeneratorUtils.inverse(depGraph);
        List<List<Label>> order = TarjanScc.getSCC(depGraph);
        for(List<Label> group : order){
            // while there are deltas to expand
            String cond = group.stream()
                    .map(l -> "(!" + idxDelta(l) + ".empty())")
                    .collect(Collectors.joining("||"));
            Scope curGroup = new Scope(group.stream().map(l ->l.name).collect(Collectors.joining(" ")), "while(" + cond + ")");
            // initialise the new relations for this iteration
            List<Label> relationsGeneratedByGroup = group.stream().flatMap(l -> depGraphInverse.get(l).stream()).distinct().collect(Collectors.toList());
            relationsGeneratedByGroup.forEach(l -> line("%s;", relationDecl(l, idxnew(l))));
            // for each non-empty delta
            for(Label l : group){
                Scope curDeltaScope = new Scope("delta " + l.name, "if(!" + idxDelta(l) + ".empty())"); // TODO stop emitting this when theres only one relation in the group
                line("relation<adt_t> cur_delta(" + idxDelta(l) + ".volume());");
                line("cur_delta.swap_contents(" + idxDelta(l) + ");");
                for(LabelUse usage : l.usages) if(usage.usedInRule.ruleHead != usage){
                    generateDeltaExpansion(usage);
                }
                curDeltaScope.popMe();
            }
            // write the new relations into their delta/current
            relationsGeneratedByGroup.stream().filter(l -> l.fieldDomainCount == 0).forEach(l -> {
                line(partitionRel(relationAccess(idxnew(l), new ArrayList<>(), new ArrayList<>()), true, "parts_" + l.name));
            });
            Scope parl = parallelScope(); // in parallel
            relationsGeneratedByGroup.stream().forEach(l ->{
                List<String> vars = new ArrayList<>();
                List<String> vols = new ArrayList<>();
                line(parallelFor());
                if(l.fieldDomainCount == 0){
                    iteratePartition("parts_" + l.name, l.name + " update", "iter");
                    line("%s.forwards.insert(%s);", relationAccess(idxRel(l), vars, vols), "iter");
                    line("%s.forwards.insert(%s);", relationAccess(idxDelta(l), vars, vols), "iter");
                    line("%s.backwards.insert(%s);", relationAccess(idxRel(l), vars, vols), "iter");
                    line("%s.backwards.insert(%s);", relationAccess(idxDelta(l), vars, vols), "iter");
                } else {
                    for (Domain dom : l.fieldDomains) {
                        vars.add("upd_" + vars.size());
                        vols.add(idxField(dom));
                        new Scope("update " + l.name + " " + (vars.size() - 1), simpleFor(vars.get(vars.size() - 1), idxField(dom)));
                    }
                    line("%s.forwards.insertAll(%s.forwards);", relationAccess(idxRel(l), vars, vols), relationAccess(idxnew(l), vars, vols));
                    line("%s.forwards.insertAll(%s.forwards);", relationAccess(idxDelta(l), vars, vols), relationAccess(idxnew(l), vars, vols));
                    line("%s.backwards.insertAll(%s.backwards);", relationAccess(idxRel(l), vars, vols), relationAccess(idxnew(l), vars, vols));
                    line("%s.backwards.insertAll(%s.backwards);", relationAccess(idxDelta(l), vars, vols), relationAccess(idxnew(l), vars, vols));
                }
                parl.popInto();
            });
            curGroup.popMe();
        }
    }

    private void generateDeltaExpansion(LabelUse delta){
        Scope entryScope = scopeStack.peek();
        if(cfg.timers || cfg.optimise){
            new TimeScope("exp " + delta.toString(), "");
        }
        Rule rule = delta.usedInRule;
        // find label usages with and without fields
        List<LabelUse> usesWithoutFields = new ArrayList<>();
        List<LabelUse> usesWithFields = new ArrayList<>();
        new Clause.InOrderVisitor<>(new Clause.VisitorBase<Void>() {
            @Override
            public Void visitLabelUse(LabelUse cl) {
                if(cl.usedField.size() > 0) usesWithFields.add(cl);
                else if(cl != delta) usesWithoutFields.add(cl); // only check the delta usages when they HAVE fields
                return null;
            }
        }).visit(rule.ruleBody);
        // only perform delta expansion when all relations have non-empty values
        // TODO this breaks in the face of negation
        if(usesWithoutFields.size() != 0) new Scope("without fields", "if(" + multiNonEmptyCheck(usesWithoutFields, delta, "&&") + ")");
        if(rule.allFieldReferences.size() > 0){
            parallelScope();
            line(parallelFor());
            rule.allFieldReferences.stream().forEach(dp ->
                    new Scope("field projection " + dp.name, simpleFor(varProject(dp), idxField(dp.referencedField))));
        }

        if(usesWithFields.size() != 0) new Scope("with fields", "if(" + multiNonEmptyCheck(usesWithFields, delta, "&&") + ")");
        //expand the rule body
        DeltaExpansionVisitor completion = new DeltaExpansionVisitor(delta, null, null);
        completion.visit(rule.ruleBody);
        line(declarePair("result", completion.fromContext, completion.toContext));
        new Scope("check add", "if(!" + relationAccess(rule.ruleHead, delta) + ".forwards.contains(result))");
        line("%s.forwards.insert(result);", relationNewAccess(rule.ruleHead));
        line("%s.backwards.insert(%s);", relationNewAccess(rule.ruleHead), initPair(completion.toContext, completion.fromContext));
        entryScope.popInto();
    }

    /**
     * A visitor to create expanded label code
     * The context for expansion is entered where the relation can know its start/end/both/neither
     */
    private class DeltaExpansionVisitor implements Clause.Visitor<Void> {
        public final LabelUse delta;
        public final Rule rule;
        public String fromContext;
        public String toContext;
        public DeltaExpansionVisitor(LabelUse de, String from, String to){
            this.delta = de;
            this.rule = de.usedInRule;
            this.fromContext = from;
            this.toContext = to;
        }

        @Override
        public Void visitCompose(Clause.Compose cl) {
            DeltaExpansionVisitor left = new DeltaExpansionVisitor(delta, fromContext, null);
            left.visit(cl.left);
            DeltaExpansionVisitor right = new DeltaExpansionVisitor(delta, left.toContext, toContext);
            right.visit(cl.right);
            fromContext = left.fromContext;
            toContext = right.toContext;
            return null;
        }

        @Override
        public Void visitIntersect(Clause.Intersect cl) {
            throw new RuntimeException("Intersection is not supported"); // TODO
        }

        @Override
        public Void visitReverse(Clause.Reverse cl) {
            DeltaExpansionVisitor sub = new DeltaExpansionVisitor(delta, toContext, fromContext);
            sub.visit(cl.sub);
            fromContext = sub.toContext;
            toContext = sub.fromContext;
            return null;
        }

        @Override
        public Void visitNegate(Clause.Negate cl) {
            throw new RuntimeException("Negation is not supported"); // TODO
        }

        @Override
        public Void visitLabelUse(LabelUse cl) {
            String nm = cl.usedLabel.name + " " + cl.usageIndex + " " + (fromContext == null?"f":"b") + (toContext == null?"f":"b");
            if(fromContext == null && toContext == null){ // no constraint, therefore just iterate through forwards
                maybeParallelIteration(nm, cl, "forwards", delta);
                fromContext = varIter(cl) + "[0]";
                toContext = varIter(cl) + "[1]";
            } else if(fromContext == null){
                line("auto range_%s = %s.backwards.getBoundaries<1>({{%s, 0}});", varIter(cl), relationAccess(cl, delta), toContext);
                new Scope(nm, "for(const auto& " + varIter(cl) + " : range_" + varIter(cl) + ")");
                fromContext = varIter(cl) + "[1]";
            } else if(toContext == null){
                line("auto range_%s = %s.forwards.getBoundaries<1>({{%s, 0}});", varIter(cl), relationAccess(cl, delta), fromContext);
                new Scope(nm, "for(const auto& " + varIter(cl) + " : range_" + varIter(cl) + ")");
                toContext = varIter(cl) + "[1]";
            } else {
                new Scope(nm, "if(" + relationAccess(cl, delta) + ".forwards.contains(" + initPair(fromContext, toContext) + "))");
            }
            return null;
        }

        @Override
        public Void visitEpsilon(Clause.Epsilon cl) {
            throw new RuntimeException("Epsilon is not supported"); // TODO
        }
    }

    private void maybeParallelIteration(String innerName, LabelUse lu, String direction, LabelUse delta){
        if(inParallelScope()){
            new Scope(innerName, "for(const auto& " + varIter(lu) + " : " + relationAccess(lu, delta) + "." + direction + ")");
        } else {
            line(partitionRel(relationAccess(lu, delta), direction.equals("forwards"), "primary_index"));
            parallelScope();
            line(parallelFor());
            iteratePartition("primary_index", innerName, varIter(lu));
        }
    }

    private void iteratePartition(String partition, String scopeName, String iterVar){
        new Scope(partition + " iteration", "for(auto pidx = " + partition + ".begin(); pidx<" + partition + ".end(); ++pidx)");
        new Scope(scopeName, "for(const auto& " + iterVar + " : *pidx)");
    }

    /**
     * Pretty-printing utilities
     */
    private boolean pretty(){
        return true;
    }
    private void line(String s){
        if(pretty()) for(int i=0; i<scopeStack.size(); i++) outputStream.print(INDENT);
        outputStream.println(s);
    }
    private void line(String s, Object... forms){
        line(String.format(s, forms));
    }

    /**
     * Converts parts of the problem representation to variables used by cauliflower
     */
    public static String idxer(Domain d){
        return "DOM_" + d.name;
    }
    public static String idxer(Label l){
        return "LBL_" + l.name;
    }
    public static String idxer(LabelUse l){
        return idxer(l.usedLabel);
    }
    public static String idxDelta(Label l){
        return "deltas[" + idxer(l) + "]";
    }
    public static String idxnew(Label l){
        return "new_" + idxer(l);
    }
    public static String idxRel(Label l){
        return "relations[" + idxer(l) + "]";
    }
    public static String idxField(Domain d){
        return "volume[" + idxer(d) + "]";
    }
    public static String varProject(DomainProjection dp){
        return "f_" + dp.name;
    }
    public static String varIter(LabelUse lu){
        return "idx_" + lu.usedLabel.name + lu.usageIndex;
    }

    /**
     * General purpose code access
     */
    public static String parallelBegin(){
        return "# pragma omp parallel";
    }
    public static String parallelFor(){
        return "# pragma omp for schedule(dynamic)";
    }
    public static String partitionRel(String relAccess, boolean forwards, String partitionName){
        return String.format("auto %s = %s.%s.partition(%d);", partitionName, relAccess, forwards ? "forwards" : "backwards", PARTITION_COUNT);
    }
    public static String simpleFor(String var, String end){
        return String.format("for(unsigned %s=0; %s<%s; ++%s)", var, var, end, var);
    }
    public static String declarePair(String vname, String first, String second){
        return String.format("adt_t::tree_t::entry_type %s(%s);", vname, initPair(first, second));
    }
    public static String initPair(String first, String second){
        return String.format("{{%s,%s}}", first, second);
    }
    public static String relationDecl(Label l, String name){
        return "relation<adt_t>" + (name.length() > 0 ? " ":"") + name + "(" + idxRel(l) + ".adts.size())";
    }
    public static String relationAccess(LabelUse l, LabelUse curDelta){
        return relationAccess(l == curDelta ? "cur_delta" : idxRel(l.usedLabel), l);
    }
    public static String relationAccess(String name, LabelUse l) {
        return relationAccess(name, l.usedField.stream().map(CppSemiNaiveBackend::varProject).collect(Collectors.toList()),
                l.usedLabel.fieldDomains.stream().map(CppSemiNaiveBackend::idxField).collect(Collectors.toList()));
    }
    public static String relationAccess(String name, List<String> fieldVars, List<String> fieldVolumes){
        StringBuilder vn = new StringBuilder();
        vn.append(name).append(".adts[");
        if(fieldVars.size() == 0){
            vn.append(0);
        } else {
            int fi = 0;
            for (String var : fieldVars) {
                if(fi != 0) vn.append(" + ");
                vn.append(var);
                for (int vi = fi + 1; vi < fieldVolumes.size(); vi++) {
                    vn.append("*").append(fieldVolumes.get(vi));
                }
                fi++;
            }
        }
        return vn.append("]").toString();
    }
    public static String relationNewAccess(LabelUse lu){
        return relationAccess(idxnew(lu.usedLabel), lu);
    }
    private String nonEmptyCheck(LabelUse l, LabelUse delta){
        return "!" + relationAccess(l, delta) + ".empty()";
    }
    private String multiNonEmptyCheck(Collection<LabelUse> lus, LabelUse delta, String join){
        return lus.stream().map(lu -> nonEmptyCheck(lu, delta)).map(s -> "(" + s + ")").collect(Collectors.joining(join));
    }
    public static String toIdent(String n){
        String ret = n.chars()
                .filter(i -> (i >= 'a' && i <= 'z') || (i >= 'A' && i <= 'Z') || (i >= '0' && i <= '9') || i == '_')
                .mapToObj(i -> "" + (char)i)
                .collect(Collectors.joining());
        Matcher m = Pattern.compile("[a-zA-Z]").matcher(ret);
        if(m.find()){
            return ret.substring(m.start());
        } else {
            return "var_" + ret;
        }
    }
    public static String timeStart(String var){
        return "time_point " + var + " = now();";
    }
    public static String timeReport(String var, String repName){
        return "time_point tend_" + var + " = now(); std::cerr << \"TIME \" << omp_get_thread_num() << \" " + repName + " \" << duration_in_ms(" + var + ", tend_" + var + ") << std::endl;";
    }

    /**
     * Generates structured scope bits, assists in pretty printing
     */
    private class Scope {
        public final String name;
        public final String code;
        public Scope(String n, String c){
            this.name = n;
            this.code = c;
            this.pushCode();
            scopeStack.push(this);
        }
        protected void pushCode(){
            line(code + (code.length()>0?" ":"") + "{ // " + name);
        }
        protected void popCode(){
            line("} // " + name);
        }
        public void popMe(){
            Scope cur = null;
            while(cur != this){
                cur = scopeStack.pop();
                cur.popCode();
            }
        }
        public void popInto(){
            while(scopeStack.peek() != this){
                scopeStack.peek().popMe();
            }
        }
    }
    private class TimeScope extends Scope {
        public TimeScope(String n, String c){
            super(n, c);
        }
        @Override
        protected void pushCode(){
            line(timeStart(toIdent("timer_" + name)));
            super.pushCode();
        }
        @Override
        protected void popCode(){
            super.popCode();
            line(timeReport(toIdent("timer_" + name), name));
        }
    }
    private boolean inParallelScope(){
        for(Scope s : scopeStack) if(s.name.equals(PARALLEL_SCOPE)) return true;
        return false;
    }
    private Scope parallelScope(){
        line(parallelBegin());
        return new Scope("parallel", "");
    }

}
