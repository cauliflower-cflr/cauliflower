package cauliflower.generator;

import cauliflower.cflr.Label;
import cauliflower.cflr.Problem;
import cauliflower.cflr.Rule;
import cauliflower.util.CFLRException;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * CppSemiNaiveBackend.java
 *
 * generates code for an execution of the semi-naive code
 *
 * Created by nic on 25/11/15.
 */
public class CppSouffleBackend implements Backend{

    public static final int PARTITION_COUNT = 400; // arbitrary value i borrowed from souffle

    private final Adt adt;
    private final PrintStream out;
    private final boolean useTimers;

    public CppSouffleBackend(Adt adt, PrintStream out, boolean timers){
        this.adt = Adt.Souffle; // force this ADT
        this.out = out;
        this.useTimers = timers;
    }

    @Override
    public void generate(String problemName, Problem prob) throws CFLRException{
        if (problemName.contains(" ")) throw new CFLRException("Problem name has spaces: \"" + problemName + "\"");
        generatePreBlock(problemName);
        generateImports();
        generateScope(problemName);
        generateDefs(prob);
        generateDeltas(prob);
        generateSemiNaive(prob);
        endScope(problemName);
    }

    private void generatePreBlock(String problemName){
        out.println("// " + problemName);
        out.println("//");
        out.println("// Semi-naive method fore evaluating CFLR solutions");
        out.println("//");
        out.println("// Generated on: " + new SimpleDateFormat("dd/MM/yyyy").format(new Date()));
        out.println("//           by: v" + cauliflower.Main.MAJOR + "." + cauliflower.Main.MINOR + "." + cauliflower.Main.REVISION);
    }

    private void generateImports(){
        out.println("#include <array>");
        out.println("#include <omp.h>");
        for(String imp : adt.imports){
            out.println("#include " + imp);
        }
        out.println("#include \"" + adt.importLoc + "\"");
        out.println("#include \"relation.h\"");
        out.println("#include \"Util.h\"");
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

    private class RuleCascadeGenerator implements Rule.ClauseVisitor{
        final int deltaLabel;
        final int deltaOccurrence;
        final int ruleIndex;
        final Rule rule;
        final Problem prob;
        final boolean doParallel;
        int closeCounter = 0; // closing braces for the variable number of opened ones
        int deltasEncountered = 0;
        boolean partitioned = false;
        boolean shouldReverse = false;
        private int numIters;

        public RuleCascadeGenerator(int dlbl, int docc, int ri, Rule r, Problem p, boolean doParallel){
            deltaLabel = dlbl;
            deltaOccurrence = docc;
            ruleIndex = ri;
            rule = r;
            prob = p;
            this.doParallel = doParallel;
        }

        public void generate(){
            out.println("adt_t::tree_t tmp_forwards;");
            out.println("adt_t::tree_t tmp_backwards;");
            // expand on every clause in the rule
            numIters = 0;
            for(Rule.Clause cls : rule.body){
                shouldReverse = false;
                this.visit(cls);
            }
            out.println("adt_t::tree_t::entry_type fwd({{iter1[0], iter" + numIters + "[1]}});");
            // update the temporary output
            String target = labelRel("relations[" + rule.head.label + "]", rule.head);
            out.println("if(!" + target + ".forwards.contains(fwd, hint_rel" + rule.head.label + ")){");
            out.println("tmp_forwards.insert(fwd);");
            out.println("tmp_backwards.insert({{iter" + numIters + "[1], iter1[0]}});");
            closeCounter++;

            // close all the scopes, except the parallel scope
            for(int i=0; i<closeCounter; i++) out.println("}");
            // update the relation and delta with their respective news:
            generateTimeStart("update" + deltaLabel + "_" + deltaOccurrence + "_" + ruleIndex);
            String targetDelt = labelRel("deltas[" + rule.head.label + "]", rule.head);
            //out.println("#pragma omp sections");
            //out.println("{");
            //out.println("#pragma omp section");
            //out.println("{");
            out.println(target + ".forwards.insertAll(tmp_forwards);");
            //out.println("}");
            //out.println("#pragma omp section");
            //out.println("{");
            out.println(target + ".backwards.insertAll(tmp_backwards);");
            //out.println("}");
            //out.println("#pragma omp section");
            //out.println("{");
            out.println(targetDelt + ".forwards.insertAll(tmp_forwards);");
            //out.println("}");
            //out.println("#pragma omp section");
            //out.println("{");
            out.println(targetDelt + ".backwards.insertAll(tmp_backwards);");
            //out.println("}");
            //out.println("}");//closes sections
            generateTimeEnd("update" + deltaLabel + "_" + deltaOccurrence + "_" + ruleIndex);
            //out.println("}");//closes parallel scope
        }

        @Override
        public void visitLbl(Rule.Lbl l) {
            String rel = labelRel("relations[" + l.label + "]", l);
            String ctxtHint = "hint_rel" + l.label;
            if(l.label == deltaLabel){
                if(deltaOccurrence == deltasEncountered){
                    rel = labelRel("cur_delta", l);
                    ctxtHint = "hint_dlt" + l.label;
                }
                deltasEncountered++;
            }
            //TODO begin with a smarter relation (i.e. the one that joins with the delta, minimal skew)
            rel = rel + (shouldReverse ? ".backwards" : ".forwards");
            if(partitioned){
                //project the equality range
                out.println("auto range" + numIters + " = " + rel + ".getBoundaries<1>({{iter" + numIters + "[1], 0}}, " + ctxtHint + ");");
                //iterate over the relation
                out.println("for(const auto& iter" + (numIters+1) + " : range" + numIters + ") {");
                closeCounter++;
                numIters++;
            } else {
                //iterate over the primary label after partitioning
                out.println("auto primary_partition = " + rel + ".partition(" + PARTITION_COUNT + ");");
                if(doParallel) out.println("# pragma omp parallel");
                out.println("{");
                // create the join hints
                Stream.concat(Stream.of(rule.head), rule.dependencies.stream()).map(lb -> lb.label).distinct().forEach(i -> {
                    out.println("adt_t::tree_t::op_context hint_rel" + i + ";");
                });
                out.println("adt_t::tree_t::op_context hint_dlt" + deltaLabel + ";");
                // for each partition in parallel
                if(doParallel) out.println("# pragma omp for schedule(dynamic)"); // TODO different schedules
                out.println("for (auto primary_index = primary_partition.begin(); primary_index<primary_partition.end(); ++primary_index){");
                out.println("for(const auto& iter" + (numIters+1) + " : *primary_index){");
                partitioned = true;
                closeCounter += 3;
                numIters++;
            }
        }

        @Override
        public void visitRev(Rule.Rev r) {
            shouldReverse = !shouldReverse;
            this.visit(r.clause);
        }

        @Override
        public void visitNeg(Rule.Neg n) {
            throw new UnsupportedOperationException("Negation to be done");
        }

        @Override
        public void visitAnd(Rule.And a) {
            throw new UnsupportedOperationException("Intersection to be done");
        }
    }

    private void generateRuleCode(int l, int r, int occurrence, Problem prob) throws CFLRException {
        Rule rule = prob.rules.get(r);
        // TODO irrelevant field optimisation:  in head, iterate over irrelevant and assign (dont evaluate rule again), in body, union all irrelevant fields
        out.println("// Label " + l + ", occurrence " + occurrence + ", rule " + prob.rules.get(r).toString());
        generateTimeStart("eval" + l + "_" + occurrence + "_" + r);
        Map<Integer, int[]> fieldIdents = prob.ruleFieldDomainMapping(r);
        boolean shouldParallel = true;
        for(int ident : fieldIdents.keySet()){ // TODO irrelevant field: if get[1] > 0...
            if(shouldParallel){
                out.println("#pragma omp parallel for schedule(auto)");
            }
            out.print("for(unsigned f" + ident + "=0; f" + ident + "<volume[" + fieldIdents.get(ident)[0] + "]; ++f" + ident + ") ");
            shouldParallel = false;
        }
        int dSeen = 0;
        for(Rule.Lbl lbl : rule.dependencies){
            if(lbl.label != l || dSeen-occurrence != 0 || !lbl.fields.isEmpty()) out.print("if(!" + labelRel("relations[" + lbl.label + "]", lbl) + ".empty()) ");
            if(lbl.label == l) dSeen ++;
        }
        out.println("{");
        RuleCascadeGenerator gen = new RuleCascadeGenerator(l, occurrence, r, rule, prob, shouldParallel);
        gen.generate();
        out.println("}"); // close the ifs
        generateTimeEnd("eval" + l + "_" + occurrence + "_" + r);
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
                out.println("if (!deltas[" + cc + "].empty()){");
                generateTimeStart("delta" + cc);
                out.println(deltaExpansionFunctionName(cc, false) + ";");
                generateTimeEnd("delta" + cc);
                out.println("continue; }");
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

    private void generateTimeStart(String name){
        if(useTimers) out.println("auto tstart_" + name + " = now();");
    }

    private void generateTimeEnd(String name){
        if(useTimers) out.println("auto tend_" + name + " = now(); std::cerr << \"TIME \" << omp_get_thread_num() << \" " + name + " \" << duration_in_ms(tstart_" + name + ", tend_" + name + ") << std::endl;");
    }

    private void generateCounterDecl(String name){
        out.println("unsigned ctr_" + name + " = 0;");
    }

    private void generateCounterIncr(String name){
        out.println("ctr_" + name + "++;");
    }

    private void generateCounterReport(String name){
        out.println("std::cerr << \"COUNT \" << omp_get_thread_num() << \" " + name + " \" << ctr_" + name + " << std::endl;");
    }

}
