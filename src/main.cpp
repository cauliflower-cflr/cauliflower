/* 
 *                                 main.cpp
 * 
 * Author: Nic H.
 * Date: 2015-Nov-10
 */

#include <map>
#include <set>
#include <iostream>

#include "adt.h"
#include "neighbourhood_map.h"
#include "problem.h"
#include "relation.h"
#include "relation_buffer.h"

using namespace std;
using namespace cflr;

int main(){
    // Init
    registrar<int> vertices;
    tuple<registrar<int>*, registrar<int>*> vert_regs{&vertices, &vertices};
    relation_buffer<int, int> a_buf(vert_regs);
    relation_buffer<int, int> b_buf(vert_regs);
    relation_buffer<int, int> s_buf(vert_regs);

    // Read inputs
    a_buf.from_csv("example/running/a.csv");
    cout << "a:" << endl;
    a_buf.to_csv(cout);
    b_buf.from_csv("example/running/b.csv");
    cout << endl << "b:" << endl;
    b_buf.to_csv(cout);
    cout << endl << "---------------------------" << endl;

    // Convert to relations
    typedef cflr::neighbourhood_map<map<ident, set<ident>>,set<ident>> nmap;
    relation<nmap> a_rel(a_buf.field_volume());
    relation<nmap> b_rel(b_buf.field_volume());
    relation<nmap> s_rel(1);
    a_rel.import_buffer(a_buf);
    b_rel.import_buffer(b_buf);
    a_rel.dump(cout);
    b_rel.dump(cout);
    typedef problem<
        label<>,
        label<>,
        label<>,
        rule<2>,
        rule<2, fwd<0>, fwd<2>, fwd<1>>
        > prob;
    cout << prob::label_count << " : " << prob::rule_count << endl << endl;
    nmap eps = nmap::identity(5);
    s_rel.adts[0].union_copy(eps);
    s_rel.adts[0].dump(cout);
    for(unsigned i=0; i<4; i++){
        nmap tmp;
        a_rel.adts[0].compose(s_rel.adts[0], tmp);
        tmp.compose(b_rel.adts[0], s_rel.adts[0]);
        cout << endl;
        s_rel.adts[0].dump(cout);
    }

    s_rel.adts[0].difference(eps);

    // Show result
    s_rel.export_buffer(s_buf);
    cout << endl << "S:" << endl;
    s_buf.to_csv(cout);
    cout << endl;
}

