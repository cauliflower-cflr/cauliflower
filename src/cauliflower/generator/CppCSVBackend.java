package cauliflower.generator;

import cauliflower.cflr.Label;
import cauliflower.cflr.Problem;
import cauliflower.util.CFLRException;
import cauliflower.util.Registrar;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Generates C++ code to read a problem from a directory of CSV files and execute it
 *
 * Created by nic on 1/12/15.
 */
public class CppCSVBackend implements Backend {

    private final PrintStream out;
    private final String snPath;
    private final Registrar labelReg;
    private final Registrar fieldReg;
    private final boolean verbose;

    public CppCSVBackend(PrintStream out, String snPath, Registrar labelReg, Registrar fieldReg, boolean verbose){
        this.out = out;
        this.snPath = snPath;
        this.labelReg = labelReg;
        this.fieldReg = fieldReg;
        this.verbose = verbose;
    }

    @Override
    public void generate(String problemName, Problem prob) throws CFLRException {
        if (problemName.contains(" ")) throw new CFLRException("Problem name has spaces: \"" + problemName + "\"");
        generatePreBlock(problemName);
        generateImportsUsing();
        generateMainStart(prob);
        generateRegistrars(prob);
        generateRelationImportSolve(prob, problemName);
        generateMainEnd(prob);
    }

    private void generatePreBlock(String problemName){
        out.println("// " + problemName);
        out.println("//");
        out.println("// Automatic importer for CFL-R Problems");
        out.println("//");
        out.println("// Generated on: " + new SimpleDateFormat("dd/MM/yyyy").format(new Date()));
        out.println("//           by: v" + cauliflower.Main.MAJOR + "." + cauliflower.Main.MINOR + "." + cauliflower.Main.REVISION);
    }

    private void generateImportsUsing(){
        out.println("#include <chrono>");
        out.println("#include <iostream>");
        out.println("#include <string>");
        out.println("#include \"relation_buffer.h\"");
        out.println("#include \"" + snPath + "\"");
        out.println("using namespace std;");
        out.println("using namespace std::chrono;");
        out.println("using namespace cflr;");
    }

    private void generateMainStart(Problem prob){
        out.println("int main(int argc, char* argv[]){");
        out.println("// Confirm the CSV directory has been provided as an argument");
        out.println("if(argc < 2){");
        out.println("cerr << \"Usage: \" << argv[0] << \" <path-to-input-csv-directory> [output-relations...]\" << endl;");
        out.println("return 1;");
        out.println("}");
    }

    private void generateRegistrars(Problem prob) {
        out.println("// The group of string registrars");
        if(verbose){
            out.println("steady_clock::time_point time0 = steady_clock::now();");
        }
        out.print("registrar_group<");
        for(int i=0; i<prob.numDomains; i++){
            if(i != 0) out.print(", ");
            out.print("string");
        }
        out.println("> regs;");

        int j=0;
        for(Label l : prob.labels){
            generateBufferDeclaration(l, "buf_" + j++);
        }

        out.println("// Import the CSV files into the registrars");
        for(int i=0; i<prob.labels.size(); i++){
            out.println("buf_" + i + ".from_csv(string(argv[1]) + \"/" + labelReg.fromIndex(i) + ".csv\");");
        }
    }

    private void generateRelationImportSolve(Problem prob, String problemName) {
        out.println("// Load the registrars into relations");
        if(verbose){
            out.println("steady_clock::time_point time1 = steady_clock::now();");
        }
        out.println("typedef " + CppSerialBackend.className(problemName) + " P;");
        out.println("P::vols_t vols = regs.volumes();");
        out.print("P::rels_t relations = {");
        int j=0;
        for(Label l : prob.labels){
            if(j != 0) out.print(",");
            out.print("relation<P::adt_t>(1");
            for(int fd : l.fDomains) out.print("*vols[" + fd + "]");
            out.print(")");
            j++;
        }
        out.println("};");
        for(int i=0; i<prob.labels.size(); i++){
            out.println("relations[" + i + "].import_buffer(buf_" + i + ");");
        }

        out.println("// Solve the problem");
        if(verbose){
            out.println("steady_clock::time_point time2 = steady_clock::now();");
        }
        out.println("P::solve(vols, relations);");
        if(verbose){
            out.println("steady_clock::time_point time3 = steady_clock::now();");
        }
    }

    private void generateBufferDeclaration(Label lbl, String name){
        out.print("relation_buffer<");
        for(int i=0; i<lbl.fDomains.size() + 2; i++){
            if(i != 0) out.print(",");
            out.print("string");
        }
        out.print("> " + name + "(regs.select<" + (lbl.fromDomain + this.fieldReg.size()) + "," + (lbl.toDomain + this.fieldReg.size()));
        for(int fd : lbl.fDomains) out.print("," + fd);
        out.println(">());");
    }

    private void generateMainEnd(Problem prob){
        out.println("// print the specified relations to stdout");
        out.println("for(int i=2; i<argc; ++i){");
        for(int l=0; l<prob.labels.size(); l++){
            if(l != 0) out.print("else ");
            out.println("if(string(argv[i]) == \"" + labelReg.fromIndex(l) + "\"){");
            out.println("cout << \"__" + labelReg.fromIndex(l) + "__\" << endl;");
            generateBufferDeclaration(prob.labels.get(l), "tmp_buf");
            out.println("relations[" + l  + "].export_buffer(tmp_buf);");
            out.println("tmp_buf.to_csv(cout);");
            out.println("}");
        }
        out.println("}");
        if(verbose){
            out.println("steady_clock::time_point time4 = steady_clock::now();");
            out.println("cerr << \"        input csv files: \" << duration_cast<duration<double>>(time1 - time0).count() << endl;");
            out.println("cerr << \"convert csv to relation: \" << duration_cast<duration<double>>(time2 - time1).count() << endl;");
            out.println("cerr << \"       solve semi-naive: \" << duration_cast<duration<double>>(time3 - time2).count() << endl;");
            out.println("cerr << \"       output csv files: \" << duration_cast<duration<double>>(time4 - time3).count() << endl;");
        }
        out.println("return 0;");
        out.println("}");
    }
}
