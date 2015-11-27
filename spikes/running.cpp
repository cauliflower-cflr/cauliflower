#include <iostream>
#include "running_OUT.h"

using namespace std;
using namespace cflr;

int main(){
    typedef running_semi_naive::adt_t adt_t;
    running_semi_naive::rels_t rels{relation<adt_t>(1), relation<adt_t>(1), relation<adt_t>(1)};
    rels[0].adts[0].initialise_import();
    rels[0].adts[0].import(0, 1);
    rels[0].adts[0].import(1, 0);
    rels[0].adts[0].finalise_import();
    rels[1].adts[0].initialise_import();
    rels[1].adts[0].import(0,4);
    rels[1].adts[0].import(0,2);
    rels[1].adts[0].import(2,3);
    rels[1].adts[0].import(3,4);
    rels[1].adts[0].finalise_import();

    running_semi_naive::vols_t volume;
    volume[0] = 5;

    cout << "\na" << endl;
    rels[0].dump(cout);
    cout << "\nb" << endl;
    rels[1].dump(cout);
    cout << "\n-----------------" << endl;
    running_semi_naive::solve(volume, rels);
    cout << "\nS" << endl;
    rels[2].dump(cout);
}
