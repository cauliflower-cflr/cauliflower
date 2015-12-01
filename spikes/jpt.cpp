#include <iostream>
#include <string>

#include "relation_buffer.h"
#include "jpt_OUT.h"

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

    al_buf.from_csv(string(argv[1]) + "/Alloc.csv");
    as_buf.from_csv(string(argv[1]) + "/Assign.csv");
    lo_buf.from_csv(string(argv[1]) + "/Load.csv");
    st_buf.from_csv(string(argv[1]) + "/Store.csv");

    typedef jpt_semi_naive::adt_t A;

    jpt_semi_naive::vols_t vols = regs.volumes();
    jpt_semi_naive::rels_t relations = {relation<A>(1), relation<A>(1), relation<A>(vols[2]), relation<A>(vols[2]), relation<A>(1), relation<A>(1), relation<A>(vols[2]), relation<A>(vols[2])};
    relations[0].import_buffer(al_buf);
    relations[1].import_buffer(as_buf);
    relations[2].import_buffer(lo_buf);
    relations[3].import_buffer(st_buf);

    jpt_semi_naive::solve(vols, relations);

    relations[5].export_buffer(pt_buf);
    pt_buf.to_csv(cout);

    return 0;
}

