/* 
 *                                 fld.csv
 * 
 * Author: Nic H.
 * Date: 2015-Nov-30
 */

#include <iostream>
#include <string>

#include "relation_buffer.h"
#include "fld_OUT.h"

using namespace cflr;
using namespace std;

typedef registrar_group<string, string> registrars_t;

int main(int argc, char* argv[]){

    if(argc != 2){
        cerr << "Usage: fld <path-to-pointsto-example>" << endl;
        return 1;
    }

    registrars_t regs;
    relation_buffer<string, string, string> kn_buf(regs.select<0, 0, 1>());
    relation_buffer<string, string, string> se_buf(regs.select<0, 0, 1>());
    relation_buffer<string, string> lo_buf(regs.select<0, 0>());

    kn_buf.from_csv(string(argv[1]) + "/knows.csv");
    se_buf.from_csv(string(argv[1]) + "/sees.csv");

    cout << "Knows:" << endl;
    kn_buf.to_csv(cout);
    cout << "Sees:" << endl;
    se_buf.to_csv(cout);

    typedef fld_semi_naive::adt_t A;

    fld_semi_naive::vols_t vols = regs.volumes();
    fld_semi_naive::rels_t relations = {relation<A>(vols[1]), relation<A>(vols[1]), relation<A>(1)};
    relations[0].import_buffer(kn_buf);
    relations[1].import_buffer(se_buf);
    relations[0].dump(cout);
    relations[1].dump(cout);
    relations[2].dump(cout);

    fld_semi_naive::solve(vols, relations);
    cout << "-----------------" << endl;

    relations[0].dump(cout);
    relations[1].dump(cout);
    relations[2].dump(cout);

    relations[2].export_buffer(lo_buf);
    cout << "Looks At:" << endl;
    lo_buf.to_csv(cout);

    return 0;
}
