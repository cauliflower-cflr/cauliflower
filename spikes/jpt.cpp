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
    relation_buffer<string, string> al_buf(regs.select<1, 2>());
    relation_buffer<string, string> as_buf(regs.select<1, 1>());
    relation_buffer<string, string, string> lo_buf(regs.select<1, 1, 0>());
    relation_buffer<string, string, string> st_buf(regs.select<1, 1, 0>());
    relation_buffer<string, string> br_buf(regs.select<1, 1>());
    relation_buffer<string, string> pt_buf(regs.select<1, 2>());

    al_buf.from_csv(string(argv[1]) + "/Alloc.csv");
    as_buf.from_csv(string(argv[1]) + "/Assign.csv");
    lo_buf.from_csv(string(argv[1]) + "/Load.csv");
    st_buf.from_csv(string(argv[1]) + "/Store.csv");

    typedef jpt_semi_naive::adt_t A;

    //cout << "|F|=" << std::get<0>(regs.group).size() << endl; 
    //cout << "|V|=" << std::get<1>(regs.group).size() << endl; 
    //cout << "|H|=" << std::get<2>(regs.group).size() << endl; 

    jpt_semi_naive::vols_t vols = regs.volumes();
    jpt_semi_naive::rels_t relations = {relation<A>(1), relation<A>(1), relation<A>(vols[0]), relation<A>(vols[0]), relation<A>(1), relation<A>(1), relation<A>(vols[0]), relation<A>(vols[0])};
    relations[0].import_buffer(al_buf);
    //relations[0].dump(std::cout);
    relations[1].import_buffer(as_buf);
    //relations[1].dump(std::cout);
    relations[2].import_buffer(lo_buf);
    //relations[2].dump(std::cout);
    relations[3].import_buffer(st_buf);
    //relations[3].dump(std::cout);

    jpt_semi_naive::solve(vols, relations);

    relations[5].export_buffer(pt_buf);
    pt_buf.to_csv(cout);

    for(int i=3; i<argc; ++i){
        if(argv[i] == "VarPointsTo"){
            relation_buffer<string, string> tmp_buf(regs.select<1, 2>());
            relations[5].export_buffer(tmp_buf);
            tmp_buf.tocsv(cout);
        }
    }

    return 0;
}

