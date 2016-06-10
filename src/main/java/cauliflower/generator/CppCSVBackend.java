package cauliflower.generator;

import cauliflower.representation.Domain;
import cauliflower.representation.Label;
import cauliflower.representation.Problem;
import cauliflower.util.CFLRException;

import java.io.PrintStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Generates C++ code to read a problem from a directory of CSV files and execute it
 * <p>
 * Created by nic on 1/12/15.
 */
public class CppCSVBackend {

    private final PrintStream out;
    private final String problemName;
    private final String snPath;
    private final Problem prob;
    private final boolean reports;

    public CppCSVBackend(String problemName, String snPath, Problem po, boolean rep, PrintStream out) {
        this.out = out;
        this.problemName = problemName;
        this.snPath = snPath;
        this.prob = po;
        this.reports = rep;
    }

    public void generate() throws CFLRException {
        if (problemName.contains(" ")) throw new CFLRException("Problem name has spaces: \"" + problemName + "\"");
        GeneratorUtils.generatePreBlock(problemName, "Automatic CSV-based importer for CFL-R Problems", this.getClass(), out);
        generateImportsUsing();
        generateMainStart();
        generateRegistrars();
        generateRelationImportSolve();
        generateMainEnd();
    }

    private void generateImportsUsing() {
        out.println("#include <chrono>");
        out.println("#include <iostream>");
        out.println("#include <string>");
        out.println("#include \"relation_buffer.h\"");
        out.println("#include \"" + snPath + "\"");
        out.println("using namespace std;");
        out.println("using namespace std::chrono;");
        out.println("using namespace cflr;");
    }

    private void generateMainStart() {
        out.println("int main(int argc, char* argv[]){");
        out.println("typedef " + CppSerialBackend.className(problemName) + " P;");
        out.println("// Confirm the CSV directory has been provided as an argument");
        out.println("if(argc < 2){");
        out.println("cerr << \"Usage: \" << argv[0] << \" <path-to-input-csv-directory> [output-relations...]\" << endl;");
        out.println("return 1;");
        out.println("}");
    }

    private void generateRegistrars() {
        out.println("// The group of string registrars");
        if (reports) {
            out.println("steady_clock::time_point time0 = steady_clock::now();");
        }
        out.print("registrar_group<");
        for (int i = 0; i < prob.vertexDomains.size() + prob.fieldDomains.size(); i++) {
            if (i != 0) out.print(", ");
            out.print("string");
        }
        out.println("> regs;");

        prob.labels.stream().forEach(l -> generateBufferDeclaration(l, "buf_" + l.name));

        out.println("// Import the CSV files into the registrars");
        prob.labels.stream().forEach(l -> out.println("buf_" + l.name + ".from_csv(string(argv[1]) + \"/" + l.name + ".csv\");"));
    }

    private void generateRelationImportSolve() {
        out.println("// Load the registrars into relations");
        if (reports) {
            out.println("steady_clock::time_point time1 = steady_clock::now();");
        }
        out.println("P::vols_t vols = regs.volumes();");
        out.print("P::rels_t relations = {");
        out.print(prob.labels.stream()
                .map(l -> l.fieldDomains.stream().map(d -> "vols[" + d.index + "]").collect(Collectors.joining("*")))
                .map(s -> s.length() == 0 ? "1" : s)
                .map(s -> "relation<P::adt_t>(" + s + ")")
                .collect(Collectors.joining(",")));
        out.println("};");
        prob.labels.stream().forEach(l -> out.println("relations[" + pseudonym(l) + "].import_buffer(buf_" + l.name + ");"));

        out.println("// Solve the problem");
        if (reports) {
            out.println("steady_clock::time_point time2 = steady_clock::now();");
        }
        out.println("P::solve(vols, relations);");
        if (reports) {
            out.println("steady_clock::time_point time3 = steady_clock::now();");
        }
    }

    public static String pseudonym(Domain dom) {
        return "P::" + CppSemiNaiveBackend.idxer(dom);
    }

    public static String pseudonym(Label l) {
        return "P::" + CppSemiNaiveBackend.idxer(l);
    }

    private static String bufferDeclaration(Label lbl, String name) {
        return "relation_buffer<"
                + Stream.generate(() -> "string").limit(lbl.fieldDomainCount + 2).collect(Collectors.joining(","))
                + "> " + name + "(regs.select<" + pseudonym(lbl.srcDomain) + "," + pseudonym(lbl.dstDomain)
                + lbl.fieldDomains.stream().map(CppCSVBackend::pseudonym).map(s -> "," + s).collect(Collectors.joining())
                + ">())";
    }

    private void generateBufferDeclaration(Label lbl, String name) {
        out.println(bufferDeclaration(lbl, name) + ";");
    }

    private void generateMainEnd() throws CFLRException {
        out.println("// print the specified relations to stdout");
        out.println("for(int i=2; i<argc; ++i){");
        out.println(prob.labels.stream()
                .map(l -> "if(string(argv[i]) == \"" + l.name + "\"){\n" +
                        "cout << \"__" + l.name + "__\" << endl;\n" +
                        bufferDeclaration(l, "tmp_buf") + ";\n" +
                        "relations[" + pseudonym(l) + "].export_buffer(tmp_buf);\n" +
                        "tmp_buf.to_csv(cout);\n" +
                        "}")
                .collect(Collectors.joining(" else ")));
        out.println("}");
        if (reports) {
            out.println("steady_clock::time_point time4 = steady_clock::now();");
            out.println("cerr << \"input csv files=\" << duration_cast<duration<double>>(time1 - time0).count() << endl;");
            out.println("cerr << \"convert csv to relation=\" << duration_cast<duration<double>>(time2 - time1).count() << endl;");
            out.println("cerr << \"solve semi-naive=\" << duration_cast<duration<double>>(time3 - time2).count() << endl;");
            out.println("cerr << \"output csv files=\" << duration_cast<duration<double>>(time4 - time3).count() << endl;");
            prob.labels.stream().forEach(l -> {
                generateBufferDeclaration(l, "count_buf_" + l.name);
                out.println("relations[" + pseudonym(l) + "].export_buffer(count_buf_" + l.name + ");");
                out.println("cerr << \"|" + l.name + "|=\" << count_buf_" + l.name + ".size() << endl;");
            });
            prob.fieldDomains.stream().forEach(d -> out.println("cerr << \"f:" + d.name + "=\" << vols[" + pseudonym(d) + "] << std::endl;"));
            prob.vertexDomains.stream().forEach(d -> out.println("cerr << \"v:" + d.name + "=\" << vols[" + pseudonym(d) + "] << std::endl;"));
        }
        out.println("return 0;");
        out.println("}");
    }
}
