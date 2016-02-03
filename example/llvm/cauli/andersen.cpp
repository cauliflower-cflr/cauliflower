// andersen
//
// Automatic importer for CFL-R Problems
//
// Generated on: 12/01/2016
//           by: v0.0.1
#include <chrono>
#include <iostream>
#include <string>
#include "relation_buffer.h"
#include "/home/nic/project/cauliflower/example/llvm/cauli/andersen.h"
using namespace std;
using namespace std::chrono;
using namespace cflr;
int main(int argc, char* argv[]) {
    // Confirm the CSV directory has been provided as an argument
    if(argc < 2) {
        cerr << "Usage: " << argv[0] << " <path-to-input-csv-directory> [output-relations...]" << endl;
        return 1;
    }
    // The group of string registrars
    registrar_group<string, string> regs;
    relation_buffer<string,string> buf_0(regs.select<0,1>());
    relation_buffer<string,string> buf_1(regs.select<0,0>());
    relation_buffer<string,string> buf_2(regs.select<0,0>());
    relation_buffer<string,string> buf_3(regs.select<0,0>());
    relation_buffer<string,string> buf_4(regs.select<0,1>());
    relation_buffer<string,string> buf_5(regs.select<0,0>());
    // Import the CSV files into the registrars
    buf_0.from_csv(string(argv[1]) + "/ref.csv");
    buf_1.from_csv(string(argv[1]) + "/assign.csv");
    buf_2.from_csv(string(argv[1]) + "/load.csv");
    buf_3.from_csv(string(argv[1]) + "/store.csv");
    buf_4.from_csv(string(argv[1]) + "/pointsto.csv");
    buf_5.from_csv(string(argv[1]) + "/alias.csv");
    // Load the registrars into relations
    typedef andersen_semi_naive P;
    P::vols_t vols = regs.volumes();
    P::rels_t relations = {relation<P::adt_t>(1),relation<P::adt_t>(1),relation<P::adt_t>(1),relation<P::adt_t>(1),relation<P::adt_t>(1),relation<P::adt_t>(1)};
    relations[0].import_buffer(buf_0);
    relations[1].import_buffer(buf_1);
    relations[2].import_buffer(buf_2);
    relations[3].import_buffer(buf_3);
    relations[4].import_buffer(buf_4);
    relations[5].import_buffer(buf_5);
    // Solve the problem
    P::solve(vols, relations);
    // print the specified relations to stdout
    for(int i=2; i<argc; ++i) {
        if(string(argv[i]) == "ref") {
            cout << "__ref__" << endl;
            relation_buffer<string,string> tmp_buf(regs.select<0,1>());
            relations[0].export_buffer(tmp_buf);
            tmp_buf.to_csv(cout);
        }
        else if(string(argv[i]) == "assign") {
            cout << "__assign__" << endl;
            relation_buffer<string,string> tmp_buf(regs.select<0,0>());
            relations[1].export_buffer(tmp_buf);
            tmp_buf.to_csv(cout);
        }
        else if(string(argv[i]) == "load") {
            cout << "__load__" << endl;
            relation_buffer<string,string> tmp_buf(regs.select<0,0>());
            relations[2].export_buffer(tmp_buf);
            tmp_buf.to_csv(cout);
        }
        else if(string(argv[i]) == "store") {
            cout << "__store__" << endl;
            relation_buffer<string,string> tmp_buf(regs.select<0,0>());
            relations[3].export_buffer(tmp_buf);
            tmp_buf.to_csv(cout);
        }
        else if(string(argv[i]) == "pointsto") {
            cout << "__pointsto__" << endl;
            relation_buffer<string,string> tmp_buf(regs.select<0,1>());
            relations[4].export_buffer(tmp_buf);
            tmp_buf.to_csv(cout);
        }
        else if(string(argv[i]) == "alias") {
            cout << "__alias__" << endl;
            relation_buffer<string,string> tmp_buf(regs.select<0,0>());
            relations[5].export_buffer(tmp_buf);
            tmp_buf.to_csv(cout);
        }
    }
    return 0;
}
