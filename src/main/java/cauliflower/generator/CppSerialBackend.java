package cauliflower.generator;

import cauliflower.cflr.Problem;
import cauliflower.cflr.Rule;
import cauliflower.util.CFLRException;

import java.io.PrintStream;
import java.util.List;
import java.util.Map;

/**
 * CppSerialBackend.java
 *
 * generates code for an execution of the semi-naive code
 *
 * Created by nic on 25/11/15.
 */
public class CppSerialBackend{

    private final Adt adt;
    private final PrintStream out;

    public CppSerialBackend(Adt adt, PrintStream out){
        this.adt = adt;
        this.out = out;
    }

    public void generate(String problemName, Problem prob) throws CFLRException{
        if (problemName.contains(" ")) throw new CFLRException("Problem name has spaces: \"" + problemName + "\"");
        GeneratorUtils.generatePreBlock(problemName, "Semi-naive method fore evaluating CFLR solutions", this.getClass(), out);
        generateImports();
        generateScope(problemName);
        generateDefs(prob);
        generateDeltas(prob);
        generateSemiNaive(prob);
        endScope(problemName);
    }

    private void generateImports(){
        out.println("#include <array>");
        for(String imp : adt.imports){
            out.println("#include " + imp);
        }
        out.println("#include \"" + adt.importLoc + "\"");
        out.println("#include \"relation.h\"");
    }

    public static String className(String problemName){
        return problemName + "_semi_naive";
    }

    private void generateScope(String problemName){
        out.println("namespace cflr {");
        out.println("struct " + className(problemName) + " {");
    }

    private void generateDefs(Problem prob){
        out.println("// Definitions");
        out.println("static const unsigned num_lbls = " + prob.labels.size() + ";");
        out.println("static const unsigned num_domains = " + prob.numDomains + ";");
        out.println("typedef " + adt.typename + " adt_t;");
        out.println("typedef std::array<relation<adt_t>, num_lbls> rels_t;");
        out.println("typedef std::array<size_t, num_domains> vols_t;");
    }

    private String labelRel(String ref, Rule.Lbl l){
        StringBuilder vn = new StringBuilder();
        vn.append(ref);
        vn.append(".adts[");
        if(l.fields.size() == 0){
            vn.append(0);
        } else {
            int fi = 0;
            for(int f : l.fields){
                if(fi > 0) vn.append(" + ");
                vn.append("f").append(f);
                for(int vi=fi+1; vi<l.fields.size(); vi++){
                    vn.append("*volume[").append(l.fieldDomains.get(vi)).append("]");
                }
                fi++;
            }
        }
        return vn.append("]").toString();
    }

    private class GeneratedClause implements Rule.ClauseVisitor{
        public boolean reversed = false;
        public boolean negated = false;
        public String varName;
        private final Rule.Clause clause;
        public int curTemps;
        private final int dl;
        private int docc;
        public GeneratedClause(int curTemps){
            this.varName = "tmp" + curTemps;
            this.curTemps = curTemps+1;
            this.dl = -1;
            this.clause = null;
        }
        public GeneratedClause(Rule.Clause clause, int curTemps, int dl, int docc) {
            this.clause = clause;
            this.curTemps = curTemps;
            this.dl = dl;
            this.docc = docc;
            this.visit(this.clause);
        }
        private GeneratedClause subClause(Rule.Clause c){
            return new GeneratedClause(c, curTemps, dl, docc);
        }
        @Override
        public void visitLbl(Rule.Lbl lbl) {
            if(lbl.label == dl && docc == 0){
                varName = labelRel("cur_delta", lbl);
            } else {
                varName = labelRel("relations[" + lbl.label + "]", lbl);
            }
            if(lbl.label == dl) this.docc--;
        }
        @Override
        public void visitRev(Rule.Rev r) {
            GeneratedClause sub = subClause(r.clause);
            this.varName = sub.varName;
            this.negated = sub.negated;
            this.reversed = !sub.reversed;
            this.curTemps = sub.curTemps;
            this.docc = sub.docc;
        }
        @Override
        public void visitNeg(Rule.Neg n) {
            GeneratedClause sub = subClause(n.clause);
            this.varName = sub.varName;
            this.negated = !sub.negated;
            this.reversed = sub.reversed;
            this.curTemps = sub.curTemps;
            this.docc = sub.docc;
        }
        @Override
        public void visitAnd(Rule.And a) {
            GeneratedClause lft = subClause(a.left);
            this.curTemps = lft.curTemps;
            this.docc = lft.docc;
            GeneratedClause rgh = subClause(a.right);
            if(lft.negated && rgh.negated) throw new RuntimeException("Unhandled - double negation not implemented");
            if((lft.negated || rgh.negated) && (lft.reversed || rgh.reversed)) throw new RuntimeException("Unhandled - negation and reversal");
            GeneratedClause tmp = new GeneratedClause(rgh.curTemps); // used to stub myself
            this.varName = tmp.varName;
            this.curTemps = tmp.curTemps;
            this.docc = rgh.docc;
            out.println("adt_t " + this.varName + ";");
            if(lft.negated){
                out.println(rgh.varName + ".deep_copy(" + this.varName + ");");
                out.println(this.varName + ".difference(" + lft.varName + ");");
            } else if (rgh.negated) {
                out.println(lft.varName + ".deep_copy(" + this.varName + ");");
                out.println(this.varName + ".difference(" + rgh.varName + ");");
            } else {
                out.println(lft.varName + ".intersect<" + lft.reversed + ", " + rgh.reversed + ">(" + rgh.varName + ", " + this.varName + ");");
            }
        }
    }

    private void generateRuleCode(int l, int r, int occurance, Problem prob) throws CFLRException {
        Rule rule = prob.rules.get(r);
        // TODO irrelevant field optimisation:  in head, iterate over irrelevant and assign (dont evaluate rule again), in body, union all irrelevant fields
        out.println("// Label " + l + ", occurance " + occurance + ", rule " + prob.rules.get(r).toString());
        Map<Integer, int[]> fieldIdents = prob.ruleFieldDomainMapping(r);
        for(int ident : fieldIdents.keySet()){ // TODO irrelevant field: if get[1] > 0...
            out.print("for(unsigned f" + ident + "=0; f" + ident + "<volume[" + fieldIdents.get(ident)[0] + "]; ++f" + ident + ") ");
        }
        int dSeen = 0;
        for(Rule.Lbl lbl : rule.dependencies){
            if(lbl.label != l || dSeen-occurance != 0 || !lbl.fields.isEmpty()) out.print("if(!" + new GeneratedClause(lbl, 0, l, dSeen-occurance).varName + ".empty()) ");
            if(lbl.label == l) dSeen ++;
        }
        out.println("{");
        int deltaClause = -1;
        for(int clause = 0; clause< rule.body.size(); clause++){
            int deltasInClause = (int)rule.body.get(clause).getDependantLabels().stream().map(lbl -> lbl.label).filter(lbl -> lbl == l).count();
            if(occurance-deltasInClause < 0){
                deltaClause = clause;
                break;
            } else occurance -= deltasInClause;
        }
        assert deltaClause != -1;
        GeneratedClause lastClause = new GeneratedClause(rule.body.get(deltaClause), 0, l, occurance);
        for(int c=deltaClause+1; c<rule.body.size(); c++){
            GeneratedClause thisClause = new GeneratedClause(rule.body.get(c), lastClause.curTemps, l, -1);
            GeneratedClause nextClause = new GeneratedClause(thisClause.curTemps);
            out.println("adt_t " + nextClause.varName + ";");
            out.println(lastClause.varName + ".compose<" + lastClause.reversed + ", " + thisClause.reversed + ">(" + thisClause.varName + ", " + nextClause.varName + ");");
            lastClause = nextClause;
        }
        for(int c=deltaClause-1; c>=0; c--){
            GeneratedClause thisClause = new GeneratedClause(rule.body.get(c), lastClause.curTemps, l, -1);
            GeneratedClause nextClause = new GeneratedClause(thisClause.curTemps);
            out.println("adt_t " + nextClause.varName + ";");
            out.println(thisClause.varName + ".compose<" + thisClause.reversed + ", " + lastClause.reversed + ">(" + lastClause.varName + ", " + nextClause.varName + ");");
            lastClause = nextClause;
        }
        if(lastClause.curTemps == 0){ // occurs for unary productions with no intersection operation
            // TODO handle negation/reversal
            GeneratedClause tempClause = new GeneratedClause(0);
            out.println("adt_t " + tempClause.varName + ";");
            out.println(lastClause.varName + ".deep_copy(" + tempClause.varName + ");");
            lastClause = tempClause;
        }
        String intoR = labelRel("relations[" + rule.head.label + "]", rule.head);
        String intoD = labelRel("deltas[" + rule.head.label + "]", rule.head);
        out.println(lastClause.varName + ".difference(" + intoR + ");");
        out.println("" + intoD + ".union_copy(" + lastClause.varName + ");");
        out.println("" + intoR + ".union_absorb(" + lastClause.varName + ");");
        out.println("}"); // end of field loop
    }

    private String deltaExpansionFunctionName(int lbl, boolean argTypes){
        return "delta_" + lbl + "(" + (argTypes ? "const vols_t& " : "") + "volume, " + (argTypes ? "rels_t& " : "") + "relations, " + (argTypes ? "rels_t& " : "") + "deltas)";
    }

    private void generateDeltaExpansionCode(int l, Problem prob) throws CFLRException{
        out.println("static void " + deltaExpansionFunctionName(l, true) + "{");
        out.println("relation<adt_t> cur_delta(deltas[" + l + "].volume());");
        out.println("cur_delta.swap_contents(deltas[" + l + "]);");
        for(int r=0; r<prob.rules.size(); r++){
            int dCount = 0;
            for(Rule.Lbl dl : prob.rules.get(r).dependencies) if (dl.label == l){
                generateRuleCode(l, r, dCount, prob);
                dCount++;
            }
        }
        out.println("}");
    }

    private void generateDeltas(Problem prob) throws CFLRException{
        out.println("// Delta expansion rules");
        for(int i=0; i<prob.labels.size(); i++){
            generateDeltaExpansionCode(i, prob);
        }
    }

    private void generateSemiNaive(Problem prob){
        out.println("// Solver definition");
        out.println("static void solve(vols_t& volume, rels_t& relations){");

        out.println("// Epsilon initialisation");
        out.println("size_t largest_vertex_domain = 0;");
        for(int i=0; i<prob.numDomains; i++) if(!prob.fields.contains(i)) out.println("largest_vertex_domain = std::max(largest_vertex_domain, volume[" + i + "]);");
        out.println("const adt_t epsilon = adt_t::identity(largest_vertex_domain);");
        prob.rules.forEach(r -> {
            if(r.body.size() == 0) out.println("for(auto& a : relations[" + r.head.label + "].adts) a.union_copy(epsilon);");
        } );

        out.println("// Delta initialisation");
        out.print("rels_t deltas{");
        boolean dprinted = false;
        for(int i=0; i<prob.labels.size(); i++){
            if(dprinted) out.print(", ");
            else dprinted = true;
            out.print("relation<adt_t>(relations[" + i + "].adts.size())");
        }
        out.println("};");
        // TODO initialise deltas in-place via some kind of deep copy
        // i.e. deep copy the whole relation, instead of the individual ADTs
        for(int l=0; l<prob.labels.size(); l++){
            out.println("for(unsigned i=0; i<relations[" + l + "].adts.size(); ++i) relations[" + l + "].adts[i].deep_copy(deltas[" + l + "].adts[i]);");
        }

        for(List<Integer> scc : prob.getLabelDependencyOrdering()){
            out.println("// SCC " + scc.toString());
            // TODO don't iterate over non-cyclic SCCs
            // TODO early exit evaluation when we reach known code
            out.println("while(true){");
            for(int cc : scc){
                out.println("if (!deltas[" + cc + "].empty()){ " + deltaExpansionFunctionName(cc, false) + "; continue; }");
            }
            out.println("break;");
            out.println("}");
        }
        out.println("}");
    }

    private void endScope(String problemName){
        out.println("}; // end struct " + problemName + "_semi_naive");
        out.println("} // end namespace cflr");
    }

}
