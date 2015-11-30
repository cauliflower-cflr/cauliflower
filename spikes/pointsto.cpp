/* 
 *                               pointsto.cpp
 * 
 * Author: Nic H.
 * Date: 2015-Nov-30
 */

#include <iostream>
#include <string>

#include "relation_buffer.h"
#include "pt_OUT.h"

using namespace cflr;
using namespace std;

typedef registrar_group<string, string, string> registrars_t;

int main(int argc, char* argv[]){

    if(argc != 2){
        cerr << "Usage: pointsto <path-to-pointsto-example>" << endl;
        return 1;
    }

    registrars_t regs;
    relation_buffer<string, string> al_buf(regs.select<0, 1>());
    relation_buffer<string, string> as_buf(regs.select<0, 0>());
    relation_buffer<string, string, string> lo_buf(regs.select<0, 0, 2>());
    relation_buffer<string, string, string> st_buf(regs.select<0, 0, 2>());
    relation_buffer<string, string> br_buf(regs.select<0, 0>());
    relation_buffer<string, string> pt_buf(regs.select<0, 1>());

    al_buf.from_csv(string(argv[1]) + "/alloc.csv");
    as_buf.from_csv(string(argv[1]) + "/assign.csv");
    lo_buf.from_csv(string(argv[1]) + "/load.csv");
    st_buf.from_csv(string(argv[1]) + "/store.csv");

    cout << "|V| = " << std::get<0>(regs.group).size() << endl;
    cout << "|H| = " << std::get<1>(regs.group).size() << endl;
    cout << "|F| = " << std::get<2>(regs.group).size() << endl;
    cout << "Alloc:" << endl;
    al_buf.to_csv(cout);
    cout << "Assign:" << endl;
    as_buf.to_csv(cout);
    cout << "Load:" << endl;
    lo_buf.to_csv(cout);
    cout << "Store:" << endl;
    st_buf.to_csv(cout);

    typedef pt_semi_naive::adt_t A;

    pt_semi_naive::vols_t vols = regs.volumes();
    pt_semi_naive::rels_t relations = {relation<A>(1), relation<A>(1), relation<A>(vols[2]), relation<A>(vols[2]), relation<A>(1), relation<A>(1)};
    relations[0].import_buffer(al_buf);
    relations[1].import_buffer(as_buf);
    relations[2].import_buffer(lo_buf);
    relations[3].import_buffer(st_buf);
    relations[0].dump(cout);
    relations[1].dump(cout);
    relations[2].dump(cout);
    relations[3].dump(cout);
    relations[4].dump(cout);
    relations[5].dump(cout);

    pt_semi_naive::solve(vols, relations);
    cout << "-----------------" << endl;

    relations[0].dump(cout);
    relations[1].dump(cout);
    relations[2].dump(cout);
    relations[3].dump(cout);
    relations[4].dump(cout);
    relations[5].dump(cout);

    relations[4].export_buffer(br_buf);
    relations[5].export_buffer(pt_buf);
    cout << "Bridge:" << endl;
    br_buf.to_csv(cout);
    cout << "PointsTo:" << endl;
    pt_buf.to_csv(cout);

    return 0;
}
