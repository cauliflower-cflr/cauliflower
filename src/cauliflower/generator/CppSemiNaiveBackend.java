package cauliflower.generator;

import cauliflower.cflr.Problem;
import cauliflower.cflr.Rule;

import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * CppSemiNaiveBackend.java
 *
 * generates code for an execution of the semi-naive code
 *
 * Created by nic on 25/11/15.
 */
public class CppSemiNaiveBackend implements Backend{

    public enum Adt {
        StdTree("neighbourhood_map<std::map<ident, std::set<ident>>, std::set<ident>>", "neighbourhood_map.h", "<map>", "<set>"),
        Quadtree("concise_tree", "concise_tree.h");

        private String typename;
        private String importLoc;
        private List<String> imports;
        Adt(String typename, String importLoc, String... imports){
            this.typename = typename;
            this.importLoc = importLoc;
            this.imports = Arrays.asList(imports);
        }
    }

    private final Adt adt;
    private final PrintStream out;

    public CppSemiNaiveBackend(Adt adt, PrintStream out){
        this.adt = adt;
        this.out = out;
    }

    private void generatePreBlock(String problemName){
        out.println("// " + problemName);
        out.println("//");
        out.println("// Generated on: " + new SimpleDateFormat("dd/MM/yyyy").format(new Date()));
        out.println("//           by: v" + cauliflower.Main.MAJOR + "." + cauliflower.Main.MINOR + "." + cauliflower.Main.REVISION);
    }

    private void generateImports(){
        out.println("#include <array>");
        //out.println("#include <tuple>");
        //out.println("#include <vector>");
        for(String imp : adt.imports){
            out.println("#include " + imp);
        }
        out.println("#include \"" + adt.importLoc + "\"");
        out.println("#include \"relation.h\"");
    }

    private void generateScope(String problemName){
        out.println("namespace cflr {");
        out.println("struct " + problemName + "_semi_naive {");
    }

    private void generateDefs(Problem prob){
        out.println("// Definitions");
        out.println("static const unsigned num_lbls = " + prob.labels.size() + ";");
        out.println("static const unsigned num_domains = " + prob.numDomains + ";");
        out.println("typedef " + adt.typename + " adt_t;");
        out.println("typedef std::array<relation<adt_t>, num_lbls> rels_t;");
        out.println("typedef std::array<size_t, num_domains> vols_t;");
    }

    private void generateRuleCode(int l, int r, int occurance, Problem prob){
        Rule rule = prob.rules.get(r);
        // TODO irrelevant field optimisation:  in head, iterate over irrelevant and assign (dont evaluate rule again), in body, union all irrelevant fields
        out.println("// Label " + l + ", occurance " + occurance + ", rule " + prob.rules.get(r).toString());
        // TODO field selection
        out.println("for(unsigned i=0; i<1; ++i){");
        int deltaClause = -1;
        for(int clause = 0; clause< rule.body.size(); clause++){
            int deltasInClause = (int)rule.body.get(clause).getDependantLabels().stream().map(lbl -> lbl.label).filter(lbl -> lbl == l).count();
            if(occurance-deltasInClause < 0){
                deltaClause = clause;
                break;
            } else occurance -= deltasInClause;
        }
        assert deltaClause != -1;
        int tempCount = 0;
        for(int c=deltaClause+1; c<rule.body.size(); c++){
            out.println("adt_t tmp" + tempCount + ";");
            String left = tempCount == 0 ? "cur_delta.adts[0]" : "tmp" + (tempCount-1);
            out.println(left + ".compose(relations[" + ((Rule.Lbl)rule.body.get(c)).label + "].adts[0], tmp" + tempCount + ");");
            tempCount++;
        }
        for(int c=deltaClause-1; c>=0; c--){
            out.println("adt_t tmp" + tempCount + ";");
            String right = tempCount == 0 ? "cur_delta.adts[0]" : "tmp" + (tempCount-1);
            out.println("relations[" + ((Rule.Lbl)rule.body.get(c)).label + "].adts[0].compose(" + right + ", tmp" + tempCount + ");");
            tempCount++;
        }
        if(tempCount == 0){
            out.println("adt_t tmp0;");
            out.println("cur_delta.deep_copy(tmp0);");
            tempCount = 1;
        }
        tempCount -= 1;//because i cant be bothered subtracting 1s
        out.println("tmp" + tempCount + ".difference(relations[" + rule.head.label + "].adts[0]);");
        out.println("deltas[" + rule.head.label + "].adts[0].union_copy(tmp" + tempCount + ");");
        out.println("relations[" + rule.head.label + "].adts[0].union_absorb(tmp" + tempCount + ");");
        out.println("}");
    }

    private String deltaExpansionFunctionName(int lbl, boolean argTypes){
        return "delta_" + lbl + "(" + (argTypes ? "const vols_t& " : "") + "volume, " + (argTypes ? "rels_t& " : "") + "relations, " + (argTypes ? "rels_t& " : "") + "deltas)";
    }

    private void generateDeltaExpansionCode(int l, Problem prob){
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

    private void generateDeltas(Problem prob){
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
        for(Rule r : prob.rules) if(r.body.size() == 0){
            out.println("for(auto& a : relations[" + r.head.label + "].adts) a.union_copy(epsilon);");
        }

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

    @Override
    public void generate(String problemName, Problem prob) throws Exception{
        if (problemName.contains(" ")) throw new IOException("Problem name has spaces: \"" + problemName + "\"");
        generatePreBlock(problemName);
        generateImports();
        generateScope(problemName);
        generateDefs(prob);
        generateDeltas(prob);
        generateSemiNaive(prob);
        endScope(problemName);
    }

}
