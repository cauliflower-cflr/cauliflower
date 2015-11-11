/* 
 *                                 main.cpp
 * 
 * Author: Nic H.
 * Date: 2015-Nov-10
 */

#include <iostream>

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

    // Show result
    cout << endl << "S:" << endl;
    s_buf.to_csv(cout);
    cout << endl;
}

