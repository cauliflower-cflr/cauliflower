package cauliflower.generator;

import cauliflower.application.CauliflowerException;
import cauliflower.representation.Domain;
import cauliflower.representation.Label;
import cauliflower.util.CFLRException;

import java.io.PrintStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Generates C++ code to read a problem from a directory of CSV files and execute it
 * <p>
 * Created by nic on 1/12/15.
 */
public class CppCSVBackend extends GeneratorForProblem {

    private final String problemName;
    private final String snPath;
    private final boolean reports;

    public CppCSVBackend(String problemName, String snPath, PrintStream outputStream, Verbosity verbosity) {
        super(outputStream, verbosity);
        this.problemName = problemName;
        this.snPath = snPath;
        this.reports = verb.isReporting();
    }

    @Override
    public void performInternal() throws CauliflowerException {
        try {
            generate();
        } catch (CFLRException e) {
            except(e);
        }
    }

    public void generate() throws CFLRException {
        if (problemName.contains(" ")) throw new CFLRException("Problem name has spaces: \"" + problemName + "\"");
        GeneratorUtils.generatePreBlock(problemName, "Automatic CSV-based importer for CFL-R Problems", this.getClass(), outputStream);
        generateImportsUsing();
        generateMainStart();
        generateRegistrars();
        generateRelationImportSolve();
        generateMainEnd();
    }

    private void generateImportsUsing() {
        outputStream.println("#include <chrono>");
        outputStream.println("#include <iostream>");
        outputStream.println("#include <string>");
        outputStream.println("#include \"relation_buffer.h\"");
        outputStream.println("#include \"" + snPath + "\"");
        outputStream.println("using namespace std;");
        outputStream.println("using namespace std::chrono;");
        outputStream.println("using namespace cflr;");
    }

    private void generateMainStart() {
        outputStream.println("int main(int argc, char* argv[]){");
        outputStream.println("typedef " + CppSerialBackend.className(problemName) + " P;");
        outputStream.println("// Confirm the CSV directory has been provided as an argument");
        outputStream.println("if(argc < 2){");
        outputStream.println("cerr << \"Usage: \" << argv[0] << \" <path-to-input-csv-directory> [output-relations...]\" << endl;");
        outputStream.println("return 1;");
        outputStream.println("}");
    }

    private void generateRegistrars() {
        outputStream.println("// The group of string registrars");
        if (reports) {
            outputStream.println("steady_clock::time_point time0 = steady_clock::now();");
        }
        outputStream.print("registrar_group<");
        for (int i = 0; i < prob().vertexDomains.size() + prob().fieldDomains.size(); i++) {
            if (i != 0) outputStream.print(", ");
            outputStream.print("string");
        }
        outputStream.println("> regs;");

        prob().labels.stream().forEach(l -> generateBufferDeclaration(l, "buf_" + l.name));

        outputStream.println("// Import the CSV files into the registrars");
        prob().labels.stream().forEach(l -> outputStream.println("buf_" + l.name + ".from_csv(string(argv[1]) + \"/" + l.name + ".csv\");"));
    }

    private void generateRelationImportSolve() {
        outputStream.println("// Load the registrars into relations");
        if (reports) {
            outputStream.println("steady_clock::time_point time1 = steady_clock::now();");
        }
        outputStream.println("P::vols_t vols = regs.volumes();");
        outputStream.print("P::rels_t relations = {");
        outputStream.print(prob().labels.stream()
                .map(l -> l.fieldDomains.stream().map(d -> "vols[" + d.index + "]").collect(Collectors.joining("*")))
                .map(s -> s.length() == 0 ? "1" : s)
                .map(s -> "relation<P::adt_t>(" + s + ")")
                .collect(Collectors.joining(",")));
        outputStream.println("};");
        prob().labels.stream().forEach(l -> outputStream.println("relations[" + pseudonym(l) + "].import_buffer(buf_" + l.name + ");"));

        outputStream.println("// Solve the problem");
        if (reports) {
            outputStream.println("steady_clock::time_point time2 = steady_clock::now();");
        }
        outputStream.println("P::solve(vols, relations);");
        if (reports) {
            outputStream.println("steady_clock::time_point time3 = steady_clock::now();");
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
        outputStream.println(bufferDeclaration(lbl, name) + ";");
    }

    private void generateMainEnd() throws CFLRException {
        outputStream.println("// print the specified relations to stdout");
        outputStream.println("for(int i=2; i<argc; ++i){");
        outputStream.println(prob().labels.stream()
                .map(l -> "if(string(argv[i]) == \"" + l.name + "\"){\n" +
                        "cout << \"__" + l.name + "__\" << endl;\n" +
                        bufferDeclaration(l, "tmp_buf") + ";\n" +
                        "relations[" + pseudonym(l) + "].export_buffer(tmp_buf);\n" +
                        "tmp_buf.to_csv(cout);\n" +
                        "}")
                .collect(Collectors.joining(" else ")));
        outputStream.println("}");
        if (reports) {
            outputStream.println("steady_clock::time_point time4 = steady_clock::now();");
            outputStream.println("cerr << \"input csv files=\" << duration_cast<duration<double>>(time1 - time0).count() << endl;");
            outputStream.println("cerr << \"convert csv to relation=\" << duration_cast<duration<double>>(time2 - time1).count() << endl;");
            outputStream.println("cerr << \"solve semi-naive=\" << duration_cast<duration<double>>(time3 - time2).count() << endl;");
            outputStream.println("cerr << \"output csv files=\" << duration_cast<duration<double>>(time4 - time3).count() << endl;");
            prob().labels.stream().forEach(l -> {
                generateBufferDeclaration(l, "count_buf_" + l.name);
                outputStream.println("relations[" + pseudonym(l) + "].export_buffer(count_buf_" + l.name + ");");
                outputStream.println("cerr << \"|" + l.name + "|=\" << count_buf_" + l.name + ".size() << endl;");
            });
            prob().fieldDomains.stream().forEach(d -> outputStream.println("cerr << \"f:" + d.name + "=\" << vols[" + pseudonym(d) + "] << std::endl;"));
            prob().vertexDomains.stream().forEach(d -> outputStream.println("cerr << \"v:" + d.name + "=\" << vols[" + pseudonym(d) + "] << std::endl;"));
        }
        outputStream.println("return 0;");
        outputStream.println("}");
    }
}
