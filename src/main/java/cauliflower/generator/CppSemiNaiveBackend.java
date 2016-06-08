package cauliflower.generator;

import cauliflower.application.Configuration;
import cauliflower.cflr.*;
import cauliflower.representation.*;
import cauliflower.representation.Label;
import cauliflower.representation.Problem;
import cauliflower.representation.Rule;
import cauliflower.util.CFLRException;
import cauliflower.util.Streamer;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * CppParallelBackend.java
 *
 * generates code for an execution of the semi-naive code
 *
 * Created by nic on 25/11/15.
 */
public class CppSemiNaiveBackend {

    public static final String INDENT = "    ";
    public static final int PARTITION_COUNT = 400; // arbitrary value i borrowed from souffle

    //static constructor
    public static void generate(String problemName, Problem prob, Configuration config, PrintStream out) throws CFLRException{
        new CppSemiNaiveBackend(problemName, prob, config, out).generate();
    }

    private final Problem p;
    private final PrintStream outputStream;
    private final Configuration cfg;
    private final String name;
    private final boolean useHints = false;

    private Stack<Scope> scopeStack;

    private CppSemiNaiveBackend(String problemName, Problem prob, Configuration config, PrintStream out){
        this.outputStream = out;
        this.cfg = config;
        this.p = prob;
        this.name = problemName;
        this.scopeStack = new Stack<>();
    }

    private String structName(){
        return name + "_semi_naive";
    }

    public void generate() throws CFLRException{
        if (!name.matches("[a-zA-Z][a-zA-Z0-9_]*")) throw new CFLRException("Problem name must be a valid c identifier: \"" + name + "\"");
        GeneratorUtils.generatePreBlock(name, "Semi-naive method fore evaluating CFLR solutions", this.getClass(), outputStream);
        generateImports();
        Scope namespaceScope = new Scope("namespace", "namespace cflr");
        new Scope("container", "struct " + structName());
        generateIdxes();
        generateDefs();
        new Scope("solve", "static void solve(vols_t& volume, rels_t& relations)");
        generateInitialisers();
        generateSemiNaive();
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
        p.labels.stream().forEach(l -> line("static const unsigned " + idxer(l) + " = " + l.index));
        //field indices precede vertex indices because of some limitation i once programmed into this thing
        p.fieldDomains.stream().forEach(d -> line("static const unsigned " + idxer(d) + " = " + d.index));
        p.vertexDomains.stream().forEach(d -> line("static const unsigned " + idxer(d) + " = " + (p.fieldDomains.size() + d.index)));
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
        line("rels_t deltas{" + p.labels.stream().map(l -> "relation<adt_t>(" + idxRel(l) + ".adts.size())").collect(Collectors.joining(",")) + "};");
        // TODO initialise deltas in-place via some kind of deep copy
        // i.e. deep copy the whole relation, instead of the individual ADTs
        p.labels.stream()
                .forEach(l -> line("for(unsigned i=0; i<%s.adts.size(); ++i) %s.adts[i].deep_copy(%s.adts[i]);", idxRel(l), idxRel(l), idxDelta(l)));
    }

    private void generateSemiNaive(){
        List<List<Label>> order = GeneratorUtils.getLabelDependencyOrder(p);
        for(List<Label> group : order){
            //while there are deltas to expand
            String cond = group.stream()
                    .map(l -> "!" + idxDelta(l) + ".empty()")
                    .collect(Collectors.joining("&&"));
            Scope curGroup = new Scope(group.stream().map(l ->l.name).collect(Collectors.joining(" ")), "while(" + cond + ")");
            //for each non-empty delta
            for(Label l : group){
                Scope curDeltaScope = new Scope("delta " + l.name, "if(!" + idxDelta(l) + ".empty())");
                line("relation<adt_t> cur_delta(" + idxDelta(l) + ".volume());");
                line("cur_delta.swap_contents(" + idxDelta(l) + ");");
                for(LabelUse usage : l.usages) if(usage.usedInRule.ruleHead != usage){
                    generateDeltaExpansion(usage);
                }
                curDeltaScope.popMe();
            }
            curGroup.popMe();
        }
    }

    private void generateDeltaExpansion(LabelUse delta){
        Rule rule = delta.usedInRule;
        int stackLength = scopeStack.size();
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
        if(usesWithoutFields.size() != 0) new Scope("without fields", "if(" + getNonEmptyChecks(usesWithoutFields.stream(), delta) + ")");
        if(rule.allFieldReferences.size() != 0) {
            new Scope("field projections", rule.allFieldReferences.stream()
                    .map(dp -> String.format("for(unsigned %s=0; %s<%s; ++%s)", varProject(dp), varProject(dp), idxField(dp.referencedField), varProject(dp)))
                    .collect(Collectors.joining(" "))); // TODO parallelise this
        }
        if(usesWithFields.size() != 0) new Scope("with fields", "if(" + getNonEmptyChecks(usesWithFields.stream(), delta) + ")");
        new DeltaExpansionVisitor(delta, null, null).visit(rule.ruleBody);
        if(scopeStack.size() > stackLength) scopeStack.get(stackLength).popMe();
    }

    private class DeltaExpansionVisitor implements Clause.Visitor<Void> { //TODO PAIR
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
            //
            // TODO WRITE ME WITH PAIR
            //
            return null;
        }

        @Override
        public Void visitIntersect(Clause.Intersect cl) {
            throw new RuntimeException("Intersection is not supported"); // TODO
        }

        @Override
        public Void visitReverse(Clause.Reverse cl) {
            new DeltaExpansionVisitor(delta, toContext, fromContext).visit(cl.sub);
            //TODO actual logic
            return null;
        }

        @Override
        public Void visitNegate(Clause.Negate cl) {
            throw new RuntimeException("Negation is not supported"); // TODO
        }

        @Override
        public Void visitLabelUse(LabelUse cl) {
            return null;
        }

        @Override
        public Void visitEpsilon(Clause.Epsilon cl) {
            throw new RuntimeException("Epsilon is not supported"); // TODO
        }
    }

    private String getNonEmptyChecks(Stream<LabelUse> slu, final LabelUse delta){
        return slu.map(lu -> "!"
                + relationAccess((lu == delta ? "cur_delta" : idxRel(lu.usedLabel)), lu)
                + ".empty()").collect(Collectors.joining("&&"));
    }

    /**
     * Pretty-printing utilities
     */
    private boolean pretty(){
        return true;
    }
    private void line(){
        //print no leading whitespace
        if(pretty()) outputStream.println();
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
    public static String idxRel(Label l){
        return "relations[" + idxer(l) + "]";
    }
    public static String idxField(Domain d){
        return "volume[" + idxer(d) + "]";
    }
    public static String varProject(DomainProjection dp){
        return "f_" + dp.name;
    }
    public static String relationAccess(String relation, LabelUse l){
        StringBuilder vn = new StringBuilder();
        vn.append(relation);
        vn.append(".adts[");
        if(l.usedLabel.fieldDomainCount == 0){
            vn.append(0);
        } else {
            int fi = 0;
            for(DomainProjection dp : l.usedField){
                if(fi > 0) vn.append(" + ");
                vn.append(varProject(dp));
                for(int vi=fi+1; vi<l.usedLabel.fieldDomainCount; vi++){
                    vn.append("*").append(idxField(l.usedLabel.fieldDomains.get(vi)));
                }
                fi++;
            }
        }
        return vn.append("]").toString();
    }

    /**
     * Generates structured scope bits, assists in pretty printing
     */
    private class Scope {
        public final String name;
        public Scope(String n, String c){
            this.name = n;
            line();
            line(c + " { // " + name);
            scopeStack.push(this);
        }
        public List<Scope> popMe(){
            List<Scope> ret = new ArrayList<>();
            Scope cur = null;
            while(cur != this){
                cur = scopeStack.pop();
                line("} // " + cur.name);
                line();
                ret.add(cur);
            }
            return ret;
        }
    }

}
