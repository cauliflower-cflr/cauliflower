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

    cflr::neighbourhood_map<map<ident, set<ident>>, set<ident>> m;
    cout << m.empty() << (m.begin() == m.end() ? " ended": " going") << endl;
    m.initialise_import();
    m.import(0, 0);
    m.import(0, 4);
    m.import(2, 4);
    m.finalise_import();
    cout << m.empty() << (m.begin() == m.end() ? " ended": " going") << endl;
    for(auto i=m.begin(); i!=m.end(); ++i){
        cout << " - " << i->first << i->second << endl;
    }
    cout << m.empty() << (m.begin() == m.end() ? " ended": " going") << endl;

    // Show result
    cout << endl << "S:" << endl;
    s_buf.to_csv(cout);
    cout << endl;
}

