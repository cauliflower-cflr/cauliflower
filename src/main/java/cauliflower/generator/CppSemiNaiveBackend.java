package cauliflower.generator;

import cauliflower.application.Configuration;
import cauliflower.representation.*;
import cauliflower.util.CFLRException;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
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
                curDeltaScope.popMe();
            }
            curGroup.popMe();
        }
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
